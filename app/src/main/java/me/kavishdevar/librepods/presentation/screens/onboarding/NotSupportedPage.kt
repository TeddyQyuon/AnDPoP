package me.kavishdevar.librepods.presentation.screens.onboarding

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.BuildConfig
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.components.AndpopGroup
import me.kavishdevar.librepods.presentation.components.AndpopRow
import me.kavishdevar.librepods.presentation.components.InfoBanner
import me.kavishdevar.librepods.presentation.theme.AndpopIcons
import me.kavishdevar.librepods.presentation.theme.AndpopType
import me.kavishdevar.librepods.presentation.theme.LocalAndpopPalette

@Composable
fun NotSupportedPage(
    bypassCompatibilityCheck: () -> Unit,
    onTroubleshooting: () -> Unit = {},
) {
    val palette = LocalAndpopPalette.current
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val devEnabled = !BuildConfig.PLAY_BUILD && prefs.getBoolean("developer_options_enabled", false)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier.size(96.dp).clip(CircleShape)
                .background(Brush.radialGradient(listOf(palette.warning.copy(alpha = 0.18f), palette.bg))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AndpopIcons.Smartphone, null, Modifier.size(40.dp), tint = palette.warning)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.andpop_unsupported_title),
            Modifier.fillMaxWidth().semantics { heading() },
            style = AndpopType.pageTitle,
            color = palette.on,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.andpop_unsupported_body),
            Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 18.dp),
            style = AndpopType.bodySmall,
            color = palette.on2,
            textAlign = TextAlign.Center,
        )
        AndpopGroup {
            row { shape ->
                AndpopRow(
                    shape = shape,
                    title = stringResource(R.string.andpop_unsupported_pixel),
                    supporting = stringResource(R.string.andpop_unsupported_pixel_ver),
                    icon = AndpopIcons.Smartphone,
                )
            }
            row { shape ->
                AndpopRow(
                    shape = shape,
                    title = stringResource(R.string.andpop_unsupported_oppo),
                    supporting = stringResource(R.string.andpop_unsupported_oppo_ver),
                    icon = AndpopIcons.Smartphone,
                )
            }
            row { shape ->
                AndpopRow(
                    shape = shape,
                    title = stringResource(R.string.andpop_unsupported_other),
                    supporting = stringResource(R.string.andpop_unsupported_other_ver),
                    icon = AndpopIcons.Smartphone,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        InfoBanner(
            text = stringResource(
                R.string.andpop_unsupported_this_phone,
                "${Build.MANUFACTURER} ${Build.MODEL}",
                Build.VERSION.RELEASE,
            ),
        )
        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { context.startActivity(Intent(Settings.ACTION_SETTINGS)) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = palette.pri, contentColor = palette.onPri),
            ) {
                Text(stringResource(R.string.andpop_check_system_update), style = AndpopType.button)
            }
            TextButton(onClick = onTroubleshooting, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.troubleshooting), color = palette.pri, style = AndpopType.button)
            }
            if (devEnabled) {
                TextButton(onClick = bypassCompatibilityCheck, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.andpop_continue_anyway_dev), color = palette.pri, style = AndpopType.button)
                }
            }
        }
    }
}
