package com.lagradost.cloudstream3.plugins

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.lagradost.cloudstream3.CommonActivity

/**
 * Host callback bridge allowing independent Android hosts (services, workers,
 * custom headless hosts) to observe and react to plugin lifecycle events without
 * coupling the core engine to Activities or Android UI components.
 */
interface PluginEventListener {
    /** Called periodically during extension download. */
    fun onDownloadProgress(pluginName: String, progress: Float) {}

    /** Called when an extension has been successfully loaded into the ClassLoader and registered. */
    fun onPluginLoaded(pluginData: PluginData) {}

    /** Called when an extension fails to download, verify, or load. */
    fun onPluginLoadFailed(pluginName: String, message: String, error: Throwable?) {}

    /** Called when a batch operation (such as update or initial download) completes. */
    fun onBatchOperationCompleted(updatedCount: Int, failedCount: Int) {}

    /** Dispatches a user-facing toast or alert (delegated to host). */
    fun onToastRequested(message: String, isLong: Boolean) {}

    /** Requests a system notification (delegated to host). */
    fun onNotificationRequested(
        title: String,
        content: String,
        pluginNames: List<String>
    ) {}
}

/**
 * Default listener implementation providing backwards-compatible behavior
 * using standard Android Context, toasts, and system notifications.
 */
open class DefaultPluginEventListener(
    protected val context: Context
) : PluginEventListener {
    override fun onToastRequested(message: String, isLong: Boolean) {
        val duration = if (isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
        CommonActivity.showToast(message, duration)
    }

    override fun onPluginLoadFailed(pluginName: String, message: String, error: Throwable?) {
        Log.e("PluginManager", "Failed to load plugin $pluginName: $message", error)
    }
}
