// SPDX-License-Identifier: GPL-3.0-or-later

package me.kavishdevar.librepods.utils

object XposedHooksBridge {
    fun register() {
        XposedState.isAvailable = false
        XposedState.bluetoothScopeEnabled = false
    }

    fun refreshState() {
        XposedState.isAvailable = false
        XposedState.bluetoothScopeEnabled = false
    }
}
