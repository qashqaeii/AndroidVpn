package com.vpn.client.data.api

import retrofit2.http.GET

/**
 * Backend API for fetching server list.
 * GET {BASE_URL}api/servers/
 */
interface ServersApi {

    @GET("api/servers/")
    suspend fun getServers(): List<ServerDto>
}
