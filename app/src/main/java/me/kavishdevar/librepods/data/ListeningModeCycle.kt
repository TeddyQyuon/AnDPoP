// SPDX-License-Identifier: GPL-3.0-or-later

package me.kavishdevar.librepods.data

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import me.kavishdevar.librepods.R

/**
 * Listening-mode values as the AirPods report them (LISTENING_MODE control command):
 * 1 Off, 2 Noise cancellation, 3 Transparency, 4 Adaptive. Shared by the notification,
 * the Quick Settings tile and the widget so they all cycle and label modes the same way.
 */
internal object ListeningModeCycle {
    const val OFF = 1
    const val NOISE_CANCELLATION = 2
    const val TRANSPARENCY = 3
    const val ADAPTIVE = 4

    /** Modes in the order the dashboard shows them. */
    fun available(offAllowed: Boolean, adaptiveSupported: Boolean): List<Int> = buildList {
        if (offAllowed) add(OFF)
        add(TRANSPARENCY)
        if (adaptiveSupported) add(ADAPTIVE)
        add(NOISE_CANCELLATION)
    }

    fun next(current: Int, offAllowed: Boolean, adaptiveSupported: Boolean): Int {
        val modes = available(offAllowed, adaptiveSupported)
        val index = modes.indexOf(current)
        return modes[(index + 1).mod(modes.size)]
    }

    @StringRes
    fun label(mode: Int): Int = when (mode) {
        OFF -> R.string.off
        TRANSPARENCY -> R.string.transparency
        ADAPTIVE -> R.string.adaptive
        NOISE_CANCELLATION -> R.string.noise_cancellation
        else -> R.string.andpop_listening_mode
    }

    @DrawableRes
    fun icon(mode: Int): Int = when (mode) {
        OFF -> R.drawable.ic_qs_off
        TRANSPARENCY -> R.drawable.ic_qs_transparency
        ADAPTIVE -> R.drawable.ic_qs_adaptive
        NOISE_CANCELLATION -> R.drawable.ic_qs_anc
        else -> R.drawable.ic_stat_andpop
    }
}
