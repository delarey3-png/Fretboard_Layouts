package com.example.fretboardlayouts.theory

// ─── ROCK ────────────────────────────────────────────────────────────────────

val rockStandardPreset = StrumPreset(
    name = "Rock Standard",
    applicableGenres = setOf(Genre.ROCK),
    layers = listOf(StrumLayer(
        patternByShape = mapOf(
            MeterShape.FOUR_BEAT to "<x>xxx",
            MeterShape.THREE_BEAT to "<x>xx",
            MeterShape.FIVE_BEAT to "<x>xxxx"
        ),
        directionsByShape = mapOf(
            MeterShape.FOUR_BEAT to "dddd",
            MeterShape.THREE_BEAT to "ddd",
            MeterShape.FIVE_BEAT to "ddddd"
        ),
        ticksPerBeat = 1, voicingSubset = VoicingSubset.FULL,
        normalVelocity = 72, accentVelocity = 80
    ))
)

val rockDrivingPreset = StrumPreset(
    name = "Rock Driving",
    applicableGenres = setOf(Genre.ROCK),
    layers = listOf(StrumLayer(
        patternByShape = mapOf(
            MeterShape.FOUR_BEAT to "<x>xxxxxxx",
            MeterShape.THREE_BEAT to "<x>xxxxx",
            MeterShape.FIVE_BEAT to "<x>xxxxxxxxx"
        ),
        directionsByShape = mapOf(
            MeterShape.FOUR_BEAT to "dddddddd",
            MeterShape.THREE_BEAT to "dddddd",
            MeterShape.FIVE_BEAT to "dddddddddd"
        ),
        ticksPerBeat = 2, voicingSubset = VoicingSubset.FULL,
        normalVelocity = 75, accentVelocity = 85
    ))
)

val rockBalladPreset = StrumPreset(
    name = "Rock Ballad",
    applicableGenres = setOf(Genre.ROCK),
    layers = listOf(StrumLayer(
        patternByShape = mapOf(
            MeterShape.FOUR_BEAT to "<x>_xxx_xx",
            MeterShape.THREE_BEAT to "<x>_xxx_",
            MeterShape.FIVE_BEAT to "<x>_xxx_xxx_x"
        ),
        directionsByShape = mapOf(
            MeterShape.FOUR_BEAT to "d_dud_du",
            MeterShape.THREE_BEAT to "d_dud_",
            MeterShape.FIVE_BEAT to "d_dud_dud_d"
        ),
        ticksPerBeat = 2, voicingSubset = VoicingSubset.FULL,
        normalVelocity = 65, accentVelocity = 75
    ))
)

// ─── COUNTRY ─────────────────────────────────────────────────────────────────

val countryBoomChickaPreset = StrumPreset(
    name = "Boom-Chicka",
    applicableGenres = setOf(Genre.COUNTRY),
    layers = listOf(StrumLayer(
        patternByShape = mapOf(
            MeterShape.FOUR_BEAT to "_x_x_x_x",
            MeterShape.THREE_BEAT to "_x_x_x",
            MeterShape.FIVE_BEAT to "_x_x_x_x_x"
        ),
        directionsByShape = mapOf(
            MeterShape.FOUR_BEAT to "_u_u_u_u",
            MeterShape.THREE_BEAT to "_u_u_u",
            MeterShape.FIVE_BEAT to "_u_u_u_u_u"
        ),
        ticksPerBeat = 2, voicingSubset = VoicingSubset.TOP_FOUR,
        normalVelocity = 75, accentVelocity = 75
    ))
)

val countryGallopPreset = StrumPreset(
    name = "Country Gallop",
    applicableGenres = setOf(Genre.COUNTRY),
    layers = listOf(StrumLayer(
        patternByShape = mapOf(
            MeterShape.FOUR_BEAT to "<x>x_xxx_xxx_xxx_x",
            MeterShape.THREE_BEAT to "<x>x_xxx_xxx_x",
            MeterShape.FIVE_BEAT to "<x>x_xxx_xxx_xxx_xxx_x"
        ),
        directionsByShape = mapOf(
            MeterShape.FOUR_BEAT to "dd_ddd_ddd_ddd_d",
            MeterShape.THREE_BEAT to "dd_ddd_ddd_d",
            MeterShape.FIVE_BEAT to "dd_ddd_ddd_ddd_ddd_d"
        ),
        ticksPerBeat = 4, voicingSubset = VoicingSubset.FULL,
        normalVelocity = 72, accentVelocity = 82
    ))
)

val countrySlowPreset = StrumPreset(
    name = "Country Slow",
    applicableGenres = setOf(Genre.COUNTRY),
    layers = listOf(StrumLayer(
        patternByShape = mapOf(
            MeterShape.FOUR_BEAT to "<x>xxx",
            MeterShape.THREE_BEAT to "<x>xx",
            MeterShape.FIVE_BEAT to "<x>xxxx"
        ),
        directionsByShape = mapOf(
            MeterShape.FOUR_BEAT to "dddd",
            MeterShape.THREE_BEAT to "ddd",
            MeterShape.FIVE_BEAT to "ddddd"
        ),
        ticksPerBeat = 1, voicingSubset = VoicingSubset.FULL,
        normalVelocity = 65, accentVelocity = 75
    ))
)

