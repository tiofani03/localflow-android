package com.localflow.sdk

/**
 * Configuration class for Locaflow Android SDK.
 * Use the [Builder] to instantiate.
 */
data class LocalflowConfig private constructor(
    val apiKey: String,
    val baseUrl: String,
    val defaultLanguage: String,
    val syncIntervalMs: Long,
    val enableAutoSync: Boolean,
    val connectTimeoutMs: Long,
    val readTimeoutMs: Long,
    val logLevel: LogLevel
) {
    enum class LogLevel {
        NONE, BASIC, VERBOSE
    }

    class Builder(
        private val apiKey: String,
        private val baseUrl: String
    ) {
        private var defaultLanguage: String = "en"
        private var syncIntervalMs: Long = 300_000L // 5 minutes
        private var enableAutoSync: Boolean = true
        private var connectTimeoutMs: Long = 30_000L
        private var readTimeoutMs: Long = 30_000L
        private var logLevel: LogLevel = LogLevel.NONE

        /**
         * Set the default fallback language code (e.g. "en", "id").
         */
        fun defaultLanguage(code: String) = apply {
            this.defaultLanguage = code.lowercase()
        }

        /**
         * Set background sync interval in milliseconds. Min is 60_000 (1 minute).
         */
        fun syncIntervalMs(ms: Long) = apply {
            this.syncIntervalMs = maxOf(60_000L, ms)
        }

        /**
         * Enable/disable automatic periodic sync when the app is in the foreground.
         */
        fun enableAutoSync(enable: Boolean) = apply {
            this.enableAutoSync = enable
        }

        /**
         * Set connect timeout for the HTTP client in milliseconds.
         */
        fun connectTimeoutMs(ms: Long) = apply {
            this.connectTimeoutMs = maxOf(0, ms)
        }

        /**
         * Set read timeout for the HTTP client in milliseconds.
         */
        fun readTimeoutMs(ms: Long) = apply {
            this.readTimeoutMs = maxOf(0, ms)
        }

        /**
         * Set logging verbosity for SDK network and operations.
         */
        fun logLevel(level: LogLevel) = apply {
            this.logLevel = level
        }

        /**
         * Build the configuration.
         * Throws [IllegalArgumentException] if parameters are invalid.
         */
        fun build(): LocalflowConfig {
            require(apiKey.startsWith("loc_live_")) {
                "API key must start with 'loc_live_'"
            }
            require(baseUrl.isNotBlank()) {
                "Base URL must not be blank"
            }
            return LocalflowConfig(
                apiKey = apiKey,
                baseUrl = baseUrl.trimEnd('/'),
                defaultLanguage = defaultLanguage,
                syncIntervalMs = syncIntervalMs,
                enableAutoSync = enableAutoSync,
                connectTimeoutMs = connectTimeoutMs,
                readTimeoutMs = readTimeoutMs,
                logLevel = logLevel
            )
        }
    }
}
