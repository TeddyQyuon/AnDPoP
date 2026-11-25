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

package me.kavishdevar.librepods.presentation.overlays

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import me.kavishdevar.librepods.MainActivity
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.data.AirPodsNotifications
import me.kavishdevar.librepods.data.Battery
import me.kavishdevar.librepods.data.BatteryComponent
import me.kavishdevar.librepods.presentation.components.AndpopTonalButton
import me.kavishdevar.librepods.presentation.theme.AndpopIcons
import me.kavishdevar.librepods.presentation.theme.AndpopType
import me.kavishdevar.librepods.presentation.theme.LocalAndpopPalette
import me.kavishdevar.librepods.presentation.theme.SavedAndpopTheme
import kotlin.io.encoding.ExperimentalEncodingApi

@SuppressLint("ClickableViewAccessibility")
class PopupWindow(
    private val context: Context,
    private val onCloseCallback: () -> Unit = {},
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val autoCloseHandler = Handler(Looper.getMainLooper())
    private var autoCloseRunnable: Runnable? = null
    private var batteryUpdateReceiver: BroadcastReceiver? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private var composeView: ComposeView? = null
    private var isClosing = false

    private var popupTitle by mutableStateOf("")
    private var batteryListState by mutableStateOf<List<Battery>>(emptyList())

    @Suppress("DEPRECATION")
    private val windowParams = WindowManager.LayoutParams().apply {
        height = WindowManager.LayoutParams.WRAP_CONTENT
        val displayMetrics = context.resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
        width = if (screenWidthDp >= 600) {
            (420 * displayMetrics.density).toInt()
        } else {
            WindowManager.LayoutParams.MATCH_PARENT
        }
        type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        format = PixelFormat.TRANSLUCENT
        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        dimAmount = 0.36f
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_DIM_BEHIND or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
    }

    @SuppressLint("InlinedApi")
    fun open(name: String = "AirPods Pro", batteryNotification: AirPodsNotifications.BatteryNotification) {
        try {
            if (composeView != null || isClosing) return
            popupTitle = name
            batteryListState = batteryNotification.getBattery()

            val owner = OverlayLifecycleOwner().also { it.onCreate() }
            lifecycleOwner = owner

            val view = ComposeView(context).apply {
                setViewTreeLifecycleOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    SavedAndpopTheme {
                        ConnectionPopupCard(
                            name = popupTitle,
                            batteries = batteryListState,
                            onOpenApp = {
                                val launchIntent = Intent(context, MainActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                }
                                context.startActivity(launchIntent)
                                close()
                            },
                            onClose = { close() },
                        )
                    }
                }
                setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_OUTSIDE) {
                        close()
                        true
                    } else {
                        false
                    }
                }
            }
            composeView = view
            windowManager.addView(view, windowParams)

            val screenHeight = context.resources.displayMetrics.heightPixels.toFloat()
            view.translationY = screenHeight
            ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, screenHeight, 0f).apply {
                duration = 320
                interpolator = DecelerateInterpolator()
                start()
            }

            registerBatteryUpdateReceiver()
            autoCloseRunnable = Runnable { close() }.also {
                autoCloseHandler.postDelayed(it, 12000)
            }
        } catch (e: Exception) {
            Log.e("PopupWindow", "Error opening popup: ${e.message}")
            cleanupNow()
            onCloseCallback()
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerBatteryUpdateReceiver() {
        batteryUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == AirPodsNotifications.BATTERY_DATA) {
                    val batteryList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableArrayListExtra("data", Battery::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableArrayListExtra("data")
                    }
                    if (batteryList != null) {
                        batteryListState = batteryList
                    }
                }
            }
        }
        val filter = IntentFilter(AirPodsNotifications.BATTERY_DATA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(batteryUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(batteryUpdateReceiver, filter)
        }
    }

    fun updateBatteryStatus(batteryNotification: AirPodsNotifications.BatteryNotification) {
        batteryListState = batteryNotification.getBattery()
    }

    fun close() {
        val view = composeView ?: return
        if (isClosing) return
        isClosing = true
        autoCloseRunnable?.let { autoCloseHandler.removeCallbacks(it) }
        unregisterReceiverSafe()

        val targetY = view.height.takeIf { it > 0 }?.toFloat() ?: 900f
        ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, targetY).apply {
            duration = 260
            interpolator = AccelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    cleanupNow()
                    onCloseCallback()
                }
            })
            start()
        }
    }

    private fun unregisterReceiverSafe() {
        batteryUpdateReceiver?.let {
            runCatching { context.unregisterReceiver(it) }
            batteryUpdateReceiver = null
        }
    }

    private fun cleanupNow() {
        autoCloseRunnable?.let { autoCloseHandler.removeCallbacks(it) }
        unregisterReceiverSafe()
        composeView?.let { view ->
            runCatching { if (view.parent != null) windowManager.removeView(view) }
        }
        composeView = null
        lifecycleOwner?.onDestroy()
        lifecycleOwner = null
        isClosing = false
    }
}

