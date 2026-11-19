package com.example

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.model.BluetoothHardwareState
import com.example.model.ThemeMode
import com.example.ui.navigation.CAPodNavHost
import com.example.ui.theme.CapodTheme
import com.example.viewmodel.CAPodViewModel
import kotlinx.coroutines.flow.collectLatest

private enum class BluetoothPermissionDialogState {
    NONE,
    RATIONALE,
    PERMANENTLY_DENIED,
}

class MainActivity : ComponentActivity() {

    /**
     * Returns the runtime permissions required to scan and connect to Bluetooth LE devices
     * for the current Android API level:
     * - Android 12+ (API 31+): BLUETOOTH_SCAN + BLUETOOTH_CONNECT
     * - Android 11 and below (API <= 30): ACCESS_FINE_LOCATION
     */
    internal fun requiredBluetoothPermissions(): Array<String> {
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

    internal fun missingBluetoothPermissions(): Array<String> {
        return requiredBluetoothPermissions()
            .filter { permission ->
                ContextCompat.checkSelfPermission(this, permission) !=
                    PackageManager.PERMISSION_GRANTED
            }
            .toTypedArray()
    }

    internal fun hasAllBluetoothPermissions(): Boolean = missingBluetoothPermissions().isEmpty()

    private fun shouldShowBluetoothRationale(permissions: Array<String>): Boolean {
        return permissions.any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
        }
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            // Settings screen unavailable on this device
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: CAPodViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val navController = rememberNavController()
            val snackbarHostState = remember { SnackbarHostState() }

            var hasAttemptedStartupPrompt by rememberSaveable { mutableStateOf(false) }
            var hasPromptedUserManually by rememberSaveable { mutableStateOf(false) }
            var pendingEnableBluetooth by rememberSaveable { mutableStateOf(false) }
            var permissionDialogState by remember {
                mutableStateOf(BluetoothPermissionDialogState.NONE)
            }

            val enableBluetoothLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) {
                viewModel.onPermissionsResult(fromUserPrompt = true)
            }

