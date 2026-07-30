package com.example.fretboardlayouts.brain

/*
|--------------------------------------------------------------------------
| DrumEvent.kt
|--------------------------------------------------------------------------
|
| Created: 24 July 2026
| Author : ChatGPT (GPT-5.5)
|
| PURPOSE
| -------
|
| Represents ONE generated drum hit.
|
| The Drum Brain never sends MIDI directly.
|
| Instead it generates a list of DrumEvents.
|
| The renderer later converts DrumEvents into:
|
| • MIDI events
| • FluidSynth notes
| • Audio samples
|
| This completely separates:
|
| Music Intelligence
|        from
| Audio Playback
|
| -------------------------------------------------------------------------
|
| Example:
|
| Kick on beat 1
|
| DrumEvent(
|     instrument = DrumInstrument.KICK,
|     step = 0,
|     velocity = 92,
|     timingOffset = -3,
|     probability = 0.92f
| )
|
| -------------------------------------------------------------------------
|
| Future versions may include:
|
| • Flam support
| • Ghost notes
| • Accents
| • Swing timing
| • Brushes
| • Stick type
| • Hand assignment (R/L)
| • Articulations
|
|--------------------------------------------------------------------------
*/

data class DrumEvent(

    /**
     * Drum instrument.
     */
    val instrument: DrumInstrument,

    /**
     * Position inside the bar.
     *
     * Normally:
     *
     * 0..15 (16th notes)
     *
     * Future:
     *
     * 0..31
     * 0..63
     */
    val step: Int,

    /**
     * MIDI velocity.
     *
     * 1..127
     */
    val velocity: Int,

    /**
     * Human timing.
     *
     * Negative = early
     *
     * Positive = late
     *
     * Units:
     * milliseconds
     */
    val timingOffset: Float = 0f,

    /**
     * Confidence from the generator.
     *
     * Mainly useful for debugging.
     */
    val probability: Float = 1f

)