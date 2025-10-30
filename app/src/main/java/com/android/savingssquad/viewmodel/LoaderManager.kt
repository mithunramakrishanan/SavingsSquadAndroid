package com.android.savingssquad.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Kotlin Jetpack Compose equivalent of Swift's LoaderManager.
 * Uses Compose's reactive state instead of @Published.
 */
class LoaderManager private constructor() {

    // 🔹 Reactive states (Compose will observe these automatically)
    var isLoading by mutableStateOf(false)
        private set

    var loadingMessage by mutableStateOf("Loading...")
        private set

    // 🔹 Singleton instance
    companion object {
        val shared: LoaderManager by lazy { LoaderManager() }
    }

    // 🔹 Show loader with optional message
    fun showLoader(message: String = "Loading...") {
        loadingMessage = message
        isLoading = true
    }

    // 🔹 Hide loader
    fun hideLoader() {
        isLoading = false
    }
}