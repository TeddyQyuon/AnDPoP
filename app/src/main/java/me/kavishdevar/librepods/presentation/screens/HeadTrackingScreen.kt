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

package me.kavishdevar.librepods.presentation.screens

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import me.kavishdevar.librepods.BuildConfig
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.components.AndpopCard
import me.kavishdevar.librepods.presentation.components.AndpopDetailPage
import me.kavishdevar.librepods.presentation.components.AndpopGroup
import me.kavishdevar.librepods.presentation.components.AndpopSectionHeader
import me.kavishdevar.librepods.presentation.components.AndpopSwitchRow
import me.kavishdevar.librepods.presentation.components.AndpopTonalButton
import me.kavishdevar.librepods.presentation.theme.AndpopIcons
import me.kavishdevar.librepods.presentation.theme.AndpopType
import me.kavishdevar.librepods.presentation.theme.LocalAndpopPalette
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel
import me.kavishdevar.librepods.services.ServiceManager
import me.kavishdevar.librepods.utils.HeadTracking
import kotlin.math.abs

/** Result of the head-gesture test shown under the head. */
internal enum class HeadTestState { Idle, Listening, Yes, No, NoGesture }

private const val HEAD_TEST_TIMEOUT_MS = 10_000L

@Composable
fun HeadTrackingScreen(viewModel: AirPodsViewModel) {
    val state by viewModel.uiState.collectAsState()
    val palette = LocalAndpopPalette.current
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val showPlot = remember {
        !BuildConfig.PLAY_BUILD &&
            context.getSharedPreferences("settings", Context.MODE_PRIVATE).getBoolean("dev_show_head_velocity_plot", false)
    }
    var test by remember { mutableStateOf(HeadTestState.Idle) }
    val nod = remember { Animatable(0f) }
    val shake = remember { Animatable(0f) }

    // Head orientation only streams while head tracking is on, so it runs while this screen is open.
    DisposableEffect(Unit) {
        viewModel.startHeadTracking()
        onDispose { viewModel.stopHeadTracking() }
    }

    LaunchedEffect(test) {
        when (test) {
            HeadTestState.Yes -> nod.animateTo(0f, keyframes { durationMillis = 700; 12f at 150; -6f at 320; 10f at 480; 0f at 700 })
            HeadTestState.No -> shake.animateTo(0f, keyframes { durationMillis = 700; -14f at 140; 14f at 300; -10f at 460; 0f at 700 })
            else -> Unit
        }
    }

    fun runTest() {
        test = HeadTestState.Listening
        scope.launch {
            val accepted: Boolean? = if (state.isDemoMode) {
                delay(1500)
                true
            } else {
                withTimeoutOrNull(HEAD_TEST_TIMEOUT_MS) { ServiceManager.getService()?.testHeadGestures() }
            }
            test = when (accepted) {
                true -> HeadTestState.Yes
                false -> HeadTestState.No
                null -> HeadTestState.NoGesture
            }
            haptics.performHapticFeedback(if (accepted == null) HapticFeedbackType.Reject else HapticFeedbackType.Confirm)
        }
    }

    AndpopDetailPage(title = stringResource(R.string.head_gestures)) {
        AndpopGroup(Modifier.padding(top = 16.dp)) {
            row { shape ->
                AndpopSwitchRow(
                    shape = shape,
                    title = stringResource(R.string.head_gestures),
                    supporting = stringResource(R.string.andpop_head_gestures_desc),
                    icon = AndpopIcons.Head,
                    iconChip = true,
                    checked = state.headGesturesEnabled,
                    onCheckedChange = viewModel::setHeadGesturesEnabled,
                )
            }
        }

        AndpopSectionHeader(stringResource(R.string.andpop_try_it))
        val detected = test == HeadTestState.Yes || test == HeadTestState.No
        val headColor by animateColorAsState(if (detected) palette.success else palette.pri, label = "Head color")
        AndpopCard {
            Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                Icon(
                    AndpopIcons.Head,
                    contentDescription = null,
                    modifier = Modifier.size(88.dp).graphicsLayer {
                        rotationX = nod.value
                        translationY = nod.value * 0.6f
                        rotationZ = shake.value * 0.4f
                        translationX = shake.value
                    },
                    tint = headColor,
                )
            }
        }
        val message = when (test) {
            HeadTestState.Idle -> R.string.andpop_head_idle
            HeadTestState.Listening -> R.string.andpop_head_listening
            HeadTestState.Yes -> R.string.andpop_head_yes
            HeadTestState.No -> R.string.andpop_head_no
            HeadTestState.NoGesture -> R.string.andpop_head_none
        }
        Text(
            stringResource(message),
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp).semantics { liveRegion = LiveRegionMode.Polite },
            color = when (test) {
                HeadTestState.Yes, HeadTestState.No -> palette.success
                HeadTestState.NoGesture -> palette.warning
                else -> palette.on2
            },
            style = AndpopType.body,
            textAlign = TextAlign.Center,
        )
        if (test == HeadTestState.Listening) {
            Box(Modifier.fillMaxWidth().padding(bottom = 8.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(24.dp), color = palette.pri, strokeWidth = 2.5.dp)
            }
        }
        AndpopTonalButton(
            text = stringResource(if (detected || test == HeadTestState.NoGesture) R.string.andpop_head_test_again else R.string.andpop_head_test),
            onClick = ::runTest,
            enabled = test != HeadTestState.Listening && state.headGesturesEnabled,
            icon = AndpopIcons.Head,
        )

        if (showPlot) {
            AndpopSectionHeader(stringResource(R.string.andpop_head_velocity))
            VelocityPlot()
        }
    }
}

/** Raw head velocity, shown only when the developer option is on. */
@Composable
private fun VelocityPlot() {
    val palette = LocalAndpopPalette.current
    val acceleration by HeadTracking.acceleration.collectAsState()
    val maxPoints = 100
    val points = remember { mutableStateListOf<Pair<Float, Float>>() }
    var maxAbs by remember { mutableFloatStateOf(1000f) }

    LaunchedEffect(acceleration) {
        points.add(acceleration.horizontal to acceleration.vertical)
        if (points.size > maxPoints) points.removeAt(0)
        maxAbs = maxOf(points.maxOf { maxOf(abs(it.first), abs(it.second)) } * 1.2f, 1000f)
    }

    val horizontal = palette.pri
    val vertical = palette.warning
    val grid = palette.line
    AndpopCard(Modifier.clip(RoundedCornerShape(24.dp))) {
        Canvas(Modifier.fillMaxWidth().height(220.dp).padding(16.dp)) {
            val xScale = size.width / maxPoints
            val yScale = size.height / (maxAbs * 2)
            val zeroY = size.height / 2
            for (i in 0..maxPoints step 10) {
                drawLine(grid, Offset(i * xScale, 0f), Offset(i * xScale, size.height), 1.dp.toPx())
            }
            drawLine(grid, Offset(0f, zeroY), Offset(size.width, zeroY), 1.5.dp.toPx())
            for (i in 0 until points.size - 1) {
                val x1 = i * xScale
                val x2 = (i + 1) * xScale
                drawLine(horizontal, Offset(x1, zeroY - points[i].first * yScale), Offset(x2, zeroY - points[i + 1].first * yScale), 2.dp.toPx())
                drawLine(vertical, Offset(x1, zeroY - points[i].second * yScale), Offset(x2, zeroY - points[i + 1].second * yScale), 2.dp.toPx())
            }
        }
    }
}
