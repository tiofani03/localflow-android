package com.localflow.sdk.data.cache

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

internal class MemoryCache {

    // ConcurrentHashMap of languageCode -> (key -> translation)
    private val translationsMap = ConcurrentHashMap<String, Map<String, String>>()
    
    // Expose overall StateFlow of current translations state
    private val _translationsState = MutableStateFlow<Map<String, Map<String, String>>>(emptyMap())
    val translationsState: StateFlow<Map<String, Map<String, String>>> = _translationsState.asStateFlow()

    fun updateAll(snapshot: Map<String, Map<String, String>>) {
        translationsMap.clear()
        translationsMap.putAll(snapshot)
        notifyObservers()
    }

    fun updateLanguage(languageCode: String, languageSnapshot: Map<String, String>) {
        translationsMap[languageCode.lowercase()] = languageSnapshot
        notifyObservers()
    }

    fun getTranslation(key: String, languageCode: String): String? {
        return translationsMap[languageCode.lowercase()]?.get(key)
    }

    fun getLanguageTranslations(languageCode: String): Map<String, String> {
        return translationsMap[languageCode.lowercase()].orEmpty()
    }

    fun clear() {
        translationsMap.clear()
        _translationsState.value = emptyMap()
    }

    private fun notifyObservers() {
        // Create an immutable defensive copy of the map for StateFlow
        val copy = HashMap<String, Map<String, String>>()
        for ((key, value) in translationsMap) {
            copy[key] = HashMap(value)
        }
        _translationsState.value = copy
    }
}
