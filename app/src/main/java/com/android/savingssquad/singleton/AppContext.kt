package com.android.savingssquad.singleton

import android.content.Context

object AppContext {
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun get(): Context {
        return appContext
            ?: throw IllegalStateException("AppContext not initialized")
    }
}