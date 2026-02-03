package com.vpn.client.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.vpn.client.R
import com.vpn.client.ui.MainActivity

/**
 * Establishes TUN interface, routes traffic to V2Ray core, handles lifecycle, foreground notification.
 */
class MyVpnService : VpnService() {

    private var tunFd: ParcelFileDescriptor? = null
    private var v2RayCore: V2RayCoreManager? = null
    private val tun2socksRunner: Tun2SocksRunner = Tun2SocksStub()

    override fun onCreate() {
        super.onCreate()
        v2RayCore = V2RayCoreManagerImpl(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val serverName = intent.getStringExtra(EXTRA_SERVER_NAME) ?: return START_NOT_STICKY
                val configJson = intent.getStringExtra(EXTRA_CONFIG_JSON) ?: return START_NOT_STICKY
                val killSwitch = intent.getBooleanExtra(EXTRA_KILL_SWITCH, false)
                startForeground(NOTIFICATION_ID, createNotification(serverName))
                establishVpn(serverName, configJson, killSwitch)
            }
            ACTION_DISCONNECT -> {
                teardown()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                ConnectionStateHolder.setState(ConnectionState.Disconnected)
            }
        }
        return START_NOT_STICKY
    }

    private fun establishVpn(serverName: String, configJson: String, killSwitch: Boolean) {
        ConnectionStateHolder.setState(ConnectionState.Connecting)
        try {
            tunFd = Builder()
                .setSession("SecureConnect")
                .addAddress("10.0.0.2", 32)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("8.8.8.8")
                .setMtu(1500)
                .establish()
            val coreStarted = v2RayCore?.start(configJson) == true
            if (coreStarted) {
                tunFd?.fd?.let { fd -> tun2socksRunner.start(fd, SOCKS_PROXY_ADDRESS) }
                ConnectionStateHolder.setState(ConnectionState.Connected(
                    com.vpn.client.data.model.ServerItem(
                        id = 0, name = serverName, country = "", flag = "",
                        pingMs = -1, status = com.vpn.client.data.model.ServerStatus.ONLINE
                    )
                ))
            } else {
                teardown()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                val detail = LibV2RayBridge.lastError
                val msg = if (detail.isNullOrBlank()) getString(R.string.vpn_core_not_integrated)
                else getString(R.string.vpn_core_not_integrated) + " [" + detail + "]"
                ConnectionStateHolder.setState(ConnectionState.Error(msg))
            }
        } catch (e: Exception) {
            ConnectionStateHolder.setState(ConnectionState.Error(e.message ?: "VPN failed"))
            if (killSwitch) { }
            teardown()
            stopSelf()
        }
    }

    private fun teardown() {
        tun2socksRunner.stop()
        try {
            tunFd?.close()
        } catch (_: Exception) { }
        tunFd = null
        v2RayCore?.stop()
    }

    private fun createNotification(serverName: String): Notification {
        createChannel()
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_channel_name))
            .setContentText(getString(R.string.notification_connected, serverName))
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(open)
            .setOngoing(true)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        teardown()
        super.onDestroy()
    }

    companion object {
        const val ACTION_CONNECT = "com.vpn.client.CONNECT"
        const val ACTION_DISCONNECT = "com.vpn.client.DISCONNECT"
        const val EXTRA_SERVER_NAME = "server_name"
        const val EXTRA_CONFIG_JSON = "config_json"
        const val EXTRA_KILL_SWITCH = "kill_switch"
        private const val CHANNEL_ID = "vpn_channel"
        private const val NOTIFICATION_ID = 1
        /** با پورت inbound در VlessParser.parseToV2RayJson هماهنگ است */
        private const val SOCKS_PROXY_ADDRESS = "127.0.0.1:10808"
    }
}
