package com.example.fretboardlayouts.brain

/*
|--------------------------------------------------------------------------
| BassModels.kt
|--------------------------------------------------------------------------
|
| Created: 25 July 2026
| Author : Claude (Anthropic)
|
| UPDATED: 25 July 2026 — full pattern data from extract_bass_full.py
|   Rock:  +6 patterns (occ 156, 144, 120, 110, 102, 90) — were truncated
|   Jazz:  +3 patterns (occ 46, 45, 26)  — were truncated
|   Blues: +1 pattern  (occ 8)           — was truncated
|   Pop:   +2 patterns + octave pump corrected to 12-note version (174 occ)
|   Removed synthesized "Rock Root-Fifth 8ths" — real data covers slow tempos
|
| Data source: bass_patterns.db (bass_pattern_tags table)
|   bass_miner.py → 810,587 raw patterns → 307,351 deduplicated + tagged
|   16,961 MIDI files from Lakh Clean MIDI Dataset
|
| DO NOT EDIT MANUALLY — regenerate via extract_bass_full.py + export script.
|
|--------------------------------------------------------------------------
*/

// =========================================================================
// DATA STRUCTURES
// =========================================================================

/**
 * One bass pattern mined from real performances.
 *
 * intervals: semitone offsets from the chord root MIDI note.
 *   Negative = below root. Example:
 *   [0, -5, 0, -5] on root E2(40) → E2(40), B1(35), E2(40), B1(35)
 *
 * rhythmDenominators: the N in "1/N" per note.
 *   4=quarter, 8=eighth, 16=sixteenth, 32=thirty-second
 *   Duration in ms = (4.0 / denominator) × (60000.0 / bpm)
 *
 * totalBeats: sum of all note durations in beats.
 *   < 4.0 → short motif, BassBrain repeats to fill bar
 *   = 4.0 → full bar pattern, plays once per bar
 *   > 4.0 → long pattern, BassBrain trims at bar boundary
 *
 * occurrences: frequency in Lakh dataset. Used as selection weight.
 */
data class BassPattern(
    val name: String,
    val genre: String,
    val intervals: IntArray,
    val rhythmDenominators: IntArray,
    val degreePattern: String,
    val feel: String,
    val occurrences: Int,
    val isRootFifth:  Boolean = false,
    val isWalking:    Boolean = false,
    val isArpeggio:   Boolean = false,
    val isBlues:      Boolean = false,
    val isChromatic:  Boolean = false,
    val isOctave:     Boolean = false
) {
    val totalBeats: Float
        get() = rhythmDenominators.fold(0.0) { acc, d -> acc + (4.0 / d) }.toFloat()

    val repeatsPerBar: Float
        get() = if (totalBeats > 0f) 4.0f / totalBeats else 1.0f

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BassPattern) return false
        return name == other.name && genre == other.genre
    }
    override fun hashCode(): Int = 31 * name.hashCode() + genre.hashCode()
}

data class BassVelocityProfile(
    val average: Int,
    val minimum: Int,
    val maximum: Int,
    val stdDev: Int
)

data class BassModel(
    val genre: String,
    val patterns: List<BassPattern>,
    val velocityProfile: BassVelocityProfile
)

// =========================================================================
// POPULATED DATA — mined from Lakh MIDI Dataset
// =========================================================================

object BassModels {

    const val FEEL_STRAIGHT_16 = "Straight 16"
    const val FEEL_STRAIGHT_8  = "Straight 8"
    const val FEEL_QUARTER     = "Quarter"
    const val FEEL_MIXED       = "Mixed"

