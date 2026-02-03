package com.vpn.client.di

import android.content.Context
import com.vpn.client.config.ConfigDecryptor
import com.vpn.client.data.api.ApiModule
import com.vpn.client.data.api.ServersApi
import com.vpn.client.data.prefs.AppPreferences
import com.vpn.client.data.repository.ServerRepository
import com.vpn.client.vpn.V2RayCoreManagerImpl
import com.vpn.client.vpn.VpnManager

/**
 * Simple container for app dependencies. No Dagger.
 */
class AppContainer(private val context: Context) {

    private val securityConfig: SecurityConfig = DefaultSecurityConfig()
    private val okHttp = ApiModule.createOkHttpClient(securityConfig)
    val api: ServersApi = ApiModule.createServersApi(okHttp)

    /** 32-byte AES key; must match backend CONFIG_ENCRYPTION_KEY (same 32 chars). */
    private val decryptKey = "vpnclient-aes256-key-32bytes!!!!!!".toByteArray(Charsets.UTF_8).copyOf(32)
    val decryptor: ConfigDecryptor = ConfigDecryptor(decryptKey)

    val serverRepository: ServerRepository = ServerRepository(api, decryptor)
    val preferences: AppPreferences = AppPreferences(context)

    private val v2RayCore = V2RayCoreManagerImpl()
    val vpnManager: VpnManager = VpnManager(context, v2RayCore)
}
