package com.flowkeys.android.asr

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.flowkeys.android.core.coordinator.FlowKeysCoordinator
import com.flowkeys.android.core.model.Language
import com.flowkeys.android.core.model.ScriptMode
import com.flowkeys.android.dictionary.PersonalDictionary
import com.flowkeys.android.polish.MicroPolisher
import com.flowkeys.android.polish.transliteration.BengaliLatinTransliterator
import com.flowkeys.android.polish.transliteration.HindiLatinTransliterator
import com.flowkeys.android.providers.GroqSpeechProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * High-performance speech recognition engine leveraging Android's native SpeechRecognizer
 * and hybrid edge-cloud acceleration.
 *
 * Capabilities:
 * - Dynamic sub-50ms live cursor token preview streaming (onPartialResults)
 * - Tri-Mode Script Engine (Translated English, Native Indic Script, Phonetic Latin Benglish/Hinglish)
 * - Auto-Learning Personal Vocabulary & Contact proper noun normalization
 * - Real-time audio waveform volume reporting (via onRmsChanged)
 * - Instant linguistic micro-polishing & translation before cursor injection
 */
class SpeechRecognitionManager(private val context: Context) {

    interface Listener {
        fun onSpeechStart()
        fun onRmsChanged(audioLevel: Float)
        fun onSpeechResult(polishedText: String)
        fun onSpeechError(errorMessage: String)
    }

    var listener: Listener? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var recognizer: SpeechRecognizer? = null
    private var isListening = false
    private var activeLanguage = Language.DEFAULT
    private var startTimeMs = 0L
    private var watchdogRunnable: Runnable? = null

    private var cloudEnabled: Boolean = false
    private var groqApiKey: String? = null
    private var geminiApiKey: String? = null
    private var qualityMode = com.flowkeys.android.data.DataStoreManager.ProcessingQualityMode.BEST_QUALITY

    init {
        val dataStoreManager = com.flowkeys.android.data.DataStoreManager(context)
        scope.launch {
            dataStoreManager.isCloudEnabled.collect { enabled ->
                cloudEnabled = enabled
            }
        }
        scope.launch {
            dataStoreManager.groqApiKey.collect { key ->
                groqApiKey = key
            }
        }
        scope.launch {
            dataStoreManager.geminiApiKey.collect { key ->
                geminiApiKey = key
            }
        }
        scope.launch {
            dataStoreManager.processingQualityMode.collect { mode ->
                qualityMode = mode
            }
        }
    }

    private var audioRecord: AudioRecord? = null
    private var audioBuffer = mutableListOf<Float>()
    private var isRecordingAudio = false
    private var audioRecordJob: kotlinx.coroutines.Job? = null

    fun updateCloudSettings(enabled: Boolean, apiKey: String?, geminiKey: String? = null) {
        cloudEnabled = enabled
        groqApiKey = apiKey
        if (geminiKey != null) geminiApiKey = geminiKey
    }

    private fun startAudioCapture() {
        if (!cloudEnabled || (groqApiKey.isNullOrBlank() && geminiApiKey.isNullOrBlank())) return
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return

        try {
            val sampleRate = 16000
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            if (minBufferSize <= 0) return
            
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                channelConfig,
                audioFormat,
                minBufferSize * 2
            )
            
            audioBuffer.clear()
            isRecordingAudio = true
            audioRecord?.startRecording()
            
            audioRecordJob = scope.launch(Dispatchers.IO) {
                val shortBuffer = ShortArray(minBufferSize)
                while (isRecordingAudio) {
                    val read = audioRecord?.read(shortBuffer, 0, shortBuffer.size) ?: 0
                    if (read > 0) {
                        for (i in 0 until read) {
                            audioBuffer.add(shortBuffer[i].toFloat() / Short.MAX_VALUE)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            isRecordingAudio = false
            audioRecord = null
        }
    }

    private fun stopAudioCapture() {
        isRecordingAudio = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioRecord = null
        audioRecordJob?.cancel()
        audioRecordJob = null
    }

    fun startListening(language: Language) {
        mainHandler.post {
            cancelWatchdog()
            if (isListening) {
                stopListening()
                return@post
            }

            activeLanguage = language
            startTimeMs = System.currentTimeMillis()

            // Immediate tactile & visual feedback: pill pulses into recording state
            FlowKeysCoordinator.updateRecordingLevel(0.15f, 0L)
            FlowKeysCoordinator.onDictationStarted()

            try {
                destroyRecognizer()
                startAudioCapture()
                recognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.speechLocaleTag)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language.speechLocaleTag)
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, language.speechLocaleTag)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                }

                recognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                        FlowKeysCoordinator.updateRecordingLevel(0.2f, 0L)
                        FlowKeysCoordinator.onDictationStarted()
                        listener?.onSpeechStart()
                    }

                    override fun onBeginningOfSpeech() {
                        isListening = true
                        FlowKeysCoordinator.onDictationStarted()
                        listener?.onSpeechStart()
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.08f, 1f)
                        val duration = System.currentTimeMillis() - startTimeMs
                        FlowKeysCoordinator.updateRecordingLevel(normalized, duration)
                        listener?.onRmsChanged(normalized)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        stopAudioCapture()
                        FlowKeysCoordinator.setProcessing("Polishing speech…")
                        armWatchdog(20000L)
                    }

