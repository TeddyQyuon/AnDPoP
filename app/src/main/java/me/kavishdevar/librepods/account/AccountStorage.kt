// SPDX-License-Identifier: GPL-3.0-or-later
package me.kavishdevar.librepods.account

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.io.IOException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject

internal data class AccountMemory(
    val project: AuthProject? = null,
    val session: AuthSession? = null,
    val flow: PendingAuth? = null,
    val recovering: Boolean = false,
)

internal class AccountVault(context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "andpop-account-v1"))
    private val alias = "andpop-account-v1"

    fun read(): AccountMemory {
        val bytes = try {
            file.readFully()
        } catch (_: java.io.FileNotFoundException) {
            return AccountMemory()
        }
        require(bytes.size in 29..32_768)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        val root = JSONObject(String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8))
        val project = root.optJSONObject("project")?.let {
            AuthProject.parse(it.getString("url"), it.getString("key"))
        } ?: return AccountMemory()
        val session = root.optJSONObject("session")?.let {
            AuthSession(it.getString("id"), it.getString("email"), it.getString("access"), it.getString("refresh"), it.getLong("expires"))
        }
        val flow = root.optJSONObject("flow")?.let {
            PendingAuth(FlowPurpose.valueOf(it.getString("purpose")), it.getString("nonce"), it.getString("verifier"), it.getLong("expires"))
        }
        return AccountMemory(project, session, flow, root.optBoolean("recovering"))
    }

    fun write(memory: AccountMemory) {
        val root = JSONObject().put("recovering", memory.recovering)
        memory.project?.let { root.put("project", JSONObject().put("url", it.url).put("key", it.key)) }
        memory.session?.let {
            root.put("session", JSONObject().put("id", it.userId).put("email", it.email)
                .put("access", it.accessToken).put("refresh", it.refreshToken).put("expires", it.expiresAt))
        }
        memory.flow?.let {
            root.put("flow", JSONObject().put("purpose", it.purpose.name).put("nonce", it.nonce)
                .put("verifier", it.verifier).put("expires", it.expiresAt))
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        check(cipher.iv.size == 12)
        val bytes = cipher.iv + cipher.doFinal(root.toString().toByteArray(Charsets.UTF_8))
        val output = file.startWrite()
        try {
            output.write(bytes)
            file.finishWrite(output)
        } catch (failure: Exception) {
            file.failWrite(output)
            throw failure
        }
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build())
            generateKey()
        }
    }
}

internal const val ACTIVE_PROFILE_KEY = "andpop_active_profile"
internal data class RememberedAirPods(val name: String, val model: String)

internal class ProfilePreferences(private val prefs: SharedPreferences) {
    // App preferences only. Pairing addresses, hardware keys, serial numbers, and live controls
    // are owned by the device/service and are never copied into a profile.
    private val defaults = mapOf(
        "andpop_appearance" to "Dark",
        "andpop_palette" to "Glacier",
        "show_phone_battery_in_widget" to true,
        "conversation_awareness_pause_music" to false,
        "relative_conversation_awareness_volume" to true,
        "conversation_awareness_volume" to 43,
        "disconnect_when_not_wearing" to false,
        "takeover_when_disconnected" to false,
        "takeover_when_idle" to false,
        "takeover_when_music" to false,
        "takeover_when_call" to false,
        "takeover_when_ringing_call" to false,
        "takeover_when_media_start" to false,
        "use_alternate_head_tracking_packets" to true,
        "custom_camera_package" to "",
        "show_bottom_sheet_popup" to true,
        "show_island_popup" to true,
        "m3e_enabled" to true,
    )

    // Keys renamed in 2.0. A profile saved by an older build is read through its old key.
    private val legacy = mapOf("andpop_palette" to "andpop_dark_tint")

    val active get() = prefs.getString(ACTIVE_PROFILE_KEY, GUEST_PROFILE) ?: GUEST_PROFILE
    val hasEntered get() = prefs.getBoolean("andpop_account_entered", false)

    fun enterProfile(target: String, removePrevious: Boolean = false) {
        val current = active
        val edit = prefs.edit().putBoolean("andpop_account_entered", true)
        if (current != target) {
            if (removePrevious) {
                edit.remove(settingsKey(current)).remove(deviceKey(current))
            } else {
                val live = prefs.all
                val snapshot = JSONObject()
                for ((key, default) in defaults) {
                    snapshot.put(key, live[key] ?: legacy[key]?.let { live[it] } ?: default)
                }
                edit.putString(settingsKey(current), snapshot.toString())
            }
            val next = read(settingsKey(target))
            for ((key, default) in defaults) {
                when (default) {
                    is Boolean -> edit.putBoolean(key, next.optBoolean(key, default))
                    is Int -> edit.putInt(key, next.optInt(key, default))
                    is String -> {
                        val value = next.optString(key, legacy[key]?.let { next.optString(it, default) } ?: default)
                        if (key == "custom_camera_package" && value.isBlank()) edit.remove(key)
                        else edit.putString(key, value)
                    }
                }
            }
            edit.putString(ACTIVE_PROFILE_KEY, target)
        }
        if (!edit.commit()) throw IOException()
    }

    fun rememberDevice(device: RememberedAirPods?) {
        val edit = prefs.edit()
        if (device == null) edit.remove(deviceKey(active)) else {
            edit.putString(deviceKey(active), JSONObject().put("name", device.name.take(120)).put("model", device.model.take(120)).toString())
        }
        if (!edit.commit()) throw IOException()
    }

    fun rememberedDevice(): RememberedAirPods? {
        val value = read(deviceKey(active))
        return value.optString("name").takeIf(String::isNotBlank)?.let {
            RememberedAirPods(it, value.optString("model"))
        }
    }

    private fun read(key: String) = runCatching { JSONObject(prefs.getString(key, "{}").orEmpty()) }
        .getOrDefault(JSONObject())
    private fun settingsKey(profile: String) = "andpop_profile_settings_$profile"
    private fun deviceKey(profile: String) = "andpop_profile_device_$profile"
}
