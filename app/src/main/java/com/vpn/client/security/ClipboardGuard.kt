package com.vpn.client.security

import android.content.ClipboardManager
import android.content.Context

/**
 * No clipboard usage for config/sensitive data. This is a reminder guard.
 */
object ClipboardGuard {

    /** Do not copy config or credentials to clipboard. */
    fun clearIfSensitive(context: Context, text: String?) {
        if (text.isNullOrBlank()) return
        if (text.contains("vless://") || text.contains("vmess://")) {
            (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.setPrimaryClip(
                android.content.ClipData.newPlainText("", "")
            )
        }
    }
}
