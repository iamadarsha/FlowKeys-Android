package com.flowkeys.android.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.flowkeys.android.core.model.Language
import com.flowkeys.android.core.model.ScriptMode
import com.flowkeys.android.modes.SmartMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "flowkeys_preferences")

/**
 * DataStore-based preference manager for FlowKeys settings.
 */
class DataStoreManager(private val context: Context) {

    companion object {
        private val KEY_LANGUAGE = stringPreferencesKey("selected_language")
        private val KEY_SCRIPT_MODE = stringPreferencesKey("script_mode")
        private val KEY_SMART_MODE = stringPreferencesKey("smart_mode")
        private val KEY_CLOUD_ENABLED = booleanPreferencesKey("cloud_enabled")
        private val KEY_GROQ_API_KEY = stringPreferencesKey("groq_api_key")
        private val KEY_GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        private val KEY_TARGET_TRANSLATION = stringPreferencesKey("target_translation_language")
        // Deprecated: contact sync was intentionally removed to eliminate spyware scanner heuristics.
        // READ_CONTACTS is not in the manifest. The key is kept to avoid DataStore migration errors.
        @Deprecated("Contact sync removed for security. Do not reactivate without manifest permission + prominent disclosure.")
        private val KEY_CONTACT_SYNC_ENABLED = booleanPreferencesKey("contact_sync_enabled")
        private val KEY_PROCESSING_MODE = stringPreferencesKey("processing_quality_mode")
        private val KEY_LEARNED_VOCAB_ENABLED = booleanPreferencesKey("learned_vocab_enabled")
        private val KEY_HEDGE_DELAY_MS = intPreferencesKey("hedge_delay_ms")
    }

    enum class ProcessingQualityMode(val id: String, val displayName: String) {
        BEST_QUALITY("best_quality", "Best Quality (Gemini 3.5 Flash Lite)"),
        FAST("fast", "Fast (Low Latency)"),
        OFFLINE("offline", "Offline (100% On-Device)")
    }

    val selectedLanguage: Flow<Language> = context.dataStore.data.map { prefs ->
        val code = prefs[KEY_LANGUAGE] ?: Language.DEFAULT.code
        Language.fromCode(code)
    }

    val scriptMode: Flow<ScriptMode> = context.dataStore.data.map { prefs ->
        val id = prefs[KEY_SCRIPT_MODE] ?: ScriptMode.DEFAULT.id
        ScriptMode.fromId(id)
    }

    val targetTranslationLanguage: Flow<Language?> = context.dataStore.data.map { prefs ->
        val code = prefs[KEY_TARGET_TRANSLATION]
        if (code.isNullOrBlank() || code == "NONE") null else Language.fromCode(code)
    }

    val smartMode: Flow<SmartMode> = context.dataStore.data.map { prefs ->
        val modeStr = prefs[KEY_SMART_MODE] ?: SmartMode.GENERAL.name
        try { SmartMode.valueOf(modeStr) } catch (e: Exception) { SmartMode.GENERAL }
    }

    val processingQualityMode: Flow<ProcessingQualityMode> = context.dataStore.data.map { prefs ->
        val modeStr = prefs[KEY_PROCESSING_MODE] ?: ProcessingQualityMode.BEST_QUALITY.id
        ProcessingQualityMode.entries.find { it.id == modeStr } ?: ProcessingQualityMode.BEST_QUALITY
    }

    val isCloudEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_CLOUD_ENABLED] ?: false
    }

    val groqApiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_GROQ_API_KEY] ?: ""
    }

    val geminiApiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_GEMINI_API_KEY] ?: ""
    }

    val isContactSyncEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_CONTACT_SYNC_ENABLED] ?: false
    }

    val isLearnedVocabEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_LEARNED_VOCAB_ENABLED] ?: true
    }

    val hedgeDelayMs: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_HEDGE_DELAY_MS] ?: 800
    }

    suspend fun setLanguage(language: Language) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LANGUAGE] = language.code
        }
    }

    suspend fun setScriptMode(mode: ScriptMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SCRIPT_MODE] = mode.id
        }
    }

    suspend fun setSmartMode(mode: SmartMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SMART_MODE] = mode.name
        }
    }

    suspend fun setCloudEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CLOUD_ENABLED] = enabled
        }
    }

    suspend fun setGroqApiKey(apiKey: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_GROQ_API_KEY] = apiKey
        }
    }

    suspend fun setGeminiApiKey(apiKey: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_GEMINI_API_KEY] = apiKey
        }
    }

    suspend fun setProcessingQualityMode(mode: ProcessingQualityMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PROCESSING_MODE] = mode.id
        }
    }

    suspend fun setTargetTranslation(language: Language?) {
        context.dataStore.edit { prefs ->
            if (language == null) {
                prefs[KEY_TARGET_TRANSLATION] = "NONE"
            } else {
                prefs[KEY_TARGET_TRANSLATION] = language.code
            }
        }
    }

    suspend fun setContactSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CONTACT_SYNC_ENABLED] = enabled
        }
    }

    suspend fun setLearnedVocabEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LEARNED_VOCAB_ENABLED] = enabled
        }
    }

    suspend fun setHedgeDelayMs(delayMs: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_HEDGE_DELAY_MS] = delayMs
        }
    }
}
