package com.localflow.sdk.ui.xml

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.localflow.sdk.Localflow
import com.localflow.sdk.OnTranslationsChangedListener

/**
 * Base [AppCompatActivity] class that automates the integration of Locaflow resources.
 * Extend this in your project Activities to support dynamic string replacements.
 */
open class LocalflowActivity : AppCompatActivity() {

    private val translationsChangedListener = OnTranslationsChangedListener { _, _ ->
        // Auto-recreate the activity to draw new string resources dynamically at runtime!
        runOnUiThread {
            recreate()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        // Intercept context resources lookup
        super.attachBaseContext(LocalflowContextWrapper(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Localflow.isInitialized) {
            Localflow.addOnTranslationsChangedListener(translationsChangedListener)
        }
    }

    override fun onDestroy() {
        if (Localflow.isInitialized) {
            Localflow.removeOnTranslationsChangedListener(translationsChangedListener)
        }
        super.onDestroy()
    }
}
