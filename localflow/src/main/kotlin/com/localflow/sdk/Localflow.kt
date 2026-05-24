package com.localflow.sdk

import android.content.Context
import com.localflow.sdk.data.cache.DiskCache
import com.localflow.sdk.data.cache.MemoryCache
import com.localflow.sdk.data.model.LanguageInfo
import com.localflow.sdk.data.network.LocalflowApiImpl
import com.localflow.sdk.domain.LanguageResolver
import com.localflow.sdk.domain.SyncManager
import com.localflow.sdk.domain.TranslationProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking

/**
 * Main Entry Point for Locaflow localization SDK.
 * This is a thread-safe singleton object.
 */
object Localflow {

    private var context: Context? = null
    private var config: LocalflowConfig? = null
    private var memoryCache: MemoryCache? = null
    private var diskCache: DiskCache? = null
    private var api: LocalflowApiImpl? = null
    private var languageResolver: LanguageResolver? = null
    private var translationProvider: TranslationProvider? = null
    private var syncManager: SyncManager? = null

    @Volatile
    private var initialized = false

    /**
     * Initializes the Locaflow SDK with the given application context and config.
     * Call this inside your Application's onCreate() method.
     */
    fun initialize(context: Context, config: LocalflowConfig) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            
            val appCtx = context.applicationContext
            this.context = appCtx
            this.config = config

            val mem = MemoryCache()
            this.memoryCache = mem

            val disk = DiskCache(appCtx, config.apiKey)
            this.diskCache = disk

            val apiImpl = LocalflowApiImpl(config)
            this.api = apiImpl

            val resolver = LanguageResolver(config.defaultLanguage)
            this.languageResolver = resolver

            val provider = TranslationProvider(mem, config.defaultLanguage)
            this.translationProvider = provider

            val sync = SyncManager(
                context = appCtx,
                config = config,
                api = apiImpl,
                memoryCache = mem,
                diskCache = disk,
                languageResolver = resolver
            )
            this.syncManager = sync

            // Listen for sync changes to update resolved language automatically
            sync.addListener { _, _ ->
                resolveAndSetBestLanguage()
            }

            initialized = true
            
