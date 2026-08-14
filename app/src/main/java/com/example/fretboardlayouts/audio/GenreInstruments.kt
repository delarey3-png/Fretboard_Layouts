package com.example.fretboardlayouts.audio

import com.example.fretboardlayouts.theory.Genre

/**
 * Maps each Genre to General MIDI program numbers (0-indexed) for every
 * instrument channel the engine supports.
 *
 * Channel map (matches INSTRUMENT_DEFS in JamLabActivity.kt and StyleEngine.kt):
 *   Ch  0  Guitar      GM 24-31
 *   Ch  1  Bass        GM 32-39
 *   Ch  2  Piano       always GM 0 — omitted here, unchanged across genres
 *   Ch  3  Organ       GM 16-23
 *   Ch  4  Strings     GM 40-47
 *   Ch  5  Ensemble    GM 48-55
 *   Ch  6  Brass       GM 56-63
 *   Ch  7  Reed        GM 64-71
 *   Ch  8  Pipe        GM 72-79
 *   Ch  9  Drums       GM kit (bank 128 auto-selected on ch9)
 *   Ch 10  Synth       GM 80-95
 *   Ch 11  Ethnic      GM 104-111
 *
 * GM guitar:  24=Nylon 25=Steel 26=Jazz Elec 27=Clean Elec 28=Muted 29=Overdriven 30=Distortion
 * GM bass:    32=Acoustic 33=Finger 34=Pick 35=Fretless 36=Slap 37=Pop 38=SynthBass1
 * GM drums:   0=Standard 8=Room 16=Power 24=Electronic 25=TR-808 32=Jazz 40=Brush 48=Orchestra
 * GM organ:   16=Drawbar 17=Percussive 18=Rock 19=Church 20=Reed 21=Accordion 22=Harmonica
 * GM strings: 40=Violin 41=Viola 42=Cello 44=Tremolo 45=Pizzicato 46=Harp 47=Timpani
 * GM ensemble:48=StringEns1 49=StringEns2 52=ChoirAahs 55=OrchestraHit
 * GM brass:   56=Trumpet 57=Trombone 58=Tuba 60=French Horn 61=BrassSection
 * GM reed:    64=SopSax 65=AltoSax 66=TenorSax 67=BariSax 68=Oboe 71=Clarinet
 * GM pipe:    72=Piccolo 73=Flute 74=Recorder 75=PanFlute 77=Shakuhachi
 * GM synth:   80=SquareLead 88=NewAgePad 89=WarmPad 91=ChoirPad
 * GM ethnic:  104=Sitar 105=Banjo 107=Koto 110=Fiddle
 *
 * -1 = not applicable for this genre (instrument not shown in genreInstrumentVisibility).
 *      Callers should skip patch loading when they see -1.
 */
// MODIFIED made by Claude 09/08/2026 — expanded from 3 fields to full channel map
data class GenreInstrumentation(
    val guitarProgram: Int,
    val bassProgram: Int,
    val drumKitProgram: Int,
    // NEW made by Claude 09/08/2026
    // Default -1 for backward compatibility — existing callers using only the first
    // three fields are unaffected. JamLabAudioEngine wiring deferred to a future session.
    val organProgram: Int    = -1,  // Ch 3
    val stringsProgram: Int  = -1,  // Ch 4
    val ensembleProgram: Int = -1,  // Ch 5
    val brassProgram: Int    = -1,  // Ch 6
    val reedProgram: Int     = -1,  // Ch 7
    val pipeProgram: Int     = -1,  // Ch 8
    val synthProgram: Int    = -1,  // Ch 10
    val ethnicProgram: Int   = -1   // Ch 11
)

