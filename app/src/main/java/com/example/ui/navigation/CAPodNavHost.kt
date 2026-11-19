package com.example.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.model.AnalyticsTimeRange
import com.example.model.BluetoothHardwareState
import com.example.service.AncQuickSettingsTile
import com.example.ui.screens.BatteryAnalyticsScreen
import com.example.ui.screens.BleDiscoveryScreen
import com.example.ui.screens.DeviceDetailScreen
import com.example.ui.screens.DevicesScreen
import com.example.ui.screens.OverviewScreen
import com.example.ui.screens.ProfileEditorScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.UnmatchedDetailScreen
import com.example.viewmodel.CAPodUiState
import com.example.viewmodel.CAPodViewModel

object CAPodRoutes {
    const val OVERVIEW = "overview"
    const val DEVICES = "devices"
    const val BLE_DISCOVERY = "ble_discovery"
    const val BATTERY_ANALYTICS = "battery_analytics/{profileId}"
    const val DEVICE_DETAIL = "device_detail/{profileId}"
    const val PROFILE_EDITOR = "profile_editor/{profileId}"
    const val UNMATCHED = "unmatched"
    const val SETTINGS = "settings"

    fun batteryAnalytics(profileId: Long = 0L): String = "battery_analytics/$profileId"
    fun deviceDetail(profileId: Long): String = "device_detail/$profileId"
    fun profileEditor(profileId: Long = 0L): String = "profile_editor/$profileId"
}

