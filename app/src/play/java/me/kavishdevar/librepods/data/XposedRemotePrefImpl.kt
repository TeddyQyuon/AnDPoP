// SPDX-License-Identifier: GPL-3.0-or-later

package me.kavishdevar.librepods.data

class XposedRemotePrefImpl : XposedRemotePref {
    override fun isAvailable(): Boolean = false

    override fun getBoolean(key: String, def: Boolean): Boolean = def

    override fun putBoolean(key: String, value: Boolean) {}
}