object GenreInstruments {
    // MODIFIED made by Claude 09/08/2026 — added new instrument defaults per genre
    // Only instruments visible for a genre (per genreInstrumentVisibility) get a real value.
    // All others stay -1.
    fun forGenre(genre: Genre): GenreInstrumentation = when (genre) {
        // ── ROCK ─────────────────────────────────────────────────────────────
        // Visible: guitar, bass, piano, strings, drums
        Genre.ROCK -> GenreInstrumentation(
            guitarProgram    = 29,   // Overdriven Guitar
            bassProgram      = 34,   // Pick Bass
            drumKitProgram   = 16,   // Power Kit
            stringsProgram   = 48    // String Ensemble 1 — wall-of-sound backing
        )
        // ── BLUES ────────────────────────────────────────────────────────────
        // Visible: guitar, bass, piano, organ, brass, reed, drums
        Genre.BLUES -> GenreInstrumentation(
            guitarProgram    = 26,   // Jazz Electric (clean but woody)
            bassProgram      = 33,   // Finger Bass
            drumKitProgram   = 0,    // Standard Kit
            organProgram     = 16,   // Drawbar Organ — the Chicago blues organ
            brassProgram     = 61,   // Brass Section — Memphis horn stabs
            reedProgram      = 65    // Alto Sax — blues sax
        )
        // ── COUNTRY ──────────────────────────────────────────────────────────
        // Visible: guitar, bass, piano, strings, pipe, ethnic, drums
        Genre.COUNTRY -> GenreInstrumentation(
            guitarProgram    = 25,   // Steel Acoustic
            bassProgram      = 32,   // Acoustic Bass
            drumKitProgram   = 0,    // Standard Kit
            stringsProgram   = 40,   // Violin — fiddle
            pipeProgram      = 73,   // Flute — country melody
            ethnicProgram    = 105   // Banjo — country staple
        )
        // ── FUNK ─────────────────────────────────────────────────────────────
        // Visible: guitar, bass, piano, organ, brass, reed, drums
        Genre.FUNK -> GenreInstrumentation(
            guitarProgram    = 28,   // Muted Electric
            bassProgram      = 36,   // Slap Bass 1
            drumKitProgram   = 24,   // Electronic Kit
            organProgram     = 18,   // Rock Organ — funk organ
            brassProgram     = 61,   // Brass Section — tight funk horns
            reedProgram      = 65    // Alto Sax — funk sax
        )
        // ── JAZZ ─────────────────────────────────────────────────────────────
        // Visible: guitar, bass, piano, organ, strings, brass, reed, drums
        Genre.JAZZ -> GenreInstrumentation(
            guitarProgram    = 26,   // Jazz Electric
            bassProgram      = 32,   // Acoustic Bass
            drumKitProgram   = 40,   // Brush Kit
            organProgram     = 17,   // Percussive Organ — combo organ
            stringsProgram   = 45,   // Pizzicato Strings — jazz strings
            brassProgram     = 56,   // Trumpet — jazz lead
            reedProgram      = 66    // Tenor Sax — jazz sax
        )
        // ── DISCO ────────────────────────────────────────────────────────────
        // Visible: guitar, bass, piano, strings, brass, drums
        Genre.DISCO -> GenreInstrumentation(
            guitarProgram    = 28,   // Muted Electric — funky chops
            bassProgram      = 35,   // Fretless Bass — disco bass
            drumKitProgram   = 0,    // Standard Kit
            stringsProgram   = 48,   // String Ensemble 1 — lush disco strings
            brassProgram     = 61    // Brass Section — disco horn stabs
        )
        // ── SKA ──────────────────────────────────────────────────────────────
        // Visible: guitar, bass, piano, brass, reed, drums
        Genre.SKA -> GenreInstrumentation(
            guitarProgram    = 29,   // Overdriven — ska skank
            bassProgram      = 34,   // Pick Bass
            drumKitProgram   = 0,    // Standard Kit
            brassProgram     = 61,   // Brass Section — ska horns
            reedProgram      = 64    // Soprano Sax — ska sax
        )
        // ── REGGAE ───────────────────────────────────────────────────────────
        // Visible: guitar, bass, piano, organ, brass, reed, drums
        Genre.REGGAE -> GenreInstrumentation(
            guitarProgram    = 27,   // Clean Electric — reggae chop
            bassProgram      = 33,   // Finger Bass — roots reggae bass
            drumKitProgram   = 0,    // Standard Kit
            organProgram     = 19,   // Church Organ — classic reggae organ
            brassProgram     = 56,   // Trumpet — reggae brass
            reedProgram      = 64    // Soprano Sax — reggae sax
        )
    }
}