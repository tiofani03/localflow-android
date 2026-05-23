package com.localflow.sdk.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LatestVersionResponse(
    val projectSlug: String,
    val version: Int,
    val publishedAt: String,
    val etag: String,
    val checksum: String,
    val requestedVersion: Int? = null,
    val hasUpdate: Boolean
)
