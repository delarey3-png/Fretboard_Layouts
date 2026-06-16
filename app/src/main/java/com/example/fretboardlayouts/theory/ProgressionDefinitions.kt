package com.example.fretboardlayouts.theory

/**
 * A single slot in a progression: which scale degree (1-7) and what
 * quality it carries. Quality is taken from standard roman-numeral
 * convention: UPPERCASE = major-family, lowercase = minor-family,
 * "7" = seventh chord, "°"/"dim" = diminished.
 */
data class ChordSlot(val degree: Int, val quality: ChordQuality, val romanLabel: String)

private val ROMAN_TO_DEGREE = mapOf(
    "I" to 1, "i" to 1,
    "II" to 2, "ii" to 2,
    "III" to 3, "iii" to 3,
    "IV" to 4, "iv" to 4,
    "V" to 5, "v" to 5,
    "VI" to 6, "vi" to 6,
    "VII" to 7, "vii" to 7
)

/** Builds a ChordSlot from a roman numeral string like "I", "vi", "V7", "vii°" */
fun chordSlot(roman: String): ChordSlot {
    val base = roman.trimEnd('7', '°', '+')
    val degree = ROMAN_TO_DEGREE[base] ?: error("Unknown roman numeral: $roman")
    val isLower = base[0].isLowerCase()
    val has7 = roman.contains("7")
    val isDim = roman.contains("°") || roman.contains("dim")
    val isAug = roman.contains("+")

    val quality = when {
        isDim && has7 -> ChordQuality.MINOR7_FLAT5
        isDim -> ChordQuality.DIMINISHED
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
    val diatonic = diatonicScalePitchClasses(key) // 7 notes
    return slots.map { slot ->
        val root = diatonic[(slot.degree - 1).coerceIn(0, 6)]
        ResolvedChord(root, slot.quality, slot.romanLabel, slot.degree)
    }
}
