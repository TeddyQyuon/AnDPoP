package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.model.BlePodSnapshot
import com.example.model.PodFormFactor

data class WidgetBatteryState(
    val deviceName: String,
    val statusText: String,
    val leftLabel: String,
    val leftValue: String,
    val rightLabel: String,
    val rightValue: String,
    val caseLabel: String,
    val caseValue: String,
    val summaryLine: String,
)

/**
 * Android Home Screen Battery Widget displaying live L / R / Case (or Headset) levels
 * for the primary matched CAPod profile.
 */
class CAPodBatteryWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val state = readCachedState(context)
        for (widgetId in appWidgetIds) {
            appWidgetManager.updateAppWidget(widgetId, buildRemoteViews(context, state))
        }
    }

    companion object {
        private const val PREFS_NAME = "capod_widget_cache"
        private const val KEY_DEVICE_NAME = "device_name"
        private const val KEY_STATUS = "status"
        private const val KEY_LEFT_LABEL = "left_label"
        private const val KEY_LEFT_VALUE = "left_value"
        private const val KEY_RIGHT_LABEL = "right_label"
        private const val KEY_RIGHT_VALUE = "right_value"
        private const val KEY_CASE_LABEL = "case_label"
        private const val KEY_CASE_VALUE = "case_value"
        private const val KEY_SUMMARY = "summary"

        fun updateFromSnapshot(
            context: Context,
            deviceName: String,
            snapshot: BlePodSnapshot?,
        ) {
            val state = if (snapshot == null) {
                WidgetBatteryState(
                    deviceName = deviceName,
                    statusText = context.getString(R.string.widget_status_idle),
                    leftLabel = context.getString(R.string.label_left_pod),
                    leftValue = "--%",
                    rightLabel = context.getString(R.string.label_right_pod),
                    rightValue = "--%",
                    caseLabel = context.getString(R.string.label_case),
                    caseValue = "--%",
                    summaryLine = context.getString(R.string.status_waiting_signal),
                )
            } else if (snapshot.model.formFactor == PodFormFactor.SINGLE_HEADSET) {
                val pct = snapshot.headsetBattery?.let {
                    "$it%" + if (snapshot.isHeadsetCharging) "⚡" else ""
                } ?: "--%"
                WidgetBatteryState(
                    deviceName = deviceName,
                    statusText = "${snapshot.rssi} dBm",
                    leftLabel = context.getString(R.string.cd_headset),
                    leftValue = pct,
                    rightLabel = context.getString(R.string.widget_label_signal),
                    rightValue = "${snapshot.signalBars}/4",
                    caseLabel = context.getString(R.string.widget_label_state),
                    caseValue = if (snapshot.isHeadsetCharging) "Charging" else "Active",
                    summaryLine = "$deviceName · $pct",
                )
            } else {
                val l = snapshot.leftBattery?.let { "$it%" + if (snapshot.isLeftCharging) "⚡" else "" } ?: "--%"
                val r = snapshot.rightBattery?.let { "$it%" + if (snapshot.isRightCharging) "⚡" else "" } ?: "--%"
                val c = snapshot.caseBattery?.let { "$it%" + if (snapshot.isCaseCharging) "⚡" else "" } ?: "--%"
                WidgetBatteryState(
                    deviceName = deviceName,
                    statusText = "${snapshot.rssi} dBm",
                    leftLabel = context.getString(R.string.label_left_pod),
                    leftValue = l,
                    rightLabel = context.getString(R.string.label_right_pod),
                    rightValue = r,
                    caseLabel = context.getString(R.string.label_case),
                    caseValue = c,
                    summaryLine = "L $l · R $r · C $c",
                )
            }

            cacheState(context, state)
            pushToAllWidgets(context, state)
        }

        fun readCachedState(context: Context): WidgetBatteryState {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return WidgetBatteryState(
                deviceName = prefs.getString(KEY_DEVICE_NAME, null)
                    ?: context.getString(R.string.app_name),
                statusText = prefs.getString(KEY_STATUS, null)
                    ?: context.getString(R.string.widget_status_idle),
                leftLabel = prefs.getString(KEY_LEFT_LABEL, null)
                    ?: context.getString(R.string.label_left_pod),
                leftValue = prefs.getString(KEY_LEFT_VALUE, null) ?: "--%",
                rightLabel = prefs.getString(KEY_RIGHT_LABEL, null)
                    ?: context.getString(R.string.label_right_pod),
                rightValue = prefs.getString(KEY_RIGHT_VALUE, null) ?: "--%",
                caseLabel = prefs.getString(KEY_CASE_LABEL, null)
                    ?: context.getString(R.string.label_case),
                caseValue = prefs.getString(KEY_CASE_VALUE, null) ?: "--%",
                summaryLine = prefs.getString(KEY_SUMMARY, null)
                    ?: context.getString(R.string.widget_status_idle),
            )
        }

        fun canRequestPinWidget(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
            return try {
                AppWidgetManager.getInstance(context)?.isRequestPinAppWidgetSupported == true
            } catch (_: Exception) {
                false
            }
        }

        fun requestPinWidget(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
            return try {
                val manager = AppWidgetManager.getInstance(context) ?: return false
                if (!manager.isRequestPinAppWidgetSupported) return false
                val provider = ComponentName(context, CAPodBatteryWidgetProvider::class.java)
                manager.requestPinAppWidget(provider, null, null)
            } catch (_: Exception) {
                false
            }
        }

        private fun cacheState(context: Context, state: WidgetBatteryState) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_DEVICE_NAME, state.deviceName)
                .putString(KEY_STATUS, state.statusText)
                .putString(KEY_LEFT_LABEL, state.leftLabel)
                .putString(KEY_LEFT_VALUE, state.leftValue)
                .putString(KEY_RIGHT_LABEL, state.rightLabel)
                .putString(KEY_RIGHT_VALUE, state.rightValue)
                .putString(KEY_CASE_LABEL, state.caseLabel)
                .putString(KEY_CASE_VALUE, state.caseValue)
                .putString(KEY_SUMMARY, state.summaryLine)
                .apply()
        }

        private fun pushToAllWidgets(context: Context, state: WidgetBatteryState) {
            try {
                val manager = AppWidgetManager.getInstance(context) ?: return
                val component = ComponentName(context, CAPodBatteryWidgetProvider::class.java)
                val widgetIds = manager.getAppWidgetIds(component)
                if (widgetIds.isEmpty()) return
                val views = buildRemoteViews(context, state)
                for (id in widgetIds) {
                    manager.updateAppWidget(id, views)
                }
            } catch (_: Exception) {
                // AppWidget service unavailable
            }
        }

        private fun buildRemoteViews(context: Context, state: WidgetBatteryState): RemoteViews {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            return RemoteViews(context.packageName, R.layout.widget_capod_battery).apply {
                setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                setTextViewText(R.id.widget_device_name, state.deviceName)
                setTextViewText(R.id.widget_status_badge, state.statusText)
                setTextViewText(R.id.widget_left_label, state.leftLabel)
                setTextViewText(R.id.widget_left_value, state.leftValue)
                setTextViewText(R.id.widget_right_label, state.rightLabel)
                setTextViewText(R.id.widget_right_value, state.rightValue)
                setTextViewText(R.id.widget_case_label, state.caseLabel)
                setTextViewText(R.id.widget_case_value, state.caseValue)
            }
        }
    }
}
