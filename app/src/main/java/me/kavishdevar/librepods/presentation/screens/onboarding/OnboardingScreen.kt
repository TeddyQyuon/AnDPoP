package me.kavishdevar.librepods.presentation.screens.onboarding

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.components.PillTone
import me.kavishdevar.librepods.presentation.components.StatusPill
import me.kavishdevar.librepods.presentation.theme.AndpopIcons
import me.kavishdevar.librepods.presentation.theme.AndpopType
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import me.kavishdevar.librepods.presentation.theme.LibrePodsTheme
import me.kavishdevar.librepods.presentation.theme.LocalAndpopPalette
import me.kavishdevar.librepods.presentation.theme.LocalDesignSystem
import me.kavishdevar.librepods.utils.XposedState
import me.kavishdevar.librepods.utils.bypassDeviceCheck
import me.kavishdevar.librepods.utils.isSupported
import me.kavishdevar.librepods.utils.removeDeviceCheckBypass

@Composable
fun OnboardingScreen(
    signedInAs: String? = null,
    onLogin: () -> Unit = {},
    onTroubleshooting: () -> Unit = {},
    onOnboardingComplete: () -> Unit,
) {
    val wrapTheme = LocalDesignSystem.current != DesignSystem.Material
    if (wrapTheme) {
        LibrePodsTheme(m3eEnabled = true) {
            OnboardingContent(
                signedInAs = signedInAs,
                onLogin = onLogin,
                onTroubleshooting = onTroubleshooting,
                onOnboardingComplete = onOnboardingComplete,
            )
        }
    } else {
        OnboardingContent(
            signedInAs = signedInAs,
            onLogin = onLogin,
            onTroubleshooting = onTroubleshooting,
            onOnboardingComplete = onOnboardingComplete,
        )
    }
}

@Composable
private fun OnboardingContent(
    signedInAs: String?,
    onLogin: () -> Unit,
    onTroubleshooting: () -> Unit,
    onOnboardingComplete: () -> Unit,
) {
    val palette = LocalAndpopPalette.current
    val sharedPreferences = LocalContext.current.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val isSupported = isSupported(sharedPreferences) || XposedState.bluetoothScopeEnabled
    val state = rememberCarouselState(initialItem = 0, itemCount = { 4 })

    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val titles = listOf(
        null,
        stringResource(R.string.andpop_privacy),
        stringResource(R.string.not_supported),
        stringResource(R.string.permissions),
    )

    val animationScope = rememberCoroutineScope()
    val goBack: () -> Unit = {
        animationScope.launch {
            if (state.canScrollBackward) {
                val targetItem = if (isSupported && state.currentItem == 3) 1 else state.currentItem - 1
                if (state.currentItem == 3 && !isSupported) removeDeviceCheckBypass(sharedPreferences)
                state.animateScrollToItem(targetItem)
            }
        }
    }

    BackHandler(enabled = state.canScrollBackward, onBack = goBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(topPadding))
        HorizontalUncontainedCarousel(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            state = state,
            itemWidth = LocalWindowInfo.current.containerDpSize.width - 16.dp,
            userScrollEnabled = false,
        ) { index ->
            val shape = rememberMaskShape(RoundedCornerShape(32.dp))
            Surface(
                shape = shape,
                color = palette.bg,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(32.dp)),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    titles[index]?.let { title ->
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = goBack, modifier = Modifier.size(48.dp)) {
                                Icon(AndpopIcons.Back, stringResource(R.string.back), tint = palette.on)
                            }
                            Text(
                                text = title,
                                modifier = Modifier.padding(start = 6.dp),
                                style = AndpopType.titleSmall,
                                color = palette.on,
                            )
                        }
                    }
                    when (index) {
                        0 -> WelcomePage(
                            onForward = { animationScope.launch { state.animateScrollToItem(1) } },
                            signedInAs = signedInAs,
                            onLogin = onLogin,
                        )
                        1 -> PrivacyPolicyPage(
                            onForward = {
                                animationScope.launch {
                                    state.animateScrollToItem(if (isSupported) 3 else 2)
                                }
                            },
                        )
                        2 -> NotSupportedPage(
                            bypassCompatibilityCheck = {
                                bypassDeviceCheck(sharedPreferences)
                                animationScope.launch { state.animateScrollToItem(3) }
                            },
                            onTroubleshooting = onTroubleshooting,
                        )
                        3 -> PermissionsPage(
                            onBackward = goBack,
                            onForward = onOnboardingComplete,
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(bottomPadding))
    }
}

@Preview
@Composable
fun OnboardingScreenPreview() {
    OnboardingScreen {}
}

/** Step dots shared by the welcome, privacy, and permissions pages. */
@Composable
internal fun OnboardingDots(active: Int, count: Int = 3) {
    val palette = LocalAndpopPalette.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(vertical = 12.dp),
    ) {
        repeat(count) { index ->
            Box(
                Modifier.size(width = if (index == active) 24.dp else 8.dp, height = 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (index == active) palette.pri else palette.s4),
            )
        }
    }
}

@Composable
private fun WelcomePage(onForward: () -> Unit, signedInAs: String?, onLogin: () -> Unit) {
    val palette = LocalAndpopPalette.current
    val bob = rememberInfiniteTransition(label = "Welcome float")
    val lift by bob.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(tween(2500), RepeatMode.Reverse),
        label = "Welcome lift",
    )
    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(240.dp).background(
                    Brush.radialGradient(listOf(palette.pri.copy(alpha = 0.28f), Color.Transparent)),
                ),
            )
            Image(
                painterResource(R.drawable.amber_pro_2_case), null,
                Modifier.size(width = 176.dp, height = 130.dp).offset(y = (28 + lift / 2).dp),
            )
            Image(
                painterResource(R.drawable.amber_pro_2_left), null,
                Modifier.size(width = 64.dp, height = 86.dp).offset(x = (-104).dp, y = (-46 + lift).dp)
                    .graphicsLayer { rotationZ = -8f },
            )
            Image(
                painterResource(R.drawable.amber_pro_2_right), null,
                Modifier.size(width = 64.dp, height = 86.dp).offset(x = 104.dp, y = (-56 - lift).dp),
            )
        }
        OnboardingDots(0)
        Text(
            stringResource(R.string.andpop_welcome_title),
            Modifier.fillMaxWidth().semantics { heading() },
            style = AndpopType.display,
            color = palette.on,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.andpop_welcome_body),
            Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 20.dp),
            style = AndpopType.bodySmall,
            color = palette.on2,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onForward,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = palette.pri, contentColor = palette.onPri),
        ) {
            Text(stringResource(R.string.andpop_get_started), style = AndpopType.button)
        }
        if (signedInAs != null) {
            Box(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 4.dp), contentAlignment = Alignment.Center) {
                StatusPill(stringResource(R.string.andpop_signed_in_as, signedInAs), PillTone.Success)
            }
        } else {
            TextButton(
                onClick = onLogin,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 4.dp),
            ) {
                Text(stringResource(R.string.andpop_already_have_account), color = palette.pri, style = AndpopType.button)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
