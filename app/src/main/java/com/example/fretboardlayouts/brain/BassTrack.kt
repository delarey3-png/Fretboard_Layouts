package com.example.fretboardlayouts.brain

/*
|--------------------------------------------------------------------------
| BassTrack.kt
|--------------------------------------------------------------------------
|
| Created: 25 July 2026
| Author : Claude (Anthropic)
|
| PURPOSE
| -------
|
| Represents the COMPLETE bass performance for one song or loop.
|
| A BassTrack is built by the Bass Brain.
|
| It contains multiple BassBars.
|
| Example (4-bar loop, I–V–vi–IV in C major):
|
| Bar 0 → chordRoot = 48 (C2) → events for C
| Bar 1 → chordRoot = 43 (G1) → events for G
| Bar 2 → chordRoot = 45 (A1) → events for Am
| Bar 3 → chordRoot = 41 (F1) → events for F
|
| -------------------------------------------------------------------------
|
| Architecture
|
| MusicBrain
|      │
|      ▼
| BassBrain
|      │
|      ▼
| BassTrack  ← you are here
|      │
|      ├── BassBar 0  (chord I)
|      ├── BassBar 1  (chord V)
|      ├── BassBar 2  (chord vi)
|      ├── BassBar 3  (chord IV)
|      │
|      ▼
| Audio Renderer
|
| -------------------------------------------------------------------------
|
| IMPORTANT
|
| BassTrack contains NO playback logic.
|
| It is purely a musical representation of what the bassist plays.
|
| Playback is handled later by:
|
| • PatternRenderer
| • FluidSynthEngine
| • StyleEngine / JamLabAudioEngine
|
| -------------------------------------------------------------------------
|
| Future additions:
|
| • Section markers (verse / chorus / bridge)
| • Fill markers
| • Intensity curve
| • Groove confidence score
| • Pattern name (for debug display)
|
|--------------------------------------------------------------------------
*/

data class BassTrack(

    /**
     * Bars generated for this performance.
     */
    val bars: MutableList<BassBar> = mutableListOf()

) {

    /**
     * Adds one generated bar.
     */
    fun addBar(bar: BassBar) {
        bars.add(bar)
    }

    /**
     * Returns the bar at the given index.
     *
     * Returns null if index is out of range.
     */
    fun getBar(index: Int): BassBar? {
        return bars.getOrNull(index)
    }

    /**
     * Total number of bars in this performance.
     */
    fun size(): Int {
        return bars.size
    }

    /**
     * Clears the entire performance.
     *
     * Used when regenerating from scratch.
     */
    fun clear() {
        bars.clear()
    }

    /**
     * Returns every BassEvent across all bars, in chronological order.
     *
     * Events within each bar are sorted by step before flattening.
     *
     * Use this for rendering the complete performance into MIDI or audio.
     *
     * Note: for duration/timing calculations the renderer also needs
     * bar number — access via getBar(index) or iterate bars directly.
     */
    fun allEvents(): List<BassEvent> {
        return bars.flatMap { it.sortedEvents() }
    }

    /**
     * Returns all bars with their events in chronological order.
     *
     * Preferred for rendering when per-bar context is needed
     * (e.g. verifying chordRootMidi, section labels in future).
     */
    fun barsWithEvents(): List<Pair<BassBar, List<BassEvent>>> {
        return bars.map { bar -> bar to bar.sortedEvents() }
    }

}