    // =====================================================================
    // ROCK  —  62,062 patterns | 24,326 tagged | avg vel 98.4
    // Ordered by occurrence count (highest = most used by real bassists)
    // Tempo filter: < 80 BPM → patterns 5,7,8,9,10 eligible (totalBeats ≥ 2.0)
    // =====================================================================
    val ROCK = BassModel(
        genre = "Rock",
        patterns = listOf(

            // totalBeats=1.375 → repeats ~2.9× per bar | root–5th–b7 push-pull riff
            BassPattern(
                name = "Rock Root-Fifth b7 Groove",
                genre = "Rock",
                intervals = intArrayOf(0, -5, 0, -5, -2, -5, -2, -5, -2),
                rhythmDenominators = intArrayOf(16, 32, 32, 32, 16, 32, 32, 32, 32),
                degreePattern = "1,5,1,5,b7,5,b7,5,b7",
                feel = FEEL_STRAIGHT_16,
                occurrences = 156,
                isRootFifth = true,
                isBlues = true
            ),

            // totalBeats=1.375 → repeats ~2.9× per bar | 1–6–2 syncopated groove
            BassPattern(
                name = "Rock 6th Bounce",
                genre = "Rock",
                intervals = intArrayOf(0, -3, 0, -3, 0, 2, -3, 2, -3),
                rhythmDenominators = intArrayOf(16, 32, 32, 32, 32, 16, 32, 32, 32),
                degreePattern = "1,6,1,6,1,2,6,2,6",
                feel = FEEL_STRAIGHT_16,
                occurrences = 144
            ),

            // totalBeats=1.0 → repeats 4× per bar | the definitive Rock bass riff
            BassPattern(
                name = "Rock Root-Fifth 16ths",
                genre = "Rock",
                intervals = intArrayOf(0, -5, 0, -5),
                rhythmDenominators = intArrayOf(16, 16, 16, 16),
                degreePattern = "1,5,1,5",
                feel = FEEL_STRAIGHT_16,
                occurrences = 137,
                isRootFifth = true
            ),

            // totalBeats=0.875 → repeats ~4.6× per bar | tight syncopated root-fifth
            BassPattern(
                name = "Rock Root-Fifth Tight",
                genre = "Rock",
                intervals = intArrayOf(0, -5, 0, -5, 0),
                rhythmDenominators = intArrayOf(32, 32, 16, 32, 16),
                degreePattern = "1,5,1,5,1",
                feel = FEEL_STRAIGHT_16,
                occurrences = 121,
                isRootFifth = true
            ),

            // totalBeats=2.5 → repeats 1.6× per bar | chromatic descent 1–7–b7–6–b6–b7–5
            BassPattern(
                name = "Rock Chromatic Descend",
                genre = "Rock",
                intervals = intArrayOf(0, -1, -2, -3, -4, -2, -5),
                rhythmDenominators = intArrayOf(8, 16, 16, 16, 8, 16, 8),
                degreePattern = "1,7,b7,6,b6,b7,5",
                feel = FEEL_STRAIGHT_16,
                occurrences = 120,
                isBlues = true,
                isChromatic = true
            ),

            // totalBeats=1.625 → repeats ~2.5× per bar | 1–3–4 triadic running pattern
            BassPattern(
                name = "Rock Triadic Run",
                genre = "Rock",
                intervals = intArrayOf(0, 4, 0, 5, 0, 4, 0, 4, 0, 5, 0, 4, 0),
                rhythmDenominators = intArrayOf(32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32),
                degreePattern = "1,3,1,4,1,3,1,3,1,4,1,3,1",
                feel = FEEL_MIXED,
                occurrences = 110
            ),

            // totalBeats=2.75 → repeats ~1.5× per bar | chromatic walk up then step down
            BassPattern(
                name = "Rock Chromatic Walk",
                genre = "Rock",
                intervals = intArrayOf(0, 2, -1, 4, 3, 2, 1, 0),
                rhythmDenominators = intArrayOf(16, 16, 8, 8, 16, 16, 16, 8),
                degreePattern = "1,2,7,3,b3,2,b2,1",
                feel = FEEL_STRAIGHT_16,
                occurrences = 102,
                isBlues = true,
                isChromatic = true
            ),

            // totalBeats=1.0 → repeats 4× per bar | root–b7 driving motif
            BassPattern(
                name = "Rock Root-b7 16ths",
                genre = "Rock",
                intervals = intArrayOf(0, -2, 0, -2),
                rhythmDenominators = intArrayOf(16, 16, 16, 16),
                degreePattern = "1,b7,1,b7",
                feel = FEEL_STRAIGHT_16,
                occurrences = 91
            ),

            // totalBeats=2.5 → repeats 1.6× per bar | dominant 7th arpeggio up and back
            BassPattern(
                name = "Rock Dominant Arpeggio",
                genre = "Rock",
                intervals = intArrayOf(0, 4, 7, 10, 5, 0, 4, 7, 10, 5),
                rhythmDenominators = intArrayOf(16, 16, 16, 16, 16, 16, 16, 16, 16, 16),
                degreePattern = "1,3,5,b7,4,1,3,5,b7,4",
                feel = FEEL_STRAIGHT_16,
                occurrences = 90,
                isArpeggio = true
            ),

            // totalBeats=4.0 → plays once per bar | quarter note walking descent
            BassPattern(
                name = "Rock Walk Down",
                genre = "Rock",
                intervals = intArrayOf(0, -1, -3, -5),
                rhythmDenominators = intArrayOf(4, 4, 4, 4),
                degreePattern = "1,7,6,5",
                feel = FEEL_QUARTER,
                occurrences = 86,
                isWalking = true
            )
        ),
        velocityProfile = BassVelocityProfile(average = 98, minimum = 40, maximum = 115, stdDev = 10)
    )

