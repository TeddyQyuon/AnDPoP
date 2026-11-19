/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package me.kavishdevar.librepods.data

import android.os.Parcelable
import android.util.Log
import kotlinx.parcelize.Parcelize

// TODO: Remove everything but Battery-related stuff

enum class Enums(val value: ByteArray) {
    NOISE_CANCELLATION(byteArrayOf(0x0d)),
    PREFIX(byteArrayOf(0x04, 0x00, 0x04, 0x00)),
    SETTINGS(byteArrayOf(0x09, 0x00)),
    NOISE_CANCELLATION_PREFIX(PREFIX.value + SETTINGS.value + NOISE_CANCELLATION.value),
    CONVERSATION_AWARENESS_RECEIVE_PREFIX(PREFIX.value + byteArrayOf(0x4b, 0x00, 0x02, 0x00)),
}

object BatteryComponent {
    const val HEADPHONES = 1
    const val LEFT = 4
    const val RIGHT = 2
    const val CASE = 8
}

object BatteryStatus {
    const val CHARGING = 1
    const val NOT_CHARGING = 2
    const val DISCONNECTED = 4
    const val OPTIMIZED_CHARGING = 5
}

@Parcelize
data class Battery(val component: Int, val level: Int, val status: Int) : Parcelable {
    val isCharging: Boolean
        get() = status == BatteryStatus.CHARGING || status == BatteryStatus.OPTIMIZED_CHARGING

    val hasValidReading: Boolean
        get() = level in 0..100 && (isCharging || status == BatteryStatus.NOT_CHARGING)

    fun getComponentName(): String? {
        return when (component) {
            BatteryComponent.HEADPHONES -> "HEADPHONES"
            BatteryComponent.LEFT -> "LEFT"
            BatteryComponent.RIGHT -> "RIGHT"
            BatteryComponent.CASE -> "CASE"
            else -> null
        }
    }

    fun getStatusName(): String? {
        return when (status) {
            BatteryStatus.CHARGING -> "CHARGING"
            BatteryStatus.NOT_CHARGING -> "NOT_CHARGING"
            BatteryStatus.DISCONNECTED -> "DISCONNECTED"
            BatteryStatus.OPTIMIZED_CHARGING -> "OPTIMIZED_CHARGING"
            else -> null
        }
    }
}

enum class NoiseControlMode {
    OFF,  NOISE_CANCELLATION, TRANSPARENCY, ADAPTIVE
}

class AirPodsNotifications {
    companion object {
        const val AIRPODS_CONNECTED = "app.andpop.android.AIRPODS_CONNECTED"
        const val AIRPODS_L2CAP_CONNECTED = "app.andpop.android.AIRPODS_CONNECTED"
        const val AIRPODS_DATA = "app.andpop.android.AIRPODS_DATA"
        const val EAR_DETECTION_DATA = "app.andpop.android.EAR_DETECTION_DATA"
        const val ANC_DATA = "app.andpop.android.ANC_DATA"
        const val BATTERY_DATA = "app.andpop.android.BATTERY_DATA"
        const val CA_DATA = "app.andpop.android.CA_DATA"
        const val AIRPODS_DISCONNECTED = "app.andpop.android.AIRPODS_DISCONNECTED"
        const val AIRPODS_CONNECTION_DETECTED = "app.andpop.android.AIRPODS_CONNECTION_DETECTED"
        const val DISCONNECT_RECEIVERS = "app.andpop.android.DISCONNECT_RECEIVERS"
        const val EQ_DATA = "app.andpop.android.HEADPHONE_ACCOMMODATION"
        const val AIRPODS_INFORMATION_UPDATED = "app.andpop.android.AIRPODS_INFORMATION_UPDATED"
    }

    class EarDetection {
        private val notificationBit = 6.toByte()
        private val notificationPrefix = Enums.PREFIX.value + notificationBit

        var status: List<Byte> = listOf(0x01, 0x01)

        fun setStatus(data: ByteArray) {
            status = listOf(data[6], data[7])
        }

