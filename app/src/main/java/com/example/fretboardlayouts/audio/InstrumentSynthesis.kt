package com.example.fretboardlayouts.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Advanced multi-instrument synthesis for the fallback audio engine.
 */
object InstrumentSynthesis {

    private const val SAMPLE_RATE = 44100

    /** CHANNEL 0: GUITAR (Karplus-Strong Pluck) */
    fun generateGuitar(pitch: Int, volume: Float): ShortArray {
        return GuitarSynthesis.generatePluck(pitch, 0.5, volume * 0.4f)
    }

    /** CHANNEL 1: BASS (Deep Karplus-Strong) */
    fun generateBass(pitch: Int, volume: Float): ShortArray {
        val freq = 440.0 * 2.0.pow((pitch - 69.0) / 12.0)
        // Ensure frequency is within valid range for buffer size
        if (freq <= 0) return ShortArray(0)
        
        val numSamples = (0.6 * SAMPLE_RATE).toInt()
        val samples = ShortArray(numSamples)
        
        val bufferSize = (SAMPLE_RATE / freq).toInt().coerceAtLeast(1)
        val ringBuffer = DoubleArray(bufferSize) { Random.nextDouble(-1.0, 1.0) }
        
        var head = 0
        val feedback = 0.996 // Longer ring for bass
        
        for (i in 0 until numSamples) {
            val nextIndex = (head + 1) % bufferSize
            // Stronger low-pass for bass (average of 3 samples)
            val output = (ringBuffer[head] + ringBuffer[nextIndex]) / 2.0 * feedback
            ringBuffer[head] = output
            
            val fadeOut = (numSamples - i).toDouble() / numSamples
            samples[i] = (output * 32767.0 * volume * 0.7 * fadeOut).toInt().toShort()
            head = nextIndex
        }
        return samples
    }

    /** CHANNEL 9: DRUMS */
    fun generateDrum(pitch: Int, volume: Float): ShortArray {
        return when (pitch) {
            36 -> generateKick(volume)
            38 -> generateSnare(volume)
            42, 44, 46 -> generateHiHat(volume)
            else -> generateKick(volume * 0.5f) // Default to light kick
        }
    }

    private fun generateKick(volume: Float): ShortArray {
        val duration = 0.08
        val numSamples = (duration * SAMPLE_RATE).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            // Pitch sweep from 150Hz to 40Hz
            val freq = 150.0 * exp(-30.0 * t) + 40.0
            val phase = 2.0 * PI * freq * t
            val envelope = exp(-20.0 * t)
            samples[i] = (sin(phase) * 32767.0 * volume * 1.0 * envelope).toInt().toShort()
        }
        return samples
    }

    private fun generateSnare(volume: Float): ShortArray {
        val duration = 0.08
        val numSamples = (duration * SAMPLE_RATE).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            // White noise mixed with a 200Hz tone
            val noise = Random.nextDouble(-1.0, 1.0)
            val tone = sin(2.0 * PI * 200.0 * t)
            val envelope = exp(-25.0 * t)
            samples[i] = ((noise * 0.8 + tone * 0.2) * 32767.0 * volume * 0.7 * envelope).toInt().toShort()
        }
        return samples
    }

    private fun generateHiHat(volume: Float): ShortArray {
        val duration = 0.05
        val numSamples = (duration * SAMPLE_RATE).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            // High-pitched noise
            val noise = Random.nextDouble(-1.0, 1.0)
            val envelope = exp(-60.0 * t)
            samples[i] = (noise * 32767.0 * volume * 0.4 * envelope).toInt().toShort()
        }
        return samples
    }
}
