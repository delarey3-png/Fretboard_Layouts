package com.example.fretboardlayouts.theory

// made by Gemini 27/06
/**
 * Represents a single hit in a picking pattern.
 * stringIndex: 0 (High E) to 5 (Low E)
 */
data class PickingAction(
    val stringIndex: Int,
    val velocity: Int,
    val durationMs: Int = 100
)

/**
 * One simultaneous picking voice. 
 * patternByShape: e.g. "<x>_x_" for 1e&a
 * stringsByShape: e.g. "5_2_" (plays Low E then G string)
 */
data class PickingLayer(
    val patternByShape: Map<MeterShape, String>,
    val stringsByShape: Map<MeterShape, String>,
    val ticksPerBeat: Int,
    val normalVelocity: Int,
    val accentVelocity: Int
)

data class PickingPreset(
    val name: String,
    val applicableGenres: Set<Genre>,
    val layers: List<PickingLayer>
)

val travisPickingPreset = PickingPreset(
    name = "Travis Picking",
    applicableGenres = setOf(Genre.COUNTRY, Genre.ROCK, Genre.BLUES),
    layers = listOf(
        // Alternating Bass (Thumb)
        PickingLayer(
            patternByShape = mapOf(MeterShape.FOUR_BEAT to "x_x_x_x_"),
            stringsByShape = mapOf(MeterShape.FOUR_BEAT to "5_4_5_4_"),
            ticksPerBeat = 2, normalVelocity = 85, accentVelocity = 95
        ),
        // Treble Melody (Fingers)
        PickingLayer(
            patternByShape = mapOf(MeterShape.FOUR_BEAT to "_x_x_x_x"),
            stringsByShape = mapOf(MeterShape.FOUR_BEAT to "_2_1_2_3"),
            ticksPerBeat = 2, normalVelocity = 75, accentVelocity = 85
        )
    )
)

val arpeggioUpPreset = PickingPreset(
    name = "Arpeggio Up",
    applicableGenres = setOf(Genre.ROCK, Genre.JAZZ),
    layers = listOf(
        PickingLayer(
            patternByShape = mapOf(MeterShape.FOUR_BEAT to "xxxx"),
            stringsByShape = mapOf(MeterShape.FOUR_BEAT to "5432"),
            ticksPerBeat = 1, normalVelocity = 80, accentVelocity = 80
        )
    )
)

val allPickingPresets = listOf(
    PickingPreset("None (Strum Only)", emptySet(), emptyList()),
    travisPickingPreset,
    arpeggioUpPreset
)
