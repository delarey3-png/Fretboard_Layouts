package com.example.fretboardlayouts.audio

import android.content.Context
import android.util.Log
import com.example.fretboardlayouts.theory.Genre // NEW made by Claude 09/08/2026

/**
 * Standalone audio engine for JamLabActivity.
 * Completely isolated from MainViewModel's audio pipeline.
 *
 * - Own MidiPlayer instance (not shared)
 * - Handles note-on/off and program changes
 * - Clean start/stop without affecting FluidSynthEngine singleton
 * - Safe to open and close without breaking Jam screen
 */
class JamLabAudioEngine(context: Context) {
    private val TAG = "JamLabAudioEngine"
    private val midiPlayer = MidiPlayer(context)

    init {
        Log.i(TAG, "JamLabAudioEngine initialized (isolated from MainViewModel)")
    }

    /**
     * Fire a note on a channel at a given velocity
     */
    fun noteOn(channel: Int, pitch: Int, velocity: Int) {
        midiPlayer.noteOn(channel, pitch, velocity)
    }

    /**
     * Turn off a note on a channel
     */
    fun noteOff(channel: Int, pitch: Int) {
        midiPlayer.noteOff(channel, pitch)
    }

    /**
     * Switch the MIDI program on a channel (e.g., change from Nylon to Distortion guitar)
     * This allows real-time instrument swapping while audio plays
     */
    fun changeProgramOnChannel(channel: Int, program: Int) {
        Log.d(TAG, "Channel $channel → Bank 0 Program $program")
        FluidSynthEngine.nativeBankAndProgramChange(channel, 0, program)
    }

    // NEW made by Claude 05/08/2026
    // Bank-aware patch selection — for soundfonts with multiple banks
    fun changePatchOnChannel(channel: Int, bank: Int, program: Int) {
        Log.d(TAG, "Channel $channel → Bank $bank Program $program")
        FluidSynthEngine.nativeBankAndProgramChange(channel, bank, program)
    }

    // NEW made by Claude 09/08/2026
    // Auto-loads genre-appropriate patches on all instrument channels.
    // Reads GenreInstruments.forGenre() — all genre defaults are bank 0.
    // Channels with program = -1 (not applicable for this genre) are skipped.
    // Ch2 (Piano) is intentionally excluded — it's always Grand Piano or the
    // user's last manual selection; genre doesn't override it.
    // Called from JamLabActivity's LaunchedEffect(currentGenre) on every genre switch.
    fun loadGenrePatches(genre: Genre) {
        val g = GenreInstruments.forGenre(genre)
        mapOf(
            0  to g.guitarProgram,
            1  to g.bassProgram,
            3  to g.organProgram,
            4  to g.stringsProgram,
            5  to g.ensembleProgram,
            6  to g.brassProgram,
            7  to g.reedProgram,
            8  to g.pipeProgram,
            9  to g.drumKitProgram,
            10 to g.synthProgram,
            11 to g.ethnicProgram
        ).forEach { (channel, program) ->
            if (program != -1) {
                changePatchOnChannel(channel, 0, program)
                Log.d(TAG, "Genre ${genre.displayName}: ch$channel → program $program")
            }
        }
    }

    /**
     * Stop all sounding notes gracefully
     * Does NOT call FluidSynthEngine.stop() — that's the shared engine's job
     */
    fun stopAudio() {
        Log.d(TAG, "Stopping audio (gentle cleanup, no engine shutdown)")
        midiPlayer.stopAllNotes()
    }

    /**
     * Full cleanup when JamLabActivity is destroyed
     * Still doesn't touch FluidSynthEngine — it's shared with MainViewModel
     */
    fun release() {
        Log.d(TAG, "Releasing JamLabAudioEngine")
        stopAudio()
        midiPlayer.release()
    }

    // NEW made by Claude 08/08/2026
    // Returns raw preset string from the loaded SF2 — parsing happens in JamLabActivity
    // Format: "bank:program:name|bank:program:name|..."
    fun getRawPresets(): String {
        return FluidSynthEngine.nativeGetPresets()
    }
}