package com.example.fretboardlayouts.brain

/*
|--------------------------------------------------------------------------
| DrumTrack.kt
|--------------------------------------------------------------------------
|
| Created: 24 July 2026
| Author : ChatGPT (GPT-5.5)
|
| PURPOSE
| -------
|
| Represents the COMPLETE drum performance for one song.
|
| A DrumTrack is built by the Drum Brain.
|
| It contains multiple DrumBars.
|
| Example:
|
| Verse
|   Bar 1
|   Bar 2
|   Bar 3
|   Bar 4
|
| Chorus
|   Bar 5
|   Bar 6
|   Bar 7
|   Fill
|
| Outro
|   Bar 9
|
| -------------------------------------------------------------------------
|
| Architecture
|
| MusicBrain
|      │
|      ▼
| DrumBrain
|      │
|      ▼
| DrumTrack
|      │
|      ├── DrumBar 1
|      ├── DrumBar 2
|      ├── DrumBar 3
|      ├── DrumBar 4
|      │
|      ▼
| Audio Renderer
|
| -------------------------------------------------------------------------
|
| IMPORTANT
|
| DrumTrack contains NO playback logic.
|
| It is simply a musical representation of everything
| the drummer intends to play.
|
| Playback is handled later by:
|
| • BackingTrackGenerator
| • PatternRenderer
| • FluidSynthEngine
|
| -------------------------------------------------------------------------
|
| Future additions
|
| • Verse markers
| • Chorus markers
| • Fill markers
| • Intro
| • Outro
| • Crash markers
| • Dynamic intensity
| • Groove confidence
| • Swing amount
|
|--------------------------------------------------------------------------
*/

data class DrumTrack(

    /**
     * Bars generated for this song.
     */
    val bars: MutableList<DrumBar> = mutableListOf()

) {

    /**
     * Adds one generated bar.
     */
    fun addBar(bar: DrumBar) {

        bars.add(bar)

    }

    /**
     * Returns the requested bar.
     */
    fun getBar(index: Int): DrumBar? {

        return bars.getOrNull(index)

    }

    /**
     * Number of bars.
     */
    fun size(): Int {

        return bars.size

    }

    /**
     * Clears the entire performance.
     */
    fun clear() {

        bars.clear()

    }

    /**
     * Returns every DrumEvent in chronological order.
     *
     * Useful for rendering into MIDI or audio.
     */
    fun allEvents(): List<DrumEvent> {

        return bars
            .flatMap { it.sortedEvents() }

    }

}