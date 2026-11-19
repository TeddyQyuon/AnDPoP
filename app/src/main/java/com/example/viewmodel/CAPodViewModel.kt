package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluetooth.AppleBleParser
import com.example.bluetooth.BatteryNotificationHelper
import com.example.bluetooth.ChimeChannel
import com.example.bluetooth.EarDetectionGattResult
import com.example.bluetooth.EarDetectionSyncUiState
import com.example.bluetooth.FirmwareNotificationHelper
import com.example.bluetooth.FirmwareReadResult
import com.example.bluetooth.PodBleScanner
import com.example.bluetooth.PodChimePlayer
import com.example.bluetooth.PodControlClient
import com.example.bluetooth.PodEarDetectionGattClient
import com.example.bluetooth.PodFirmwareReader
import com.example.data.BatteryLogEntity
import com.example.data.CAPodDatabase
import com.example.data.CustomDeviceNames
import com.example.data.DeviceNameRepository
import com.example.data.DeviceProfileEntity
import com.example.data.DeviceProfileRepository
import com.example.data.SettingsRepository
import com.example.model.AnalyticsTimeRange
import com.example.model.AppSettings
import com.example.model.BlePodSnapshot
import com.example.model.BleScanMode
import com.example.model.BluetoothHardwareState
import com.example.model.BondedBluetoothDevice
import com.example.model.DiscoveredBleDevice
import com.example.model.FirmwareCatalog
import com.example.model.FirmwareEvaluation
import com.example.model.ListeningMode
import com.example.model.PodFormFactor
import com.example.model.PodModel
import com.example.model.ThemeMode
import com.example.service.AncQuickSettingsTile
import com.example.service.AncTilePinResult
import com.example.service.CAPodQuickSettingsTile
import com.example.widget.CAPodBatteryWidgetProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class MatchedDeviceState(
    val profile: DeviceProfileEntity,
    val liveSnapshot: BlePodSnapshot?,
    val bondedDevice: BondedBluetoothDevice?,
    val customName: String? = null,
    val isUpdatingAnc: Boolean = false,
    val isCheckingFirmware: Boolean = false,
) {
    val displayName: String
        get() = customName?.takeIf { it.isNotBlank() } ?: profile.name

    val hasCustomName: Boolean
        get() = !customName.isNullOrBlank()

    val isLive: Boolean get() = liveSnapshot != null
    val isBluetoothConnected: Boolean get() = bondedDevice?.isConnected == true

    val firmwareEvaluation: FirmwareEvaluation
        get() = FirmwareCatalog.evaluate(
            model = profile.model,
            currentVersion = profile.lastFirmwareVersion,
            lastCheckedMillis = profile.lastFirmwareCheckMillis,
        )

    /**
     * Best-effort battery readout for single-unit headsets: prefers live BLE Continuity packet,
     * falling back to Android Classic Bluetooth battery level if bonded & connected.
     */
    val effectiveHeadsetBattery: Int?
        get() = liveSnapshot?.headsetBattery
            ?: if (profile.model.formFactor == PodFormFactor.SINGLE_HEADSET) {
                bondedDevice?.classicBatteryPercent
            } else {
                null
            }
}

data class CAPodUiState(
    val hardwareState: BluetoothHardwareState = BluetoothHardwareState.PERMISSION_REQUIRED,
    val isScanning: Boolean = false,
    val isScanPausedByUser: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val matchedDevices: List<MatchedDeviceState> = emptyList(),
    val unmatchedDevices: List<BlePodSnapshot> = emptyList(),
    val bondedDevices: List<BondedBluetoothDevice> = emptyList(),
    val isUnmatchedExpanded: Boolean = false,
    val popupDevice: MatchedDeviceState? = null,
    val ancQuickTileProfileId: Long = 0L,
    val earDetectionSyncState: EarDetectionSyncUiState = EarDetectionSyncUiState.Idle,
)

class CAPodViewModel(application: Application) : AndroidViewModel(application) {

    private val database = CAPodDatabase.getInstance(application)
    private val profileRepository = DeviceProfileRepository(database.deviceProfileDao())
    private val batteryLogDao = database.batteryLogDao()
    private val settingsRepository = SettingsRepository(application)
    private val deviceNameRepository = DeviceNameRepository(application)
    val bleScanner = PodBleScanner(application)
    private val controlClient = PodControlClient(application)
    private val chimePlayer = PodChimePlayer()
    private val notificationHelper = BatteryNotificationHelper(application)
    private val firmwareReader = PodFirmwareReader(application)
    private val firmwareNotificationHelper = FirmwareNotificationHelper(application)
    private val earDetectionGattClient = PodEarDetectionGattClient(application)

