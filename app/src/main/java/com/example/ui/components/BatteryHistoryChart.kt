package com.example.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.BatteryLogEntity
import com.example.model.PodFormFactor
import com.example.ui.theme.BatteryMidAmber
import com.example.ui.theme.CapodMint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Multi-series Compose Canvas chart plotting real recorded battery percentages over time
 * for Left pod, Right pod, and Case (or single Headset).
 */
@Composable
fun BatteryHistoryChart(
    logs: List<BatteryLogEntity>,
    formFactor: PodFormFactor,
    modifier: Modifier = Modifier,
) {
    val ordered = remember(logs) { logs.sortedBy { it.timestampMillis } }
    val leftColor = MaterialTheme.colorScheme.primary
    val rightColor = CapodMint
    val caseColor = BatteryMidAmber
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Column(modifier = modifier.fillMaxWidth()) {
        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (formFactor == PodFormFactor.SINGLE_HEADSET) {
                LegendChip(color = leftColor, label = stringResource(R.string.cd_headset))
            } else {
                LegendChip(color = leftColor, label = stringResource(R.string.label_left_pod))
                LegendChip(color = rightColor, label = stringResource(R.string.label_right_pod))
                LegendChip(color = caseColor, label = stringResource(R.string.label_case))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (ordered.size < 2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.history_empty_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(horizontal = 12.dp, vertical = 14.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val w = size.width
                val h = size.height
                val minTime = ordered.first().timestampMillis
                val maxTime = ordered.last().timestampMillis
                val timeSpan = (maxTime - minTime).coerceAtLeast(1L).toFloat()

                // Horizontal 0 / 50 / 100 gridlines
                for (pct in listOf(0f, 50f, 100f)) {
                    val y = h - (pct / 100f) * h
                    drawLine(
                        color = gridColor.copy(alpha = 0.7f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                fun drawSeries(color: Color, valueSelector: (BatteryLogEntity) -> Int?) {
                    val points = ordered.mapNotNull { entry ->
                        val v = valueSelector(entry) ?: return@mapNotNull null
                        val x = ((entry.timestampMillis - minTime) / timeSpan) * w
                        val y = h - (v.coerceIn(0, 100) / 100f) * h
                        Offset(x, y)
                    }
                    if (points.size < 2) return

                    val path = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            lineTo(points[i].x, points[i].y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(
                            width = 2.6.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                    // Highlight latest reading dot
                    val last = points.last()
                    drawCircle(color = color, radius = 4.dp.toPx(), center = last)
                }

                if (formFactor == PodFormFactor.SINGLE_HEADSET) {
                    drawSeries(leftColor) { it.headsetBattery }
                } else {
                    drawSeries(caseColor) { it.caseBattery }
                    drawSeries(rightColor) { it.rightBattery }
                    drawSeries(leftColor) { it.leftBattery }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        val formatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatter.format(Date(ordered.first().timestampMillis)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${ordered.size} samples",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatter.format(Date(ordered.last().timestampMillis)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LegendChip(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
