package com.localflow.sdk.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ProjectInfo(
    val id: String? = null, // Optional in some responses
    val name: String,
    val slug: String,
    val description: String? = null
)
