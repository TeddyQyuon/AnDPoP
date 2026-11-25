package me.kavishdevar.librepods.data.updates

import androidx.compose.runtime.Composable
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.screens.EqualizerScreen
import me.kavishdevar.librepods.presentation.viewmodel.demoState

/** What's new in AnDPoP 2.0, shown by the release notes screen. */
val update2_0_0 = listOf(
    UpdateItem(
        titleRes = R.string.andpop_release_2_title,
        descriptionRes = R.string.andpop_release_2_body,
        demoComposeable = @Composable {
            EqualizerScreen(state = demoState, setCustomEqEnabled = {}, setCustomEq = { _, _, _ -> })
        },
    ),
)

val updates = update2_0_0