// ─── BLUES ───────────────────────────────────────────────────────────────────

val bluesShufflePreset = StrumPreset(
    name = "Shuffle",
    applicableGenres = setOf(Genre.BLUES),
    layers = listOf(
        StrumLayer(
            patternByShape = mapOf(
                MeterShape.FOUR_BEAT to "x__x__x__x__",
                MeterShape.THREE_BEAT to "x__x__x__",
                MeterShape.FIVE_BEAT to "x__x__x__x__x__"
            ),
            directionsByShape = mapOf(
                MeterShape.FOUR_BEAT to "d__d__d__d__",
                MeterShape.THREE_BEAT to "d__d__d__",
                MeterShape.FIVE_BEAT to "d__d__d__d__d__"
            ),
            ticksPerBeat = 3, voicingSubset = VoicingSubset.FULL,
            normalVelocity = 85, accentVelocity = 85
        ),
        StrumLayer(
            patternByShape = mapOf(
                MeterShape.FOUR_BEAT to "__x__x__x__x",
                MeterShape.THREE_BEAT to "__x__x__x",
                MeterShape.FIVE_BEAT to "__x__x__x__x__x"
            ),
            directionsByShape = mapOf(
                MeterShape.FOUR_BEAT to "__u__u__u__u",
                MeterShape.THREE_BEAT to "__u__u__u",
                MeterShape.FIVE_BEAT to "__u__u__u__u__u"
            ),
            ticksPerBeat = 3, voicingSubset = VoicingSubset.TOP_THREE,
            normalVelocity = 60, accentVelocity = 60
        )
    )
)

val bluesSlowPreset = StrumPreset(
    name = "Blues Slow",
    applicableGenres = setOf(Genre.BLUES),
    layers = listOf(StrumLayer(
        patternByShape = mapOf(
            MeterShape.FOUR_BEAT to "<x>__x__x__x__",
            MeterShape.THREE_BEAT to "<x>__x__x__",
            MeterShape.FIVE_BEAT to "<x>__x__x__x__x__"
        ),
        directionsByShape = mapOf(
            MeterShape.FOUR_BEAT to "d__d__d__d__",
            MeterShape.THREE_BEAT to "d__d__d__",
            MeterShape.FIVE_BEAT to "d__d__d__d__d__"
        ),
        ticksPerBeat = 3, voicingSubset = VoicingSubset.FULL,
        normalVelocity = 78, accentVelocity = 88
    ))
)

val bluesRockPreset = StrumPreset(
    name = "Blues Rock",
    applicableGenres = setOf(Genre.BLUES),
    layers = listOf(StrumLayer(
        patternByShape = mapOf(
            MeterShape.FOUR_BEAT to "<x>_xxx_xx",
            MeterShape.THREE_BEAT to "<x>_xxx_",
            MeterShape.FIVE_BEAT to "<x>_xxx_xxx_x"
        ),
        directionsByShape = mapOf(
            MeterShape.FOUR_BEAT to "d_dud_du",
            MeterShape.THREE_BEAT to "d_dud_",
            MeterShape.FIVE_BEAT to "d_dud_dud_d"
        ),
        ticksPerBeat = 2, voicingSubset = VoicingSubset.FULL,
        normalVelocity = 80, accentVelocity = 90
    ))
)

// ─── FUNK ────────────────────────────────────────────────────────────────────

val funkScratchPreset = StrumPreset(
    name = "Scratch",
    applicableGenres = setOf(Genre.FUNK),
    layers = listOf(
        StrumLayer(
            patternByShape = mapOf(
                MeterShape.FOUR_BEAT to "__x_".repeat(4),
                MeterShape.THREE_BEAT to "__x_".repeat(3),
                MeterShape.FIVE_BEAT to "__x_".repeat(5)
            ),
            directionsByShape = mapOf(
                MeterShape.FOUR_BEAT to "__d_".repeat(4),
                MeterShape.THREE_BEAT to "__d_".repeat(3),
                MeterShape.FIVE_BEAT to "__d_".repeat(5)
            ),
            ticksPerBeat = 4, voicingSubset = VoicingSubset.TOP_THREE,
            normalVelocity = 80, accentVelocity = 80
        ),
        StrumLayer(
            patternByShape = mapOf(
                MeterShape.FOUR_BEAT to "___x".repeat(4),
                MeterShape.THREE_BEAT to "___x".repeat(3),
                MeterShape.FIVE_BEAT to "___x".repeat(5)
            ),
            directionsByShape = mapOf(
                MeterShape.FOUR_BEAT to "___u".repeat(4),
                MeterShape.THREE_BEAT to "___u".repeat(3),
                MeterShape.FIVE_BEAT to "___u".repeat(5)
            ),
            ticksPerBeat = 4, voicingSubset = VoicingSubset.TOP_THREE,
            normalVelocity = 60, accentVelocity = 60
        )
    )
)

