package com.localflow.sdk.data.network

internal sealed class ApiResult<out T> {
    data class Success<out T>(
        val data: T,
        val etag: String?,
        val version: Int?
    ) : ApiResult<T>()

    data object NotModified : ApiResult<Nothing>()

    data class Error(
        val code: String,
        val message: String,
        val httpStatus: Int
    ) : ApiResult<Nothing>()

    data class NetworkFailure(
        val cause: Throwable
    ) : ApiResult<Nothing>()
}
