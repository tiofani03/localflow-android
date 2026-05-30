package com.localflow.sdk.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.localflow.sdk.Localflow
import com.localflow.sdk.data.model.LanguageInfo

/**
 * Remembers the list of currently registered languages within the project.
 */
import androidx.compose.runtime.collectAsState

@Composable
fun rememberLocalflowLanguages(): List<LanguageInfo> {
    return Localflow.observeAvailableLanguages().collectAsState(initial = Localflow.getAvailableLanguages()).value
}

/**
 * Remembers the currently active language code.
 */
@Composable
fun rememberLocalflowLanguage(): String {
    return Localflow.observeLanguage().collectAsState(initial = Localflow.getCurrentLanguage()).value
}

/**
 * Launched effect Composable that safely registers lifecycle listeners
 * and triggers background synchronizations when the lifecycle starts.
 */
@Composable
fun LocalflowSyncEffect() {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            Localflow.sync()
        }
    }
}
