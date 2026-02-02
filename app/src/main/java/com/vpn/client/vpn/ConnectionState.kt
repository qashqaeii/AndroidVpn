package com.vpn.client.vpn

import com.vpn.client.data.model.ServerItem

/**
 * Observable connection states for UI.
 */
sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Connecting : ConnectionState()
    data class Connected(val server: ServerItem) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
