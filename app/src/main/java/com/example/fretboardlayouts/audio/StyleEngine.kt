package com.example.fretboardlayouts.audio

import com.example.fretboardlayouts.theory.Genre
import com.example.fretboardlayouts.theory.JamTimeline
import com.example.fretboardlayouts.theory.ResolvedChord
import com.example.fretboardlayouts.theory.midiNote
import com.example.fretboardlayouts.theory.pitchClassAt
import com.example.fretboardlayouts.theory.TimeSignature
import com.example.fretboardlayouts.theory.parsePattern
import com.example.fretboardlayouts.theory.StrumPreset
import com.example.fretboardlayouts.theory.PickingPreset
import com.example.fretboardlayouts.theory.shape

/**
 * The "Band-in-a-Box" style engine.
 * Takes a Chord Timeline and a Genre, and produces a multi-track MIDI performance.
 *
 * PATTERN NOTATION RULES (important — read before editing patterns):
 * ─────────────────────────────────────────────────────────────────
 * Every character in a pattern string is exactly ONE grid slot:
 *   x   = normal hit
 *   <x> = accented hit  (counts as ONE slot despite 3 characters)
 *   _   = rest
 *   spaces/commas = ignored (use freely for readability)
 *
 * DO NOT use digit characters (1, 2, 3, 4) as beat labels inside
 * pattern strings — parsePattern() treats them as regular hits,
 * adding unwanted notes and corrupting slot counts.
 *
 * SLOT COUNT REQUIREMENTS per time signature × ticksPerBeat:
 *   4/4 tpb=4 → 16 slots    3/4 tpb=4 → 12 slots    5/4 tpb=4 → 20 slots
 *   4/4 tpb=3 → 12 slots    3/4 tpb=3 →  9 slots    5/4 tpb=3 → 15 slots
 *   4/4 tpb=2 →  8 slots    3/4 tpb=2 →  6 slots    5/4 tpb=2 → 10 slots
 *   4/4 tpb=1 →  4 slots    3/4 tpb=1 →  3 slots    5/4 tpb=1 →  5 slots
 *
 * Beat positions at tpb=4:   beat1=slot0, beat2=slot4, beat3=slot8, beat4=slot12
 * Beat positions at tpb=3:   beat1=slot0, beat2=slot3, beat3=slot6, beat4=slot9
 */
object StyleEngine {

    fun generateAccompaniment(
        timeline: JamTimeline,
        genre: Genre,
        guitarPreset: StrumPreset,
        pickingPreset: PickingPreset? = null
    ): List<BackingTrackGenerator.MidiNoteEvent> {
        val allEvents = mutableListOf<BackingTrackGenerator.MidiNoteEvent>()
        val timeSignature = timeline.timeSignature

        timeline.events.forEach { event ->
            val chord = event.chord
            val startMs = event.startMs
            val durationMs = event.durationMs

            allEvents.addAll(generateDrums(startMs, durationMs, genre, timeSignature))
            allEvents.addAll(generateBass(startMs, durationMs, chord, genre, timeSignature))
            
            if (pickingPreset != null && pickingPreset.layers.isNotEmpty()) {
                allEvents.addAll(generateGuitarPicking(startMs, durationMs, chord, pickingPreset, timeSignature))
            } else {
                allEvents.addAll(generateGuitar(startMs, durationMs, chord, guitarPreset, timeSignature))
            }
        }

        return allEvents.sortedBy { it.timeMs }
    }


    // ─── DRUMS ───────────────────────────────────────────────────────────────