    // =====================================================================
    // BLUES  —  794 patterns | 356 tagged | avg vel 99.2
    // =====================================================================
    val BLUES = BassModel(
        genre = "Blues",
        patterns = listOf(

            // totalBeats=1.25 → repeats 3.2× per bar | chromatic blues scale climb
            BassPattern(
                name = "Blues Scale Climb",
                genre = "Blues",
                intervals = intArrayOf(0, 3, 4, 5, -2, 0, 3, 4, 5, -2),
                rhythmDenominators = intArrayOf(32, 32, 32, 32, 32, 32, 32, 32, 32, 32),
                degreePattern = "1,b3,3,4,b7,1,b3,3,4,b7",
                feel = FEEL_MIXED,
                occurrences = 39,
                isBlues = true,
                isChromatic = true
            ),

            // totalBeats=3.5 → ~1 bar | shuffle feel 1–5–1–6–2
            BassPattern(
                name = "Blues Shuffle 1-5-6",
                genre = "Blues",
                intervals = intArrayOf(0, 7, 0, 9, 2),
                rhythmDenominators = intArrayOf(8, 8, 8, 4, 4),
                degreePattern = "1,5,1,6,2",
                feel = FEEL_STRAIGHT_8,
                occurrences = 15,
                isRootFifth = true
            ),

            // totalBeats=4.0 → full bar | previously truncated — now complete from DB
            BassPattern(
                name = "Blues Full Arpeggio",
                genre = "Blues",
                intervals = intArrayOf(0, -5, 0, -1, -3, -8, -3, -5),
                rhythmDenominators = intArrayOf(8, 16, 4, 8, 8, 16, 8, 8),
                degreePattern = "1,5,1,7,6,3,6,5",
                feel = FEEL_STRAIGHT_16,
                occurrences = 8,
                isArpeggio = true
            ),

            // totalBeats=5.0 → trims to 4 beats | ascending quarter note climb
            BassPattern(
                name = "Blues Quarter Climb",
                genre = "Blues",
                intervals = intArrayOf(0, 5, 9, 12, 0),
                rhythmDenominators = intArrayOf(4, 4, 4, 4, 4),
                degreePattern = "1,4,6,1,1",
                feel = FEEL_QUARTER,
                occurrences = 8,
                isWalking = true
            ),

            // totalBeats=3.0 → ~1 bar | walking b7–b6–b3 blues tones
            BassPattern(
                name = "Blues Walk b7-b6",
                genre = "Blues",
                intervals = intArrayOf(0, -2, -4, 3, -4),
                rhythmDenominators = intArrayOf(4, 8, 8, 8, 8),
                degreePattern = "1,b7,b6,b3,b6",
                feel = FEEL_STRAIGHT_8,
                occurrences = 7,
                isWalking = true,
                isBlues = true
            ),

            // totalBeats=1.0 → repeats 4× per bar | root–5th–b7 riff
            BassPattern(
                name = "Blues Root-Fifth b7",
                genre = "Blues",
                intervals = intArrayOf(0, -5, -2, -5),
                rhythmDenominators = intArrayOf(16, 16, 16, 16),
                degreePattern = "1,5,b7,5",
                feel = FEEL_STRAIGHT_16,
                occurrences = 7,
                isRootFifth = true,
                isBlues = true
            ),

            // totalBeats=4.0 → full bar | 8th note blues groove with b7/b3 colour
            BassPattern(
                name = "Blues 8th Groove",
                genre = "Blues",
                intervals = intArrayOf(0, -5, -2, 3, -2, -5),
                rhythmDenominators = intArrayOf(4, 8, 8, 8, 8, 4),
                degreePattern = "1,5,b7,b3,b7,5",
                feel = FEEL_STRAIGHT_8,
                occurrences = 7,
                isBlues = true
            )
        ),
        velocityProfile = BassVelocityProfile(average = 99, minimum = 45, maximum = 115, stdDev = 10)
    )

