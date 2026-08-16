package com.example.fretboardlayouts.theory

/**
 * A single slot in a progression: which scale degree (1-7) and what
 * quality it carries. Quality is taken from standard roman-numeral
 * convention: UPPERCASE = major-family, lowercase = minor-family,
 * "7" = seventh chord, "°"/"dim" = diminished.
 */
// made by Gemini 27/06: Added rootOffset to support Lydian (#4), Mixolydian (b7), etc.
data class ChordSlot(
    val degree: Int,
    val quality: ChordQuality,
    val romanLabel: String,
    val rootOffset: Int = 0, // e.g. -1 for flat, +1 for sharp
    val genreQualityOverride: ChordQuality? = null, // NEW made by Claude 17/08/2026 — set by applyGenreChordStyle(); genre styling writes here
    val userQualityOverride: ChordQuality? = null   // reserved for future pencil-icon per-chord editing; always wins
) {
    // Three-tier priority: user's explicit pick > genre style > base quality from progression // MODIFIED
    val effectiveQuality: ChordQuality get() = userQualityOverride ?: genreQualityOverride ?: quality // MODIFIED
}

private val ROMAN_TO_DEGREE = mapOf(
    "I" to 1, "i" to 1,
    "II" to 2, "ii" to 2,
    "III" to 3, "iii" to 3,
    "IV" to 4, "iv" to 4,
    "V" to 5, "v" to 5,
    "VI" to 6, "vi" to 6,
    "VII" to 7, "vii" to 7
)

// made by Gemini 27/06: Upgraded parser to handle sharps, flats, and diminished symbols
fun chordSlot(roman: String): ChordSlot {
    var working = roman.trim()
    var offset = 0

    // Handle b (flat) or # (sharp) prefix
    if (working.startsWith('b')) {
        offset = -1
        working = working.substring(1)
    } else if (working.startsWith('#')) {
        offset = 1
        working = working.substring(1)
    }

    val base = working.trimEnd('7', '°', '+', '2', '4', '9')
        .replace("sus", "").replace("add", "")

    val degree = ROMAN_TO_DEGREE[base] ?: error("Unknown roman numeral: $roman")
    val isLower = base[0].isLowerCase()
    val has7 = roman.contains("7")
    val isDim = roman.contains("°") || roman.contains("dim")
    val isAug = roman.contains("+") || roman.contains("aug")
    val isSus2 = roman.contains("sus2")
    val isSus4 = roman.contains("sus4")
    val is7Sus4 = roman.contains("7sus4")

    val quality = when {
        is7Sus4 -> ChordQuality.DOMINANT7_SUS4
        isSus2 -> ChordQuality.SUS2
        isSus4 -> ChordQuality.SUS4
        isDim && has7 -> ChordQuality.MINOR7_FLAT5
        isDim -> ChordQuality.DIMINISHED
        isAug && has7 -> ChordQuality.AUGMENTED7
        isAug -> ChordQuality.AUGMENTED
        has7 && !isLower -> ChordQuality.DOMINANT7
        has7 && isLower -> ChordQuality.MINOR7
        isLower -> ChordQuality.MINOR
        else -> ChordQuality.MAJOR
    }
    return ChordSlot(degree, quality, roman, rootOffset = offset)
}

/** Convenience: build a progression from a list of roman numeral strings */
private fun prog(vararg romans: String): List<ChordSlot> = romans.map { chordSlot(it) }

// made by Gemini 27/06: Split progressions into Major/Minor categories
object Progressions {
    val MAJOR: Map<String, List<ChordSlot>> = mapOf(
        "I - V - vi - IV (Pop/Country/Rock)" to prog("I", "V", "vi", "IV"),
        "I - IV - V (Western foundation)" to prog("I", "IV", "V"),
        "I - vi - IV - V (50's Doo-Wop)" to prog("I", "vi", "IV", "V"),
        "vi - IV - I - V" to prog("vi", "IV", "I", "V"),
        "I - V - vi - iii - IV - I - IV - V" to prog("I", "V", "vi", "iii", "IV", "I", "IV", "V"),
        "I - iii - IV - V" to prog("I", "iii", "IV", "V"),
        "IV - V - I - vi" to prog("IV", "V", "I", "vi"),
        "12 Bar Blues" to prog(
            "I7", "I7", "I7", "I7",
            "IV7", "IV7", "I7", "I7",
            "V7", "IV7", "I7", "V7"
        )
    )

