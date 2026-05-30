package com.localflow.sdk.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
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

/**
 * Returns a translated string replacing {placeholder} values with namedArgs.
 */
@Composable
@ReadOnlyComposable
fun localflowString(key: String, namedArgs: Map<String, Any>): String {
    var raw = localflowString(key)
    if (namedArgs.isNotEmpty()) {
        for ((k, v) in namedArgs) {
            raw = raw.replace("{$k}", v.toString())
        }
    }
    return raw
}

/**
 * Returns a translated string as an AnnotatedString for rendering HTML tags in Compose.
 */
@Composable
@ReadOnlyComposable
fun localflowHtmlString(key: String): AnnotatedString {
    return AnnotatedString.fromHtml(localflowString(key))
}

/**
 * Returns a formatted translated string as an AnnotatedString for rendering HTML tags in Compose.
 */
@Composable
@ReadOnlyComposable
fun localflowHtmlString(key: String, vararg args: Any): AnnotatedString {
    return AnnotatedString.fromHtml(localflowString(key, *args))
}

/**
 * Returns a translated string with replaced {placeholder} values as an AnnotatedString for rendering HTML tags in Compose.
 */
@Composable
@ReadOnlyComposable
fun localflowHtmlString(key: String, namedArgs: Map<String, Any>): AnnotatedString {
    return AnnotatedString.fromHtml(localflowString(key, namedArgs))
}
