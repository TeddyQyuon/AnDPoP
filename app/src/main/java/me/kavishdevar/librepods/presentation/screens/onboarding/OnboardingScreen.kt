package me.kavishdevar.librepods.presentation.screens.onboarding

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.theme.AndpopType
import me.kavishdevar.librepods.presentation.theme.LibrePodsTheme
import me.kavishdevar.librepods.presentation.theme.LocalAndpopPalette
import me.kavishdevar.librepods.utils.XposedState
import me.kavishdevar.librepods.utils.bypassDeviceCheck
import me.kavishdevar.librepods.utils.isSupported
import me.kavishdevar.librepods.utils.removeDeviceCheckBypass

@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
) {
    val sharedPreferences = LocalContext.current.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val isSupported = isSupported(sharedPreferences) || XposedState.bluetoothScopeEnabled

    val state = rememberCarouselState(
        initialItem = 0,
        itemCount = { 4 }
    )

    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val titles = listOf(
        null,
        null,
        stringResource(R.string.not_supported),
        stringResource(R.string.permissions),
    )

    val animationScope = rememberCoroutineScope()

    BackHandler {
        animationScope.launch {
            if (state.canScrollBackward) {
                val targetItem = if (isSupported && state.currentItem == 3) 1 else state.currentItem - 1
                state.animateScrollToItem(targetItem)
            }
        }
    }

    LibrePodsTheme(
        m3eEnabled = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(topPadding))
            HorizontalUncontainedCarousel(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(12.dp),
                state = state,
                itemWidth = LocalWindowInfo.current.containerDpSize.width - 24.dp,
                userScrollEnabled = false
            ) { index ->
                val shape = rememberMaskShape(RoundedCornerShape(36.dp))
                Surface(
                    shape = shape,
                    color = LocalAndpopPalette.current.bg,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(36.dp))
                ) {
                    Column(
                        modifier = Modifier,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        titles[index]?.let {
                            Text(
                                text = it,
                                modifier = Modifier.padding(top = 16.dp),
                                style = AndpopType.pageTitle,
                                color = LocalAndpopPalette.current.on,
                                textAlign = TextAlign.Center
                            )
                        }
                        when (index) {
                            0 -> {
                                WelcomePage(onForward = {
                                    animationScope.launch { state.animateScrollToItem(1) }
                                })
                            }
                            1 -> {
                                PrivacyPolicyPage(
                                    onForward = {
                                        animationScope.launch {
                                            if (isSupported) state.animateScrollToItem(3) else state.animateScrollToItem(2)
                                        }
                                    }
                                )
                            }
                            2 -> {
                                NotSupportedPage(
                                    bypassCompatibilityCheck = {
                                        bypassDeviceCheck(sharedPreferences)
                                        animationScope.launch {
                                            state.animateScrollToItem(3)
                                        }
                                    }
                                )
                            }
                            3 -> {
                                PermissionsPage(
                                    onBackward = {
                                        animationScope.launch {
                                            if (state.canScrollBackward) state.animateScrollToItem(if (isSupported) 1 else 2)
                                            if (!isSupported) {
                                                removeDeviceCheckBypass(sharedPreferences)
                                            }
                                        }
                                    },
                                    onForward = onOnboardingComplete
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(bottomPadding))
        }
    }
}


@Preview
@Composable
fun OnboardingScreenPreview(){
    OnboardingScreen {}
}


/** Step dots shared by the welcome and privacy pages. */
@Composable
internal fun OnboardingDots(active: Int, count: Int = 3) {
    val palette = LocalAndpopPalette.current
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(vertical = 14.dp),
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
private fun WelcomePage(onForward: () -> Unit) {
    val palette = LocalAndpopPalette.current
    val bob = rememberInfiniteTransition(label = "Welcome float")
    val lift by bob.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(tween<Float>(2500), RepeatMode.Reverse),
        label = "Welcome lift",
    )
    Column(
        Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(240.dp).background(
                    androidx.compose.ui.graphics.Brush.radialGradient(
                        listOf(palette.pri.copy(alpha = 0.28f), androidx.compose.ui.graphics.Color.Transparent),
                    ),
                ),
            )
            androidx.compose.foundation.Image(
                androidx.compose.ui.res.painterResource(R.drawable.amber_pro_2_case), null,
                Modifier.size(width = 176.dp, height = 130.dp).offset(y = (28 + lift / 2).dp),
            )
            androidx.compose.foundation.Image(
                androidx.compose.ui.res.painterResource(R.drawable.amber_pro_2_left), null,
                Modifier.size(width = 64.dp, height = 86.dp).offset(x = (-104).dp, y = (-46 + lift).dp)
                    .graphicsLayer { rotationZ = -8f },
            )
            androidx.compose.foundation.Image(
                androidx.compose.ui.res.painterResource(R.drawable.amber_pro_2_right), null,
                Modifier.size(width = 64.dp, height = 86.dp).offset(x = 104.dp, y = (-56 - lift).dp),
            )
        }
        OnboardingDots(0)
        Text(
            stringResource(R.string.andpop_welcome_title),
            Modifier.fillMaxWidth(),
            style = AndpopType.display,
            color = palette.on,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.andpop_welcome_body),
            Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 24.dp),
            style = AndpopType.bodySmall,
            color = palette.on2,
            textAlign = TextAlign.Center,
        )
        androidx.compose.material3.Button(
            onClick = onForward,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = palette.pri, contentColor = palette.onPri,
            ),
        ) {
            Text(stringResource(R.string.andpop_get_started), style = AndpopType.button)
        }
        Spacer(Modifier.height(16.dp))
    }
}
