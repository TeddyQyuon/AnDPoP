package me.kavishdevar.librepods.presentation.navigation

import androidx.activity.BackEventCompat.Companion.EDGE_LEFT
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import me.kavishdevar.librepods.BuildConfig
import me.kavishdevar.librepods.account.AccountPage
import me.kavishdevar.librepods.account.AccountScreen
import me.kavishdevar.librepods.account.AccountViewModel
import me.kavishdevar.librepods.account.RememberedAirPods
import me.kavishdevar.librepods.bluetooth.AACPManager
import me.kavishdevar.librepods.data.updates.updates
import me.kavishdevar.librepods.presentation.screens.AccessibilitySettingsScreen
import me.kavishdevar.librepods.presentation.screens.AdaptiveStrengthScreen
import me.kavishdevar.librepods.presentation.screens.AirPodsSettingsRoute
import me.kavishdevar.librepods.presentation.screens.AndpopDashboard
import me.kavishdevar.librepods.presentation.screens.AppSettingsScreen
import me.kavishdevar.librepods.presentation.screens.CallControlScreen
import me.kavishdevar.librepods.presentation.screens.DashboardDeviceControl
import me.kavishdevar.librepods.presentation.screens.DeveloperOptionsScreen
import me.kavishdevar.librepods.presentation.screens.EqualizerRoute
import me.kavishdevar.librepods.presentation.screens.HeadTrackingScreen
import me.kavishdevar.librepods.presentation.screens.HearingAidAdjustmentsScreen
import me.kavishdevar.librepods.presentation.screens.HearingAidScreen
import me.kavishdevar.librepods.presentation.screens.HearingProtectionScreen
import me.kavishdevar.librepods.presentation.screens.LoadingScreen
import me.kavishdevar.librepods.presentation.screens.LongPress
import me.kavishdevar.librepods.presentation.screens.MicrophoneSettingsRoute
import me.kavishdevar.librepods.presentation.screens.OpenSourceLicensesScreen
import me.kavishdevar.librepods.presentation.screens.ReleaseNotesScreen
import me.kavishdevar.librepods.presentation.screens.RenameScreen
import me.kavishdevar.librepods.presentation.screens.TransparencySettingsScreen
import me.kavishdevar.librepods.presentation.screens.TroubleshootingScreen
import me.kavishdevar.librepods.presentation.screens.UpdateHearingTestRoute
import me.kavishdevar.librepods.presentation.screens.VersionScreen
import me.kavishdevar.librepods.presentation.screens.onboarding.OnboardingScreen
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import me.kavishdevar.librepods.presentation.theme.LocalDesignSystem
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel
import me.kavishdevar.librepods.presentation.viewmodel.AppSettingsViewModel

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
internal fun AppNavGraph(
    showReleaseNotes: Boolean = false,
    updatesShown: () -> Unit = {},
    showOnboarding: Boolean = false,
    onboardingComplete: () -> Unit = {},
    backStack: SnapshotStateList<Screen>,
    airPodsViewModel: AirPodsViewModel,
    accountViewModel: AccountViewModel? = null,
    onAccount: () -> Unit = {},
) {
    val navigate: (Screen) -> Unit = { screen ->
        backStack.add(screen)
    }

    val pop: () -> Unit = {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    val m3eEnabled = LocalDesignSystem.current == DesignSystem.Material
    val deviceState by airPodsViewModel.uiState.collectAsState()
    val accountState = accountViewModel?.state?.collectAsState()?.value
    val ready = airPodsViewModel.isReady || deviceState.isDemoMode

    SharedTransitionLayout {
        NavDisplay(
            sharedTransitionScope = this,
            backStack = backStack,
            onBack = {
                if (backStack.size > 1) {
                    backStack.removeAt(backStack.lastIndex)
                }
            },
            entryProvider = { screen ->
                when (screen) {
                    Screen.Onboarding ->
                        NavEntry(screen) {
                            OnboardingScreen(
                                signedInAs = accountState?.takeIf { it.signedIn }?.email,
                                onLogin = {
                                    accountViewModel?.page(AccountPage.Login)
                                    navigate(Screen.Account(loginFirst = true))
                                },
                                onTroubleshooting = { navigate(Screen.Troubleshooting) },
                                onOnboardingComplete = {
                                    onboardingComplete()
                                    if (showReleaseNotes) navigate(Screen.ReleaseNotes) else navigate(Screen.Dashboard)
                                    backStack.remove(screen)
                                },
                            )
                        }

                    Screen.Dashboard ->
                        NavEntry(screen) {
                            val initials = if (accountState?.signedIn == true) {
                                accountState.email.trim().take(2).uppercase().ifBlank { "AN" }
                            } else null
                            AndpopDashboard(
                                viewModel = airPodsViewModel,
                                accountInitials = initials,
                                onAccount = {
                                    if (accountViewModel != null) {
                                        accountViewModel.page(AccountPage.Profile)
                                        navigate(Screen.Account(loginFirst = false))
                                    } else {
                                        onAccount()
                                    }
                                },
                                onDeviceControl = { control ->
                                    navigate(when (control) {
                                        DashboardDeviceControl.Rename -> Screen.Rename
                                        DashboardDeviceControl.Microphone -> Screen.MicrophoneSettings
                                        DashboardDeviceControl.LeftStem -> Screen.LongPress("Left")
                                        DashboardDeviceControl.RightStem -> Screen.LongPress("Right")
                                        DashboardDeviceControl.Accessibility -> Screen.Accessibility
                                        DashboardDeviceControl.Firmware -> Screen.VersionInfo
                                        DashboardDeviceControl.Advanced -> Screen.AirPodsSettings
                                    })
                                },
                                onAppSettings = { navigate(Screen.AppSettings) },
                                onTroubleshooting = { navigate(Screen.Troubleshooting) },
                            )
                        }

                    is Screen.Account ->
                        NavEntry(screen) {
                            val vm = accountViewModel ?: viewModel<AccountViewModel>()
                            val state by vm.state.collectAsState()
                            LaunchedEffect(screen) {
                                if (screen.loginFirst && state.page == AccountPage.Profile && !state.signedIn) {
                                    vm.page(AccountPage.Login)
                                }
                            }
                            // After signing in from the welcome page, continue onboarding.
                            var wasSignedIn by remember { mutableStateOf(state.signedIn) }
                            LaunchedEffect(state.signedIn) {
                                if (screen.loginFirst && state.signedIn && !wasSignedIn) pop()
                                wasSignedIn = state.signedIn
                            }
                            val connected = deviceState.takeIf { it.isLocallyConnected && !it.isDemoMode }?.let {
                                RememberedAirPods(it.deviceName.ifBlank { "AirPods" }, it.modelName)
                            }
                            AccountScreen(
                                state = state,
                                model = vm,
                                connectedDevice = connected,
                                // From the welcome page, return to onboarding so the privacy summary and
                                // permissions still follow a sign-in.
                                onExit = pop,
                            )
                        }

                    Screen.AirPodsSettings ->
                        NavEntry(screen) {
                            if (!ready) {
                                LoadingScreen()
                            } else {
                                AirPodsSettingsRoute(
                                    viewModel = airPodsViewModel,
                                    navigateToHearingProtection = { navigate(Screen.HearingProtection) },
                                    navigateToHearingAid = { navigate(Screen.HearingAid) },
                                    navigateToAdaptiveStrength = { navigate(Screen.AdaptiveStrength) },
                                    navigateToEqualizer = { navigate(Screen.Equalizer) },
                                    navigateToHeadTracking = { navigate(Screen.HeadTracking) },
                                    navigateToVersion = { navigate(Screen.VersionInfo) },
                                    navigateToCallControlScreen = { navigate(Screen.CallControl(it)) },
                                    onDisconnected = pop,
                                )
                            }
                        }

                    Screen.Rename ->
                        NavEntry(screen) {
                            if (!ready) LoadingScreen() else RenameScreen(airPodsViewModel, onSaved = pop)
                        }

                    Screen.AppSettings ->
                        NavEntry(screen) {
                            val vm: AppSettingsViewModel = viewModel()
                            AppSettingsScreen(
                                viewModel = vm,
                                navigateToTroubleshooting = { navigate(Screen.Troubleshooting) },
                                navigateToOpenSourceLicenses = { navigate(Screen.OpenSourceLicenses) },
                                navigateToDeveloperOptions = {
                                    if (!BuildConfig.PLAY_BUILD) navigate(Screen.DeveloperOptions)
                                },
                            )
                        }

                    Screen.DeveloperOptions ->
                        NavEntry(screen) {
                            if (BuildConfig.PLAY_BUILD) {
                                pop()
                            } else {
                                val vm: AppSettingsViewModel = viewModel()
                                DeveloperOptionsScreen(
                                    appSettingsViewModel = vm,
                                    airPodsViewModel = airPodsViewModel,
                                    accountViewModel = accountViewModel,
                                    onTurnedOff = pop,
                                )
                            }
                        }

                    Screen.Troubleshooting ->
                        NavEntry(screen) {
                            TroubleshootingScreen()
                        }

                    Screen.HeadTracking ->
                        NavEntry(screen) {
                            if (!ready) LoadingScreen() else HeadTrackingScreen(airPodsViewModel)
                        }

                    Screen.Accessibility ->
                        NavEntry(screen) {
                            if (!ready) {
                                LoadingScreen()
                            } else {
                                AccessibilitySettingsScreen(
                                    viewModel = airPodsViewModel,
                                    navigateToTransparencyCustomization = { navigate(Screen.TransparencyCustomization) },
                                )
                            }
                        }

                    Screen.TransparencyCustomization ->
                        NavEntry(screen) {
                            if (BuildConfig.PLAY_BUILD) pop() else if (!ready) LoadingScreen() else TransparencySettingsScreen(airPodsViewModel)
                        }

                    Screen.HearingAid ->
                        NavEntry(screen) {
                            if (BuildConfig.PLAY_BUILD) {
                                pop()
                            } else if (!ready) {
                                LoadingScreen()
                            } else {
                                HearingAidScreen(
                                    viewModel = airPodsViewModel,
                                    onNavigateHearingAidAdjustments = { navigate(Screen.HearingAidAdjustments) },
                                    onNavigateHearingTest = { navigate(Screen.UpdateHearingTest) },
                                )
                            }
                        }

                    Screen.HearingAidAdjustments ->
                        NavEntry(screen) {
                            if (BuildConfig.PLAY_BUILD) pop() else if (!ready) LoadingScreen() else HearingAidAdjustmentsScreen(airPodsViewModel)
                        }

                    Screen.AdaptiveStrength ->
                        NavEntry(screen) {
                            if (!ready) LoadingScreen() else AdaptiveStrengthScreen(airPodsViewModel)
                        }

                    Screen.OpenSourceLicenses ->
                        NavEntry(screen) {
                            OpenSourceLicensesScreen()
                        }

                    Screen.UpdateHearingTest ->
                        NavEntry(screen) {
                            if (BuildConfig.PLAY_BUILD) pop() else UpdateHearingTestRoute(airPodsViewModel)
                        }

                    Screen.VersionInfo ->
                        NavEntry(screen) {
                            if (!ready) LoadingScreen() else VersionScreen(airPodsViewModel)
                        }

                    Screen.HearingProtection ->
                        NavEntry(screen) {
                            if (!ready) LoadingScreen() else HearingProtectionScreen(airPodsViewModel)
                        }

                    Screen.Equalizer ->
                        NavEntry(screen) {
                            if (!ready) LoadingScreen() else EqualizerRoute(airPodsViewModel)
                        }

                    is Screen.LongPress ->
                        NavEntry(screen) {
                            if (!ready) {
                                LoadingScreen()
                            } else {
                                LongPress(
                                    viewModel = airPodsViewModel,
                                    name = screen.bud,
                                )
                            }
                        }

                    is Screen.CallControl ->
                        NavEntry(screen) {
                            if (!ready) {
                                LoadingScreen()
                            } else {
                                CallControlScreen(
                                    viewModel = airPodsViewModel,
                                    action = screen.action,
                                    onCallControlValueChanged = { flipped ->
                                        airPodsViewModel.setControlCommandValue(
                                            AACPManager.Companion.ControlCommandIdentifiers.CALL_MANAGEMENT_CONFIG,
                                            if (flipped) byteArrayOf(0x00, 0x02) else byteArrayOf(0x00, 0x03),
                                        )
                                    },
                                )
                            }
                        }

                    is Screen.MicrophoneSettings ->
                        NavEntry(screen) {
                            if (!ready) LoadingScreen() else MicrophoneSettingsRoute(viewModel = airPodsViewModel)
                        }

                    is Screen.ReleaseNotes ->
                        NavEntry(screen) {
                            ReleaseNotesScreen(
                                updates = updates,
                                releaseNotesShown = {
                                    if (showReleaseNotes) {
                                        navigate(Screen.Dashboard)
                                        backStack.remove(screen)
                                        updatesShown()
                                    } else {
                                        pop()
                                    }
                                },
                            )
                        }
                }
            },
            transitionSpec = {
                slideInHorizontally { it } togetherWith slideOutHorizontally { -it / 4 }
            },
            popTransitionSpec = {
                slideInHorizontally { -it / 4 } togetherWith slideOutHorizontally { it }
            },
            predictivePopTransitionSpec = { swipeEdge ->
                if (m3eEnabled) {
                    val enterOffset: (Int) -> Int = if (swipeEdge == EDGE_LEFT) ({ -it / 6 }) else ({ it / 6 })
                    val exitOffset: (Int) -> Int = if (swipeEdge == EDGE_LEFT) ({ it / 8 }) else ({ -it / 8 })

                    fadeIn(animationSpec = tween(250)) +
                        slideInHorizontally(initialOffsetX = enterOffset, animationSpec = tween(250)) togetherWith
                        fadeOut(targetAlpha = 0.75f, animationSpec = tween(250)) +
                        scaleOut(targetScale = 0.9f, animationSpec = tween(250)) +
                        slideOutHorizontally(targetOffsetX = exitOffset, animationSpec = tween(250))
                } else {
                    slideInHorizontally { -it / 4 } togetherWith slideOutHorizontally { it }
                }
            },
        )
    }
}
