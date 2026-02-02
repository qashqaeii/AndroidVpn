package com.vpn.client.di

import okhttp3.OkHttpClient

/**
 * Security configuration: certificate pinning, debug logging.
 */
interface SecurityConfig {
    fun applyCertificatePinning(builder: OkHttpClient.Builder)
    val isDebugLogging: Boolean
}
