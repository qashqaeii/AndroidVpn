package com.vpn.client.vpn

import android.content.Context
import android.net.VpnService
import android.os.Build

/**
 * پیاده‌سازی مدیر هستهٔ V2Ray.
 * اگر libv2ray.aar در app/libs باشد از LibV2RayBridge استفاده می‌کند؛ وگرنه به‌صورت stub برمی‌گرداند false.
 */
class V2RayCoreManagerImpl(private val context: Context) : V2RayCoreManager {

    private var running = false

    override fun start(configJson: String): Boolean {
        if (!LibV2RayBridge.isAvailable) {
            running = false
            return false
        }
        val envPath = context.filesDir?.absolutePath ?: ""
        LibV2RayBridge.initCoreEnv(envPath, "")
        val protectCallback: (Int) -> Boolean = { fd ->
            (context as? VpnService)?.let { vpn ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vpn.protect(fd)
                } else {
                    try {
                        VpnService::class.java.getMethod("protect", Int::class.javaPrimitiveType).invoke(vpn, fd) == true
                    } catch (_: Throwable) { true }
                }
            } ?: false
        }
        val started = LibV2RayBridge.start(configJson, protectCallback)
        running = started
        return started
    }

    override fun stop() {
        LibV2RayBridge.stop()
        running = false
    }

    override fun isRunning(): Boolean = running
}
