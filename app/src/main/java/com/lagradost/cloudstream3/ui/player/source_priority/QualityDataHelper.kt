package com.lagradost.cloudstream3.ui.player.source_priority

import com.lagradost.cloudstream3.utils.ExtractorLink

object QualityDataHelper {
    enum class QualityProfileType {
        Download,
        Playback
    }

    data class Profile(
        val id: String = "default",
        val name: String = "Default",
        val types: Set<QualityProfileType> = setOf(QualityProfileType.Download, QualityProfileType.Playback)
    )

    fun getProfiles(): List<Profile> = listOf(Profile())

    fun getLinkPriority(profileId: String, link: ExtractorLink): Int {
        return link.quality
    }
}
