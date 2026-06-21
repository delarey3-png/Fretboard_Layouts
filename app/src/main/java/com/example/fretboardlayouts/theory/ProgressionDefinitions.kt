package com.example.fretboardlayouts.theory

/**
 * A single slot in a progression: which scale degree (1-7) and what
 * quality it carries. Quality is taken from standard roman-numeral
 * convention: UPPERCASE = major-family, lowercase = minor-family,
 * "7" = seventh chord, "°"/"dim" = diminished.
 */
data class ChordSlot(
    val degree: Int,
    val quality: ChordQuality,
    val romanLabel: String,
    val userQualityOverride: ChordQuality? = null
) {
    val effectiveQuality: ChordQuality get() = userQualityOverride ?: quality
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
fun chordSlot(roman: String): ChordSlot {
    val base = roman.trimEnd('7', '°', '+', '2', '4', '9')
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
    return ChordSlot(degree, quality, roman)
}

/** Convenience: build a progression from a list of roman numeral strings */
private fun prog(vararg romans: String): List<ChordSlot> = romans.map { chordSlot(it) }

object Progressions {
    val ALL: Map<String, List<ChordSlot>> = mapOf(
        "I-IV-VI" to prog("I", "IV", "VI"),
        "I-V-vi-IV (Pop/Country/Rock)" to prog("I", "V", "vi", "IV"),
        "I-IV-V (Western foundation)" to prog("I", "IV", "V"),
        "I-vi-IV-V (50's Doo-Wop)" to prog("I", "vi", "IV", "V"),
        "vi-IV-I-V" to prog("vi", "IV", "I", "V"),
        "I-V-vi-iii-IV-I-IV-V" to prog("I", "V", "vi", "iii", "IV", "I", "IV", "V"),
        "I-iii-IV-V" to prog("I", "iii", "IV", "V"),
        "IV-V-I-vi" to prog("IV", "V", "I", "vi"),
        "12 Bar Blues" to prog(
            "I7", "I7", "I7", "I7",
            "IV7", "IV7", "I7", "I7",
            "V7", "IV7", "I7", "I7"
        )
        // "Custom" handled separately in the UI layer - user builds their own List<ChordSlot>
    )
}

/** Resolves a list of ChordSlots into actual chords for the given key */
fun resolveProgression(key: MusicKey, slots: List<ChordSlot>): List<ResolvedChord> {
    val diatonic = diatonicScalePitchClasses(key)
    return slots.map { slot ->
        val root = diatonic[(slot.degree - 1).coerceIn(0, 6)]
        ResolvedChord(root, slot.effectiveQuality, slot.romanLabel, slot.degree)
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