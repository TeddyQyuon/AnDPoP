// SPDX-License-Identifier: GPL-3.0-or-later
package me.kavishdevar.librepods.account

import android.util.Patterns
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.components.AndpopCard
import me.kavishdevar.librepods.presentation.components.AndpopGroup
import me.kavishdevar.librepods.presentation.components.AndpopPageHeader
import me.kavishdevar.librepods.presentation.components.AndpopRow
import me.kavishdevar.librepods.presentation.components.AndpopSectionHeader
import me.kavishdevar.librepods.presentation.components.InfoBanner
import me.kavishdevar.librepods.presentation.components.PillTone
import me.kavishdevar.librepods.presentation.components.StatusPill
import me.kavishdevar.librepods.presentation.theme.AndpopIcons
import me.kavishdevar.librepods.presentation.theme.AndpopType
import me.kavishdevar.librepods.presentation.theme.LocalAndpopPalette

@Composable
internal fun AccountScreen(
    state: AccountUiState,
    model: AccountViewModel,
    connectedDevice: RememberedAirPods?,
) {
    AccountScreen(state = state, model = model, connectedDevice = connectedDevice, onExit = null)
}

@Composable
internal fun AccountScreen(
    state: AccountUiState,
    model: AccountViewModel,
    connectedDevice: RememberedAirPods?,
    onExit: (() -> Unit)? = null,
) {
    val palette = LocalAndpopPalette.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var emailDraft by rememberSaveable { mutableStateOf("") }

    val handleBack: () -> Unit = {
        if (onExit != null && (state.page == AccountPage.Profile || (!state.entered && state.page == AccountPage.Login))) {
            onExit()
        } else {
            model.back()
        }
    }
    BackHandler(enabled = onExit != null || state.entered || state.page != AccountPage.Profile, onBack = handleBack)

    Scaffold(
        containerColor = palette.bg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val showBack = onExit != null || state.entered || state.page != AccountPage.Profile
                if (showBack) {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (state.page == AccountPage.Profile) {
                            IconButton(
                                onClick = handleBack,
                                enabled = !state.busy,
                                modifier = Modifier.size(48.dp).clip(CircleShape).background(palette.s2),
                            ) {
                                Icon(AndpopIcons.Back, stringResource(R.string.back), tint = palette.on)
                            }
                            Text(
                                stringResource(R.string.andpop_account),
                                Modifier.padding(start = 6.dp),
                                color = palette.on,
                                style = AndpopType.titleSmall,
                            )
                        } else {
                            TextButton(
                                onClick = handleBack,
                                enabled = !state.busy,
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) {
                                Icon(AndpopIcons.Back, null, Modifier.size(18.dp), tint = palette.pri)
                                Spacer(Modifier.size(6.dp))
                                Text(stringResource(R.string.back), color = palette.pri, style = AndpopType.button)
                            }
                        }
                    }
                }

                if (state.page != AccountPage.Profile) {
                    val title = when (state.page) {
                        AccountPage.Login -> stringResource(R.string.andpop_account_login)
                        AccountPage.SignUp -> stringResource(R.string.andpop_account_create)
                        AccountPage.Forgot -> stringResource(R.string.andpop_account_forgot_title)
                        AccountPage.Reset -> stringResource(R.string.andpop_account_reset_title)
                        AccountPage.Project -> stringResource(R.string.andpop_account_service_title)
                        AccountPage.Profile -> ""
                    }
                    val subtitle = when (state.page) {
                        AccountPage.Login -> stringResource(R.string.andpop_account_login_desc)
                        AccountPage.SignUp -> stringResource(R.string.andpop_account_signup_desc)
                        AccountPage.Forgot -> stringResource(R.string.andpop_account_forgot_desc)
                        else -> null
                    }
                    AndpopPageHeader(title = title, subtitle = subtitle)
                }

                if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = palette.pri)

                state.message?.let { msg ->
                    InfoBanner(
                        text = msg,
                        warning = true,
                        icon = AndpopIcons.Warning,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }

                val forgottenMsg = stringResource(R.string.andpop_account_forgotten)
                val undoLabel = stringResource(R.string.andpop_undo)

                key(state.page) {
                    when (state.page) {
                        AccountPage.Profile -> ProfileCard(
                            state = state,
                            model = model,
                            connected = connectedDevice,
                            onForgetWithUndo = { previous ->
                                model.remember(null)
                                scope.launch {
                                    val res = snackbarHostState.showSnackbar(
                                        message = forgottenMsg,
                                        actionLabel = undoLabel,
                                        duration = SnackbarDuration.Short,
                                    )
                                    if (res == SnackbarResult.ActionPerformed) {
                                        model.remember(previous)
                                    }
                                }
                            },
                            onGuest = {
                                model.guest()
                                onExit?.invoke()
                            },
                        )
                        AccountPage.Login, AccountPage.SignUp, AccountPage.Forgot, AccountPage.Reset ->
                            AccountForm(
                                state = state,
                                model = model,
                                emailDraft = emailDraft,
                                updateEmailDraft = { emailDraft = it },
                                onGuest = {
                                    model.guest()
                                    onExit?.invoke()
                                },
                            )
                        AccountPage.Project -> ProjectForm(state, model) {
                            model.guest()
                            onExit?.invoke()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(
    state: AccountUiState,
    model: AccountViewModel,
    connected: RememberedAirPods?,
    onForgetWithUndo: (RememberedAirPods) -> Unit,
    onGuest: () -> Unit,
) {
    val palette = LocalAndpopPalette.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (state.signedIn) {
            val initials = state.email.trim().take(2).uppercase().ifBlank { "AN" }
            Box(
                Modifier.size(72.dp).clip(CircleShape).background(palette.pc),
                contentAlignment = Alignment.Center,
            ) {
                Text(initials, color = palette.onPc, style = AndpopType.title)
            }
            Text(
                state.email.ifBlank { stringResource(R.string.andpop_account_signed_in) },
                Modifier.semantics { heading() },
                color = palette.on,
                style = AndpopType.title,
                textAlign = TextAlign.Center,
            )
            StatusPill(stringResource(R.string.andpop_account_signed_in), PillTone.Success)
        } else {
            Box(
                Modifier.size(72.dp).clip(CircleShape).background(palette.s2),
                contentAlignment = Alignment.Center,
            ) {
                Icon(AndpopIcons.Person, null, Modifier.size(36.dp), tint = palette.on)
            }
            Text(
                stringResource(R.string.andpop_account_guest),
                Modifier.semantics { heading() },
                color = palette.on,
                style = AndpopType.title,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.andpop_account_guest_desc),
                color = palette.on2,
                style = AndpopType.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
    }

    AndpopSectionHeader(stringResource(R.string.andpop_account_remembered))
    val device = state.remembered
    if (device != null) {
        AndpopGroup {
            row { shape ->
                AndpopRow(
                    shape = shape,
                    title = device.name,
                    supporting = device.model.takeIf { it.isNotBlank() && it != device.name },
                    icon = AndpopIcons.Buds,
                    trailing = {
                        TextButton(onClick = { onForgetWithUndo(device) }, enabled = !state.busy) {
                            Text(stringResource(R.string.andpop_account_forget), color = palette.pri, style = AndpopType.button)
                        }
                    },
                )
            }
        }
    } else {
        AndpopCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.andpop_account_none_saved), color = palette.on2, style = AndpopType.bodySmall)
                if (connected != null) {
                    OutlinedButton(
                        onClick = { model.remember(connected) },
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        border = BorderStroke(1.dp, palette.line2),
                    ) {
                        Text(stringResource(R.string.andpop_account_remember_connected), color = palette.pri, style = AndpopType.button)
                    }
                }
            }
        }
    }

    if (connected != null && device != null && connected != device) {
        OutlinedButton(
            onClick = { model.remember(connected) },
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            border = BorderStroke(1.dp, palette.line2),
        ) {
            Text(stringResource(R.string.andpop_account_remember_connected), color = palette.pri, style = AndpopType.button)
        }
    }

    if (state.signedIn) {
        AndpopSectionHeader(stringResource(R.string.andpop_account_security))
        AndpopGroup {
            row { shape ->
                AndpopRow(
                    shape = shape,
                    title = stringResource(R.string.andpop_account_change_password),
                    icon = AndpopIcons.Lock,
                    enabled = !state.busy && state.email.isNotBlank(),
                    onClick = { model.page(AccountPage.Forgot) },
                )
            }
            row { shape ->
                AndpopRow(
                    shape = shape,
                    title = stringResource(R.string.andpop_account_sign_out),
                    supporting = stringResource(R.string.andpop_account_sign_out_desc),
                    icon = AndpopIcons.Logout,
                    enabled = !state.busy,
                    showChevron = false,
                    onClick = model::signOut,
                )
            }
        }

        AndpopSectionHeader(stringResource(R.string.andpop_account_delete))
        AndpopCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "${stringResource(R.string.andpop_account_delete_desc)} ${stringResource(R.string.andpop_account_delete_web_note)}",
                    color = palette.on2,
                    style = AndpopType.bodySmall,
                )
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    border = BorderStroke(1.dp, palette.error.copy(alpha = 0.45f)),
                ) {
                    Icon(AndpopIcons.Trash, null, Modifier.size(18.dp), tint = palette.error)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.andpop_account_delete), color = palette.error, style = AndpopType.button)
                }
            }
        }
    } else if (state.configured) {
        val cardShape = RoundedCornerShape(24.dp)
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clip(cardShape)
                .background(Brush.linearGradient(listOf(palette.pc.copy(alpha = 0.72f), palette.s1)))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.andpop_account_promo_title), color = palette.on, style = AndpopType.titleSmall)
            Text(
                stringResource(R.string.andpop_account_promo_body),
                color = palette.on2,
                style = AndpopType.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Button(
                onClick = { model.page(AccountPage.SignUp) },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = palette.pri, contentColor = palette.onPri),
            ) {
                Text(stringResource(R.string.andpop_account_create), style = AndpopType.button)
            }
            FilledTonalButton(
                onClick = { model.page(AccountPage.Login) },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = palette.pc, contentColor = palette.onPc),
            ) {
                Text(stringResource(R.string.andpop_account_login), style = AndpopType.button)
            }
        }
        if (!state.entered) {
            AccountLink(stringResource(R.string.andpop_account_continue_guest), !state.busy, onGuest)
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = palette.s2,
            icon = { Icon(AndpopIcons.Trash, null, tint = palette.error) },
            title = { Text(stringResource(R.string.andpop_account_delete_dialog_title), style = AndpopType.title) },
            text = { Text(stringResource(R.string.andpop_account_delete_dialog_body), color = palette.on2, style = AndpopType.bodySmall) },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; model.deleteAccount() }) {
                    Text(stringResource(R.string.andpop_account_delete), color = palette.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun AccountForm(
    state: AccountUiState,
    model: AccountViewModel,
    emailDraft: String,
    updateEmailDraft: (String) -> Unit,
    onGuest: () -> Unit,
) {
    val palette = LocalAndpopPalette.current
    val page = state.page
    var email by rememberSaveable {
        mutableStateOf(if (page == AccountPage.Forgot) state.email.ifBlank { emailDraft } else emailDraft)
    }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    val emailValid = email.trim().length <= 254 && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val lenOk = password.length >= 8
    val bytesOk = password.isNotEmpty() && password.toByteArray(Charsets.UTF_8).size <= 72
    val confirmedPassword = validNewPassword(password) && password == confirmation
    val active = state.configured && !state.busy
    val invalidEmail = email.isNotBlank() && !emailValid

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (page != AccountPage.Reset) {
            OutlinedTextField(
                value = email,
                onValueChange = { value ->
                    email = value
                    if (!state.signedIn) updateEmailDraft(value)
                },
                label = { Text(stringResource(R.string.andpop_account_email)) },
                leadingIcon = { Icon(AndpopIcons.Mail, null, tint = palette.on3) },
                modifier = Modifier.fillMaxWidth(),
                enabled = active,
                singleLine = true,
                isError = invalidEmail,
                supportingText = if (invalidEmail) ({ Text(stringResource(R.string.andpop_account_email_invalid)) }) else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = palette.s2,
                    unfocusedContainerColor = palette.s2,
                    focusedBorderColor = palette.pri,
                    unfocusedBorderColor = palette.line2,
                ),
            )
        }
        if (page != AccountPage.Forgot) {
            PasswordField(
                value = password,
                onChange = { password = it },
                enabled = active,
                label = stringResource(if (page == AccountPage.Reset) R.string.andpop_account_new_password else R.string.andpop_account_password),
            )
        }
        if (page == AccountPage.SignUp || page == AccountPage.Reset) {
            Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PasswordRule(lenOk, stringResource(R.string.andpop_account_pw_rule_len))
                PasswordRule(bytesOk, stringResource(R.string.andpop_account_pw_rule_bytes))
            }
            val mismatch = confirmation.isNotEmpty() && confirmation != password
            PasswordField(
                value = confirmation,
                onChange = { confirmation = it },
                enabled = active,
                label = stringResource(R.string.andpop_account_confirm_password),
                isError = mismatch,
                supportingText = if (mismatch) stringResource(R.string.andpop_account_pw_mismatch) else null,
            )
        }
        if (page == AccountPage.Login) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { model.page(AccountPage.Forgot) }, enabled = active) {
                    Text(stringResource(R.string.andpop_account_forgot_link), color = palette.pri, style = AndpopType.label)
                }
            }
        }
        when (page) {
            AccountPage.Login -> AccountPrimaryButton(
                stringResource(R.string.andpop_account_login),
                active && emailValid && password.isNotEmpty(),
            ) { model.signIn(email.trim(), password) }
            AccountPage.SignUp -> AccountPrimaryButton(
                stringResource(R.string.andpop_account_create),
                active && emailValid && confirmedPassword,
            ) { model.signUp(email.trim(), password) }
            AccountPage.Forgot -> AccountPrimaryButton(
                stringResource(R.string.andpop_account_send_reset),
                active && emailValid,
            ) { model.forgot(email.trim()) }
            AccountPage.Reset -> AccountPrimaryButton(
                stringResource(R.string.andpop_account_save_password),
                active && confirmedPassword,
            ) { model.resetPassword(password) }
            else -> Unit
        }

        if (page == AccountPage.Login || page == AccountPage.SignUp) {
            if (state.showSocialSignIn) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HorizontalDivider(Modifier.weight(1f), color = palette.line)
                    Text(stringResource(R.string.andpop_account_or), color = palette.on3, style = AndpopType.caption)
                    HorizontalDivider(Modifier.weight(1f), color = palette.line)
                }
                AccountOutline(stringResource(R.string.andpop_account_continue_google), active) { model.signInWithProvider(FlowPurpose.Google) }
                AccountOutline(stringResource(R.string.andpop_account_continue_apple), active) { model.signInWithProvider(FlowPurpose.Apple) }
            }
            if (page == AccountPage.Login) {
                AccountOutline(stringResource(R.string.andpop_account_create), active) { model.page(AccountPage.SignUp) }
            } else {
                AccountLink(stringResource(R.string.andpop_account_already_login), active) { model.page(AccountPage.Login) }
            }
            AccountLink(stringResource(R.string.andpop_account_continue_guest), active, onGuest)
        }
    }
}

