package com.localflow.sdk.domain

import com.localflow.sdk.data.model.LanguageInfo
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

class LanguageResolverTest {

    private lateinit var resolver: LanguageResolver
    private val availableLanguages = listOf(
        LanguageInfo(code = "en", name = "English", isDefault = true),
        LanguageInfo(code = "id", name = "Indonesian", isDefault = false),
        LanguageInfo(code = "ja", name = "Japanese", isDefault = false)
    )

    @Before
    fun setUp() {
        resolver = LanguageResolver("en")
    }

    @Test
    fun testResolveExplicitMatch() {
        assertEquals("id", resolver.resolve("id", availableLanguages))
        assertEquals("en", resolver.resolve("en", availableLanguages))
    }

    @Test
    fun testResolveExplicitPrefixMatch() {
        // "en-US" splitting prefix -> matches "en"
        assertEquals("en", resolver.resolve("en-US", availableLanguages))
        // "id-ID" splitting prefix -> matches "id"
        assertEquals("id", resolver.resolve("id-ID", availableLanguages))
    }

    @Test
    fun testResolveSystemLocaleFallback() {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale("id", "ID"))
            // No explicit code, should use default system language "id"
            assertEquals("id", resolver.resolve(null, availableLanguages))

            Locale.setDefault(Locale("ja", "JP"))
            assertEquals("ja", resolver.resolve(null, availableLanguages))
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun testResolveProjectDefaultFallback() {
        val originalLocale = Locale.getDefault()
        try {
            // Set system locale to Spanish ("es") which is NOT in availableLanguages
            Locale.setDefault(Locale("es", "ES"))

            // No explicit match, system locale not available, should use project default "en"
            assertEquals("en", resolver.resolve(null, availableLanguages))
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun testResolveAbsoluteFirstAvailableFallback() {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale("es", "ES"))

            val noDefaultLangs = listOf(
                LanguageInfo(code = "ja", name = "Japanese", isDefault = false),
                LanguageInfo(code = "id", name = "Indonesian", isDefault = false)
            )

            // No explicit, system "es" not available, no project default marked -> should use first available "ja"
            assertEquals("ja", resolver.resolve(null, noDefaultLangs))
        } finally {
            Locale.setDefault(originalLocale)
        }
    }
}
