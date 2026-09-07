package com.flowkeys.android.core.model

import android.graphics.Rect

/**
 * Lifecycle states of the FlowKeys dictation experience.
 */
sealed interface DictationState {

    /**
     * No editable field focused. Floating pill is either hidden or docked to the margin.
     */
    data object Dormant : DictationState

    /**
     * An editable text field is active and focused.
     * The pill is positioned near the field or above the keyboard.
     */
    data class FieldFocused(
        val packageName: String,
        val fieldBounds: Rect,
        val selectedLanguage: Language,
        val existingTextLength: Int = 0,
        val imeTop: Int? = null
    ) : DictationState

    /**
     * Microphone is actively recording audio.
     */
    data class Recording(
        val durationMs: Long = 0L,
        val audioLevel: Float = 0f, // 0.0 to 1.0 for waveform animation
        val language: Language
    ) : DictationState

    /**
     * Audio capture completed; ASR inference and text polishing in progress.
     */
    data class Processing(
        val language: Language,
        val message: String = "Processing…"
    ) : DictationState

    /**
     * Text was successfully inserted into the target field.
     */
    data class Success(
        val insertedText: String,
        val language: Language
    ) : DictationState

    /**
     * A failure occurred during recording, recognition, or insertion.
     */
    data class Error(
        val errorMessage: String,
        val recoverableText: String? = null,
        val canRetry: Boolean = true
    ) : DictationState
}
