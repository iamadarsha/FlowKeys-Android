package com.flowkeys.android.asr

import java.nio.ByteBuffer
import java.nio.ByteOrder

object AudioEncoder {
    fun encodeToWav(samples: FloatArray, sampleRate: Int = 16000): ByteArray {
        val channels = 1
        val bitRate = 16
        val byteRate = sampleRate * channels * bitRate / 8
        val blockAlign = channels * bitRate / 8
        val dataSize = samples.size * 2
        val totalSize = 36 + dataSize

        val header = ByteBuffer.allocate(44).apply {
            order(ByteOrder.LITTLE_ENDIAN)
            put("RIFF".toByteArray())
            putInt(totalSize)
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16) // Subchunk1Size
            putShort(1) // AudioFormat (PCM)
            putShort(channels.toShort())
            putInt(sampleRate)
            putInt(byteRate)
            putShort(blockAlign.toShort())
            putShort(bitRate.toShort())
            put("data".toByteArray())
            putInt(dataSize)
        }.array()

        val dataBuffer = ByteBuffer.allocate(dataSize).apply {
            order(ByteOrder.LITTLE_ENDIAN)
            for (sample in samples) {
                var s = sample
                if (s > 1.0f) s = 1.0f
                if (s < -1.0f) s = -1.0f
                putShort((s * Short.MAX_VALUE).toInt().toShort())
            }
        }.array()

        return header + dataBuffer
    }
}
