package com.lagradost.cloudstream3

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import java.io.File

class MainActivity {
    companion object {
        @Volatile
        var lastError: Throwable? = HeadlessCompat.lastError
        fun setLastError(context: Context?) = HeadlessCompat.setLastError(context)
        fun deleteFileOnExit(file: File) = HeadlessCompat.deleteFileOnExit(file)
        var activityResultLauncher: ActivityResultLauncher<Intent>? = HeadlessCompat.activityResultLauncher
        const val API_NAME_EXTRA_KEY = HeadlessCompat.API_NAME_EXTRA_KEY
        fun bookmarksUpdatedEvent(force: Boolean = true) = HeadlessCompat.bookmarksUpdatedEvent(force)
        fun reloadLibraryEvent(force: Boolean = true) = HeadlessCompat.reloadLibraryEvent(force)
        fun reloadHomeEvent(force: Boolean = true) = HeadlessCompat.reloadHomeEvent(force)
        fun afterPluginsLoadedEvent() = HeadlessCompat.afterPluginsLoadedEvent()
        fun afterRepositoryLoadedEvent(force: Boolean = true) = HeadlessCompat.afterRepositoryLoadedEvent(force)
    }
}
