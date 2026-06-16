package com.example.fretboardlayouts.theory

/** A single highlighted position on the fretboard */
data class FretboardPosition(val stringIndex: Int, val fret: Int, val page: Int, val pitchClass: Int)

/** Generates the static grey-box scale overlay (constant for the whole jam) */
fun generateScaleOverlay(key: MusicKey, scaleType: ScaleType): List<FretboardPosition> {
    val scalePcs = overlayScalePitchClasses(key, scaleType)
    val result = mutableListOf<FretboardPosition>()
    for (stringIndex in 0..5) {
        for (fret in 0..MAX_FRET) {
            val pc = pitchClassAt(stringIndex, fret)
            if (pc in scalePcs) {
                result.add(FretboardPosition(stringIndex, fret, pageForFret(fret), pc))
            }
        }
    }
    return result
}

enum class ChordOverlayMode {
    ALL_CHORD_TONES,
    ARPEGGIO,          // same positions as ALL_CHORD_TONES, but consumer renders them sequentially
    TRIAD_STRINGS_123, // high E, B, G
    TRIAD_STRINGS_234, // B, G, D
    TRIAD_STRINGS_345, // G, D, A
    TRIAD_STRINGS_456, // D, A, low E
    CUSTOM
}

private val TRIAD_STRING_SETS = mapOf(
    ChordOverlayMode.TRIAD_STRINGS_123 to setOf(0, 1, 2),
    ChordOverlayMode.TRIAD_STRINGS_234 to setOf(1, 2, 3),
    ChordOverlayMode.TRIAD_STRINGS_345 to setOf(2, 3, 4),
    ChordOverlayMode.TRIAD_STRINGS_456 to setOf(3, 4, 5)
)

/** One highlighted chord-tone position, flagged with whether it sits inside the grey scale boxes */
data class ChordTonePosition(
    val stringIndex: Int,
    val fret: Int,
    val page: Int,
    val pitchClass: Int,
    val inScaleOverlay: Boolean,
    val isRoot: Boolean
)

/**
 * Generates the chord-tone overlay for ONE chord.
 * [scalePcs] is the same pitch-class set used for the grey boxes, so we can
 * flag whether each chord tone falls inside or outside them.
 */
fun generateChordToneOverlay(
    chord: ResolvedChord,
    mode: ChordOverlayMode,
    scalePcs: Set<Int>,
    customStringFilter: Set<Int>? = null
): List<ChordTonePosition> {
    val targetPcs = if (mode == ChordOverlayMode.ALL_CHORD_TONES || mode == ChordOverlayMode.ARPEGGIO) {
        chord.chordTonePitchClasses
    } else {
        chord.triadPitchClasses
    }.toSet()

    val stringFilter: Set<Int>? = when (mode) {
        ChordOverlayMode.CUSTOM -> customStringFilter
        in TRIAD_STRING_SETS.keys -> TRIAD_STRING_SETS[mode]
        else -> null // null = all strings
    }

    val result = mutableListOf<ChordTonePosition>()
    // Sort by string index descending (low E to high E) then fret ascending
    for (stringIndex in 5 downTo 0) {
        if (stringFilter != null && stringIndex !in stringFilter) continue
        for (fret in 0..MAX_FRET) {
            val pc = pitchClassAt(stringIndex, fret)
            if (pc in targetPcs) {
                result.add(
                    ChordTonePosition(
                        stringIndex = stringIndex,
                        fret = fret,
                        page = pageForFret(fret),
                        pitchClass = pc,
                        inScaleOverlay = pc in scalePcs,
                        isRoot = pc == chord.rootPitchClass
                    )
                )
            }
        }
    }
    return result
}
