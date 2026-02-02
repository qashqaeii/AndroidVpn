package com.vpn.client.config

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Decrypts AES-encrypted VLESS config from server.
 * Key must be shared with backend (e.g. build-time constant or secure storage).
 */
class ConfigDecryptor(private val secretKey: ByteArray) {

    companion object {
        private const val ALGORITHM = "AES/CBC/PKCS5Padding"
        private const val KEY_LENGTH = 32
        private const val IV_LENGTH = 16
    }

    /**
     * Decrypts base64-encoded ciphertext. Returns plain VLESS URL string.
     * Never log the result.
     */
    fun decrypt(encryptedBase64: String): String {
        val decoded = Base64.decode(encryptedBase64, Base64.NO_WRAP)
        val iv = decoded.copyOfRange(0, IV_LENGTH)
        val cipherBytes = decoded.copyOfRange(IV_LENGTH, decoded.size)
        val keySpec = SecretKeySpec(secretKey.copyOf(KEY_LENGTH), "AES")
        val ivSpec = IvParameterSpec(iv)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
        return String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
    }
}
