package com.android.savingssquad.view
import android.app.Application
import android.util.Log
import com.android.savingssquad.singleton.AppContext
import com.android.savingssquad.singleton.LocalDatabase
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.viewmodel.FirebaseFunctionsManager
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.initialize
import com.google.firebase.messaging.FirebaseMessaging

class SavingsSquadApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Init Firebase
        FirebaseApp.initializeApp(this)
        AppContext.init(this)
        // Enable App Check Debug
        Firebase.initialize(context = this)
        Firebase.appCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance(),
        )


        // Logs appear automatically when app runs (no manual token API exists)
        Log.d("AppCheck", "Debug provider enabled. Token will print automatically on first request.")
        FirebaseApp.getInstance().options.projectId?.let { Log.d("PROJECT", it) };
        //b8976f60-a7b2-4b6b-8039-d5ddccd032e1

        // Init functions + local managers
        FirebaseFunctionsManager.shared.init(this)
        UserDefaultsManager.init(this)

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) return@addOnCompleteListener

            val token = task.result
            Log.d("FCM_TOKEN", token)

            // Save to backend
        }

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