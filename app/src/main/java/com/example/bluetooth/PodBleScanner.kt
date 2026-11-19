package com.example.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager

import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.model.BlePodSnapshot
import com.example.model.BleScanMode
import com.example.model.BluetoothHardwareState
import com.example.model.BondedBluetoothDevice
import com.example.model.DiscoveredBleDevice
import com.example.model.PodModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Manages real Android Bluetooth LE scanning for Apple 0x004C Continuity advertisements,
 * general nearby BLE peripheral discovery & pairing (createBond), Bluetooth adapter state tracking,
 * and Classic Bluetooth bonded/connected audio device lookup.
 */
class PodBleScanner(private val context: Context) {

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter

    private val _hardwareState = MutableStateFlow(resolveHardwareState())
    val hardwareState: StateFlow<BluetoothHardwareState> = _hardwareState.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _nearbySnapshots = MutableStateFlow<Map<String, BlePodSnapshot>>(emptyMap())
    val nearbySnapshots: StateFlow<Map<String, BlePodSnapshot>> = _nearbySnapshots.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<Map<String, DiscoveredBleDevice>>(emptyMap())
    val discoveredDevices: StateFlow<Map<String, DiscoveredBleDevice>> = _discoveredDevices.asStateFlow()

    private val _bondedDevices = MutableStateFlow<List<BondedBluetoothDevice>>(emptyList())
    val bondedDevices: StateFlow<List<BondedBluetoothDevice>> = _bondedDevices.asStateFlow()

    private val _bondEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val bondEvents: SharedFlow<String> = _bondEvents.asSharedFlow()

