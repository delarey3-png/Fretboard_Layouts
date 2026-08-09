package com.example.fretboardlayouts.audio

import com.example.fretboardlayouts.theory.Genre

/**
 * Maps each Genre to General MIDI program numbers (0-indexed) for the
 * three channels we use: 0 = guitar, 1 = bass, 9 = drums (fixed channel).
 *
 * GM guitar family: 24=Acoustic Nylon 25=Acoustic(steel) 26=Jazz Electric 27=Clean Electric
 *                    28=Muted Electric 29=Overdriven 30=Distortion
 * GM bass family:   32=Acoustic Bass 33=Finger Bass 34=Pick Bass 36=Slap Bass 1
 * GM drum kits (program change on ch 9): 0=Standard 8=Room 16=Power
 *                    24=Electronic 25=TR-808 32=Jazz 40=Brush 48=Orchestra
 */
data class GenreInstrumentation(
    val guitarProgram: Int,
    val bassProgram: Int,
    val drumKitProgram: Int
)

object GenreInstruments {
    fun forGenre(genre: Genre): GenreInstrumentation = when (genre) {
        Genre.ROCK    -> GenreInstrumentation(guitarProgram = 29, bassProgram = 34, drumKitProgram = 16)
        Genre.BLUES   -> GenreInstrumentation(guitarProgram = 26, bassProgram = 33, drumKitProgram = 0)
        Genre.COUNTRY -> GenreInstrumentation(guitarProgram = 25, bassProgram = 32, drumKitProgram = 0)
        Genre.FUNK    -> GenreInstrumentation(guitarProgram = 28, bassProgram = 36, drumKitProgram = 24)
        Genre.JAZZ    -> GenreInstrumentation(guitarProgram = 26, bassProgram = 32, drumKitProgram = 40)
        Genre.DISCO   -> GenreInstrumentation(guitarProgram = 28, bassProgram = 35, drumKitProgram = 0) // NEW made by Claude 05/08/2026
        Genre.SKA     -> GenreInstrumentation(guitarProgram = 29, bassProgram = 34, drumKitProgram = 0) // NEW made by Claude 05/08/2026
        Genre.REGGAE  -> GenreInstrumentation(guitarProgram = 27, bassProgram = 33, drumKitProgram = 0) // NEW made by Claude 05/08/2026
    }
}
