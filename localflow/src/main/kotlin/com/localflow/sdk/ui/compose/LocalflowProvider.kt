package com.localflow.sdk.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import com.localflow.sdk.Localflow

/**
 * CompositionLocal key holding current active translations map (key -> value)
 */
val LocalTranslations = compositionLocalOf<Map<String, String>> { emptyMap() }

/**
 * CompositionLocal key holding current active language code (e.g. "en")
 */
val LocalCurrentLanguage = compositionLocalOf<String> { "en" }

/**
 * Main provider wrapper that should enclose your Composable hierarchy to enable
 * reactive, real-time string translations without rebuilding layouts.
 */
@Composable
fun LocalflowProvider(
    languageCode: String = Localflow.getCurrentLanguage(),
    content: @Composable () -> Unit
) {
    val translations by Localflow.observeTranslations()
        .collectAsState(initial = Localflow.getAllTranslations(languageCode))
    
    val currentLanguage by Localflow.observeLanguage()
        .collectAsState(initial = languageCode)

    CompositionLocalProvider(
        LocalTranslations provides translations,
        LocalCurrentLanguage provides currentLanguage
    ) {
        content()
    }
}
