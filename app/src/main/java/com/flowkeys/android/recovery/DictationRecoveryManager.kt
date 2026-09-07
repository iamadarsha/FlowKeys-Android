package com.flowkeys.android.recovery

import com.flowkeys.android.core.model.Language
import java.util.concurrent.atomic.AtomicReference

/**
 * Durable in-memory buffer guaranteeing that no dictation is ever lost
 * if an accessibility node detaches, app switches, or system insertion fails.
 */
object DictationRecoveryManager {

    data class RecoverableDictation(
        val text: String,
        val language: Language,
        val packageName: String,
        val reason: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val latestRecoverable = AtomicReference<RecoverableDictation?>(null)

    fun saveRecoverable(text: String, language: Language, packageName: String, reason: String) {
        if (text.isBlank()) return
        latestRecoverable.set(
            RecoverableDictation(
                text = text,
                language = language,
                packageName = packageName,
                reason = reason
            )
        )
    }

    fun getLatestRecoverable(): RecoverableDictation? = latestRecoverable.get()

    fun clear() {
        latestRecoverable.set(null)
    }
}
