package com.example.fretboardlayouts.audio

import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * A simple implementation of the Karplus-Strong string synthesis algorithm.
 * This creates a much more realistic "plucked string" sound than a basic sine wave.
 */
object GuitarSynthesis {
    
    private const val SAMPLE_RATE = 44100
    
    fun generatePluck(pitch: Int, durationSeconds: Double, volume: Float): ShortArray {
        val freq = 440.0 * 2.0.pow((pitch - 69.0) / 12.0)
        if (freq <= 0) return ShortArray(0)
        
        val numSamples = (durationSeconds * SAMPLE_RATE).toInt()
        val samples = ShortArray(numSamples)
        
        // The "delay line" or "buffer" represents the length of the string
        val bufferSize = (SAMPLE_RATE / freq).roundToInt().coerceAtLeast(1)
        val ringBuffer = DoubleArray(bufferSize)
        
        // 1. Initialize buffer with white noise (the "pluck" or "pick attack")
        for (i in 0 until bufferSize) {
            ringBuffer[i] = Random.nextDouble(-1.0, 1.0)
        }
        
        // 2. Synthesize using the ring buffer
        var head = 0
        val feedback = 0.992 // Dampening factor (how long it rings)
        
        for (i in 0 until numSamples) {
            val nextIndex = (head + 1) % bufferSize
            
            // Average the current sample and the next sample (low-pass filter)
            val output = (ringBuffer[head] + ringBuffer[nextIndex]) / 2.0 * feedback
            
            // Write back to buffer
            ringBuffer[head] = output
            
            // Scale and save to output array
            // Apply a global envelope to ensure silence at the very end
            val fadeOut = (numSamples - i).toDouble() / numSamples
            samples[i] = (output * 32767.0 * volume * fadeOut).toInt().toShort()
            
            head = nextIndex
        }
        
        return samples
    }
    
    /** Mixes multiple plucked strings into a single audio buffer */
    fun mixStrum(notePitches: List<Int>, durationSeconds: Double, volume: Float, strumDelayMs: Int): ShortArray {
        val totalSamples = (durationSeconds * SAMPLE_RATE).toInt() + (notePitches.size * strumDelayMs * SAMPLE_RATE / 1000)
        val finalBuffer = FloatArray(totalSamples)
        
        notePitches.forEachIndexed { index, pitch ->
            val offset = index * strumDelayMs * SAMPLE_RATE / 1000
            val pluck = generatePluck(pitch, durationSeconds, volume / notePitches.size.coerceAtLeast(1))
            
            for (i in pluck.indices) {
                if (offset + i < finalBuffer.size) {
                    finalBuffer[offset + i] += pluck[i].toFloat()
                }
            }
        }
        
        // Convert back to ShortArray and clip if necessary
        return ShortArray(finalBuffer.size) { i ->
            finalBuffer[i].coerceIn(-32768f, 32767f).toInt().toShort()
        }
    }
}
