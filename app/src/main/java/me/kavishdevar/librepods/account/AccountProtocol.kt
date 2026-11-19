// SPDX-License-Identifier: GPL-3.0-or-later
package me.kavishdevar.librepods.account

import java.net.URI
import java.net.URLDecoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import org.json.JSONObject

internal const val CALLBACK = "app.andpop.android://auth/callback"
internal const val GUEST_PROFILE = "guest"

internal data class AuthProject(val url: String, val key: String) {
    companion object {
        fun parse(url: String, key: String): AuthProject? {
            val clean = url.trim().trimEnd('/')
            val host = runCatching { URI(clean).host }.getOrNull() ?: return null
            if (!host.matches(Regex("[a-z0-9-]+[.]supabase[.]co")) || clean != "https://$host") return null
            val publicKey = key.trim()
            val publishable = publicKey.matches(Regex("sb_publishable_[A-Za-z0-9_-]{12,160}"))
            val legacyAnon = runCatching {
                if (!publicKey.matches(Regex("[A-Za-z0-9_-]+[.][A-Za-z0-9_-]+[.][A-Za-z0-9_-]+")) ||
                    publicKey.length > 1500
                ) return@runCatching false
                val payload = Base64.getUrlDecoder().decode(publicKey.split('.')[1])
                JSONObject(String(payload, Charsets.UTF_8)).optString("role") == "anon"
            }.getOrDefault(false)
            return if (publishable || legacyAnon) AuthProject(clean, publicKey) else null
        }
    }
}

internal enum class FlowPurpose { SignUp, Google, Apple, Recovery }

internal data class PendingAuth(
    val purpose: FlowPurpose,
    val nonce: String,
    val verifier: String,
    val expiresAt: Long,
) {
    val redirect get() = "$CALLBACK/$nonce"
    val challenge get() = sha256Url(verifier)

    fun readReturn(value: String, now: Long): AuthReturn? {
        if (now >= expiresAt || value.length > 4096) return null
        val uri = runCatching { URI(value) }.getOrNull() ?: return null
        if (uri.fragment != null || uri.rawAuthority != "auth" || uri.scheme != "app.andpop.android" ||
            uri.rawPath != "/callback/$nonce"
        ) return null
        val pairs = runCatching {
            uri.rawQuery.orEmpty().split('&').filter { '=' in it }.map {
                val (key, value) = it.split('=', limit = 2)
                URLDecoder.decode(key, Charsets.UTF_8) to URLDecoder.decode(value, Charsets.UTF_8)
            }
        }.getOrNull() ?: return null
        val codes = pairs.filter { it.first == "code" }
        if (pairs.any { it.first == "error" || it.first == "error_code" }) {
            return if (codes.isEmpty()) AuthReturn.Rejected else null
        }
        val code = codes.singleOrNull()?.second ?: return null
        if (!code.matches(Regex("[A-Za-z0-9_-]{16,256}"))) return null
        return AuthReturn.Code(code)
    }

    companion object {
        fun create(purpose: FlowPurpose, now: Long): PendingAuth {
            val duration = if (purpose == FlowPurpose.SignUp || purpose == FlowPurpose.Recovery) {
                24 * 60 * 60 * 1000L
            } else {
                10 * 60 * 1000L
            }
            return PendingAuth(purpose, randomUrl(24), randomUrl(32), now + duration)
        }
    }
}

internal sealed interface AuthReturn {
    data class Code(val value: String) : AuthReturn
    data object Rejected : AuthReturn
}

private val encoder = Base64.getUrlEncoder().withoutPadding()
private val random = SecureRandom()

private fun randomUrl(size: Int) = encoder.encodeToString(ByteArray(size).also(random::nextBytes))

internal fun sha256Url(value: String) = encoder.encodeToString(
    MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
)

internal fun validNewPassword(value: String) = value.length >= 8 &&
    value.toByteArray(Charsets.UTF_8).size <= 72

internal data class AuthSession(
    val userId: String,
    val email: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long,
) {
    fun profile(project: AuthProject) = sha256Url("${project.url}/$userId")
    fun expiresSoon(now: Long) = now >= expiresAt - 60_000
}
