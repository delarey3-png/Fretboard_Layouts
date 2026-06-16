package com.example.fretboardlayouts.audio

import com.example.fretboardlayouts.theory.RhythmPattern
import com.example.fretboardlayouts.theory.SlotState
import com.example.fretboardlayouts.theory.TimeSignature
import com.example.fretboardlayouts.theory.beatTickToMs

fun renderVoice(
    pattern: RhythmPattern,
    startMs: Long,
    durationMs: Long,
    timeSignature: TimeSignature,
    channel: Int,
    pitch: Int,
    normalVelocity: Int,
    accentVelocity: Int,
    noteLengthMs: Int = 50,
    ticksPerBeat: Int = 4
): List<BackingTrackGenerator.MidiNoteEvent> {
    val events = mutableListOf<BackingTrackGenerator.MidiNoteEvent>()
    pattern.forEachIndexed { tick, state ->
        if (state == SlotState.REST) return@forEachIndexed
        val velocity = if (state == SlotState.ACCENT) accentVelocity else normalVelocity
        val beat = tick / ticksPerBeat
        val tickInBeat = tick % ticksPerBeat
        events.add(
            BackingTrackGenerator.MidiNoteEvent(
                startMs + beatTickToMs(beat, tickInBeat, ticksPerBeat, timeSignature, durationMs),
                channel, pitch, velocity, noteLengthMs
            )
        )
    }
    return events
}
/**
 * Like [renderVoice], but each hit advances through a sequence of pitches
 * (e.g. bass cycling root-fifth, or walking root-third-fifth-sixth) instead
 * of always playing the same note. Cycles back to the start if there are
 * more hits than pitches.
 */
fun renderPitchSequence(
    pattern: RhythmPattern,
    pitches: List<Int>,
    startMs: Long,
    durationMs: Long,
    timeSignature: TimeSignature,
    channel: Int,
    normalVelocity: Int,
    accentVelocity: Int,
    noteLengthMs: Int = 400,
    ticksPerBeat: Int = 4
): List<BackingTrackGenerator.MidiNoteEvent> {
    val events = mutableListOf<BackingTrackGenerator.MidiNoteEvent>()
    var hitIndex = 0
    pattern.forEachIndexed { tick, state ->
        if (state == SlotState.REST) return@forEachIndexed
        val velocity = if (state == SlotState.ACCENT) accentVelocity else normalVelocity
        val pitch = pitches[hitIndex % pitches.size]
        hitIndex++
        val beat = tick / ticksPerBeat
        val tickInBeat = tick % ticksPerBeat
        events.add(
            BackingTrackGenerator.MidiNoteEvent(
                startMs + beatTickToMs(beat, tickInBeat, ticksPerBeat, timeSignature, durationMs),
                channel, pitch, velocity, noteLengthMs
            )
        )
    }
    return events
}

/**
 * Renders a guitar strum pattern: [pattern] controls hit/rest/accent (and so velocity),
 * [directions] controls down/up per hit, and [voicing] is the chord shape to strum.
 * Both lists must be the same length.
 */
fun renderStrum(
    pattern: RhythmPattern,
    directions: List<Boolean>,
    voicing: List<Int>,
    startMs: Long,
    durationMs: Long,
    timeSignature: TimeSignature,
    channel: Int,
    normalVelocity: Int,
    accentVelocity: Int,
    ticksPerBeat: Int = 4
): List<BackingTrackGenerator.MidiNoteEvent> {
    val events = mutableListOf<BackingTrackGenerator.MidiNoteEvent>()
    pattern.forEachIndexed { tick, state ->
        if (state == SlotState.REST) return@forEachIndexed
        val velocity = if (state == SlotState.ACCENT) accentVelocity else normalVelocity
        val beat = tick / ticksPerBeat
        val tickInBeat = tick % ticksPerBeat
        val time = startMs + beatTickToMs(beat, tickInBeat, ticksPerBeat, timeSignature, durationMs)
        addStrum(events, time, channel, voicing, velocity, isDownstroke = directions[tick])
    }
    return events
}
private fun addStrum(events: MutableList<BackingTrackGenerator.MidiNoteEvent>, time: Long, channel: Int, pitches: List<Int>, velocity: Int, isDownstroke: Boolean) {
    val sortedPitches = if (isDownstroke) pitches.sorted() else pitches.sortedDescending()
    sortedPitches.forEachIndexed { i, pitch ->
        events.add(BackingTrackGenerator.MidiNoteEvent(time + (i * 20), channel, pitch, velocity, 800))
    }
}