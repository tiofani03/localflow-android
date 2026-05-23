package com.localflow.sdk.ui.xml

import android.content.res.Resources
import com.localflow.sdk.Localflow

/**
 * Custom [Resources] wrapper that intercepts string and text lookups
 * and replaces them with dynamic values fetched from the Locaflow localization engine.
 */
class LocalflowResources(
    private val delegate: Resources,
    private val theme: Resources.Theme? = null
) : Resources(delegate.assets, delegate.displayMetrics, delegate.configuration) {

    @Throws(NotFoundException::class)
    override fun getString(id: Int): String {
        val entryName = getResourceEntryKeyName(id)
        if (entryName != null) {
            val dynamicString = Localflow.getStringOrNull(entryName)
            if (dynamicString != null) {
                return dynamicString
            }
        }
        return delegate.getString(id)
    }

    @Throws(NotFoundException::class)
    override fun getString(id: Int, vararg formatArgs: Any?): String {
        val entryName = getResourceEntryKeyName(id)
        if (entryName != null) {
            val dynamicString = Localflow.getStringOrNull(entryName)
            if (dynamicString != null) {
                return String.format(delegate.configuration.locales[0], dynamicString, *formatArgs)
            }
        }
        return delegate.getString(id, *formatArgs)
    }

    @Throws(NotFoundException::class)
    override fun getText(id: Int): CharSequence {
        val entryName = getResourceEntryKeyName(id)
        if (entryName != null) {
            val dynamicString = Localflow.getStringOrNull(entryName)
            if (dynamicString != null) {
                return dynamicString
            }
        }
        return delegate.getText(id)
    }

    @Throws(NotFoundException::class)
    override fun getText(id: Int, def: CharSequence): CharSequence {
        val entryName = getResourceEntryKeyName(id)
        if (entryName != null) {
            val dynamicString = Localflow.getStringOrNull(entryName)
            if (dynamicString != null) {
                return dynamicString
            }
        }
        return delegate.getText(id, def)
    }

    /**
     * Map a Resource ID to a dot-separated Locaflow key (e.g. R.string.home_title -> "home.title").
     */
    private fun getResourceEntryKeyName(id: Int): String? {
        return try {
            val typeName = delegate.getResourceTypeName(id)
            if (typeName == "string") {
                val entryName = delegate.getResourceEntryName(id)
                // Convert snake_case from resource entry back to dot notation used by dashboard
                entryName.replace("_", ".")
            } else {
                null
            }
        } catch (e: Resources.NotFoundException) {
            null
        }
    }
}