    private fun generateDrums(
        startMs: Long,
        durationMs: Long,
        genre: Genre,
        timeSignature: TimeSignature
    ): List<BackingTrackGenerator.MidiNoteEvent> {
        val events = mutableListOf<BackingTrackGenerator.MidiNoteEvent>()
        val b = timeSignature.beatsPerBar

        when (genre) {

            // ── ROCK ─────────────────────────────────────────────────────────
            // Feel: driving 8th-note hihat, kick beats 1&3, snare beats 2&4.
            // tpb=4 (sixteenth-note grid): beat1=slot0, beat2=slot4, beat3=slot8, beat4=slot12
            Genre.ROCK -> {
                // Hihat: every 8th note (every other slot), accented on the beat
                val hihat = parsePattern(when (b) {
                    3    -> "<x>_x_<x>_x_<x>_x_"           // 12 slots ✓
                    5    -> "<x>_x_<x>_x_<x>_x_<x>_x_<x>_x_" // 20 slots ✓
                    else -> "<x>_x_<x>_x_<x>_x_<x>_x_"    // 16 slots ✓
                })
                events.addAll(renderVoice(hihat, startMs, durationMs, timeSignature, 9, 42, 65, 80, ticksPerBeat = 4))

                // Kick: beats 1 and 3
                val kick = parsePattern(when (b) {
                    3    -> "<x>___________"                // 12 slots, beat 1 only ✓
                    5    -> "<x>_______x___________"        // 20 slots, beats 1&3 ✓
                    else -> "<x>_______x_______"            // 16 slots, beats 1&3 ✓
                })
                events.addAll(renderVoice(kick, startMs, durationMs, timeSignature, 9, 36, 95, 105, noteLengthMs = 100, ticksPerBeat = 4))

                // Snare: beats 2 and 4
                val snare = parsePattern(when (b) {
                    3    -> "____<x>_______"                // 12 slots, beat 2 only ✓
                    5    -> "____<x>___________<x>___"      // 20 slots, beats 2&4 ✓
                    else -> "____<x>_______<x>___"          // 16 slots, beats 2&4 ✓
                })
                events.addAll(renderVoice(snare, startMs, durationMs, timeSignature, 9, 38, 95, 95, noteLengthMs = 100, ticksPerBeat = 4))
            }

            // ── COUNTRY ──────────────────────────────────────────────────────
            // Feel: train-beat hihat (same as rock 8ths), kick beats 1&3,
            //       snare ghost on upbeat of 1 (slot 2), main snare on beat 3 (slot 8).
            // tpb=4 (sixteenth-note grid)
            Genre.COUNTRY -> {
                val hihat = parsePattern(when (b) {
                    3    -> "<x>_x_<x>_x_<x>_x_"           // 12 slots ✓
                    5    -> "<x>_x_<x>_x_<x>_x_<x>_x_<x>_x_" // 20 slots ✓
                    else -> "<x>_x_<x>_x_<x>_x_<x>_x_"    // 16 slots ✓
                })
                events.addAll(renderVoice(hihat, startMs, durationMs, timeSignature, 9, 42, 50, 70, ticksPerBeat = 4))

                val kick = parsePattern(when (b) {
                    3    -> "<x>___________"                // 12 slots ✓
                    5    -> "<x>_______x___________"        // 20 slots ✓
                    else -> "<x>_______x_______"            // 16 slots ✓
                })
                events.addAll(renderVoice(kick, startMs, durationMs, timeSignature, 9, 36, 95, 100, noteLengthMs = 100, ticksPerBeat = 4))

                // Country snare: beat 2 accent + beat 4 (same grid as rock snare)
                val snare = parsePattern(when (b) {
                    3    -> "____<x>_______"                // 12 slots ✓
                    5    -> "____<x>___________<x>___"      // 20 slots ✓
                    else -> "____<x>_______<x>___"          // 16 slots ✓
                })
                events.addAll(renderVoice(snare, startMs, durationMs, timeSignature, 9, 38, 70, 70, noteLengthMs = 100, ticksPerBeat = 4))
            }

            // ── FUNK ─────────────────────────────────────────────────────────
            // Feel: 16th-note hihat on every slot, kick beats 1&3, snare beats 2&4.
            // tpb=4 (sixteenth-note grid)
            Genre.FUNK -> {
                // Hihat: every 16th note, accented on each beat
                val hihat = parsePattern(when (b) {
                    3    -> "<x>xxx<x>xxx<x>xxx"            // 12 slots ✓
                    5    -> "<x>xxx<x>xxx<x>xxx<x>xxx<x>xxx" // 20 slots ✓
                    else -> "<x>xxx<x>xxx<x>xxx<x>xxx"      // 16 slots ✓
                })
                events.addAll(renderVoice(hihat, startMs, durationMs, timeSignature, 9, 42, 50, 80, noteLengthMs = 40, ticksPerBeat = 4))

                // Kick: beats 1 and 3 (same positions as rock)
                val kick = parsePattern(when (b) {
                    3    -> "<x>___________"                // 12 slots ✓
                    5    -> "<x>_______x___________"        // 20 slots ✓
                    else -> "<x>_______x_______"            // 16 slots ✓
                })
                events.addAll(renderVoice(kick, startMs, durationMs, timeSignature, 9, 36, 85, 100, noteLengthMs = 100, ticksPerBeat = 4))

                // Snare: beats 2 and 4
                val snare = parsePattern(when (b) {
                    3    -> "____<x>_______"                // 12 slots ✓
                    5    -> "____<x>___________<x>___"      // 20 slots ✓
                    else -> "____<x>_______<x>___"          // 16 slots ✓
                })
                events.addAll(renderVoice(snare, startMs, durationMs, timeSignature, 9, 38, 100, 100, noteLengthMs = 100, ticksPerBeat = 4))
            }

            // ── BLUES ─────────────────────────────────────────────────────────
            // Feel: shuffle/swing triplet grid. tpb=3 (triplet subdivisions).
            // Each beat = 3 slots: slot 0 = downbeat, slot 1 = skipped, slot 2 = upbeat "and"
            // beat1=slot0, beat2=slot3, beat3=slot6, beat4=slot9
            Genre.BLUES -> {
                // Ride cymbal: classic shuffle swing — downbeat + upbeat of each beat
                // Produces the swung 8th "da-da-DUM" feel
                val ride = parsePattern(when (b) {
                    3    -> "<x>_x<x>_x<x>_x"              // 9 slots ✓
                    5    -> "<x>_x<x>_x<x>_x<x>_x<x>_x"   // 15 slots ✓
                    else -> "<x>_x<x>_x<x>_x<x>_x"         // 12 slots ✓
                })
                events.addAll(renderVoice(ride, startMs, durationMs, timeSignature, 9, 42, 60, 75, ticksPerBeat = 3))

                // Kick: beats 1 and 3
                val kick = parsePattern(when (b) {
                    3    -> "<x>________"                   // 9 slots, beat 1 only ✓
                    5    -> "<x>_____x_____x__"             // 15 slots, beats 1,3,5 ✓
                    else -> "<x>_____x_____"                // 12 slots, beats 1&3 ✓
                })
                events.addAll(renderVoice(kick, startMs, durationMs, timeSignature, 9, 36, 90, 100, noteLengthMs = 100, ticksPerBeat = 3))

                // Snare: beats 2 and 4
                val snare = parsePattern(when (b) {
                    3    -> "___x_____"                     // 9 slots, beat 2 only ✓
                    5    -> "___x_____x_____"               // 15 slots, beats 2&4 ✓
                    else -> "___x_____x__"                  // 12 slots, beats 2&4 ✓
                })
                events.addAll(renderVoice(snare, startMs, durationMs, timeSignature, 9, 38, 90, 90, noteLengthMs = 100, ticksPerBeat = 3))
            }

            // ── JAZZ ─────────────────────────────────────────────────────────
            // Feel: swing ride pattern, hihat pedal on 2&4. tpb=3 (triplet grid).
            // Ride: beat + "and" of beat, but skipping "and" of 2 and 4 for swing lilt.
            // beat1=slot0, beat2=slot3, beat3=slot6, beat4=slot9
            Genre.JAZZ -> {
                // Ride: classic jazz swing ride — "ding-da-ding ... ding-da-ding"
                // Hits on: beat1(0), and-of-1(2), beat2(3), beat3(6), and-of-3(8), beat4(9)
                val ride = parsePattern(when (b) {
                    3    -> "<x>_x<x>_____"                 // 9 slots: beat1, and1, beat2 ✓
                    5    -> "<x>_x<x>__<x>_x<x>__<x>__"    // 15 slots ✓
                    else -> "<x>_x<x>__<x>_x<x>__"         // 12 slots ✓
                })
                events.addAll(renderVoice(ride, startMs, durationMs, timeSignature, 9, 51, 55, 70, noteLengthMs = 100, ticksPerBeat = 3))

                // Hihat pedal: beats 2 and 4 (the "chick" on 2&4)
                val hihat = parsePattern(when (b) {
                    3    -> "___x_____"                     // 9 slots, beat 2 only ✓
                    5    -> "___x_____x_____"               // 15 slots, beats 2&4 ✓
                    else -> "___x_____x__"                  // 12 slots, beats 2&4 ✓
                })
                events.addAll(renderVoice(hihat, startMs, durationMs, timeSignature, 9, 44, 80, 80, noteLengthMs = 50, ticksPerBeat = 3))
            }
        }

        return events
    }

