package com.example.fretboardlayouts.theory

import android.content.Context
import org.json.JSONArray

// ================================================================
// PIANO CHORD LIBRARY
// NEW made by Claude 01/09/2026
//
// Loads piano chord voicings from voicings_compact_piano.json
// (app/src/main/assets/). 412 entries, all in octave 4 (MIDI 60–71),
// suitable for mid-register piano comping. One voicing per chord name.
//
// Chord name conventions differ from GuitarChordLibrary:
//   MINOR suffix → "m"       (guitar uses "minor")
//   MAJOR7_SHARP5 → "maj7sharp5"  (guitar uses "maj7#5")
//
// Degenerate 1–2 note entries (power/incomplete voicings) are
// discarded on load. Returns null for unknown chords; caller falls
// back to ChordNoteBuilder.
//
// Initialize once from FretboardLayoutsApplication.onCreate().
// ================================================================

object PianoChordLibrary {

    private val index = mutableMapOf<String, MutableList<List<Int>>>()
    private var initialized = false

    private val KEY_PREFIX = arrayOf(
        "C", "Csharp", "D", "Eb", "E", "F",
        "Fsharp", "G", "Ab", "A", "Bb", "B"
    )

    // Piano JSON uses "m" for minor and "maj7sharp5" for augmented major7.
    // All other suffixes match GuitarChordLibrary.
    private val QUALITY_SUFFIX = mapOf(
        ChordQuality.MAJOR          to "",
        ChordQuality.MINOR          to "m",
        ChordQuality.DIMINISHED     to "dim",
        ChordQuality.AUGMENTED      to "aug",
        ChordQuality.SUS2           to "sus2",
        ChordQuality.SUS4           to "sus4",
        ChordQuality.MAJOR7         to "maj7",
        ChordQuality.MINOR7         to "m7",
        ChordQuality.DOMINANT7      to "7",
        ChordQuality.MINOR7_FLAT5   to "m7b5",
        ChordQuality.DIMINISHED7    to "dim7",
        ChordQuality.DOMINANT7_SUS4 to "7sus4",
        ChordQuality.MINOR_MAJOR7   to "mmaj7",
        ChordQuality.AUGMENTED7     to "aug7",
        ChordQuality.MAJOR7_SHARP5  to "maj7sharp5",
        ChordQuality.SIX            to "6",
        ChordQuality.MINOR_SIX      to "m6",
        ChordQuality.SIX_NINE       to "69",
        ChordQuality.ADD9           to "add9",
        ChordQuality.MINOR_ADD9     to "madd9",
        ChordQuality.MAJOR9         to "maj9",
        ChordQuality.MINOR9         to "m9",
        ChordQuality.DOMINANT9      to "9",
        ChordQuality.DOMINANT11     to "11",
        ChordQuality.DOMINANT13     to "13"
    )

    fun initialize(context: Context) {
        if (initialized) return
        val json = context.assets.open("voicings_compact_piano.json")
            .bufferedReader().readText()
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val entry = array.getJSONObject(i)
            if (entry.getBoolean("slash")) continue
            val name    = entry.getString("chord")
            val midiArr = entry.getJSONArray("midi")
            val notes   = (0 until midiArr.length()).map { midiArr.getInt(it) }
            if (notes.size < 3) continue  // discard degenerate 1–2 note entries
            index.getOrPut(name) { mutableListOf() }.add(notes)
        }
        initialized = true
    }

    /**
     * Returns the best piano voicing for the given root pitch class and quality,
     * or null if not found (caller falls back to ChordNoteBuilder).
     *
     * Primary: chord-tone-pure voicings of 3–5 notes.
     * Fallback: any voicing of 3+ notes in the dataset for this chord.
     *
     * All voicings in this dataset sit in octave 4 (MIDI 60–71), making
     * them ideal for mid-register piano comping above the bass channel.
     */
    fun bestVoicing(rootPitchClass: Int, quality: ChordQuality): List<Int>? {
        val suffix   = QUALITY_SUFFIX[quality] ?: return null
        val name     = KEY_PREFIX[rootPitchClass] + suffix
        val voicings = index[name] ?: return null

        val validPCs = (ChordNoteBuilder.INTERVALS[quality] ?: return null)
            .map { (rootPitchClass + it) % 12 }.toSet()

        return voicings
            .filter { notes ->
                notes.size in 3..5
                        && notes.all { it % 12 in validPCs }
            }
            .minByOrNull { it.size }   // prefer 3-note shell over 4-note where both exist
            ?: voicings                // fallback: relax PC check, any 3+ note voicing
                .filter { it.size >= 3 }
                .minByOrNull { it.size }
    }
}