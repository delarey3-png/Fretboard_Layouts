package com.example.fretboardlayouts.audio

import android.content.Context
import android.util.Log
import com.example.fretboardlayouts.brain.*

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
        Log.d(TAG, "Channel $channel → Program $program")
        FluidSynthEngine.nativeProgramChange(channel, program)
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
}
