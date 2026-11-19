package com.example.model

import com.example.data.BatteryLogEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryAnalyticsCalculatorTest {

    @Test
    fun calculatesAccurateDrainRateAndLrImbalanceForDualPods() {
        val t0 = 1_700_000_000_000L
        val oneHour = 60L * 60L * 1000L
        val logs = listOf(
            BatteryLogEntity(1, 1L, t0, leftBattery = 100, rightBattery = 100, caseBattery = 80, headsetBattery = null, rssi = -58),
            BatteryLogEntity(2, 1L, t0 + oneHour / 2, leftBattery = 90, rightBattery = 95, caseBattery = 80, headsetBattery = null, rssi = -60),
            BatteryLogEntity(3, 1L, t0 + oneHour, leftBattery = 80, rightBattery = 90, caseBattery = 80, headsetBattery = null, rssi = -59),
        )

        val summary = BatteryAnalyticsCalculator.compute(
            logs = logs,
            model = PodModel.AIRPODS_PRO_2,
            range = AnalyticsTimeRange.SIX_HOURS,
            nowMillis = t0 + oneHour
        )

        assertEquals(3, summary.sampleCount)
        assertNotNull(summary.drainRatePerHour)
        // Average pod battery dropped from 100% to 85% in 1 hour => 15%/hr
        assertEquals(15f, summary.drainRatePerHour!!, 0.2f)
        assertEquals(15, summary.totalPercentDrained)
        assertEquals(0, summary.totalPercentCharged)
        assertEquals("Left", summary.fastestDrainingSide)
        assertNotNull(summary.averageLrImbalance)
        // Deltas: 0, 5, 10 => average 5%
        assertEquals(5f, summary.averageLrImbalance!!, 0.01f)
    }

    @Test
    fun separatesChargingGainsFromDrainRate() {
        val t0 = 1_700_000_000_000L
        val thirtyMin = 30L * 60L * 1000L
        val logs = listOf(
            BatteryLogEntity(1, 2L, t0, null, null, null, headsetBattery = 80, rssi = -55),
            BatteryLogEntity(2, 2L, t0 + thirtyMin, null, null, null, headsetBattery = 70, rssi = -55),
            BatteryLogEntity(3, 2L, t0 + thirtyMin * 2, null, null, null, headsetBattery = 95, rssi = -55),
        )

        val summary = BatteryAnalyticsCalculator.compute(
            logs = logs,
            model = PodModel.AIRPODS_MAX,
            range = AnalyticsTimeRange.SIX_HOURS,
            nowMillis = t0 + thirtyMin * 2
        )

        assertEquals(10, summary.totalPercentDrained)
        assertEquals(25, summary.totalPercentCharged)
        // 10% drained in 0.5 hr => 20%/hr
        assertNotNull(summary.drainRatePerHour)
        assertEquals(20f, summary.drainRatePerHour!!, 0.2f)
        assertTrue(summary.buckets.isNotEmpty())
    }
}
