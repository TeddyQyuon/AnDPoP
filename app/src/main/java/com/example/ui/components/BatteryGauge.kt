package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.PodIconStyle
import com.example.model.PodModel
import com.example.model.estimateChargeTimeRemaining
import com.example.model.estimateListeningTime
import com.example.ui.theme.BatteryLowRed
import com.example.ui.theme.BatteryMidAmber
import com.example.ui.theme.CapodMint

@Composable
fun batteryAccentColor(batteryPercent: Int?): Color {
    return when {
        batteryPercent == null -> MaterialTheme.colorScheme.outlineVariant
        batteryPercent <= 15 -> BatteryLowRed
        batteryPercent <= 30 -> BatteryMidAmber
        else -> MaterialTheme.colorScheme.primary
    }
}

/**
 * CAPod-style circular battery ring with centered earbud silhouette, tabular percentage + time remaining,
 * and warm amber charging pill badge when charging.
 */
@Composable
fun CircularPodBatteryGauge(
    batteryPercent: Int?,
    isCharging: Boolean,
    isInEar: Boolean,
    isRight: Boolean,
    model: PodModel,
    ringSize: Dp = 74.dp,
    modifier: Modifier = Modifier,
) {
    val targetFraction = (batteryPercent?.coerceIn(0, 100) ?: 0) / 100f
    val animatedSweep by animateFloatAsState(
        targetValue = targetFraction * 360f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "pod_battery_sweep"
    )

    val ringColor = batteryAccentColor(batteryPercent)
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f)
    val podTint = if (batteryPercent != null) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(ringSize),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(ringSize)) {
                val strokeWidth = 5.5.dp.toPx()
                val inset = strokeWidth / 2f
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = Offset(inset, inset)

                // Track circle
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Active battery progress arc starting at 12 o'clock (-90°)
                if (animatedSweep > 0.5f) {
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = animatedSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }

            // Inner soft circular well for subtle depth
            Box(
                modifier = Modifier
                    .size(ringSize - 16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                SinglePodGlyph(
                    style = model.iconStyle,
                    isRight = isRight,
                    tint = podTint,
                    ventTint = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.size(ringSize * 0.44f),
                    contentDescription = if (isRight) {
                        stringResource(R.string.cd_pod_right)
                    } else {
                        stringResource(R.string.cd_pod_left)
                    }
                )
            }

            // Crisp L / R side badge anchored at bottom of the gauge ring
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter),
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 1.dp,
            ) {
                Text(
                    text = if (isRight) "R" else "L",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Percentage + Estimated Listening Time
        val listeningEstimate = estimateListeningTime(batteryPercent, model)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = batteryPercent?.let { "$it%" } ?: stringResource(R.string.label_battery_unknown),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum"
                ),
                color = if (batteryPercent != null) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            if (listeningEstimate != null) {
                Text(
                    text = " · $listeningEstimate",
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Status pill: Charging (warm amber) or In-Ear (subtle mint/primary)
        when {
            isCharging -> {
                val chargeRemaining = estimateChargeTimeRemaining(batteryPercent, model)
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = stringResource(R.string.cd_charging),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (chargeRemaining != null) {
                                "${stringResource(R.string.status_charging_badge)} · $chargeRemaining"
                            } else {
                                stringResource(R.string.status_charging_badge)
                            },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFeatureSettings = "tnum"
                            ),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            isInEar -> {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Hearing,
                            contentDescription = stringResource(R.string.cd_in_ear),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = stringResource(R.string.status_in_ear),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            else -> {
                // Keep vertical balance when neither charging nor in-ear
                Spacer(modifier = Modifier.height(22.dp))
            }
        }
    }
}

/**
 * Compact circular battery ring for single-unit headsets (AirPods Max, Beats Studio/Solo Pro).
 */
@Composable
fun CompactHeadsetBatteryRow(
    batteryPercent: Int?,
    isCharging: Boolean,
    model: PodModel,
    modifier: Modifier = Modifier,
) {
    val targetFraction = (batteryPercent?.coerceIn(0, 100) ?: 0) / 100f
    val animatedSweep by animateFloatAsState(
        targetValue = targetFraction * 360f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "headset_battery_sweep"
    )
    val ringColor = batteryAccentColor(batteryPercent)
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.75f)
    val listeningEstimate = estimateListeningTime(batteryPercent, model)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Canvas(modifier = Modifier.size(28.dp)) {
            val strokeW = 3.8.dp.toPx()
            val inset = strokeW / 2f
            val arcSize = Size(size.width - strokeW, size.height - strokeW)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
            if (animatedSweep > 0.5f) {
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = animatedSweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = batteryPercent?.let { "$it%" } ?: stringResource(R.string.label_battery_unknown),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = "tnum"
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        if (listeningEstimate != null) {
            Text(
                text = " · $listeningEstimate",
                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isCharging) {
            Spacer(modifier = Modifier.width(10.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.tertiaryContainer,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = stringResource(R.string.cd_charging),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = stringResource(R.string.status_charging_badge),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}

/**
 * Horizontal rounded case battery progress bar matching CAPod's bottom case strip.
 */
@Composable
fun CaseBatteryBar(
    caseBatteryPercent: Int?,
    isCharging: Boolean,
    modifier: Modifier = Modifier,
) {
    val targetFraction = (caseBatteryPercent?.coerceIn(0, 100) ?: 0) / 100f
    val animatedFill by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "case_battery_bar"
    )
    val barColor = batteryAccentColor(caseBatteryPercent)
    val ledColor = when {
        isCharging -> BatteryMidAmber
        (caseBatteryPercent ?: 0) >= 80 -> CapodMint
        else -> null
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChargingCaseGlyph(
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            seamTint = MaterialTheme.colorScheme.surfaceContainerLow,
            ledColor = ledColor,
            modifier = Modifier.size(22.dp),
            contentDescription = stringResource(R.string.cd_pod_case)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = caseBatteryPercent?.let { "$it%" } ?: stringResource(R.string.label_battery_unknown),
            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
            color = if (caseBatteryPercent != null) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )

        if (isCharging) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Rounded.Bolt,
                contentDescription = stringResource(R.string.cd_charging),
                tint = BatteryMidAmber,
                modifier = Modifier.size(15.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Rounded linear track + fill
        Box(
            modifier = Modifier
                .weight(1f)
                .height(7.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f))
        ) {
            if (animatedFill > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedFill)
                        .height(7.dp)
                        .clip(RoundedCornerShape(50))
                        .background(barColor)
                )
            }
        }
    }
}
