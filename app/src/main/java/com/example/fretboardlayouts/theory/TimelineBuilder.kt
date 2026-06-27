package com.example.fretboardlayouts.theory

enum class TimeSignature(val beatsPerBar: Int, val denominator: Int, val display: String) {
    FOUR_FOUR(4, 4, "4/4"),
    THREE_FOUR(3, 4, "3/4"),
    TWO_FOUR(2, 4, "2/4"),
    FIVE_FOUR(5, 4, "5/4"),
    SIX_EIGHT(6, 8, "6/8"),
    NINE_EIGHT(9, 8, "9/8"),
    TWELVE_EIGHT(12, 8, "12/8")
}

/** Number of strum/rhythm grid slots per bar - sixteenths for simple meters, eighths for compound. */
val TimeSignature.subdivisionCount: Int
    get() = if (denominator == 8) beatsPerBar else beatsPerBar * 4

/** Converts a grid slot index (0-based) into a millisecond offset within the bar. */
fun slotToMs(slot: Int, timeSignature: TimeSignature, durationMs: Long): Long {
    return (durationMs * slot) / timeSignature.subdivisionCount
}

/** Converts a beat index + triplet-within-beat (0, 1, 2) into a millisecond offset within the bar, for shuffle/swing feel. */
fun tripletToMs(beat: Int, tripletInBeat: Int, timeSignature: TimeSignature, durationMs: Long): Long {
    val tripletsPerBar = timeSignature.beatsPerBar * 3
    return (durationMs * (beat * 3 + tripletInBeat)) / tripletsPerBar
}

/** Converts a beat index + tick-within-beat into a millisecond offset, for an arbitrary
 *  number of ticks per beat. Generalizes slotToMs (ticksPerBeat = 4) and tripletToMs (ticksPerBeat = 3). */
fun beatTickToMs(beat: Int, tickInBeat: Int, ticksPerBeat: Int, timeSignature: TimeSignature, durationMs: Long): Long {
    val totalTicks = timeSignature.beatsPerBar * ticksPerBeat
    return (durationMs * (beat * ticksPerBeat + tickInBeat)) / totalTicks
}

/** Everything screen 2 needs to render + play ONE chord's worth of the jam */
data class TimelineEvent(
    val barIndex: Int,
    val startMs: Long,
    val durationMs: Long,
    val chord: ResolvedChord,
    val chordToneOverlay: List<ChordTonePosition>
)

/** The fully precomputed jam session - built once during the loading screen */
data class JamTimeline(
    val key: MusicKey,
    val scaleOverlay: List<FretboardPosition>,
    val events: List<TimelineEvent>,
    val loopDurationMs: Long,
    val progressionLabels: List<String>,
    val timeSignature: TimeSignature
)

/**
 * Builds the complete timeline for one loop of the progression.
 */
fun buildJamTimeline(
    key: MusicKey,
    progressionSlots: List<ChordSlot>,
    scaleType: ScaleType,
    chordOverlayMode: ChordOverlayMode,
    tempoBpm: Int,
    timeSignature: TimeSignature,
    customStringFilter: Set<Int>? = null,
    barsPerChord: Int = 1
): JamTimeline {
    val resolvedChords = resolveProgression(key, progressionSlots)
    val scalePositions = generateScaleOverlay(key, scaleType)
    val scalePcs = scalePositions.map { it.pitchClass }.toSet()

    val barDurationMs = (timeSignature.beatsPerBar * 60_000L) / tempoBpm

    val events = mutableListOf<TimelineEvent>()
    var currentMs = 0L
    resolvedChords.forEachIndexed { index, chord ->
        val overlay = generateChordToneOverlay(chord, chordOverlayMode, scalePcs, customStringFilter)
        val duration = barDurationMs * barsPerChord
        events.add(
            TimelineEvent(
                barIndex = index,
                startMs = currentMs,
                durationMs = duration,
                chord = chord,
                chordToneOverlay = overlay
            )
        )
        currentMs += duration
    }

    return JamTimeline(
        key = key,
        scaleOverlay = scalePositions,
        events = events,
        loopDurationMs = currentMs,
        progressionLabels = progressionSlots.map { it.romanLabel },
        timeSignature = timeSignature
    )
}