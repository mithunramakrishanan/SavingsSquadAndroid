package com.android.savingssquad.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Kotlin Jetpack Compose equivalent of Swift's LoaderManager.
 * Uses Compose's reactive state instead of @Published.
 */
class LoaderManager private constructor() {

    var isLoading by mutableStateOf(false)
        private set

    var loadingMessage by mutableStateOf("Loading...")
        private set

    companion object {
        val shared: LoaderManager by lazy { LoaderManager() }
    }

    fun showLoader(message: String = "Loading...") {
        loadingMessage = message
        isLoading = true
        Log.d("LoaderManager", "🔄 Loader Started: $message")
    }

    fun hideLoader() {
        isLoading = false
        Log.d("LoaderManager", "✅ Loader Hidden")
    }
}