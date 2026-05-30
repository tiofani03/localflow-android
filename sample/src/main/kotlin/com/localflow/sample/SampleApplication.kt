package com.localflow.sample

import android.app.Application
import com.localflow.sdk.Localflow
import com.localflow.sdk.LocalflowConfig

class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Configuration mapped to localhost Next.js server running in dev mode
        val config = LocalflowConfig.Builder(
            apiKey = "loc_live_6ae08dd90137d37c8c893598d8eb84cdff6b7deeef5864e192becc446b3e82cc", // Seeded key
            baseUrl = "https://localflow-1085648460092.asia-southeast2.run.app/"
        )
            .defaultLanguage("id")
            .syncIntervalMs(60_000L) // 1 minute interval for quick testing
            .enableAutoSync(true)
            .logLevel(LocalflowConfig.LogLevel.VERBOSE)
            .fallbackAssetPath("localflow_fallback.json")
            .build()

        Localflow.initialize(this, config)
    }
}