            fun launchEnableBluetoothIntent() {
                try {
                    enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                } catch (_: Exception) {
                    try {
                        startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                    } catch (_: Exception) {
                        // Bluetooth settings unavailable on this device
                    }
                }
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { grantResults ->
                val allGranted = hasAllBluetoothPermissions() ||
                    (grantResults.isNotEmpty() && grantResults.values.all { it })

                if (allGranted) {
                    permissionDialogState = BluetoothPermissionDialogState.NONE
                    viewModel.onPermissionsResult(fromUserPrompt = true)
                    if (pendingEnableBluetooth) {
                        pendingEnableBluetooth = false
                        if (viewModel.bleScanner.resolveHardwareState() ==
                            BluetoothHardwareState.BLUETOOTH_DISABLED
                        ) {
                            launchEnableBluetoothIntent()
                        }
                    }
                } else {
                    pendingEnableBluetooth = false
                    val stillMissing = missingBluetoothPermissions()
                    viewModel.onPermissionsResult(fromUserPrompt = hasPromptedUserManually)
                    if (hasPromptedUserManually && stillMissing.isNotEmpty()) {
                        permissionDialogState = if (shouldShowBluetoothRationale(stillMissing)) {
                            BluetoothPermissionDialogState.RATIONALE
                        } else {
                            BluetoothPermissionDialogState.PERMANENTLY_DENIED
                        }
                    }
                }
            }

            fun requestBluetoothPermissions(userInitiated: Boolean) {
                val missing = missingBluetoothPermissions()
                if (missing.isEmpty()) {
                    permissionDialogState = BluetoothPermissionDialogState.NONE
                    viewModel.onPermissionsResult(fromUserPrompt = userInitiated)
                    return
                }

                if (userInitiated) {
                    hasPromptedUserManually = true
                    if (shouldShowBluetoothRationale(missing)) {
                        permissionDialogState = BluetoothPermissionDialogState.RATIONALE
                        return
                    }
                }

                permissionLauncher.launch(missing)
            }

            // Proactively request Bluetooth scan/connect permissions once on first launch
            // when Bluetooth hardware is present so the scanning UI works immediately.
            LaunchedEffect(uiState.hardwareState) {
                if (uiState.hardwareState == BluetoothHardwareState.PERMISSION_REQUIRED &&
                    !hasAttemptedStartupPrompt
                ) {
                    hasAttemptedStartupPrompt = true
                    val missing = missingBluetoothPermissions()
                    if (missing.isNotEmpty() && !shouldShowBluetoothRationale(missing)) {
                        permissionLauncher.launch(missing)
                    }
                }
            }

            // Refresh Bluetooth hardware state and resume scanning when returning from Settings or background
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        if (hasAllBluetoothPermissions()) {
                            permissionDialogState = BluetoothPermissionDialogState.NONE
                        }
                        viewModel.onForegroundResumed()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            // Collect one-shot user snackbar feedback messages
            LaunchedEffect(Unit) {
                viewModel.snackbarEvents.collectLatest { message ->
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(message)
                }
            }

            val darkTheme = when (uiState.settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            CapodTheme(
                darkTheme = darkTheme,
                dynamicColor = uiState.settings.dynamicColor
            ) {
                CAPodNavHost(
                    navController = navController,
                    uiState = uiState,
                    viewModel = viewModel,
                    snackbarHostState = snackbarHostState,
                    onRequestPermissions = {
                        requestBluetoothPermissions(userInitiated = true)
                    },
                    onRequestEnableBluetooth = {
                        if (!hasAllBluetoothPermissions()) {
                            pendingEnableBluetooth = true
                            requestBluetoothPermissions(userInitiated = true)
                        } else {
                            launchEnableBluetoothIntent()
                        }
                    }
                )

                BluetoothPermissionDialog(
                    state = permissionDialogState,
                    onDismiss = { permissionDialogState = BluetoothPermissionDialogState.NONE },
                    onConfirmRationale = {
                        permissionDialogState = BluetoothPermissionDialogState.NONE
                        val missing = missingBluetoothPermissions()
                        if (missing.isNotEmpty()) {
                            permissionLauncher.launch(missing)
                        } else {
                            viewModel.onPermissionsResult(fromUserPrompt = true)
                        }
                    },
                    onOpenSettings = {
                        permissionDialogState = BluetoothPermissionDialogState.NONE
                        openAppSettings()
                    }
                )
            }
        }
    }
}

@Composable
private fun BluetoothPermissionDialog(
    state: BluetoothPermissionDialogState,
    onDismiss: () -> Unit,
    onConfirmRationale: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    when (state) {
        BluetoothPermissionDialogState.NONE -> Unit

        BluetoothPermissionDialogState.RATIONALE -> {
            val rationaleMessage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                stringResource(R.string.perm_rationale_body_s)
            } else {
                stringResource(R.string.perm_rationale_body_legacy)
            }
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Bluetooth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = { Text(stringResource(R.string.perm_rationale_title)) },
                text = { Text(rationaleMessage) },
                confirmButton = {
                    TextButton(
                        onClick = onConfirmRationale,
                        modifier = Modifier.testTag("permission_rationale_allow_button")
                    ) {
                        Text(stringResource(R.string.action_allow))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("permission_rationale_dismiss_button")
                    ) {
                        Text(stringResource(R.string.action_not_now))
                    }
                },
                modifier = Modifier.testTag("permission_rationale_dialog")
            )
        }

        BluetoothPermissionDialogState.PERMANENTLY_DENIED -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Bluetooth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = { Text(stringResource(R.string.perm_settings_title)) },
                text = { Text(stringResource(R.string.perm_settings_body)) },
                confirmButton = {
                    TextButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("permission_open_settings_button")
                    ) {
                        Text(stringResource(R.string.action_open_settings))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("permission_settings_dismiss_button")
                    ) {
                        Text(stringResource(R.string.action_not_now))
                    }
                },
                modifier = Modifier.testTag("permission_settings_dialog")
            )
        }
    }
}
