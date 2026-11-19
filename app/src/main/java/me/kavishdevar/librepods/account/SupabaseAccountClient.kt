// SPDX-License-Identifier: GPL-3.0-or-later
package me.kavishdevar.librepods.account

import android.net.Uri
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal class AuthFailure(val status: Int, val code: String) : IOException()

internal class SupabaseAccountClient(private val project: AuthProject) {
    suspend fun signIn(email: String, password: String) = session(
        request("POST", "/auth/v1/token?grant_type=password", credentials(email, password))
    )

    suspend fun signUp(email: String, password: String, flow: PendingAuth): AuthSession? {
        val response = request(
            "POST", "/auth/v1/signup?redirect_to=${Uri.encode(flow.redirect)}",
            credentials(email, password).withChallenge(flow),
        )
        return if (!response.isNull("access_token")) session(response) else null
    }

    suspend fun sendRecovery(email: String, flow: PendingAuth) {
        request(
            "POST", "/auth/v1/recover?redirect_to=${Uri.encode(flow.redirect)}",
            JSONObject().put("email", email).withChallenge(flow),
        )
    }

    fun providerUrl(flow: PendingAuth): Uri =
        Uri.parse("${project.url}/auth/v1/authorize").buildUpon()
            .appendQueryParameter("provider", if (flow.purpose == FlowPurpose.Google) "google" else "apple")
            .appendQueryParameter("redirect_to", flow.redirect)
            .appendQueryParameter("code_challenge", flow.challenge)
            .appendQueryParameter("code_challenge_method", "s256")
            .build()

    suspend fun exchange(code: String, flow: PendingAuth) = session(
        request(
            "POST", "/auth/v1/token?grant_type=pkce",
            JSONObject().put("auth_code", code).put("code_verifier", flow.verifier),
        )
    )

    suspend fun refresh(refreshToken: String) = session(
        request("POST", "/auth/v1/token?grant_type=refresh_token", JSONObject().put("refresh_token", refreshToken))
    )

    suspend fun updatePassword(session: AuthSession, password: String) {
        request("PUT", "/auth/v1/user", JSONObject().put("password", password), session.accessToken)
    }

    suspend fun signOut(session: AuthSession) {
        request("POST", "/auth/v1/logout?scope=local", token = session.accessToken)
    }

    suspend fun deleteAccount(session: AuthSession) {
        request("POST", "/rest/v1/rpc/delete_my_andpop_account", JSONObject(), session.accessToken)
    }

    private fun credentials(email: String, password: String) =
        JSONObject().put("email", email).put("password", password)

    private fun JSONObject.withChallenge(flow: PendingAuth) =
        put("code_challenge", flow.challenge).put("code_challenge_method", "s256")

    private fun session(json: JSONObject): AuthSession {
        val user = json.getJSONObject("user")
        val id = user.getString("id")
        val access = json.getString("access_token")
        val refresh = json.getString("refresh_token")
        val expiresIn = json.getLong("expires_in")
        if (!id.matches(Regex("[0-9a-fA-F-]{36}")) || access.isBlank() || refresh.isBlank() ||
            expiresIn !in 1..604_800
        ) throw IOException()
        return AuthSession(id, user.optString("email", ""), access, refresh, System.currentTimeMillis() + expiresIn * 1000)
    }

    private suspend fun request(
        method: String,
        path: String,
        body: JSONObject? = null,
        token: String? = null,
    ): JSONObject = withContext(Dispatchers.IO) {
        val connection = URL(project.url + path).openConnection() as HttpsURLConnection
        try {
            connection.requestMethod = method
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 12_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("apikey", project.key)
            connection.setRequestProperty("Accept", "application/json")
            if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                val bytes = body.toString().toByteArray(Charsets.UTF_8)
                connection.setFixedLengthStreamingMode(bytes.size)
                connection.outputStream.use { it.write(bytes) }
            }
            val status = connection.responseCode
            val success = status in 200..299
            val stream = if (success) connection.inputStream else connection.errorStream
            val content = stream?.use { it.readNBytes(131_073) } ?: byteArrayOf()
            if (content.size > 131_072) throw IOException()
            val response = runCatching { JSONObject(String(content, Charsets.UTF_8)) }.getOrNull()
            if (!success) {
                val code = response?.optString("error_code")?.ifBlank { response.optString("code") }.orEmpty()
                throw AuthFailure(status, code)
            }
            response ?: if (content.isEmpty() || String(content, Charsets.UTF_8) == "null") JSONObject() else throw IOException()
        } finally {
            connection.disconnect()
        }
    }
}
