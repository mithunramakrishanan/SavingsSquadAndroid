package com.android.savingssquad.view
import android.app.Application
import android.util.Log
import com.android.savingssquad.singleton.LocalDatabase
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.viewmodel.FirebaseFunctionsManager
import com.google.firebase.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

class SavingsSquadApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Init Firebase
        FirebaseApp.initializeApp(this)

        // Enable App Check Debug
        val providerFactory = DebugAppCheckProviderFactory.getInstance()
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(providerFactory)

        // Logs appear automatically when app runs (no manual token API exists)
        Log.d("AppCheck", "Debug provider enabled. Token will print automatically on first request.")

        //b8976f60-a7b2-4b6b-8039-d5ddccd032e1

        // Init functions + local managers
        FirebaseFunctionsManager.shared.init(this)
        UserDefaultsManager.init(this)

        val db = LocalDatabase.getInstance(this)

        Thread {
            try {
                // Fetch saved orders from local database
                val savedOrders = db.fetchOrders()
                Log.d("LocalDatabase", "Saved orders: $savedOrders")

                // Only call Firebase function if there are orders
                if (savedOrders.isNotEmpty()) {
                    FirebaseFunctionsManager.shared.verifyBulkOrders(savedOrders,this)
                } else {
                    Log.d("LocalDatabase", "No orders to verify.")
                }
            } catch (e: Exception) {
                Log.e("LocalDatabase", "❌ Failed to fetch orders: ${e.localizedMessage}")
            }
        }.start()


    }
}