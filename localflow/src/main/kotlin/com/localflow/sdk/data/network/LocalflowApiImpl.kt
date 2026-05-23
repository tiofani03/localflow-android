package com.localflow.sdk.data.network

import android.util.Log
import com.localflow.sdk.LocalflowConfig
import com.localflow.sdk.data.model.AllLocalizationsResponse
import com.localflow.sdk.data.model.BootstrapResponse
import com.localflow.sdk.data.model.ErrorResponse
import com.localflow.sdk.data.model.LatestVersionResponse
import com.localflow.sdk.data.model.LocalizationBundleResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

internal class LocalflowApiImpl(
    private val config: LocalflowConfig
) : LocalflowApi {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(config.connectTimeoutMs, TimeUnit.MILLISECONDS)
        .readTimeout(config.readTimeoutMs, TimeUnit.MILLISECONDS)
        .build()

    private val baseProjectUrl = "${config.baseUrl}/api/mobile/v1/projects/${config.apiKey}"

    private fun log(level: LocalflowConfig.LogLevel, msg: String, err: Throwable? = null) {
        if (config.logLevel >= level) {
            if (err != null) {
                Log.e("LocalflowApi", msg, err)
            } else {
                Log.d("LocalflowApi", msg)
            }
        }
    }

    private operator fun LocalflowConfig.LogLevel.compareTo(other: LocalflowConfig.LogLevel): Int {
        return this.ordinal.compareTo(other.ordinal)
    }

    private suspend fun <T> executeRequest(
        url: String,
        etag: String? = null,
        parseBlock: (String) -> T
    ): ApiResult<T> = withContext(Dispatchers.IO) {
        val requestBuilder = Request.Builder()
            .url(url)
            .get()

        if (etag != null) {
            requestBuilder.header("If-None-Match", etag)
            log(LocalflowConfig.LogLevel.VERBOSE, "Sending request to $url with ETag: $etag")
        } else {
            log(LocalflowConfig.LogLevel.VERBOSE, "Sending request to $url")
        }

        val request = requestBuilder.build()

        try {
            client.newCall(request).execute().use { response ->
                handleResponse(response, parseBlock)
            }
        } catch (e: IOException) {
            log(LocalflowConfig.LogLevel.BASIC, "Network error during request to $url", e)
            ApiResult.NetworkFailure(e)
        } catch (e: Exception) {
            log(LocalflowConfig.LogLevel.BASIC, "Unexpected error during request to $url", e)
            ApiResult.NetworkFailure(e)
        }
    }

    private fun <T> handleResponse(
        response: Response,
        parseBlock: (String) -> T
    ): ApiResult<T> {
        val code = response.code
        val responseBody = response.body?.string() ?: ""

        log(LocalflowConfig.LogLevel.VERBOSE, "HTTP Response Code: $code")

        when (code) {
            200 -> {
                return try {
                    val parsedData = parseBlock(responseBody)
                    val responseEtag = response.header("ETag")
                    val responseVersion = response.header("X-Localization-Version")?.toIntOrNull()
                    
                    log(LocalflowConfig.LogLevel.VERBOSE, "Request succeeded. ETag: $responseEtag, Version: $responseVersion")
                    ApiResult.Success(parsedData, responseEtag, responseVersion)
                } catch (e: Exception) {
                    log(LocalflowConfig.LogLevel.BASIC, "Error parsing successful response", e)
                    ApiResult.Error("PARSE_ERROR", "Failed to parse API response: ${e.localizedMessage}", 200)
                }
            }
            304 -> {
                log(LocalflowConfig.LogLevel.VERBOSE, "Response 304 Not Modified")
                return ApiResult.NotModified
            }
            401, 404, 500 -> {
                return try {
                    val errRes = json.decodeFromString<ErrorResponse>(responseBody)
                    log(LocalflowConfig.LogLevel.BASIC, "Server returned error: ${errRes.error.code} - ${errRes.error.message}")
                    ApiResult.Error(errRes.error.code, errRes.error.message, code)
                } catch (e: Exception) {
                    log(LocalflowConfig.LogLevel.BASIC, "Failed to parse server error response (HTTP $code)", e)
                    ApiResult.Error("SERVER_ERROR", "Server returned HTTP $code", code)
                }
            }
            else -> {
                log(LocalflowConfig.LogLevel.BASIC, "Unexpected HTTP status: $code")
                return ApiResult.Error("HTTP_$code", "Unexpected HTTP status $code", code)
            }
        }
    }

    override suspend fun bootstrap(): ApiResult<BootstrapResponse> {
        val url = "$baseProjectUrl/bootstrap"
        return executeRequest(url) { body ->
            json.decodeFromString<BootstrapResponse>(body)
        }
    }

    override suspend fun getLatestVersion(currentVersion: Int?): ApiResult<LatestVersionResponse> {
        val builder = "$baseProjectUrl/versions/latest".toHttpUrlOrNull()?.newBuilder()
            ?: return ApiResult.NetworkFailure(IOException("Invalid URL configuration"))

        if (currentVersion != null) {
            builder.addQueryParameter("currentVersion", currentVersion.toString())
        }

        val url = builder.build().toString()
        return executeRequest(url) { body ->
            json.decodeFromString<LatestVersionResponse>(body)
        }
    }

    override suspend fun getLocalizations(etag: String?): ApiResult<AllLocalizationsResponse> {
        val url = "$baseProjectUrl/localizations"
        return executeRequest(url, etag) { body ->
            json.decodeFromString<AllLocalizationsResponse>(body)
        }
    }

    override suspend fun getLanguageBundle(
        languageCode: String,
        etag: String?
    ): ApiResult<LocalizationBundleResponse> {
        val url = "$baseProjectUrl/localizations/$languageCode"
        return executeRequest(url, etag) { body ->
            json.decodeFromString<LocalizationBundleResponse>(body)
        }
    }
}
