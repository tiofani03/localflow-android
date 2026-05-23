package com.localflow.sdk.domain

import android.content.Context
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.localflow.sdk.LocalflowConfig
import com.localflow.sdk.data.cache.DiskCache
import com.localflow.sdk.data.cache.MemoryCache
import com.localflow.sdk.data.model.BootstrapResponse
import com.localflow.sdk.data.model.LanguageInfo
import com.localflow.sdk.data.model.LocalizationBundleResponse
import com.localflow.sdk.data.model.ProjectInfo
import com.localflow.sdk.data.network.ApiResult
import com.localflow.sdk.data.network.LocalflowApi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SyncManagerTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mockContext: Context
    private lateinit var mockApi: LocalflowApi
    private lateinit var memoryCache: MemoryCache
    private lateinit var mockDiskCache: DiskCache
    private lateinit var languageResolver: LanguageResolver
    private lateinit var config: LocalflowConfig
    private lateinit var mockLifecycleOwner: LifecycleOwner
    private lateinit var syncManager: SyncManager

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0

        mockLifecycleOwner = mockk(relaxed = true)
        val mockLifecycle = mockk<Lifecycle>(relaxed = true)
        every { mockLifecycleOwner.lifecycle } returns mockLifecycle

        mockContext = mockk()
        mockApi = mockk()
        memoryCache = MemoryCache()
        mockDiskCache = mockk(relaxed = true)
        languageResolver = LanguageResolver("en")

        config = LocalflowConfig.Builder(
            apiKey = "loc_live_test123",
            baseUrl = "https://api.localflow.com"
        )
            .defaultLanguage("en")
            .enableAutoSync(false)
            .build()

        coEvery { mockDiskCache.loadTranslations() } returns null
        coEvery { mockDiskCache.loadMetadata() } returns null
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun testFirstSyncCallsBootstrapAndDownloadsAllLocalizations() = kotlinx.coroutines.runBlocking {
        // First sync - cache is empty
        val bootstrapData = BootstrapResponse(
            project = ProjectInfo(name = "Test Project", slug = "test-project", description = "Test"),
            defaultLanguage = LanguageInfo(code = "en", name = "English", isDefault = true),
            availableLanguages = listOf(LanguageInfo(code = "en", name = "English", isDefault = true)),
            latestVersion = 1,
            publishedAt = "2026-05-22T13:00:00Z",
            cacheTtlSeconds = 300,
            etag = "\"boot-etag\"",
            checksum = "boot-checksum",
            fallbackStrategy = null,
            sdkConfig = null
        )

        coEvery { mockApi.bootstrap() } returns ApiResult.Success(bootstrapData, "\"boot-etag\"", 1)

        val allLocalizationsData = com.localflow.sdk.data.model.AllLocalizationsResponse(
            project = ProjectInfo(name = "Test Project", slug = "test-project", description = "Test"),
            version = 1,
            publishedAt = "2026-05-22T13:00:00Z",
            etag = "\"bundle-etag\"",
            checksum = "bundle-checksum",
            availableLanguages = listOf(LanguageInfo(code = "en", name = "English", isDefault = true)),
            localizations = mapOf("en" to mapOf("home.title" to "Welcome"))
        )
        coEvery { mockApi.getLocalizations(any()) } returns ApiResult.Success(allLocalizationsData, "\"bundle-etag\"", 1)

        coEvery { mockDiskCache.saveCache(any(), any()) } returns true

        syncManager = SyncManager(mockContext, config, mockApi, memoryCache, mockDiskCache, languageResolver, mockLifecycleOwner)

        // Run sync
        val result = syncManager.sync(force = true)
        assertTrue(result)

        coVerify { mockApi.bootstrap() }
        coVerify { mockApi.getLocalizations(null) }
        coVerify { mockDiskCache.saveCache(any(), any()) }

        // Assert memory cache is populated
        assertTrue(memoryCache.translationsState.value.isNotEmpty())
        assertEquals("Welcome", memoryCache.getTranslation("home.title", "en"))
    }
}
