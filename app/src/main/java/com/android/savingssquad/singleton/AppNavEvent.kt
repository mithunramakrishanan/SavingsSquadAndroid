package com.android.savingssquad.singleton

import kotlinx.coroutines.flow.MutableStateFlow

object AppNavEvent {
    val route = MutableStateFlow<String?>(null)
    val payment = MutableStateFlow<String?>(null)

}