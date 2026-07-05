package com.example.fretboardlayouts.audio

import android.content.Context
import android.util.Log
import com.example.fretboardlayouts.theory.Genre
import kotlinx.coroutines.*

/**
 * Simple audio engine for TestingActivity.
 * Handles play/stop/program switching without complex architecture.
 */
class TestingAudioEngine(context: Context) {
    private val TAG = "TestingAudioEngine"
    private val midiPlayer = MidiPlayer(context)
    private var playbackJob: Job? = null
    private var currentGenre = Genre.ROCK
    private var isPlaying = false
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    /**
     * Load a genre (setup instrument programs)
     */
    fun loadGenre(genre: Genre) {
        Log.d(TAG, "Loading genre: $genre")
        currentGenre = genre

        // Set up default programs for the genre
        val instrumentation = when (genre) {
            Genre.ROCK -> GenreInstrumentation(
                guitarProgram = 29,
                bassProgram = 34,
                drumKitProgram = 16
            )

            Genre.BLUES -> GenreInstrumentation(
                guitarProgram = 26,
                bassProgram = 33,
                drumKitProgram = 0
            )

            Genre.COUNTRY -> GenreInstrumentation(
                guitarProgram = 25,
                bassProgram = 32,
                drumKitProgram = 0
            )

            Genre.FUNK -> GenreInstrumentation(
                guitarProgram = 28,
                bassProgram = 36,
                drumKitProgram = 24
            )

            Genre.JAZZ -> GenreInstrumentation(
                guitarProgram = 26,
                bassProgram = 32,
                drumKitProgram = 40
            )
        }

        midiPlayer.setupInstruments(instrumentation)
    }

    /**
     * Play a simple test loop for the current genre
     */
    fun playLoop() {
        if (isPlaying) return

        Log.d(TAG, "Starting playback for genre: $currentGenre")
        isPlaying = true

        playbackJob?.cancel()
        playbackJob = scope.launch {
            try {
                while (isPlaying) {
                    // Play a simple 4-beat loop
                    val beat = 500L // 500ms per beat

                    // Kick on beat 1
                    midiPlayer.noteOn(0, 60, 100) // Gemini help 03.07 added this whole line
                    midiPlayer.noteOn(1, 36, 100) //// Gemini help 03.07 changed 9 to 1
                    delay(50)
                    midiPlayer.noteOff(0, 36) // Gemini help 03.07 changed 9 to 0
                    delay(beat - 150) // Gemini help 03.07 changed 50 to 150

                    // Snare on beat 2
                    midiPlayer.noteOn(9, 38, 90)
                    delay(50)
                    midiPlayer.noteOff(9, 38)
                    delay(beat - 50)

                    // Kick on beat 3
                    midiPlayer.noteOn(9, 36, 95)
                    delay(50)
                    midiPlayer.noteOff(9, 36)
                    delay(beat - 50)

                    // Snare on beat 4
                    midiPlayer.noteOn(9, 38, 90)
                    delay(50)
                    midiPlayer.noteOff(9, 38)
                    delay(beat - 50)
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "Playback cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "Playback error: ${e.message}", e)
            }
        }
    }

    /**
     * Stop the current playback
     */
    fun stopLoop() {
        Log.d(TAG, "Stopping playback")
        isPlaying = false
        playbackJob?.cancel()
        midiPlayer.stopAllNotes()
    }

    /**
     * Switch MIDI program on a channel in real-time
     */
    fun switchProgram(channel: Int, programNum: Int) {
        Log.d(TAG, "Switching channel $channel to program $programNum")
        midiPlayer.setProgram(channel, programNum)
    }

    /**
     * Restart the current loop with new program
     */
    fun restartLoop() {
        if (isPlaying) {
            stopLoop()
            Thread.sleep(100)
            playLoop()
        }
    }

    /**
     * Release all resources
     */
    fun release() {
        stopLoop()
        playbackJob?.cancel()
        scope.cancel()
        midiPlayer.release()
    }

    fun stopAudio() {
        // Fully silence the shared engine state, not just this engine instance
        for (channel in 0..15) {
            for (note in 0..127) {
                midiPlayer.noteOff(channel, note)
            }
        }
        midiPlayer.stopAllNotes()
    }
    fun changeProgramOnChannel(channel: Int, program: Int) {
        FluidSynthEngine.nativeProgramChange(channel, program)
    }
}