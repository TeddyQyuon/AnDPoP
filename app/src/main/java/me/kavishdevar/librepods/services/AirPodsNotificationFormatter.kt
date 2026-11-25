// SPDX-License-Identifier: GPL-3.0-or-later

package me.kavishdevar.librepods.services

import android.content.Context
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.data.Battery
import me.kavishdevar.librepods.data.BatteryComponent
import me.kavishdevar.librepods.data.CaseBatteryReading
import me.kavishdevar.librepods.data.ListeningModeCycle

/**
 * Pure formatting helpers for AnDPoP's foreground-service and connected-status notifications.
 */
internal object AirPodsNotificationFormatter {
    fun runningTitle(context: Context): String =
        context.getString(R.string.andpop_notif_running)

    fun formatTitle(context: Context, deviceName: String?): String =
        deviceName?.takeIf { it.isNotBlank() } ?: context.getString(R.string.andpop_airpods_fallback)

    fun formatStatusSubtitle(context: Context, listeningMode: Int?): String {
        val mode = listeningMode?.takeIf { it in ListeningModeCycle.OFF..ListeningModeCycle.ADAPTIVE }
        return if (mode != null) {
            val modeLabel = context.getString(ListeningModeCycle.label(mode))
            context.getString(R.string.andpop_notif_connected_mode, modeLabel)
        } else {
            context.getString(R.string.andpop_connected)
        }
    }

    fun formatBatterySummary(
        context: Context,
        batteryList: List<Battery>?,
        caseReading: CaseBatteryReading? = null,
        caseCurrent: Boolean = false,
    ): String {
        val valid = batteryList.orEmpty().filter { it.hasValidReading }
        val parts = mutableListOf<String>()
        val headphones = valid.firstOrNull { it.component == BatteryComponent.HEADPHONES }
        if (headphones != null) {
            parts += formatEntry(context.getString(R.string.andpop_widget_headphones), headphones.level, headphones.isCharging)
        } else {
            valid.firstOrNull { it.component == BatteryComponent.LEFT }?.let { left ->
                parts += formatEntry(context.getString(R.string.andpop_widget_left), left.level, left.isCharging)
            }
            valid.firstOrNull { it.component == BatteryComponent.RIGHT }?.let { right ->
                parts += formatEntry(context.getString(R.string.andpop_widget_right), right.level, right.isCharging)
            }
            val directCase = valid.firstOrNull { it.component == BatteryComponent.CASE }
            when {
                caseReading != null && caseCurrent && caseReading.level in 0..100 -> {
                    val est = if (caseReading.estimated) "~" else ""
                    val bolt = if (caseReading.charging) " ⚡" else ""
                    parts += "${context.getString(R.string.andpop_widget_case)} $est${caseReading.level}%$bolt"
                }
                directCase != null -> {
                    parts += formatEntry(context.getString(R.string.andpop_widget_case), directCase.level, directCase.isCharging)
                }
            }
        }
        return if (parts.isNotEmpty()) {
            parts.joinToString(" · ")
        } else {
            context.getString(R.string.andpop_battery_unavailable)
        }
    }

    private fun formatEntry(label: String, level: Int, charging: Boolean): String {
        val bolt = if (charging) " ⚡" else ""
        return "$label $level%$bolt"
    }
}