@Composable
private fun PasswordRule(met: Boolean, text: String) {
    val palette = LocalAndpopPalette.current
    val color = if (met) palette.success else palette.on3
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(if (met) AndpopIcons.Check else AndpopIcons.Minus, null, Modifier.size(16.dp), tint = color)
        Text(text, color = color, style = AndpopType.caption)
    }
}

@Composable
private fun ProjectForm(state: AccountUiState, model: AccountViewModel, onGuest: () -> Unit) {
    val palette = LocalAndpopPalette.current
    var url by rememberSaveable { mutableStateOf(state.projectUrl) }
    var key by remember { mutableStateOf("") }
    Text(stringResource(R.string.andpop_account_service_body), color = palette.on2, style = AndpopType.bodySmall)
    AndpopCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                url, { url = it }, label = { Text(stringResource(R.string.andpop_account_supabase_url)) },
                placeholder = { Text("https://project-ref.supabase.co") },
                modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !state.busy,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )
            OutlinedTextField(
                key, { key = it }, label = { Text(stringResource(R.string.andpop_account_publishable_key)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !state.busy,
            )
            AccountPrimaryButton(stringResource(R.string.andpop_account_save_connection), !state.busy && url.isNotBlank() && key.isNotBlank()) {
                model.connectProject(url, key)
                key = ""
            }
        }
    }
    AccountLink(stringResource(R.string.andpop_account_continue_guest), !state.busy, onGuest)
}

