package com.localflow.sample

import android.app.Application
import com.localflow.sdk.Localflow
import com.localflow.sdk.LocalflowConfig

class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Configuration mapped to localhost Next.js server running in dev mode
        val config = LocalflowConfig.Builder(
            apiKey = "lloc_live_8cd75ae4819d67e8cbda063460f79b8c3f6fde7969b084702f62773208df299f", // Seeded key
            baseUrl = "http://192.168.0.114:3000"
        )
            .defaultLanguage("en")
            .syncIntervalMs(60_000L) // 1 minute interval for quick testing
            .enableAutoSync(true)
            .logLevel(LocalflowConfig.LogLevel.VERBOSE)
            .build()

        Localflow.initialize(this, config)
    }
}
