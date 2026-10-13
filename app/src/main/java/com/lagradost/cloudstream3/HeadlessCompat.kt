package com.lagradost.cloudstream3

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import java.io.File

object HeadlessCompat {
    @Volatile
    var lastError: Throwable? = null
    var activityResultLauncher: ActivityResultLauncher<Intent>? = null
    const val API_NAME_EXTRA_KEY = "API_NAME_EXTRA_KEY"

    fun deleteFileOnExit(file: File) {
        try { file.deleteOnExit() } catch (_: Throwable) {}
    }

    fun bookmarksUpdatedEvent(force: Boolean = true) {}
    fun reloadLibraryEvent(force: Boolean = true) {}
    fun reloadHomeEvent(force: Boolean = true) {}
    fun afterPluginsLoadedEvent() {}
    fun afterRepositoryLoadedEvent(force: Boolean = true) {}

    fun setLastError(context: Context?) { lastError = null }
}
