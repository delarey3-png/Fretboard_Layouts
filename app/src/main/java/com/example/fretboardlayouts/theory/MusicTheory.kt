package com.example.fretboardlayouts.theory

// Pitch classes 0-11, C=0, C#=1, D=2 ... B=11
val NOTE_NAMES = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

data class MusicKey(val rootPitchClass: Int, val isMinor: Boolean) {
    val name: String get() = NOTE_NAMES[rootPitchClass] + if (isMinor) " Minor" else " Major"

    companion object {
        /** Parses dropdown strings like "C Major", "A# Minor", "Db Minor" */
        fun fromString(s: String): MusicKey {
            val parts = s.trim().split(" ")
            val rootName = parts[0]
            val isMinor = parts.getOrNull(1)?.equals("Minor", ignoreCase = true) == true
            val root = NOTE_NAMES.indexOf(rootName.uppercase().replace("DB","C#").replace("EB","D#")
                .replace("GB","F#").replace("AB","G#").replace("BB","A#"))
            require(root >= 0) { "Unknown root note: $rootName" }
            return MusicKey(root, isMinor)
        }
    }
}

enum class ScaleType { FULL, PENTATONIC }

object ScaleIntervals {
    val MAJOR = listOf(0, 2, 4, 5, 7, 9, 11)
    val NATURAL_MINOR = listOf(0, 2, 3, 5, 7, 8, 10)
    val MAJOR_PENTATONIC = listOf(0, 2, 4, 7, 9)
    val MINOR_PENTATONIC = listOf(0, 3, 5, 7, 10)
}

/**
 * The 7-note "theory scale" used to derive chord roots from scale degrees.
 * This is ALWAYS 7 notes (major or natural minor) regardless of what the
 * user picked for the visual scale overlay.
 */
fun diatonicScalePitchClasses(key: MusicKey): List<Int> {
    val intervals = if (key.isMinor) ScaleIntervals.NATURAL_MINOR else ScaleIntervals.MAJOR
    return intervals.map { (key.rootPitchClass + it) % 12 }
}

/**
 * The scale used for the GREY BOX overlay - depends on the user's
 * "Scale overlay" dropdown (Full scale vs Pentatonic).
 */
fun overlayScalePitchClasses(key: MusicKey, scaleType: ScaleType): Set<Int> {
    val intervals = when {
        scaleType == ScaleType.FULL && !key.isMinor -> ScaleIntervals.MAJOR
        scaleType == ScaleType.FULL && key.isMinor -> ScaleIntervals.NATURAL_MINOR
        scaleType == ScaleType.PENTATONIC && !key.isMinor -> ScaleIntervals.MAJOR_PENTATONIC
        else -> ScaleIntervals.MINOR_PENTATONIC
    }
    return intervals.map { (key.rootPitchClass + it) % 12 }.toSet()
}

enum class ChordQuality(val intervals: List<Int>, val symbol: String) {
    MAJOR(listOf(0, 4, 7), ""),
    MINOR(listOf(0, 3, 7), "m"),
    DIMINISHED(listOf(0, 3, 6), "dim"),
    AUGMENTED(listOf(0, 4, 8), "aug"),
    MAJOR7(listOf(0, 4, 7, 11), "maj7"),
    MINOR7(listOf(0, 3, 7, 10), "m7"),
    DOMINANT7(listOf(0, 4, 7, 10), "7"),
    MINOR7_FLAT5(listOf(0, 3, 6, 10), "m7b5"),
    DIMINISHED7(listOf(0, 3, 6, 9), "dim7")
}

/** A fully resolved chord: which notes (pitch classes) make it up, and its display name */
data class ResolvedChord(
    val rootPitchClass: Int,
    val quality: ChordQuality,
    val romanLabel: String,   // e.g. "IV", "ii", "V7" - for on-screen display
    val degree: Int           // 1 through 7
) {
    val name: String get() = NOTE_NAMES[rootPitchClass] + quality.symbol
    val chordTonePitchClasses: List<Int> get() = quality.intervals.map { (rootPitchClass + it) % 12 }
    /** First 3 chord tones (root, 3rd, 5th) - used for "triad-only" overlays even on 7th chords */
    val triadPitchClasses: List<Int> get() = chordTonePitchClasses.take(3)
}
