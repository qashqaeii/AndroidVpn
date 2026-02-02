package com.vpn.client.data.ping

import com.vpn.client.config.VlessParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.min

/**
 * TCP socket test to host:port parsed from config.
 * Returns latency in ms or -1 on failure.
 */
object PingHelper {

    private const val TIMEOUT_MS = 5000

    suspend fun pingHostPort(host: String, port: Int): Int = withContext(Dispatchers.IO) {
        try {
            val start = System.currentTimeMillis()
            Socket().use { socket ->
                socket.soTimeout = TIMEOUT_MS
                socket.connect(InetSocketAddress(host, port), TIMEOUT_MS)
            }
            (System.currentTimeMillis() - start).toInt().coerceAtLeast(0)
        } catch (_: Exception) {
            -1
        }
    }

    /**
     * Ping using encrypted config: decrypt, parse host:port, then TCP test.
     * Do not log decrypted content.
     */
    suspend fun pingEncryptedConfig(
        encryptedConfig: String,
        decryptor: com.vpn.client.config.ConfigDecryptor
    ): Int = withContext(Dispatchers.IO) {
        try {
            val vlessUrl = decryptor.decrypt(encryptedConfig)
            val (host, port) = VlessParser.extractHostPort(vlessUrl)
            pingHostPort(host, port)
        } catch (_: Exception) {
            -1
        }
    }

    fun statusFromPingMs(pingMs: Int): com.vpn.client.data.model.ServerStatus {
        return when {
            pingMs < 0 -> com.vpn.client.data.model.ServerStatus.OFFLINE
            pingMs <= 300 -> com.vpn.client.data.model.ServerStatus.ONLINE
            else -> com.vpn.client.data.model.ServerStatus.SLOW
        }
    }
}