val funkHeavyPreset = StrumPreset(
    name = "Funk Heavy",
    applicableGenres = setOf(Genre.FUNK),
    layers = listOf(StrumLayer(
        patternByShape = mapOf(
            MeterShape.FOUR_BEAT to "<x>xx_xxx_xxx_xxx_",
            MeterShape.THREE_BEAT to "<x>xx_xxx_xxx_",
            MeterShape.FIVE_BEAT to "<x>xx_xxx_xxx_xxx_xxx_"
        ),
        directionsByShape = mapOf(
            MeterShape.FOUR_BEAT to "ddu_dud_dud_dud_",
            MeterShape.THREE_BEAT to "ddu_dud_dud_",
            MeterShape.FIVE_BEAT to "ddu_dud_dud_dud_dud_"
        ),
        ticksPerBeat = 4, voicingSubset = VoicingSubset.TOP_THREE,
        normalVelocity = 75, accentVelocity = 90
    ))
)

val funkGroovePreset = StrumPreset(
    name = "Funk Groove",
    applicableGenres = setOf(Genre.FUNK),
    layers = listOf(StrumLayer(
        patternByShape = mapOf(
            MeterShape.FOUR_BEAT to "<x>__x_x__x__x_",
            MeterShape.THREE_BEAT to "<x>__x_x__x_",
            MeterShape.FIVE_BEAT to "<x>__x_x__x__x_x__x_"
        ),
        directionsByShape = mapOf(
            MeterShape.FOUR_BEAT to "d__d_u__d__u_",
            MeterShape.THREE_BEAT to "d__d_u__d_",
            MeterShape.FIVE_BEAT to "d__d_u__d__u_d__u_"
        ),
        ticksPerBeat = 4, voicingSubset = VoicingSubset.TOP_THREE,
        normalVelocity = 72, accentVelocity = 85
    ))
)

// ─── JAZZ ────────────────────────────────────────────────────────────────────

val jazzFreddieGreenPreset = StrumPreset(
    name = "Freddie Green",
    applicableGenres = setOf(Genre.JAZZ),
    layers = listOf(StrumLayer(
        patternByShape = mapOf(
            MeterShape.FOUR_BEAT to "x".repeat(4),
            MeterShape.THREE_BEAT to "x".repeat(3),
            MeterShape.FIVE_BEAT to "x".repeat(5)
        ),
        directionsByShape = mapOf(
            MeterShape.FOUR_BEAT to "d".repeat(4),
            MeterShape.THREE_BEAT to "d".repeat(3),
            MeterShape.FIVE_BEAT to "d".repeat(5)
        ),
        ticksPerBeat = 1, voicingSubset = VoicingSubset.TOP_THREE,
        normalVelocity = 65, accentVelocity = 65
    ))
)

val jazzCompPreset = StrumPreset(
    name = "Jazz Comp",
    applicableGenres = setOf(Genre.JAZZ),
    layers = listOf(StrumLayer(
        patternByShape = mapOf(
            MeterShape.FOUR_BEAT to "<x>____xx____x",
            MeterShape.THREE_BEAT to "<x>____xx__",
            MeterShape.FIVE_BEAT to "<x>____xx____x____x"
        ),
        directionsByShape = mapOf(
            MeterShape.FOUR_BEAT to "d____ud____u",
            MeterShape.THREE_BEAT to "d____ud__",
            MeterShape.FIVE_BEAT to "d____ud____u____u"
        ),
        ticksPerBeat = 3, voicingSubset = VoicingSubset.TOP_THREE,
        normalVelocity = 60, accentVelocity = 72
    ))
)

val jazzBalladPreset = StrumPreset(
    name = "Jazz Ballad",
    applicableGenres = setOf(Genre.JAZZ),
    layers = listOf(StrumLayer(
        patternByShape = mapOf(
            MeterShape.FOUR_BEAT to "<x>_x_",
            MeterShape.THREE_BEAT to "<x>_x",
            MeterShape.FIVE_BEAT to "<x>_x_x"
        ),
        directionsByShape = mapOf(
            MeterShape.FOUR_BEAT to "d_d_",
            MeterShape.THREE_BEAT to "d_d",
            MeterShape.FIVE_BEAT to "d_d_d"
        ),
        ticksPerBeat = 1, voicingSubset = VoicingSubset.TOP_THREE,
        normalVelocity = 55, accentVelocity = 65
    ))
)

// ─── REGISTRY ────────────────────────────────────────────────────────────────

val allGuitarPresets: List<StrumPreset> = listOf(
    rockStandardPreset, rockDrivingPreset, rockBalladPreset,
    countryBoomChickaPreset, countryGallopPreset, countrySlowPreset,
    bluesShufflePreset, bluesSlowPreset, bluesRockPreset,
    funkScratchPreset, funkHeavyPreset, funkGroovePreset,
    jazzFreddieGreenPreset, jazzCompPreset, jazzBalladPreset
)

fun isApplicable(preset: StrumPreset, genre: Genre): Boolean =
    genre in preset.applicableGenres