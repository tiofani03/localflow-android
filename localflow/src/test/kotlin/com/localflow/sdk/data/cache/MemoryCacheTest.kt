package com.localflow.sdk.data.cache

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MemoryCacheTest {

    private lateinit var memoryCache: MemoryCache

    @Before
    fun setUp() {
        memoryCache = MemoryCache()
    }

    @Test
    fun testUpdateAllAndGetTranslation() {
        val snapshot = mapOf(
            "en" to mapOf("home.title" to "Home", "home.subtitle" to "Welcome"),
            "id" to mapOf("home.title" to "Beranda")
        )

        memoryCache.updateAll(snapshot)

        assertEquals("Home", memoryCache.getTranslation("home.title", "en"))
        assertEquals("Welcome", memoryCache.getTranslation("home.subtitle", "en"))
        assertEquals("Beranda", memoryCache.getTranslation("home.title", "id"))
        assertNull(memoryCache.getTranslation("home.subtitle", "id"))
    }

    @Test
    fun testUpdateLanguage() {
        val initial = mapOf(
            "en" to mapOf("home.title" to "Home")
        )
        memoryCache.updateAll(initial)

        val newIdSnapshot = mapOf("home.title" to "Beranda", "btn.login" to "Masuk")
        memoryCache.updateLanguage("id", newIdSnapshot)

        assertEquals("Home", memoryCache.getTranslation("home.title", "en"))
        assertEquals("Beranda", memoryCache.getTranslation("home.title", "id"))
        assertEquals("Masuk", memoryCache.getTranslation("btn.login", "id"))
    }

    @Test
    fun testGetLanguageTranslations() {
        val snapshot = mapOf(
            "en" to mapOf("home.title" to "Home")
        )
        memoryCache.updateAll(snapshot)

        val enTranslations = memoryCache.getLanguageTranslations("en")
        assertEquals(1, enTranslations.size)
        assertEquals("Home", enTranslations["home.title"])

        val idTranslations = memoryCache.getLanguageTranslations("id")
        assertTrue(idTranslations.isEmpty())
    }

    @Test
    fun testClear() {
        val snapshot = mapOf(
            "en" to mapOf("home.title" to "Home")
        )
        memoryCache.updateAll(snapshot)
        assertEquals("Home", memoryCache.getTranslation("home.title", "en"))

        memoryCache.clear()
        assertNull(memoryCache.getTranslation("home.title", "en"))
        assertTrue(memoryCache.translationsState.value.isEmpty())
    }
}
