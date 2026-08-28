package com.example.fretboardlayouts.theory

import android.content.Context
import org.json.JSONArray

// ================================================================
// GUITAR CHORD LIBRARY
// NEW made by Claude 25/08/2026
//
// Loads real guitar chord voicings from guitar_voicings.json
// (app/src/main/assets/). Source: tombatossals/chords-db, converted
// to MIDI by GPT/Claude 2. 2,942 entries, all 12 chromatic roots,
// 25 chord qualities, 3–6 voicings each. MIDI range 40–79.
//
// Replaces ChordNoteBuilder's closed-position interval stacking with
// actual playable guitar grips. Called by StyleEngine.findGuitarVoicing()
// as primary source; ChordNoteBuilder remains as fallback for TRIAD/POWER
// and any chord not found in the library.
//
// Initialize once from FretboardLayoutsApplication.onCreate().
// All other callers just call bestVoicing() directly.
// ================================================================

object GuitarChordLibrary {

    // chord name → list of MIDI voicings for that chord
    // e.g. "Cmaj7" → [[43,48,52,55,59,64], [43,48,55,59,64,67], ...]
    private val index = mutableMapOf<String, MutableList<List<Int>>>()
    private var initialized = false

    // Pitch class 0–11 → chord name prefix.
    // C# and F# use the "sharp" spelling used by tombatossals/chords-db.
    private val KEY_PREFIX = arrayOf(
        "C", "Csharp", "D", "Eb", "E", "F",
        "Fsharp", "G", "Ab", "A", "Bb", "B"
    )

    // All 25 ChordQuality values → chord name suffix, verified against JSON.
    private val QUALITY_SUFFIX = mapOf(
        ChordQuality.MAJOR          to "",
        ChordQuality.MINOR          to "minor",
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
        ChordQuality.MAJOR7_SHARP5  to "maj7#5",
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
        val json = context.assets.open("voicings_compact.json")
            .bufferedReader().readText()
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val entry = array.getJSONObject(i)
            if (entry.getBoolean("slash")) continue  // skip slash chords
            val name    = entry.getString("chord")
            val midiArr = entry.getJSONArray("midi")
            val notes   = (0 until midiArr.length()).map { midiArr.getInt(it) }
            index.getOrPut(name) { mutableListOf() }.add(notes)
        }
        initialized = true
    }

    /**
     * Returns the best guitar voicing for the given root pitch class and quality,
     * or null if not found (caller falls back to ChordNoteBuilder).
     *
     * Selection rule: prefer voicings where the lowest note is in E2–G3
     * (MIDI 40–55) with the widest spread. This produces open, guitar-
     * idiomatic grips rather than high-register barre positions.
     * Secondary fallback: voicing whose lowest note is closest to C3 (48).
     */
    fun bestVoicing(rootPitchClass: Int, quality: ChordQuality): List<Int>? {
        val suffix   = QUALITY_SUFFIX[quality] ?: return null
        val name     = KEY_PREFIX[rootPitchClass] + suffix
        val voicings = index[name] ?: return null

        val validPCs = (ChordNoteBuilder.INTERVALS[quality] ?: return null)
            .map { (rootPitchClass + it) % 12 }.toSet()

        return voicings
            .filter { notes ->
                notes.isNotEmpty()
                        && notes.first() in 40..55   // bass note on a low string
                        && notes.last() <= 72        // MODIFIED: cap top note at C5 — prevents shrill high voicings
                        && notes.size in 3..6
                        && notes.all { it % 12 in validPCs }
            }
            .maxByOrNull { it.last() - it.first() }
            ?: voicings  // fallback: relax PC and ceiling checks, just find mid-register voicing
                .filter { it.isNotEmpty() && it.first() in 40..55 && it.size in 3..6 }
                .minByOrNull { Math.abs(it.first() - 48) }
    }
}