    private val connectedAddresses = mutableSetOf<String>()
    private val discoveredDeviceHandles = mutableMapOf<String, BluetoothDevice>()
    private var activeScanner: BluetoothLeScanner? = null
    private var discoveryScanner: BluetoothLeScanner? = null
    private var currentScanMode: BleScanMode = BleScanMode.BALANCED
    private var receiverRegistered = false

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            handleAppleScanResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach(::handleAppleScanResult)
        }

        override fun onScanFailed(errorCode: Int) {
            Log.w(TAG, "Apple Continuity BLE scan failed with errorCode=$errorCode")
            _isScanning.value = false
        }
    }

    private val generalDiscoveryCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            handleGeneralDiscoveryResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach(::handleGeneralDiscoveryResult)
        }

        override fun onScanFailed(errorCode: Int) {
            Log.w(TAG, "General BLE discovery scan failed with errorCode=$errorCode")
            _isDiscovering.value = false
        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = resolveHardwareState()
                    _hardwareState.value = state
                    if (state != BluetoothHardwareState.READY) {
                        stopScanning()
                        stopGeneralDiscovery()
                    }
                    refreshBondedDevices()
                }
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    val device = extractBluetoothDevice(intent)
                    device?.address?.let { connectedAddresses.add(it) }
                    refreshBondedDevices()
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device = extractBluetoothDevice(intent)
                    device?.address?.let { connectedAddresses.remove(it) }
                    refreshBondedDevices()
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val device = extractBluetoothDevice(intent)
                    val newBondState = intent.getIntExtra(
                        BluetoothDevice.EXTRA_BOND_STATE,
                        BluetoothDevice.ERROR
                    )
                    val prevBondState = intent.getIntExtra(
                        BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE,
                        BluetoothDevice.ERROR
                    )
                    if (device != null && newBondState != BluetoothDevice.ERROR) {
                        val addr = device.address
                        val label = try {
                            device.name?.takeIf { it.isNotBlank() } ?: addr
                        } catch (_: SecurityException) {
                            addr
                        }
                        _discoveredDevices.update { current ->
                            val existing = current[addr] ?: return@update current
                            current + (addr to existing.copy(bondState = newBondState))
                        }
                        if (newBondState == BluetoothDevice.BOND_BONDED) {
                            _bondEvents.tryEmit("Paired with $label")
                        } else if (newBondState == BluetoothDevice.BOND_NONE &&
                            prevBondState == BluetoothDevice.BOND_BONDING
                        ) {
                            _bondEvents.tryEmit("Pairing cancelled or failed for $label")
                        }
                    }
                    refreshBondedDevices()
                }
            }
        }
    }

    fun registerLifecycleReceivers() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        try {
            ContextCompat.registerReceiver(
                context,
                bluetoothReceiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
            receiverRegistered = true
        } catch (e: Exception) {
            Log.w(TAG, "Unable to register Bluetooth lifecycle receiver", e)
        }
        refreshStateAndBondedDevices()
    }

    fun unregisterLifecycleReceivers() {
        stopScanning()
        stopGeneralDiscovery()
        if (receiverRegistered) {
            try {
                context.unregisterReceiver(bluetoothReceiver)
            } catch (_: IllegalArgumentException) {
                // Already unregistered
            }
            receiverRegistered = false
        }
    }

    fun hasRequiredPermissions(): Boolean {
        return requiredPermissions().all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        }
    }

    fun resolveHardwareState(): BluetoothHardwareState {
        val adapter = try {
            bluetoothAdapter
        } catch (_: Exception) {
            null
        } ?: return BluetoothHardwareState.UNAVAILABLE
        if (!hasRequiredPermissions()) return BluetoothHardwareState.PERMISSION_REQUIRED
        val enabled = try {
            adapter.isEnabled
        } catch (_: Exception) {
            false
        }
        if (!enabled) return BluetoothHardwareState.BLUETOOTH_DISABLED
        return BluetoothHardwareState.READY
    }

    fun refreshStateAndBondedDevices() {
        _hardwareState.value = resolveHardwareState()
        refreshBondedDevices()
    }

    @SuppressLint("MissingPermission")
    fun startScanning(scanMode: BleScanMode = currentScanMode) {
        val state = resolveHardwareState()
        _hardwareState.value = state
        if (state != BluetoothHardwareState.READY) {
            _isScanning.value = false
            return
        }

        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            _isScanning.value = false
            return
        }

        if (_isScanning.value && currentScanMode == scanMode) return
        if (_isScanning.value) {
            stopScanning()
        }

        val appleFilter = ScanFilter.Builder()
            .setManufacturerData(AppleBleParser.APPLE_MANUFACTURER_ID, byteArrayOf())
            .build()

        val androidScanMode = when (scanMode) {
            BleScanMode.LOW_LATENCY -> ScanSettings.SCAN_MODE_LOW_LATENCY
            BleScanMode.BALANCED -> ScanSettings.SCAN_MODE_BALANCED
            BleScanMode.LOW_POWER -> ScanSettings.SCAN_MODE_LOW_POWER
        }

        val settings = ScanSettings.Builder()
            .setScanMode(androidScanMode)
            .setReportDelay(0L)
            .build()

        try {
            scanner.startScan(listOf(appleFilter), settings, scanCallback)
            currentScanMode = scanMode
            activeScanner = scanner
            _isScanning.value = true
        } catch (e: SecurityException) {
            Log.w(TAG, "Missing permission to start BLE scan", e)
            _hardwareState.value = BluetoothHardwareState.PERMISSION_REQUIRED
            _isScanning.value = false
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Bluetooth adapter not ready for BLE scan", e)
            _hardwareState.value = BluetoothHardwareState.BLUETOOTH_DISABLED
            _isScanning.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {
        if (!_isScanning.value) return
        try {
            activeScanner?.stopScan(scanCallback)
        } catch (_: Exception) {
            // Safe cleanup if Bluetooth turned off mid-scan
        } finally {
            activeScanner = null
            _isScanning.value = false
        }
    }

    /**
     * Starts an unfiltered, low-latency Bluetooth LE discovery scan to find all nearby
     * BLE peripherals for inspection and pairing.
     */
    @SuppressLint("MissingPermission")
    fun startGeneralDiscovery(clearPrevious: Boolean = false) {
        val state = resolveHardwareState()
        _hardwareState.value = state
        if (state != BluetoothHardwareState.READY) {
            _isDiscovering.value = false
            return
        }

        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            _isDiscovering.value = false
            return
        }

        if (_isDiscovering.value) {
            stopGeneralDiscovery()
        }

        if (clearPrevious) {
            _discoveredDevices.value = emptyMap()
            discoveredDeviceHandles.clear()
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0L)
            .build()

        try {
            scanner.startScan(null, settings, generalDiscoveryCallback)
            discoveryScanner = scanner
            _isDiscovering.value = true
        } catch (e: SecurityException) {
            Log.w(TAG, "Missing permission for general BLE discovery", e)
            _hardwareState.value = BluetoothHardwareState.PERMISSION_REQUIRED
            _isDiscovering.value = false
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Bluetooth adapter not ready for general BLE discovery", e)
            _hardwareState.value = BluetoothHardwareState.BLUETOOTH_DISABLED
            _isDiscovering.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopGeneralDiscovery() {
        if (!_isDiscovering.value) return
        try {
            discoveryScanner?.stopScan(generalDiscoveryCallback)
        } catch (_: Exception) {
            // Safe cleanup if Bluetooth turned off mid-scan
        } finally {
            discoveryScanner = null
            _isDiscovering.value = false
        }
    }

    /**
     * Initiates real Android Bluetooth pairing (bonding) with a discovered BLE device.
     */
    @SuppressLint("MissingPermission")
    fun initiatePairing(address: String): Result<Unit> {
        val state = resolveHardwareState()
        if (state != BluetoothHardwareState.READY) {
            return Result.failure(IllegalStateException("Bluetooth must be enabled with permissions granted."))
        }
        val adapter = bluetoothAdapter
            ?: return Result.failure(IllegalStateException("Bluetooth adapter unavailable."))

        val device = discoveredDeviceHandles[address] ?: try {
            adapter.getRemoteDevice(address)
        } catch (_: IllegalArgumentException) {
            return Result.failure(IllegalStateException("Invalid Bluetooth address."))
        }

        return try {
            when (device.bondState) {
                BluetoothDevice.BOND_BONDED -> {
                    _discoveredDevices.update { current ->
                        val existing = current[address] ?: return@update current
                        current + (address to existing.copy(bondState = BluetoothDevice.BOND_BONDED))
                    }
                    Result.success(Unit)
                }
                BluetoothDevice.BOND_BONDING -> {
                    Result.success(Unit)
                }
                else -> {
                    val initiated = device.createBond()
                    if (initiated) {
                        _discoveredDevices.update { current ->
                            val existing = current[address] ?: return@update current
                            current + (address to existing.copy(bondState = BluetoothDevice.BOND_BONDING))
                        }
                        Result.success(Unit)
                    } else {
                        Result.failure(
                            IllegalStateException("Unable to start pairing. Make sure the device is in pairing mode.")
                        )
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Missing BLUETOOTH_CONNECT permission for createBond", e)
            Result.failure(IllegalStateException("Bluetooth Connect permission is required to pair."))
        }
    }

    fun pruneStaleSnapshots(maxAgeMillis: Long = 45_000L) {
        val now = System.currentTimeMillis()
        _nearbySnapshots.update { current ->
            current.filterValues { (now - it.lastSeenMillis) <= maxAgeMillis }
        }
        _discoveredDevices.update { current ->
            current.filterValues { (now - it.lastSeenMillis) <= 60_000L || it.isBonding || it.isBonded }
        }
    }

    @SuppressLint("MissingPermission")
    fun refreshBondedDevices() {
        val adapter = bluetoothAdapter
        if (adapter == null || !hasRequiredPermissions() || !adapter.isEnabled) {
            _bondedDevices.value = emptyList()
            return
        }

        try {
            val bonded = adapter.bondedDevices.orEmpty().map { device ->
                val connected = isDeviceConnected(device) || device.address in connectedAddresses
                BondedBluetoothDevice(
                    name = device.name?.takeIf { it.isNotBlank() } ?: device.address,
                    address = device.address,
                    isConnected = connected,
                    classicBatteryPercent = readClassicBatteryLevel(device),
                )
            }.sortedByDescending { it.isConnected }

            _bondedDevices.value = bonded
        } catch (e: SecurityException) {
            Log.w(TAG, "Unable to read bonded devices", e)
            _bondedDevices.value = emptyList()
        }
    }

    private fun handleAppleScanResult(result: ScanResult) {
        val record = result.scanRecord ?: return
        val appleData = record.getManufacturerSpecificData(AppleBleParser.APPLE_MANUFACTURER_ID)
            ?: return
        val address = result.device?.address ?: return
        val snapshot = AppleBleParser.parse(
            address = address,
            rssi = result.rssi,
            manufacturerData = appleData,
            timestampMillis = System.currentTimeMillis(),
        ) ?: return

        _nearbySnapshots.update { current ->
            current + (address to snapshot)
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleGeneralDiscoveryResult(result: ScanResult) {
        val device = result.device ?: return
        val address = device.address ?: return
        val record = result.scanRecord
        val now = System.currentTimeMillis()

        discoveredDeviceHandles[address] = device

        val recordName = record?.deviceName?.takeIf { it.isNotBlank() }
        val deviceName = try {
            device.name?.takeIf { it.isNotBlank() }
        } catch (_: SecurityException) {
            null
        }
        val bondState = try {
            device.bondState
        } catch (_: SecurityException) {
            BluetoothDevice.BOND_NONE
        }
        val isConnectable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            result.isConnectable
        } else {
            true
        }

        // Check if Apple Continuity packet is present to identify AirPods / Beats automatically
        var detectedPodModel: PodModel? = null
        val appleData = record?.getManufacturerSpecificData(AppleBleParser.APPLE_MANUFACTURER_ID)
        if (appleData != null) {
            val parsed = AppleBleParser.parse(address, result.rssi, appleData, now)
            if (parsed != null) {
                if (parsed.model != PodModel.UNKNOWN) {
                    detectedPodModel = parsed.model
                }
                _nearbySnapshots.update { current -> current + (address to parsed) }
            }
        }

        val manufacturerLabel = resolveManufacturerLabel(record)

        _discoveredDevices.update { current ->
            val previous = current[address]
            val resolvedName = recordName ?: deviceName ?: previous?.name
            val resolvedModel = detectedPodModel ?: previous?.detectedPodModel
            val resolvedMfg = manufacturerLabel ?: previous?.manufacturerLabel

            current + (
                address to DiscoveredBleDevice(
                    address = address,
                    name = resolvedName,
                    rssi = result.rssi,
                    bondState = bondState,
                    isConnectable = isConnectable,
                    manufacturerLabel = resolvedMfg,
                    detectedPodModel = resolvedModel,
                    lastSeenMillis = now,
                )
            )
        }
    }

    private fun resolveManufacturerLabel(record: android.bluetooth.le.ScanRecord?): String? {
        val sparse = record?.manufacturerSpecificData ?: return null
        if (sparse.size() == 0) return null
        val companyId = sparse.keyAt(0)
        return when (companyId) {
            0x004C -> "Apple"
            0x0075 -> "Samsung"
            0x00E0 -> "Google"
            0x012D -> "Sony"
            0x0006 -> "Microsoft"
            0x009E -> "Bose"
            0x0059 -> "Nordic"
            0x000F -> "Broadcom"
            0x000A -> "Qualcomm"
            0x00D2 -> "Dialog"
            0x038F -> "Xiaomi"
            0x027D -> "Huawei"
            0x0087 -> "Garmin"
            else -> "BLE 0x%04X".format(companyId)
        }
    }

    private fun isDeviceConnected(device: BluetoothDevice): Boolean {
        return try {
            val method = device.javaClass.getMethod("isConnected")
            (method.invoke(device) as? Boolean) == true
        } catch (_: Exception) {
            false
        }
    }

    private fun readClassicBatteryLevel(device: BluetoothDevice): Int? {
        return try {
            val method = device.javaClass.getMethod("getBatteryLevel")
            val level = method.invoke(device) as? Int ?: -1
            if (level in 0..100) level else null
        } catch (_: Exception) {
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun extractBluetoothDevice(intent: Intent): BluetoothDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
    }

    companion object {
        private const val TAG = "PodBleScanner"
    }
}
