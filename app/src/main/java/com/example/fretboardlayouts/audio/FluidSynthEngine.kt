package com.example.fretboardlayouts.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack

// MODIFIED 10/08/2026 — Added reference counting so the shared singleton is only
// truly stopped when the last owner releases it (previously any screen's cleanup
// could kill the engine while another screen was still using it).
object FluidSynthEngine {
    init { System.loadLibrary("fluidsynth_jni") }
    private const val SAMPLE_RATE = 44100
    private var audioTrack: AudioTrack? = null
    private var renderThread: Thread? = null
    @Volatile private var running = false
    @Volatile private var nativeInitialized = false

    // ADDED 10/08/2026 — Reference count: incremented by start(), decremented by release().
    // The engine only tears down when this reaches zero.
    @Volatile private var refCount = 0

    external fun nativeInit(sampleRate: Int): Boolean
    external fun nativeLoadSoundFont(path: String): Int
    external fun nativeProgramChange(channel: Int, program: Int)
    external fun nativeNoteOn(channel: Int, key: Int, velocity: Int)
    external fun nativeNoteOff(channel: Int, key: Int)
    external fun nativeRender(buffer: ShortArray, numFrames: Int)
    external fun nativeBankAndProgramChange(channel: Int, bank: Int, program: Int)
    external fun nativeGetPresets(): String
    // ADDED 10/08/2026 — Native destroy to properly free FluidSynth resources on shutdown.
    external fun nativeDestroy()

    @Synchronized
    fun start(sf2Path: String): Boolean {
        // MODIFIED 10/08/2026 — Increment ref count on every start() call.
        // If the render thread is already alive, just bump the count and reuse.
        if (running && renderThread?.isAlive == true) {
            refCount++
            return true
        }

        if (!nativeInit(SAMPLE_RATE)) return false
        nativeInitialized = true

        val sfId = nativeLoadSoundFont(sf2Path)
        if (sfId == -1) return false

        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT
        )
        audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC, SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
            minBuf, AudioTrack.MODE_STREAM
        ).also { it.play() }

        running = true
        refCount++ // ADDED 10/08/2026
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
        nativeProgramChange(0, 25)
        nativeNoteOn(0, 60, 100)
        nativeNoteOn(0, 64, 100)
        nativeNoteOn(0, 67, 100)
    }

    // MODIFIED 10/08/2026 — Renamed internal teardown to stopInternal().
    // External callers now use release() which respects the reference count.
    // stop() kept as a package-private alias so JamLabAudioEngine still compiles
    // unchanged — but release() is the correct call from lifecycle owners.
    @Synchronized
    fun release() {
        if (refCount > 0) refCount--
        if (refCount == 0) stopInternal()
    }

    // ADDED 10/08/2026 — Direct stop for emergency/test use; bypasses ref counting.
    @Synchronized
    fun stop() {
        refCount = 0
        stopInternal()
    }

    private fun stopInternal() {
        running = false
        renderThread?.join()
        renderThread = null
        // MODIFIED 10/08/2026 — Guard double-stop: only act if track exists.
        audioTrack?.let {
            it.stop()
            it.release()
        }
        audioTrack = null
        // ADDED 10/08/2026 — Destroy native FluidSynth instance to prevent memory leak
        // on every stop/start cycle (previously the native pointers were simply overwritten).
        if (nativeInitialized) {
            nativeDestroy()
            nativeInitialized = false
        }
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