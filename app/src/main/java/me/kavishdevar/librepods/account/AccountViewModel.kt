// SPDX-License-Identifier: GPL-3.0-or-later
package me.kavishdevar.librepods.account

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.kavishdevar.librepods.R

private val defaultAuthProject = AuthProject(
    url = "https://btfbwwnbkarlpcqmkzzb.supabase.co",
    key = "sb_publishable_s3GqJUDO5e_3PAC_aqytLg_tSdk5tSr",
)

internal enum class AccountPage { Profile, Login, SignUp, Forgot, Reset, Project }

internal data class AccountUiState(
    val open: Boolean = false,
    val entered: Boolean = false,
    val page: AccountPage = AccountPage.Profile,
    val busy: Boolean = true,
    val configured: Boolean = false,
    val showSocialSignIn: Boolean = false,
    val projectUrl: String = "",
    val signedIn: Boolean = false,
    val email: String = "",
    val remembered: RememberedAirPods? = null,
    val message: String? = null,
)

internal class AccountViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val profiles = ProfilePreferences(preferences)
    private val vault = AccountVault(application)
    private var memory = AccountMemory()
    private val mutableState = MutableStateFlow(AccountUiState())
    val state = mutableState.asStateFlow()
    private val browserChannel = Channel<Uri>(Channel.BUFFERED)
    val browser = browserChannel.receiveAsFlow()
    private var operation: Job? = null
    private var returnFromProject = AccountPage.Profile

    private fun str(resId: Int): String = getApplication<Application>().getString(resId)

    init {
        val entered = profiles.hasEntered || preferences.getBoolean("onboarding_complete", false)
        mutableState.update { it.copy(open = false, entered = entered) }
        perform {
            val restored = try {
                withContext(Dispatchers.IO) { vault.read() }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                message(str(R.string.andpop_account_msg_corrupt))
                AccountMemory()
            }
            // Keep existing custom projects and their stored sessions.
            memory = if (restored.project == null) restored.copy(project = defaultAuthProject) else restored
            val signed = restored.session
            if (restored.recovering && signed != null && !signed.expiresSoon(now())) {
                mutableState.update { it.copy(open = true, page = AccountPage.Reset) }
            } else {
                if (restored.recovering) save(memory.copy(session = null, recovering = false))
                val user = memory.session
                val project = memory.project
                if (project != null && user != null) {
                    try {
                        if (user.expiresSoon(now())) refresh(project, user)
                    } catch (failure: AuthFailure) {
                        if (failure.status == 401 || failure.code == "refresh_token_not_found" ||
                            failure.code == "refresh_token_already_used"
                        ) {
                            save(memory.copy(session = null))
                            message(str(R.string.andpop_account_msg_expired))
                        } else {
                            message(str(R.string.andpop_account_msg_offline))
                        }
                    } catch (_: IOException) {
                        message(str(R.string.andpop_account_msg_offline))
                    }
                }
            }
            val expected = profileId()
            if (profiles.active != expected) useProfile(expected)
            publish()
        }
    }

    fun show(initialPage: AccountPage? = null) {
        val target = when {
            memory.recovering -> AccountPage.Reset
            initialPage != null -> initialPage
            else -> AccountPage.Profile
        }
        mutableState.update { it.copy(open = true, page = target, message = null) }
        publish()
    }

    fun consumeOpenRequest() {
        mutableState.update { it.copy(open = false) }
    }

    fun ensureGuestEntered() {
        if (!profiles.hasEntered) {
            perform {
                if (memory.session == null) {
                    useProfile(GUEST_PROFILE)
                }
                finishEntering()
            }
        }
    }

    fun close() {
        if (state.value.entered && !state.value.busy && !memory.recovering) {
            mutableState.update { it.copy(open = false) }
        }
    }

    fun page(page: AccountPage) {
        if (state.value.busy) return
        val destination = when (page) {
            AccountPage.Login, AccountPage.SignUp, AccountPage.Forgot ->
                if (memory.project == null) AccountPage.Project else page
            else -> page
        }
        if (destination == AccountPage.Project) {
            if (state.value.signedIn || state.value.page == AccountPage.Project) return
            returnFromProject = state.value.page
        }
        mutableState.update { it.copy(page = destination, message = null) }
    }

    fun back() {
        if (state.value.busy) return
        when (state.value.page) {
            AccountPage.Profile -> close()
            AccountPage.Project -> page(returnFromProject)
            AccountPage.Forgot -> page(if (state.value.signedIn) AccountPage.Profile else AccountPage.Login)
            AccountPage.SignUp -> page(AccountPage.Login)
            AccountPage.Login -> page(AccountPage.Profile)
            AccountPage.Reset -> perform {
                save(memory.copy(session = null, flow = null, recovering = false))
                useProfile(GUEST_PROFILE)
                publish()
                mutableState.update { it.copy(page = AccountPage.Login) }
            }
        }
    }

    fun connectProject(url: String, key: String) {
        val project = AuthProject.parse(url, key)
        if (project == null) {
            message(str(R.string.andpop_account_msg_invalid_project))
            return
        }
        if (memory.session != null) return
        perform {
            save(AccountMemory(project = project))
            publish()
            mutableState.update { it.copy(page = AccountPage.Login) }
        }
    }

    fun guest() = perform {
        if (memory.session != null) return@perform
        useProfile(GUEST_PROFILE)
        finishEntering()
    }

    fun signIn(email: String, password: String) = withProject { project, client ->
        val session = client.signIn(email, password)
        accept(project, session)
    }

    fun signUp(email: String, password: String) = withProject { project, client ->
        val flow = PendingAuth.create(FlowPurpose.SignUp, now())
        save(memory.copy(flow = flow))
        val session = client.signUp(email, password, flow)
        if (session == null) {
            mutableState.update { it.copy(page = AccountPage.Login) }
            message(str(R.string.andpop_account_msg_confirm_email))
        } else {
            accept(project, session)
        }
    }

    fun forgot(email: String) = withProject { _, client ->
        val flow = PendingAuth.create(FlowPurpose.Recovery, now())
        save(memory.copy(flow = flow))
        client.sendRecovery(email, flow)
        message(str(R.string.andpop_account_msg_reset_sent))
    }

    fun signInWithProvider(purpose: FlowPurpose) = withProject { _, client ->
        if (purpose != FlowPurpose.Google && purpose != FlowPurpose.Apple) return@withProject
        val flow = PendingAuth.create(purpose, now())
        save(memory.copy(flow = flow))
        browserChannel.send(client.providerUrl(flow))
    }

    fun browserUnavailable() = message(str(R.string.andpop_account_msg_no_browser))

    fun callback(uri: String) {
        viewModelScope.launch {
            operation?.join()
            val flow = memory.flow ?: return@launch
            val result = flow.readReturn(uri, now()) ?: return@launch
            val project = memory.project ?: return@launch
            perform {
                val client = SupabaseAccountClient(project)
                mutableState.update { it.copy(open = true) }
                if (result is AuthReturn.Rejected) {
                    save(memory.copy(flow = null))
                    message(str(R.string.andpop_account_msg_cancelled))
                    return@perform
                }
                val session = client.exchange((result as AuthReturn.Code).value, flow)
                if (flow.purpose == FlowPurpose.Recovery) {
                    save(memory.copy(flow = null, session = session, recovering = true))
                    publish()
                    mutableState.update { it.copy(page = AccountPage.Reset) }
                } else {
                    accept(project, session)
                }
            }
        }
    }

    fun resetPassword(value: String) = withProject { project, client ->
        val session = memory.session ?: return@withProject
        if (!memory.recovering) return@withProject
        if (session.expiresSoon(now())) {
            save(memory.copy(session = null, recovering = false))
            publish()
            mutableState.update { it.copy(page = AccountPage.Forgot) }
            message(str(R.string.andpop_account_msg_reset_expired))
            return@withProject
        }
        client.updatePassword(session, value)
        save(AccountMemory(project = project))
        useProfile(GUEST_PROFILE)
        publish()
        mutableState.update { it.copy(page = AccountPage.Login) }
        message(str(R.string.andpop_account_msg_pw_updated))
    }

    fun signOut() = perform {
        val project = memory.project
        val session = memory.session
        save(AccountMemory(project = project))
        useProfile(GUEST_PROFILE)
        publish()
        mutableState.update { it.copy(page = AccountPage.Profile) }
        if (session != null && project != null) {
            try {
                SupabaseAccountClient(project).signOut(session)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                message(str(R.string.andpop_account_msg_signout_offline))
            }
        }
    }

    fun deleteAccount() = withProject { project, client ->
        val existing = memory.session ?: return@withProject
        if (memory.recovering) return@withProject
        val session = if (existing.expiresSoon(now())) refresh(project, existing) else existing
        client.deleteAccount(session)
        useProfile(GUEST_PROFILE, removePrevious = true)
        save(AccountMemory(project = project))
        publish()
        mutableState.update { it.copy(page = AccountPage.Profile) }
        message(str(R.string.andpop_account_msg_deleted))
    }

    fun remember(device: RememberedAirPods?) = perform {
        withContext(Dispatchers.IO) { profiles.rememberDevice(device) }
        publish()
    }

    private fun perform(action: suspend () -> Unit) {
        if (operation?.isActive == true) return
        mutableState.update { it.copy(busy = true, message = null) }
        operation = viewModelScope.launch {
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: AuthFailure) {
                message(errorText(failure))
            } catch (_: IOException) {
                message(str(R.string.andpop_account_msg_io))
            } catch (_: Exception) {
                message(str(R.string.andpop_account_msg_generic))
            } finally {
                mutableState.update { it.copy(busy = false) }
            }
        }
    }

    private fun withProject(action: suspend (AuthProject, SupabaseAccountClient) -> Unit) {
        val project = memory.project
        if (project == null) {
            page(AccountPage.Project)
            return
        }
        perform { action(project, SupabaseAccountClient(project)) }
    }

    private suspend fun accept(project: AuthProject, session: AuthSession) {
        save(AccountMemory(project, session))
        useProfile(session.profile(project))
        finishEntering()
    }

    private suspend fun refresh(project: AuthProject, session: AuthSession): AuthSession {
        val result = SupabaseAccountClient(project).refresh(session.refreshToken)
        if (result.userId != session.userId) throw IOException()
        save(memory.copy(session = result))
        return result
    }

    private suspend fun save(value: AccountMemory) {
        withContext(Dispatchers.IO) { vault.write(value) }
        memory = value
    }

    private suspend fun useProfile(profile: String, removePrevious: Boolean = false) {
        withContext(Dispatchers.IO) { profiles.enterProfile(profile, removePrevious) }
    }

    private fun finishEntering() {
        publish()
        mutableState.update { it.copy(open = false, entered = true, page = AccountPage.Profile) }
    }

    private fun profileId() = if (memory.recovering) GUEST_PROFILE else {
        val project = memory.project
        if (project == null) GUEST_PROFILE else memory.session?.profile(project) ?: GUEST_PROFILE
    }

    private fun publish() {
        val session = memory.session?.takeUnless { memory.recovering }
        mutableState.update {
            it.copy(
                entered = it.entered || profiles.hasEntered,
                signedIn = session != null,
                email = session?.email.orEmpty(),
                configured = memory.project != null,
                showSocialSignIn = memory.project?.let { it.url != defaultAuthProject.url } ?: false,
                projectUrl = memory.project?.url.orEmpty(),
                remembered = profiles.rememberedDevice(),
            )
        }
    }

    private fun message(value: String) = mutableState.update { it.copy(message = value) }

    private fun now() = System.currentTimeMillis()

    private fun errorText(error: AuthFailure) = when {
        error.status == 429 -> str(R.string.andpop_account_err_rate)
        error.code == "email_not_confirmed" -> str(R.string.andpop_account_err_confirm)
        error.code == "weak_password" -> str(R.string.andpop_account_err_weak_pw)
        error.code == "invalid_credentials" -> str(R.string.andpop_account_err_creds)
        error.code == "user_already_exists" -> str(R.string.andpop_account_err_exists)
        error.code == "flow_state_not_found" || error.code == "flow_state_expired" ->
            str(R.string.andpop_account_err_link_expired)
        error.code == "PGRST202" || error.status == 404 && state.value.signedIn ->
            str(R.string.andpop_account_err_delete_unavail)
        error.status == 401 -> str(R.string.andpop_account_err_refused)
        else -> str(R.string.andpop_account_err_service)
    }
}
