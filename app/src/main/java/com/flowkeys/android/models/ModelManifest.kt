package com.flowkeys.android.models

import com.flowkeys.android.core.model.Language

/**
 * Metadata definition for downloadable on-device model packs.
 */
data class ModelItem(
    val id: String,
    val name: String,
    val description: String,
    val languages: List<Language>,
    val sizeBytes: Long,
    val sha256Checksum: String,
    val modelFileName: String,
    val tokensFileName: String,
    val downloadUrl: String,
    /** Whether this model is ready for download. False = Sherpa JNI not yet integrated. */
    val isAvailable: Boolean = false,
    /** Human-readable status for UI display */
    val statusLabel: String = "Coming Soon"
)

object ModelManifest {

    val INDIC_CONFORMER = ModelItem(
        id = "indic-conformer-int8",
        name = "Indic Conformer (Hindi & Bengali)",
        description = "On-device model for Indian languages. Requires Sherpa-ONNX native integration (planned).",
        languages = listOf(Language.BENGALI, Language.HINDI),
        sizeBytes = 188L * 1024 * 1024, // ~188 MB
        sha256Checksum = "", // Placeholder — will be set when model is finalized
        modelFileName = "indic_conformer_int8.onnx",
        tokensFileName = "indic_tokens.txt",
        downloadUrl = "https://huggingface.co/meetsync/indic-conformer-onnx-sherpa/resolve/main/indic_conformer_int8.onnx",
        isAvailable = false,
        statusLabel = "Planned · Use Cloud ASR"
    )

    val ENGLISH_CORE = ModelItem(
        id = "english-zipformer-int8",
        name = "English Zipformer Core",
        description = "On-device English speech recognition. Requires Sherpa-ONNX native integration (planned).",
        languages = listOf(Language.ENGLISH),
        sizeBytes = 55L * 1024 * 1024, // ~55 MB
        sha256Checksum = "", // Placeholder
        modelFileName = "english_zipformer_int8.onnx",
        tokensFileName = "tokens.txt",
        downloadUrl = "https://k2-fsa.github.io/sherpa/onnx/pretrained_models/zipformer/english_zipformer_int8.onnx",
        isAvailable = false,
        statusLabel = "Planned · Using Android ASR"
    )

    val SILERO_VAD = ModelItem(
        id = "silero-vad-v4",
        name = "Silero VAD v4",
        description = "Ultra-lightweight voice activity detection neural network.",
        languages = Language.entries,
        sizeBytes = 2L * 1024 * 1024, // ~1.8 MB
        sha256Checksum = "", // Placeholder
        modelFileName = "silero_vad.onnx",
        tokensFileName = "",
        downloadUrl = "https://github.com/snakers4/silero-vad/raw/master/files/silero_vad.onnx",
        isAvailable = false,
        statusLabel = "Planned"
    )

    val ALL_MODELS = listOf(INDIC_CONFORMER, ENGLISH_CORE, SILERO_VAD)

    fun getModelForLanguage(language: Language): ModelItem {
        return when (language) {
            Language.BENGALI, Language.HINDI -> INDIC_CONFORMER
            Language.ENGLISH -> ENGLISH_CORE
        }
    }

    /**
     * Current active ASR engine description for UI display.
     */
    fun getActiveAsrDescription(cloudEnabled: Boolean, hasGroqKey: Boolean): String {
        return when {
            cloudEnabled && hasGroqKey -> "Groq Whisper-large-v3-turbo (Cloud)"
            else -> "Android Speech Services (On-Device)"
        }
    }
}

