package com.localflow.sdk.data.model

import kotlinx.serialization.Serializable

@Serializable
data class BootstrapResponse(
    val project: ProjectInfo,
    val defaultLanguage: LanguageInfo? = null,
    val availableLanguages: List<LanguageInfo>,
    val latestVersion: Int,
    val publishedAt: String,
    val cacheTtlSeconds: Long = 300,
    val etag: String,
    val checksum: String,
    val fallbackStrategy: FallbackStrategy? = null,
    val sdkConfig: SdkConfig? = null
)

@Serializable
data class FallbackStrategy(
    val order: List<String> = listOf("requested-language", "default-language"),
    val defaultLanguage: String? = null
)

@Serializable
data class SdkConfig(
    val versionCheckPath: String,
    val localizationsPath: String,
    val languagePathTemplate: String
)
