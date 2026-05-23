package com.localflow.sdk.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LanguageInfo(
    val code: String,
    val name: String,
    val isDefault: Boolean = false
)
