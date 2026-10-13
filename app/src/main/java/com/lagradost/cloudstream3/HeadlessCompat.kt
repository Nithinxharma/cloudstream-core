package com.lagradost.cloudstream3

import android.content.Context
import com.lagradost.cloudstream3.utils.Event
import java.io.File

object HeadlessCompat {
    const val API_NAME_EXTRA_KEY = "api_name"

    var lastError: String? = null

    fun setLastError(context: Context?) {
        try {
            val file = context?.filesDir?.resolve("last_error")
            if (file?.exists() == true) {
                lastError = file.readText()
            }
        } catch (_: Exception) {}
    }

    val afterPluginsLoadedEvent = Event<Boolean>()
    val afterRepositoryLoadedEvent = Event<Boolean>()
    val bookmarksUpdatedEvent = Event<Boolean>()
    val reloadLibraryEvent = Event<Boolean>()
    val reloadHomeEvent = Event<Boolean>()

    fun reloadLibraryEvent(force: Boolean) {
        reloadLibraryEvent.invoke(force)
    }

    fun reloadHomeEvent(force: Boolean) {
        reloadHomeEvent.invoke(force)
    }

    fun deleteFileOnExit(file: File) {
        try {
            file.deleteOnExit()
        } catch (_: Exception) {}
    }
}
