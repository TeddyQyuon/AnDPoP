// SPDX-License-Identifier: GPL-3.0-or-later

package me.kavishdevar.librepods.utils

import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

object XposedHooksBridge : XposedServiceHelper.OnServiceListener {
    fun register() {
        XposedServiceHelper.registerListener(this)
    }

    fun refreshState() {
        val service = XposedServiceHolder.service
        XposedState.isAvailable = service != null
        val scope = service?.scope.orEmpty()
        XposedState.bluetoothScopeEnabled =
            "com.google.android.bluetooth" in scope || "com.android.bluetooth" in scope
    }

    override fun onServiceBind(service: XposedService) {
        XposedServiceHolder.service = service
        refreshState()
    }

    override fun onServiceDied(service: XposedService) {
        XposedServiceHolder.service = null
        XposedState.isAvailable = false
        XposedState.bluetoothScopeEnabled = false
    }
}