        fun isEarDetectionData(data: ByteArray): Boolean {
            if (data.size != 8) {
                return false
            }
            val prefixHex = notificationPrefix.joinToString("") { "%02x".format(it) }
            val dataHex = data.joinToString("") { "%02x".format(it) }
            return dataHex.startsWith(prefixHex)
        }
    }

    class ANC {
        private val notificationPrefix = Enums.NOISE_CANCELLATION_PREFIX.value

        var status: Int = 1
            private set

        fun isANCData(data: ByteArray): Boolean {
            if (data.size != 11) {
                return false
            }
            val prefixHex = notificationPrefix.joinToString("") { "%02x".format(it) }
            val dataHex = data.joinToString("") { "%02x".format(it) }
            return dataHex.startsWith(prefixHex)
        }

        fun setStatus(data: ByteArray) {
            when (data.size) {
                // if the whole packet is given
                11 -> {
                    status = data[7].toInt()
                }
                // if only the data is given
                1 -> {
                    status = data[0].toInt()
                }
                // if the value of control command is given
                4 -> {
                    status = data[0].toInt()
                }
                else -> {
                    Log.d("ANC", "Invalid ANC data size: ${data.size}")
                }
            }
        }

        val name: String =
            when (status) {
                1 -> "OFF"
                2 -> "ON"
                3 -> "TRANSPARENCY"
                4 -> "ADAPTIVE"
                else -> "UNKNOWN"
            }

    }

    class BatteryNotification {
        @Volatile private var readings: List<Battery> = emptyList()

        fun setBatteryDirect(
            leftLevel: Int?,
            leftCharging: Boolean,
            rightLevel: Int?,
            rightCharging: Boolean,
            caseLevel: Int?,
            caseCharging: Boolean
        ) {
            fun reading(component: Int, level: Int?, charging: Boolean): Battery? {
                if (level == null || level !in 0..100) return null
                val status = if (charging) BatteryStatus.CHARGING else BatteryStatus.NOT_CHARGING
                return Battery(component, level, status)
            }

            readings = listOfNotNull(
                reading(BatteryComponent.LEFT, leftLevel, leftCharging),
                reading(BatteryComponent.RIGHT, rightLevel, rightCharging),
                reading(BatteryComponent.CASE, caseLevel, caseCharging),
            )
        }

        fun setBattery(data: ByteArray): List<Battery>? {
            // AACP sends a component count followed by five-byte records.
            val count = data.getOrNull(6)?.toInt()?.and(0xff) ?: return null
            if (count == 0 || data.size < 7 + count * 5) return null

            val report = List(count) { index ->
                val start = 7 + index * 5
                Battery(data[start].toInt(), data[start + 2].toInt() and 0xff, data[start + 3].toInt())
            }
            readings = report
            return report
        }

        fun clear() { readings = emptyList() }

        fun getBattery(): List<Battery> = readings
    }

    class ConversationAwarenessNotification {
        @Suppress("PrivatePropertyName")
        private val NOTIFICATION_PREFIX = Enums.CONVERSATION_AWARENESS_RECEIVE_PREFIX.value

        var status: Byte = 0
            private set

        fun isConversationAwarenessData(data: ByteArray): Boolean {
            if (data.size != 10) {
                return false
            }
            val prefixHex = NOTIFICATION_PREFIX.joinToString("") { "%02x".format(it) }
            val dataHex = data.joinToString("") { "%02x".format(it) }
            return dataHex.startsWith(prefixHex)
        }

        fun setData(data: ByteArray) {
            status = data[9]
        }
    }
}

fun isHeadTrackingData(data: ByteArray): Boolean {
    if (data.size <= 60) return false

    val prefixPattern = byteArrayOf(
        0x04, 0x00, 0x04, 0x00, 0x17, 0x00, 0x00, 0x00,
        0x10, 0x00
    )

    for (i in prefixPattern.indices) {
        if (data[i] != prefixPattern[i]) return false
    }

    if (data[10] != 0x44.toByte() && data[10] != 0x45.toByte()) return false

    if (data[11] != 0x00.toByte()) return false

    return true
}
