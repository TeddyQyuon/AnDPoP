package me.kavishdevar.librepods

import android.graphics.Bitmap
import android.os.Environment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.platform.app.InstrumentationRegistry
import me.kavishdevar.librepods.bluetooth.AACPManager.Companion.ControlCommandIdentifiers
import me.kavishdevar.librepods.data.AirPodsModels
import me.kavishdevar.librepods.data.Battery
import me.kavishdevar.librepods.data.BatteryComponent
import me.kavishdevar.librepods.data.BatteryStatus
import me.kavishdevar.librepods.data.CaseBatteryReading
import me.kavishdevar.librepods.presentation.screens.AppSettingsScreen
import me.kavishdevar.librepods.presentation.screens.DashboardContent
import me.kavishdevar.librepods.presentation.screens.EqualizerScreen
import me.kavishdevar.librepods.presentation.screens.LongPress
import me.kavishdevar.librepods.presentation.screens.onboarding.OnboardingScreen
import me.kavishdevar.librepods.presentation.theme.AndpopAppearance
import me.kavishdevar.librepods.presentation.theme.AndpopTint
import me.kavishdevar.librepods.presentation.theme.LibrePodsTheme
import me.kavishdevar.librepods.presentation.theme.LocalAndpopAppearance
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsUiState
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel
import me.kavishdevar.librepods.presentation.viewmodel.demoInstance
import me.kavishdevar.librepods.presentation.viewmodel.demoState
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * Renders the main 2.0 screens and saves PNGs for design review. Files go to the additional test
 * output directory, which the Android Gradle plugin copies to
 * app/build/outputs/connected_android_test_additional_output after the run.
 */
class ScreenshotTour {
    @get:Rule val compose = createComposeRule()

    private val outputDir: File by lazy {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val path = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
        val dir = path?.let(::File)
            ?: File(instrumentation.targetContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "screens")
        dir.also { it.mkdirs() }
    }

