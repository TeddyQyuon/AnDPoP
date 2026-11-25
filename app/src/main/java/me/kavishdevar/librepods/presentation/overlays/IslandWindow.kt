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
import android.animation.PropertyValuesHolder
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AnticipateOvershootInterpolator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import me.kavishdevar.librepods.MainActivity
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.data.AirPodsNotifications
import me.kavishdevar.librepods.data.Battery
import me.kavishdevar.librepods.data.BatteryComponent
import me.kavishdevar.librepods.presentation.theme.AndpopType
import me.kavishdevar.librepods.presentation.theme.LocalAndpopPalette
import me.kavishdevar.librepods.presentation.theme.SavedAndpopTheme
import me.kavishdevar.librepods.services.ServiceManager
import kotlin.io.encoding.ExperimentalEncodingApi

enum class IslandType {
    CONNECTED,
    TAKING_OVER,
    MOVED_TO_REMOTE,
    MOVED_TO_OTHER_DEVICE,
}

class IslandWindow(private val context: Context) {
    private val windowManager: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var composeView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private var isClosing = false
    private var autoCloseHandler: Handler? = null
    private var autoCloseRunnable: Runnable? = null

    private var islandTitle by mutableStateOf("")
    private var islandSubtitle by mutableStateOf("")
    private var batteryLevel by mutableIntStateOf(0)
    private var hasBattery by mutableStateOf(false)
    private var showUndoAction by mutableStateOf(false)
    private var isMaxDevice by mutableStateOf(false)

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                AirPodsNotifications.BATTERY_DATA -> {
                    val batteryList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableArrayListExtra("data", Battery::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableArrayListExtra("data")
                    }
                    updateBatteryDisplay(batteryList)
                }
                AirPodsNotifications.DISCONNECT_RECEIVERS -> {
                    runCatching { context?.unregisterReceiver(this) }
                }
            }
        }
    }

    val isVisible: Boolean
        get() = composeView?.parent != null && composeView?.visibility == View.VISIBLE

    private fun updateBatteryDisplay(batteryList: ArrayList<Battery>?) {
        if (batteryList.isNullOrEmpty()) return
        val valid = batteryList.filter { it.hasValidReading }
        val headphones = valid.firstOrNull { it.component == BatteryComponent.HEADPHONES }
        if (headphones != null) {
            isMaxDevice = true
            batteryLevel = headphones.level
            hasBattery = true
            return
        }
        val budLevels = listOfNotNull(
            valid.firstOrNull { it.component == BatteryComponent.LEFT }?.level,
            valid.firstOrNull { it.component == BatteryComponent.RIGHT }?.level,
        )
        val lowest = budLevels.minOrNull()
        if (lowest != null) {
            batteryLevel = lowest
            hasBattery = true
        }
    }

    @SuppressLint("ClickableViewAccessibility", "UnspecifiedRegisterReceiverFlag")
    fun show(
        name: String,
        batteryPercentage: Int,
        context: Context,
        type: IslandType = IslandType.CONNECTED,
        reversed: Boolean = false,
        otherDeviceName: String? = null,
    ) {
        if (ServiceManager.getService()?.islandOpen == true) return
        ServiceManager.getService()?.islandOpen = true

        val service = ServiceManager.getService()
        isMaxDevice = service?.airpodsInstance?.model?.displayName?.startsWith("AirPods Max") == true

        islandTitle = name.ifBlank { context.getString(R.string.andpop_airpods_fallback) }
        islandSubtitle = when (type) {
            IslandType.CONNECTED -> context.getString(R.string.island_connected_text)
            IslandType.TAKING_OVER -> context.getString(R.string.andpop_island_taking_over)
            IslandType.MOVED_TO_REMOTE -> context.getString(R.string.andpop_island_moved_to, context.getString(R.string.andpop_island_other_device))
            IslandType.MOVED_TO_OTHER_DEVICE -> if (reversed) {
                context.getString(R.string.andpop_island_kept)
            } else {
                val target = otherDeviceName?.takeIf { it.isNotBlank() }
                    ?: context.getString(R.string.andpop_island_other_device)
                context.getString(R.string.andpop_island_moved_to, target)
            }
        }
        showUndoAction = type == IslandType.MOVED_TO_OTHER_DEVICE && !reversed

        batteryLevel = batteryPercentage.coerceIn(0, 100)
        hasBattery = batteryPercentage in 1..100
        updateBatteryDisplay(service?.getBattery()?.let { ArrayList(it) })

        val filter = IntentFilter(AirPodsNotifications.BATTERY_DATA).apply {
            addAction(AirPodsNotifications.DISCONNECT_RECEIVERS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(batteryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(batteryReceiver, filter)
        }
        service?.sendBatteryBroadcast()

        val owner = OverlayLifecycleOwner().also { it.onCreate() }
        lifecycleOwner = owner

        var initialY = 0f
        val view = ComposeView(context).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SavedAndpopTheme {
                    IslandCapsule(
                        title = islandTitle,
                        subtitle = islandSubtitle,
                        batteryLevel = if (hasBattery) batteryLevel else null,
                        showUndo = showUndoAction,
                        isMax = isMaxDevice,
                        onOpenApp = {
                            val launch = Intent(context, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            }
                            context.startActivity(launch)
                            close()
                        },
                        onUndo = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                ServiceManager.getService()?.takeOver("reverse")
                            }
                            close()
                        },
                    )
                }
            }
            setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        initialY = event.rawY
                        false
                    }
                    MotionEvent.ACTION_UP -> {
                        if (event.rawY - initialY < -60f) {
                            close()
                            true
                        } else {
                            v.performClick()
                            false
                        }
                    }
                    else -> false
                }
            }
        }
        composeView = view

        val displayMetrics = Resources.getSystem().displayMetrics
        val widthPx = (displayMetrics.widthPixels * 0.94f).toInt().coerceAtMost((440 * displayMetrics.density).toInt())
        val params = WindowManager.LayoutParams(
            widthPx,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = (12 * displayMetrics.density).toInt()
        }

        try {
            windowManager.addView(view, params)
        } catch (e: Exception) {
            Log.e("IslandWindow", "Error adding island overlay: ${e.message}")
            cleanupAndRemoveView()
            return
        }

        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.72f, 1f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.72f, 1f)
        val translationY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, -120f, 0f)
        ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY, translationY).apply {
            duration = 380
            interpolator = AnticipateOvershootInterpolator(1.0f)
            start()
        }

        autoCloseHandler = Handler(Looper.getMainLooper())
        autoCloseRunnable = Runnable { close() }.also {
            autoCloseHandler?.postDelayed(it, 4500)
        }
    }

    fun close() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Handler(Looper.getMainLooper()).post { close() }
            return
        }
        val view = composeView ?: run {
            ServiceManager.getService()?.islandOpen = false
            return
        }
        if (isClosing) return
        isClosing = true

        runCatching { context.unregisterReceiver(batteryReceiver) }
        ServiceManager.getService()?.islandOpen = false
        autoCloseRunnable?.let { autoCloseHandler?.removeCallbacks(it) }

        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, view.scaleX, 0.7f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, view.scaleY, 0.7f)
        val translationY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, view.translationY, -140f)
        ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY, translationY).apply {
            duration = 240
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    cleanupAndRemoveView()
                }
            })
            start()
        }
    }

    private fun cleanupAndRemoveView() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Handler(Looper.getMainLooper()).post { cleanupAndRemoveView() }
            return
        }
        composeView?.let { view ->
            runCatching { view.visibility = View.GONE }
            runCatching { if (view.parent != null) windowManager.removeView(view) }
        }
        composeView = null
        lifecycleOwner?.onDestroy()
        lifecycleOwner = null
        ServiceManager.getService()?.islandOpen = false
        isClosing = false
    }

    fun forceClose() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Handler(Looper.getMainLooper()).post { forceClose() }
            return
        }
        if (isClosing) return
        isClosing = true
        runCatching { context.unregisterReceiver(batteryReceiver) }
        autoCloseRunnable?.let { autoCloseHandler?.removeCallbacks(it) }
        cleanupAndRemoveView()
    }
}