    val MINOR: Map<String, List<ChordSlot>> = mapOf(
        "i - VI - III - VII (Pop Minor)" to prog("i", "VI", "III", "VII"),
        "i - bVII - bVI - V (Andalusian Cadence)" to prog("i", "bVII", "bVI", "V"),
        "i - bIII - iv - VI (Natural Minor Climb)" to prog("i", "bIII", "iv", "VI"),
        "ii° - V - I (Jazz Standard)" to prog("ii°", "V", "I"),
        "i - iv - V (Classical / Blues Minor)" to prog("i", "iv", "V"),
        "i - iv - V - iv (12 Bar Minor Blues)" to prog("i", "iv", "V", "iv")
    )

    val ALL: Map<String, List<ChordSlot>> = MAJOR + MINOR
}

/** Resolves a list of ChordSlots into actual chords for the given key */
// made by Gemini 27/06: Upgraded to be modality-aware (Major vs Minor base scale)
fun resolveProgression(key: MusicKey, slots: List<ChordSlot>): List<ResolvedChord> {
    // Diatonic scale is already modality-aware (Major or Natural Minor)
    val diatonic = diatonicScalePitchClasses(key)

    return slots.map { slot ->
        val baseRoot = diatonic[(slot.degree - 1).coerceIn(0, 6)]
        val finalRoot = (baseRoot + slot.rootOffset + 12) % 12
        ResolvedChord(finalRoot, slot.effectiveQuality, slot.romanLabel, slot.degree)
    }
}
fun validQualitiesForDegree(degree: Int, key: MusicKey): List<ChordQuality> {
    val isMajorKey = !key.isMinor
    return if (isMajorKey) {
        when (degree) {
            1 -> listOf(
                ChordQuality.MAJOR, ChordQuality.MAJOR7, ChordQuality.MAJOR9,
                ChordQuality.ADD9, ChordQuality.SIX, ChordQuality.SIX_NINE,
                ChordQuality.SUS2, ChordQuality.SUS4
            )
            2 -> listOf(
                ChordQuality.MINOR, ChordQuality.MINOR7, ChordQuality.MINOR9,
                ChordQuality.DOMINANT7, ChordQuality.SUS2, ChordQuality.SUS4
            )
            3 -> listOf(
                ChordQuality.MINOR, ChordQuality.MINOR7, ChordQuality.SUS4
            )
            4 -> listOf(
                ChordQuality.MAJOR, ChordQuality.MAJOR7, ChordQuality.ADD9,
                ChordQuality.SUS2, ChordQuality.SUS4, ChordQuality.DOMINANT7
            )
            5 -> listOf(
                ChordQuality.MAJOR, ChordQuality.DOMINANT7, ChordQuality.DOMINANT9,
                ChordQuality.DOMINANT11, ChordQuality.DOMINANT13,
                ChordQuality.AUGMENTED, ChordQuality.AUGMENTED7,
                ChordQuality.DOMINANT7_SUS4, ChordQuality.SUS2, ChordQuality.SUS4
            )
            6 -> listOf(
                ChordQuality.MINOR, ChordQuality.MINOR7, ChordQuality.MINOR9,
                ChordQuality.SUS2, ChordQuality.SUS4
            )
            7 -> listOf(
                ChordQuality.DIMINISHED, ChordQuality.MINOR7_FLAT5,
                ChordQuality.DIMINISHED7
            )
            else -> listOf(ChordQuality.MAJOR)
        }
    } else {
        when (degree) {
            1 -> listOf(
                ChordQuality.MINOR, ChordQuality.MINOR7, ChordQuality.MINOR9,
                ChordQuality.MINOR_MAJOR7, ChordQuality.MINOR_ADD9,
                ChordQuality.SUS2, ChordQuality.SUS4
            )
            2 -> listOf(
                ChordQuality.DIMINISHED, ChordQuality.MINOR7_FLAT5,
                ChordQuality.MINOR, ChordQuality.MINOR7
            )
            3 -> listOf(
                ChordQuality.MAJOR, ChordQuality.MAJOR7, ChordQuality.AUGMENTED
            )
            4 -> listOf(
                ChordQuality.MINOR, ChordQuality.MINOR7,
                ChordQuality.MAJOR, ChordQuality.DOMINANT7,
                ChordQuality.SUS2, ChordQuality.SUS4
            )
            5 -> listOf(
                ChordQuality.MINOR, ChordQuality.MINOR7,
                ChordQuality.MAJOR, ChordQuality.DOMINANT7,
                ChordQuality.DOMINANT9, ChordQuality.DOMINANT7_SUS4
            )
            6 -> listOf(
                ChordQuality.MAJOR, ChordQuality.MAJOR7, ChordQuality.ADD9
            )
            7 -> listOf(
                ChordQuality.MAJOR, ChordQuality.DOMINANT7, ChordQuality.DOMINANT9
            )
            else -> listOf(ChordQuality.MINOR)
        }
    }
}