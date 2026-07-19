package com.android.savingssquad.singleton

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.android.savingssquad.model.Squad
import com.android.savingssquad.model.Login
import com.android.savingssquad.model.PaymentsDetails
import com.android.savingssquad.model.RemainderModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


/**
 * UserDefaultsManager - Combined manager that handles SharedPreferences operations.
 * Equivalent to Swift's UserDefaultsManager + Keys + Helper in one place.
 */
object UserDefaultsManager {

    // 🔹 Preferences setup
    private const val PREFS_NAME = "SavingsSquadPrefs"
    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    // 🔹 Keys (like Swift's enum)
    private object Keys {
        const val IS_LOGGED_IN = "ISLOGGEDIN"

        const val IS_FROM_NOTIFICATION_IN = "IS_FROM_NOTIFICATION_IN"

        const val IS_CASH_REQUEST_NOTIFICATION = "IS_CASH_REQUEST_NOTIFICATION"
        const val savedSquadLogins = "SAVEDSQUADLOGINS"

        const val IS_MULTIPLE_ACCOUNT = "ISMULTIPLEACCOUNT"
        const val LOGGED_USER = "LOGGEDUSER"
        const val SAVED_SQUAD = "SAVEDCHIT"
        const val SAVED_REMAINDER = "SAVEDREMAINDER"
        const val IS_MANAGER_LOGIN = "ISMANAGERLOGIN"
        const val LOGGED_GROUP_ID = "LOGGEDGROUPID"

        const val PENDING_PAYMENT = "PENDING_PAYMENT"
    }

    /**
     * Initialize this once in Application.onCreate()
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Ensure prefs is initialized before use
    private fun ensureInitialized() {
        if (!::prefs.isInitialized) {
            throw IllegalStateException("⚠️ UserDefaultsManager not initialized. Call init(context) in Application.onCreate()!")
        }
    }

    // 🔹 Save Boolean
    private fun saveBool(key: String, value: Boolean) {
        ensureInitialized()
        prefs.edit().putBoolean(key, value).apply()
    }

    // 🔹 Get Boolean
    private fun getBool(key: String): Boolean {
        ensureInitialized()
        return prefs.getBoolean(key, false)
    }

    // 🔹 Save any Serializable (Codable) object
    private fun <T> saveObject(key: String, value: T) {
        ensureInitialized()
        try {
            val json = gson.toJson(value)
            prefs.edit().putString(key, json).apply()
            Log.d("UserDefaultsManager", "✅ Object saved: $key")
        } catch (e: Exception) {
            Log.e("UserDefaultsManager", "❌ Error encoding $key: ${e.localizedMessage}")
        }
    }

    // 🔹 Retrieve any Serializable (Codable) object
    private inline fun <reified T> getObject(key: String): T? {
        ensureInitialized()
        val json = prefs.getString(key, null) ?: return null
        return try {
            gson.fromJson<T>(json, object : TypeToken<T>() {}.type)
        } catch (e: Exception) {
            Log.e("UserDefaultsManager", "❌ Error decoding $key: ${e.localizedMessage}")
            null
        }
    }

    // 🔹 Remove an object
    private fun removeObject(key: String) {
        ensureInitialized()
        prefs.edit().remove(key).apply()
    }

    // -----------------------------------------------------------
    // MARK: - Public Helper Methods (Same as Swift extension)
    // -----------------------------------------------------------

    // 🔹 Login
    fun saveIsLoggedIn(isLoggedIn: Boolean) = saveBool(Keys.IS_LOGGED_IN, isLoggedIn)
    fun getIsLoggedIn(): Boolean = getBool(Keys.IS_LOGGED_IN)

    fun saveIsMultipleAccount(isMultipleAccount: Boolean) = saveBool(Keys.IS_MULTIPLE_ACCOUNT, isMultipleAccount)
    fun getIsMultipleAccount(): Boolean = getBool(Keys.IS_MULTIPLE_ACCOUNT)

    fun saveLogin(login: Login) = saveObject(Keys.LOGGED_USER, login)
    fun getLogin(): Login? = getObject(Keys.LOGGED_USER)

    fun saveIsFromnotification(isLoggedIn: Boolean) = saveBool(Keys.IS_FROM_NOTIFICATION_IN, isLoggedIn)
    fun getIsFromnotification(): Boolean = getBool(Keys.IS_FROM_NOTIFICATION_IN)

    // 🔹 Squad
    fun saveSquad(squad: Squad) = saveObject(Keys.SAVED_SQUAD, squad)
    fun getSquad(): Squad? = getObject(Keys.SAVED_SQUAD)
    fun removeSquad() = removeObject(Keys.SAVED_SQUAD)

    // 🔹 Remainder
    fun saveRemainder(remainder: RemainderModel) = saveObject(Keys.SAVED_REMAINDER, remainder)
    fun getRemainder(): RemainderModel? = getObject(Keys.SAVED_REMAINDER)
    fun removeRemainder() = removeObject(Keys.SAVED_REMAINDER)

    // 🔹 User Type
    fun saveSquadManagerLogged(isManager: Boolean) = saveBool(Keys.IS_MANAGER_LOGIN, isManager)
    fun getSquadManagerLogged(): Boolean = getBool(Keys.IS_MANAGER_LOGIN)

    fun savePendingPayment(payment: PaymentsDetails) {
        saveObject(Keys.PENDING_PAYMENT, payment)
    }

    fun getPendingPayment(): PaymentsDetails? {
        return getObject(Keys.PENDING_PAYMENT)
    }

    fun clearPendingPayment() {
        removeObject(Keys.PENDING_PAYMENT)
    }


    fun saveSquadLogins(logins: List<Login>) {
        saveObject(Keys.savedSquadLogins, logins)
    }

    fun getSquadLogins(): List<Login> {
        return getObject(Keys.savedSquadLogins) ?: emptyList()
    }

    fun saveIsCashReqNotification(isManager: Boolean) = saveBool(Keys.IS_CASH_REQUEST_NOTIFICATION, isManager)
    fun getIsCashReqnotification(): Boolean = getBool(Keys.IS_CASH_REQUEST_NOTIFICATION)

    // 🔹 Clear All
    fun clearAll() {
        listOf(
            Keys.IS_FROM_NOTIFICATION_IN,
            Keys.IS_LOGGED_IN,
            Keys.LOGGED_USER,
            Keys.SAVED_SQUAD,
            Keys.SAVED_REMAINDER,
            Keys.IS_MANAGER_LOGIN,
            Keys.LOGGED_GROUP_ID,
            Keys.IS_MULTIPLE_ACCOUNT,
            Keys.PENDING_PAYMENT,
            Keys.IS_CASH_REQUEST_NOTIFICATION,
            Keys.savedSquadLogins
        ).forEach { key -> removeObject(key) }

        Log.d("UserDefaultsManager", "🔄 All user data cleared successfully!")
    }
}