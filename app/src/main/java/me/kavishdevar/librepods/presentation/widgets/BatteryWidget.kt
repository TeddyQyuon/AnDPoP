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

@file:OptIn(ExperimentalEncodingApi::class)

package me.kavishdevar.librepods.presentation.widgets

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import me.kavishdevar.librepods.MainActivity
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.theme.AndpopPalette
import me.kavishdevar.librepods.presentation.theme.resolveSavedAndpopPalette
import me.kavishdevar.librepods.services.ServiceManager
import kotlin.io.encoding.ExperimentalEncodingApi

class BatteryWidget : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BatteryGlanceWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        ServiceManager.getService()?.updateBatteryWidget()
    }
}

class BatteryGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val palette = resolveSavedAndpopPalette(context)
            val state = WidgetStateStore.loadBattery(context)
            BatteryWidgetContent(context = context, palette = palette, state = state)
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun BatteryWidgetContent(
    context: Context,
    palette: AndpopPalette,
    state: WidgetBatteryState,
) {
    val cells = buildList {
        if (state.isMax) {
            add(
                Triple(
                    context.getString(R.string.andpop_widget_headphones),
                    state.headphonesLevel to state.headphonesCharging,
                    R.drawable.ic_stat_andpop,
                ),
            )
        } else {
            add(
                Triple(
                    context.getString(R.string.andpop_widget_left),
                    state.leftLevel to state.leftCharging,
                    R.drawable.ic_stat_andpop,
                ),
            )
            add(
                Triple(
                    context.getString(R.string.andpop_widget_right),
                    state.rightLevel to state.rightCharging,
                    R.drawable.ic_stat_andpop,
                ),
            )
            add(
                Triple(
                    context.getString(R.string.andpop_widget_case),
                    state.caseLevel to state.caseCharging,
                    R.drawable.airpods_pro_case_notification,
                ),
            )
        }
        if (state.showPhone) {
            add(
                Triple(
                    context.getString(R.string.andpop_widget_phone),
                    state.phoneLevel to state.phoneCharging,
                    R.drawable.smartphone,
                ),
            )
        }
    }

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(ColorProvider(palette.s1))
            .clickable(actionStartActivity<MainActivity>())
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        cells.forEach { (label, reading, iconRes) ->
            val (level, charging) = reading
            val ring = WidgetRingRenderer.renderBatteryRing(
                context = context,
                palette = palette,
                level = level,
                charging = charging && level != null,
                iconRes = iconRes,
            )
            val valueText = if (level != null) {
                context.getString(R.string.andpop_percent_value, level)
            } else {
                context.getString(R.string.andpop_battery_empty)
            }

            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    provider = ImageProvider(ring),
                    contentDescription = "$label $valueText",
                    modifier = GlanceModifier.size(48.dp),
                )
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    text = valueText,
                    style = TextStyle(
                        color = ColorProvider(if (level != null) palette.on else palette.on3),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
                Text(
                    text = label,
                    style = TextStyle(
                        color = ColorProvider(palette.on2),
                        fontSize = 11.sp,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}
