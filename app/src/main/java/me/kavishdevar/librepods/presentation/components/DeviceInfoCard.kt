package me.kavishdevar.librepods.presentation.components

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.kavishdevar.librepods.BuildConfig
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.utils.XposedState

@Composable
fun DeviceInfoCard() {
    val items = buildList {
        add(stringResource(R.string.manufacturer) to Build.MANUFACTURER)
        add(stringResource(R.string.model_number) to Build.MODEL)
        add(stringResource(R.string.build_id) to Build.DISPLAY)
        add(stringResource(R.string.version) to "${Build.ID} (${Build.VERSION.SDK_INT_FULL})")
        if (!BuildConfig.PLAY_BUILD) {
            add(stringResource(R.string.xposed_available) to stringResource(if (XposedState.isAvailable) R.string.yes else R.string.no))
            add(stringResource(R.string.app_enabled_in_xposed) to stringResource(if (XposedState.bluetoothScopeEnabled) R.string.yes else R.string.no))
        }
    }
    AndpopGroup {
        items.forEach { (label, value) ->
            row { shape ->
                AndpopRow(shape = shape, title = label, value = value)
            }
        }
    }
}
