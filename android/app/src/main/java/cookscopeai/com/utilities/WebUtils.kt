package cookscopeai.com.utilities

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Open a URL in the device's default browser. If no browser is available,
 * the call is silently ignored.
 */
fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        // No browser available; ignore.
    }
}
