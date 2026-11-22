package me.kavishdevar.librepods

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import me.kavishdevar.librepods.data.*
import me.kavishdevar.librepods.presentation.navigation.AppNavGraph
import me.kavishdevar.librepods.presentation.navigation.Screen
import me.kavishdevar.librepods.presentation.screens.*
import me.kavishdevar.librepods.presentation.theme.LibrePodsTheme
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel
import me.kavishdevar.librepods.presentation.viewmodel.demoState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DashboardRegressionTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun label(id: Int) = instrumentation.targetContext.getString(id)

    @Test fun componentChargingUsesValidReadings() {
        val case = CaseBatteryReading(40, true, false, 1)
        val report = demoState.copy(battery = listOf(
            Battery(BatteryComponent.LEFT, 100, BatteryStatus.NOT_CHARGING),
            Battery(BatteryComponent.RIGHT, 48, BatteryStatus.CHARGING),
        ))
        fun flags(connected: Boolean, current: Boolean) =
            dashboardBattery(report, connected, false, case, current).map { it.activelyCharging }
        assertEquals(listOf(false, true, true), flags(true, true))
        assertEquals(listOf(false, true, false), flags(true, false))
        assertEquals(listOf(false), flags(false, false))
        // Nearby broadcasts may supply a live case reading without an audio connection.
        assertEquals(listOf(true), flags(false, true))
        for (component in listOf(BatteryComponent.LEFT, BatteryComponent.RIGHT, BatteryComponent.HEADPHONES)) {
            for (level in listOf(0, 12, 100, 255)) {
                val max = component == BatteryComponent.HEADPHONES
                val state = report.copy(battery = listOf(Battery(component, level, BatteryStatus.CHARGING)))
                val items = dashboardBattery(state, true, max, null, false)
                assertEquals(if (max) 1 else 3, items.size)
                assertEquals(if (level <= 100) 1 else 0, items.count { it.activelyCharging })
            }
        }
    }

    @Test fun rightOnlyChargeIsAnnouncedOnDashboardAndSheet() {
        val items = listOf(
            DashboardBattery(DeviceArtKind.Left, R.string.andpop_left, 100, false),
            DashboardBattery(DeviceArtKind.Right, R.string.andpop_right, 48, true),
            DashboardBattery(DeviceArtKind.Case, R.string.andpop_case, 80, false, lastSeen = true),
        )
        val sheet = androidx.compose.runtime.mutableStateOf(false)
        compose.setContent {
            LibrePodsTheme(darkTheme = true, m3eEnabled = true) {
                BatterySection(items, null, false, false, {}, { sheet.value = true })
                if (sheet.value) BatteryQuickView("AirPods", null, items) { sheet.value = false }
            }
        }
        val charging = hasContentDescription(label(R.string.andpop_charging))
        val right = hasText(label(R.string.andpop_right))
        compose.onNode(charging and right).assertIsDisplayed()
        compose.onAllNodes(charging).assertCountEquals(1)
        compose.onNodeWithText(label(R.string.andpop_battery_details)).performClick()
        compose.onNodeWithContentDescription(label(R.string.andpop_close_battery)).assertIsDisplayed()
        compose.onAllNodes(charging and right).onLast().assertIsDisplayed()
        compose.onNodeWithContentDescription(label(R.string.andpop_close_battery)).performClick()
        compose.onNode(charging and right).assertIsDisplayed()
    }

    @Test fun caseGaugeReflectsCurrentCaseReading() {
        val case = androidx.compose.runtime.mutableStateOf(DashboardBattery(DeviceArtKind.Case, R.string.andpop_case, 90, false))
        compose.setContent {
            LibrePodsTheme(darkTheme = true, m3eEnabled = true) {
                BatterySection(listOf(DashboardBattery(DeviceArtKind.Left, R.string.andpop_left, 100, true), case.value),
                    null, false, false, {}, {})
            }
        }
        val gauge = SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.ProgressBarRangeInfo,
            androidx.compose.ui.semantics.ProgressBarRangeInfo(90f, 0f..100f))
        val caseGauge = hasText(label(R.string.andpop_case)) and gauge
        val charging = hasContentDescription(label(R.string.andpop_charging))
        compose.onNode(caseGauge).assertIsDisplayed().assert(!charging)
        compose.runOnIdle { case.value = case.value.copy(charging = true) }
        compose.onNode(caseGauge and charging).assertIsDisplayed()
        compose.runOnIdle { case.value = case.value.copy(lastSeen = true) }
        compose.onNode(caseGauge).assertDoesNotExist()
        compose.runOnIdle { case.value = case.value.copy(level = null, lastSeen = false) }
        compose.onNode(caseGauge).assertDoesNotExist()
    }

    @Test fun narrowLargeTextKeepsTransparencyWhole() {
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 1.6f)) {
                LibrePodsTheme(darkTheme = false, m3eEnabled = true) {
                    Box(Modifier.width(288.dp).verticalScroll(rememberScrollState())) {
                        DashboardListeningModes(demoState.copy(offListeningMode = true)) {}
                    }
                }
            }
        }
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(label(R.string.transparency), useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        val layout = results.single()
        assertEquals(1, layout.lineCount)
        // The paragraph may use the parent's full width even when the Text node wraps its contents.
        // Check the actual glyph bounds against the visible node instead of the paragraph's width.
        val text = layout.layoutInput.text
        assertEquals(text.length, layout.getLineEnd(0, visibleEnd = true))
        for (index in text.indices) {
            val glyph = layout.getBoundingBox(index)
            assertTrue("Glyph $index at $glyph outside ${layout.size}",
                glyph.left >= -1f && glyph.right <= layout.size.width + 1f &&
                    glyph.top >= -1f && glyph.bottom <= layout.size.height + 1f)
        }
    }

    @Test fun dashboardSlidersOpenFullSettingsAndSystemBackReturns() {
        val stack = mutableStateListOf<Screen>(Screen.Dashboard)
        val model = AirPodsViewModel()
        compose.setContent {
            LibrePodsTheme(darkTheme = true, m3eEnabled = true) {
                AppNavGraph(backStack = stack, airPodsViewModel = model, onAccount = {})
            }
        }
        val sliders = label(R.string.settings)
        compose.onNodeWithContentDescription(sliders).performClick()
        compose.onNodeWithText(label(R.string.andpop_appearance)).assertIsDisplayed()
        compose.runOnIdle { assertEquals(Screen.AppSettings, stack.last()) }
        instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.onNodeWithContentDescription(sliders).assertIsDisplayed()
        compose.runOnIdle { assertEquals(listOf(Screen.Dashboard), stack.toList()) }
    }
}
