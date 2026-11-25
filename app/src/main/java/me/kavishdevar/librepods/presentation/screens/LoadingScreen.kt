package me.kavishdevar.librepods.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.presentation.theme.LibrePodsTheme
import me.kavishdevar.librepods.presentation.theme.LocalAndpopPalette

@Composable
fun LoadingScreen() {
    val palette = LocalAndpopPalette.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg),
        contentAlignment = Alignment.Center,
    ) {
        CircularWavyProgressIndicator(
            modifier = Modifier.size(96.dp),
            color = palette.pri,
        )
    }
}

@Preview
@Composable
fun LoadingScreenPreview() {
    LibrePodsTheme(m3eEnabled = true) {
        LoadingScreen()
    }
}
