package com.example.fretboardlayouts.audio

/**
 * Retains the MidiNoteEvent data class used across the audio pipeline.
 * generateLoopEvents() and its helpers (addStrum, findGuitarVoicing, findBassPitch)
 * were deleted — StyleEngine.generateAccompaniment() is the sole event source.
 */
object BackingTrackGenerator {

    /**
     * Represents a single note to be played at a specific time.
     */
    data class MidiNoteEvent(
        val timeMs: Long,
        val channel: Int, // 0=Guitar, 1=Bass, 9=Drums (standard MIDI)
        val pitch: Int,
        val velocity: Int,
        val durationMs: Int
    )
}