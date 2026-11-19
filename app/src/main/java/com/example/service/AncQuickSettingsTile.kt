package com.example.service

import android.app.PendingIntent
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.bluetooth.PodControlClient
import com.example.data.CAPodDatabase
import com.example.data.DeviceNameRepository
import com.example.data.DeviceProfileEntity
import com.example.model.ListeningMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AncTilePinResult {
    ADDED,
    ALREADY_ADDED,
    DISMISSED,
    MANUAL_INSTRUCTIONS,
}

data class AncQuickTileSnapshot(
    val profileId: Long = 0L,
    val deviceName: String = "",
    val mode: ListeningMode = ListeningMode.OFF,
    val hasAncDevice: Boolean = false,
)

/**
 * Android Quick Settings Tile that toggles Active Noise Cancellation modes
 * (Noise Cancellation -> Transparency -> Off) for the user's connected AirPods / Beats.
 *
 * Updates both the local Room profile (keeping the in-app device status card in sync)
 * and transmits the Apple Accessory Protocol (AAP) control frame over Bluetooth.
 */
class AncQuickSettingsTile : TileService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartListening() {
        super.onStartListening()
        // Render cached state immediately for zero flicker
        applyTileVisuals(readCachedSnapshot(this))

        // Reconcile against Room + DataStore custom names
        serviceScope.launch {
            val fresh = loadLiveSnapshot(applicationContext)
            writeCachedSnapshot(applicationContext, fresh)
            withContext(Dispatchers.Main) {
                applyTileVisuals(fresh)
            }
        }
    }

    override fun onClick() {
        super.onClick()
        serviceScope.launch {
            val db = CAPodDatabase.getInstance(applicationContext)
            val dao = db.deviceProfileDao()
            val profiles = dao.observeAll().first()
            val preferredId = readTargetProfileId(applicationContext)
            val target = resolveActiveAncProfile(profiles, preferredId)

            if (target == null) {
                withContext(Dispatchers.Main) {
                    applyTileVisuals(AncQuickTileSnapshot(hasAncDevice = false))
                    openApp()
                }
                return@launch
            }

            val nextMode = target.preferredListeningMode.nextQuickToggleMode()
            val customNames = DeviceNameRepository(applicationContext).customNames.first()
            val displayName = customNames.resolveForProfile(target.id, target.linkedBluetoothAddress)
                ?: target.name

            // Persist mode so the device status card and Room observers update immediately
            dao.updateListeningMode(target.id, nextMode.id)

            val updatedSnapshot = AncQuickTileSnapshot(
                profileId = target.id,
                deviceName = displayName,
                mode = nextMode,
                hasAncDevice = true,
            )
            writeCachedSnapshot(applicationContext, updatedSnapshot)

            withContext(Dispatchers.Main) {
                applyTileVisuals(updatedSnapshot)
            }

            // Dispatch real AAP listening-mode control packet over Bluetooth Classic
            PodControlClient(applicationContext).sendListeningMode(
                bluetoothAddress = target.linkedBluetoothAddress,
                mode = nextMode,
            )
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun applyTileVisuals(snapshot: AncQuickTileSnapshot) {
        val tile = qsTile ?: return
        if (!snapshot.hasAncDevice) {
            tile.label = getString(R.string.qs_tile_anc_label)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = getString(R.string.qs_tile_anc_no_device)
            }
            tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_anc_off)
            tile.state = Tile.STATE_INACTIVE
            tile.updateTile()
            return
        }

        val modeLabel = modeLabelFor(this, snapshot.mode)
        tile.label = modeLabel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = snapshot.deviceName.ifBlank { getString(R.string.app_name) }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            tile.stateDescription = modeLabel
        }
        tile.icon = Icon.createWithResource(this, iconResForMode(snapshot.mode))
        tile.state = when (snapshot.mode) {
            ListeningMode.NOISE_CANCELLATION,
            ListeningMode.TRANSPARENCY,
            ListeningMode.ADAPTIVE -> Tile.STATE_ACTIVE
            ListeningMode.OFF -> Tile.STATE_INACTIVE
        }
        tile.updateTile()
    }

    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                102,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            // The PendingIntent overload is available from API 34.
            @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(intent)
        }
    }

    companion object {
        private const val PREFS_NAME = "capod_anc_qs_tile_prefs"
        private const val KEY_TARGET_PROFILE_ID = "target_profile_id"
        private const val KEY_DEVICE_NAME = "device_name"
        private const val KEY_MODE_ID = "mode_id"
        private const val KEY_HAS_ANC_DEVICE = "has_anc_device"

        fun resolveActiveAncProfile(
            profiles: List<DeviceProfileEntity>,
            preferredProfileId: Long,
        ): DeviceProfileEntity? {
            val ancProfiles = profiles.filter { it.model.supportsAnc }
            if (ancProfiles.isEmpty()) return null
            if (preferredProfileId > 0L) {
                ancProfiles.firstOrNull { it.id == preferredProfileId }?.let { return it }
            }
            return ancProfiles.first()
        }

        @DrawableRes
        fun iconResForMode(mode: ListeningMode): Int = when (mode) {
            ListeningMode.NOISE_CANCELLATION, ListeningMode.ADAPTIVE -> R.drawable.ic_qs_anc_on
            ListeningMode.TRANSPARENCY -> R.drawable.ic_qs_anc_transparency
            ListeningMode.OFF -> R.drawable.ic_qs_anc_off
        }

        fun modeLabelFor(context: Context, mode: ListeningMode): String = when (mode) {
            ListeningMode.NOISE_CANCELLATION -> context.getString(R.string.anc_noise_cancellation)
            ListeningMode.TRANSPARENCY -> context.getString(R.string.anc_transparency)
            ListeningMode.ADAPTIVE -> context.getString(R.string.anc_adaptive)
            ListeningMode.OFF -> context.getString(R.string.qs_tile_anc_off_label)
        }

        fun readTargetProfileId(context: Context): Long {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getLong(KEY_TARGET_PROFILE_ID, 0L)
        }

        fun setTargetProfileId(context: Context, profileId: Long) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_TARGET_PROFILE_ID, profileId)
                .apply()
        }

        fun readCachedSnapshot(context: Context): AncQuickTileSnapshot {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return AncQuickTileSnapshot(
                profileId = prefs.getLong(KEY_TARGET_PROFILE_ID, 0L),
                deviceName = prefs.getString(KEY_DEVICE_NAME, "") ?: "",
                mode = ListeningMode.fromId(prefs.getString(KEY_MODE_ID, ListeningMode.OFF.id) ?: ListeningMode.OFF.id),
                hasAncDevice = prefs.getBoolean(KEY_HAS_ANC_DEVICE, false),
            )
        }

        fun writeCachedSnapshot(context: Context, snapshot: AncQuickTileSnapshot) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_TARGET_PROFILE_ID, snapshot.profileId)
                .putString(KEY_DEVICE_NAME, snapshot.deviceName)
                .putString(KEY_MODE_ID, snapshot.mode.id)
                .putBoolean(KEY_HAS_ANC_DEVICE, snapshot.hasAncDevice)
                .apply()
        }

        private suspend fun loadLiveSnapshot(context: Context): AncQuickTileSnapshot {
            val profiles = CAPodDatabase.getInstance(context).deviceProfileDao().observeAll().first()
            val preferredId = readTargetProfileId(context)
            val target = resolveActiveAncProfile(profiles, preferredId)
                ?: return AncQuickTileSnapshot(hasAncDevice = false)
            val customNames = DeviceNameRepository(context).customNames.first()
            val displayName = customNames.resolveForProfile(target.id, target.linkedBluetoothAddress)
                ?: target.name
            return AncQuickTileSnapshot(
                profileId = target.id,
                deviceName = displayName,
                mode = target.preferredListeningMode,
                hasAncDevice = true,
            )
        }

        /**
         * Updates the cached tile state and prompts Android SystemUI to refresh the ANC Quick Settings tile.
         */
        fun syncAndRequestRefresh(
            context: Context,
            profileId: Long,
            deviceDisplayName: String,
            mode: ListeningMode,
            makeTarget: Boolean = false,
        ) {
            val currentTarget = readTargetProfileId(context)
            val shouldUpdateCache = makeTarget || currentTarget == 0L || currentTarget == profileId
            if (shouldUpdateCache) {
                writeCachedSnapshot(
                    context = context,
                    snapshot = AncQuickTileSnapshot(
                        profileId = profileId,
                        deviceName = deviceDisplayName,
                        mode = mode,
                        hasAncDevice = true,
                    ),
                )
            }
            requestRefresh(context)
        }

        fun requestRefresh(context: Context) {
            try {
                requestListeningState(
                    context,
                    ComponentName(context, AncQuickSettingsTile::class.java)
                )
            } catch (_: Exception) {
                // Safe no-op if Quick Settings tile is not currently added
            }
        }

        /**
         * Links the ANC Quick Settings tile to the selected device status card and prompts
         * Android 13+ (API 33+) to add the tile directly to the system Quick Settings shade.
         */
        fun requestAddTile(
            context: Context,
            profileId: Long,
            deviceDisplayName: String,
            currentMode: ListeningMode,
            onResult: (AncTilePinResult) -> Unit,
        ) {
            syncAndRequestRefresh(
                context = context,
                profileId = profileId,
                deviceDisplayName = deviceDisplayName,
                mode = currentMode,
                makeTarget = true,
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val statusBarManager = context.getSystemService(StatusBarManager::class.java)
                if (statusBarManager != null) {
                    try {
                        statusBarManager.requestAddTileService(
                            ComponentName(context, AncQuickSettingsTile::class.java),
                            context.getString(R.string.qs_tile_anc_label),
                            Icon.createWithResource(context, iconResForMode(currentMode)),
                            ContextCompat.getMainExecutor(context),
                        ) { resultCode ->
                            val mapped = when (resultCode) {
                                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ->
                                    AncTilePinResult.ADDED
                                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED ->
                                    AncTilePinResult.ALREADY_ADDED
                                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED ->
                                    AncTilePinResult.DISMISSED
                                else -> AncTilePinResult.MANUAL_INSTRUCTIONS
                            }
                            onResult(mapped)
                        }
                        return
                    } catch (_: Exception) {
                        // Fall through to manual guidance when system dialog isn't supported
                    }
                }
            }

            onResult(AncTilePinResult.MANUAL_INSTRUCTIONS)
        }
    }
}