            // Try resolving language immediately on launch from disk cache
            resolveAndSetBestLanguage()
        }
    }

    val isInitialized: Boolean
        get() = initialized

    private fun requireInitialized() {
        if (!initialized) {
            throw LocalflowException.NotInitializedException()
        }
    }

    private fun resolveAndSetBestLanguage() {
        val meta = syncManager?.getMetadata()
        val resolver = languageResolver ?: return
        val provider = translationProvider ?: return
        
        val resolved = resolver.resolve(
            explicitCode = provider.currentLanguage.value,
            availableLanguages = meta?.availableLanguages.orEmpty()
        )
        provider.setLanguage(resolved)
    }

    /**
     * Get translated text for the specified key in the currently active language.
     * Returns the key name if no translation is found.
     */
    fun getString(key: String): String {
        requireInitialized()
        return translationProvider?.getString(key) ?: key
    }

    /**
     * Get translated text for the specified key in a specific language.
     * Returns the key name if no translation is found.
     */
    fun getString(key: String, language: String): String {
        requireInitialized()
        return translationProvider?.getString(key, language) ?: key
    }

    /**
     * Get translated text for the specified key, formatting it with the provided arguments.
     */
    fun getString(key: String, vararg args: Any): String {
        val raw = getString(key)
        return if (args.isNotEmpty()) String.format(raw, *args) else raw
    }

    /**
     * Get translated text for the specified key or null if not found.
     */
    fun getStringOrNull(key: String): String? {
        requireInitialized()
        return translationProvider?.getStringOrNull(key)
    }

    /**
     * Get translated text for the specified key in a specific language or null if not found.
     */
    fun getStringOrNull(key: String, language: String): String? {
        requireInitialized()
        return translationProvider?.getStringOrNull(key, language)
    }

    /**
     * Sets the active language code explicitly (e.g. "id" or "en").
     * This override will persist in runtime. To reset, pass null or resolve from system.
     */
    fun setLanguage(code: String) {
        requireInitialized()
        translationProvider?.setLanguage(code)
        resolveAndSetBestLanguage()
        syncManager?.notifyTranslationsChanged()
    }

    /**
     * Get the currently active language code.
     */
    fun getCurrentLanguage(): String {
        requireInitialized()
        return translationProvider?.currentLanguage?.value ?: config?.defaultLanguage ?: "en"
    }

    /**
     * Get the list of all available languages fetched from the server.
     */
    fun getAvailableLanguages(): List<LanguageInfo> {
        requireInitialized()
        return syncManager?.getMetadata()?.availableLanguages.orEmpty()
    }

    /**
     * Get the default language code configured in the project.
     */
    fun getDefaultLanguage(): String {
        requireInitialized()
        return syncManager?.getMetadata()?.defaultLanguage ?: config?.defaultLanguage ?: "en"
    }

    /**
     * Returns a map of all translations loaded for a specific language code.
     */
    fun getAllTranslations(language: String): Map<String, String> {
        requireInitialized()
        return translationProvider?.getTranslationsForLanguage(language).orEmpty()
    }

    /**
     * Triggers a manual background synchronization check with the server.
     * Caches are updated automatically.
     */
    suspend fun sync() {
        requireInitialized()
        syncManager?.sync(force = false)
    }

    /**
     * Forcefully re-downloads all project localizations, ignoring current versions and ETags.
     */
    suspend fun forceSync() {
        requireInitialized()
        syncManager?.sync(force = true)
    }

    /**
     * Add a listener to get notified when new translation versions are published & loaded.
     */
    fun addOnTranslationsChangedListener(listener: OnTranslationsChangedListener) {
        requireInitialized()
        syncManager?.addListener(listener)
    }

    /**
     * Remove a registered translations changed listener.
     */
    fun removeOnTranslationsChangedListener(listener: OnTranslationsChangedListener) {
        requireInitialized()
        syncManager?.removeListener(listener)
    }

    /**
     * Expose a Flow of the active language's translation map.
     * Extremely useful for reactive bindings like Jetpack Compose.
     */
    fun observeTranslations(): Flow<Map<String, String>> {
        requireInitialized()
        return translationProvider?.activeTranslationsFlow ?: kotlinx.coroutines.flow.emptyFlow()
    }

    /**
     * Expose a Flow of the active language code.
     */
    fun observeLanguage(): Flow<String> {
        requireInitialized()
        return translationProvider?.currentLanguage ?: kotlinx.coroutines.flow.emptyFlow()
    }

    /**
     * Expose a Flow of the available languages list.
     * Use this in Jetpack Compose to dynamically update language pickers when sync finishes.
     */
    fun observeAvailableLanguages(): Flow<List<LanguageInfo>> {
        requireInitialized()
        return syncManager?.availableLanguages ?: kotlinx.coroutines.flow.emptyFlow()
    }

    /**
     * Get current active version of loaded localizations.
     */
    fun getVersion(): Int? {
        requireInitialized()
        return syncManager?.getMetadata()?.version
    }

    /**
     * Get current cached translations hash checksum.
     */
    fun getChecksum(): String? {
        requireInitialized()
        return syncManager?.getMetadata()?.checksum
    }

    /**
     * Clear all persistent disk caches and memory caches.
     */
    fun clearCache() {
        requireInitialized()
        runBlocking {
            syncManager?.shutdown()
            diskCache?.clear()
            memoryCache?.clear()
            initialized = false
            initialize(context!!, config!!)
        }
    }

    /**
     * Stops the periodic sync engine and releases system resources.
     */
    fun shutdown() {
        if (!initialized) return
        synchronized(this) {
            if (!initialized) return
            syncManager?.shutdown()
            memoryCache?.clear()
            initialized = false
            context = null
            config = null
            memoryCache = null
            diskCache = null
            api = null
            languageResolver = null
            translationProvider = null
            syncManager = null
        }
    }
}
