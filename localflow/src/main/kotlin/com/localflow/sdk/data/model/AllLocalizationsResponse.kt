package com.localflow.sdk.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AllLocalizationsResponse(
    val project: ProjectInfo,
    val version: Int,
    val publishedAt: String,
    val etag: String,
    val checksum: String,
    val availableLanguages: List<LanguageInfo>,
    val localizations: Map<String, Map<String, String>>
)