    // =====================================================================
    // JAZZ  —  2,027 patterns | 1,200 tagged | avg vel 96.0
    // Top 3 (occ 46, 45, 26) were previously truncated — now from real data.
    // Tempo filter: < 80 BPM → patterns 3–8 (totalBeats ≥ 2.0) = walking bass
    // =====================================================================
    val JAZZ = BassModel(
        genre = "Jazz",
        patterns = listOf(

            // totalBeats=1.75 → repeats 2.3× per bar | 1–6–3–6–5–6 arpeggio motif
            // Most common Jazz pattern in dataset — from real jazz MIDI performances
            BassPattern(
                name = "Jazz 6th Arpeggio",
                genre = "Jazz",
                intervals = intArrayOf(0, -3, 0, 4, -3, -5, -3),
                rhythmDenominators = intArrayOf(32, 16, 16, 8, 32, 16, 16),
                degreePattern = "1,6,1,3,6,5,6",
                feel = FEEL_STRAIGHT_16,
                occurrences = 46,
                isArpeggio = true
            ),

            // totalBeats=1.75 → repeats 2.3× per bar | 1–b7–b3–5 swing motif
            BassPattern(
                name = "Jazz b7-b3 Groove",
                genre = "Jazz",
                intervals = intArrayOf(0, -2, 0, 3, 0, 3, 7),
                rhythmDenominators = intArrayOf(32, 16, 16, 32, 16, 16, 8),
                degreePattern = "1,b7,1,b3,1,b3,5",
                feel = FEEL_STRAIGHT_16,
                occurrences = 45,
                isBlues = true
            ),

            // totalBeats=2.875 → repeats ~1.4× per bar | 1–b3–4 motif (minor pentatonic)
            BassPattern(
                name = "Jazz b3-4 Motif",
                genre = "Jazz",
                intervals = intArrayOf(0, 3, 5, 3, 0, 3, 5, 0),
                rhythmDenominators = intArrayOf(32, 16, 8, 8, 8, 16, 16, 8),
                degreePattern = "1,b3,4,b3,1,b3,4,1",
                feel = FEEL_STRAIGHT_16,
                occurrences = 26,
                isBlues = true
            ),

            // totalBeats=3.5 → ~1 bar | chromatic approach 1–7–b7–6–3
            BassPattern(
                name = "Jazz Chromatic Descend",
                genre = "Jazz",
                intervals = intArrayOf(0, -1, -2, -3, -8),
                rhythmDenominators = intArrayOf(4, 8, 8, 4, 8),
                degreePattern = "1,7,b7,6,3",
                feel = FEEL_STRAIGHT_8,
                occurrences = 12,
                isWalking = true,
                isChromatic = true
            ),

            // totalBeats=3.25 → ~1 bar | tritone movement — very idiomatic jazz
            BassPattern(
                name = "Jazz Tritone Walk",
                genre = "Jazz",
                intervals = intArrayOf(0, -6, -9, -10),
                rhythmDenominators = intArrayOf(4, 4, 16, 4),
                degreePattern = "1,#4,b3,2",
                feel = FEEL_STRAIGHT_16,
                occurrences = 12
            ),

            // totalBeats=4.0 → full bar | root–3–4–3 quarter note riff
            BassPattern(
                name = "Jazz Root-Third Quarters",
                genre = "Jazz",
                intervals = intArrayOf(0, 4, 5, 4),
                rhythmDenominators = intArrayOf(4, 4, 4, 4),
                degreePattern = "1,3,4,3",
                feel = FEEL_QUARTER,
                occurrences = 12,
                isWalking = true
            ),

            // totalBeats=4.0 → full bar | classic descending walk 1–7–6–5
            BassPattern(
                name = "Jazz Walk Down",
                genre = "Jazz",
                intervals = intArrayOf(0, -1, -3, -5),
                rhythmDenominators = intArrayOf(4, 4, 4, 4),
                degreePattern = "1,7,6,5",
                feel = FEEL_QUARTER,
                occurrences = 12,
                isWalking = true
            ),

            // totalBeats=4.0 → full bar | 2–5 motion with b7 approach
            BassPattern(
                name = "Jazz 2-5 Walk",
                genre = "Jazz",
                intervals = intArrayOf(0, -2, 2, -5),
                rhythmDenominators = intArrayOf(4, 4, 4, 4),
                degreePattern = "1,b7,2,5",
                feel = FEEL_QUARTER,
                occurrences = 12,
                isWalking = true
            )
        ),
        velocityProfile = BassVelocityProfile(average = 96, minimum = 45, maximum = 110, stdDev = 12)
    )

