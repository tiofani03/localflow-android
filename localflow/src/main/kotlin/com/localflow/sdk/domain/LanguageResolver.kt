package com.localflow.sdk.domain

import com.localflow.sdk.data.model.LanguageInfo
import java.util.Locale

internal class LanguageResolver(
    private val defaultLanguageCode: String
) {
    /**
     * Resolves the best available language from the project languages
     * based on: explicit selected language, current system locale, and default configured fallback.
     */
    fun resolve(
        explicitCode: String?,
        availableLanguages: List<LanguageInfo>
    ): String {
        if (availableLanguages.isEmpty()) {
            return explicitCode?.lowercase() ?: defaultLanguageCode
        }

        val availableCodes = availableLanguages.map { it.code.lowercase() }.toSet()

        // 1. Try explicit selected language (if valid)
        if (explicitCode != null) {
            val normalizedExplicit = explicitCode.lowercase()
            if (availableCodes.contains(normalizedExplicit)) {
                return normalizedExplicit
            }
            // Try prefix match for explicit code (e.g. "en-us" -> "en")
            val explicitPrefix = normalizedExplicit.split("-")[0]
            if (availableCodes.contains(explicitPrefix)) {
                return explicitPrefix
            }
        }

        // 2. Try current system language
        val systemLocale = Locale.getDefault()
        val systemLang = systemLocale.language.lowercase() // e.g. "en" or "id"
        if (availableCodes.contains(systemLang)) {
            return systemLang
        }
        
        val systemCountryLang = "${systemLang}-${systemLocale.country.lowercase()}" // e.g. "en-us"
        if (availableCodes.contains(systemCountryLang)) {
            return systemCountryLang
        }

        // 3. Try to use default project language from server
        val projectDefault = availableLanguages.firstOrNull { it.isDefault }?.code?.lowercase()
        if (projectDefault != null && availableCodes.contains(projectDefault)) {
            return projectDefault
        }

        // 4. Fallback to SDK default configuration language
        if (availableCodes.contains(defaultLanguageCode.lowercase())) {
            return defaultLanguageCode.lowercase()
        }

        // 5. Absolute fallback: use first available language
        return availableLanguages.firstOrNull()?.code?.lowercase() ?: defaultLanguageCode
    }
}
