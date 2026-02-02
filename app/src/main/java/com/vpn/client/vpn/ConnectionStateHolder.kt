package com.vpn.client.vpn

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global connection state for UI observation. Updated by MyVpnService.
 */
object ConnectionStateHolder {

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    fun setState(newState: ConnectionState) {
        _state.value = newState
    }
}
