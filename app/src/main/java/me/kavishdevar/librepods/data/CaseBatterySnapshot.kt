// SPDX-License-Identifier: GPL-3.0-or-later

package me.kavishdevar.librepods.data

data class CaseBatteryReading(
    val level: Int,
    val charging: Boolean,
    val estimated: Boolean,
    val observedAt: Long,
)

data class CaseBatterySnapshot(
    val deviceAddress: String? = null,
    val direct: CaseBatteryReading? = null,
    val nearby: CaseBatteryReading? = null,
    val lastKnown: CaseBatteryReading? = null,
) {
    fun startSession(address: String) = CaseBatterySnapshot(
        deviceAddress = address,
        lastKnown = lastKnown.takeIf { address == deviceAddress },
    )

    fun receiveDirect(report: List<Battery>, now: Long): CaseBatterySnapshot {
        val battery = report.firstOrNull { it.component == BatteryComponent.CASE && it.hasValidReading }
        val reading = battery?.let { CaseBatteryReading(it.level, it.isCharging, false, now) }
        val updated = copy(direct = reading)
        return updated.copy(lastKnown = updated.current(now) ?: lastKnown)
    }

    fun receiveNearby(owner: String?, level: Int?, charging: Boolean, estimated: Boolean, now: Long): CaseBatterySnapshot {
        if (owner == null || owner != deviceAddress || level == null || level !in 0..100) return this
        val updated = copy(nearby = CaseBatteryReading(level, charging, estimated, now))
        return updated.copy(lastKnown = updated.current(now) ?: lastKnown)
    }

    fun current(now: Long): CaseBatteryReading? {
        val directCurrent = direct?.takeIf { now - it.observedAt < DIRECT_LIFETIME_MS }
        val nearbyCurrent = nearby?.takeIf { now - it.observedAt < NEARBY_LIFETIME_MS }
        return when {
            directCurrent == null -> nearbyCurrent
            nearbyCurrent != null && !nearbyCurrent.estimated && nearbyCurrent.observedAt > directCurrent.observedAt -> nearbyCurrent
            else -> directCurrent
        }
    }

    fun nextExpiry(now: Long): Long? = listOfNotNull(
        direct?.observedAt?.plus(DIRECT_LIFETIME_MS),
        nearby?.observedAt?.plus(NEARBY_LIFETIME_MS),
    ).filter { it > now }.minOrNull()

    companion object {
        private const val DIRECT_LIFETIME_MS = 30_000L
        private const val NEARBY_LIFETIME_MS = 12_000L
    }
}
