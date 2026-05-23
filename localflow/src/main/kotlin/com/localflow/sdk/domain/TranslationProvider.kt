package com.localflow.sdk.domain

import com.localflow.sdk.data.cache.MemoryCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

internal class TranslationProvider(
    private val memoryCache: MemoryCache,
    private val defaultLanguage: String
) {
    private val _currentLanguage = MutableStateFlow(defaultLanguage)
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    // Expose flow of active translations map for Jetpack Compose integration
    val activeTranslationsFlow: StateFlow<Map<String, String>> by lazy {
        val flow = MutableStateFlow<Map<String, String>>(emptyMap())
        
        // Combine translations map and current language to emit active translation mapping
        @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            combine(memoryCache.translationsState, _currentLanguage) { translations, lang ->
                translations[lang.lowercase()].orEmpty()
            }.collect {
                flow.value = it
            }
        }
        flow
    }

    fun setLanguage(languageCode: String) {
        _currentLanguage.value = languageCode.lowercase()
    }

    /**
     * Resolves a key's translation value using the standard fallback hierarchy:
     * 1. Value in current language
     * 2. Value in default language
     * 3. Key name itself (fallback)
     */
    fun getString(key: String, languageCode: String? = null): String {
        return getStringOrNull(key, languageCode) ?: key
    }

    fun getStringOrNull(key: String, languageCode: String? = null): String? {
        val targetLang = (languageCode ?: _currentLanguage.value).lowercase()
        
        // 1. Current/Explicit language
        val primaryVal = memoryCache.getTranslation(key, targetLang)
        if (primaryVal != null) return primaryVal

        // 2. Default fallback language
        val fallbackLang = defaultLanguage.lowercase()
        if (targetLang != fallbackLang) {
            val fallbackVal = memoryCache.getTranslation(key, fallbackLang)
            if (fallbackVal != null) return fallbackVal
        }

        return null
    }

    fun getTranslationsForLanguage(languageCode: String): Map<String, String> {
        return memoryCache.getLanguageTranslations(languageCode)
    }
}

