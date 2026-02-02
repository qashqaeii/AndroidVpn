package com.vpn.client.data.api

import retrofit2.http.GET

/**
 * Backend API for fetching server list.
 * GET https://example.com/api/servers
 */
interface ServersApi {

    @GET("api/servers")
    suspend fun getServers(): List<ServerDto>
}
