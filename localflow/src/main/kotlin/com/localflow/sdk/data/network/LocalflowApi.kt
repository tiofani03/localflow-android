package com.localflow.sdk.data.network

import com.localflow.sdk.data.model.AllLocalizationsResponse
import com.localflow.sdk.data.model.BootstrapResponse
import com.localflow.sdk.data.model.LatestVersionResponse
import com.localflow.sdk.data.model.LocalizationBundleResponse

internal interface LocalflowApi {
    suspend fun bootstrap(): ApiResult<BootstrapResponse>
    
    suspend fun getLatestVersion(currentVersion: Int?): ApiResult<LatestVersionResponse>
    
    suspend fun getLocalizations(etag: String?): ApiResult<AllLocalizationsResponse>
    
    suspend fun getLanguageBundle(languageCode: String, etag: String?): ApiResult<LocalizationBundleResponse>
}
