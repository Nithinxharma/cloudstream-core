package com.lagradost.cloudstream3.ui.result

import android.os.Bundle
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.IDownloadableMinimum
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.Score
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.ui.player.SubtitleData
import com.lagradost.cloudstream3.utils.ExtractorLink
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ResultEpisode(
    @JsonProperty("name") @SerialName("name") val name: String = "",
    @JsonProperty("poster") @SerialName("poster") val poster: String? = null,
    @JsonProperty("episode") @SerialName("episode") val episode: Int = 0,
    @JsonProperty("season") @SerialName("season") val season: Int? = null,
    @JsonProperty("data") @SerialName("data") val data: String = "",
    @JsonProperty("id") @SerialName("id") val id: Int = 0,
    @JsonProperty("parentId") @SerialName("parentId") val parentId: Int = 0,
    @JsonProperty("rating") @SerialName("rating") val rating: Int? = null,
    @JsonProperty("score") @SerialName("score") val score: Score? = null,
    @JsonProperty("description") @SerialName("description") val description: String? = null,
    @JsonProperty("totalEpisodeIndex") @SerialName("totalEpisodeIndex") val totalEpisodeIndex: Int? = null,
    @JsonProperty("headerName") @SerialName("headerName") val headerName: String? = null,
    @JsonProperty("videoWatchState") @SerialName("videoWatchState") val videoWatchState: VideoWatchState = VideoWatchState.None,
    @JsonProperty("tvType") @SerialName("tvType") val tvType: com.lagradost.cloudstream3.TvType = com.lagradost.cloudstream3.TvType.TvSeries
)

fun getId(episode: ResultEpisode): Int = episode.id
fun LoadResponse.getId(): Int = (this.url + this.apiName).hashCode()

enum class EpisodeSortType {
    NUMBER_ASC,
    NUMBER_DESC,
    ALPHABETICAL_ASC,
    ALPHABETICAL_DESC
}

enum class VideoWatchState {
    None,
    Watched
}

@Serializable
data class ExtractorSubtitleLink(
    @JsonProperty("name") @SerialName("name") val name: String,
    @JsonProperty("url") @SerialName("url") override val url: String,
    @JsonProperty("origin") @SerialName("origin") val origin: String = "",
    @JsonProperty("headers") @SerialName("headers") override val headers: Map<String, String> = emptyMap(),
    @JsonProperty("referer") @SerialName("referer") override val referer: String = ""
) : IDownloadableMinimum

data class LinkLoadingResult(
    val links: List<ExtractorLink> = emptyList(),
    val subs: List<SubtitleData> = emptyList(),
    val syncData: Map<String, String>? = null
)

object ResultFragment {
    fun updateUI() {}

    fun newInstance(
        url: String,
        apiName: String,
        name: String,
        startAction: Int = 0,
        startValue: Int = 0
    ): Bundle = Bundle().apply {
        putString("url", url)
        putString("apiName", apiName)
        putString("name", name)
        putInt("startAction", startAction)
        putInt("startValue", startValue)
    }

    fun newInstance(
        card: SearchResponse,
        startAction: Int = 0,
        startValue: Int? = null
    ): Bundle = Bundle().apply {
        putString("url", card.url)
        putString("apiName", card.apiName)
        putString("name", card.name)
        putInt("startAction", startAction)
        if (startValue != null) putInt("startValue", startValue)
    }
}
