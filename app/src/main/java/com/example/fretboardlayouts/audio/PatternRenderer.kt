package com.example.fretboardlayouts.audio

import com.example.fretboardlayouts.theory.RhythmPattern
import com.example.fretboardlayouts.theory.SlotState
import com.example.fretboardlayouts.theory.TimeSignature
import com.example.fretboardlayouts.theory.slotToMs

/**
 * Renders one voice's [RhythmPattern] into MIDI note events for one bar.
 * [pattern].size must equal [timeSignature].subdivisionCount.
 */
fun renderVoice(
    pattern: RhythmPattern,
    startMs: Long,
    durationMs: Long,
    timeSignature: TimeSignature,
    channel: Int,
    pitch: Int,
    normalVelocity: Int,
    accentVelocity: Int,
    noteLengthMs: Int = 50
): List<BackingTrackGenerator.MidiNoteEvent> {
    val events = mutableListOf<BackingTrackGenerator.MidiNoteEvent>()
    pattern.forEachIndexed { slot, state ->
        if (state == SlotState.REST) return@forEachIndexed
        val velocity = if (state == SlotState.ACCENT) accentVelocity else normalVelocity
        events.add(
            BackingTrackGenerator.MidiNoteEvent(
                startMs + slotToMs(slot, timeSignature, durationMs),
                channel, pitch, velocity, noteLengthMs
            )
        )
    }
    return events
}