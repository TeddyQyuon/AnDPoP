package me.kavishdevar.librepods

import android.app.Application
import android.content.Context
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import me.kavishdevar.librepods.account.AccountViewModel
import me.kavishdevar.librepods.data.Battery
import me.kavishdevar.librepods.data.BatteryComponent
import me.kavishdevar.librepods.data.BatteryStatus
import me.kavishdevar.librepods.data.CaseBatteryReading
import me.kavishdevar.librepods.data.ListeningModeCycle
import me.kavishdevar.librepods.data.XposedRemotePrefProvider
import me.kavishdevar.librepods.presentation.navigation.AppNavGraph
import me.kavishdevar.librepods.presentation.navigation.Screen
import me.kavishdevar.librepods.presentation.screens.AppSettingsScreen
import me.kavishdevar.librepods.presentation.screens.DashboardContent
import me.kavishdevar.librepods.presentation.screens.onboarding.OnboardingScreen
import me.kavishdevar.librepods.presentation.screens.onboarding.PermissionsPage
import me.kavishdevar.librepods.presentation.theme.LibrePodsTheme
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsUiState
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel
import me.kavishdevar.librepods.services.AirPodsNotificationFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PlayReadinessRegressionTest {
    @get:Rule
    val compose = createComposeRule()

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private fun label(id: Int) = context.getString(id)

    @Test
    fun dashboardAvatarOpensAccountAndBackReturns() {
        val stack = mutableStateListOf<Screen>(Screen.Dashboard)
        val model = AirPodsViewModel().apply { setDemoMode(true) }
        val accountModel = AccountViewModel(context.applicationContext as Application)
        var backDispatcher: OnBackPressedDispatcher? = null
        compose.setContent {
            LibrePodsTheme(darkTheme = true, m3eEnabled = true) {
                backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
                AppNavGraph(
                    backStack = stack,
                    airPodsViewModel = model,
                    accountViewModel = accountModel,
                )
            }
        }
        compose.onNodeWithContentDescription(label(R.string.andpop_account)).performClick()
        compose.waitUntil(5_000) { !accountModel.state.value.busy }
        compose.onNodeWithText(label(R.string.andpop_account_guest)).assertIsDisplayed()
        compose.runOnIdle { assertTrue(stack.last() is Screen.Account) }
        compose.runOnIdle { backDispatcher!!.onBackPressed() }
        compose.onNodeWithContentDescription(label(R.string.andpop_account)).assertIsDisplayed()
        compose.runOnIdle { assertEquals(listOf(Screen.Dashboard), stack.toList()) }
    }

    @Test
    fun welcomeAlreadyHaveAccountOpensLogin() {
        var loginRequested = false
        compose.setContent {
            OnboardingScreen(
                onLogin = { loginRequested = true },
                onOnboardingComplete = {},
            )
        }
        compose.onNodeWithText(label(R.string.andpop_already_have_account)).assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(loginRequested) }
    }

    @Test
    fun permissionsPageGatesContinueAndHasDistinctIconLabels() {
        compose.setContent {
            LibrePodsTheme(darkTheme = true, m3eEnabled = true) {
                PermissionsPage(onBackward = {}, onForward = {})
            }
        }
        assertTrue(compose.onAllNodesWithText(label(R.string.andpop_allow)).fetchSemanticsNodes().isNotEmpty())
        compose.onNodeWithText(label(R.string.andpop_continue_need_nearby)).performScrollTo().assertIsNotEnabled()
        compose.onAllNodesWithContentDescription("bluetooth").assertCountEquals(0)
    }

    @Test
    fun developerOptionsAreHiddenInPlayAndUnlockableInFoss() {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .edit()
            .remove("developer_options_enabled")
            .commit()

        compose.setContent {
            LibrePodsTheme(darkTheme = true, m3eEnabled = true) {
                AppSettingsScreen(
                    navigateToTroubleshooting = {},
                    navigateToOpenSourceLicenses = {},
                )
            }
        }

        val appVersionNode = compose.onNodeWithText(label(R.string.andpop_app_version)).performScrollTo()
        repeat(7) {
            appVersionNode.performClick()
        }

        if (BuildConfig.PLAY_BUILD) {
            compose.onNodeWithText(label(R.string.andpop_developer_options)).assertDoesNotExist()
            assertFalse(XposedRemotePrefProvider.create().isAvailable())
        } else {
            compose.onNodeWithText(label(R.string.andpop_developer_options)).assertExists()
        }
    }

    @Test
    fun notificationFormatterFormatsConnectedAndIdleStates() {
        assertEquals(label(R.string.andpop_notif_running), AirPodsNotificationFormatter.runningTitle(context))
        assertEquals("Qyuon's AirPods", AirPodsNotificationFormatter.formatTitle(context, "Qyuon's AirPods"))

        val subtitle = AirPodsNotificationFormatter.formatStatusSubtitle(context, ListeningModeCycle.NOISE_CANCELLATION)
        assertTrue(subtitle.contains(label(R.string.noise_cancellation)))
        assertFalse(subtitle.contains("aacpSocket"))

        val summary = AirPodsNotificationFormatter.formatBatterySummary(
            context = context,
            batteryList = listOf(
                Battery(BatteryComponent.LEFT, 92, BatteryStatus.NOT_CHARGING),
                Battery(BatteryComponent.RIGHT, 48, BatteryStatus.CHARGING),
            ),
            caseReading = CaseBatteryReading(71, charging = true, estimated = false, observedAt = 1L),
            caseCurrent = true,
        )
        assertEquals("Left 92% · Right 48% ⚡ · Case 71% ⚡", summary)

        val maxSummary = AirPodsNotificationFormatter.formatBatterySummary(
            context = context,
            batteryList = listOf(Battery(BatteryComponent.HEADPHONES, 68, BatteryStatus.NOT_CHARGING)),
        )
        assertEquals("Headphones 68%", maxSummary)
    }

    @Test
    fun pairingGuideShowsTryDemoModeAndEntersDemoState() {
        var demoRequested = false
        compose.setContent {
            LibrePodsTheme(darkTheme = true, m3eEnabled = true) {
                DashboardContent(
                    state = AirPodsUiState(),
                    connected = false,
                    caseReading = null,
                    caseCurrent = false,
                    refreshing = false,
                    onRefresh = {},
                    onOpenBluetooth = {},
                    onSelectMode = {},
                    onWearDetectionChanged = {},
                    onConversationAwarenessChanged = {},
                    onDeviceControl = {},
                    onAppSettings = {},
                    onTroubleshooting = {},
                    onTryDemo = { demoRequested = true },
                )
            }
        }
        compose.onNodeWithText(label(R.string.andpop_try_demo)).performScrollTo().performClick()
        compose.runOnIdle { assertTrue(demoRequested) }
    }
}