    // ─── BASS ────────────────────────────────────────────────────────────────

    private fun generateBass(
        startMs: Long,
        durationMs: Long,
        chord: ResolvedChord,
        genre: Genre,
        timeSignature: TimeSignature
    ): List<BackingTrackGenerator.MidiNoteEvent> {
        val events = mutableListOf<BackingTrackGenerator.MidiNoteEvent>()
        val root  = findBassPitch(chord.rootPitchClass)
        val fifth = findBassPitch((chord.rootPitchClass + 7) % 12)

        when (genre) {
            Genre.COUNTRY -> {
                // Alternating root-fifth, one hit per beat
                val pattern = parsePattern("<x>" + "x".repeat(timeSignature.beatsPerBar - 1))
                events.addAll(renderPitchSequence(pattern, listOf(root, fifth), startMs, durationMs, timeSignature, 1, 87, 95, noteLengthMs = 400, ticksPerBeat = 1))
            }
            Genre.BLUES -> {
                // Walking bass fragment: root-third-fifth-sixth
                val third = findBassPitch((chord.rootPitchClass + chord.quality.intervals[1]) % 12)
                val sixth = findBassPitch((chord.rootPitchClass + 9) % 12)
                val pattern = parsePattern("<x>" + "x".repeat(timeSignature.beatsPerBar - 1))
                events.addAll(renderPitchSequence(pattern, listOf(root, third, fifth, sixth), startMs, durationMs, timeSignature, 1, 90, 95, noteLengthMs = 400, ticksPerBeat = 1))
            }
            Genre.FUNK -> {
                // Syncopated slap bass: root, root, octave pop, fifth
                val pattern = parsePattern("x__x" + "<x>__x" + "____".repeat(timeSignature.beatsPerBar - 2))
                events.addAll(renderPitchSequence(pattern, listOf(root, root, root + 12, fifth), startMs, durationMs, timeSignature, 1, 90, 110, noteLengthMs = 150, ticksPerBeat = 4))
            }
            Genre.JAZZ -> {
                // Walking bass: root-third-fifth-sixth
                val third = findBassPitch((chord.rootPitchClass + chord.quality.intervals[1]) % 12)
                val sixth = findBassPitch((chord.rootPitchClass + 9) % 12)
                val pattern = parsePattern("<x>" + "x".repeat(timeSignature.beatsPerBar - 1))
                events.addAll(renderPitchSequence(pattern, listOf(root, third, fifth, sixth), startMs, durationMs, timeSignature, 1, 85, 90, noteLengthMs = 400, ticksPerBeat = 1))
            }
            else -> {
                // Rock: pedal root notes on the 8th-note grid
                val pattern = parsePattern("x".repeat(timeSignature.beatsPerBar * 2))
                events.addAll(renderVoice(pattern, startMs, durationMs, timeSignature, 1, root, 85, 85, noteLengthMs = 200, ticksPerBeat = 2))
            }
        }

        return events
    }