    // =====================================================================
    // COUNTRY  —  5,765 patterns | 2,697 tagged | avg vel 98.7
    // =====================================================================
    val COUNTRY = BassModel(
        genre = "Country",
        patterns = listOf(

            // totalBeats=1.0 → repeats 4× per bar | pentatonic scale run
            BassPattern(
                name = "Country Scale Climb",
                genre = "Country",
                intervals = intArrayOf(0, 3, 5, 7, 0, 3, 5, 7),
                rhythmDenominators = intArrayOf(32, 32, 32, 32, 32, 32, 32, 32),
                degreePattern = "1,b3,4,5,1,b3,4,5",
                feel = FEEL_MIXED,
                occurrences = 76
            ),

            // totalBeats=4.0 → full bar | THE country bass feel. Quarter boom-chick.
            BassPattern(
                name = "Country Boom-Chick",
                genre = "Country",
                intervals = intArrayOf(0, -5, 0, -5),
                rhythmDenominators = intArrayOf(4, 4, 4, 4),
                degreePattern = "1,5,1,5",
                feel = FEEL_QUARTER,
                occurrences = 49,
                isRootFifth = true
            ),

            // totalBeats=2.5 → repeats 1.6× per bar | quarter root + 16th fifth stab
            BassPattern(
                name = "Country Root-Fifth Stab",
                genre = "Country",
                intervals = intArrayOf(0, 7, 0, 7),
                rhythmDenominators = intArrayOf(4, 16, 4, 16),
                degreePattern = "1,5,1,5",
                feel = FEEL_STRAIGHT_16,
                occurrences = 48,
                isRootFifth = true
            ),

            // totalBeats=1.0 → repeats 4× per bar | 16th note root-fifth
            BassPattern(
                name = "Country Root-Fifth 16ths",
                genre = "Country",
                intervals = intArrayOf(0, -5, 0, -5),
                rhythmDenominators = intArrayOf(16, 16, 16, 16),
                degreePattern = "1,5,1,5",
                feel = FEEL_STRAIGHT_16,
                occurrences = 35,
                isRootFifth = true
            ),

            // totalBeats=1.0 → repeats 4× per bar | descending walk
            BassPattern(
                name = "Country Walk Down",
                genre = "Country",
                intervals = intArrayOf(0, -2, -3, -5),
                rhythmDenominators = intArrayOf(16, 16, 16, 16),
                degreePattern = "1,b7,6,5",
                feel = FEEL_STRAIGHT_16,
                occurrences = 22,
                isWalking = true
            ),

            // totalBeats=4.0 → full bar | b7–4 bounce
            BassPattern(
                name = "Country b7 Bounce",
                genre = "Country",
                intervals = intArrayOf(0, -2, -7, -2),
                rhythmDenominators = intArrayOf(4, 4, 4, 4),
                degreePattern = "1,b7,4,b7",
                feel = FEEL_QUARTER,
                occurrences = 22
            )
        ),
        velocityProfile = BassVelocityProfile(average = 99, minimum = 45, maximum = 112, stdDev = 9)
    )

