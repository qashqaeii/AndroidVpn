package com.vpn.client.security

import java.io.File

/**
 * Root detection. Used to warn or restrict functionality on rooted devices.
 */
object RootDetector {

    private val paths = listOf(
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su",
        "/su/bin/su"
    )

    fun isRooted(): Boolean {
        return paths.any { File(it).exists() } || checkSuBinary()
    }

    private fun checkSuBinary(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            process.inputStream.bufferedReader().readText().isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }
}