@Composable
fun CAPodNavHost(
    navController: NavHostController,
    uiState: CAPodUiState,
    viewModel: CAPodViewModel,
    snackbarHostState: SnackbarHostState,
    onRequestPermissions: () -> Unit,
    onRequestEnableBluetooth: () -> Unit,
) {
    LaunchedEffect(navController, snackbarHostState) {
        navController.currentBackStackEntryFlow.collect {
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }

    NavHost(
        navController = navController,
        startDestination = CAPodRoutes.OVERVIEW,
        enterTransition = {
            fadeIn(animationSpec = tween(220)) +
                slideInHorizontally(animationSpec = tween(240), initialOffsetX = { it / 12 })
        },
        exitTransition = {
            fadeOut(animationSpec = tween(180))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(200))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(180)) +
                slideOutHorizontally(animationSpec = tween(220), targetOffsetX = { it / 12 })
        },
    ) {
        composable(CAPodRoutes.OVERVIEW) {
            val context = LocalContext.current
            OverviewScreen(
                uiState = uiState,
                snackbarHostState = snackbarHostState,
                onRequestPermissions = onRequestPermissions,
                onRequestEnableBluetooth = onRequestEnableBluetooth,
                onBluetoothUnavailable = viewModel::notifyBluetoothUnavailable,
                onToggleScanning = viewModel::toggleScanning,
                onOpenDiscovery = { navController.navigate(CAPodRoutes.BLE_DISCOVERY) },
                onOpenAnalytics = {
                    val primaryId = uiState.matchedDevices.firstOrNull()?.profile?.id ?: 0L
                    navController.navigate(CAPodRoutes.batteryAnalytics(primaryId))
                },
                onOpenDevices = { navController.navigate(CAPodRoutes.DEVICES) },
                onOpenSettings = { navController.navigate(CAPodRoutes.SETTINGS) },
                onAddProfile = { navController.navigate(CAPodRoutes.profileEditor(0L)) },
                onOpenDeviceDetail = { id -> navController.navigate(CAPodRoutes.deviceDetail(id)) },
                onEditProfile = { id -> navController.navigate(CAPodRoutes.profileEditor(id)) },
                onRenameDevice = viewModel::renameDevice,
                onResetDeviceName = viewModel::resetDeviceName,
                onQuickToggleAncMode = viewModel::toggleAncQuickMode,
                onSelectListeningMode = viewModel::selectListeningMode,
                onPinAncQuickTile = { matched ->
                    AncQuickSettingsTile.requestAddTile(
                        context = context,
                        profileId = matched.profile.id,
                        deviceDisplayName = matched.displayName,
                        currentMode = matched.profile.preferredListeningMode,
                    ) { result ->
                        viewModel.handleAncQuickTilePinResult(
                            profileId = matched.profile.id,
                            displayName = matched.displayName,
                            result = result,
                        )
                    }
                },
                onToggleUnmatchedExpanded = viewModel::toggleUnmatchedExpanded,
                onQuickAddUnmatched = viewModel::quickAddFromUnmatchedSnapshot,
                onQuickAddBonded = viewModel::quickAddFromBondedDevice,
                onInspectUnmatched = { navController.navigate(CAPodRoutes.UNMATCHED) },
                onDismissPopup = viewModel::dismissPopup
            )
        }

        composable(CAPodRoutes.BLE_DISCOVERY) {
            val isDiscovering by viewModel.isDiscovering.collectAsStateWithLifecycle()
            val discoveredDevices by viewModel.discoveredDevices.collectAsStateWithLifecycle()

            LaunchedEffect(uiState.hardwareState) {
                if (uiState.hardwareState == BluetoothHardwareState.READY) {
                    viewModel.startBleDiscovery(clearPrevious = false)
                }
            }

            DisposableEffect(Unit) {
                onDispose { viewModel.stopBleDiscovery() }
            }

            BleDiscoveryScreen(
                hardwareState = uiState.hardwareState,
                isDiscovering = isDiscovering,
                discoveredDevices = discoveredDevices,
                snackbarHostState = snackbarHostState,
                onRequestPermissions = onRequestPermissions,
                onRequestEnableBluetooth = onRequestEnableBluetooth,
                onStartDiscovery = viewModel::startBleDiscovery,
                onStopDiscovery = viewModel::stopBleDiscovery,
                onPairDevice = viewModel::pairBleDevice,
                onAddProfileForDevice = viewModel::quickAddFromDiscoveredDevice,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = CAPodRoutes.BATTERY_ANALYTICS,
            arguments = listOf(
                navArgument("profileId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) { backStackEntry ->
            val initialProfileId = backStackEntry.arguments?.getLong("profileId") ?: 0L
            var selectedProfileId by rememberSaveable(initialProfileId) {
                mutableStateOf(initialProfileId.takeIf { it > 0L })
            }
            var selectedRange by rememberSaveable { mutableStateOf(AnalyticsTimeRange.SIX_HOURS) }

            val effectiveProfileId = selectedProfileId
                ?: uiState.matchedDevices.firstOrNull()?.profile?.id
                ?: 0L

            val logsFlow = remember(effectiveProfileId, selectedRange) {
                viewModel.observeBatteryHistorySince(effectiveProfileId, selectedRange)
            }
            val logs by logsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

            BatteryAnalyticsScreen(
                matchedDevices = uiState.matchedDevices,
                selectedProfileId = effectiveProfileId.takeIf { it > 0L },
                selectedRange = selectedRange,
                logsForSelectedProfile = logs,
                onSelectProfile = { selectedProfileId = it },
                onSelectRange = { selectedRange = it },
                onAddProfile = { navController.navigate(CAPodRoutes.profileEditor(0L)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(CAPodRoutes.DEVICES) {
            DevicesScreen(
                uiState = uiState,
                snackbarHostState = snackbarHostState,
                onNavigateBack = { navController.popBackStack() },
                onOpenDiscovery = { navController.navigate(CAPodRoutes.BLE_DISCOVERY) },
                onOpenDeviceDetail = { id -> navController.navigate(CAPodRoutes.deviceDetail(id)) },
                onAddProfile = { navController.navigate(CAPodRoutes.profileEditor(0L)) },
                onEditProfile = { id -> navController.navigate(CAPodRoutes.profileEditor(id)) },
                onDeleteProfile = { id -> viewModel.deleteProfile(id) },
                onRenameDevice = viewModel::renameDevice,
                onResetDeviceName = viewModel::resetDeviceName,
                onRenameBondedDevice = viewModel::renameBluetoothDevice,
                onResetBondedDeviceName = viewModel::resetBluetoothDeviceName,
                onQuickAddBonded = viewModel::quickAddFromBondedDevice
            )
        }

        composable(
            route = CAPodRoutes.DEVICE_DETAIL,
            arguments = listOf(
                navArgument("profileId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) { backStackEntry ->
            val profileId = backStackEntry.arguments?.getLong("profileId") ?: 0L
            val deviceState = uiState.matchedDevices.firstOrNull { it.profile.id == profileId }
            val historyFlow = remember(profileId) { viewModel.observeBatteryHistory(profileId) }
            val batteryHistory by historyFlow.collectAsStateWithLifecycle(initialValue = emptyList())
            val activeChime by viewModel.activeChimeChannel.collectAsStateWithLifecycle()

            DisposableEffect(Unit) {
                onDispose { viewModel.stopLocatingChime() }
            }

            DeviceDetailScreen(
                deviceState = deviceState,
                batteryHistory = batteryHistory,
                activeChimeChannel = activeChime,
                snackbarHostState = snackbarHostState,
                onNavigateBack = { navController.popBackStack() },
                onEditProfile = { id -> navController.navigate(CAPodRoutes.profileEditor(id)) },
                onRenameDevice = viewModel::renameDevice,
                onResetDeviceName = viewModel::resetDeviceName,
                onOpenAnalytics = { id -> navController.navigate(CAPodRoutes.batteryAnalytics(id)) },
                onSelectListeningMode = { mode ->
                    if (deviceState != null) {
                        viewModel.selectListeningMode(deviceState.profile, mode)
                    }
                },
                onSelectMicrophoneMode = { mode ->
                    if (deviceState != null) {
                        viewModel.setMicrophoneMode(deviceState.profile, mode)
                    }
                },
                onSetConversationalAwareness = { enabled ->
                    if (deviceState != null) {
                        viewModel.setConversationalAwareness(deviceState.profile, enabled)
                    }
                },
                onSetPersonalizedVolume = { enabled ->
                    if (deviceState != null) {
                        viewModel.setPersonalizedVolume(deviceState.profile, enabled)
                    }
                },
                onSetAllowAncOnePod = { enabled ->
                    if (deviceState != null) {
                        viewModel.setAllowAncOnePod(deviceState.profile, enabled)
                    }
                },
                onSetAdaptiveNoiseLevel = { level ->
                    if (deviceState != null) {
                        viewModel.setAdaptiveNoiseLevel(deviceState.profile, level)
                    }
                },
                onCheckFirmware = { profile ->
                    viewModel.checkFirmwareForProfile(profile, isAutomatic = false)
                },
                onPlayChime = viewModel::playLocatingChime,
                onStopChime = viewModel::stopLocatingChime,
                onClearHistory = viewModel::clearBatteryHistoryForProfile
            )
        }

        composable(
            route = CAPodRoutes.PROFILE_EDITOR,
            arguments = listOf(
                navArgument("profileId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) { backStackEntry ->
            val profileId = backStackEntry.arguments?.getLong("profileId") ?: 0L
            ProfileEditorScreen(
                editingProfileId = profileId.takeIf { it > 0L },
                bondedDevices = uiState.bondedDevices,
                onLoadProfile = { id -> viewModel.loadProfile(id) },
                onSave = { profile ->
                    viewModel.saveProfile(profile) {
                        navController.popBackStack()
                    }
                },
                onDelete = { id ->
                    viewModel.deleteProfile(id) {
                        val poppedDetail = navController.previousBackStackEntry
                            ?.destination
                            ?.route == CAPodRoutes.DEVICE_DETAIL
                        if (poppedDetail) {
                            navController.popBackStack(CAPodRoutes.OVERVIEW, inclusive = false)
                        } else {
                            navController.popBackStack()
                        }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(CAPodRoutes.UNMATCHED) {
            UnmatchedDetailScreen(
                unmatchedDevices = uiState.unmatchedDevices,
                snackbarHostState = snackbarHostState,
                onQuickAddProfile = { snapshot ->
                    viewModel.quickAddFromUnmatchedSnapshot(snapshot)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(CAPodRoutes.SETTINGS) {
            val activeBtName = uiState.matchedDevices
                .firstOrNull { it.isBluetoothConnected && !it.profile.linkedBluetoothAddress.isNullOrBlank() }
                ?.displayName
                ?: uiState.matchedDevices
                    .firstOrNull { !it.profile.linkedBluetoothAddress.isNullOrBlank() }
                    ?.displayName
                ?: uiState.bondedDevices.firstOrNull { it.isConnected }?.displayName
                ?: uiState.bondedDevices.firstOrNull()?.displayName

            SettingsScreen(
                settings = uiState.settings,
                earDetectionSyncState = uiState.earDetectionSyncState,
                activeBluetoothDeviceName = activeBtName,
                snackbarHostState = snackbarHostState,
                onSetThemeMode = viewModel::setThemeMode,
                onSetDynamicColor = viewModel::setDynamicColor,
                onSetScanMode = viewModel::setScanMode,
                onSetShowUnmatched = viewModel::setShowUnmatchedDevices,
                onSetAutomaticEarDetection = viewModel::setAutomaticEarDetectionEnabled,
                onSyncEarDetectionGatt = viewModel::syncAutomaticEarDetectionNow,
                onSetShowPopup = viewModel::setShowConnectionPopup,
                onSetBatteryNotification = viewModel::setBatteryNotificationEnabled,
                onSetFirmwareAlerts = viewModel::setFirmwareAlertsEnabled,
                onNotificationPermissionDenied = viewModel::notifyNotificationPermissionDenied,
                onWidgetPinResult = viewModel::notifyWidgetPinResult,
                onClearAllBatteryHistory = viewModel::clearAllBatteryHistory,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
