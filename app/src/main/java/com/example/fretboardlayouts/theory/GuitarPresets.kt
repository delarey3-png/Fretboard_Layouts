package com.example.fretboardlayouts.theory

val rockGuitarPreset = StrumPreset(
    name = "Rock Standard",
    applicableGenres = setOf(Genre.ROCK),
    layers = listOf(
        StrumLayer(
            patternByShape = mapOf(
                MeterShape.FOUR_BEAT to "<x>" + "x".repeat(3),
                MeterShape.THREE_BEAT to "<x>" + "x".repeat(2)
            ),
            directionsByShape = mapOf(
                MeterShape.FOUR_BEAT to "d".repeat(4),
                MeterShape.THREE_BEAT to "d".repeat(3)
            ),
            ticksPerBeat = 1, voicingSubset = VoicingSubset.FULL,
            normalVelocity = 72, accentVelocity = 80
        )
    )
)

val countryGuitarPreset = StrumPreset(
    name = "Boom-Chicka",
    applicableGenres = setOf(Genre.COUNTRY),
    layers = listOf(
        StrumLayer(
            patternByShape = mapOf(
                MeterShape.FOUR_BEAT to "_x".repeat(4),
                MeterShape.THREE_BEAT to "_x".repeat(3)
            ),
            directionsByShape = mapOf(
                MeterShape.FOUR_BEAT to "_u".repeat(4),
                MeterShape.THREE_BEAT to "_u".repeat(3)
            ),
            ticksPerBeat = 2, voicingSubset = VoicingSubset.TOP_FOUR,
            normalVelocity = 75, accentVelocity = 75
        )
    )
)

val jazzGuitarPreset = StrumPreset(
    name = "Freddie Green",
    applicableGenres = setOf(Genre.JAZZ),
    layers = listOf(
        StrumLayer(
            patternByShape = mapOf(
                MeterShape.FOUR_BEAT to "x".repeat(4),
                MeterShape.THREE_BEAT to "x".repeat(3)
            ),
            directionsByShape = mapOf(
                MeterShape.FOUR_BEAT to "d".repeat(4),
                MeterShape.THREE_BEAT to "d".repeat(3)
            ),
            ticksPerBeat = 1, voicingSubset = VoicingSubset.TOP_THREE,
            normalVelocity = 65, accentVelocity = 65
        )
    )
)

val bluesGuitarPreset = StrumPreset(
    name = "Shuffle",
    applicableGenres = setOf(Genre.BLUES),
    layers = listOf(
        StrumLayer( // downbeat, full voicing
            patternByShape = mapOf(
                MeterShape.FOUR_BEAT to "x__".repeat(4),
                MeterShape.THREE_BEAT to "x__".repeat(3)
            ),
            directionsByShape = mapOf(
                MeterShape.FOUR_BEAT to "d__".repeat(4),
                MeterShape.THREE_BEAT to "d__".repeat(3)
            ),
            ticksPerBeat = 3, voicingSubset = VoicingSubset.FULL,
            normalVelocity = 85, accentVelocity = 85
        ),
        StrumLayer( // shuffle "and", lighter upstroke
            patternByShape = mapOf(
                MeterShape.FOUR_BEAT to "__x".repeat(4),
                MeterShape.THREE_BEAT to "__x".repeat(3)
            ),
            directionsByShape = mapOf(
                MeterShape.FOUR_BEAT to "__u".repeat(4),
                MeterShape.THREE_BEAT to "__u".repeat(3)
            ),
            ticksPerBeat = 3, voicingSubset = VoicingSubset.TOP_THREE,
            normalVelocity = 60, accentVelocity = 60
        )
    )
)

val funkGuitarPreset = StrumPreset(
    name = "Scratch",
    applicableGenres = setOf(Genre.FUNK),
    layers = listOf(
        StrumLayer( // down on the "&"
            patternByShape = mapOf(
                MeterShape.FOUR_BEAT to "__x_".repeat(4),
                MeterShape.THREE_BEAT to "__x_".repeat(3)
            ),
            directionsByShape = mapOf(
                MeterShape.FOUR_BEAT to "__d_".repeat(4),
                MeterShape.THREE_BEAT to "__d_".repeat(3)
            ),
            ticksPerBeat = 4, voicingSubset = VoicingSubset.TOP_THREE,
            normalVelocity = 80, accentVelocity = 80
        ),
        StrumLayer( // up on the "a"
            patternByShape = mapOf(
                MeterShape.FOUR_BEAT to "___x".repeat(4),
                MeterShape.THREE_BEAT to "___x".repeat(3)
            ),
            directionsByShape = mapOf(
                MeterShape.FOUR_BEAT to "___u".repeat(4),
                MeterShape.THREE_BEAT to "___u".repeat(3)
            ),
            ticksPerBeat = 4, voicingSubset = VoicingSubset.TOP_THREE,
            normalVelocity = 60, accentVelocity = 60
        )
    )
)

val allGuitarPresets: List<StrumPreset> =
    listOf(rockGuitarPreset, countryGuitarPreset, bluesGuitarPreset, funkGuitarPreset, jazzGuitarPreset)

fun isApplicable(preset: StrumPreset, genre: Genre): Boolean = genre in preset.applicableGenres