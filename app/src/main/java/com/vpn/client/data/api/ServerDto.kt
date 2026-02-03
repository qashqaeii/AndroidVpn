package com.vpn.client.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Server item from backend API.
 * Config is encrypted; never exposed in UI or logs.
 */
@JsonClass(generateAdapter = false)
data class ServerDto(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "country") val country: String,
    @Json(name = "flag") val flag: String,
    @Json(name = "config") val config: String,
    @Json(name = "status") val status: String
)
