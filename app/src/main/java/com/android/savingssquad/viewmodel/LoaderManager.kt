package com.android.savingssquad.singleton

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.concurrent.atomic.AtomicInteger

class LoaderManager private constructor() {

    /** Whether the loader UI should currently be visible. */
    var isLoading by mutableStateOf(false)
        private set

    /** The message shown alongside the spinner. */
    var loadingMessage by mutableStateOf("Loading...")
        private set

    private val refCount = AtomicInteger(0)
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        val shared: LoaderManager by lazy { LoaderManager() }
        private const val TAG = "LoaderManager"
    }

    /**
     * Call once per operation that should show a loader. Safe to call from
     * any thread. Nested/parallel calls are supported — the loader only
     * disappears once every matching hideLoader() has been called.
     */
    fun showLoader(message: String = "Loading...") {

        val countOld = refCount.updateAndGet { (it - 1).coerceAtLeast(0) }
        if (countOld == 0) {
            runOnMain { isLoading = false }
        }

        val count = refCount.incrementAndGet()
        runOnMain {
            loadingMessage = message
            if (count == 1) {
                isLoading = true
            }
        }
        Log.d(TAG, "🔄 showLoader() -> outstanding=$count : $message")
    }

    /**
     * Call once per matching showLoader(). Safe to call even if the loader
     * was never shown (count is clamped at zero) — this makes it safe to
     * leave a defensive hideLoader() in early-return branches.
     */
    fun hideLoader() {
        val count = refCount.updateAndGet { (it - 1).coerceAtLeast(0) }
        if (count == 0) {
            runOnMain { isLoading = false }
        }
        Log.d(TAG, "✅ hideLoader() -> outstanding=$count")
    }

    /**
     * Immediately clears the loader and resets the internal counter to
     * zero, regardless of how many outstanding showLoader() calls are
     * pending. Use this as a safety net on logout, on tearing down a
     * screen/ViewModel, or after a request timeout — anywhere a "stuck
     * forever" spinner would otherwise be possible.
     */
    fun forceReset() {
        refCount.set(0)
        runOnMain { isLoading = false }
        Log.d(TAG, "♻️ forceReset()")
    }

    /** Debug-only introspection, useful for catching show/hide mismatches. */
    val debugOutstandingCount: Int
        get() = refCount.get()

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }
}