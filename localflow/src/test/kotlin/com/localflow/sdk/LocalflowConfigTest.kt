package com.localflow.sdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalflowConfigTest {

    @Test
    fun testBuilderValidParams() {
        val config = LocalflowConfig.Builder(
            apiKey = "loc_live_abc123",
            baseUrl = "https://api.localflow.com/"
        )
            .defaultLanguage("ID")
            .syncIntervalMs(120_000L)
            .enableAutoSync(false)
            .connectTimeoutMs(15_000L)
            .readTimeoutMs(20_000L)
            .logLevel(LocalflowConfig.LogLevel.VERBOSE)
            .build()

        assertEquals("loc_live_abc123", config.apiKey)
        // Verify base URL trailing slash is trimmed
        assertEquals("https://api.localflow.com", config.baseUrl)
        // Verify default language is lowercased
        assertEquals("id", config.defaultLanguage)
        assertEquals(120_000L, config.syncIntervalMs)
        assertFalse(config.enableAutoSync)
        assertEquals(15_000L, config.connectTimeoutMs)
        assertEquals(20_000L, config.readTimeoutMs)
        assertEquals(LocalflowConfig.LogLevel.VERBOSE, config.logLevel)
    }

    @Test
    fun testBuilderDefaultParams() {
        val config = LocalflowConfig.Builder(
            apiKey = "loc_live_abc123",
            baseUrl = "https://api.localflow.com"
        ).build()

        assertEquals("en", config.defaultLanguage)
        assertEquals(300_000L, config.syncIntervalMs)
        assertTrue(config.enableAutoSync)
        assertEquals(30_000L, config.connectTimeoutMs)
        assertEquals(30_000L, config.readTimeoutMs)
        assertEquals(LocalflowConfig.LogLevel.NONE, config.logLevel)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testBuilderInvalidApiKey() {
        LocalflowConfig.Builder(
            apiKey = "invalid_key_123",
            baseUrl = "https://api.localflow.com"
        ).build()
    }

    @Test(expected = IllegalArgumentException::class)
    fun testBuilderBlankBaseUrl() {
        LocalflowConfig.Builder(
            apiKey = "loc_live_abc123",
            baseUrl = "   "
        ).build()
    }

    @Test
    fun testBuilderMinSyncInterval() {
        val config = LocalflowConfig.Builder(
            apiKey = "loc_live_abc123",
            baseUrl = "https://api.localflow.com"
        )
            .syncIntervalMs(5_000L) // lower than 60_000 minimum
            .build()

        // Should enforce minimum of 60_000 ms
        assertEquals(60_000L, config.syncIntervalMs)
    }
}
