package com.example.fretboardlayouts.theory

// made by Claude 17/08/2026 — genre-aware chord quality styling.
//
// Sits between "look up a progression" and "resolve it against a key":
//
//   Progressions.ALL[name]        -> List<ChordSlot>     (harmonic skeleton)
//   applyGenreChordStyle(...)     -> List<ChordSlot>     (genreQualityOverride set per genre)  <-- THIS FILE
//   [future: per-chord override]  -> List<ChordSlot>     (userQualityOverride wins last)
//   resolveProgression(key, ...)  -> List<ResolvedChord> (final pitches via slot.effectiveQuality)
//
// Classifies each slot by its current chord FAMILY (major/minor/dominant/diminished/
// augmented/sus) rather than raw scale degree, so the same rule works correctly in
// major or minor keys and correctly handles borrowed chords (bVII, bVI, etc.).

/** Broad harmonic family a chord quality belongs to, used to drive genre-style rules. */
enum class ChordFamily {
    MAJOR_FAMILY,
    MINOR_FAMILY,
    DOMINANT_FAMILY,
    DIMINISHED_FAMILY,
    AUGMENTED_FAMILY,
    SUS_FAMILY
}

/** Classifies a ChordQuality into its broad harmonic family. Exhaustive over all 25 qualities. */
fun ChordQuality.family(): ChordFamily = when (this) {
    ChordQuality.MAJOR, ChordQuality.MAJOR7, ChordQuality.SIX,
    ChordQuality.MAJOR9, ChordQuality.ADD9, ChordQuality.SIX_NINE
        -> ChordFamily.MAJOR_FAMILY

    ChordQuality.MINOR, ChordQuality.MINOR7, ChordQuality.MINOR_SIX,
    ChordQuality.MINOR9, ChordQuality.MINOR_ADD9, ChordQuality.MINOR_MAJOR7
        -> ChordFamily.MINOR_FAMILY

    ChordQuality.DOMINANT7, ChordQuality.DOMINANT9, ChordQuality.DOMINANT11,
    ChordQuality.DOMINANT13, ChordQuality.DOMINANT7_SUS4
        -> ChordFamily.DOMINANT_FAMILY

    ChordQuality.DIMINISHED, ChordQuality.DIMINISHED7, ChordQuality.MINOR7_FLAT5
        -> ChordFamily.DIMINISHED_FAMILY

    ChordQuality.AUGMENTED, ChordQuality.AUGMENTED7, ChordQuality.MAJOR7_SHARP5
        -> ChordFamily.AUGMENTED_FAMILY

    ChordQuality.SUS2, ChordQuality.SUS4
        -> ChordFamily.SUS_FAMILY
}

/**
 * A named, selectable rule for how a genre re-colours chord qualities.
 * apply() returns null to mean "no genre opinion — keep the base quality as-is."
 * This null is stored in ChordSlot.genreQualityOverride, which the three-tier
 * effectiveQuality chain treats as "not set."
 */
sealed interface GenreChordRule {
    val id: String            // stable key — used for dropdown state
    val displayName: String   // shown in the UI dropdown
    fun apply(slot: ChordSlot): ChordQuality?  // null = no override
}

/**
 * Maps by harmonic FUNCTION, not raw scale degree.
 * e.g. Jazz: major-family → Maj7, minor-family → m7, dominant-family → 7.
 * Families not present in [familyMap] return null (chord left unchanged).
 */
data class FunctionAwareRule(
    override val id: String,
    override val displayName: String,
    val familyMap: Map<ChordFamily, ChordQuality>
) : GenreChordRule {
    override fun apply(slot: ChordSlot): ChordQuality? =
        familyMap[slot.effectiveQuality.family()] // null if this family has no mapping
}

/**
 * Forces ONE quality onto every slot, or only slots matching [appliesTo].
 * e.g. Blues: every chord → Dominant7 (no filter).
 * e.g. "Dominant V7 only": just the dominant-family slot gets a 7th, rest return null.
 */