    // ─── GUITAR ──────────────────────────────────────────────────────────────

    private fun generateGuitar(
        startMs: Long,
        durationMs: Long,
        chord: ResolvedChord,
        preset: StrumPreset,
        timeSignature: TimeSignature
    ): List<BackingTrackGenerator.MidiNoteEvent> {
        val voicing = findGuitarVoicing(chord)
        return renderPreset(preset, voicing, startMs, durationMs, timeSignature, channel = 0)
    }

    // made by Gemini 27/06
    private fun generateGuitarPicking(
        startMs: Long,
        durationMs: Long,
        chord: ResolvedChord,
        preset: PickingPreset,
        timeSignature: TimeSignature
    ): List<BackingTrackGenerator.MidiNoteEvent> {
        val voicing = findGuitarVoicing(chord)
        // Note: renderPickingPreset doesn't exist yet, I'll use a logic similar to renderPreset but for strings
        val shape = timeSignature.shape()
        val events = mutableListOf<BackingTrackGenerator.MidiNoteEvent>()
        
        preset.layers.forEach { layer ->
            val patternStr = layer.patternByShape[shape] ?: return@forEach
            val stringsStr = layer.stringsByShape[shape] ?: return@forEach
            
            val pattern = parsePattern(patternStr)
            val strings = com.example.fretboardlayouts.theory.parseStrings(stringsStr)
            
            pattern.forEachIndexed { tick, state ->
                if (state == com.example.fretboardlayouts.theory.SlotState.REST) return@forEachIndexed
                val velocity = if (state == com.example.fretboardlayouts.theory.SlotState.ACCENT) layer.accentVelocity else layer.normalVelocity
                
                val stringIdx = strings.getOrNull(tick) ?: return@forEachIndexed
                if (stringIdx < 0 || stringIdx >= voicing.size) return@forEachIndexed
                
                val pitch = voicing[stringIdx]
                val beat = tick / layer.ticksPerBeat
                val tickInBeat = tick % layer.ticksPerBeat
                
                events.add(
                    BackingTrackGenerator.MidiNoteEvent(
                        startMs + com.example.fretboardlayouts.theory.beatTickToMs(beat, tickInBeat, layer.ticksPerBeat, timeSignature, durationMs),
                        0, pitch, velocity, 200
                    )
                )
            }
        }
        return events
    }


    // ─── HELPERS ─────────────────────────────────────────────────────────────

    private fun findGuitarVoicing(chord: ResolvedChord): List<Int> {
        val chordPcs = chord.chordTonePitchClasses.toSet()
        return (0..5).mapNotNull { stringIndex ->
            (0..5).map { fret -> fret to pitchClassAt(stringIndex, fret) }
                .filter { it.second in chordPcs }
                .minByOrNull { it.first }
                ?.let { (fret, _) -> midiNote(stringIndex, fret) }
        }
    }

    private fun findBassPitch(pitchClass: Int): Int {
        var pitch = 28 + pitchClass
        while (pitch < 28) pitch += 12
        while (pitch > 40) pitch -= 12
        return pitch
    }
}
