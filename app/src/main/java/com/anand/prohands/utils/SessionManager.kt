package com.anand.prohands.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionManager(context: Context) {
    
    private var prefs: SharedPreferences

    companion object {
        const val PREFS_NAME = "prohands_secure_session"
        const val PREFS_FALLBACK = "prohands_session_fallback"
        const val KEY_TOKEN = "jwt_token"
        const val KEY_USER_ID = "user_id"
    }

    init {
        prefs = try {
            val masterKey = MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e("SessionManager", "Failed to create EncryptedSharedPreferences, falling back to standard", e)
            context.getSharedPreferences(PREFS_FALLBACK, Context.MODE_PRIVATE)
        }
    }

    fun saveAuthToken(token: String?) {
        if (token == null) {
            prefs.edit().remove(KEY_TOKEN).apply()
        } else {
            prefs.edit().putString(KEY_TOKEN, token).apply()
        }
    }

    fun getAuthToken(): String? {
        return try {
            prefs.getString(KEY_TOKEN, null)
        } catch (e: Exception) {
            null
        }
    }

    fun saveUserId(userId: String?) {
        if (userId == null) {
            prefs.edit().remove(KEY_USER_ID).apply()
        } else {
            prefs.edit().putString(KEY_USER_ID, userId).apply()
        }
    }

    fun getUserId(): String? {
        return try {
            prefs.getString(KEY_USER_ID, null)
        } catch (e: Exception) {
            null
        }
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
