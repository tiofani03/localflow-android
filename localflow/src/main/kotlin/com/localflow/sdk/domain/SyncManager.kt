package com.localflow.sdk.domain

import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.localflow.sdk.LocalflowConfig
import com.localflow.sdk.OnTranslationsChangedListener
import com.localflow.sdk.data.cache.CacheMetadata
import com.localflow.sdk.data.cache.DiskCache
import com.localflow.sdk.data.cache.MemoryCache
import com.localflow.sdk.data.model.LanguageInfo
import com.localflow.sdk.data.network.ApiResult
import com.localflow.sdk.data.network.LocalflowApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.CopyOnWriteArrayList

internal class SyncManager(
    private val context: Context,
    private val config: LocalflowConfig,
    private val api: LocalflowApi,
    private val memoryCache: MemoryCache,
    private val diskCache: DiskCache,
    private val languageResolver: LanguageResolver,
    private val lifecycleOwner: LifecycleOwner = ProcessLifecycleOwner.get()
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val syncMutex = Mutex()
    private var syncJob: Job? = null
    private var isAppInForeground = false

    private val listeners = CopyOnWriteArrayList<OnTranslationsChangedListener>()

    // Tracking cache details locally
    private var currentMetadata: CacheMetadata? = null

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _availableLanguages = MutableStateFlow<List<LanguageInfo>>(emptyList())
    val availableLanguages = _availableLanguages.asStateFlow()

    init {
        // Startup behavior: Immediately load disk cache into memory
        scope.launch {
            loadFromDiskCache()
            
            // Start process lifecycle observations for foreground auto sync
            withContext(Dispatchers.Main) {
                lifecycleOwner.lifecycle.addObserver(this@SyncManager)
            }
        }
    }

    private suspend fun loadFromDiskCache() {
        syncMutex.withLock {
            log(LocalflowConfig.LogLevel.VERBOSE, "Loading translations from disk cache...")
            val diskData = diskCache.loadTranslations()
            val diskMeta = diskCache.loadMetadata()
            if (diskData != null && diskMeta != null) {
                currentMetadata = diskMeta
                _availableLanguages.value = diskMeta.availableLanguages
                memoryCache.updateAll(diskData)
                log(LocalflowConfig.LogLevel.BASIC, "Disk cache loaded successfully. Version: ${diskMeta.version}")
            } else {
                log(LocalflowConfig.LogLevel.BASIC, "No local cache found on disk.")
                config.fallbackAssetPath?.let { assetPath ->
                    log(LocalflowConfig.LogLevel.VERBOSE, "Attempting to load fallback from asset: $assetPath")
                    val assetData = diskCache.loadFallbackFromAsset(assetPath)
                    if (assetData != null) {
                        memoryCache.updateAll(assetData)
                        log(LocalflowConfig.LogLevel.BASIC, "Fallback asset loaded to memory cache successfully.")
                    } else {
                        log(LocalflowConfig.LogLevel.BASIC, "Failed to load fallback asset from: $assetPath")
                    }
                }
            }
        }
    }

    /**
     * Executes network synchronization to fetch the latest translations.
     */
    suspend fun sync(force: Boolean = false): Boolean = withContext(Dispatchers.Default) {
        if (_isSyncing.value) return@withContext false
        _isSyncing.value = true

        var success = false
        try {
            syncMutex.withLock {
                success = performSync(force)
            }
        } catch (e: Exception) {
            log(LocalflowConfig.LogLevel.BASIC, "Error occurred during sync execution", e)
        } finally {
            _isSyncing.value = false
        }
        return@withContext success
    }

    private suspend fun performSync(force: Boolean): Boolean {
        val meta = currentMetadata
        if (meta == null || force) {
            // First time sync or forced sync: Run bootstrap first, then download all localizations
            log(LocalflowConfig.LogLevel.VERBOSE, "Performing fresh bootstrap...")
            when (val bootstrapResult = api.bootstrap()) {
                is ApiResult.Success -> {
                    val boot = bootstrapResult.data
                    log(LocalflowConfig.LogLevel.VERBOSE, "Bootstrap succeeded. Version: ${boot.latestVersion}")
                    return fetchAndPersistAllLocalizations(
                        targetVersion = boot.latestVersion,
                        etag = null, // fresh fetch
                        fallbackDefaultLang = boot.defaultLanguage?.code ?: config.defaultLanguage,
                        fallbackAvailableLangs = boot.availableLanguages
                    )
                }
                is ApiResult.Error -> {
                    log(LocalflowConfig.LogLevel.BASIC, "Bootstrap failed with error: ${bootstrapResult.code}")
                    return false
                }
                is ApiResult.NetworkFailure -> {
                    log(LocalflowConfig.LogLevel.BASIC, "Bootstrap failed due to network failure", bootstrapResult.cause)
                    return false
                }
                ApiResult.NotModified -> return false // Cannot happen on bootstrap
            }
        } else {
            // Incremental sync check: Call versions/latest first to see if update exists
            log(LocalflowConfig.LogLevel.VERBOSE, "Checking for updates with current version: ${meta.version}")
            when (val versionResult = api.getLatestVersion(currentVersion = meta.version)) {
                is ApiResult.Success -> {
                    val ver = versionResult.data
                    if (ver.hasUpdate) {
                        log(LocalflowConfig.LogLevel.BASIC, "New localization version available: ${ver.version} (current: ${meta.version})")
                        return fetchAndPersistAllLocalizations(
                            targetVersion = ver.version,
                            etag = meta.etag,
                            fallbackDefaultLang = meta.defaultLanguage,
                            fallbackAvailableLangs = meta.availableLanguages
                        )
                    } else {
                        log(LocalflowConfig.LogLevel.VERBOSE, "Cache is already up to date. Version: ${meta.version}")
                        // Update last checked sync timestamp in metadata
                        val updatedMeta = meta.copy(lastSyncTimestamp = System.currentTimeMillis())
                        diskCache.saveCache(memoryCache.translationsState.value, updatedMeta)
                        currentMetadata = updatedMeta
                        return true
                    }
                }
                ApiResult.NotModified -> {
                    log(LocalflowConfig.LogLevel.VERBOSE, "Server reports 304 Not Modified on version check")
                    return true
                }
                is ApiResult.Error -> {
                    log(LocalflowConfig.LogLevel.BASIC, "Version check failed: ${versionResult.code}")
                    return false
                }
                is ApiResult.NetworkFailure -> {
                    log(LocalflowConfig.LogLevel.BASIC, "Version check failed due to network", versionResult.cause)
                    return false
                }
            }
        }
    }

    private suspend fun fetchAndPersistAllLocalizations(
        targetVersion: Int,
        etag: String?,
        fallbackDefaultLang: String,
        fallbackAvailableLangs: List<LanguageInfo>
    ): Boolean {
        log(LocalflowConfig.LogLevel.VERBOSE, "Fetching full localizations bundle...")
        when (val locResult = api.getLocalizations(etag)) {
            is ApiResult.Success -> {
                val locResponse = locResult.data
                val newEtag = locResult.etag ?: locResponse.etag
                
                val newAvailableLangs = locResponse.availableLanguages
                val newDefaultLang = newAvailableLangs.firstOrNull { it.isDefault }?.code ?: fallbackDefaultLang
                
                log(LocalflowConfig.LogLevel.BASIC, "Localization bundle download successful. Version: $targetVersion")
                
                // Construct and save cache
                val newMeta = CacheMetadata(
                    version = targetVersion,
                    etag = newEtag,
                    checksum = locResponse.checksum,
                    publishedAt = locResponse.publishedAt,
                    defaultLanguage = newDefaultLang,
                    availableLanguages = newAvailableLangs,
                    lastSyncTimestamp = System.currentTimeMillis()
                )

                val saved = diskCache.saveCache(locResponse.localizations, newMeta)
                if (saved) {
                    currentMetadata = newMeta
                    _availableLanguages.value = newMeta.availableLanguages
                    memoryCache.updateAll(locResponse.localizations)
                    notifyListeners(targetVersion)
                    return true
                }
                return false
            }
            ApiResult.NotModified -> {
                log(LocalflowConfig.LogLevel.VERBOSE, "Server reports 304 Not Modified on localization download")
                return true
            }
            is ApiResult.Error -> {
                log(LocalflowConfig.LogLevel.BASIC, "Failed to download localization bundle: ${locResult.code}")
                return false
            }
            is ApiResult.NetworkFailure -> {
                log(LocalflowConfig.LogLevel.BASIC, "Failed to download localization bundle due to network", locResult.cause)
                return false
            }
        }
    }

    private fun notifyListeners(version: Int) {
        val resolvedLang = languageResolver.resolve(
            explicitCode = null, // resolves best locale
            availableLanguages = currentMetadata?.availableLanguages.orEmpty()
        )
        for (listener in listeners) {
            try {
                listener.onTranslationsChanged(version, resolvedLang)
            } catch (e: Exception) {
                Log.e("SyncManager", "Error in OnTranslationsChangedListener", e)
            }
        }
    }

    internal fun notifyTranslationsChanged() {
        notifyListeners(currentMetadata?.version ?: 0)
    }

    fun addListener(listener: OnTranslationsChangedListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: OnTranslationsChangedListener) {
        listeners.remove(listener)
    }

    fun getMetadata(): CacheMetadata? = currentMetadata

    // Lifecycle observations to trigger sync & periodic polling
    override fun onStart(owner: LifecycleOwner) {
        isAppInForeground = true
        log(LocalflowConfig.LogLevel.VERBOSE, "App came to foreground. Scheduling auto sync.")
        startPeriodicSync()
    }

    override fun onStop(owner: LifecycleOwner) {
        isAppInForeground = false
        log(LocalflowConfig.LogLevel.VERBOSE, "App went to background. Cancelling periodic sync.")
        stopPeriodicSync()
    }

    private fun startPeriodicSync() {
        if (!config.enableAutoSync) return
        stopPeriodicSync()
        syncJob = scope.launch {
            while (isAppInForeground) {
                log(LocalflowConfig.LogLevel.VERBOSE, "Auto sync triggering...")
                sync()
                delay(config.syncIntervalMs)
            }
        }
    }

    private fun stopPeriodicSync() {
        syncJob?.cancel()
        syncJob = null
    }

    fun shutdown() {
        stopPeriodicSync()
        listeners.clear()
        scope.launch {
            withContext(Dispatchers.Main) {
                lifecycleOwner.lifecycle.removeObserver(this@SyncManager)
            }
        }
    }

    private fun log(level: LocalflowConfig.LogLevel, msg: String, err: Throwable? = null) {
        if (config.logLevel >= level) {
            if (err != null) {
                Log.e("SyncManager", msg, err)
            } else {
                Log.d("SyncManager", msg)
            }
        }
    }

    private operator fun LocalflowConfig.LogLevel.compareTo(other: LocalflowConfig.LogLevel): Int {
        return this.ordinal.compareTo(other.ordinal)
    }
}
