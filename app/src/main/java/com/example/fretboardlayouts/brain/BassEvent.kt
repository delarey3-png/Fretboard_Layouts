package com.example.fretboardlayouts.brain

/*
|--------------------------------------------------------------------------
| BassEvent.kt
|--------------------------------------------------------------------------
|
| Created: 25 July 2026
| Author : Claude (Anthropic)
|
| PURPOSE
| -------
|
| Represents ONE generated bass note.
|
| Mirrors DrumEvent but adapted for a pitched instrument.
|
| Key differences from DrumEvent:
|
| • midiNote varies per chord (resolved by BassBrain at generation time)
| • durationSteps defines how long the note sustains
|   (drums are percussive — bass notes need explicit duration)
|
| The Bass Brain never sends MIDI directly.
|
| Instead it generates a list of BassEvents inside a BassBar.
|
| The renderer later converts BassEvents into:
|
| • MIDI note-on / note-off pairs
| • FluidSynth events
| • Audio samples
|
| -------------------------------------------------------------------------
|
| Architecture
|
| Music Brain
|      ↓
| Bass Brain
|      ↓
| BassBar
|      ↓
| List<BassEvent>
|      ↓
| Audio Engine
|
| -------------------------------------------------------------------------
|
| Example: Root note on beat 1 (E2, Rock, quarter note)
|
| BassEvent(
|     step         = 0,
|     midiNote     = 40,   // E2 — root of E chord
|     velocity     = 98,
|     durationSteps = 4,   // quarter note = 4 steps on 16th grid
|     timingOffset  = 0.2f
| )
|
| Example: Fifth below on step 4 (B1)
|
| BassEvent(
|     step         = 4,
|     midiNote     = 35,   // B1 — fifth below E2
|     velocity     = 91,
|     durationSteps = 4
| )
|
| -------------------------------------------------------------------------
|
| Step grid reference (4/4, 16th note resolution):
|
| Step  0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15
| Beat  1  e  &  a  2  e  &  a  3  e  &  a  4  e  &  a
|
| -------------------------------------------------------------------------
|
| Future versions may include:
|
| • Slide / glide between notes
| • Hammer-on / pull-off articulation
| • Slap / pop articulation
| • Harmonic flag
| • Ghost note flag
| • Accent flag
| • String assignment (for tab display)
|
|--------------------------------------------------------------------------
*/

data class BassEvent(

    /**
     * Position inside the bar.
     *
     * 0..15 on a 16th note grid.
     *
     * Beat 1 = step 0
     * Beat 2 = step 4
     * Beat 3 = step 8
     * Beat 4 = step 12
     *
     * Future: expand to 0..31 or 0..63 for higher resolution.
     */
    val step: Int,

    /**
     * Absolute MIDI note number.
     *
     * Always in bass register: 28 (E1) to 52 (E3).
     *
     * Resolved by BassBrain from (chordRootMidi + intervalFromRoot).
     *
     * The renderer uses this directly — no chord knowledge required.
     */
    val midiNote: Int,

    /**
     * MIDI velocity.
     *
     * 1..127
     *
     * Typical bass range: 40..115 depending on genre.
     */
    val velocity: Int,

    /**
     * Note sustain length, measured in 16th note steps.
     *
     * 1  = 16th note
     * 2  = 8th note
     * 4  = quarter note
     * 8  = half note
     * 16 = whole note
     *
     * Renderer sends note-off after (durationSteps × stepDurationMs).
     */
    val durationSteps: Int = 4,

    /**
     * Human timing offset.
     *
     * Negative = plays early (ahead of grid)
     * Positive = plays late (behind grid)
     *
     * Units: milliseconds
     *
     * Applied by renderer on top of the grid-quantised timestamp.
     */
    val timingOffset: Float = 0f,

    /**
     * Generator confidence.
     *
     * 1.0 = pattern core hit (always plays)
     * < 1.0 = probabilistic variation
     *
     * Mainly useful for debugging and future fill generation.
     */
    val probability: Float = 1f

)
