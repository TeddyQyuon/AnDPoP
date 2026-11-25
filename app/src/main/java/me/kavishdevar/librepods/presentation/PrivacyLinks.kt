// SPDX-License-Identifier: GPL-3.0-or-later

package me.kavishdevar.librepods.presentation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri
import me.kavishdevar.librepods.R

/**
 * External policy links required for Google Play release.
 * TODO: Replace placeholder URLs with the final hosted HTTPS URLs before publishing on Play Console.
 */
internal object PrivacyLinks {
    const val PRIVACY_POLICY_URL: String = ""
    const val ACCOUNT_DELETION_URL: String = ""

    fun openPrivacyPolicy(context: Context) {
        if (PRIVACY_POLICY_URL.isBlank()) {
            Toast.makeText(context, context.getString(R.string.andpop_privacy_policy_pending), Toast.LENGTH_SHORT).show()
            return
        }
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, PRIVACY_POLICY_URL.toUri()))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, context.getString(R.string.andpop_account_msg_no_browser), Toast.LENGTH_SHORT).show()
        }
    }
}
