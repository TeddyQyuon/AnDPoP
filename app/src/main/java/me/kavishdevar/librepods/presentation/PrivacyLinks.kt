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
 * Hosted via GitHub Pages from docs/ on main (TeddyQyuon/AnDPoP repository).
 */
internal object PrivacyLinks {
    const val PRIVACY_POLICY_URL: String = "https://teddyqyuon.github.io/AnDPoP/privacy.html"
    const val ACCOUNT_DELETION_URL: String = "https://teddyqyuon.github.io/AnDPoP/delete-account.html"

    fun openPrivacyPolicy(context: Context) {
        openExternalUrl(context, PRIVACY_POLICY_URL)
    }

    fun openAccountDeletion(context: Context) {
        openExternalUrl(context, ACCOUNT_DELETION_URL)
    }

    private fun openExternalUrl(context: Context, url: String) {
        if (url.isBlank()) {
            Toast.makeText(context, context.getString(R.string.andpop_privacy_policy_pending), Toast.LENGTH_SHORT).show()
            return
        }
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, context.getString(R.string.andpop_account_msg_no_browser), Toast.LENGTH_SHORT).show()
        }
    }
}