                    override fun onError(error: Int) {
                        cancelWatchdog()
                        stopAudioCapture()
                        isListening = false
                        val errorMsg = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                            SpeechRecognizer.ERROR_NETWORK,
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition network timeout"
                            else -> "Recognition error ($error)"
                        }

                        if (error == SpeechRecognizer.ERROR_NO_MATCH ||
                            error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                            error == SpeechRecognizer.ERROR_CLIENT) {
                            FlowKeysCoordinator.reset()
                        } else {
                            FlowKeysCoordinator.onError(errorMsg)
                        }
                        listener?.onSpeechError(errorMsg)
                        destroyRecognizer()
                    }

                    override fun onResults(results: Bundle?) {
                        cancelWatchdog()
                        stopAudioCapture()
                        isListening = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val rawText = matches?.firstOrNull() ?: ""

                        if (rawText.isBlank() && (!cloudEnabled || audioBuffer.isEmpty())) {
                            FlowKeysCoordinator.reset()
                            destroyRecognizer()
                            return
                        }

                        scope.launch {
                            var finalTextToProcess = rawText
                            val dataStore = com.flowkeys.android.data.DataStoreManager(context)
                            val currentKey = if (!groqApiKey.isNullOrBlank()) groqApiKey else dataStore.groqApiKey.first()
                            val currentGeminiKey = if (!geminiApiKey.isNullOrBlank()) geminiApiKey else dataStore.geminiApiKey.first()
                            val isCloud = (cloudEnabled || dataStore.isCloudEnabled.first()) && qualityMode != com.flowkeys.android.data.DataStoreManager.ProcessingQualityMode.OFFLINE
                            val targetLang = FlowKeysCoordinator.targetTranslationLanguage.value 
                                ?: dataStore.targetTranslationLanguage.first()
                            val currentScriptMode = FlowKeysCoordinator.scriptMode.value
                            val currentSmartMode = dataStore.smartMode.first()

                            android.util.Log.d("SpeechRecognitionManager", "Raw Android ASR: '$rawText', activeLang: $activeLanguage, isCloud: $isCloud, scriptMode: $currentScriptMode, targetLang: $targetLang, qualityMode: $qualityMode")

                            // Cloud Multimodal ASR: Prefer Gemini 2.0 Flash in Best Quality, then Groq Whisper
                            if (isCloud && audioBuffer.isNotEmpty()) {
                                val floatArray = audioBuffer.toFloatArray()
                                val wavBytes = AudioEncoder.encodeToWav(floatArray)

                                if (!currentGeminiKey.isNullOrBlank() && qualityMode == com.flowkeys.android.data.DataStoreManager.ProcessingQualityMode.BEST_QUALITY) {
                                    FlowKeysCoordinator.setProcessing("Gemini transcribing…")
                                    val geminiResult = com.flowkeys.android.providers.GeminiCloudProvider.transcribe(wavBytes, activeLanguage, currentGeminiKey)
                                    android.util.Log.d("SpeechRecognitionManager", "Gemini ASR Result: '$geminiResult'")
                                    if (!geminiResult.isNullOrBlank()) {
                                        finalTextToProcess = geminiResult
                                    }
                                }

                                if (finalTextToProcess == rawText && !currentKey.isNullOrBlank()) {
                                    FlowKeysCoordinator.setProcessing("Groq transcribing…")
                                    val groqProvider = GroqSpeechProvider(currentKey)
                                    val groqResult = groqProvider.transcribe(wavBytes, activeLanguage)
                                    android.util.Log.d("SpeechRecognitionManager", "Groq ASR Result: '$groqResult'")
                                    if (!groqResult.isNullOrBlank()) {
                                        finalTextToProcess = groqResult
                                    }
                                }
                            }
                            
                            if (finalTextToProcess.isBlank()) {
                                FlowKeysCoordinator.reset()
                                destroyRecognizer()
                                return@launch
                            }

                            // 1. Linguistic Micro-Polisher (Kolkata Bengali, Hindi, English, Voice Commands & Context Formatting)
                            val currentContext = FlowKeysCoordinator.activeDictationContext.value
                            val polished = MicroPolisher.polish(finalTextToProcess, activeLanguage, currentContext)
                            
                            // 2. Personal Vocabulary & Contact Proper Noun Normalization
                            val vocabularyApplied = PersonalDictionary.applyVocabulary(polished)
                            android.util.Log.d("SpeechRecognitionManager", "Vocabulary Applied text: '$vocabularyApplied'")

                            // 3. Tri-Mode Script Engine & Direct Translation
                            val finalText = if (targetLang != null && targetLang != activeLanguage) {
                                FlowKeysCoordinator.setProcessing("Translating to ${targetLang.displayName}…")
                                com.flowkeys.android.polish.translation.TranslationEngine.translate(
                                    vocabularyApplied,
                                    activeLanguage,
                                    targetLang,
                                    apiKey = if (isCloud) currentKey else null,
                                    geminiKey = if (isCloud) currentGeminiKey else null,
                                    smartMode = currentSmartMode
                                )
                            } else {
                                when (currentScriptMode) {
                                    ScriptMode.TRANSLATED_ENGLISH -> {
                                        if (activeLanguage != Language.ENGLISH) {
                                            FlowKeysCoordinator.setProcessing("Translating to English…")
                                            com.flowkeys.android.polish.translation.TranslationEngine.translate(
                                                vocabularyApplied,
                                                activeLanguage,
                                                Language.ENGLISH,
                                                apiKey = if (isCloud) currentKey else null,
                                                geminiKey = if (isCloud) currentGeminiKey else null,
                                                smartMode = currentSmartMode
                                            )
                                        } else {
                                            vocabularyApplied
                                        }
                                    }
                                    ScriptMode.NATIVE_SCRIPT -> {
                                        if (isCloud && !currentGeminiKey.isNullOrBlank() && qualityMode == com.flowkeys.android.data.DataStoreManager.ProcessingQualityMode.BEST_QUALITY) {
                                            val geminiPolished = com.flowkeys.android.providers.GeminiCloudProvider.translateOrPolish(
                                                vocabularyApplied,
                                                activeLanguage,
                                                activeLanguage,
                                                currentGeminiKey,
                                                currentSmartMode
                                            )
                                            if (!geminiPolished.isNullOrBlank()) geminiPolished else vocabularyApplied
                                        } else {
                                            vocabularyApplied
                                        }
                                    }
                                    ScriptMode.PHONETIC_LATIN -> {
                                        when (activeLanguage) {
                                            Language.BENGALI -> BengaliLatinTransliterator.transliterate(vocabularyApplied)
                                            Language.HINDI -> HindiLatinTransliterator.transliterate(vocabularyApplied)
                                            Language.ENGLISH -> vocabularyApplied
                                        }
                                    }
                                }
                            }

                            android.util.Log.i("SpeechRecognitionManager", "Final text delivered: '$finalText'")

                            // 4. Deliver text to active field via coordinator
                            listener?.onSpeechResult(finalText)
                            FlowKeysCoordinator.onTextReady(finalText, context)
                            destroyRecognizer()
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partialText = matches?.firstOrNull()
                        if (!partialText.isNullOrBlank()) {
                            FlowKeysCoordinator.onPartialTextAvailable(partialText)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                recognizer?.startListening(intent)
                isListening = true
            } catch (e: Exception) {
                isListening = false
                stopAudioCapture()
                FlowKeysCoordinator.onError("Could not start speech engine: ${e.localizedMessage}")
                listener?.onSpeechError(e.localizedMessage ?: "Start error")
                destroyRecognizer()
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            if (!isListening) return@post
            isListening = false
            stopAudioCapture()
            FlowKeysCoordinator.setProcessing("Polishing speech…")
            armWatchdog(20000L)
            try {
                recognizer?.stopListening()
            } catch (e: Exception) {
                destroyRecognizer()
            }
        }
    }

    fun cancel() {
        mainHandler.post {
            cancelWatchdog()
            isListening = false
            stopAudioCapture()
            try {
                recognizer?.cancel()
            } catch (ignored: Exception) {}
            destroyRecognizer()
        }
    }

    private fun armWatchdog(timeoutMs: Long) {
        cancelWatchdog()
        val runnable = Runnable {
            val state = FlowKeysCoordinator.dictationState.value
            if (state is com.flowkeys.android.core.model.DictationState.Processing) {
                FlowKeysCoordinator.reset()
                destroyRecognizer()
            }
        }
        watchdogRunnable = runnable
        mainHandler.postDelayed(runnable, timeoutMs)
    }

    private fun cancelWatchdog() {
        watchdogRunnable?.let { mainHandler.removeCallbacks(it) }
        watchdogRunnable = null
    }

    private fun destroyRecognizer() {
        try {
            recognizer?.destroy()
        } catch (ignored: Exception) {}
        recognizer = null
        isListening = false
    }
}