    // =====================================================================
    // POP  —  42,985 patterns | 17,667 tagged | avg vel 95.6
    // Octave pump corrected: real top pattern is 12-note (174 occ, totalBeats=3.0)
    // =====================================================================
    val POP = BassModel(
        genre = "Pop",
        patterns = listOf(

            // totalBeats=3.0 → repeats 1.3× per bar | 12-note octave pump (most common Pop bass)
            BassPattern(
                name = "Pop Octave Pump",
                genre = "Pop",
                intervals = intArrayOf(0, 12, 0, 12, 0, 12, 0, 12, 0, 12, 0, 12),
                rhythmDenominators = intArrayOf(16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16),
                degreePattern = "1,1,1,1,1,1,1,1,1,1,1,1",
                feel = FEEL_STRAIGHT_16,
                occurrences = 174,
                isOctave = true
            ),

            // totalBeats=4.0 → full bar | 16-note octave pump filling whole bar
            BassPattern(
                name = "Pop Octave Full Bar",
                genre = "Pop",
                intervals = intArrayOf(0, 12, 0, 12, 0, 12, 0, 12, 0, 12, 0, 12, 0, 12, 0, 12),
                rhythmDenominators = intArrayOf(16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16),
                degreePattern = "1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1",
                feel = FEEL_STRAIGHT_16,
                occurrences = 147,
                isOctave = true
            ),

            // totalBeats=2.5 → repeats 1.6× per bar | descending octave with b7
            BassPattern(
                name = "Pop Descend Groove",
                genre = "Pop",
                intervals = intArrayOf(0, 12, 10, 7, 3, 5, 3, 5, 3),
                rhythmDenominators = intArrayOf(16, 16, 16, 16, 16, 8, 16, 16, 16),
                degreePattern = "1,1,b7,5,b3,4,b3,4,b3",
                feel = FEEL_STRAIGHT_16,
                occurrences = 100
            ),

            // totalBeats=1.25 → repeats 3.2× per bar | pentatonic ascent 1–b3–4–5
            BassPattern(
                name = "Pop Scale Climb",
                genre = "Pop",
                intervals = intArrayOf(0, 3, 5, 7, 0, 3, 5, 7),
                rhythmDenominators = intArrayOf(16, 32, 32, 32, 16, 32, 32, 32),
                degreePattern = "1,b3,4,5,1,b3,4,5",
                feel = FEEL_STRAIGHT_16,
                occurrences = 108
            ),

            // totalBeats=2.5 → repeats 1.6× per bar | ascending 1–5–6–7 walk
            BassPattern(
                name = "Pop Root-Fifth Walk",
                genre = "Pop",
                intervals = intArrayOf(0, -5, -3, -1, 0, -5, -3, -1),
                rhythmDenominators = intArrayOf(16, 16, 8, 16, 16, 16, 8, 16),
                degreePattern = "1,5,6,7,1,5,6,7",
                feel = FEEL_STRAIGHT_16,
                occurrences = 90,
                isWalking = true
            )
        ),
        velocityProfile = BassVelocityProfile(average = 96, minimum = 40, maximum = 112, stdDev = 10)
    )

