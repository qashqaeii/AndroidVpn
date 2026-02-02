package com.vpn.client.data.model

/**
 * UI model for a server. Ping and status are computed locally.
 */
data class ServerItem(
    val id: Int,
    val name: String,
    val country: String,
    val flag: String,
    val pingMs: Int = -1,
    val status: ServerStatus
)

enum class ServerStatus {
    ONLINE,
    SLOW,
    OFFLINE
}
