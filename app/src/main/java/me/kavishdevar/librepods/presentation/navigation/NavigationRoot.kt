package me.kavishdevar.librepods.presentation.navigation

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.account.AccountViewModel
import me.kavishdevar.librepods.presentation.MaterialIcons
import me.kavishdevar.librepods.presentation.components.StyledIconButton
import me.kavishdevar.librepods.presentation.components.StyledScaffold
import me.kavishdevar.librepods.presentation.screens.AndpopDashboard
import me.kavishdevar.librepods.presentation.screens.DashboardDeviceControl
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import me.kavishdevar.librepods.presentation.theme.LocalAndpopPalette
import me.kavishdevar.librepods.presentation.theme.LocalDesignSystem
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel

@Composable
internal fun NavigationRoot(
    showReleaseNotes: Boolean = false,
    updatesShown: () -> Unit = {},
    showOnboarding: Boolean = false,
    onboardingComplete: () -> Unit = {},
    airPodsViewModel: AirPodsViewModel,
    accountViewModel: AccountViewModel? = null,
    onAccount: () -> Unit = {},
) {
    val backStack = remember {
        mutableStateListOf(
            when {
                showOnboarding -> Screen.Onboarding
                showReleaseNotes -> Screen.ReleaseNotes
                else -> Screen.Dashboard
            }
        )
    }

    val accountState = accountViewModel?.state?.collectAsState()?.value
    LaunchedEffect(accountState?.open) {
        if (accountState?.open == true && backStack.lastOrNull() !is Screen.Account) {
            backStack.add(Screen.Account(loginFirst = false))
            accountViewModel.consumeOpenRequest()
        } else if (accountState?.open == true) {
            accountViewModel.consumeOpenRequest()
        }
    }

    val currentScreen = backStack.last()
    val state by airPodsViewModel.uiState.collectAsState()
    val m3eEnabled = LocalDesignSystem.current == DesignSystem.Material
    val palette = LocalAndpopPalette.current

    val title = when (currentScreen) {
        Screen.Onboarding, Screen.Dashboard, is Screen.Account -> ""
        Screen.AirPodsSettings -> stringResource(R.string.andpop_all_device_settings)
        Screen.Accessibility -> stringResource(R.string.accessibility)
        Screen.AdaptiveStrength -> stringResource(R.string.customize_adaptive_audio)
        Screen.AppSettings -> stringResource(R.string.settings)
        Screen.DeveloperOptions -> stringResource(R.string.andpop_developer_options)
        Screen.Equalizer -> stringResource(R.string.equalizer)
        Screen.HeadTracking -> stringResource(R.string.head_tracking)
        Screen.HearingAid -> stringResource(R.string.hearing_aid)
        Screen.HearingAidAdjustments -> stringResource(R.string.adjustments)
        Screen.HearingProtection -> stringResource(R.string.hearing_protection)
        is Screen.LongPress -> stringResource(
            if (currentScreen.bud.equals("Left", ignoreCase = true)) R.string.andpop_left_bud else R.string.andpop_right_bud,
        )
        Screen.OpenSourceLicenses -> stringResource(R.string.open_source_licenses)
        Screen.Rename -> stringResource(R.string.name)
        Screen.TransparencyCustomization -> stringResource(R.string.customize_transparency_mode)
        Screen.Troubleshooting -> stringResource(R.string.troubleshooting)
        Screen.UpdateHearingTest -> stringResource(R.string.update_hearing_test)
        Screen.VersionInfo -> stringResource(R.string.andpop_firmware_title)
        is Screen.CallControl -> stringResource(if (currentScreen.action == me.kavishdevar.librepods.presentation.screens.CALL_ACTION_MUTE) R.string.andpop_mute_unmute else R.string.andpop_hang_up)
        Screen.MicrophoneSettings -> stringResource(R.string.microphone_mode)
        Screen.ReleaseNotes -> ""
    }

    // Head tracking starts and stops with its screen, so no screen needs a top-bar action.
    val actionButtons = emptyList<@Composable (backdrop: LayerBackdrop) -> Unit>()

    val isWideTwoPane = LocalConfiguration.current.screenWidthDp >= 840 &&
        backStack.firstOrNull() == Screen.Dashboard &&
        currentScreen != Screen.Dashboard &&
        currentScreen != Screen.Onboarding &&
        currentScreen !is Screen.Account

    val accountInitials = if (accountState?.signedIn == true) {
        accountState.email.trim().take(2).uppercase().ifBlank { "AN" }
    } else null

    if (isWideTwoPane) {
        Row(Modifier.fillMaxSize().background(palette.bg)) {
            Box(Modifier.width(400.dp).fillMaxHeight()) {
                AndpopDashboard(
                    viewModel = airPodsViewModel,
                    accountInitials = accountInitials,
                    onAccount = {
                        if (accountViewModel != null) {
                            accountViewModel.page(me.kavishdevar.librepods.account.AccountPage.Profile)
                            backStack.add(Screen.Account(loginFirst = false))
                        } else {
                            onAccount()
                        }
                    },
                    onDeviceControl = { control ->
                        backStack.add(when (control) {
                            DashboardDeviceControl.Rename -> Screen.Rename
                            DashboardDeviceControl.Microphone -> Screen.MicrophoneSettings
                            DashboardDeviceControl.LeftStem -> Screen.LongPress("Left")
                            DashboardDeviceControl.RightStem -> Screen.LongPress("Right")
                            DashboardDeviceControl.Accessibility -> Screen.Accessibility
                            DashboardDeviceControl.Firmware -> Screen.VersionInfo
                            DashboardDeviceControl.Advanced -> Screen.AirPodsSettings
                        })
                    },
                    onAppSettings = { backStack.add(Screen.AppSettings) },
                    onTroubleshooting = { backStack.add(Screen.Troubleshooting) },
                )
            }
            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                StyledScaffold(
                    visible = currentScreen.showTopBar,
                    title = title,
                    showBackButton = backStack.size > 1,
                    onNavigateBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) },
                    actionButtons = actionButtons,
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                        Box(Modifier.widthIn(max = 560.dp).fillMaxSize()) {
                            AppNavGraph(
                                showReleaseNotes = showReleaseNotes,
                                updatesShown = updatesShown,
                                showOnboarding = showOnboarding,
                                onboardingComplete = onboardingComplete,
                                backStack = backStack,
                                airPodsViewModel = airPodsViewModel,
                                accountViewModel = accountViewModel,
                                onAccount = onAccount,
                            )
                        }
                    }
                }
            }
        }
    } else {
        StyledScaffold(
            visible = currentScreen.showTopBar,
            title = title,
            showBackButton = backStack.size > 1,
            onNavigateBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) },
            actionButtons = actionButtons,
        ) {
            Box(Modifier.fillMaxSize().background(palette.bg), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.widthIn(max = if (currentScreen == Screen.Dashboard || currentScreen == Screen.Onboarding || currentScreen is Screen.Account) 1200.dp else 560.dp).fillMaxSize()) {
                    AppNavGraph(
                        showReleaseNotes = showReleaseNotes,
                        updatesShown = updatesShown,
                        showOnboarding = showOnboarding,
                        onboardingComplete = onboardingComplete,
                        backStack = backStack,
                        airPodsViewModel = airPodsViewModel,
                        accountViewModel = accountViewModel,
                        onAccount = onAccount,
                    )
                }
            }
        }
    }
}
