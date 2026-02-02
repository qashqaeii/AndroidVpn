package com.vpn.client.di

import okhttp3.OkHttpClient

/**
 * Default implementation. In release, add real certificate pins for your API host.
 */
class DefaultSecurityConfig : SecurityConfig {
    override fun applyCertificatePinning(builder: OkHttpClient.Builder) {
        // Add CertificatePinner when you have real pins for example.com
        // builder.certificatePinner(CertificatePinner.Builder().add("example.com", "sha256/...").build())
    }

    override val isDebugLogging: Boolean = false
}
