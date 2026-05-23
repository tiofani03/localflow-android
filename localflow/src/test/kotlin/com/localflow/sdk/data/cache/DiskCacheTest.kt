package com.localflow.sdk.data.cache

import android.content.Context
import android.util.Log
import com.localflow.sdk.data.model.LanguageInfo
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DiskCacheTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var mockContext: Context
    private lateinit var diskCache: DiskCache
    private val apiKey = "loc_live_test123"

    @Before
    fun setUp() {
        // Mock Android Log static methods to avoid stub exceptions on JVM
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0

        mockContext = mockk()
        every { mockContext.filesDir } returns tempFolder.newFolder("files")

        diskCache = DiskCache(mockContext, apiKey)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testSaveAndLoadCache() = kotlinx.coroutines.runBlocking {
        val translations = mapOf(
            "en" to mapOf("home.title" to "Home", "home.subtitle" to "Welcome"),
            "id" to mapOf("home.title" to "Beranda")
        )

        val metadata = CacheMetadata(
            version = 5,
            etag = "\"test-etag\"",
            checksum = "test-checksum",
            publishedAt = "2026-05-22T13:00:00Z",
            defaultLanguage = "en",
            availableLanguages = listOf(
                LanguageInfo(code = "en", name = "English", isDefault = true),
                LanguageInfo(code = "id", name = "Indonesian", isDefault = false)
            ),
            lastSyncTimestamp = System.currentTimeMillis()
        )

        val saveResult = diskCache.saveCache(translations, metadata)
        assertTrue(saveResult)

        val loadedTranslations = diskCache.loadTranslations()
        assertNotNull(loadedTranslations)
        assertEquals("Home", loadedTranslations!!["en"]?.get("home.title"))
        assertEquals("Beranda", loadedTranslations["id"]?.get("home.title"))

        val loadedMeta = diskCache.loadMetadata()
        assertNotNull(loadedMeta)
        assertEquals(5, loadedMeta!!.version)
        assertEquals("\"test-etag\"", loadedMeta.etag)
        assertEquals("en", loadedMeta.defaultLanguage)
        assertEquals(2, loadedMeta.availableLanguages.size)
    }

    @Test
    fun testLoadNonExistentCache() = kotlinx.coroutines.runBlocking {
        assertNull(diskCache.loadTranslations())
        assertNull(diskCache.loadMetadata())
    }

    @Test
    fun testClearCache() = kotlinx.coroutines.runBlocking {
        val translations = mapOf("en" to mapOf("home.title" to "Home"))
        val metadata = CacheMetadata(
            version = 1,
            etag = "\"etag\"",
            checksum = "checksum",
            publishedAt = "2026-05-22T13:00:00Z",
            defaultLanguage = "en",
            availableLanguages = emptyList(),
            lastSyncTimestamp = System.currentTimeMillis()
        )

        diskCache.saveCache(translations, metadata)
        assertNotNull(diskCache.loadTranslations())

        diskCache.clear()
        assertNull(diskCache.loadTranslations())
        assertNull(diskCache.loadMetadata())
    }
}