    private val isScanPausedByUser = MutableStateFlow(false)
    private val isUnmatchedExpanded = MutableStateFlow(false)
    private val updatingAncProfileId = MutableStateFlow<Long?>(null)
    private val checkingFirmwareProfileId = MutableStateFlow<Long?>(null)
    private val popupProfileId = MutableStateFlow<Long?>(null)
    private val ancQuickTileProfileId = MutableStateFlow(
        AncQuickSettingsTile.readTargetProfileId(application)
    )
    private val earDetectionSyncState = MutableStateFlow<EarDetectionSyncUiState>(
        EarDetectionSyncUiState.Idle
    )

    private val _activeChimeChannel = MutableStateFlow<ChimeChannel?>(null)
    val activeChimeChannel: StateFlow<ChimeChannel?> = _activeChimeChannel.asStateFlow()
    private var chimeJob: Job? = null

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    // Tracks ear-detection, lid-open, firmware checks, and battery history log throttling per profile ID
    private val previousInEarState = mutableMapOf<Long, Boolean>()
    private val previousLidOpenState = mutableMapOf<Long, Boolean>()
    private val lastLoggedBatteryKey = mutableMapOf<Long, String>()
    private val lastLoggedTimestamp = mutableMapOf<Long, Long>()
    private val autoCheckedFirmwareProfileIds = mutableSetOf<Long>()
    private val notifiedFirmwareKeys = mutableSetOf<String>()

    private data class CoreInputs(
        val profiles: List<DeviceProfileEntity>,
        val snapshots: Map<String, BlePodSnapshot>,
        val bonded: List<BondedBluetoothDevice>,
        val settings: AppSettings,
        val hardwareState: BluetoothHardwareState,
    )

    private data class UiFlags(
        val isScanning: Boolean,
        val isPaused: Boolean,
        val isUnmatchedExpanded: Boolean,
        val updatingAncId: Long?,
        val checkingFirmwareId: Long?,
        val popupProfileId: Long?,
    )