@Composable
private fun IslandCapsule(
    title: String,
    subtitle: String,
    batteryLevel: Int?,
    showUndo: Boolean,
    isMax: Boolean,
    onOpenApp: () -> Unit,
    onUndo: () -> Unit,
) {
    val palette = LocalAndpopPalette.current
    val shape = RoundedCornerShape(32.dp)
    Row(
        modifier = Modifier
            .widthIn(max = 440.dp)
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .clip(shape)
            .background(palette.s1)
            .border(1.dp, palette.on.copy(alpha = 0.10f), shape)
            .clickable(onClick = onOpenApp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(palette.s2),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(if (isMax) R.drawable.andpop_max_photo else R.drawable.amber_pro_2_case),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                contentScale = ContentScale.Fit,
            )
        }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                color = palette.on,
                style = AndpopType.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                color = palette.on2,
                style = AndpopType.supporting,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (showUndo) {
            FilledTonalButton(
                onClick = onUndo,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = palette.pc,
                    contentColor = palette.onPc,
                ),
            ) {
                Text(stringResource(R.string.andpop_undo), style = AndpopType.label)
            }
        } else {
            val level = batteryLevel ?: 0
            val ringColor = when {
                batteryLevel == null -> palette.on3
                level <= 20 -> palette.warning
                else -> palette.charge
            }
            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(40.dp)) {
                    val strokeWidth = 3.5.dp.toPx()
                    drawCircle(
                        color = palette.s3,
                        style = Stroke(width = strokeWidth),
                    )
                    if (batteryLevel != null) {
                        drawArc(
                            color = ringColor,
                            startAngle = -90f,
                            sweepAngle = (level / 100f * 360f).coerceAtLeast(8f),
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        )
                    }
                }
                Text(
                    text = if (batteryLevel != null) "$batteryLevel" else stringResource(R.string.andpop_battery_empty),
                    color = palette.on,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
