package com.lagradost.cloudstream3.ui.player

import android.net.Uri
import android.os.Bundle
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.InternalAPI
import com.lagradost.cloudstream3.ui.result.ResultEpisode
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.SubtitleHelper
import com.lagradost.cloudstream3.utils.serializers.UriSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class SubtitleOrigin {
    URL,
    DOWNLOADED_FILE,
    EMBEDDED_IN_CONTAINER,
    OPEN_SUBTITLES
}

@Serializable
data class SubtitleData(
    @JsonProperty("name") @SerialName("name") val name: String = "",
    @JsonProperty("originalName") @SerialName("originalName") val originalName: String = name,
    @JsonProperty("nameSuffix") @SerialName("nameSuffix") val nameSuffix: String? = null,
    @JsonProperty("url") @SerialName("url") val url: String = "",
    @JsonProperty("origin") @SerialName("origin") val origin: SubtitleOrigin = SubtitleOrigin.URL,
    @JsonProperty("mimeType") @SerialName("mimeType") val mimeType: String = "text/vtt",
    @JsonProperty("headers") @SerialName("headers") val headers: Map<String, String> = emptyMap(),
    @JsonProperty("id") @SerialName("id") val id: String? = null,
    @JsonProperty("languageCode") @SerialName("languageCode") val languageCode: String? = null
) {
    fun getIETF_tag(): String? {
        if (!languageCode.isNullOrBlank()) {
            val codeTag = SubtitleHelper.fromCodeToLangTagIETF(languageCode)
                ?: SubtitleHelper.fromLanguageToTagIETF(languageCode)
            if (codeTag != null) return codeTag
        }
        val cleanName = originalName.replace(Regex("\\[.*?\\]"), "").trim()
        val nameTag = SubtitleHelper.fromLanguageToTagIETF(cleanName, halfMatch = false)
            ?: SubtitleHelper.fromLanguageToTagIETF(originalName, halfMatch = true)
        if (nameTag != null) return nameTag
        if (!languageCode.isNullOrBlank() && SubtitleHelper.isWellFormedTagIETF(languageCode)) {
            return languageCode
        }
        return null
    }
}

@OptIn(InternalAPI::class)
@Serializable
data class ExtractorUri(
    @Serializable(with = UriSerializer::class)
    @JsonProperty("uri") @SerialName("uri") val uri: Uri? = null,
    @JsonProperty("name") @SerialName("name") val name: String? = null,
    @JsonProperty("id") @SerialName("id") val id: Int = 0,
    @JsonProperty("parentId") @SerialName("parentId") val parentId: Int = 0,
    @JsonProperty("episode") @SerialName("episode") val episode: Int? = null,
    @JsonProperty("season") @SerialName("season") val season: Int? = null
)

val LOADTYPE_INAPP_DOWNLOAD: Set<ExtractorLinkType> = ExtractorLinkType.entries.toSet()
val LOADTYPE_INAPP: Set<ExtractorLinkType> = ExtractorLinkType.entries.toSet()

const val NEXT_WATCH_EPISODE_PERCENTAGE = 90L

abstract class VideoGenerator<T>(val items: List<T>) {
    abstract val hasCache: Boolean
    abstract val canSkipLoading: Boolean
    abstract fun getId(index: Int): Int
    abstract suspend fun generateLinks(
        clearCache: Boolean,
        sourceTypes: Set<ExtractorLinkType>,
        callback: (Pair<ExtractorLink?, ExtractorUri?>) -> Unit,
        subtitleCallback: (SubtitleData) -> Unit,
        offset: Int,
        isCasting: Boolean
    ): Boolean
}

object GeneratorPlayer {
    fun newInstance(
        generator: Any,
        index: Int,
        syncData: Any? = null
    ): Bundle = Bundle().apply {
        putInt("index", index)
    }
}

class RepoLinkGenerator(val episodes: List<ResultEpisode>) {
    suspend fun generateLinks(
        offset: Int = 0,
        isCasting: Boolean = false,
        clearCache: Boolean = false,
        sourceTypes: Set<ExtractorLinkType> = LOADTYPE_INAPP_DOWNLOAD,
        callback: (Pair<ExtractorLink?, ExtractorUri?>) -> Unit,
        subtitleCallback: (SubtitleData) -> Unit
    ) {
    }
}
