package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.PodFormFactor
import com.example.model.PodIconStyle
import com.example.model.PodModel

/**
 * Draws a single left or right earbud or over-ear headset silhouette with crisp geometry.
 */
@Composable
fun SinglePodGlyph(
    style: PodIconStyle,
    isRight: Boolean,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    ventTint: Color = MaterialTheme.colorScheme.surfaceContainer,
    modifier: Modifier = Modifier.size(32.dp),
    contentDescription: String? = null,
) {
    val semanticsModifier = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else {
        modifier
    }

    Canvas(modifier = semanticsModifier) {
        val w = size.width
        val h = size.height

        when (style) {
            PodIconStyle.AIRPODS_PRO -> {
                withTransform({
                    if (isRight) {
                        scale(scaleX = -1f, scaleY = 1f, pivot = Offset(w / 2f, h / 2f))
                    }
                }) {
                    // Angled silicone ear tip (left side)
                    drawOval(
                        color = tint.copy(alpha = 0.82f),
                        topLeft = Offset(w * 0.14f, h * 0.18f),
                        size = Size(w * 0.24f, h * 0.34f)
                    )
                    // Main rounded pod head
                    drawOval(
                        color = tint,
                        topLeft = Offset(w * 0.24f, h * 0.16f),
                        size = Size(w * 0.54f, h * 0.42f)
                    )
                    // Short angled stem
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(w * 0.54f, h * 0.44f),
                        size = Size(w * 0.20f, h * 0.42f),
                        cornerRadius = CornerRadius(w * 0.10f, w * 0.10f)
                    )
                    // Black acoustic grille / sensor dot
                    drawRoundRect(
                        color = ventTint,
                        topLeft = Offset(w * 0.46f, h * 0.26f),
                        size = Size(w * 0.16f, h * 0.12f),
                        cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
                    )
                }
            }

            PodIconStyle.AIRPODS_GEN3 -> {
                withTransform({
                    if (isRight) {
                        scale(scaleX = -1f, scaleY = 1f, pivot = Offset(w / 2f, h / 2f))
                    }
                }) {
                    // Contoured ear head
                    drawOval(
                        color = tint,
                        topLeft = Offset(w * 0.20f, h * 0.16f),
                        size = Size(w * 0.56f, h * 0.40f)
                    )
                    // Short stem
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(w * 0.54f, h * 0.42f),
                        size = Size(w * 0.19f, h * 0.44f),
                        cornerRadius = CornerRadius(w * 0.095f, w * 0.095f)
                    )
                    // Acoustic mesh slot
                    drawOval(
                        color = ventTint,
                        topLeft = Offset(w * 0.30f, h * 0.26f),
                        size = Size(w * 0.14f, h * 0.18f)
                    )
                }
            }

            PodIconStyle.AIRPODS_CLASSIC -> {
                withTransform({
                    if (isRight) {
                        scale(scaleX = -1f, scaleY = 1f, pivot = Offset(w / 2f, h / 2f))
                    }
                }) {
                    // Classic rounded head
                    drawOval(
                        color = tint,
                        topLeft = Offset(w * 0.22f, h * 0.12f),
                        size = Size(w * 0.52f, h * 0.36f)
                    )
                    // Long slim stem
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(w * 0.55f, h * 0.36f),
                        size = Size(w * 0.17f, h * 0.54f),
                        cornerRadius = CornerRadius(w * 0.085f, w * 0.085f)
                    )
                    // Speaker dot
                    drawCircle(
                        color = ventTint,
                        radius = w * 0.07f,
                        center = Offset(w * 0.40f, h * 0.29f)
                    )
                }
            }

            PodIconStyle.BEATS_BUDS -> {
                withTransform({
                    if (isRight) {
                        scale(scaleX = -1f, scaleY = 1f, pivot = Offset(w / 2f, h / 2f))
                    }
                }) {
                    // Compact rounded bud body
                    drawOval(
                        color = tint,
                        topLeft = Offset(w * 0.18f, h * 0.22f),
                        size = Size(w * 0.64f, h * 0.56f)
                    )
                    // Subtle ear-tip protrusion
                    drawCircle(
                        color = tint.copy(alpha = 0.78f),
                        radius = w * 0.15f,
                        center = Offset(w * 0.24f, h * 0.42f)
                    )
                    // Center brand accent dot
                    drawCircle(
                        color = ventTint,
                        radius = w * 0.10f,
                        center = Offset(w * 0.54f, h * 0.50f)
                    )
                }
            }

            PodIconStyle.AIRPODS_MAX, PodIconStyle.BEATS_OVER_EAR -> {
                val strokeW = w * 0.10f
                // Canopy headband arc
                drawArc(
                    color = tint,
                    startAngle = 185f,
                    sweepAngle = 170f,
                    useCenter = false,
                    topLeft = Offset(w * 0.16f, h * 0.12f),
                    size = Size(w * 0.68f, h * 0.68f),
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
                // Left earcup
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.10f, h * 0.46f),
                    size = Size(w * 0.24f, h * 0.42f),
                    cornerRadius = CornerRadius(w * 0.11f, w * 0.11f)
                )
                // Right earcup
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.66f, h * 0.46f),
                    size = Size(w * 0.24f, h * 0.42f),
                    cornerRadius = CornerRadius(w * 0.11f, w * 0.11f)
                )
                // Subtle cushion inner lines
                drawRoundRect(
                    color = ventTint,
                    topLeft = Offset(w * 0.27f, h * 0.53f),
                    size = Size(w * 0.05f, h * 0.28f),
                    cornerRadius = CornerRadius(w * 0.025f, w * 0.025f)
                )
                drawRoundRect(
                    color = ventTint,
                    topLeft = Offset(w * 0.68f, h * 0.53f),
                    size = Size(w * 0.05f, h * 0.28f),
                    cornerRadius = CornerRadius(w * 0.025f, w * 0.025f)
                )
            }
        }
    }
}

