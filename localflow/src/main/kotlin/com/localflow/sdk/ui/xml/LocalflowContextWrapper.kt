package com.localflow.sdk.ui.xml

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources

/**
 * Custom [ContextWrapper] that replaces default [Resources] with [LocalflowResources].
 */
class LocalflowContextWrapper(base: Context) : ContextWrapper(base) {

    private val localflowResources: LocalflowResources by lazy {
        LocalflowResources(super.getResources())
    }

    override fun getResources(): Resources {
        return localflowResources
    }
}
