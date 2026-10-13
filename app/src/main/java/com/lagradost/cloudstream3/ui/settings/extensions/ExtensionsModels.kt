package com.lagradost.cloudstream3.ui.settings.extensions

import android.os.Bundle
import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val REPOSITORIES_KEY = "repositories"

@Serializable
data class RepositoryData(
    @JsonProperty("name") @SerialName("name") val name: String,
    @JsonProperty("url") @SerialName("url") val url: String,
    @JsonProperty("iconUrl") @SerialName("iconUrl") val iconUrl: String? = null
)

object PluginsFragment {
    fun newInstance(repositoryData: RepositoryData): Bundle = Bundle().apply {
        putString("name", repositoryData.name)
        putString("url", repositoryData.url)
    }
}