/**
 * Header device badge showing either a mirrored Left+Right pair or an over-ear headset icon.
 */
@Composable
fun DeviceModelIcon(
    model: PodModel,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    ventTint: Color = MaterialTheme.colorScheme.surfaceContainer,
    iconSize: Dp = 32.dp,
    modifier: Modifier = Modifier,
) {
    if (model.formFactor == PodFormFactor.SINGLE_HEADSET) {
        Box(modifier = modifier.size(iconSize), contentAlignment = Alignment.Center) {
            SinglePodGlyph(
                style = model.iconStyle,
                isRight = false,
                tint = tint,
                ventTint = ventTint,
                modifier = Modifier.size(iconSize)
            )
        }
    } else {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SinglePodGlyph(
                style = model.iconStyle,
                isRight = false,
                tint = tint,
                ventTint = ventTint,
                modifier = Modifier.size(iconSize * 0.72f)
            )
            Spacer(modifier = Modifier.width(1.dp))
            SinglePodGlyph(
                style = model.iconStyle,
                isRight = true,
                tint = tint,
                ventTint = ventTint,
                modifier = Modifier.size(iconSize * 0.72f)
            )
        }
    }
}

/**
 * Crisp rounded AirPods/Beats charging case silhouette with lid seam and status LED dot.
 */
@Composable
fun ChargingCaseGlyph(
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    seamTint: Color = MaterialTheme.colorScheme.surface,
    ledColor: Color? = null,
    modifier: Modifier = Modifier.size(22.dp),
    contentDescription: String? = null,
) {
    val semanticsModifier = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else {
        modifier
    }

    Canvas(modifier = semanticsModifier) {
        val w = size.width
        val h = size.height

        // Outer case shell
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.08f, h * 0.16f),
            size = Size(w * 0.84f, h * 0.68f),
            cornerRadius = CornerRadius(w * 0.24f, w * 0.24f)
        )
        // Lid split line
        drawLine(
            color = seamTint,
            start = Offset(w * 0.08f, h * 0.40f),
            end = Offset(w * 0.92f, h * 0.40f),
            strokeWidth = h * 0.075f
        )
        // Center clasp notch
        drawRoundRect(
            color = seamTint,
            topLeft = Offset(w * 0.38f, h * 0.36f),
            size = Size(w * 0.24f, h * 0.09f),
            cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
        )
        // Status LED
        if (ledColor != null) {
            drawCircle(
                color = ledColor,
                radius = w * 0.07f,
                center = Offset(w * 0.50f, h * 0.58f)
            )
        }
    }
}

/**
 * 4-bar ascending Bluetooth signal strength glyph (RSSI indicator).
 */
@Composable
fun SignalBarsIcon(
    bars: Int,
    activeColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    inactiveColor: Color = MaterialTheme.colorScheme.outlineVariant,
    modifier: Modifier = Modifier.size(width = 16.dp, height = 13.dp),
    contentDescription: String? = null,
) {
    val semanticsModifier = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else {
        modifier
    }

    Canvas(modifier = semanticsModifier) {
        val barCount = 4
        val gap = size.width * 0.12f
        val barWidth = (size.width - gap * (barCount - 1)) / barCount
        for (i in 0 until barCount) {
            val fraction = (i + 1) / barCount.toFloat()
            val barHeight = size.height * (0.28f + 0.72f * fraction)
            val x = i * (barWidth + gap)
            val y = size.height - barHeight
            drawRoundRect(
                color = if (i < bars) activeColor else inactiveColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
