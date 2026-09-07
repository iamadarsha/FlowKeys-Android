package com.flowkeys.android.polish.translation

import android.util.Log
import com.flowkeys.android.core.model.Language
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

/**
 * Google ML Kit On-Device Neural Translation Provider.
 * Provides 100% offline, private, zero-key neural machine translation between:
 * - Bengali <-> English
 * - Hindi <-> English
 * - Bengali <-> Hindi (via direct ML Kit translator client)
 */
object MLKitTranslationProvider {

    private const val TAG = "MLKitTranslate"
    private val translatorCache = ConcurrentHashMap<String, Translator>()

    private fun mapLanguageToMLKit(language: Language): String {
        return when (language) {
            Language.BENGALI -> TranslateLanguage.BENGALI
            Language.HINDI -> TranslateLanguage.HINDI
            Language.ENGLISH -> TranslateLanguage.ENGLISH
        }
    }

    private fun getTranslator(sourceLang: Language, targetLang: Language): Translator {
        val key = "${sourceLang.code}_to_${targetLang.code}"
        return translatorCache.getOrPut(key) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(mapLanguageToMLKit(sourceLang))
                .setTargetLanguage(mapLanguageToMLKit(targetLang))
                .build()
            Translation.getClient(options)
        }
    }

    suspend fun translate(
        text: String,
        sourceLang: Language,
        targetLang: Language
    ): String? = withContext(Dispatchers.IO) {
        if (text.isBlank() || sourceLang == targetLang) return@withContext text

        try {
            val translator = try { getTranslator(sourceLang, targetLang) } catch (t: Throwable) { return@withContext null }
            
            // Ensure model is available on device (downloads if connected, or uses local cached model)
            val conditions = DownloadConditions.Builder().build()
            val downloadOk = suspendCancellableCoroutine<Boolean> { cont ->
                try {
                    translator.downloadModelIfNeeded(conditions)
                        .addOnSuccessListener { cont.resume(true) }
                        .addOnFailureListener { e ->
                            cont.resume(false)
                        }
                } catch (t: Throwable) {
                    cont.resume(false)
                }
            }

            if (!downloadOk) {
                return@withContext null
            }

            // Perform translation
            val translated = suspendCancellableCoroutine<String?> { cont ->
                try {
                    translator.translate(text)
                        .addOnSuccessListener { result -> cont.resume(result) }
                        .addOnFailureListener { e ->
                            cont.resume(null)
                        }
                } catch (t: Throwable) {
                    cont.resume(null)
                }
            }

            return@withContext translated?.takeIf { it.isNotBlank() }
        } catch (t: Throwable) {
            return@withContext null
        }
    }
}
