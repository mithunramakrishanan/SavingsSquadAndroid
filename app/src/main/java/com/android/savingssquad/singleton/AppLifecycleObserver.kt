package com.android.savingssquad.singleton

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.android.savingssquad.viewmodel.AppDestination

class AppLifecycleObserver(
    private val onAppForeground: () -> Unit
) : DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) {
        // App comes to foreground (LIKE didBecomeActive)
        onAppForeground()
    }
}

fun handlePendingPayment(
    context: Context,
    navController: NavController
) {
    val payment = UserDefaultsManager.getPendingPayment()

    if (payment != null &&
        navController.currentDestination?.route != AppDestination.PAYMENT_CONFIRMATION.route
    ) {
        navController.navigate(AppDestination.PAYMENT_CONFIRMATION.route)
    }
}

@Composable
fun ObserveAppResume(
    navController: NavController,
    context: Context
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(Unit) {

        val observer = AppLifecycleObserver {
            handlePendingPayment(context, navController)
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}