package com.localflow.sample

import android.app.Application
import com.localflow.sdk.Localflow
import com.localflow.sdk.LocalflowConfig

class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Configuration mapped to localhost Next.js server running in dev mode
        val config = LocalflowConfig.Builder(
            apiKey = "loc_live_90b73700dd2ffc69a9c3701f87ec3dec0d40ea232d09718690041f942143c82e", // Seeded key
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