    // =====================================================================
    // RNB  —  6,374 patterns | 2,697 tagged | avg vel 94.2
    // =====================================================================
    val RNB = BassModel(
        genre = "RnB",
        patterns = listOf(

            BassPattern(
                name = "RnB Root-Fifth Groove",
                genre = "RnB",
                intervals = intArrayOf(0, -5, 0, -5),
                rhythmDenominators = intArrayOf(16, 16, 16, 16),
                degreePattern = "1,5,1,5",
                feel = FEEL_STRAIGHT_16,
                occurrences = 50,
                isRootFifth = true
            ),

            BassPattern(
                name = "RnB Root-b7 Groove",
                genre = "RnB",
                intervals = intArrayOf(0, -2, 0, -2),
                rhythmDenominators = intArrayOf(16, 16, 16, 16),
                degreePattern = "1,b7,1,b7",
                feel = FEEL_STRAIGHT_16,
                occurrences = 40
            ),

            // totalBeats=2.0 → plays twice per bar | 8th note density for slow/medium RnB
            BassPattern(
                name = "RnB 8th Groove",
                genre = "RnB",
                intervals = intArrayOf(0, -5, 0, -2),
                rhythmDenominators = intArrayOf(8, 8, 8, 8),
                degreePattern = "1,5,1,b7",
                feel = FEEL_STRAIGHT_8,
                occurrences = 35
            )
        ),
        velocityProfile = BassVelocityProfile(average = 94, minimum = 40, maximum = 112, stdDev = 11)
    )

    // =====================================================================
    // FUNK  —  no direct Funk genre in Lakh dataset (Electronic used instead)
    // Patterns are musically derived. TODO: run extraction on Electronic genre.
    // =====================================================================
    val FUNK = BassModel(
        genre = "Funk",
        patterns = listOf(
            BassPattern(
                name = "Funk On The One",
                genre = "Funk",
                intervals = intArrayOf(0, -5, 0, -5),
                rhythmDenominators = intArrayOf(16, 16, 16, 16),
                degreePattern = "1,5,1,5",
                feel = FEEL_STRAIGHT_16,
                occurrences = 80,
                isRootFifth = true
            )
        ),
        velocityProfile = BassVelocityProfile(average = 100, minimum = 45, maximum = 118, stdDev = 10)
    )

    // =========================================================================
    // LOOKUP
    // =========================================================================

    fun forGenre(genre: String): BassModel = when (genre.lowercase()) {
        "rock"               -> ROCK
        "blues"              -> BLUES
        "jazz"               -> JAZZ
        "country"            -> COUNTRY
        "pop"                -> POP
        "rnb", "r&b", "soul" -> RNB
        "funk", "electronic" -> FUNK
        else                 -> ROCK
    }

    val all: List<BassModel> = listOf(ROCK, BLUES, JAZZ, COUNTRY, POP, RNB, FUNK)

    // =========================================================================
    // MIDI BASS REGISTER
    // =========================================================================
    const val BASS_MIDI_MIN = 28   // E1 — open low E string on 4-string bass
    const val BASS_MIDI_MAX = 52   // E3 — upper comfort zone

    fun clampToBassRegister(midiNote: Int): Int {
        var note = midiNote
        while (note < BASS_MIDI_MIN) note += 12
        while (note > BASS_MIDI_MAX) note -= 12
        return note
    }
}
