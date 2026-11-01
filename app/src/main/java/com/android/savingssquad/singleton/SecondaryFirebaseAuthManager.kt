package com.android.savingssquad.singleton

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit
import com.google.firebase.FirebaseException

class SecondaryFirebaseAuthManager private constructor(
    context: Context
) {

    companion object {

        private const val TAG = "SecondaryFirebaseAuth"
        private const val APP_NAME = "SavingsSquadMemberAuth"

        @Volatile
        private var INSTANCE: SecondaryFirebaseAuthManager? = null

        fun getInstance(context: Context): SecondaryFirebaseAuthManager {

            return INSTANCE ?: synchronized(this) {

                INSTANCE ?: SecondaryFirebaseAuthManager(
                    context.applicationContext
                ).also {
                    INSTANCE = it
                }
            }
        }
    }

    private val secondaryAuth: FirebaseAuth

    init {

        // Check whether secondary Firebase app already exists.
        val existingApp = FirebaseApp.getApps(context)
            .firstOrNull { it.name == APP_NAME }

        val secondaryApp = if (existingApp != null) {

            Log.d(TAG, "Secondary Firebase app already configured")

            existingApp

        } else {

            // Get default Firebase configuration.
            val defaultApp = FirebaseApp.getInstance()

            // Configure secondary Firebase app.
            FirebaseApp.initializeApp(
                context,
                defaultApp.options,
                APP_NAME
            ) ?: throw IllegalStateException(
                "Failed to configure secondary Firebase app."
            )
        }

        secondaryAuth = FirebaseAuth.getInstance(secondaryApp)

        Log.d(TAG, "Secondary Firebase Auth initialized successfully")
    }

    // MARK: - Send OTP

    interface SendOTPCallback {

        fun onCodeSent(verificationId: String)

        fun onAutoVerified(firebaseUID: String)

        fun onFailure(error: String)
    }

    fun sendOTP(
        activity: Activity,
        phoneNumber: String,
        callback: SendOTPCallback
    ) {

        val options = PhoneAuthOptions.newBuilder(secondaryAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(
                object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                    override fun onVerificationCompleted(
                        credential: PhoneAuthCredential
                    ) {

                        Log.d(TAG, "OTP automatically verified")

                        // Sign in using secondary Firebase Auth.
                        signInWithCredential(
                            credential = credential,
                            expectedPhoneNumber = phoneNumber
                        ) { result ->

                            result.onSuccess { firebaseUID ->
                                callback.onAutoVerified(firebaseUID)
                            }

                            result.onFailure { error ->
                                callback.onFailure(
                                    error.localizedMessage
                                        ?: "Automatic OTP verification failed."
                                )
                            }
                        }
                    }

                    override fun onVerificationFailed(
                        exception: FirebaseException
                    ) {
                        Log.e(TAG, "OTP verification failed", exception)

                        callback.onFailure(
                            exception.localizedMessage ?: "Unable to send OTP."
                        )
                    }

                    override fun onCodeSent(
                        verificationId: String,
                        token: PhoneAuthProvider.ForceResendingToken
                    ) {

                        Log.d(TAG, "OTP sent successfully")

                        callback.onCodeSent(verificationId)
                    }
                }
            )
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    // MARK: - Verify OTP

    fun verifyOTP(
        verificationId: String,
        otp: String,
        expectedPhoneNumber: String,
        callback: (Result<String>) -> Unit
    ) {

        val credential = PhoneAuthProvider.getCredential(
            verificationId,
            otp
        )

        signInWithCredential(
            credential = credential,
            expectedPhoneNumber = expectedPhoneNumber,
            callback = callback
        )
    }

    // MARK: - Sign In With Credential

    private fun signInWithCredential(
        credential: PhoneAuthCredential,
        expectedPhoneNumber: String,
        callback: (Result<String>) -> Unit
    ) {

        secondaryAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->

                if (!task.isSuccessful) {

                    val error = task.exception

                    Log.e(
                        TAG,
                        "Secondary Firebase sign-in failed",
                        error
                    )

                    callback(
                        Result.failure(
                            error ?: Exception(
                                "Firebase authentication failed."
                            )
                        )
                    )

                    return@addOnCompleteListener
                }

                val user = task.result?.user

                if (user == null) {

                    callback(
                        Result.failure(
                            Exception("Unable to retrieve Firebase user.")
                        )
                    )

                    return@addOnCompleteListener
                }

                // Verify expected phone number.
                if (user.phoneNumber != expectedPhoneNumber) {

                    Log.e(TAG, "Verified phone number does not match")

                    secondaryAuth.signOut()

                    callback(
                        Result.failure(
                            Exception(
                                "Verified phone number does not match."
                            )
                        )
                    )

                    return@addOnCompleteListener
                }

                Log.d(TAG, "Secondary Firebase OTP verified")
                Log.d(TAG, "Verified Firebase UID: ${user.uid}")

                callback(
                    Result.success(user.uid)
                )
            }
    }

    // MARK: - Sign Out

    fun signOut() {

        try {

            secondaryAuth.signOut()

            Log.d(
                TAG,
                "Secondary Firebase Auth signed out"
            )

        } catch (exception: Exception) {

            Log.e(
                TAG,
                "Secondary Firebase sign out failed",
                exception
            )
        }
    }

    // MARK: - Current Secondary User

    fun getCurrentUserUID(): String? {
        return secondaryAuth.currentUser?.uid
    }
}