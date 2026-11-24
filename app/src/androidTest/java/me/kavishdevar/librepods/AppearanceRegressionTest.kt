package me.kavishdevar.librepods

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import me.kavishdevar.librepods.presentation.screens.AppSettingsScreen
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import me.kavishdevar.librepods.account.*
import me.kavishdevar.librepods.bluetooth.AACPManager.Companion.ControlCommandIdentifiers
import me.kavishdevar.librepods.data.*
import me.kavishdevar.librepods.presentation.screens.DashboardListeningModes
import me.kavishdevar.librepods.presentation.theme.*
import me.kavishdevar.librepods.presentation.viewmodel.demoState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AppearanceRegressionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun modesFollowReportedStateInEveryColorTheme() {
        val dark = mutableStateOf(true)
        val tint = mutableStateOf(AndpopTint.Glacier)
        val reported = mutableStateOf(NoiseControlMode.ADAPTIVE)
        var requested: NoiseControlMode? = null
        compose.setContent {
            LibrePodsTheme(darkTheme = dark.value, m3eEnabled = true, tint = tint.value) {
                DashboardListeningModes(demoState.copy(
                    capabilities = AirPodsModels.getModelByModelNumber("A3047")!!.capabilities,
                    offListeningMode = true,
                    controlStates = mapOf(ControlCommandIdentifiers.LISTENING_MODE to
                        byteArrayOf((reported.value.ordinal + 1).toByte())),
                )) { requested = it }
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val adaptive = context.getString(R.string.adaptive)
        val transparency = context.getString(R.string.transparency)
        for (isDark in listOf(true, false)) for (choice in AndpopTint.entries) {
            compose.runOnIdle { dark.value = isDark; tint.value = choice; reported.value = NoiseControlMode.ADAPTIVE }
            compose.onNodeWithText(adaptive).assertIsSelected()
            compose.onNodeWithText(transparency).performClick()
            compose.runOnIdle {
                assertEquals(NoiseControlMode.TRANSPARENCY, requested)
                reported.value = NoiseControlMode.TRANSPARENCY
            }
            compose.onNodeWithText(transparency).assertIsSelected()
        }
    }

    @Test fun creditsAndPalettePickerAreReachableFromSettings() {
        val tint = mutableStateOf(AndpopTint.Glacier)
        compose.setContent {
            CompositionLocalProvider(LocalSetAndpopTint provides { tint.value = it }) {
                LibrePodsTheme(darkTheme = true, m3eEnabled = true, tint = tint.value) {
                    AppSettingsScreen(navigateToTroubleshooting = {}, navigateToOpenSourceLicenses = {})
                }
            }
        }
        compose.onNodeWithText("Guava").performClick().assertIsSelected()
        val labels = InstrumentationRegistry.getInstrumentation().targetContext.resources
        val credits = labels.getString(R.string.andpop_credits)
        val source = labels.getString(R.string.andpop_upstream)
        compose.onNodeWithText(source).assertDoesNotExist()
        compose.onNodeWithText(credits).performScrollTo().performClick()
        compose.onNodeWithText(source).assertExists()
        compose.onNodeWithText(labels.getString(android.R.string.ok)).performClick()
        compose.onNodeWithText(source).assertDoesNotExist()
    }

    @Test fun accountFormsAndGuestNavigation() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as android.app.Application
        lateinit var model: AccountViewModel
        compose.runOnIdle { model = AccountViewModel(app) }
        compose.setContent {
            LibrePodsTheme(darkTheme = true, m3eEnabled = true) {
                AccountScreen(model.state.collectAsState().value, model, null)
            }
        }
        compose.waitUntil(10000) { !model.state.value.busy }
        fun tap(label: String) = compose.onNode(hasText(label) and hasClickAction()).performScrollTo().performClick()
        tap("Log in")
        tap("Forgot password?")
        compose.onNodeWithText("Send reset link").assertIsNotEnabled()
        tap("Back")
        tap("Create an account")
        compose.onNode(hasText("Create an account") and hasClickAction()).assertIsNotEnabled()
        tap("Continue as guest")
        compose.waitUntil(10000) { model.state.value.entered }
    }

    @Test fun profileSwitchRestoresColorThemeAndRememberedDevice() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("appearance_test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val profiles = ProfilePreferences(prefs)
        val buds = RememberedAirPods("My buds", "AirPods Pro 2")
        profiles.rememberDevice(buds)
        prefs.edit().putString(ANDPOP_PALETTE_KEY, "Sage").commit()
        profiles.enterProfile("test-user")
        assertEquals("Dark", prefs.getString("andpop_appearance", null))
        assertEquals("Glacier", prefs.getString(ANDPOP_PALETTE_KEY, null))
        assertNull(profiles.rememberedDevice())
        profiles.enterProfile(GUEST_PROFILE)
        assertEquals("Sage", prefs.getString(ANDPOP_PALETTE_KEY, null))
        assertEquals(buds, profiles.rememberedDevice())
    }

    @Test fun olderDarkThemeChoiceCarriesOver() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("appearance_migration_test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        // A 1.5 build stored one theme per appearance.
        prefs.edit().putString(LEGACY_DARK_TINT_KEY, "Iris").putString("andpop_light_tint", "Guava").commit()
        val profiles = ProfilePreferences(prefs)
        profiles.enterProfile("test-user")
        profiles.enterProfile(GUEST_PROFILE)
        assertEquals("Iris", prefs.getString(ANDPOP_PALETTE_KEY, null))
        assertEquals(AndpopTint.Iris, AndpopTint.saved(prefs.getString(ANDPOP_PALETTE_KEY, null)))
        assertEquals(AndpopTint.Glacier, AndpopTint.saved("Unknown"))
    }

    @Test fun accountCallbacksAndPasswordRules() {
        for (purpose in FlowPurpose.entries) {
            val pending = PendingAuth.create(purpose, 0)
            val url = pending.redirect + "?code=1234567890abcdef"
            assertEquals(AuthReturn.Code("1234567890abcdef"), pending.readReturn(url, 1))
            assertNull(pending.readReturn(url, pending.expiresAt))
            assertNull(pending.readReturn(url.replace(pending.nonce, "other"), 1))
        }
        assertFalse(validNewPassword("short"))
        assertTrue(validNewPassword("test-password"))
    }

    @Test fun caseReadingsRespectSessionAndFreshness() {
        val session = CaseBatterySnapshot().startSession("paired")
        assertNull(session.receiveNearby("other", 99, false, true, 1).current(1))
        val updated = session.receiveNearby("paired", 90, true, true, 1)
        assertEquals(90, updated.current(2)?.level)
        assertNull(updated.current(12001))
        assertEquals(90, updated.lastKnown?.level)
        assertNull(updated.startSession("another").lastKnown)
    }
}
