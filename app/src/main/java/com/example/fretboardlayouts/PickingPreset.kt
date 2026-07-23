package com.example.fretboardlayouts.theory

// Original structure by Gemini (27/06) - kept for StyleEngine compatibility
// Enhanced with patterns from backing-tracks (MIT License, Copyright 2025 Andrej)
// https://github.com/ako/backing-tracks/blob/main/midi/patterns.go

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

// ============================================================
// ORIGINAL PRESETS (compatible with StyleEngine)
// ============================================================

val nonePreset = PickingPreset(
    name = "None (Strum Only)",
    applicableGenres = emptySet(),
    layers = emptyList()
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

// ============================================================
// ENHANCED PRESETS (from backing-tracks, adapted to PickingLayer format)
// ============================================================

val travisSimplePreset = PickingPreset(
    name = "Travis Simple",
    applicableGenres = setOf(Genre.COUNTRY, Genre.ROCK, Genre.BLUES),
    layers = listOf(
        // Bass: 0-3, 1-3, 0-3, 1-3 (alternating low strings)
        PickingLayer(
            patternByShape = mapOf(MeterShape.FOUR_BEAT to "x_x_x_x_"),
            stringsByShape = mapOf(MeterShape.FOUR_BEAT to "5_4_5_4_"),
            ticksPerBeat = 2, normalVelocity = 85, accentVelocity = 95
        ),
        // Treble: single melody notes
        PickingLayer(
            patternByShape = mapOf(MeterShape.FOUR_BEAT to "_x_x_x_x"),
            stringsByShape = mapOf(MeterShape.FOUR_BEAT to "_3_2_3_2"),
            ticksPerBeat = 2, normalVelocity = 70, accentVelocity = 80
        )
    )
)

val arpeggioUpPimaPreset = PickingPreset(
    name = "Arpeggio PIMA",
    applicableGenres = setOf(Genre.JAZZ),
    layers = listOf(
        PickingLayer(
            patternByShape = mapOf(MeterShape.FOUR_BEAT to "xxxx xxxx"),
            stringsByShape = mapOf(MeterShape.FOUR_BEAT to "5 243 5 243"),
            ticksPerBeat = 1, normalVelocity = 75, accentVelocity = 85
        )
    )
)

val folkBalladPreset = PickingPreset(
    name = "Folk Ballad",
    applicableGenres = setOf(Genre.ROCK, Genre.COUNTRY),
    layers = listOf(
        PickingLayer(
            patternByShape = mapOf(MeterShape.FOUR_BEAT to "x__x__x__"),
            stringsByShape = mapOf(MeterShape.FOUR_BEAT to "5__3__2__"),
            ticksPerBeat = 1, normalVelocity = 75, accentVelocity = 85
        )
    )
)

val classicalTremoloPreset = PickingPreset(
    name = "Classical Tremolo",
    applicableGenres = setOf(Genre.JAZZ),
    layers = listOf(
        PickingLayer(
            patternByShape = mapOf(MeterShape.FOUR_BEAT to "xxxx xxxx xxxx xxxx"),
            stringsByShape = mapOf(MeterShape.FOUR_BEAT to "5222 5222 5222 5222"),
            ticksPerBeat = 4, normalVelocity = 70, accentVelocity = 80
        )
    )
)

val bossaNovaPreset = PickingPreset(
    name = "Bossa Nova",
    applicableGenres = setOf(Genre.JAZZ),
    layers = listOf(
        PickingLayer(
            patternByShape = mapOf(MeterShape.FOUR_BEAT to "x_x__x_x_"),
            stringsByShape = mapOf(MeterShape.FOUR_BEAT to "4_3__4_3_"),
            ticksPerBeat = 2, normalVelocity = 75, accentVelocity = 85
        )
    )
)

val ballad44Preset = PickingPreset(
    name = "Ballad 4/4",
    applicableGenres = setOf(Genre.ROCK),
    layers = listOf(
        PickingLayer(
            patternByShape = mapOf(MeterShape.FOUR_BEAT to "x____x____"),
            stringsByShape = mapOf(MeterShape.FOUR_BEAT to "5____4____"),
            ticksPerBeat = 1, normalVelocity = 70, accentVelocity = 80
        )
    )
)

val simpleFingerPickPreset = PickingPreset(
    name = "Simple Fingerpick",
    applicableGenres = setOf(Genre.ROCK, Genre.COUNTRY, Genre.BLUES),
    layers = listOf(
        PickingLayer(
            patternByShape = mapOf(MeterShape.FOUR_BEAT to "x_x_x_x_"),
            stringsByShape = mapOf(MeterShape.FOUR_BEAT to "5_3_4_2_"),
            ticksPerBeat = 2, normalVelocity = 75, accentVelocity = 85
        )
    )
)

val waltz34Preset = PickingPreset(
    name = "Waltz 3/4",
    applicableGenres = setOf(Genre.ROCK),
    layers = listOf(
        PickingLayer(
            patternByShape = mapOf(MeterShape.FOUR_BEAT to "x__"),
            stringsByShape = mapOf(MeterShape.FOUR_BEAT to "5__"),
            ticksPerBeat = 1, normalVelocity = 80, accentVelocity = 90
        )
    )
)

// ============================================================
// MASTER LIST
// ============================================================

val allPickingPresets = listOf(
    nonePreset,
    travisPickingPreset,
    travisSimplePreset,
    arpeggioUpPreset,
    arpeggioUpPimaPreset,
    folkBalladPreset,
    classicalTremoloPreset,
    bossaNovaPreset,
    ballad44Preset,
    simpleFingerPickPreset,
    waltz34Preset
)
