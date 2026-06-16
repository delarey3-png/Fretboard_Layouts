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

    external fun nativeInit(sampleRate: Int): Boolean
    external fun nativeLoadSoundFont(path: String): Int
    external fun nativeProgramChange(channel: Int, program: Int)
    external fun nativeNoteOn(channel: Int, key: Int, velocity: Int)
    external fun nativeNoteOff(channel: Int, key: Int)
    external fun nativeRender(buffer: ShortArray, numFrames: Int)

    fun start(sf2Path: String): Boolean {
        if (!nativeInit(SAMPLE_RATE)) return false
        val id = nativeLoadSoundFont(sf2Path)
        if (id < 0) return false

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