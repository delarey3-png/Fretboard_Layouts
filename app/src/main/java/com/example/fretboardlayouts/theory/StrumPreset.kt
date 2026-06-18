package com.example.fretboardlayouts.theory

enum class MeterShape { FOUR_BEAT, THREE_BEAT }

fun TimeSignature.shape(): MeterShape = if (beatsPerBar == 3) MeterShape.THREE_BEAT else MeterShape.FOUR_BEAT

/** Which strings of a chord voicing a layer plays. Voicings are built low-to-high,
 *  so "top" means the higher-pitched strings. */
enum class VoicingSubset { FULL, TOP_THREE, TOP_FOUR }

fun List<Int>.applySubset(subset: VoicingSubset): List<Int> = when (subset) {
    VoicingSubset.FULL -> this
    VoicingSubset.TOP_THREE -> takeLast(3)
    VoicingSubset.TOP_FOUR -> takeLast(4)
}

/** One simultaneous strum voice within a preset -- most genres need only one,
 *  but shuffle/syncopated feels (Blues, Funk) layer two together. */
data class StrumLayer(
    val patternByShape: Map<MeterShape, String>,
    val directionsByShape: Map<MeterShape, String>,
    val ticksPerBeat: Int,
    val voicingSubset: VoicingSubset,
    val normalVelocity: Int,
    val accentVelocity: Int
)

data class StrumPreset(
    val name: String,
    val applicableGenres: Set<Genre>,
    val layers: List<StrumLayer>
)