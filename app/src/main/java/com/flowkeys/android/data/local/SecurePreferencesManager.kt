package com.flowkeys.android.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Secure storage manager for API keys and sensitive tokens.
 * Uses EncryptedSharedPreferences backed by Android Keystore hardware with AES-256-GCM.
 */
class SecurePreferencesManager private constructor(context: Context) {

    companion object {
        private const val PREFS_NAME = "flowkeys_secure_vault"
        private const val KEY_GROQ_API = "sec_groq_api_key"
        private const val KEY_GEMINI_API = "sec_gemini_api_key"

        @Volatile
        private var INSTANCE: SecurePreferencesManager? = null

        fun getInstance(context: Context): SecurePreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecurePreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val sharedPreferences: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
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
            // Fallback for emulators/environments with restricted KeyStore implementations
            context.getSharedPreferences(PREFS_NAME + "_compat", Context.MODE_PRIVATE)
        }
    }

    private val _groqApiKeyFlow = MutableStateFlow(getGroqApiKey())
    val groqApiKeyFlow: StateFlow<String> = _groqApiKeyFlow.asStateFlow()

    private val _geminiApiKeyFlow = MutableStateFlow(getGeminiApiKey())
    val geminiApiKeyFlow: StateFlow<String> = _geminiApiKeyFlow.asStateFlow()

    fun getGroqApiKey(): String {
        return try {
            sharedPreferences.getString(KEY_GROQ_API, "") ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    fun setGroqApiKey(apiKey: String) {
        try {
            sharedPreferences.edit().putString(KEY_GROQ_API, apiKey).apply()
            _groqApiKeyFlow.value = apiKey
        } catch (ignored: Exception) {}
    }

    fun getGeminiApiKey(): String {
        return try {
            sharedPreferences.getString(KEY_GEMINI_API, "") ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    fun setGeminiApiKey(apiKey: String) {
        try {
            sharedPreferences.edit().putString(KEY_GEMINI_API, apiKey).apply()
            _geminiApiKeyFlow.value = apiKey
        } catch (ignored: Exception) {}
    }
}
