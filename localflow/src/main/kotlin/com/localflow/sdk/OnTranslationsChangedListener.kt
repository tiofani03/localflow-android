package com.localflow.sdk

/**
 * Interface definition for a callback to be invoked when the localizations have been updated.
 */
fun interface OnTranslationsChangedListener {
    /**
     * Called when localizations are updated with a new version.
     *
     * @param version The new published version number loaded.
     * @param languageCode The active resolved language code.
     */
    fun onTranslationsChanged(version: Int, languageCode: String)
}
