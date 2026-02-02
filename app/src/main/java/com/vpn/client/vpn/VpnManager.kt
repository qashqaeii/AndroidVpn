package com.vpn.client.vpn

import android.content.Context
import android.content.Intent
import com.vpn.client.data.model.ServerItem
import com.vpn.client.vpn.MyVpnService.Companion.EXTRA_CONFIG_JSON
import com.vpn.client.vpn.MyVpnService.Companion.EXTRA_SERVER_NAME

/**
 * Starts/stops MyVpnService and coordinates with V2RayCoreManager.
 */
class VpnManager(
    private val context: Context,
    private val v2RayCore: V2RayCoreManager
) {

    fun connect(server: ServerItem, configJson: String, killSwitch: Boolean = false) {
        val intent = Intent(context, MyVpnService::class.java).apply {
            action = MyVpnService.ACTION_CONNECT
            putExtra(EXTRA_SERVER_NAME, server.name)
            putExtra(EXTRA_CONFIG_JSON, configJson)
            putExtra(MyVpnService.EXTRA_KILL_SWITCH, killSwitch)
        }
        context.startForegroundService(intent)
    }

    fun disconnect() {
        val intent = Intent(context, MyVpnService::class.java).apply {
            action = MyVpnService.ACTION_DISCONNECT
        }
        context.startService(intent)
    }

    fun getV2RayCore(): V2RayCoreManager = v2RayCore
}
