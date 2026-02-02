package com.vpn.client.config

import org.json.JSONObject
import java.net.URLDecoder

/**
 * Parses vless://UUID@host:port?type=tcp&security=none&...
 * and builds V2Ray outbound JSON. Configs must never be visible in UI or logs.
 */
object VlessParser {

    private const val SCHEME = "vless://"

    /**
     * Parses VLESS URL and returns V2Ray config JSON string for core.
     */
    fun parseToV2RayJson(vlessUrl: String): String {
        if (!vlessUrl.startsWith(SCHEME)) throw IllegalArgumentException("Invalid VLESS URL")
        val withoutScheme = vlessUrl.removePrefix(SCHEME)
        val atIndex = withoutScheme.indexOf('@')
        require(atIndex > 0) { "Invalid VLESS: no @" }
        val uuid = withoutScheme.substring(0, atIndex).trim()
        val rest = withoutScheme.substring(atIndex + 1)
        val queryStart = rest.indexOf('?')
        val hostPort = if (queryStart < 0) rest else rest.substring(0, queryStart)
        val host: String
        val port: Int
        val colon = hostPort.lastIndexOf(':')
        require(colon > 0) { "Invalid host:port" }
        host = hostPort.substring(0, colon).trim()
        port = hostPort.substring(colon + 1).trim().toIntOrNull()
            ?: throw IllegalArgumentException("Invalid port")

        val query = if (queryStart >= 0 && queryStart < rest.length - 1) {
            rest.substring(queryStart + 1)
        } else ""
        val params = parseQuery(query)
        val type = params["type"] ?: "tcp"
        val security = params["security"] ?: "none"

        val outbound = JSONObject().apply {
            put("protocol", "vless")
            put("settings", JSONObject().apply {
                put("vnext", org.json.JSONArray().put(JSONObject().apply {
                    put("address", host)
                    put("port", port)
                    put("users", org.json.JSONArray().put(JSONObject().apply {
                        put("id", uuid)
                        put("encryption", "none")
                    }))
                }))
            })
            put("streamSettings", JSONObject().apply {
                put("network", type)
                put("security", security)
                put("tcpSettings", type.takeIf { it == "tcp" }?.let { JSONObject() })
            })
            put("tag", "proxy")
        }

        val config = JSONObject().apply {
            put("outbounds", org.json.JSONArray().put(outbound))
        }
        return config.toString()
    }

    private fun parseQuery(query: String): Map<String, String> {
        return query.split('&').mapNotNull { param ->
            val eq = param.indexOf('=')
            if (eq < 0) return@mapNotNull null
            val key = URLDecoder.decode(param.substring(0, eq), "UTF-8")
            val value = URLDecoder.decode(param.substring(eq + 1), "UTF-8")
            key to value
        }.toMap()
    }

    /**
     * Extracts host and port from VLESS URL for ping (TCP socket test).
     */
    fun extractHostPort(vlessUrl: String): Pair<String, Int> {
        if (!vlessUrl.startsWith(SCHEME)) throw IllegalArgumentException("Invalid VLESS URL")
        val withoutScheme = vlessUrl.removePrefix(SCHEME)
        val atIndex = withoutScheme.indexOf('@')
        require(atIndex > 0) { "Invalid VLESS" }
        val rest = withoutScheme.substring(atIndex + 1)
        val queryStart = rest.indexOf('?')
        val hostPort = if (queryStart < 0) rest else rest.substring(0, queryStart)
        val colon = hostPort.lastIndexOf(':')
        require(colon > 0) { "Invalid host:port" }
        val host = hostPort.substring(0, colon).trim()
        val port = hostPort.substring(colon + 1).trim().toIntOrNull()
            ?: throw IllegalArgumentException("Invalid port")
        return host to port
    }
}
