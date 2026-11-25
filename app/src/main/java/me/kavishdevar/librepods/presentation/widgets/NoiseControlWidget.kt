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
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import me.kavishdevar.librepods.MainActivity
import me.kavishdevar.librepods.bluetooth.AACPManager
import me.kavishdevar.librepods.data.ListeningModeCycle
import me.kavishdevar.librepods.presentation.theme.resolveSavedAndpopPalette
import me.kavishdevar.librepods.services.ServiceManager
import kotlin.io.encoding.ExperimentalEncodingApi

private val MODE_PARAM = ActionParameters.Key<Int>("anc_mode")

class NoiseControlWidget : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NoiseControlGlanceWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        ServiceManager.getService()?.updateNoiseControlWidget()
    }
}

class SetListeningModeAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val mode = parameters[MODE_PARAM] ?: return
        val service = ServiceManager.getService()
        if (service == null) {
            Log.w("NoiseControlWidget", "Service unavailable for mode switch")
            return
        }
        service.aacpManager.sendControlCommand(
            AACPManager.Companion.ControlCommandIdentifiers.LISTENING_MODE.value,
            mode.toByte(),
        )
    }
}

class NoiseControlGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val palette = resolveSavedAndpopPalette(context)
            val state = WidgetStateStore.loadNoiseControl(context)
            NoiseControlWidgetContent(context = context, palette = palette, state = state)
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun NoiseControlWidgetContent(
    context: Context,
    palette: me.kavishdevar.librepods.presentation.theme.AndpopPalette,
    state: WidgetNoiseState,
) {
    val modes = ListeningModeCycle.available(
        offAllowed = state.offAllowed,
        adaptiveSupported = state.adaptiveSupported,
    )

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(ColorProvider(palette.s1))
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        modes.forEach { mode ->
            val selected = state.connected && state.currentMode == mode
            val bgColor = if (selected) palette.pc else palette.s1
            val contentInk = if (selected) palette.onPc else palette.on2
            val label = context.getString(ListeningModeCycle.label(mode))
            val clickAction = if (state.connected) {
                actionRunCallback<SetListeningModeAction>(actionParametersOf(MODE_PARAM to mode))
            } else {
                actionStartActivity<MainActivity>()
            }

            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
                    .cornerRadius(18.dp)
                    .background(ColorProvider(bgColor))
                    .clickable(clickAction)
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    provider = ImageProvider(ListeningModeCycle.icon(mode)),
                    contentDescription = label,
                    colorFilter = ColorFilter.tint(ColorProvider(contentInk)),
                    modifier = GlanceModifier.size(22.dp),
                )
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    text = label,
                    style = TextStyle(
                        color = ColorProvider(contentInk),
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}
