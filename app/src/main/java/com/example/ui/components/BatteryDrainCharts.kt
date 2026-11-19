package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.BatteryLogEntity
import com.example.model.AnalyticsTimeRange
import com.example.model.DrainRateBucket
import com.example.model.PodFormFactor
import com.example.ui.theme.BatteryLowRed
import com.example.ui.theme.BatteryMidAmber
import com.example.ui.theme.CapodMint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Interactive Compose Canvas multi-series battery drain area & line chart supporting:
 * - Smooth cubic Bézier curves with translucent area gradients
 * - Series visibility toggles (Left / Right / Case or Headset)
 * - Dashed 0/25/50/75/100% gridlines + 20% low-battery reference line
 * - Tap & horizontal drag scrubbing with vertical crosshair and pinned readout tooltip
 */
@Composable
fun InteractiveBatteryDrainChart(
    logs: List<BatteryLogEntity>,
    formFactor: PodFormFactor,
    timeRange: AnalyticsTimeRange,
    modifier: Modifier = Modifier,
) {
    val ordered = remember(logs) { logs.sortedBy { it.timestampMillis } }
    var showLeft by remember { mutableStateOf(true) }
    var showRight by remember { mutableStateOf(true) }
    var showCase by remember { mutableStateOf(true) }
    var scrubbedIndex by remember(ordered) { mutableStateOf<Int?>(null) }

    val leftColor = MaterialTheme.colorScheme.primary
    val rightColor = CapodMint
    val caseColor = BatteryMidAmber
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val crosshairColor = MaterialTheme.colorScheme.onSurfaceVariant

    val axisFormat = remember(timeRange) {
        if (timeRange == AnalyticsTimeRange.SEVEN_DAYS) {
            SimpleDateFormat("EEE", Locale.getDefault())
        } else {
            SimpleDateFormat("HH:mm", Locale.getDefault())
        }
    }
    val tooltipFormat = remember {
        SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    }

    val activeSample = scrubbedIndex?.let { ordered.getOrNull(it) } ?: ordered.lastOrNull()

    Column(modifier = modifier.fillMaxWidth()) {
        // Interactive legend & series toggle chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (formFactor == PodFormFactor.SINGLE_HEADSET) {
                SeriesTogglePill(
                    label = stringResource(R.string.cd_headset),
                    color = leftColor,
                    selected = showLeft,
                    onClick = { showLeft = !showLeft },
                    tag = "toggle_series_headset"
                )
            } else {
                SeriesTogglePill(
                    label = stringResource(R.string.label_left_pod),
                    color = leftColor,
                    selected = showLeft,
                    onClick = { showLeft = !showLeft },
                    tag = "toggle_series_left"
                )
                SeriesTogglePill(
                    label = stringResource(R.string.label_right_pod),
                    color = rightColor,
                    selected = showRight,
                    onClick = { showRight = !showRight },
                    tag = "toggle_series_right"
                )
                SeriesTogglePill(
                    label = stringResource(R.string.label_case),
                    color = caseColor,
                    selected = showCase,
                    onClick = { showCase = !showCase },
                    tag = "toggle_series_case"
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Live / Scrubbed Sample Readout Strip
        if (activeSample != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (scrubbedIndex != null) {
                                stringResource(R.string.analytics_selected_point)
                            } else {
                                stringResource(R.string.analytics_latest_reading)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = tooltipFormat.format(Date(activeSample.timestampMillis)),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (formFactor == PodFormFactor.SINGLE_HEADSET) {
                            ReadoutValue(
                                label = "H",
                                value = activeSample.headsetBattery?.let { "$it%" } ?: "--",
                                color = leftColor
                            )
                        } else {
                            if (showLeft) {
                                ReadoutValue(
                                    label = "L",
                                    value = activeSample.leftBattery?.let { "$it%" } ?: "--",
                                    color = leftColor
                                )
                            }
                            if (showRight) {
                                ReadoutValue(
                                    label = "R",
                                    value = activeSample.rightBattery?.let { "$it%" } ?: "--",
                                    color = rightColor
                                )
                            }
                            if (showCase) {
                                ReadoutValue(
                                    label = "C",
                                    value = activeSample.caseBattery?.let { "$it%" } ?: "--",
                                    color = caseColor
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (ordered.size < 2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.analytics_waiting_title),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.analytics_waiting_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            return
        }

        val minTime = ordered.first().timestampMillis
        val maxTime = ordered.last().timestampMillis
        val timeSpan = (maxTime - minTime).coerceAtLeast(1L).toFloat()

        // Main Canvas + Y-Axis container
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(216.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(start = 10.dp, end = 14.dp, top = 14.dp, bottom = 14.dp)
        ) {
            // Y-Axis Labels (100% down to 0%)
            Column(
                modifier = Modifier
                    .height(188.dp)
                    .width(34.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                listOf("100%", "75%", "50%", "25%", "0%").forEach { tick ->
                    Text(
                        text = tick,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Interactive Chart Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(188.dp)
                    .testTag("battery_drain_canvas")
                    .pointerInput(ordered) {
                        fun selectForX(touchX: Float) {
                            val w = size.width.toFloat().coerceAtLeast(1f)
                            val fraction = (touchX / w).coerceIn(0f, 1f)
                            val targetTime = minTime + (fraction * timeSpan).toLong()
                            val nearestIdx = ordered.indices.minByOrNull { idx ->
                                abs(ordered[idx].timestampMillis - targetTime)
                            }
                            scrubbedIndex = nearestIdx
                        }

                        detectTapGestures(
                            onTap = { offset -> selectForX(offset.x) },
                            onDoubleTap = { scrubbedIndex = null }
                        )
                    }
                    .pointerInput(ordered) {
                        detectHorizontalDragGestures(
                            onDragEnd = { /* keep selected point visible until tapped/dragged */ },
                            onHorizontalDrag = { change, _ ->
                                val w = size.width.toFloat().coerceAtLeast(1f)
                                val fraction = (change.position.x / w).coerceIn(0f, 1f)
                                val targetTime = minTime + (fraction * timeSpan).toLong()
                                val nearestIdx = ordered.indices.minByOrNull { idx ->
                                    abs(ordered[idx].timestampMillis - targetTime)
                                }
                                scrubbedIndex = nearestIdx
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

                    // Horizontal gridlines (0, 25, 50, 75, 100%)
                    for (pct in listOf(0f, 25f, 50f, 75f, 100f)) {
                        val y = h - (pct / 100f) * h
                        drawLine(
                            color = gridColor.copy(alpha = 0.65f),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = if (pct == 0f || pct == 100f) null else dashEffect
                        )
                    }

                    // Subtle 20% low-battery warning threshold line
                    val lowY = h - 0.20f * h
                    drawLine(
                        color = BatteryLowRed.copy(alpha = 0.35f),
                        start = Offset(0f, lowY),
                        end = Offset(w, lowY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 6f), 0f)
                    )

                    fun drawSmoothSeries(
                        color: Color,
                        fillAlphaTop: Float,
                        selector: (BatteryLogEntity) -> Int?,
                    ) {
                        val points = ordered.mapNotNull { entry ->
                            val value = selector(entry) ?: return@mapNotNull null
                            val x = ((entry.timestampMillis - minTime) / timeSpan) * w
                            val y = h - (value.coerceIn(0, 100) / 100f) * h
                            Offset(x, y)
                        }
                        if (points.size < 2) return

                        val linePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 0 until points.size - 1) {
                                val p0 = points[i]
                                val p1 = points[i + 1]
                                val midX = (p0.x + p1.x) / 2f
                                cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                            }
                        }

                        val areaPath = Path().apply {
                            addPath(linePath)
                            lineTo(points.last().x, h)
                            lineTo(points.first().x, h)
                            close()
                        }

                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    color.copy(alpha = fillAlphaTop),
                                    color.copy(alpha = 0.0f)
                                ),
                                startY = 0f,
                                endY = h
                            )
                        )

                        drawPath(
                            path = linePath,
                            color = color,
                            style = Stroke(
                                width = 2.6.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }

                    if (formFactor == PodFormFactor.SINGLE_HEADSET) {
                        if (showLeft) {
                            drawSmoothSeries(leftColor, 0.24f) { it.headsetBattery }
                        }
                    } else {
                        if (showCase) {
                            drawSmoothSeries(caseColor, 0.14f) { it.caseBattery }
                        }
                        if (showRight) {
                            drawSmoothSeries(rightColor, 0.18f) { it.rightBattery }
                        }
                        if (showLeft) {
                            drawSmoothSeries(leftColor, 0.22f) { it.leftBattery }
                        }
                    }

                    // Crosshair & active marker halos on scrubbed or latest sample
                    val markerSample = scrubbedIndex?.let { ordered.getOrNull(it) }
                    if (markerSample != null) {
                        val markerX = ((markerSample.timestampMillis - minTime) / timeSpan) * w
                        drawLine(
                            color = crosshairColor.copy(alpha = 0.55f),
                            start = Offset(markerX, 0f),
                            end = Offset(markerX, h),
                            strokeWidth = 1.2.dp.toPx(),
                            pathEffect = dashEffect
                        )

                        fun drawMarkerDot(color: Color, value: Int?) {
                            if (value == null) return
                            val y = h - (value.coerceIn(0, 100) / 100f) * h
                            drawCircle(
                                color = color.copy(alpha = 0.26f),
                                radius = 9.dp.toPx(),
                                center = Offset(markerX, y)
                            )
                            drawCircle(
                                color = color,
                                radius = 4.5.dp.toPx(),
                                center = Offset(markerX, y)
                            )
                        }

                        if (formFactor == PodFormFactor.SINGLE_HEADSET) {
                            if (showLeft) drawMarkerDot(leftColor, markerSample.headsetBattery)
                        } else {
                            if (showCase) drawMarkerDot(caseColor, markerSample.caseBattery)
                            if (showRight) drawMarkerDot(rightColor, markerSample.rightBattery)
                            if (showLeft) drawMarkerDot(leftColor, markerSample.leftBattery)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // X-Axis time labels
        val midTime = minTime + (maxTime - minTime) / 2L
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 42.dp, end = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = axisFormat.format(Date(minTime)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = axisFormat.format(Date(midTime)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = axisFormat.format(Date(maxTime)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Diverging Compose Canvas bar chart visualizing net drain (negative bars) vs charging gains
 * (positive bars) across equal time buckets in the selected time window.
 */
@Composable
fun NetDrainChargeBarChart(
    buckets: List<DrainRateBucket>,
    modifier: Modifier = Modifier,
) {
    val drainColor = MaterialTheme.colorScheme.primary
    val chargeColor = CapodMint
    val baselineColor = MaterialTheme.colorScheme.outline
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)

    val hasAnyMovement = buckets.any { abs(it.netDeltaPercent) >= 0.5f }

    Column(modifier = modifier.fillMaxWidth()) {
        if (!hasAnyMovement) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.analytics_bars_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            return
        }

        val maxAbs = buckets.maxOf { abs(it.netDeltaPercent) }.coerceAtLeast(10f)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(126.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val midY = h / 2f
                val count = buckets.size.coerceAtLeast(1)
                val slotW = w / count
                val barW = (slotW * 0.44f).coerceIn(10.dp.toPx(), 26.dp.toPx())
                val corner = CornerRadius(barW / 2f, barW / 2f)

                // Zero center baseline
                drawLine(
                    color = baselineColor.copy(alpha = 0.5f),
                    start = Offset(0f, midY),
                    end = Offset(w, midY),
                    strokeWidth = 1.dp.toPx()
                )

                buckets.forEachIndexed { idx, bucket ->
                    val centerX = slotW * idx + slotW / 2f
                    val leftX = centerX - barW / 2f
                    val delta = bucket.netDeltaPercent

                    // Subtle slot background indicator
                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset(leftX, 4.dp.toPx()),
                        size = Size(barW, h - 8.dp.toPx()),
                        cornerRadius = corner
                    )

                    if (abs(delta) >= 0.5f) {
                        val barH = ((abs(delta) / maxAbs) * (midY - 6.dp.toPx()))
                            .coerceAtLeast(4.dp.toPx())
                        if (delta > 0f) {
                            // Charging gain bar (upward from baseline)
                            drawRoundRect(
                                color = chargeColor,
                                topLeft = Offset(leftX, midY - barH),
                                size = Size(barW, barH),
                                cornerRadius = corner
                            )
                        } else {
                            // Battery drain bar (downward from baseline)
                            drawRoundRect(
                                color = drainColor,
                                topLeft = Offset(leftX, midY),
                                size = Size(barW, barH),
                                cornerRadius = corner
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            buckets.forEach { bucket ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = bucket.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val rounded = bucket.netDeltaPercent.roundToInt()
                    if (rounded != 0) {
                        Text(
                            text = if (rounded > 0) "+$rounded%" else "$rounded%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = if (rounded > 0) chargeColor else drainColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeriesTogglePill(
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String,
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) {
            color.copy(alpha = 0.16f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable { onClick() }
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (selected) color else color.copy(alpha = 0.3f))
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun ReadoutValue(
    label: String,
    value: String,
    color: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = "$label $value",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = "tnum"
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
