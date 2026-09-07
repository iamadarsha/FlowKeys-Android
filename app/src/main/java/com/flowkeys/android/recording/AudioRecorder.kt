package com.flowkeys.android.recording

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

/**
 * High-performance, zero-allocation audio recording engine.
 * Captures 16 kHz, 16-bit Mono PCM audio for Silero VAD and Sherpa-ONNX ASR.
 */
class AudioRecorder {

    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val CHUNK_SIZE_SAMPLES = 512 // 32ms chunk at 16kHz
        private const val CHUNK_SIZE_BYTES = CHUNK_SIZE_SAMPLES * 2 // 16-bit = 2 bytes/sample
    }

    interface AudioChunkListener {
        fun onAudioChunk(samples: FloatArray, audioLevel: Float)
        fun onRecordError(message: String)
    }

    private var audioRecord: AudioRecord? = null
    @Volatile
    private var isRecording = false

    // Direct buffer allocated once to prevent Garbage Collection pauses in the audio thread
    private val directBuffer = ByteBuffer.allocateDirect(CHUNK_SIZE_BYTES).apply {
        order(ByteOrder.LITTLE_ENDIAN)
    }
    private val floatBuffer = FloatArray(CHUNK_SIZE_SAMPLES)

    @SuppressLint("MissingPermission")
    suspend fun startRecording(listener: AudioChunkListener) = withContext(Dispatchers.IO) {
        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = (minBufferSize * 2).coerceAtLeast(CHUNK_SIZE_BYTES * 4)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                listener.onRecordError("AudioRecord initialization failed")
                return@withContext
            }

            audioRecord?.startRecording()
            isRecording = true

            val shortArray = ShortArray(CHUNK_SIZE_SAMPLES)

            while (isActive && isRecording) {
                val readCount = audioRecord?.read(shortArray, 0, CHUNK_SIZE_SAMPLES) ?: -1
                if (readCount > 0) {
                    var sum = 0f
                    // Convert 16-bit PCM shorts to normalized [-1.0, 1.0] floats
                    for (i in 0 until readCount) {
                        val sample = shortArray[i] / 32768.0f
                        floatBuffer[i] = sample
                        sum += abs(sample)
                    }

                    val level = (sum / readCount * 4.0f).coerceIn(0f, 1f)
                    listener.onAudioChunk(floatBuffer.copyOf(readCount), level)
                }
            }
        } catch (e: Exception) {
            listener.onRecordError("Recording exception: ${e.localizedMessage}")
        } finally {
            release()
        }
    }

    fun stopRecording() {
        isRecording = false
    }

    private fun release() {
        try {
            if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord?.stop()
            }
            audioRecord?.release()
        } catch (ignored: Exception) {}
        audioRecord = null
        isRecording = false
    }
}
