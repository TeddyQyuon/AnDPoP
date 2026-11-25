// SPDX-License-Identifier: GPL-3.0-or-later

package me.kavishdevar.librepods.presentation.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.BatteryManager
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.createBitmap
import me.kavishdevar.librepods.data.Battery
import me.kavishdevar.librepods.data.BatteryComponent
import me.kavishdevar.librepods.data.CaseBatteryReading
import me.kavishdevar.librepods.data.ListeningModeCycle
import me.kavishdevar.librepods.presentation.theme.AndpopPalette

internal data class WidgetBatteryState(
    val connected: Boolean,
    val isMax: Boolean,
    val leftLevel: Int?,
    val leftCharging: Boolean,
    val rightLevel: Int?,
    val rightCharging: Boolean,
    val caseLevel: Int?,
    val caseCharging: Boolean,
    val headphonesLevel: Int?,
    val headphonesCharging: Boolean,
    val showPhone: Boolean,
    val phoneLevel: Int?,
    val phoneCharging: Boolean,
)

internal data class WidgetNoiseState(
    val connected: Boolean,
    val currentMode: Int,
    val offAllowed: Boolean,
    val adaptiveSupported: Boolean,
)

internal object WidgetStateStore {
    private const val PREFS = "andpop_widgets"

    fun saveBattery(
        context: Context,
        connected: Boolean,
        deviceName: String,
        batteryList: List<Battery>,
        caseReading: CaseBatteryReading?,
        showPhoneBattery: Boolean,
    ) {
        val valid = batteryList.filter { it.hasValidReading }
        val headphones = valid.firstOrNull { it.component == BatteryComponent.HEADPHONES }
        val left = valid.firstOrNull { it.component == BatteryComponent.LEFT }
        val right = valid.firstOrNull { it.component == BatteryComponent.RIGHT }
        val directCase = valid.firstOrNull { it.component == BatteryComponent.CASE }
        val resolvedCaseLevel = caseReading?.level?.takeIf { it in 0..100 } ?: directCase?.level
        val resolvedCaseCharging = when {
            caseReading != null -> caseReading.charging
            directCase != null -> directCase.isCharging
            else -> false
        }

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putBoolean("batt_connected", connected)
            putString("device_name", deviceName)
            putBoolean("is_max", headphones != null)
            putInt("left_level", left?.level ?: -1)
            putBoolean("left_charging", left?.isCharging == true)
            putInt("right_level", right?.level ?: -1)
            putBoolean("right_charging", right?.isCharging == true)
            putInt("case_level", resolvedCaseLevel ?: -1)
            putBoolean("case_charging", resolvedCaseCharging)
            putInt("hp_level", headphones?.level ?: -1)
            putBoolean("hp_charging", headphones?.isCharging == true)
            putBoolean("show_phone", showPhoneBattery)
        }
    }

    fun loadBattery(context: Context): WidgetBatteryState {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val settings = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val showPhone = prefs.getBoolean("show_phone", settings.getBoolean("show_phone_battery_in_widget", true))
        val batteryManager = context.getSystemService(BatteryManager::class.java)
        val phoneLevel = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it in 0..100 }
        val phoneCharging = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) == BatteryManager.BATTERY_STATUS_CHARGING

        fun levelOrNull(key: String): Int? = prefs.getInt(key, -1).takeIf { it in 0..100 }

        return WidgetBatteryState(
            connected = prefs.getBoolean("batt_connected", false),
            isMax = prefs.getBoolean("is_max", false),
            leftLevel = levelOrNull("left_level"),
            leftCharging = prefs.getBoolean("left_charging", false),
            rightLevel = levelOrNull("right_level"),
            rightCharging = prefs.getBoolean("right_charging", false),
            caseLevel = levelOrNull("case_level"),
            caseCharging = prefs.getBoolean("case_charging", false),
            headphonesLevel = levelOrNull("hp_level"),
            headphonesCharging = prefs.getBoolean("hp_charging", false),
            showPhone = showPhone,
            phoneLevel = phoneLevel,
            phoneCharging = phoneCharging,
        )
    }

    fun saveNoiseControl(
        context: Context,
        connected: Boolean,
        currentMode: Int,
        offAllowed: Boolean,
        adaptiveSupported: Boolean,
    ) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putBoolean("anc_connected", connected)
            putInt("anc_mode", currentMode)
            putBoolean("anc_off_allowed", offAllowed)
            putBoolean("anc_adaptive_supported", adaptiveSupported)
        }
    }

    fun loadNoiseControl(context: Context): WidgetNoiseState {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val settings = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        return WidgetNoiseState(
            connected = prefs.getBoolean("anc_connected", false),
            currentMode = prefs.getInt("anc_mode", ListeningModeCycle.NOISE_CANCELLATION),
            offAllowed = prefs.getBoolean("anc_off_allowed", settings.getBoolean("off_listening_mode", true)),
            adaptiveSupported = prefs.getBoolean("anc_adaptive_supported", true),
        )
    }
}

internal object WidgetRingRenderer {
    fun renderBatteryRing(
        context: Context,
        palette: AndpopPalette,
        level: Int?,
        charging: Boolean,
        @DrawableRes iconRes: Int,
    ): Bitmap {
        val sizePx = 156
        val bitmap = createBitmap(sizePx, sizePx)
        val canvas = Canvas(bitmap)
        val stroke = 11f
        val pad = stroke / 2f + 10f
        val rect = RectF(pad, pad, sizePx - pad, sizePx - pad)

        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            color = palette.s3.toArgb()
        }
        canvas.drawOval(rect, trackPaint)

        if (level != null && level in 0..100) {
            val activeCharging = charging
            val ringColor = when {
                activeCharging -> palette.charge.toArgb()
                level <= 20 -> palette.warning.toArgb()
                else -> palette.pri.toArgb()
            }
            val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = stroke
                strokeCap = Paint.Cap.ROUND
                color = ringColor
            }
            val sweep = (level / 100f * 360f).coerceAtLeast(if (level > 0) 6f else 0f)
            canvas.drawArc(rect, -90f, sweep, false, progressPaint)
        }

        val iconSize = (sizePx * 0.42f).toInt()
        val left = (sizePx - iconSize) / 2
        val top = (sizePx - iconSize) / 2
        ContextCompat.getDrawable(context, iconRes)?.mutate()?.apply {
            setTint(if (level != null) palette.on.toArgb() else palette.on3.toArgb())
            setBounds(left, top, left + iconSize, top + iconSize)
            draw(canvas)
        }

        if (level != null && charging) {
            val badgeRadius = 21f
            val cx = sizePx - badgeRadius - 4f
            val cy = sizePx - badgeRadius - 4f

            val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = palette.s1.toArgb()
            }
            canvas.drawCircle(cx, cy, badgeRadius + 4f, rimPaint)

            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = palette.charge.toArgb()
            }
            canvas.drawCircle(cx, cy, badgeRadius, badgePaint)

            val boltPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = palette.bolt.toArgb()
            }
            val bolt = Path().apply {
                val s = badgeRadius * 0.72f
                moveTo(cx + s * 0.14f, cy - s)
                lineTo(cx - s * 0.62f, cy + s * 0.08f)
                lineTo(cx - s * 0.05f, cy + s * 0.08f)
                lineTo(cx - s * 0.22f, cy + s)
                lineTo(cx + s * 0.62f, cy - s * 0.08f)
                lineTo(cx + s * 0.05f, cy - s * 0.08f)
                close()
            }
            canvas.drawPath(bolt, boltPaint)
        }

        return bitmap
    }
}
