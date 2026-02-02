package com.vpn.client.data.repository

import com.vpn.client.config.ConfigDecryptor
import com.vpn.client.data.api.ServerDto
import com.vpn.client.data.api.ServersApi
import com.vpn.client.data.model.ServerItem
import com.vpn.client.data.model.ServerStatus
import com.vpn.client.data.ping.PingHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
/**
 * Fetches server list from API, maps to UI model, and provides ping via TCP.
 */
class ServerRepository(
    private val api: ServersApi,
    private val decryptor: ConfigDecryptor
) {

    /** Encrypted configs by server id; never exposed. */
    private val configCache = mutableMapOf<Int, String>()

    fun getServers(): Flow<List<ServerItem>> = flow {
        val dtos = api.getServers()
        configCache.clear()
        dtos.forEach { configCache[it.id] = it.config }
        val items = dtos.map { dto -> mapToItem(dto) }
        emit(items)
    }

    /** Returns encrypted config for selected server; used only to decrypt and pass to core. */
    fun getEncryptedConfig(serverId: Int): String? = configCache[serverId]

    suspend fun pingServer(encryptedConfig: String): Int {
        return PingHelper.pingEncryptedConfig(encryptedConfig, decryptor)
    }

    suspend fun getDecryptedV2RayJson(encryptedConfig: String): String {
        val vlessUrl = decryptor.decrypt(encryptedConfig)
        return com.vpn.client.config.VlessParser.parseToV2RayJson(vlessUrl)
    }

    private suspend fun mapToItem(dto: ServerDto): ServerItem {
        val pingMs = PingHelper.pingEncryptedConfig(dto.config, decryptor)
        val status = PingHelper.statusFromPingMs(pingMs)
        return ServerItem(
            id = dto.id,
            name = dto.name,
            country = dto.country,
            flag = dto.flag,
            pingMs = if (pingMs >= 0) pingMs else -1,
            status = status
        )
    }
}
