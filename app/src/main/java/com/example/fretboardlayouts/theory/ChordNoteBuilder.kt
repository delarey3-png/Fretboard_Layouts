package com.example.fretboardlayouts.theory

// ================================================================
// CHORD NOTE BUILDER
// NEW made by Claude 19/08/2026
//
// Translates ChordQuality into semitone intervals and actual MIDI
// pitch lists. The missing link between the theory layer (which knows
// what chord is playing) and the audio layer (which needs MIDI notes).
//
// Source: music_theory_database_v2.json section_04_chord_formulas,
//         verified against the app's existing ChordQuality enum (25 values).
//
// Two public entry points:
//   ChordNoteBuilder.intervalsFor(quality, chordType) → semitone offsets from root
//   ChordNoteBuilder.buildNotes(rootMidi, quality, chordType) → MIDI note list
//
// StyleEngine generators (guitar, piano, strings, brass) call buildNotes()
// to replace the old hardcoded note-picking logic with correct voicings.
// The ChordType parameter is the bridge to the future ChordType selector UI.
// ================================================================

// ── Chord type selector ───────────────────────────────────────────

/**
 * Controls how many chord tones are included when building notes.
 * Owned by each generator call site — guitar, piano, and strings
 * can each operate in a different ChordType independently.
 *
 * POWER:    Root + fifth only. No third. Rock/metal with distortion;
 *           avoids the clash between major/minor 3rd and fuzz.
 * TRIAD:    Root, 3rd (or 2nd/4th for sus), 5th. Three voices only,
 *           even for 7th/9th qualities. Clean and fundamental.
 * FULL:     All tones the quality defines — the chord as written.
 *           Triads = 3 notes, 7th chords = 4, 9th chords = 5 etc.
 * EXTENDED: Reserved for voicing-engine expansion. Currently identical
 *           to FULL. Planned use: voicing engine adds colour tones
 *           (e.g. #11 on Maj7) for jazz context.
 */
enum class ChordType(val displayName: String) {
    POWER   ("Power (5)"),
    TRIAD   ("Triad"),
    TETRAD  ("Tetrad (7th)"),
    FULL    ("Full"),
    EXTENDED("Extended")
}

// ── Chord note builder ────────────────────────────────────────────

object ChordNoteBuilder {

    /**
     * Semitone intervals above the root for all 25 ChordQuality values.
     *
     * Source: music_theory_database_v2.json section_04_chord_formulas.
     * Slash chord intervals are NOT here — slash chords affect only the bass
     * note (handled in generateBass()); upper voices use the base chord quality.
     *
     * Intervals that exceed one octave (9th = 14, 11th = 17, 13th = 21)
     * are included for extended qualities and controlled via ChordType.
     *
     * NB — DOMINANT13 omits the 11th (interval 17) per standard practice:
     * the natural 11 clashes with the major 3rd. Always omit the 11 on a 13 chord
     * unless it is specifically a dominant 11th chord.
     */
    val INTERVALS: Map<ChordQuality, List<Int>> = mapOf(

        // ── Triads ───────────────────────────────────────────────
        ChordQuality.MAJOR          to listOf(0, 4, 7),
        ChordQuality.MINOR          to listOf(0, 3, 7),
        ChordQuality.DIMINISHED     to listOf(0, 3, 6),
        ChordQuality.AUGMENTED      to listOf(0, 4, 8),
        ChordQuality.SUS2           to listOf(0, 2, 7),
        ChordQuality.SUS4           to listOf(0, 5, 7),

        // ── Seventh chords ───────────────────────────────────────
        ChordQuality.MAJOR7         to listOf(0, 4, 7, 11),
        ChordQuality.MINOR7         to listOf(0, 3, 7, 10),
        ChordQuality.DOMINANT7      to listOf(0, 4, 7, 10),
        ChordQuality.MINOR7_FLAT5   to listOf(0, 3, 6, 10),  // half-diminished (ø7)
        ChordQuality.DIMINISHED7    to listOf(0, 3, 6,  9),  // fully diminished
        ChordQuality.DOMINANT7_SUS4 to listOf(0, 5, 7, 10),
        ChordQuality.MINOR_MAJOR7   to listOf(0, 3, 7, 11),  // melodic minor tonic
        ChordQuality.AUGMENTED7     to listOf(0, 4, 8, 10),  // aug + b7
        ChordQuality.MAJOR7_SHARP5  to listOf(0, 4, 8, 11),  // aug + Maj7

        // ── Sixth chords ─────────────────────────────────────────
        ChordQuality.SIX            to listOf(0, 4, 7,  9),
        ChordQuality.MINOR_SIX      to listOf(0, 3, 7,  9),
        ChordQuality.SIX_NINE       to listOf(0, 4, 7,  9, 14),

        // ── Add chords (no 7th) ──────────────────────────────────
        ChordQuality.ADD9           to listOf(0, 4, 7, 14),
        ChordQuality.MINOR_ADD9     to listOf(0, 3, 7, 14),

        // ── Ninth chords (7th + 9th) ─────────────────────────────
        ChordQuality.MAJOR9         to listOf(0, 4, 7, 11, 14),
        ChordQuality.MINOR9         to listOf(0, 3, 7, 10, 14),
        ChordQuality.DOMINANT9      to listOf(0, 4, 7, 10, 14),

        // ── Extended chords ──────────────────────────────────────
        // DOMINANT11: 7th + 9th + 11th (natural 11 is fine on a dom11 chord)
        ChordQuality.DOMINANT11     to listOf(0, 4, 7, 10, 14, 17),
        // DOMINANT13: omit the 11th (interval 17) — standard practice
        ChordQuality.DOMINANT13     to listOf(0, 4, 7, 10, 14, 21)
    )