    private fun save(name: String) {
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(outputDir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun pro2(charging: Boolean, mode: Int): AirPodsUiState {
        val model = AirPodsModels.getModelByModelNumber("A3047")!!
        return demoState.copy(
            instance = demoInstance.copy(model = model),
            deviceName = "Qyuon’s AirPods Pro",
            modelName = model.displayName,
            actualModel = "A3047",
            version3 = "8454480",
            capabilities = model.capabilities,
            offListeningMode = true,
            isPremium = true,
            battery = listOf(
                Battery(BatteryComponent.LEFT, if (charging) 92 else 82, BatteryStatus.NOT_CHARGING),
                Battery(BatteryComponent.RIGHT, if (charging) 48 else 79, if (charging) BatteryStatus.CHARGING else BatteryStatus.NOT_CHARGING),
            ),
            controlStates = mapOf(
                ControlCommandIdentifiers.LISTENING_MODE to byteArrayOf(mode.toByte()),
                ControlCommandIdentifiers.CONVERSATION_DETECT_CONFIG to byteArrayOf(1),
            ),
        )
    }

    private fun max(): AirPodsUiState {
        val model = AirPodsModels.getModelByModelNumber("A3184")!!
        return demoState.copy(
            instance = demoInstance.copy(model = model),
            deviceName = "Qyuon’s AirPods Max",
            modelName = model.displayName,
            capabilities = model.capabilities,
            isPremium = true,
            battery = listOf(Battery(BatteryComponent.HEADPHONES, 68, BatteryStatus.NOT_CHARGING)),
            controlStates = mapOf(ControlCommandIdentifiers.LISTENING_MODE to byteArrayOf(2)),
        )
    }

    @Composable
    private fun Dashboard(state: AirPodsUiState, connected: Boolean = true, caseLevel: Int? = 71, caseCharging: Boolean = true) {
        DashboardContent(
            state = state,
            connected = connected,
            caseReading = caseLevel?.let { CaseBatteryReading(it, charging = caseCharging, estimated = false, observedAt = 0) },
            caseCurrent = connected,
            refreshing = false,
            onRefresh = {},
            onOpenBluetooth = {},
            onSelectMode = {},
            onWearDetectionChanged = {},
            onConversationAwarenessChanged = {},
            onDeviceControl = {},
            onAppSettings = {},
            onTroubleshooting = {},
            onReconnect = {},
        )
    }

    @Test fun dashboardThemes() {
        var dark by mutableStateOf(true)
        var tint by mutableStateOf(AndpopTint.Glacier)
        var screen by mutableStateOf(0)
        compose.setContent {
            LibrePodsTheme(darkTheme = dark, m3eEnabled = true, tint = tint) {
                Box(Modifier.fillMaxSize()) {
                    when (screen) {
                        0 -> Dashboard(pro2(charging = true, mode = 2))
                        1 -> Dashboard(pro2(charging = false, mode = 3), caseCharging = false, caseLevel = 64)
                        2 -> Dashboard(max())
                        3 -> Dashboard(AirPodsUiState(deviceName = "Qyuon’s AirPods Pro", connectionSuccessful = true), connected = false, caseLevel = null)
                        else -> Dashboard(AirPodsUiState(), connected = false, caseLevel = null)
                    }
                }
            }
        }
        save("app-01-dashboard-glacier-dark")
        compose.onRoot().performTouchInput { swipeUp() }
        save("app-02-dashboard-glacier-dark-scrolled")
        compose.runOnIdle { screen = 1; dark = false; tint = AndpopTint.Iris }
        save("app-03-dashboard-iris-light")
        compose.runOnIdle { screen = 2; dark = true; tint = AndpopTint.Guava }
        save("app-04-airpods-max-guava-dark")
        compose.runOnIdle { screen = 3; dark = true; tint = AndpopTint.Sage }
        save("app-05-disconnected-sage-dark")
        compose.runOnIdle { screen = 4; dark = false; tint = AndpopTint.Apricot }
        save("app-06-pairing-apricot-light")
        compose.runOnIdle { screen = 0; dark = true; tint = AndpopTint.Graphite }
        save("app-07-dashboard-graphite-dark")
        compose.runOnIdle { screen = 0; dark = false; tint = AndpopTint.Wallpaper }
        save("app-08-dashboard-wallpaper-light")
    }

    @Test fun settingsScreen() {
        var dark by mutableStateOf(true)
        compose.setContent {
            CompositionLocalProvider(LocalAndpopAppearance provides AndpopAppearance.Dark) {
                LibrePodsTheme(darkTheme = dark, m3eEnabled = true, tint = AndpopTint.Glacier) {
                    AppSettingsScreen(navigateToTroubleshooting = {}, navigateToOpenSourceLicenses = {})
                }
            }
        }
        save("app-09-settings-glacier-dark")
        compose.onRoot().performTouchInput { swipeUp() }
        save("app-10-settings-glacier-dark-scrolled")
        compose.runOnIdle { dark = false }
        save("app-11-settings-glacier-light-scrolled")
    }

    @Test fun onboardingPages() {
        compose.setContent { OnboardingScreen {} }
        save("app-12-onboarding-welcome")
    }

    @Test fun detailScreensForStore() {
        var page by mutableStateOf(0)
        val demoModel = AirPodsViewModel().apply { setDemoMode(true) }
        compose.setContent {
            LibrePodsTheme(darkTheme = true, m3eEnabled = true, tint = AndpopTint.Glacier) {
                when (page) {
                    0 -> LongPress(viewModel = demoModel, name = "Left")
                    else -> EqualizerScreen(
                        state = demoState,
                        setCustomEqEnabled = {},
                        setCustomEq = { _, _, _ -> },
                    )
                }
            }
        }
        save("app-13-press-and-hold-left")
        compose.runOnIdle { page = 1 }
        save("app-14-equalizer-custom")
    }
}
