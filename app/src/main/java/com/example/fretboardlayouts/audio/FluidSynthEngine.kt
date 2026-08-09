package com.example.fretboardlayouts.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack

object FluidSynthEngine {
    init { System.loadLibrary("fluidsynth_jni") }
    private const val SAMPLE_RATE = 44100
    private var audioTrack: AudioTrack? = null
    private var renderThread: Thread? = null
    @Volatile private var running = false
    @Volatile private var nativeInitialized = false  // NEW — track native init state

    external fun nativeInit(sampleRate: Int): Boolean
    external fun nativeLoadSoundFont(path: String): Int
    external fun nativeProgramChange(channel: Int, program: Int)
    external fun nativeNoteOn(channel: Int, key: Int, velocity: Int)
    external fun nativeNoteOff(channel: Int, key: Int)
    external fun nativeRender(buffer: ShortArray, numFrames: Int)
    external fun nativeBankAndProgramChange(channel: Int, bank: Int, program: Int) // NEW made by Claude 05/08/2026
    external fun nativeGetPresets(): String // NEW made by Claude 08/08/2026

    fun start(sf2Path: String): Boolean {
        // Check if thread is actually alive, not just flagged as running
        if (running && renderThread?.isAlive == true) {
            return true  // Real thread is running, reuse it
        }

        // Initialize native engine only once per process
        if (!nativeInit(SAMPLE_RATE)) return false
        nativeInitialized = true  // NEW — mark as initialized

        val sfId = nativeLoadSoundFont(sf2Path)
        if (sfId == -1) return false

        // Create/restart audio thread (rest of function stays the same)
        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT
        )
        audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC, SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
            minBuf, AudioTrack.MODE_STREAM
        ).also { it.play() }

        running = true
        renderThread = Thread {
            val frames = 512
            val buffer = ShortArray(frames * 2)
            while (running) {
                nativeRender(buffer, frames)
                audioTrack?.write(buffer, 0, buffer.size)
            }
        }.also { it.start() }
        return true
    }

    fun playTestChord() {
        nativeProgramChange(0, 25) // 25 = nylon acoustic guitar
        nativeNoteOn(0, 60, 100)
        nativeNoteOn(0, 64, 100)
        nativeNoteOn(0, 67, 100)
    }

    fun stop() {
        running = false
        renderThread?.join()
        audioTrack?.stop()
        audioTrack?.release()
    }
    fun copySoundFontFromAssets(context: android.content.Context, assetName: String): String {
        val outFile = java.io.File(context.filesDir, assetName)
        if (!outFile.exists()) {
            context.assets.open(assetName).use { input ->
                java.io.FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
        return outFile.absolutePath
    }
}