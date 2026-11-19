package me.kavishdevar.librepods.presentation.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.R

@Composable
fun PrivacyPolicyPage(
    onForward: () -> Unit
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier.background(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(42.dp)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "AnDPoP communicates with your AirPods over Bluetooth. It saves device information and settings on your phone so it can reconnect and restore your preferences.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "Nearby devices access is required. Notifications, phone access and display over other apps are optional and used for their respective features.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "AnDPoP includes no ads, analytics or purchases. Links to LibrePods open in your browser.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = onForward,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.andpop_continue))
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
