package com.localflow.sdk.domain

import com.localflow.sdk.data.cache.MemoryCache
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class TranslationProviderTest {

    private lateinit var memoryCache: MemoryCache
    private lateinit var provider: TranslationProvider
    private val defaultLang = "en"

    @Before
    fun setUp() {
        memoryCache = MemoryCache()
        provider = TranslationProvider(memoryCache, defaultLang)
    }

    @Test
    fun testGetStringDirect() {
        val snapshot = mapOf(
            "en" to mapOf("home.title" to "Home English"),
            "id" to mapOf("home.title" to "Home Indonesian")
        )
        memoryCache.updateAll(snapshot)

        provider.setLanguage("id")
        assertEquals("Home Indonesian", provider.getString("home.title"))

        provider.setLanguage("en")
        assertEquals("Home English", provider.getString("home.title"))
    }

    @Test
    fun testGetStringFallbackToDefault() {
        val snapshot = mapOf(
            "en" to mapOf("home.title" to "Home English"),
            "id" to emptyMap()
        )
        memoryCache.updateAll(snapshot)

        provider.setLanguage("id")
        // "home.title" doesn't exist in "id", should fallback to "en"
        assertEquals("Home English", provider.getString("home.title"))
    }

    @Test
    fun testGetStringLastResortKeyName() {
        val snapshot = mapOf(
            "en" to emptyMap<String, String>(),
            "id" to emptyMap<String, String>()
        )
        memoryCache.updateAll(snapshot)

        provider.setLanguage("id")
        // Doesn't exist anywhere, should return key name
        assertEquals("home.title", provider.getString("home.title"))
    }

    @Test
    fun testGetStringOrNull() {
        val snapshot = mapOf(
            "en" to mapOf("home.title" to "Home English")
        )
        memoryCache.updateAll(snapshot)

        provider.setLanguage("id")
        // home.title exists in en (default), but not in id. getStringOrNull should resolve fallback.
        assertEquals("Home English", provider.getStringOrNull("home.title"))

        // totally missing key
        assertNull(provider.getStringOrNull("missing.key"))
    }

    @Test
    fun testExplicitLanguageLookup() {
        val snapshot = mapOf(
            "en" to mapOf("home.title" to "Home English"),
            "id" to mapOf("home.title" to "Home Indonesian")
        )
        memoryCache.updateAll(snapshot)

        provider.setLanguage("en")
        // Even if active is English, lookup Indonesian explicitly
        assertEquals("Home Indonesian", provider.getString("home.title", "id"))
    }
}
