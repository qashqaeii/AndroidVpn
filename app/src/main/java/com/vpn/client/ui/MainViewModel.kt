package com.vpn.client.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vpn.client.VpnApplication
import com.vpn.client.data.model.ServerItem
import com.vpn.client.data.repository.ServerRepository
import com.vpn.client.vpn.ConnectionState
import com.vpn.client.vpn.ConnectionStateHolder
import com.vpn.client.vpn.VpnManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MVVM ViewModel: server list, selected server, connection state, connect/disconnect,
 * Kill Switch, Auto Reconnect.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ServerRepository
    private val vpnManager: VpnManager
    private val preferences: com.vpn.client.data.prefs.AppPreferences

    init {
        val app = application as? VpnApplication
            ?: throw IllegalStateException("Application must be VpnApplication")
        repository = app.container.serverRepository
        vpnManager = app.container.vpnManager
        preferences = app.container.preferences
    }

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var autoReconnectJob: Job? = null

    init {
        loadServers()
        ConnectionStateHolder.state
            .onEach { state ->
                _uiState.update { it.copy(connectionState = state) }
                if (state is ConnectionState.Error && preferences.autoReconnectEnabled) {
                    tryAutoReconnect()
                } else {
                    autoReconnectJob?.cancel()
                }
            }
            .launchIn(viewModelScope)
    }

    private fun tryAutoReconnect() {
        autoReconnectJob?.cancel()
        val server = _uiState.value.selectedServer ?: return
        autoReconnectJob = viewModelScope.launch {
            delay(AUTO_RECONNECT_DELAY_MS)
            connect()
        }
    }

    fun loadServers() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            repository.getServers()
                .catch { e -> _uiState.update { it.copy(loading = false, refreshing = false, error = e.message) } }
                .collect { list ->
                    _uiState.update {
                        it.copy(servers = list, loading = false, refreshing = false, error = null)
                    }
                }
        }
    }

    fun selectServer(server: ServerItem?) {
        _uiState.update { it.copy(selectedServer = server) }
    }

    fun connect() {
        val server = _uiState.value.selectedServer ?: return
        val encrypted = repository.getEncryptedConfig(server.id) ?: return
        autoReconnectJob?.cancel()
        viewModelScope.launch {
            try {
                val configJson = repository.getDecryptedV2RayJson(encrypted)
                val killSwitch = preferences.killSwitchEnabled
                vpnManager.connect(server, configJson, killSwitch)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(connectionState = ConnectionState.Error(e.message ?: "Error"))
                }
            }
        }
    }

    fun disconnect() {
        autoReconnectJob?.cancel()
        vpnManager.disconnect()
    }

    fun setRefreshing(refreshing: Boolean) {
        _uiState.update { it.copy(refreshing = refreshing) }
    }

    companion object {
        private const val AUTO_RECONNECT_DELAY_MS = 3000L
    }
}

data class MainUiState(
    val servers: List<ServerItem> = emptyList(),
    val selectedServer: ServerItem? = null,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val error: String? = null
)
