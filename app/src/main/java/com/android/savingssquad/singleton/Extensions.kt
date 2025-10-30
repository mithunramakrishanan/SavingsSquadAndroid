package com.android.savingssquad.singleton

import android.content.Context
import android.content.SharedPreferences
import com.android.savingssquad.model.GroupFund
import com.android.savingssquad.model.Login
import com.android.savingssquad.model.RemainderModel
import com.google.gson.Gson
import com.google.firebase.Timestamp
import java.util.Date
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import java.text.NumberFormat
import java.util.Locale

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Convert Date to Firebase Timestamp
val Date.asTimestamp: Timestamp
    get() = Timestamp(this)

// Optional Timestamp to Date or now
val Timestamp?.orNow: Date
    get() = this?.toDate() ?: Date()

fun Color.Companion.fromHex(hex: String): Color {
    val cleanHex = hex.replace(Regex("[^A-Fa-f0-9]"), "")
    val colorInt = when (cleanHex.length) {
        6 -> cleanHex.toLong(16) or 0xFF000000
        else -> 0xFF000000
    }
    return Color(colorInt.toInt())
}

fun hideKeyboard(focusManager: androidx.compose.ui.focus.FocusManager) {
    focusManager.clearFocus(force = true)
}


fun Int.currencyFormattedWithCommas(symbol: String = "₹ "): String {
    val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
    return "$symbol${formatter.format(this)}"
}

object JsonUtil {
    private val gson = com.google.gson.Gson()
    fun <T> fromJson(json: String, clazz: Class<T>): T = gson.fromJson(json, clazz)
}