    /**
     * Returns intervals filtered to [chordType].
     *
     * POWER    → [0, 7] regardless of quality (overrides everything)
     * TRIAD    → first 3 intervals only (root + 2nd voice + 5th/aug5/dim5)
     * FULL     → all intervals as defined in INTERVALS for this quality
     * EXTENDED → same as FULL (reserved for voicing-engine expansion)
     *
     * Falls back to major triad [0, 4, 7] for any unmapped quality.
     */
    fun intervalsFor(
        quality: ChordQuality,
        chordType: ChordType = ChordType.FULL
    ): List<Int> {
        val all = INTERVALS[quality] ?: listOf(0, 4, 7)
        return when (chordType) {
            ChordType.POWER    -> listOf(0, 7)
            ChordType.TRIAD  -> all.take(3)
            ChordType.TETRAD -> all.take(4)
            ChordType.FULL,
            ChordType.EXTENDED -> all
        }
    }

    /**
     * Builds a list of MIDI pitch numbers for a chord.
     *
     * @param rootMidi  MIDI pitch of the root (already octave-placed by caller).
     *                  E.g. C3 = 48, G3 = 55, C4 = 60.
     * @param quality   Chord quality — from ResolvedChord or ChordSlot.effectiveQuality.
     * @param chordType How many tones to include (default FULL).
     * @return          MIDI pitches, root first, ascending within the voicing.
     *                  Extended chords may span more than one octave.
     */
    fun buildNotes(
        rootMidi: Int,
        quality: ChordQuality,
        chordType: ChordType = ChordType.FULL
    ): List<Int> = intervalsFor(quality, chordType).map { rootMidi + it }

    /**
     * Power chord: root + fifth + octave above root.
     * The doubled octave is standard for rock power chords —
     * gives the full sound on distortion without harmonic clutter.
     */
    fun buildPowerChord(rootMidi: Int): List<Int> =
        listOf(rootMidi, rootMidi + 7, rootMidi + 12)

    /**
     * Finds the MIDI pitch for [pitchClass] at the closest octave to [nearMidi].
     * Returns the first pitch >= nearMidi that has the given pitch class.
     * Useful for placing individual chord tones near a reference pitch.
     *
     * Example: nearestMidi(pitchClass = 4 /*E*/, nearMidi = 60 /*C4*/) → 64 (E4)
     */
    fun nearestMidi(pitchClass: Int, nearMidi: Int): Int {
        val base = (nearMidi / 12) * 12 + pitchClass
        return if (base < nearMidi) base + 12 else base
    }
}
// made by Claude 02/09/2026
enum class ChordDensity(val displayName: String) {
    POWER  ("Power"),
    TRIAD  ("Triad"),
    TETRAD ("Tetrad"),
    FULL   ("Full"),
    AUTO   ("Auto")
}

fun ChordDensity.toChordType(channel: Int): ChordType = when (this) {
    ChordDensity.POWER  -> ChordType.POWER
    ChordDensity.TRIAD  -> ChordType.TRIAD
    ChordDensity.TETRAD -> ChordType.TETRAD
    ChordDensity.FULL   -> ChordType.FULL
    ChordDensity.AUTO   -> when (channel) {
        0    -> ChordType.TRIAD   // Guitar: tight triads by default
        2, 3 -> ChordType.FULL   // Piano, Organ: full voicings
        else -> ChordType.FULL
    }
}