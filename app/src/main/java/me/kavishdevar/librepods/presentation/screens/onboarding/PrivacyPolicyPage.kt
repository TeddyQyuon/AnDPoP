package me.kavishdevar.librepods.presentation.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.components.AndpopGroup
import me.kavishdevar.librepods.presentation.components.AndpopRow
import me.kavishdevar.librepods.presentation.theme.AndpopIcons
import me.kavishdevar.librepods.presentation.theme.AndpopType
import me.kavishdevar.librepods.presentation.theme.LocalAndpopPalette

/** Short, accurate privacy summary shown during onboarding and from Settings › Privacy. */
@Composable
fun PrivacyPolicyPage(
    onForward: () -> Unit
) {
    val palette = LocalAndpopPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(20.dp))
        Box(
            Modifier.size(96.dp).clip(CircleShape)
                .background(Brush.radialGradient(listOf(palette.pc, palette.pc.copy(alpha = 0.3f)))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AndpopIcons.Shield, null, Modifier.size(40.dp), tint = palette.onPc)
        }
        OnboardingDots(1)
        Text(
            stringResource(R.string.andpop_privacy_title),
            Modifier.fillMaxWidth().padding(bottom = 20.dp),
            style = AndpopType.pageTitle,
            color = palette.on,
            textAlign = TextAlign.Center,
        )
        AndpopGroup {
            row { shape ->
                AndpopRow(
                    shape, stringResource(R.string.andpop_privacy_bluetooth_title),
                    supporting = stringResource(R.string.andpop_privacy_bluetooth_body), icon = AndpopIcons.Bluetooth,
                )
            }
            row { shape ->
                AndpopRow(
                    shape, stringResource(R.string.andpop_privacy_account_title),
                    supporting = stringResource(R.string.andpop_privacy_account_body), icon = AndpopIcons.Person,
                )
            }
            row { shape ->
                AndpopRow(
                    shape, stringResource(R.string.andpop_privacy_tracking_title),
                    supporting = stringResource(R.string.andpop_privacy_tracking_body), icon = AndpopIcons.Heart,
                )
            }
        }
        Text(
            stringResource(R.string.andpop_privacy_permissions_note),
            Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 20.dp),
            style = AndpopType.caption,
            color = palette.on3,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onForward,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = palette.pri, contentColor = palette.onPri),
        ) {
            Text(stringResource(R.string.andpop_continue), style = AndpopType.button)
        }
        Spacer(Modifier.height(24.dp))
    }
}
