package com.localflow.sdk.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.localflow.sdk.Localflow

/**
 * Returns a translated string dynamically inside Compose layouts.
 * Automatically recomposes when the language or translations map changes.
 */
@Composable
@ReadOnlyComposable
fun localflowString(key: String): String {
    val lang = LocalCurrentLanguage.current // Force compose to track language changes
    val translations = LocalTranslations.current
    return translations[key] ?: Localflow.getStringOrNull(key) ?: key
}

/**
 * Returns a formatted translated string dynamically inside Compose layouts.
 * Automatically recomposes when the language or translations map changes.
 */
@Composable
@ReadOnlyComposable
fun localflowString(key: String, vararg args: Any): String {
    val raw = localflowString(key)
    return if (args.isNotEmpty()) String.format(raw, *args) else raw
}
