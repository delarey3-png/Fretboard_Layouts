package com.example.fretboardlayouts.brain

/*
|--------------------------------------------------------------------------
| DrumBar.kt
|--------------------------------------------------------------------------
|
| Created: 24 July 2026
| Author : ChatGPT (GPT-5.5)
|
| PURPOSE
| -------
|
| Represents ONE complete measure (bar) of generated drums.
|
| The Drum Brain does NOT generate MIDI.
|
| Instead it generates DrumBars.
|
| A DrumBar contains DrumEvents.
|
| The renderer later converts DrumEvents into:
|
| • MIDI notes
| • FluidSynth events
| • PCM Audio
|
| -------------------------------------------------------------------------
|
| Architecture
|
| Music Brain
|      ↓
| Drum Brain
|      ↓
| DrumBar
|      ↓
| List<DrumEvent>
|      ↓
| Audio Engine
|
| -------------------------------------------------------------------------
|
| WHY THIS EXISTS
|
| Keeping bars separate makes it easy to:
|
| • repeat grooves
| • create fills
| • replace only bar 8
| • add endings
| • create intros
| • build verse/chorus structures
|
| instead of working with one giant list of notes.
|
|--------------------------------------------------------------------------
*/

data class DrumBar(

    /**
     * Bar number inside the song.
     */
    val barNumber: Int,

    /**
     * Events generated for this bar.
     */
    val events: MutableList<DrumEvent> = mutableListOf()

) {

    /**
     * Adds one generated hit.
     */
    fun add(event: DrumEvent) {

        events.add(event)

    }

    /**
     * Returns events ordered by step.
     */
    fun sortedEvents(): List<DrumEvent> {

        return events.sortedBy { it.step }

    }

    /**
     * Removes every generated hit.
     */
    fun clear() {

        events.clear()

    }

}