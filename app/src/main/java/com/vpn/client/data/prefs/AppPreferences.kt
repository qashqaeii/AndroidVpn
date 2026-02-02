package com.vpn.client.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * ذخیره تنظیمات: Kill Switch و Auto Reconnect.
 */
class AppPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var killSwitchEnabled: Boolean
        get() = prefs.getBoolean(KEY_KILL_SWITCH, false)
        set(value) = prefs.edit { putBoolean(KEY_KILL_SWITCH, value) }

    var autoReconnectEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_RECONNECT, true)
        set(value) = prefs.edit { putBoolean(KEY_AUTO_RECONNECT, value) }

    companion object {
        private const val PREFS_NAME = "vpn_prefs"
        private const val KEY_KILL_SWITCH = "kill_switch"
        private const val KEY_AUTO_RECONNECT = "auto_reconnect"
    }
}
