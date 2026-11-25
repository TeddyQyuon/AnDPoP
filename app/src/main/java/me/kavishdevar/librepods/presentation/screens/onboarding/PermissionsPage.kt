package me.kavishdevar.librepods.presentation.screens.onboarding

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.components.AndpopGroup
import me.kavishdevar.librepods.presentation.components.PillTone
import me.kavishdevar.librepods.presentation.components.StatusPill
import me.kavishdevar.librepods.presentation.theme.AndpopIcons
import me.kavishdevar.librepods.presentation.theme.AndpopType
import me.kavishdevar.librepods.presentation.theme.LocalAndpopPalette

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsPage(
    onBackward: () -> Unit,
    onForward: () -> Unit,
) {
    val palette = LocalAndpopPalette.current
    val context = LocalContext.current
    var canDrawOverlays by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    val bluetoothState = rememberMultiplePermissionsState(
        listOf(
            "android.permission.BLUETOOTH_CONNECT",
            "android.permission.BLUETOOTH_SCAN",
        ),
    )
    val notificationState = rememberPermissionState("android.permission.POST_NOTIFICATIONS")
    val phoneState = rememberMultiplePermissionsState(
        listOf(
            "android.permission.READ_PHONE_STATE",
            "android.permission.ANSWER_PHONE_CALLS",
        ),
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canDrawOverlays = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OnboardingDots(2)
        Text(
            stringResource(R.string.andpop_allow_access_title),
            Modifier.fillMaxWidth().semantics { heading() },
            style = AndpopType.pageTitle,
            color = palette.on,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.andpop_allow_access_body),
            Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp),
            style = AndpopType.bodySmall,
            color = palette.on2,
            textAlign = TextAlign.Center,
        )

        AndpopGroup {
            row { shape ->
                PermissionRow(
                    shape = shape,
                    icon = AndpopIcons.Bluetooth,
                    title = stringResource(R.string.andpop_perm_nearby_title),
                    reason = stringResource(R.string.andpop_perm_nearby_desc),
                    required = true,
                    granted = bluetoothState.allPermissionsGranted,
                    onAllow = { bluetoothState.launchMultiplePermissionRequest() },
                )
            }
            row { shape ->
                PermissionRow(
                    shape = shape,
                    icon = AndpopIcons.Bell,
                    title = stringResource(R.string.andpop_perm_notif_title),
                    reason = stringResource(R.string.andpop_perm_notif_desc),
                    required = false,
                    granted = notificationState.status.isGranted,
                    onAllow = { notificationState.launchPermissionRequest() },
                )
            }
            row { shape ->
                PermissionRow(
                    shape = shape,
                    icon = AndpopIcons.Phone,
                    title = stringResource(R.string.andpop_perm_phone_title),
                    reason = stringResource(R.string.andpop_perm_phone_desc),
                    required = false,
                    granted = phoneState.allPermissionsGranted,
                    onAllow = { phoneState.launchMultiplePermissionRequest() },
                )
            }
            row { shape ->
                PermissionRow(
                    shape = shape,
                    icon = AndpopIcons.Popup,
                    title = stringResource(R.string.andpop_perm_overlay_title),
                    reason = stringResource(R.string.andpop_perm_overlay_desc),
                    required = false,
                    granted = canDrawOverlays,
                    onAllow = {
                        context.startActivity(
                            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri()),
                        )
                    },
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        val ready = bluetoothState.allPermissionsGranted
        Button(
            onClick = onForward,
            enabled = ready,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 24.dp).height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = palette.pri,
                contentColor = palette.onPri,
                disabledContainerColor = palette.s3,
                disabledContentColor = palette.on3,
            ),
        ) {
            Text(
                stringResource(if (ready) R.string.andpop_continue else R.string.andpop_continue_need_nearby),
                style = AndpopType.button,
            )
        }
    }
}

@Composable
private fun PermissionRow(
    shape: Shape,
    icon: ImageVector,
    title: String,
    reason: String,
    required: Boolean,
    granted: Boolean,
    onAllow: () -> Unit,
) {
    val palette = LocalAndpopPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(shape)
            .background(palette.s1)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(if (granted) palette.successContainer else palette.s2),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (granted) AndpopIcons.Check else icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = if (granted) palette.success else palette.pri,
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (required) {
                Text(
                    stringResource(R.string.andpop_required).uppercase(),
                    color = palette.pri,
                    style = AndpopType.tiny,
                )
            }
            Text(title, color = palette.on, style = AndpopType.titleSmall)
            Text(reason, color = palette.on2, style = AndpopType.caption)
        }
        if (granted) {
            StatusPill(stringResource(R.string.andpop_allowed), PillTone.Success)
        } else {
            FilledTonalButton(
                onClick = onAllow,
                modifier = Modifier.heightIn(min = 48.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = palette.pc, contentColor = palette.onPc),
            ) {
                Text(stringResource(R.string.andpop_allow), style = AndpopType.label)
            }
        }
    }
}
