package com.example.fretboardlayouts.brain

/*
|--------------------------------------------------------------------------
| BassBar.kt
|--------------------------------------------------------------------------
|
| Created: 25 July 2026
| Author : Claude (Anthropic)
|
| PURPOSE
| -------
|
| Represents ONE complete measure (bar) of generated bass.
|
| Mirrors DrumBar — one key addition:
|
| chordRootMidi — the MIDI root note of the chord active in this bar.
|
| The Bass Brain resolves intervals to absolute MIDI notes at
| generation time, so the renderer needs no chord knowledge.
|
| chordRootMidi is stored for reference and future features
| (fills, transitions, intensity scaling).
|
| -------------------------------------------------------------------------
|
| Architecture
|
| Music Brain
|      ↓
| Bass Brain
|      ↓
| BassBar  ← you are here
|      ↓
| List<BassEvent>
|      ↓
| Audio Engine
|
| -------------------------------------------------------------------------
|
| WHY THIS EXISTS
|
| Keeping bars separate makes it easy to:
|
| • Change the bass pattern on chorus bar 1
| • Add a fill on bar 8
| • Transition from verse to chorus
| • Repeat a groove block
| • Build song sections (verse/chorus/bridge)
|
| instead of managing one giant list of events.
|
| -------------------------------------------------------------------------
|
| Future additions:
|
| • Section label (VERSE / CHORUS / BRIDGE / FILL)
| • Intensity level (0.0..1.0)
| • Override pattern (for fills and endings)
| • Swing amount
| • Groove feel
|
|--------------------------------------------------------------------------
*/

data class BassBar(

    /**
     * Bar number inside the song.
     *
     * Zero-indexed. Bar 0 = first bar.
     */
    val barNumber: Int,

    /**
     * MIDI root note of the chord active in this bar.
     *
     * Always in bass register: 28 (E1) to 52 (E3).
     *
     * Stored for context and future features.
     * Events already contain resolved absolute MIDI notes.
     *
     * Example: E major → chordRootMidi = 40 (E2)
     */
    val chordRootMidi: Int,

    /**
     * Bass events generated for this bar.
     */
    val events: MutableList<BassEvent> = mutableListOf()

) {

    /**
     * Adds one generated bass hit.
     */
    fun add(event: BassEvent) {
        events.add(event)
    }

    /**
     * Returns events ordered by step position.
     *
     * Use this for rendering — guarantees chronological order
     * even if events were added out of order.
     */
    fun sortedEvents(): List<BassEvent> {
        return events.sortedBy { it.step }
    }

    /**
     * Removes all generated hits from this bar.
     *
     * Used when regenerating a specific bar (e.g. fill replacement).
     */
    fun clear() {
        events.clear()
    }

    /**
     * Number of note events in this bar.
     */
    fun noteCount(): Int = events.size

}
