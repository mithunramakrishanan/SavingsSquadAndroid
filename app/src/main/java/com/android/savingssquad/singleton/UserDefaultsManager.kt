package com.android.savingssquad.singleton

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.android.savingssquad.model.GroupFund
import com.android.savingssquad.model.Login
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
        const val LOGGED_USER = "LOGGEDUSER"
        const val SAVED_GROUP_FUND = "SAVEDCHIT"
        const val SAVED_REMAINDER = "SAVEDREMAINDER"
        const val IS_MANAGER_LOGIN = "ISMANAGERLOGIN"
        const val LOGGED_GROUP_ID = "LOGGEDGROUPID"
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

    fun saveLogin(login: Login) = saveObject(Keys.LOGGED_USER, login)
    fun getLogin(): Login? = getObject(Keys.LOGGED_USER)

    // 🔹 Group Fund
    fun saveGroupFund(groupFund: GroupFund) = saveObject(Keys.SAVED_GROUP_FUND, groupFund)
    fun getGroupFund(): GroupFund? = getObject(Keys.SAVED_GROUP_FUND)
    fun removeGroupFund() = removeObject(Keys.SAVED_GROUP_FUND)

    // 🔹 Remainder
    fun saveRemainder(remainder: RemainderModel) = saveObject(Keys.SAVED_REMAINDER, remainder)
    fun getRemainder(): RemainderModel? = getObject(Keys.SAVED_REMAINDER)
    fun removeRemainder() = removeObject(Keys.SAVED_REMAINDER)

    // 🔹 User Type
    fun saveGroupFundManagerLogged(isManager: Boolean) = saveBool(Keys.IS_MANAGER_LOGIN, isManager)
    fun getGroupFundManagerLogged(): Boolean = getBool(Keys.IS_MANAGER_LOGIN)

    // 🔹 Clear All
    fun clearAll() {
        listOf(
            Keys.IS_LOGGED_IN,
            Keys.LOGGED_USER,
            Keys.SAVED_GROUP_FUND,
            Keys.SAVED_REMAINDER,
            Keys.IS_MANAGER_LOGIN,
            Keys.LOGGED_GROUP_ID
        ).forEach { key -> removeObject(key) }

        Log.d("UserDefaultsManager", "🔄 All user data cleared successfully!")
    }
}