    val uiState: StateFlow<CAPodUiState> = combine(
        combine(
            profileRepository.profiles,
            bleScanner.nearbySnapshots,
            bleScanner.bondedDevices,
            settingsRepository.settings,
            bleScanner.hardwareState,
        ) { profiles, snapshots, bonded, settings, hwState ->
            CoreInputs(profiles, snapshots, bonded, settings, hwState)
        },
        combine(
            bleScanner.isScanning,
            isScanPausedByUser,
            isUnmatchedExpanded,
            combine(updatingAncProfileId, checkingFirmwareProfileId) { ancId, fwId -> ancId to fwId },
            popupProfileId,
        ) { scanning, paused, unmatchedExp, (ancId, fwId), popupId ->
            UiFlags(scanning, paused, unmatchedExp, ancId, fwId, popupId)
        },
        combine(
            deviceNameRepository.customNames,
            ancQuickTileProfileId,
            earDetectionSyncState,
        ) { names, qsProfileId, earSync ->
            Triple(names, qsProfileId, earSync)
        },
    ) { core, flags, (customNames, qsProfileId, earSync) ->
        val renamedBonded = core.bonded.map { device ->
            val override = customNames.resolveForBluetoothAddress(device.address)
            if (override != null) device.copy(customName = override) else device
        }
        val (matched, unmatched) = matchProfilesToSnapshots(
            profiles = core.profiles,
            snapshots = core.snapshots.values.toList(),
            bondedDevices = renamedBonded,
            customNames = customNames,
            updatingAncId = flags.updatingAncId,
            checkingFirmwareId = flags.checkingFirmwareId,
        )

        handleTelemetrySideEffects(matched, core.settings)

        val popupDevice = flags.popupProfileId?.let { targetId ->
            matched.firstOrNull { it.profile.id == targetId && it.isLive }
        }

        val resolvedQsProfileId = AncQuickSettingsTile.resolveActiveAncProfile(
            profiles = core.profiles,
            preferredProfileId = qsProfileId,
        )?.id ?: 0L

        CAPodUiState(
            hardwareState = core.hardwareState,
            isScanning = flags.isScanning,
            isScanPausedByUser = flags.isPaused,
            settings = core.settings,
            matchedDevices = matched,
            unmatchedDevices = unmatched,
            bondedDevices = renamedBonded,
            isUnmatchedExpanded = flags.isUnmatchedExpanded,
            popupDevice = popupDevice,
            ancQuickTileProfileId = resolvedQsProfileId,
            earDetectionSyncState = earSync,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CAPodUiState()
    )

    val isDiscovering: StateFlow<Boolean> = bleScanner.isDiscovering

    val discoveredDevices: StateFlow<List<DiscoveredBleDevice>> = combine(
        bleScanner.discoveredDevices,
        deviceNameRepository.customNames,
    ) { map, customNames ->
        map.values.map { device ->
            val override = customNames.resolveForBluetoothAddress(device.address)
            if (override != null) device.copy(name = override) else device
        }.sortedWith(
            compareByDescending<DiscoveredBleDevice> { it.isBonding }
                .thenByDescending { it.isBonded }
                .thenByDescending { it.hasName }
                .thenByDescending { it.rssi }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    init {
        bleScanner.registerLifecycleReceivers()

        // Forward real Android Bluetooth bond state updates to snackbar feedback
        viewModelScope.launch {
            bleScanner.bondEvents.collect { message ->
                _snackbarEvents.tryEmit(message)
            }
        }

        // React to scan mode changes when scanning is active
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                if (bleScanner.isScanning.value && !isScanPausedByUser.value) {
                    bleScanner.startScanning(settings.scanMode)
                }
                if (!settings.batteryNotificationEnabled) {
                    notificationHelper.clearNotification()
                }
            }
        }

        // Periodically prune stale BLE packets so cards don't show outdated telemetry
        viewModelScope.launch {
            while (isActive) {
                delay(5_000L)
                bleScanner.pruneStaleSnapshots()
            }
        }
    }

    fun startBleDiscovery(clearPrevious: Boolean = false) {
        bleScanner.startGeneralDiscovery(clearPrevious = clearPrevious)
    }

    fun stopBleDiscovery() {
        bleScanner.stopGeneralDiscovery()
    }

    fun pairBleDevice(device: DiscoveredBleDevice) {
        val result = bleScanner.initiatePairing(device.address)
        result.onSuccess {
            val msg = if (device.isBonded) {
                "${device.displayName} is already paired"
            } else {
                "Pairing requested with ${device.displayName}"
            }
            _snackbarEvents.tryEmit(msg)
        }.onFailure { err ->
            _snackbarEvents.tryEmit(err.message ?: "Unable to start pairing")
        }
    }

    fun quickAddFromDiscoveredDevice(device: DiscoveredBleDevice) {
        val model = device.detectedPodModel ?: PodModel.guessFromName(device.displayName)
        val profile = DeviceProfileEntity(
            name = device.displayName,
            modelId = model.id,
            linkedBluetoothAddress = device.address,
            linkedBluetoothName = device.displayName,
            minRssiDbm = (device.rssi - 15).coerceIn(-95, -55),
            preferredListeningModeId = if (model.supportsAdaptive) {
                ListeningMode.ADAPTIVE.id
            } else if (model.supportsAnc) {
                ListeningMode.NOISE_CANCELLATION.id
            } else {
                ListeningMode.OFF.id
            }
        )
        saveProfile(profile)
    }

    fun onPermissionsResult(fromUserPrompt: Boolean = false) {
        bleScanner.refreshStateAndBondedDevices()
        when (bleScanner.hardwareState.value) {
            BluetoothHardwareState.READY -> {
                if (!isScanPausedByUser.value) {
                    startScanningWithCurrentMode()
                }
            }
            BluetoothHardwareState.PERMISSION_REQUIRED -> {
                if (fromUserPrompt) {
                    _snackbarEvents.tryEmit(
                        "Bluetooth permission is required to scan nearby AirPods and Beats."
                    )
                }
            }
            BluetoothHardwareState.BLUETOOTH_DISABLED -> {
                if (fromUserPrompt) {
                    _snackbarEvents.tryEmit("Turn on Bluetooth to start scanning.")
                }
            }
            BluetoothHardwareState.UNAVAILABLE -> {
                if (fromUserPrompt) {
                    notifyBluetoothUnavailable()
                }
            }
        }
    }

    fun notifyBluetoothUnavailable() {
        _snackbarEvents.tryEmit(
            "Bluetooth LE hardware is unavailable on this device. You can still manage device profiles."
        )
    }

    fun onForegroundResumed() {
        bleScanner.refreshStateAndBondedDevices()
        if (bleScanner.hardwareState.value == BluetoothHardwareState.READY && !isScanPausedByUser.value) {
            startScanningWithCurrentMode()
        }
    }

    fun toggleScanning() {
        if (bleScanner.isScanning.value) {
            isScanPausedByUser.value = true
            bleScanner.stopScanning()
        } else {
            isScanPausedByUser.value = false
            startScanningWithCurrentMode()
        }
    }

    private fun startScanningWithCurrentMode() {
        viewModelScope.launch {
            val mode = settingsRepository.settings.first().scanMode
            bleScanner.startScanning(mode)
        }
    }

    fun toggleUnmatchedExpanded() {
        isUnmatchedExpanded.value = !isUnmatchedExpanded.value
    }

    fun dismissPopup() {
        popupProfileId.value = null
    }

    fun saveProfile(profile: DeviceProfileEntity, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            profileRepository.save(profile)
            onSaved()
            _snackbarEvents.tryEmit("Saved profile \"${profile.name}\"")
        }
    }

