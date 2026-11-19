// SPDX-License-Identifier: GPL-3.0-or-later
package me.kavishdevar.librepods.account

import android.util.Patterns
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.presentation.theme.LocalAndpopPalette
import me.kavishdevar.librepods.presentation.theme.devicePanel

@Composable
internal fun AccountScreen(
    state: AccountUiState,
    model: AccountViewModel,
    connectedDevice: RememberedAirPods?,
) {
    BackHandler(onBack = model::back)
    val palette = LocalAndpopPalette.current
    Box(
        Modifier.fillMaxSize().background(palette.scheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing).imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier.widthIn(max = 540.dp).fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val canGoBack = state.entered || state.page != AccountPage.Profile
            if (canGoBack) {
                TextButton(onClick = model::back, enabled = !state.busy) {
                    Text(if (state.page == AccountPage.Profile) "Close" else "Back")
                }
            }
            val title = when (state.page) {
                AccountPage.Profile -> if (state.signedIn) "Your account" else if (state.entered) "Guest profile" else "Welcome to AnDPoP"
                AccountPage.Login -> "Log in"
                AccountPage.SignUp -> "Create an account"
                AccountPage.Forgot -> "Forgot your password?"
                AccountPage.Reset -> "Set a new password"
                AccountPage.Project -> "Connect account service"
            }
            Text(
                title,
                Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineLarge,
            )
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.message?.let {
                Text(
                    it, Modifier.fillMaxWidth().devicePanel(palette).padding(14.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    color = palette.scheme.onSurface, style = MaterialTheme.typography.bodyMedium,
                )
            }
            key(state.page) {
                when (state.page) {
                    AccountPage.Profile -> ProfileCard(state, model, connectedDevice)
                    AccountPage.Login, AccountPage.SignUp, AccountPage.Forgot, AccountPage.Reset ->
                        AccountForm(state, model)
                    AccountPage.Project -> ProjectForm(state, model)
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(state: AccountUiState, model: AccountViewModel, connected: RememberedAirPods?) {
    val palette = LocalAndpopPalette.current
    var delete by remember { mutableStateOf(false) }
    if (state.entered) {
        Panel {
            Text(if (state.signedIn) state.email.ifBlank { "Signed in" } else "On this phone", style = MaterialTheme.typography.titleMedium)
            Text(
                "The remembered AirPods and app preferences belong to this profile on this phone.",
                color = palette.scheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium,
            )
            HorizontalDivider(color = palette.scheme.outlineVariant)
            Text("Remembered AirPods", style = MaterialTheme.typography.labelLarge)
            val device = state.remembered
            if (device == null) {
                Text("No AirPods saved yet.", color = palette.scheme.onSurfaceVariant)
            } else {
                Text(device.name, style = MaterialTheme.typography.titleMedium)
                if (device.model.isNotBlank() && device.model != device.name) {
                    Text(device.model, color = palette.scheme.onSurfaceVariant)
                }
            }
            if (connected != null && connected != device) {
                OutlinedButton(onClick = { model.remember(connected) }, enabled = !state.busy) {
                    Text("Remember connected AirPods")
                }
            } else if (connected == null && device == null) {
                Text("Connect your AirPods to save them here.", color = palette.scheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall)
            }
            if (device != null) {
                TextButton(onClick = { model.remember(null) }, enabled = !state.busy) {
                    Text("Forget saved device")
                }
            }
        }
    } else {
        Text("Set up a profile for your AirPods and preferences.", color = palette.scheme.onSurfaceVariant)
    }
    if (state.signedIn) {
        TextButton(onClick = { model.page(AccountPage.Forgot) }, enabled = !state.busy && state.email.isNotBlank()) {
            Text("Change password")
        }
        OutlinedButton(onClick = model::signOut, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
            Text("Sign out to guest")
        }
        TextButton(onClick = { delete = true }, enabled = !state.busy) {
            Text("Delete account", color = palette.scheme.error)
        }
    } else {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AccountButton("Log in", !state.busy) { model.page(AccountPage.Login) }
            OutlinedButton(onClick = { model.page(AccountPage.SignUp) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                Text("Create an account")
            }
            TextButton(onClick = model::guest, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                Text("Continue as guest")
            }
        }
    }
    if (delete) {
        AlertDialog(
            onDismissRequest = { delete = false },
            title = { Text("Delete your account?") },
            text = { Text("This removes your online account and its saved profile on this phone.") },
            confirmButton = {
                TextButton(onClick = { delete = false; model.deleteAccount() }) { Text("Delete account") }
            },
            dismissButton = { TextButton(onClick = { delete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun AccountForm(state: AccountUiState, model: AccountViewModel) {
    val palette = LocalAndpopPalette.current
    val page = state.page
    var email by rememberSaveable { mutableStateOf(if (page == AccountPage.Forgot) state.email else "") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    val emailValid = email.trim().length <= 254 && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val confirmedPassword = validNewPassword(password) && password == confirmation
    val active = state.configured && !state.busy
    if (!state.configured && page != AccountPage.Reset) {
        Panel {
            Text("Connect your account service to use email, Google, or Apple.", color = palette.scheme.onSurfaceVariant)
            AccountButton("Connect account service", !state.busy) { model.page(AccountPage.Project) }
        }
    }

    if (page == AccountPage.Forgot) {
        Text("We’ll send a link to set a new password.", color = palette.scheme.onSurfaceVariant)
    }
    Panel {
        if (page != AccountPage.Reset) {
            OutlinedTextField(
                email, { email = it }, label = { Text("Email address") },
                modifier = Modifier.fillMaxWidth(), enabled = active, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )
        }
        if (page != AccountPage.Forgot) {
            PasswordField(
                value = password, onChange = { password = it }, enabled = active,
                label = if (page == AccountPage.Reset) "New password" else "Password",
            )
        }
        if (page == AccountPage.SignUp || page == AccountPage.Reset) {
            PasswordField(confirmation, { confirmation = it }, active, "Confirm password")
            Text(
                if (confirmation.isNotEmpty() && confirmation != password) "Passwords must match." else "Use at least 8 characters.",
                color = palette.scheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall,
            )
        }
        when (page) {
            AccountPage.Login -> AccountButton("Log in", active && emailValid && password.isNotEmpty()) {
                model.signIn(email.trim(), password)
                password = ""
            }
            AccountPage.SignUp -> AccountButton("Create an account", active && emailValid && confirmedPassword) {
                model.signUp(email.trim(), password)
                password = ""
                confirmation = ""
            }
            AccountPage.Forgot -> AccountButton("Send reset link", active && emailValid) { model.forgot(email.trim()) }
            AccountPage.Reset -> AccountButton("Save password", active && confirmedPassword) {
                model.resetPassword(password)
                password = ""
                confirmation = ""
            }
            else -> Unit
        }
    }
    if (page == AccountPage.Login || page == AccountPage.SignUp) {
        if (page == AccountPage.Login) {
            TextButton(onClick = { model.page(AccountPage.Forgot) }, enabled = !state.busy) {
                Text("Forgot password?")
            }
        }
        OutlinedButton(
            onClick = { model.signInWithProvider(FlowPurpose.Google) }, enabled = active,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) { Text("Continue with Google") }
        OutlinedButton(
            onClick = { model.signInWithProvider(FlowPurpose.Apple) }, enabled = active,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) { Text("Continue with Apple") }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val switch = if (page == AccountPage.Login) AccountPage.SignUp else AccountPage.Login
            TextButton(onClick = { model.page(switch) }, enabled = !state.busy) {
                Text(if (page == AccountPage.Login) "Create an account" else "Log in")
            }
            TextButton(onClick = model::guest, enabled = !state.busy) { Text("Continue as guest") }
        }
        if (state.configured) {
            TextButton(onClick = { model.page(AccountPage.Project) }, enabled = !state.busy) {
                Text("Account service settings")
            }
        }
    }
}

@Composable
private fun ProjectForm(state: AccountUiState, model: AccountViewModel) {
    val palette = LocalAndpopPalette.current
    var url by rememberSaveable { mutableStateOf(state.projectUrl) }
    var key by remember { mutableStateOf("") }
    Text(
        "Enter the project URL and public publishable key for the Supabase project you use for AnDPoP.",
        color = palette.scheme.onSurfaceVariant,
    )
    Panel {
        OutlinedTextField(
            url, { url = it }, label = { Text("Supabase project URL") },
            placeholder = { Text("https://project-ref.supabase.co") },
            modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !state.busy,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        )
        OutlinedTextField(
            key, { key = it }, label = { Text("Publishable key") },
            modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !state.busy,
        )
        AccountButton("Save connection", !state.busy && url.isNotBlank() && key.isNotBlank()) {
            model.connectProject(url, key)
            key = ""
        }
    }
    TextButton(onClick = model::guest, enabled = !state.busy) { Text("Continue as guest") }
}

@Composable
private fun PasswordField(value: String, onChange: (String) -> Unit, enabled: Boolean, label: String) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value, onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(),
        enabled = enabled, singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            TextButton(onClick = { visible = !visible }, enabled = enabled) {
                Text(if (visible) "Hide" else "Show")
            }
        },
    )
}

@Composable
private fun AccountButton(label: String, enabled: Boolean, action: () -> Unit) {
    Button(action, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = enabled) { Text(label) }
}

@Composable
private fun Panel(content: @Composable ColumnScope.() -> Unit) {
    val palette = LocalAndpopPalette.current
    Column(
        Modifier.fillMaxWidth().devicePanel(palette).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp), content = content,
    )
}
