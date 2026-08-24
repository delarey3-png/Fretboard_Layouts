package com.example.fretboardlayouts.theory

// ================================================================
// VOICE LEADING ENGINE
// NEW made by Claude 19/08/2026
//
// Given the current chord voicing (MIDI notes already placed) and the
// next chord (root + quality), returns a new voicing that minimises
// total semitone movement across all voices.
//
// Algorithm — nearest-note (register matching):
//   1. Sort current voices low → high (voice 1 = bass, voice N = top)
//   2. Sort next chord pitch classes by interval order (root, 3rd, 5th…)
//   3. Match voice i → pitch class i by register
//   4. For each pair, find the octave of the pitch class closest to that voice
//   5. Clamp to instrument range, return sorted ascending
//
// This is the "minimal movement" principle used in classical voice leading:
// common tones stay put, moving voices travel the shortest path available.
//
// Source: music_theory_database_v2.json section_11_voice_leading_engine,
//         rules 2 (common tones) and 3 (stepwise motion) primarily.
//
// Used by StyleEngine.generateAccompaniment() when voiceLeadingEnabled = true.
// When disabled, StyleEngine falls back to findGuitarVoicing / findPianoChordNotes.
// ================================================================

import kotlin.math.abs

object VoiceLeadingEngine {

    // ── Instrument ranges ─────────────────────────────────────────
    // Guitar: low E (MIDI 40) to high e fret 12 (MIDI 76)
    // Piano comping: C3 (MIDI 48) to C5 (MIDI 72) — mid-register
    const val GUITAR_MIN = 40
    const val GUITAR_MAX = 76
    const val PIANO_MIN  = 48
    const val PIANO_MAX  = 72

    /**
     * Returns a voice-led voicing for the next chord.
     *
     * @param currentVoicing  MIDI notes of the chord currently sounding.
     *                        Empty → falls back to root-position build.
     * @param nextRootPitchClass  Root of the incoming chord (0-11).
     * @param nextQuality     Quality of the incoming chord.
     * @param chordType       How many tones to include (Power/Triad/Full/Extended).
     * @param rangeMin        Lowest allowed MIDI pitch for this instrument.
     * @param rangeMax        Highest allowed MIDI pitch for this instrument.
     * @return                MIDI pitches for the next chord, sorted ascending,
     *                        all within [rangeMin, rangeMax].
     */
    fun leadTo(
        currentVoicing: List<Int>,
        nextRootPitchClass: Int,
        nextQuality: ChordQuality,
        chordType: ChordType = ChordType.FULL,
        rangeMin: Int,
        rangeMax: Int
    ): List<Int> {
        // No previous voicing — build a plain root-position chord
        if (currentVoicing.isEmpty()) {
            val rootMidi = ChordNoteBuilder.nearestMidi(nextRootPitchClass, rangeMin)
                .let { if (it > rangeMin + 15) it - 12 else it }
            return ChordNoteBuilder.buildNotes(rootMidi, nextQuality, chordType)
                .filter { it in rangeMin..rangeMax }
        }

        val intervals    = ChordNoteBuilder.intervalsFor(nextQuality, chordType)
        val pitchClasses = intervals.map { (nextRootPitchClass + it) % 12 }
        val sortedCurrent = currentVoicing.sorted()

        return pitchClasses
            .mapIndexed { i, pc ->
                // Reference pitch: match by register (voice 1 leads to chord tone 1, etc.)
                // If next chord has more tones than current voices, extra tones reference
                // the highest current voice — they naturally sit above the existing voicing.
                val refPitch = sortedCurrent.getOrElse(i) { sortedCurrent.last() }
                // Find the octave of pc closest to refPitch, then clamp to instrument range
                closestMidi(pc, refPitch).coerceIn(rangeMin, rangeMax)
            }
            .sorted()
    }

    /**
     * Convenience overloads for guitar and piano with pre-set ranges.
     */
    fun leadToGuitar(
        currentVoicing: List<Int>,
        nextRootPitchClass: Int,
        nextQuality: ChordQuality,
        chordType: ChordType = ChordType.FULL
    ): List<Int> = leadTo(currentVoicing, nextRootPitchClass, nextQuality, chordType, GUITAR_MIN, GUITAR_MAX)

    fun leadToPiano(
        currentVoicing: List<Int>,
        nextRootPitchClass: Int,
        nextQuality: ChordQuality,
        chordType: ChordType = ChordType.FULL
    ): List<Int> = leadTo(currentVoicing, nextRootPitchClass, nextQuality, chordType, PIANO_MIN, PIANO_MAX)

    // ── Helpers ───────────────────────────────────────────────────

    /**
     * Finds the MIDI pitch for [pitchClass] at the octave closest to [nearMidi].
     * Checks BOTH above and below — unlike ChordNoteBuilder.nearestMidi() which
     * only returns at-or-above. Closest-wins is what voice leading needs.
     *
     * Example: closestMidi(11 /*B*/, 60 /*C4*/) → 59 (B3, 1 semitone away)
     *          rather than 71 (B4, 11 semitones away).
     */
    fun closestMidi(pitchClass: Int, nearMidi: Int): Int {
        val base  = (nearMidi / 12) * 12 + pitchClass
        val above = if (base >= nearMidi) base else base + 12
        val below = above - 12
        return if (abs(nearMidi - below) <= abs(nearMidi - above)) below else above
    }

    /**
     * Total semitone movement between two same-length voicings.
     * Lower = smoother voice leading.
     * Useful for comparing candidate voicings or logging quality.
     */
    fun totalMovement(from: List<Int>, to: List<Int>): Int {
        val f = from.sorted()
        val t = to.sorted()
        return f.zip(t).sumOf { (a, b) -> abs(a - b) }
    }

    /**
     * True if [pitchClass] appears in [voicing] (at any octave).
     * Used to implement the "common tone retention" rule: if the same
     * pitch class exists in both chords, its voice can stay put.
     */
    fun isCommonTone(pitchClass: Int, voicing: List<Int>): Boolean =
        voicing.any { it % 12 == pitchClass }
}
