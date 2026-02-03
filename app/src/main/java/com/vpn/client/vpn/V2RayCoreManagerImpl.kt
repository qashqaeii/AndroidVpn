package com.vpn.client.vpn

import android.content.Context

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
        val started = LibV2RayBridge.start(configJson)
        running = started
        return started
    }

    override fun stop() {
        LibV2RayBridge.stop()
        running = false
    }

    override fun isRunning(): Boolean = running
}
