package com.vpn.client.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.vpn.client.R
import com.vpn.client.data.model.ServerItem
import com.vpn.client.databinding.ActivityMainBinding
import com.vpn.client.vpn.ConnectionState
import com.vpn.client.vpn.MyVpnService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * صفحه اصلی: کارت وضعیت، لیست سرور با Pull-to-refresh، دکمه اتصال/قطع، تنظیمات.
 */
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private lateinit var serverAdapter: ServerAdapter

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) viewModel.connect()
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        blockScreenshots()
        requestNotificationPermissionIfNeeded()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        serverAdapter = ServerAdapter(
            viewModel.uiState.value.selectedServer?.id,
            { viewModel.selectServer(it) }
        )
        binding.serverList.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = serverAdapter
        }

        binding.swipeRefresh.setColorSchemeResources(R.color.primary)
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.setRefreshing(true)
            viewModel.loadServers()
        }

        binding.btnRetry.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            viewModel.setRefreshing(true)
            viewModel.loadServers()
        }

        binding.serverList.itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator().apply {
            addDuration = 150
            removeDuration = 150
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    serverAdapter.submitList(state.servers)
                    serverAdapter.setSelectedId(viewModel.uiState.value.selectedServer?.id)
                    binding.loading.isVisible = state.loading && state.servers.isEmpty()
                    binding.swipeRefresh.isRefreshing = state.refreshing || (state.loading && state.servers.isNotEmpty())
                    binding.emptyState.isVisible = !state.loading && state.servers.isEmpty()
                    state.error?.let { if (state.servers.isEmpty()) binding.statusText.text = it }
                    updateConnectionUi(state.connectionState)
                    state.selectedServer?.let { updatePingDisplay(it) }
                }
            }
        }

        binding.connectButton.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            when (viewModel.uiState.value.connectionState) {
                is ConnectionState.Connected -> viewModel.disconnect()
                else -> {
                    if (viewModel.uiState.value.selectedServer != null) {
                        checkVpnPermissionAndConnect()
                    } else {
                        Snackbar.make(binding.root, R.string.select_server, Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun blockScreenshots() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(false)
            setTurnScreenOn(false)
        }
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }

    private fun checkVpnPermissionAndConnect() {
        val prepared = android.net.VpnService.prepare(this)
        if (prepared != null) {
            vpnPermissionLauncher.launch(prepared)
        } else {
            viewModel.connect()
        }
    }

    private fun updateConnectionUi(state: ConnectionState) {
        when (state) {
            ConnectionState.Disconnected -> {
                setConnectionIndicatorColor(R.color.offline)
                binding.statusProgress.isVisible = false
                binding.statusText.text = getString(R.string.disconnected)
                binding.connectButton.text = getString(R.string.connect)
                binding.connectButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.primary))
                binding.connectButton.iconTint = ContextCompat.getColorStateList(this, android.R.color.white)
                binding.pingText.isVisible = false
            }
            ConnectionState.Connecting -> {
                setConnectionIndicatorColor(R.color.primary)
                binding.statusProgress.isVisible = true
                binding.statusText.text = getString(R.string.connecting)
                binding.connectButton.isEnabled = false
                binding.pingText.isVisible = false
            }
            is ConnectionState.Connected -> {
                setConnectionIndicatorColor(R.color.connected)
                binding.statusProgress.isVisible = false
                binding.statusText.text = getString(R.string.connected_to, state.server.name)
                binding.connectButton.text = getString(R.string.disconnect)
                binding.connectButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.error))
                binding.connectButton.iconTint = ContextCompat.getColorStateList(this, android.R.color.white)
                binding.connectButton.isEnabled = true
                binding.pingText.isVisible = state.server.pingMs >= 0
                binding.pingText.text = getString(R.string.ping_ms, state.server.pingMs)
            }
            is ConnectionState.Error -> {
                setConnectionIndicatorColor(R.color.error)
                binding.statusProgress.isVisible = false
                binding.statusText.text = getString(R.string.error_connection) + ": " + state.message
                binding.connectButton.text = getString(R.string.connect)
                binding.connectButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.primary))
                binding.connectButton.isEnabled = true
                binding.pingText.isVisible = false
            }
        }
    }

    private fun setConnectionIndicatorColor(colorResId: Int) {
        ViewCompat.setBackgroundTintList(
            binding.connectionIndicator,
            ContextCompat.getColorStateList(this, colorResId)
        )
    }

    private fun updatePingDisplay(server: ServerItem) {
        if (viewModel.uiState.value.connectionState is ConnectionState.Connected &&
            (viewModel.uiState.value.connectionState as ConnectionState.Connected).server.id == server.id
        ) {
            binding.pingText.isVisible = server.pingMs >= 0
            binding.pingText.text = getString(R.string.ping_ms, server.pingMs)
        }
    }
}
