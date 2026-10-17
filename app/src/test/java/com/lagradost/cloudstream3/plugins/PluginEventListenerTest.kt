package com.lagradost.cloudstream3.plugins

import com.lagradost.cloudstream3.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginEventListenerTest {

    @Test
    fun `PluginEventListener default implementations do not throw with null or empty callbacks`() {
        val listener = object : PluginEventListener {}
        // Ensure none of the default implementations throw exceptions
        listener.onDownloadProgress("test_plugin", 0.5f)
        listener.onPluginLoaded(PluginData("url", "internal", true, "/path", 1))
        listener.onPluginLoadFailed("test_plugin", "error message", RuntimeException("test"))
        listener.onBatchOperationCompleted(5, 0)
        listener.onToastRequested("toast message", false)
        listener.onNotificationRequested("Title", "Content", listOf("plugin1", "plugin2"))
    }

    @Test
    fun `custom PluginEventListener captures all lifecycle events`() {
        var progressCalled = false
        var loadedCalled = false
        var failedCalled = false
        var batchCalled = false
        var toastCalled = false
        var notificationCalled = false

        val listener = object : PluginEventListener {
            override fun onDownloadProgress(pluginName: String, progress: Float) {
                progressCalled = true
                assertEquals("test_plugin", pluginName)
                assertEquals(0.75f, progress, 0.001f)
            }

            override fun onPluginLoaded(pluginData: PluginData) {
                loadedCalled = true
                assertEquals("plugin_id", pluginData.internalName)
            }

            override fun onPluginLoadFailed(pluginName: String, message: String, error: Throwable?) {
                failedCalled = true
                assertEquals("fail_plugin", pluginName)
            }

            override fun onBatchOperationCompleted(updatedCount: Int, failedCount: Int) {
                batchCalled = true
                assertEquals(3, updatedCount)
                assertEquals(1, failedCount)
            }

            override fun onToastRequested(message: String, isLong: Boolean) {
                toastCalled = true
                assertEquals("hello", message)
                assertTrue(isLong)
            }

            override fun onNotificationRequested(
                title: String,
                content: String,
                pluginNames: List<String>
            ) {
                notificationCalled = true
                assertEquals(2, pluginNames.size)
            }
        }

        listener.onDownloadProgress("test_plugin", 0.75f)
        listener.onPluginLoaded(PluginData(internalName = "plugin_id", url = "url", isOnline = true, filePath = "/path", version = 1))
        listener.onPluginLoadFailed("fail_plugin", "failed", null)
        listener.onBatchOperationCompleted(3, 1)
        listener.onToastRequested("hello", true)
        listener.onNotificationRequested("Title", "Content", listOf("p1", "p2"))

        assertTrue(progressCalled)
        assertTrue(loadedCalled)
        assertTrue(failedCalled)
        assertTrue(batchCalled)
        assertTrue(toastCalled)
        assertTrue(notificationCalled)
    }

    @Test
    fun `MainActivity afterPluginsLoadedEvent observer registers and receives invocations`() {
        var eventFired = false
        var eventValue: Boolean? = null

        val observer: (Boolean) -> Unit = { value ->
            eventFired = true
            eventValue = value
        }

        MainActivity.afterPluginsLoadedEvent += observer
        try {
            MainActivity.afterPluginsLoadedEvent.invoke(true)
            assertTrue(eventFired)
            assertEquals(true, eventValue)

            MainActivity.afterPluginsLoadedEvent.invoke(false)
            assertEquals(false, eventValue)
        } finally {
            MainActivity.afterPluginsLoadedEvent -= observer
        }
    }
}
