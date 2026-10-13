package com.lagradost.cloudstream3

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.lagradost.cloudstream3.utils.Event
import java.io.File

/**
 * Headless compatibility shim for MainActivity.
 * Background services or providers referencing MainActivity companion methods
 * delegate to HeadlessCompat without inflating any UI.
 */
class MainActivity {
    companion object {
        const val API_NAME_EXTRA_KEY = HeadlessCompat.API_NAME_EXTRA_KEY

        var activityResultLauncher: ActivityResultLauncher<Intent>? = null

        var lastError: String?
            get() = HeadlessCompat.lastError
            set(value) {
                HeadlessCompat.lastError = value
            }

        fun setLastError(context: Context?) = HeadlessCompat.setLastError(context)

        val afterPluginsLoadedEvent get() = HeadlessCompat.afterPluginsLoadedEvent
        val afterRepositoryLoadedEvent get() = HeadlessCompat.afterRepositoryLoadedEvent
        val bookmarksUpdatedEvent get() = HeadlessCompat.bookmarksUpdatedEvent
        val reloadLibraryEvent get() = HeadlessCompat.reloadLibraryEvent
        val reloadHomeEvent get() = HeadlessCompat.reloadHomeEvent

        fun reloadLibraryEvent(force: Boolean) = HeadlessCompat.reloadLibraryEvent(force)
        fun reloadHomeEvent(force: Boolean) = HeadlessCompat.reloadHomeEvent(force)
        fun deleteFileOnExit(file: File) = HeadlessCompat.deleteFileOnExit(file)
    }
}