    fun deleteProfile(profileId: Long, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            profileRepository.deleteById(profileId)
            previousInEarState.remove(profileId)
            previousLidOpenState.remove(profileId)
            autoCheckedFirmwareProfileIds.remove(profileId)
            onDeleted()
            _snackbarEvents.tryEmit("Device profile removed")
        }
    }

    suspend fun loadProfile(profileId: Long): DeviceProfileEntity? {
        return profileRepository.getById(profileId)
    }

    fun quickAddFromBondedDevice(device: BondedBluetoothDevice) {
        val guessedModel = PodModel.guessFromName(device.name)
        val profile = DeviceProfileEntity(
            name = device.name,
            modelId = guessedModel.id,
            linkedBluetoothAddress = device.address,
            linkedBluetoothName = device.name,
            preferredListeningModeId = if (guessedModel.supportsAdaptive) {
                ListeningMode.ADAPTIVE.id
            } else if (guessedModel.supportsAnc) {
                ListeningMode.NOISE_CANCELLATION.id
            } else {
                ListeningMode.OFF.id
            }
        )
        saveProfile(profile)
    }

    fun quickAddFromUnmatchedSnapshot(snapshot: BlePodSnapshot) {
        val model = if (snapshot.model == PodModel.UNKNOWN) PodModel.AIRPODS_PRO_2 else snapshot.model
        val profile = DeviceProfileEntity(
            name = model.displayName,
            modelId = model.id,
            minRssiDbm = (snapshot.rssi - 12).coerceIn(-95, -55),
            preferredListeningModeId = if (model.supportsAdaptive) {
                ListeningMode.ADAPTIVE.id
            } else if (model.supportsAnc) {
                ListeningMode.NOISE_CANCELLATION.id
            } else {
                ListeningMode.OFF.id
            }
        )
        saveProfile(profile)
    }

    fun toggleAncQuickMode(profile: DeviceProfileEntity) {
        val nextMode = profile.preferredListeningMode.nextQuickToggleMode()
        selectListeningMode(profile, nextMode)
    }

    fun handleAncQuickTilePinResult(
        profileId: Long,
        displayName: String,
        result: AncTilePinResult,
    ) {
        ancQuickTileProfileId.value = profileId
        val appContext = getApplication<Application>()
        val message = when (result) {
            AncTilePinResult.ADDED ->
                appContext.getString(com.example.R.string.qs_tile_added_snackbar, displayName)
            AncTilePinResult.ALREADY_ADDED ->
                appContext.getString(com.example.R.string.qs_tile_already_added_snackbar, displayName)
            AncTilePinResult.MANUAL_INSTRUCTIONS ->
                appContext.getString(com.example.R.string.qs_tile_manual_hint_snackbar, displayName)
            AncTilePinResult.DISMISSED -> null
        }
        if (message != null) {
            _snackbarEvents.tryEmit(message)
        }
    }

    fun selectListeningMode(profile: DeviceProfileEntity, mode: ListeningMode) {
        viewModelScope.launch {
            updatingAncProfileId.value = profile.id
            profileRepository.updateListeningMode(profile.id, mode.id)
            val displayName = deviceNameRepository.customNames.first()
                .resolveForProfile(profile.id, profile.linkedBluetoothAddress)
                ?: profile.name
            AncQuickSettingsTile.syncAndRequestRefresh(
                context = getApplication(),
                profileId = profile.id,
                deviceDisplayName = displayName,
                mode = mode,
            )
            val result = controlClient.sendListeningMode(profile.linkedBluetoothAddress, mode)
            updatingAncProfileId.value = null
            result.onSuccess {
                _snackbarEvents.tryEmit("$displayName: switched listening mode")
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.message ?: "Saved mode preference; connect pods to apply.")
            }
        }
    }

    // Settings updates
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        settingsRepository.setThemeMode(mode)
    }

    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setDynamicColor(enabled)
    }

    fun setScanMode(mode: BleScanMode) = viewModelScope.launch {
        settingsRepository.setScanMode(mode)
    }

    fun setShowUnmatchedDevices(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setShowUnmatchedDevices(enabled)
    }

    fun setShowConnectionPopup(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setShowConnectionPopup(enabled)
    }

    fun checkFirmwareForProfile(
        profile: DeviceProfileEntity,
        isAutomatic: Boolean = false,
    ) {
        if (checkingFirmwareProfileId.value == profile.id) return

        viewModelScope.launch {
            checkingFirmwareProfileId.value = profile.id
            try {
                val result = firmwareReader.readFirmwareVersion(profile.linkedBluetoothAddress)
                when (result) {
                    is FirmwareReadResult.Success -> {
                        val checkedAt = System.currentTimeMillis()
                        profileRepository.updateFirmwareInfo(
                            id = profile.id,
                            version = result.version,
                            checkedAtMillis = checkedAt,
                        )
                        val evaluation = FirmwareCatalog.evaluate(
                            model = profile.model,
                            currentVersion = result.version,
                            lastCheckedMillis = checkedAt,
                        )
                        val alertsEnabled = settingsRepository.settings.first().firmwareAlertsEnabled
                        val displayName = deviceNameRepository.customNames.first()
                            .resolveForProfile(profile.id, profile.linkedBluetoothAddress)
                            ?: profile.name
                        if (evaluation.isUpdateAvailable) {
                            val notificationKey =
                                "${profile.id}:${result.version}:${evaluation.latestVersion}"
                            val shouldPost = !isAutomatic ||
                                (alertsEnabled && notifiedFirmwareKeys.add(notificationKey))
                            if (shouldPost) {
                                notifiedFirmwareKeys.add(notificationKey)
                                firmwareNotificationHelper.postUpdateAvailableNotification(
                                    profileId = profile.id,
                                    deviceName = displayName,
                                    currentVersion = result.version,
                                    latestVersion = evaluation.latestVersion,
                                    highlights = evaluation.releaseHighlights,
                                )
                            }
                            if (!isAutomatic) {
                                _snackbarEvents.tryEmit(
                                    "Update available for $displayName: ${result.version} → ${evaluation.latestVersion}"
                                )
                            }
                        } else if (!isAutomatic) {
                            _snackbarEvents.tryEmit(
                                "$displayName firmware ${result.version} is up to date (${result.source})"
                            )
                        }
                    }

                    is FirmwareReadResult.Failure -> {
                        if (!isAutomatic) {
                            _snackbarEvents.tryEmit(result.reason)
                        }
                    }
                }
            } finally {
                if (checkingFirmwareProfileId.value == profile.id) {
                    checkingFirmwareProfileId.value = null
                }
            }
        }
    }

    fun setBatteryNotificationEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setBatteryNotificationEnabled(enabled)
        if (!enabled) {
            notificationHelper.clearNotification()
        }
    }

    fun setFirmwareAlertsEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setFirmwareAlertsEnabled(enabled)
    }

    fun setAutomaticEarDetectionEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setAutomaticEarDetectionEnabled(enabled)
        dispatchEarDetectionOverGatt(enabled)
    }

    fun syncAutomaticEarDetectionNow() = viewModelScope.launch {
        val enabled = settingsRepository.settings.first().automaticEarDetectionEnabled
        dispatchEarDetectionOverGatt(enabled)
    }

    private suspend fun dispatchEarDetectionOverGatt(enabled: Boolean) {
        val currentState = uiState.value
        val preferredMatch = currentState.matchedDevices.firstOrNull {
            it.isBluetoothConnected && !it.profile.linkedBluetoothAddress.isNullOrBlank()
        } ?: currentState.matchedDevices.firstOrNull {
            !it.profile.linkedBluetoothAddress.isNullOrBlank()
        }
        val fallbackBonded = currentState.bondedDevices.firstOrNull { it.isConnected }
            ?: currentState.bondedDevices.firstOrNull()

        val targetAddress = preferredMatch?.profile?.linkedBluetoothAddress
            ?: fallbackBonded?.address
        val targetName = preferredMatch?.displayName
            ?: fallbackBonded?.displayName
            ?: "Bluetooth headphones"

        earDetectionSyncState.value = EarDetectionSyncUiState.Syncing(
            targetDeviceName = targetAddress?.let { targetName }
        )

        val stateLabel = if (enabled) "enabled" else "disabled"
        when (val result = earDetectionGattClient.writeEarDetectionState(targetAddress, targetName, enabled)) {
            is EarDetectionGattResult.SyncedViaGatt -> {
                earDetectionSyncState.value = EarDetectionSyncUiState.SyncedGatt(
                    deviceName = result.deviceName,
                    enabled = enabled,
                )
                _snackbarEvents.tryEmit(
                    "Automatic Ear Detection $stateLabel on ${result.deviceName} via BLE GATT"
                )
            }

            is EarDetectionGattResult.SyncedViaAapFallback -> {
                earDetectionSyncState.value = EarDetectionSyncUiState.SyncedAap(
                    deviceName = result.deviceName,
                    enabled = enabled,
                )
                _snackbarEvents.tryEmit(
                    "Automatic Ear Detection $stateLabel on ${result.deviceName} via Bluetooth"
                )
            }

            is EarDetectionGattResult.SavedLocallyOnly -> {
                earDetectionSyncState.value = EarDetectionSyncUiState.SavedLocally(
                    reason = result.reason,
                    enabled = enabled,
                )
                _snackbarEvents.tryEmit(
                    "Automatic Ear Detection $stateLabel (${result.reason})"
                )
            }
        }
    }

    fun renameDevice(profile: DeviceProfileEntity, newName: String) {
        val cleaned = newName.trim().take(DeviceNameRepository.MAX_NAME_LENGTH)
        viewModelScope.launch {
            if (cleaned.isEmpty()) {
                deviceNameRepository.clearCustomName(
                    bluetoothAddress = profile.linkedBluetoothAddress,
                    profileId = profile.id,
                )
                if (profile.model.supportsAnc) {
                    AncQuickSettingsTile.syncAndRequestRefresh(
                        context = getApplication(),
                        profileId = profile.id,
                        deviceDisplayName = profile.name,
                        mode = profile.preferredListeningMode,
                    )
                }
                _snackbarEvents.tryEmit("Reset device name to \"${profile.name}\"")
            } else {
                deviceNameRepository.saveCustomName(
                    bluetoothAddress = profile.linkedBluetoothAddress,
                    profileId = profile.id,
                    customName = cleaned,
                )
                if (profile.model.supportsAnc) {
                    AncQuickSettingsTile.syncAndRequestRefresh(
                        context = getApplication(),
                        profileId = profile.id,
                        deviceDisplayName = cleaned,
                        mode = profile.preferredListeningMode,
                    )
                }
                _snackbarEvents.tryEmit("Renamed device to \"$cleaned\"")
            }
        }
    }

    fun resetDeviceName(profile: DeviceProfileEntity) {
        viewModelScope.launch {
            deviceNameRepository.clearCustomName(
                bluetoothAddress = profile.linkedBluetoothAddress,
                profileId = profile.id,
            )
            if (profile.model.supportsAnc) {
                AncQuickSettingsTile.syncAndRequestRefresh(
                    context = getApplication(),
                    profileId = profile.id,
                    deviceDisplayName = profile.name,
                    mode = profile.preferredListeningMode,
                )
            }
            _snackbarEvents.tryEmit("Reset device name to \"${profile.name}\"")
        }
    }

    fun renameBluetoothDevice(address: String, defaultName: String, newName: String) {
        val cleaned = newName.trim().take(DeviceNameRepository.MAX_NAME_LENGTH)
        viewModelScope.launch {
            if (cleaned.isEmpty()) {
                deviceNameRepository.clearCustomName(bluetoothAddress = address)
                _snackbarEvents.tryEmit("Reset device name to \"$defaultName\"")
            } else {
                deviceNameRepository.saveCustomName(
                    bluetoothAddress = address,
                    customName = cleaned,
                )
                _snackbarEvents.tryEmit("Renamed device to \"$cleaned\"")
            }
        }
    }

    fun resetBluetoothDeviceName(address: String, defaultName: String) {
        viewModelScope.launch {
            deviceNameRepository.clearCustomName(bluetoothAddress = address)
            _snackbarEvents.tryEmit("Reset device name to \"$defaultName\"")
        }
    }

    fun observeBatteryHistory(profileId: Long): Flow<List<BatteryLogEntity>> =
        batteryLogDao.observeRecentForProfile(profileId, limit = 60)

    fun observeBatteryHistorySince(
        profileId: Long,
        range: AnalyticsTimeRange,
    ): Flow<List<BatteryLogEntity>> {
        val cutoff = System.currentTimeMillis() - range.durationMillis
        return batteryLogDao.observeSince(profileId, cutoff)
    }

    fun clearBatteryHistoryForProfile(profileId: Long) = viewModelScope.launch {
        batteryLogDao.clearForProfile(profileId)
        lastLoggedBatteryKey.remove(profileId)
        lastLoggedTimestamp.remove(profileId)
        _snackbarEvents.tryEmit("Battery history cleared")
    }

    fun clearAllBatteryHistory() = viewModelScope.launch {
        batteryLogDao.clearAll()
        lastLoggedBatteryKey.clear()
        lastLoggedTimestamp.clear()
        _snackbarEvents.tryEmit("All battery history cleared")
    }

    fun playLocatingChime(channel: ChimeChannel) {
        chimeJob?.cancel()
        _activeChimeChannel.value = channel
        chimeJob = viewModelScope.launch {
            try {
                chimePlayer.playLocatingChime(channel)
            } catch (_: Exception) {
                // Playback interrupted or unavailable
            } finally {
                _activeChimeChannel.value = null
            }
        }
    }

    fun stopLocatingChime() {
        chimeJob?.cancel()
        chimeJob = null
        _activeChimeChannel.value = null
    }

    private fun matchProfilesToSnapshots(
        profiles: List<DeviceProfileEntity>,
        snapshots: List<BlePodSnapshot>,
        bondedDevices: List<BondedBluetoothDevice>,
        customNames: CustomDeviceNames,
        updatingAncId: Long?,
        checkingFirmwareId: Long?,
    ): Pair<List<MatchedDeviceState>, List<BlePodSnapshot>> {
        val remaining = snapshots.sortedByDescending { it.rssi }.toMutableList()
        val matchedStates = mutableListOf<MatchedDeviceState>()

        for (profile in profiles) {
            val bonded = profile.linkedBluetoothAddress?.let { addr ->
                bondedDevices.firstOrNull { it.address.equals(addr, ignoreCase = true) }
            }

            // 1. IRK cryptographic RPA verification match
            var matchedIndex = if (!profile.identityResolvingKeyHex.isNullOrBlank()) {
                remaining.indexOfFirst { snap ->
                    snap.rssi >= profile.minRssiDbm &&
                        AppleBleParser.verifyRpaWithIrk(snap.address, profile.identityResolvingKeyHex)
                }
            } else {
                -1
            }

            var irkVerified = matchedIndex >= 0

            // 2. Direct MAC address match (for non-randomized BLE devices)
            if (matchedIndex < 0 && !profile.linkedBluetoothAddress.isNullOrBlank()) {
                matchedIndex = remaining.indexOfFirst { snap ->
                    snap.rssi >= profile.minRssiDbm &&
                        snap.address.equals(profile.linkedBluetoothAddress, ignoreCase = true)
                }
            }

            // 3. Model code match above min RSSI threshold
            if (matchedIndex < 0) {
                matchedIndex = remaining.indexOfFirst { snap ->
                    snap.rssi >= profile.minRssiDbm && isCompatibleModel(profile.model, snap.model)
                }
            }

            val claimedSnapshot = if (matchedIndex >= 0) {
                remaining.removeAt(matchedIndex).copy(isIrkVerified = irkVerified)
            } else {
                null
            }

            val resolvedCustomName = customNames.resolveForProfile(
                profileId = profile.id,
                bluetoothAddress = profile.linkedBluetoothAddress,
            )

            matchedStates.add(
                MatchedDeviceState(
                    profile = profile,
                    liveSnapshot = claimedSnapshot,
                    bondedDevice = bonded,
                    customName = resolvedCustomName,
                    isUpdatingAnc = updatingAncId == profile.id,
                    isCheckingFirmware = checkingFirmwareId == profile.id,
                )
            )
        }

        return matchedStates to remaining
    }

    private fun isCompatibleModel(profileModel: PodModel, detectedModel: PodModel): Boolean {
        if (profileModel == detectedModel) return true
        // Treat USB-C and Lightning revisions of AirPods Pro 2 / AirPods Max as compatible
        if (profileModel in setOf(PodModel.AIRPODS_PRO_2, PodModel.AIRPODS_PRO_2_USBC) &&
            detectedModel in setOf(PodModel.AIRPODS_PRO_2, PodModel.AIRPODS_PRO_2_USBC)
        ) return true
        if (profileModel in setOf(PodModel.AIRPODS_MAX, PodModel.AIRPODS_MAX_USBC) &&
            detectedModel in setOf(PodModel.AIRPODS_MAX, PodModel.AIRPODS_MAX_USBC)
        ) return true
        return false
    }

    private fun handleTelemetrySideEffects(
        matched: List<MatchedDeviceState>,
        settings: AppSettings,
    ) {
        var postedNotification = false
        var updatedWidget = false
        val appContext = getApplication<Application>()

        val now = System.currentTimeMillis()
        for (item in matched) {
            val profile = item.profile

            // Automatic firmware check once per session when a linked device is actively connected
            if (settings.firmwareAlertsEnabled &&
                item.isBluetoothConnected &&
                !profile.linkedBluetoothAddress.isNullOrBlank() &&
                !autoCheckedFirmwareProfileIds.contains(profile.id)
            ) {
                val lastCheck = profile.lastFirmwareCheckMillis ?: 0L
                val isStale = (now - lastCheck) >= AUTO_FIRMWARE_CHECK_INTERVAL_MS
                if (isStale) {
                    autoCheckedFirmwareProfileIds.add(profile.id)
                    checkFirmwareForProfile(profile, isAutomatic = true)
                } else {
                    autoCheckedFirmwareProfileIds.add(profile.id)
                }
            }

            val snapshot = item.liveSnapshot ?: continue

            // Update Home Screen Widget & Quick Settings Tile from the primary live device
            if (!updatedWidget) {
                CAPodBatteryWidgetProvider.updateFromSnapshot(appContext, item.displayName, snapshot)
                CAPodQuickSettingsTile.requestRefresh(appContext)
                updatedWidget = true
            }

            // Record throttled real battery history samples (on % change or every 2 minutes)
            recordBatteryLogIfNeeded(profile.id, snapshot)

            // Live status notification for the first active matched device
            if (settings.batteryNotificationEnabled && !postedNotification) {
                notificationHelper.postBatteryUpdate(item.displayName, snapshot)
                postedNotification = true
            }

            // Case-open popup trigger
            val wasLidOpen = previousLidOpenState[profile.id] ?: false
            if (settings.showConnectionPopup && snapshot.isLidOpen && !wasLidOpen) {
                popupProfileId.value = profile.id
            }
            previousLidOpenState[profile.id] = snapshot.isLidOpen

            // Automatic Ear Detection (Pause / Resume media when globally enabled)
            val currentlyInEar = if (profile.model.formFactor == PodFormFactor.SINGLE_HEADSET) {
                snapshot.isLeftInEar
            } else if (profile.onePodModeEnabled) {
                snapshot.isLeftInEar || snapshot.isRightInEar
            } else {
                snapshot.isLeftInEar && snapshot.isRightInEar
            }

            val wasInEar = previousInEarState[profile.id]
            if (settings.automaticEarDetectionEnabled && wasInEar != null && wasInEar != currentlyInEar) {
                if (!currentlyInEar && profile.autoPauseEnabled) {
                    controlClient.dispatchMediaPause()
                } else if (currentlyInEar && profile.autoResumeEnabled) {
                    controlClient.dispatchMediaPlay()
                }
            }
            previousInEarState[profile.id] = currentlyInEar
        }

        if (!updatedWidget && matched.isNotEmpty()) {
            CAPodBatteryWidgetProvider.updateFromSnapshot(
                appContext,
                matched.first().displayName,
                null
            )
            CAPodQuickSettingsTile.requestRefresh(appContext)
        }

        val activeAncItem = matched.firstOrNull {
            it.profile.model.supportsAnc && it.profile.id == ancQuickTileProfileId.value
        } ?: matched.firstOrNull { it.profile.model.supportsAnc }
        if (activeAncItem != null) {
            AncQuickSettingsTile.syncAndRequestRefresh(
                context = appContext,
                profileId = activeAncItem.profile.id,
                deviceDisplayName = activeAncItem.displayName,
                mode = activeAncItem.profile.preferredListeningMode,
            )
        }

        if (!postedNotification && settings.batteryNotificationEnabled) {
            notificationHelper.clearNotification()
        }
    }

    private fun recordBatteryLogIfNeeded(profileId: Long, snapshot: BlePodSnapshot) {
        val now = snapshot.lastSeenMillis
        val key = "${snapshot.leftBattery}_${snapshot.rightBattery}_${snapshot.caseBattery}_${snapshot.headsetBattery}"
        val lastKey = lastLoggedBatteryKey[profileId]
        val lastTime = lastLoggedTimestamp[profileId] ?: 0L

        val shouldLog = (key != lastKey && (now - lastTime) >= 10_000L) ||
            (now - lastTime) >= 120_000L
        if (!shouldLog) return

        lastLoggedBatteryKey[profileId] = key
        lastLoggedTimestamp[profileId] = now

        viewModelScope.launch {
            batteryLogDao.insert(
                BatteryLogEntity(
                    profileId = profileId,
                    timestampMillis = now,
                    leftBattery = snapshot.leftBattery,
                    rightBattery = snapshot.rightBattery,
                    caseBattery = snapshot.caseBattery,
                    headsetBattery = snapshot.headsetBattery,
                    rssi = snapshot.rssi,
                )
            )
            // Keep 7 days of local battery history
            batteryLogDao.pruneOlderThan(now - 7L * 24L * 60L * 60L * 1000L)
        }
    }

    override fun onCleared() {
        stopLocatingChime()
        bleScanner.unregisterLifecycleReceivers()
        super.onCleared()
    }

    companion object {
        private const val AUTO_FIRMWARE_CHECK_INTERVAL_MS = 12L * 60L * 60L * 1000L
    }
}