@Composable
private fun ConnectionPopupCard(
    name: String,
    batteries: List<Battery>,
    onOpenApp: () -> Unit,
    onClose: () -> Unit,
) {
    val palette = LocalAndpopPalette.current
    val valid = batteries.filter { it.hasValidReading }
    val headphones = valid.firstOrNull { it.component == BatteryComponent.HEADPHONES }
    val left = valid.firstOrNull { it.component == BatteryComponent.LEFT }
    val right = valid.firstOrNull { it.component == BatteryComponent.RIGHT }
    val caseItem = valid.firstOrNull { it.component == BatteryComponent.CASE }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(palette.s1)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(36.dp, 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(palette.on3.copy(alpha = 0.55f)),
            )

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = name.ifBlank { stringResource(R.string.andpop_airpods_fallback) },
                        color = palette.on,
                        style = AndpopType.title,
                    )
                    Text(
                        text = stringResource(R.string.andpop_popup_connected_phone),
                        color = palette.on2,
                        style = AndpopType.bodySmall,
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(palette.s2),
                ) {
                    Icon(
                        imageVector = AndpopIcons.Close,
                        contentDescription = stringResource(R.string.andpop_account_close),
                        tint = palette.on,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            if (headphones != null) {
                PopupBatteryCell(
                    artRes = R.drawable.andpop_max_photo,
                    label = stringResource(R.string.andpop_widget_headphones),
                    battery = headphones,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PopupBatteryCell(
                        artRes = R.drawable.amber_pro_2_left,
                        label = stringResource(R.string.andpop_left_bud),
                        battery = left,
                        modifier = Modifier.weight(1f),
                    )
                    Box(Modifier.width(1.dp).fillMaxHeight(0.75f).background(palette.line))
                    PopupBatteryCell(
                        artRes = R.drawable.amber_pro_2_case,
                        label = stringResource(R.string.andpop_case),
                        battery = caseItem,
                        modifier = Modifier.weight(1f),
                    )
                    Box(Modifier.width(1.dp).fillMaxHeight(0.75f).background(palette.line))
                    PopupBatteryCell(
                        artRes = R.drawable.amber_pro_2_right,
                        label = stringResource(R.string.andpop_right_bud),
                        battery = right,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            AndpopTonalButton(
                text = stringResource(R.string.andpop_popup_open_app),
                onClick = onOpenApp,
            )
        }
    }
}

@Composable
private fun PopupBatteryCell(
    artRes: Int,
    label: String,
    battery: Battery?,
    modifier: Modifier = Modifier,
) {
    val palette = LocalAndpopPalette.current
    val level = battery?.level
    val charging = battery?.isCharging == true && level != null

    Column(
        modifier = modifier.padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Image(
            painter = painterResource(artRes),
            contentDescription = null,
            modifier = Modifier.height(68.dp),
            contentScale = ContentScale.Fit,
        )
        Text(
            text = if (level != null) {
                stringResource(R.string.andpop_percent_value, level)
            } else {
                stringResource(R.string.andpop_battery_empty)
            },
            color = if (level != null) palette.on else palette.on3,
            style = AndpopType.battery,
        )
        Text(
            text = label,
            color = palette.on2,
            style = AndpopType.caption,
        )
        if (level != null) {
            PopupBatteryBar(level = level, charging = charging)
        } else {
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun PopupBatteryBar(level: Int, charging: Boolean) {
    val palette = LocalAndpopPalette.current
    val fillColor = when {
        charging -> palette.charge
        level <= 20 -> palette.warning
        else -> palette.on
    }
    val shellColor = palette.on2.copy(alpha = 0.8f)

    Canvas(Modifier.size(34.dp, 16.dp)) {
        withTransform({ scale(size.width / 36f, size.height / 18f, Offset.Zero) }) {
            drawRoundRect(shellColor, Offset(1f, 1.5f), Size(29.5f, 15f), CornerRadius(4.6f), style = Stroke(1.5f))
            drawRoundRect(shellColor, Offset(32f, 6f), Size(2.6f, 6f), CornerRadius(1.2f))
            drawRoundRect(fillColor, Offset(3.5f, 4f), Size((level.coerceIn(0, 100) / 100f * 25f).coerceAtLeast(2f), 10f), CornerRadius(2.6f))
            if (charging) {
                val bolt = Path().apply {
                    moveTo(19.6f, 1.6f)
                    lineTo(12.4f, 10.4f)
                    lineTo(17.0f, 10.4f)
                    lineTo(15.1f, 17.2f)
                    lineTo(22.7f, 7.8f)
                    lineTo(18.0f, 7.8f)
                    close()
                }
                drawPath(bolt, palette.bolt)
                drawPath(bolt, palette.boltEdge, style = Stroke(1f, join = StrokeJoin.Round))
            }
        }
    }
}
