package com.vpn.client.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.vpn.client.VpnApplication
import com.vpn.client.data.prefs.AppPreferences
import com.vpn.client.databinding.ActivitySettingsBinding

/**
 * صفحه تنظیمات: Kill Switch، Auto Reconnect، About.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = (application as VpnApplication).container.preferences

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.switchKillSwitch.isChecked = prefs.killSwitchEnabled
        binding.switchAutoReconnect.isChecked = prefs.autoReconnectEnabled

        binding.switchKillSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.killSwitchEnabled = checked
        }
        binding.switchAutoReconnect.setOnCheckedChangeListener { _, checked ->
            prefs.autoReconnectEnabled = checked
        }
    }
}
