package com.example.fretboardlayouts.theory

// made by Claude 17/08/2026 — genre-aware chord quality styling.
// MODIFIED made by Claude 19/08/2026 — fixed degree-vs-quality bug:
//   FunctionAwareRule and dominantVOnly previously checked slot.effectiveQuality.family()
//   to identify the dominant chord. Since V starts as plain MAJOR quality (correct),
//   it was indistinguishable from I and IV. jazz7ths incorrectly returned MAJOR7 for V;
//   dominantVOnly's appliesTo never fired. Fixed via isDominantFunction() which checks
//   slot.degree alongside quality, so degree 5 major is correctly identified as dominant.

/** Broad harmonic family a chord quality belongs to. */
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
 * Returns true if this slot functions as the dominant in its progression context.
 *
 * Checks BOTH degree and current base quality:
 *   - Degree 5 + major or already-dominant quality → dominant function (V or V7)
 *   - Degree 5 + minor quality → NOT dominant function (natural minor v — leave it alone)
 *   - Degree 7 + diminished quality → dominant function (vii°, leading-tone chord)
 *
 * Using slot.quality (not slot.effectiveQuality) is intentional: we're deciding
 * what genre override TO APPLY, so we must read the base quality before any override.
 * MODIFIED made by Claude 19/08/2026
 */
fun isDominantFunction(slot: ChordSlot): Boolean =
    (slot.degree == 5 &&
            slot.quality.family() in setOf(ChordFamily.MAJOR_FAMILY, ChordFamily.DOMINANT_FAMILY)) ||
            (slot.degree == 7 && slot.quality.family() == ChordFamily.DIMINISHED_FAMILY)

// ── Rule types ────────────────────────────────────────────────────

sealed interface GenreChordRule {
    val id: String
    val displayName: String
    fun apply(slot: ChordSlot): ChordQuality?
}

/**
 * Maps by harmonic FUNCTION.
 * Degree-aware: dominant function (degree 5 major, degree 7 dim) routes to
 * DOMINANT_FAMILY mapping regardless of the chord's current quality family.
 * All other slots route by their quality family as before.
 * MODIFIED made by Claude 19/08/2026 — was purely quality-family-based
 */
data class FunctionAwareRule(
    override val id: String,
    override val displayName: String,
    val familyMap: Map<ChordFamily, ChordQuality>
) : GenreChordRule {
    override fun apply(slot: ChordSlot): ChordQuality? =
        if (isDominantFunction(slot))
            familyMap[ChordFamily.DOMINANT_FAMILY]   // V always maps to the dominant entry
        else
            familyMap[slot.quality.family()]         // use base quality, not effectiveQuality
}

/**
 * Forces ONE quality onto every slot matching [appliesTo].
 * MODIFIED made by Claude 19/08/2026 — dominantVOnly now uses isDominantFunction()
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

/** No change — "as written." */
object AsWrittenRule : GenreChordRule {
    override val id = "as_written"
    override val displayName = "As Written"
    override fun apply(slot: ChordSlot): ChordQuality? = null
}

// ── Rule definitions ──────────────────────────────────────────────

object GenreChordStyles {

    private val jazz7ths = FunctionAwareRule(
        id = "jazz_7ths",
        displayName = "Jazz 7ths (Δ⁷ / m⁷ / 7)",
        familyMap = mapOf(
            ChordFamily.MAJOR_FAMILY      to ChordQuality.MAJOR7,      // I, IV → Maj7
            ChordFamily.MINOR_FAMILY      to ChordQuality.MINOR7,      // ii, vi → m7
            ChordFamily.DOMINANT_FAMILY   to ChordQuality.DOMINANT7,   // V (routed by degree) → 7
            ChordFamily.DIMINISHED_FAMILY to ChordQuality.MINOR7_FLAT5 // vii° → ø7
        )
    )

    private val blues7ths = BlanketRule(
        id = "blues_7ths",
        displayName = "Blues 7ths (all Dominant 7)",
        targetQuality = ChordQuality.DOMINANT7
    )

    // V only — uses isDominantFunction() so it correctly identifies degree 5 major
    // and vii° as dominant-function chords. Minor v (natural minor) is left unchanged.
    // MODIFIED made by Claude 19/08/2026 — was broken (never fired)
    private val dominantVOnly = BlanketRule(
        id = "dominant_v_only",
        displayName = "Dominant V7 Only",
        targetQuality = ChordQuality.DOMINANT7,
        appliesTo = { slot -> isDominantFunction(slot) }
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

    fun defaultFor(genre: Genre): GenreChordRule = byGenre[genre]?.firstOrNull() ?: AsWrittenRule
    fun stylesFor(genre: Genre): List<GenreChordRule> = byGenre[genre] ?: listOf(AsWrittenRule)
}

fun applyGenreChordStyle(slots: List<ChordSlot>, rule: GenreChordRule): List<ChordSlot> =
    slots.map { slot -> slot.copy(genreQualityOverride = rule.apply(slot)) }