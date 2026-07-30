package com.example.fretboardlayouts.brain

/*
|--------------------------------------------------------------------------
| DrumInstrument.kt
|--------------------------------------------------------------------------
|
| Created: 24 July 2026
| Author : ChatGPT (GPT-5.5)
|
| PURPOSE
| -------
|
| Defines every drum instrument understood by the Music Brain.
|
| This is NOT tied to any particular SoundFont.
|
| Instead it represents the logical instruments used by the
| procedural generators.
|
| The renderer later converts these instruments into MIDI notes
| according to the General MIDI Drum Standard.
|
| -------------------------------------------------------------------------
|
| Music Brain
|        ↓
| DrumInstrument.KICK
|        ↓
| MIDI Note 36
|        ↓
| FluidSynth
|        ↓
| SoundFont Sample
|
| -------------------------------------------------------------------------
|
| Every future Drum Brain version should use ONLY these instruments.
|
| Never hardcode MIDI note numbers inside generation code.
|
|--------------------------------------------------------------------------
*/

enum class DrumInstrument(

    /**
     * General MIDI note number.
     */
    val midiNote: Int

) {

    //-------------------------------------------------------------------------
    // Kick
    //-------------------------------------------------------------------------

    KICK(36),

    //-------------------------------------------------------------------------
    // Snares
    //-------------------------------------------------------------------------

    SNARE(38),
    SIDE_STICK(37),

    //-------------------------------------------------------------------------
    // Hi Hats
    //-------------------------------------------------------------------------

    CLOSED_HAT(42),
    PEDAL_HAT(44),
    OPEN_HAT(46),

    //-------------------------------------------------------------------------
    // Toms
    //-------------------------------------------------------------------------

    LOW_TOM(45),
    MID_TOM(47),
    HIGH_TOM(50),
    FLOOR_TOM(41),

    //-------------------------------------------------------------------------
    // Cymbals
    //-------------------------------------------------------------------------

    CRASH(49),
    RIDE(51),
    RIDE_BELL(53),
    SPLASH(55),
    CHINA(52),

    //-------------------------------------------------------------------------
    // Percussion
    //-------------------------------------------------------------------------

    COWBELL(56),
    TAMBOURINE(54),
    CLAP(39),
    SHAKER(70),

    //-------------------------------------------------------------------------
    // Generic fallback
    //-------------------------------------------------------------------------

    OTHER(0);

    companion object {

        /**
         * Returns the DrumInstrument for a MIDI note.
         *
         * Useful when importing MIDI files.
         */
        fun fromMidi(note: Int): DrumInstrument {

            return entries.firstOrNull {
                it.midiNote == note
            } ?: OTHER

        }

    }

}