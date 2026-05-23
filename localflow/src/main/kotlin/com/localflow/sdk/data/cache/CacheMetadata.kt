package com.localflow.sdk.data.cache

import com.localflow.sdk.data.model.LanguageInfo
import kotlinx.serialization.Serializable

@Serializable
internal data class CacheMetadata(
    val version: Int,
    val etag: String,
    val checksum: String,
    val publishedAt: String,
    val defaultLanguage: String,
    val availableLanguages: List<LanguageInfo>,
    val lastSyncTimestamp: Long
)
