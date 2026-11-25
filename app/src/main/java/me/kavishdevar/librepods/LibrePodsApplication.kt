package me.kavishdevar.librepods

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import me.kavishdevar.librepods.utils.XposedHooksBridge

class LibrePodsApplication : Application(), DefaultLifecycleObserver {

    override fun onCreate() {
        if (!BuildConfig.PLAY_BUILD) {
            XposedHooksBridge.register()
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        super<Application>.onCreate()
    }

    override fun onResume(owner: LifecycleOwner) {
        if (BuildConfig.PLAY_BUILD) return
        XposedHooksBridge.refreshState()
    }
}
