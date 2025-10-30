package com.android.savingssquad.viewmodel

import com.android.savingssquad.singleton.AlertType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AlertManager private constructor() {

    companion object {
        val shared = AlertManager()
    }

    // @Published var isShowing = false
    private val _isShowing = MutableStateFlow(false)
    val isShowing: StateFlow<Boolean> = _isShowing

    var title: String = ""
    var message: String = ""
    var primaryButtonTitle: String = "OK"
    var primaryAction: (() -> Unit)? = null
    var secondaryButtonTitle: String? = null
    var secondaryAction: (() -> Unit)? = null
    var alertType: AlertType = AlertType.INFO

    fun showAlert(
        title: String,
        message: String,
        type: AlertType = AlertType.SUCCESS,
        primaryButtonTitle: String = "OK",
        primaryAction: (() -> Unit)? = null,
        secondaryButtonTitle: String? = null,
        secondaryAction: (() -> Unit)? = null
    ) {
        this.title = title
        this.message = message
        this.alertType = type
        this.primaryButtonTitle = primaryButtonTitle
        this.primaryAction = primaryAction
        this.secondaryButtonTitle = secondaryButtonTitle
        this.secondaryAction = secondaryAction
        _isShowing.value = true
    }

    fun hideAlert() {
        _isShowing.value = false
    }
}