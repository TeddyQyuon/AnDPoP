package com.example.model

import com.example.data.BatteryLogEntity
import kotlin.math.abs
import kotlin.math.roundToInt

enum class AnalyticsTimeRange(
    val key: String,
    val shortLabel: String,
    val durationMillis: Long,
    val bucketCount: Int,
) {
    ONE_HOUR("1h", "1H", 60L * 60L * 1000L, 6),
    SIX_HOURS("6h", "6H", 6L * 60L * 60L * 1000L, 6),
    TWENTY_FOUR_HOURS("24h", "24H", 24L * 60L * 60L * 1000L, 8),
    SEVEN_DAYS("7d", "7D", 7L * 24L * 60L * 60L * 1000L, 7),
}

data class DrainRateBucket(
    val label: String,
    val netDeltaPercent: Float, // Negative = net drain, Positive = net charge gain
    val sampleCount: Int,
)

data class BatteryAnalyticsSummary(
    val sampleCount: Int,
    val drainRatePerHour: Float?,      // Positive value representing % drained per hour
    val estimatedMinutesRemaining: Int?,
    val totalPercentDrained: Int,
    val totalPercentCharged: Int,
    val averageLrImbalance: Float?,    // Average |Left - Right| % for dual-pod devices
    val fastestDrainingSide: String?,  // "Left", "Right", or "Balanced"
    val buckets: List<DrainRateBucket>,
)

/**
 * Pure domain calculator computing real battery drain rates, charging gains, L/R imbalance,
 * and bucketed drain/charge deltas from Room-persisted BatteryLogEntity rows.
 */
object BatteryAnalyticsCalculator {

    fun compute(
        logs: List<BatteryLogEntity>,
        model: PodModel,
        range: AnalyticsTimeRange,
        nowMillis: Long = System.currentTimeMillis(),
    ): BatteryAnalyticsSummary {
        val ordered = logs.sortedBy { it.timestampMillis }
        if (ordered.isEmpty()) {
            return BatteryAnalyticsSummary(
                sampleCount = 0,
                drainRatePerHour = null,
                estimatedMinutesRemaining = null,
                totalPercentDrained = 0,
                totalPercentCharged = 0,
                averageLrImbalance = null,
                fastestDrainingSide = null,
                buckets = emptyList(),
            )
        }

        val isSingleHeadset = model.formFactor == PodFormFactor.SINGLE_HEADSET

        fun primaryBattery(log: BatteryLogEntity): Float? {
            return if (isSingleHeadset) {
                log.headsetBattery?.toFloat()
            } else {
                val values = listOfNotNull(log.leftBattery, log.rightBattery)
                if (values.isEmpty()) null else values.average().toFloat()
            }
        }

        var totalDrainPct = 0f
        var totalChargePct = 0f
        var totalDrainDurationMs = 0L

        var leftDrainTotal = 0
        var rightDrainTotal = 0

        for (i in 1 until ordered.size) {
            val prev = ordered[i - 1]
            val curr = ordered[i]
            val dtMs = curr.timestampMillis - prev.timestampMillis
            if (dtMs <= 0L) continue

            val prevPct = primaryBattery(prev)
            val currPct = primaryBattery(curr)
            if (prevPct != null && currPct != null) {
                val delta = currPct - prevPct
                if (delta < 0f) {
                    totalDrainPct += -delta
                    totalDrainDurationMs += dtMs
                } else if (delta > 0f) {
                    totalChargePct += delta
                }
            }

            if (!isSingleHeadset) {
                val pL = prev.leftBattery
                val cL = curr.leftBattery
                if (pL != null && cL != null && cL < pL) {
                    leftDrainTotal += (pL - cL)
                }
                val pR = prev.rightBattery
                val cR = curr.rightBattery
                if (pR != null && cR != null && cR < pR) {
                    rightDrainTotal += (pR - cR)
                }
            }
        }

        val drainRatePerHour = if (totalDrainDurationMs >= 60_000L && totalDrainPct > 0f) {
            val hours = totalDrainDurationMs / (1000f * 60f * 60f)
            (totalDrainPct / hours).coerceIn(0.1f, 100f)
        } else {
            null
        }

        val latestPrimary = ordered.asReversed().firstNotNullOfOrNull { primaryBattery(it) }
        val estimatedMinutesRemaining = when {
            latestPrimary == null || latestPrimary <= 0f -> null
            drainRatePerHour != null && drainRatePerHour > 0.2f -> {
                ((latestPrimary / drainRatePerHour) * 60f).roundToInt().coerceAtLeast(1)
            }
            else -> {
                ((latestPrimary / 100f) * model.ratedListeningMinutes).roundToInt().coerceAtLeast(1)
            }
        }

        val lrDeltas = if (!isSingleHeadset) {
            ordered.mapNotNull { entry ->
                val l = entry.leftBattery
                val r = entry.rightBattery
                if (l != null && r != null) abs(l - r).toFloat() else null
            }
        } else {
            emptyList()
        }

        val averageLrImbalance = if (lrDeltas.isNotEmpty()) {
            lrDeltas.average().toFloat()
        } else {
            null
        }

        val fastestDrainingSide = when {
            isSingleHeadset -> null
            leftDrainTotal == 0 && rightDrainTotal == 0 -> null
            abs(leftDrainTotal - rightDrainTotal) <= 3 -> "Balanced"
            leftDrainTotal > rightDrainTotal -> "Left"
            else -> "Right"
        }

        val buckets = computeBuckets(ordered, range, nowMillis, ::primaryBattery)

        return BatteryAnalyticsSummary(
            sampleCount = ordered.size,
            drainRatePerHour = drainRatePerHour,
            estimatedMinutesRemaining = estimatedMinutesRemaining,
            totalPercentDrained = totalDrainPct.roundToInt(),
            totalPercentCharged = totalChargePct.roundToInt(),
            averageLrImbalance = averageLrImbalance,
            fastestDrainingSide = fastestDrainingSide,
            buckets = buckets,
        )
    }

    private fun computeBuckets(
        ordered: List<BatteryLogEntity>,
        range: AnalyticsTimeRange,
        nowMillis: Long,
        batterySelector: (BatteryLogEntity) -> Float?,
    ): List<DrainRateBucket> {
        val count = range.bucketCount
        val windowStart = nowMillis - range.durationMillis
        val bucketWidthMs = range.durationMillis / count

        return (0 until count).map { idx ->
            val start = windowStart + idx * bucketWidthMs
            val end = if (idx == count - 1) nowMillis + 1L else start + bucketWidthMs
            val inBucket = ordered.filter { it.timestampMillis in start until end }
            val values = inBucket.mapNotNull(batterySelector)

            val netDelta = if (values.size >= 2) {
                values.last() - values.first()
            } else {
                0f
            }

            val label = when (range) {
                AnalyticsTimeRange.ONE_HOUR -> "-${(count - idx) * 10}m"
                AnalyticsTimeRange.SIX_HOURS -> "-${count - idx}h"
                AnalyticsTimeRange.TWENTY_FOUR_HOURS -> "-${(count - idx) * 3}h"
                AnalyticsTimeRange.SEVEN_DAYS -> "D-${count - 1 - idx}".let {
                    if (idx == count - 1) "Today" else it
                }
            }

            DrainRateBucket(
                label = label,
                netDeltaPercent = netDelta,
                sampleCount = inBucket.size,
            )
        }
    }
}
