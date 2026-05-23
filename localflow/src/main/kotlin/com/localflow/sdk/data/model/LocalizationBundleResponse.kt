package com.localflow.sdk.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LocalizationBundleResponse(
    val project: ProjectInfo,
    val language: LanguageInfo,
    val version: Int,
    val publishedAt: String,
    val etag: String,
    val checksum: String,
    val localizations: Map<String, String>
)