@Composable
private fun PasswordField(
    value: String,
    onChange: (String) -> Unit,
    enabled: Boolean,
    label: String,
    isError: Boolean = false,
    supportingText: String? = null,
) {
    val palette = LocalAndpopPalette.current
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        leadingIcon = { Icon(AndpopIcons.Lock, null, tint = palette.on3) },
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        isError = isError,
        supportingText = supportingText?.let { ({ Text(it) }) },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = palette.s2,
            unfocusedContainerColor = palette.s2,
            focusedBorderColor = palette.pri,
            unfocusedBorderColor = palette.line2,
        ),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }, enabled = enabled) {
                Icon(
                    imageVector = if (visible) AndpopIcons.EyeOff else AndpopIcons.Eye,
                    contentDescription = stringResource(if (visible) R.string.andpop_account_hide_pw else R.string.andpop_account_show_pw),
                    tint = palette.on2,
                )
            }
        },
    )
}

@Composable
private fun AccountPrimaryButton(label: String, enabled: Boolean, action: () -> Unit) {
    val palette = LocalAndpopPalette.current
    Button(
        onClick = action,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = palette.pri, contentColor = palette.onPri),
    ) {
        Text(label, style = AndpopType.button)
    }
}

@Composable
private fun AccountOutline(label: String, enabled: Boolean, action: () -> Unit) {
    val palette = LocalAndpopPalette.current
    OutlinedButton(
        onClick = action,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        enabled = enabled,
        border = BorderStroke(1.dp, palette.line2),
    ) {
        Text(label, color = palette.pri, style = AndpopType.button)
    }
}

@Composable
private fun AccountLink(label: String, enabled: Boolean, action: () -> Unit) {
    val palette = LocalAndpopPalette.current
    TextButton(onClick = action, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = enabled) {
        Text(label, color = palette.pri, style = AndpopType.button)
    }
}