data class BlanketRule(
    override val id: String,
    override val displayName: String,
    val targetQuality: ChordQuality,
    val appliesTo: (ChordSlot) -> Boolean = { true }
) : GenreChordRule {
    override fun apply(slot: ChordSlot): ChordQuality? =
        if (appliesTo(slot)) targetQuality else null
}

/** No change — "as written." Stores null in genreQualityOverride, base quality wins. */
object AsWrittenRule : GenreChordRule {
    override val id = "as_written"
    override val displayName = "As Written"
    override fun apply(slot: ChordSlot): ChordQuality? = null
}

/**
 * Registry of available chord styles per genre. First entry in each list is the
 * default applied automatically when that genre is selected; the rest populate
 * a dropdown for manual override — same pattern as allGuitarPresets / allPickingPresets.
 * Genres absent from the map fall back to AsWrittenRule via defaultFor().
 */
object GenreChordStyles {

    private val jazz7ths = FunctionAwareRule(
        id = "jazz_7ths",
        displayName = "Jazz 7ths (Δ⁷ / m⁷ / 7)",
        familyMap = mapOf(
            ChordFamily.MAJOR_FAMILY      to ChordQuality.MAJOR7,
            ChordFamily.MINOR_FAMILY      to ChordQuality.MINOR7,
            ChordFamily.DOMINANT_FAMILY   to ChordQuality.DOMINANT7,
            ChordFamily.DIMINISHED_FAMILY to ChordQuality.MINOR7_FLAT5
        )
    )

    private val blues7ths = BlanketRule(
        id = "blues_7ths",
        displayName = "Blues 7ths (all Dominant 7)",
        targetQuality = ChordQuality.DOMINANT7
    )

    // Only the dominant-family chord (the V) gets a 7th by default.
    // I, IV, vi stay as plain triads. Full jazz-style 7ths still available
    // as a manual dropdown option for genres that want it.
    private val dominantVOnly = BlanketRule(
        id = "dominant_v_only",
        displayName = "Dominant V7 Only",
        targetQuality = ChordQuality.DOMINANT7,
        appliesTo = { slot -> slot.effectiveQuality.family() == ChordFamily.DOMINANT_FAMILY }
    )

    val byGenre: Map<Genre, List<GenreChordRule>> = mapOf(
        Genre.JAZZ    to listOf(jazz7ths, blues7ths, AsWrittenRule),
        Genre.BLUES   to listOf(blues7ths, dominantVOnly, AsWrittenRule),
        Genre.ROCK    to listOf(dominantVOnly, jazz7ths, AsWrittenRule),
        Genre.COUNTRY to listOf(AsWrittenRule, dominantVOnly),
        Genre.FUNK    to listOf(AsWrittenRule, dominantVOnly, jazz7ths),
        Genre.DISCO   to listOf(AsWrittenRule, dominantVOnly),
        Genre.SKA     to listOf(AsWrittenRule, dominantVOnly),
        Genre.REGGAE  to listOf(AsWrittenRule, dominantVOnly)
    )

    /** The rule applied automatically when [genre] is first selected. */
    fun defaultFor(genre: Genre): GenreChordRule = byGenre[genre]?.firstOrNull() ?: AsWrittenRule

    /** All available rules for [genre], for populating the style dropdown. */
    fun stylesFor(genre: Genre): List<GenreChordRule> = byGenre[genre] ?: listOf(AsWrittenRule)
}

/**
 * Applies [rule] to every slot in [slots], writing the result into
 * ChordSlot.genreQualityOverride. A null result from rule.apply() means
 * "no genre opinion" — the base quality will win in the effectiveQuality chain.
 */
fun applyGenreChordStyle(slots: List<ChordSlot>, rule: GenreChordRule): List<ChordSlot> =
    slots.map { slot -> slot.copy(genreQualityOverride = rule.apply(slot)) }
