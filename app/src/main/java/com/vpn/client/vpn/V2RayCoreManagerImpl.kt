package com.vpn.client.vpn

/**
 * Placeholder implementation until libv2ray.aar is integrated.
 * Replace with actual LibV2ray JNI calls.
 */
class V2RayCoreManagerImpl : V2RayCoreManager {

    private var running = false

    override fun start(configJson: String): Boolean {
        // TODO: LibV2ray.init(context); LibV2ray.startV2ray(configJson)
        running = true
        return true
    }

    override fun stop() {
        // TODO: LibV2ray.stopV2ray()
        running = false
    }

    override fun isRunning(): Boolean = running
}
