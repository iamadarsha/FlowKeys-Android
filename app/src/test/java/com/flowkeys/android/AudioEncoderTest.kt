package com.flowkeys.android

import com.flowkeys.android.asr.AudioEncoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioEncoderTest {

    @Test
    fun testWavHeaderMagicBytes() {
        val samples = FloatArray(1600) { 0.5f }
        val wavBytes = AudioEncoder.encodeToWav(samples, sampleRate = 16000)

        // Total size must be 44-byte header + 1600 * 2 bytes = 3244 bytes
        assertEquals(44 + 3200, wavBytes.size)

        // Check RIFF header
        assertEquals('R'.code.toByte(), wavBytes[0])
        assertEquals('I'.code.toByte(), wavBytes[1])
        assertEquals('F'.code.toByte(), wavBytes[2])
        assertEquals('F'.code.toByte(), wavBytes[3])

        // Check WAVE fmt
        assertEquals('W'.code.toByte(), wavBytes[8])
        assertEquals('A'.code.toByte(), wavBytes[9])
        assertEquals('V'.code.toByte(), wavBytes[10])
        assertEquals('E'.code.toByte(), wavBytes[11])

        // Check data subchunk header
        assertEquals('d'.code.toByte(), wavBytes[36])
        assertEquals('a'.code.toByte(), wavBytes[37])
        assertEquals('t'.code.toByte(), wavBytes[38])
        assertEquals('a'.code.toByte(), wavBytes[39])
    }

    @Test
    fun testSampleRateAndChannelInHeader() {
        val samples = FloatArray(800) { 0.0f }
        val sampleRate = 16000
        val wavBytes = AudioEncoder.encodeToWav(samples, sampleRate = sampleRate)

        val buffer = ByteBuffer.wrap(wavBytes).order(ByteOrder.LITTLE_ENDIAN)

        // NumChannels at offset 22
        buffer.position(22)
        val channels = buffer.short
        assertEquals(1.toShort(), channels)

        // SampleRate at offset 24
        val readSampleRate = buffer.int
        assertEquals(16000, readSampleRate)

        // BitsPerSample at offset 34
        buffer.position(34)
        val bitsPerSample = buffer.short
        assertEquals(16.toShort(), bitsPerSample)

        // Data size at offset 40
        buffer.position(40)
        val dataSize = buffer.int
        assertEquals(800 * 2, dataSize)
    }

    @Test
    fun testSampleClampingAndScaling() {
        val samples = floatArrayOf(-2.0f, -1.0f, 0.0f, 1.0f, 2.0f)
        val wavBytes = AudioEncoder.encodeToWav(samples, sampleRate = 16000)

        val buffer = ByteBuffer.wrap(wavBytes, 44, samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)

        val s0 = buffer.short // clamped to -1.0
        val s1 = buffer.short // -1.0
        val s2 = buffer.short // 0.0
        val s3 = buffer.short // 1.0
        val s4 = buffer.short // clamped to 1.0

        assertTrue(s0 <= -32767)
        assertTrue(s1 <= -32767)
        assertEquals(0.toShort(), s2)
        assertTrue(s3 >= 32767)
        assertTrue(s4 >= 32767)
    }
}
