package com.flowkeys.android.recording

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.flowkeys.android.MainActivity
import com.flowkeys.android.R
import com.flowkeys.android.asr.SherpaAsrEngine
import com.flowkeys.android.core.coordinator.FlowKeysCoordinator
import com.flowkeys.android.models.ModelManager
import com.flowkeys.android.vad.AudioRingBuffer
import com.flowkeys.android.vad.SileroVadEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Transient Foreground Service managing microphone capture, VAD, and Sherpa-ONNX speech inference.
 * Runs exclusively during active dictation and shuts down immediately upon text insertion.
 */
class DictationRecordingService : Service(), AudioRecorder.AudioChunkListener, SileroVadEngine.Listener {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var recordingJob: Job? = null
    private var isRecording = false

    private val audioRecorder = AudioRecorder()
    private val audioRingBuffer = AudioRingBuffer()
    private val vadEngine = SileroVadEngine()

    private lateinit var asrEngine: SherpaAsrEngine
    private lateinit var modelManager: ModelManager

    companion object {
        const val ACTION_START_RECORDING = "com.flowkeys.android.action.START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.flowkeys.android.action.STOP_RECORDING"
        const val ACTION_CANCEL_RECORDING = "com.flowkeys.android.action.CANCEL_RECORDING"

        private const val CHANNEL_ID = "flowkeys_dictation_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        modelManager = ModelManager(this)
        asrEngine = SherpaAsrEngine(this, modelManager, FlowKeysCoordinator.deviceTier)
        vadEngine.listener = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> startRecordingSession()
            ACTION_STOP_RECORDING -> stopRecordingAndProcess()
            ACTION_CANCEL_RECORDING -> cancelRecording()
        }
        return START_NOT_STICKY
    }

    private fun startRecordingSession() {
        if (isRecording) return
        isRecording = true
        audioRingBuffer.clear()
        vadEngine.reset()

        val notification = buildForegroundNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        recordingJob = serviceScope.launch {
            audioRecorder.startRecording(this@DictationRecordingService)
        }
    }

    override fun onAudioChunk(samples: FloatArray, audioLevel: Float) {
        if (!isRecording) return

        // 1. Accumulate audio in circular buffer
        audioRingBuffer.write(samples)

        // 2. Feed to VAD for speech activity and endpoint detection
        vadEngine.processChunk(samples)

        // 3. Update UI waveform via coordinator
        val durationMs = (audioRingBuffer.currentDurationSeconds * 1000f).toLong()
        FlowKeysCoordinator.updateRecordingLevel(audioLevel, durationMs)
    }

    override fun onSpeechStarted() {
        // Speech detected
    }

    override fun onSpeechEnded() {
        // User naturally paused or stopped speaking; auto-finish dictation
        stopRecordingAndProcess()
    }

    override fun onRecordError(message: String) {
        FlowKeysCoordinator.onError(message)
        cancelRecording()
    }

    private fun stopRecordingAndProcess() {
        if (!isRecording) return
        isRecording = false
        audioRecorder.stopRecording()
        recordingJob?.cancel()

        FlowKeysCoordinator.setProcessing("Polishing speech…")

        serviceScope.launch {
            val audioSamples = audioRingBuffer.toFloatArray()
            val language = FlowKeysCoordinator.selectedLanguage.value

            // 1. ASR Transcription
            val asrResult = asrEngine.transcribe(audioSamples, language)

            // 2. Pass raw text through Linguistic Micro-Polisher (Phase 3 hook)
            val polishedText = com.flowkeys.android.polish.MicroPolisher.polish(
                rawText = asrResult.rawText,
                language = language
            )

            // 3. Insert polished text into focused field
            FlowKeysCoordinator.onTextReady(polishedText, applicationContext)

            // 4. Clean up and stop foreground service
            audioRingBuffer.clear()
            asrEngine.evictModel()
            stopSelf()
        }
    }

    private fun cancelRecording() {
        isRecording = false
        audioRecorder.stopRecording()
        recordingJob?.cancel()
        audioRingBuffer.clear()
        asrEngine.evictModel()
        FlowKeysCoordinator.reset()
        stopSelf()
    }

    private fun buildForegroundNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_recording_title))
            .setContentText(getString(R.string.notification_recording_text))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRecording = false
        audioRecorder.stopRecording()
        recordingJob?.cancel()
        asrEngine.release()
    }
}
