package com.vpn.client.vpn

/**
 * Bridge to libv2ray: load core, start/stop with config JSON, capture logs in debug only.
 * In a full implementation you would:
 * - Add libv2ray.aar and JNI bindings
 * - Call LibV2ray.init() and LibV2ray.startV2ray(configJson)
 * - Call LibV2ray.stopV2ray() on disconnect
 * Config JSON must never be logged.
 */
interface V2RayCoreManager {

    fun start(configJson: String): Boolean
    fun stop()
    fun isRunning(): Boolean
}
