package com.example.fretboardlayouts.audio

import com.example.fretboardlayouts.theory.Genre
import com.example.fretboardlayouts.theory.JamTimeline
import com.example.fretboardlayouts.theory.ResolvedChord
import com.example.fretboardlayouts.theory.midiNote
import com.example.fretboardlayouts.theory.pitchClassAt
import com.example.fretboardlayouts.theory.TimeSignature
import com.example.fretboardlayouts.theory.slotToMs
import com.example.fretboardlayouts.theory.subdivisionCount
import com.example.fretboardlayouts.theory.tripletToMs
import com.example.fretboardlayouts.theory.parsePattern
import com.example.fretboardlayouts.theory.parseDirections

/**
 * The "Band-in-a-Box" style engine.
 * Takes a Chord Timeline and a Genre, and produces a multi-track MIDI performance.
 */
object StyleEngine {

    fun generateAccompaniment(timeline: JamTimeline, genre: Genre): List<BackingTrackGenerator.MidiNoteEvent> {
        val allEvents = mutableListOf<BackingTrackGenerator.MidiNoteEvent>()
        val timeSignature = timeline.timeSignature

        timeline.events.forEach { event ->
            val chord = event.chord
            val startMs = event.startMs
            val durationMs = event.durationMs

            // Generate each instrument track based on the genre
            allEvents.addAll(generateDrums(startMs, durationMs, genre, timeSignature))
            allEvents.addAll(generateBass(startMs, durationMs, chord, genre, timeSignature))
            allEvents.addAll(generateGuitar(startMs, durationMs, chord, genre, timeSignature))
        }

        return allEvents.sortedBy { it.timeMs }
    }

    private fun generateDrums(startMs: Long, durationMs: Long, genre: Genre, timeSignature: TimeSignature): List<BackingTrackGenerator.MidiNoteEvent> {
        val events = mutableListOf<BackingTrackGenerator.MidiNoteEvent>()

        when (genre) {
            Genre.ROCK -> {
                val is3 = timeSignature.beatsPerBar == 3

                // Hi-hat: straight 8ths, accented on each downbeat
                val hihatPattern = parsePattern(
                    if (is3) "<1>_&_<2>_&_<3>_&_" else "<1>_&_<2>_&_<3>_&_<4>_&_"
                )
                events.addAll(renderVoice(hihatPattern, startMs, durationMs, timeSignature, 9, 42, 65, 80))

                // Kick: beat 1 always (accented), beat 3 too in 4/4
                val kickPattern = parsePattern(
                    if (is3) "<1>___ ____ ____" else "<1>___ ____ 3___ ____"
                )
                events.addAll(renderVoice(kickPattern, startMs, durationMs, timeSignature, 9, 36, 95, 105, noteLengthMs = 100))

                // Snare: beat 2 always; beat 4 in 4/4, or a softer beat 3 in 3/4
                val snarePattern = parsePattern(
                    if (is3) "____ <2>___ 3___" else "____ 2___ ____ 4___"
                )
                events.addAll(renderVoice(snarePattern, startMs, durationMs, timeSignature, 9, 38, if (is3) 90 else 95, 95, noteLengthMs = 100))
            }
            Genre.BLUES -> {
                val is3 = timeSignature.beatsPerBar == 3

                // Shuffle ride/hat: accented hit on the beat, lighter hit on the shuffle "and"
                val ridePattern = parsePattern(
                    if (is3) "<x>_x<x>_x<x>_x" else "<x>_x<x>_x<x>_x<x>_x"
                )
                events.addAll(renderVoice(ridePattern, startMs, durationMs, timeSignature, 9, 42, 60, 75, ticksPerBeat = 3))

                // Kick: beat 1 (accented); beat 3 too in 4/4
                val kickPattern = parsePattern(
                    if (is3) "<x>__ ___ ___" else "<x>__ ___ x__ ___"
                )
                events.addAll(renderVoice(kickPattern, startMs, durationMs, timeSignature, 9, 36, 90, 100, noteLengthMs = 100, ticksPerBeat = 3))

                // Snare: beat 2 always; beat 4 in 4/4, or beat 3 in 3/4
                val snarePattern = parsePattern(
                    if (is3) "___ x__ x__" else "___ x__ ___ x__"
                )
                events.addAll(renderVoice(snarePattern, startMs, durationMs, timeSignature, 9, 38, 90, 90, noteLengthMs = 100, ticksPerBeat = 3))
            }
            Genre.COUNTRY -> {
                val is3 = timeSignature.beatsPerBar == 3

                // "Boom-Chicka" hat: hit on every beat, ghost hit on the "&" of every beat
                val hihatPattern = parsePattern(
                    if (is3) "<1>_&_<2>_&_<3>_&_" else "<1>_&_<2>_&_<3>_&_<4>_&_"
                )
                events.addAll(renderVoice(hihatPattern, startMs, durationMs, timeSignature, 9, 42, 50, 70))

                // Kick: beat 1 always; beat 3 too in 4/4
                val kickPattern = parsePattern(
                    if (is3) "<1>___ ____ ____" else "<1>___ ____ 3___ ____"
                )
                events.addAll(renderVoice(kickPattern, startMs, durationMs, timeSignature, 9, 36, 95, 100, noteLengthMs = 100))

                // Snare: "&" of beat 1, and "&" of beat 3 (4/4) or beat 2 (3/4)
                val snarePattern = parsePattern(
                    if (is3) "__&_ __&_ ____" else "__&_ ____ __&_ ____"
                )
                events.addAll(renderVoice(snarePattern, startMs, durationMs, timeSignature, 9, 38, 70, 70, noteLengthMs = 100))
            }
            Genre.FUNK -> {
                val is3 = timeSignature.beatsPerBar == 3

                // Hi-hat: every 16th, accented on each downbeat
                val hihatPattern = parsePattern(
                    if (is3) "<1>xxx<2>xxx<3>xxx" else "<1>xxx<2>xxx<3>xxx<4>xxx"
                )
                events.addAll(renderVoice(hihatPattern, startMs, durationMs, timeSignature, 9, 42, 50, 80, noteLengthMs = 40))

                // Kick: beat 1 (accented), a pickup before beat 2, a syncopated late hit in beat 3
                val kickPattern = parsePattern(
                    if (is3) "<1>__x ____ __x_" else "<1>__x ____ __x_ ____"
                )
                events.addAll(renderVoice(kickPattern, startMs, durationMs, timeSignature, 9, 36, 85, 100, noteLengthMs = 100))

                // Snare: beat 2, and the last beat of the bar
                val snarePattern = parsePattern(
                    if (is3) "____ x___ x___" else "____ x___ ____ x___"
                )
                events.addAll(renderVoice(snarePattern, startMs, durationMs, timeSignature, 9, 38, 100, 100, noteLengthMs = 100))
            }
            Genre.JAZZ -> {
                val is3 = timeSignature.beatsPerBar == 3

                // Swing ride: accented hit on every beat, lighter "swing a" leading into each backbeat
                val ridePattern = parsePattern(
                    if (is3) "<x>_x<x>_x<x>__" else "<x>_x<x>__<x>_x<x>__"
                )
                events.addAll(renderVoice(ridePattern, startMs, durationMs, timeSignature, 9, 51, 55, 70, noteLengthMs = 100, ticksPerBeat = 3))

                // Hi-hat pedal on the backbeats (beats 2 & 4 in 4/4, beats 2 & 3 in 3/4)
                val hihatPedalPattern = parsePattern(
                    if (is3) "___ x__ x__" else "___ x__ ___ x__"
                )
                events.addAll(renderVoice(hihatPedalPattern, startMs, durationMs, timeSignature, 9, 44, 80, 80, noteLengthMs = 50, ticksPerBeat = 3))
            }
        }
        return events
    }

    private fun generateBass(startMs: Long, durationMs: Long, chord: ResolvedChord, genre: Genre, timeSignature: TimeSignature): List<BackingTrackGenerator.MidiNoteEvent> {
        val events = mutableListOf<BackingTrackGenerator.MidiNoteEvent>()
        val root = findBassPitch(chord.rootPitchClass)
        val fifth = findBassPitch((chord.rootPitchClass + 7) % 12)

        when (genre) {
            Genre.COUNTRY -> {
                // Alternating root-fifth, one hit per beat
                val pattern = parsePattern("<x>" + "x".repeat(timeSignature.beatsPerBar - 1))
                events.addAll(renderPitchSequence(pattern, listOf(root, fifth), startMs, durationMs, timeSignature, 1, 87, 95, noteLengthMs = 400, ticksPerBeat = 1))
            }
            Genre.BLUES -> {
                // Walking bass fragment (1-3-5-6), one note per beat
                val third = findBassPitch((chord.rootPitchClass + chord.quality.intervals[1]) % 12)
                val sixth = findBassPitch((chord.rootPitchClass + 9) % 12)
                val pattern = parsePattern("<x>" + "x".repeat(timeSignature.beatsPerBar - 1))
                events.addAll(renderPitchSequence(pattern, listOf(root, third, fifth, sixth), startMs, durationMs, timeSignature, 1, 90, 95, noteLengthMs = 400, ticksPerBeat = 1))
            }
            Genre.FUNK -> {
                // Syncopated "slap" bass: root, root, octave pop, fifth
                val pattern = parsePattern("x__x" + "<x>__x" + "____".repeat(timeSignature.beatsPerBar - 2))
                events.addAll(renderPitchSequence(pattern, listOf(root, root, root + 12, fifth), startMs, durationMs, timeSignature, 1, 90, 110, noteLengthMs = 150, ticksPerBeat = 4))
            }
            Genre.JAZZ -> {
                // Walking bass, one note per beat
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

    private fun generateGuitar(startMs: Long, durationMs: Long, chord: ResolvedChord, genre: Genre, timeSignature: TimeSignature): List<BackingTrackGenerator.MidiNoteEvent> {
        val events = mutableListOf<BackingTrackGenerator.MidiNoteEvent>()
        val voicing = findGuitarVoicing(chord)

        when (genre) {
            Genre.ROCK -> {
                // Driving downstrokes, accented on beat 1
                val pattern = parsePattern("<x>" + "x".repeat(timeSignature.beatsPerBar - 1))
                val directions = parseDirections("d".repeat(timeSignature.beatsPerBar))
                events.addAll(renderStrum(pattern, directions, voicing, startMs, durationMs, timeSignature, 0, 72, 80, ticksPerBeat = 1))
            }
            Genre.BLUES -> {
                // Shuffle: down on the beat (full voicing), quick up on the shuffle tail (smaller voicing)
                val downPattern = parsePattern("x__".repeat(timeSignature.beatsPerBar))
                val downDirections = parseDirections("d__".repeat(timeSignature.beatsPerBar))
                events.addAll(renderStrum(downPattern, downDirections, voicing, startMs, durationMs, timeSignature, 0, 85, 85, ticksPerBeat = 3))

                val upPattern = parsePattern("__x".repeat(timeSignature.beatsPerBar))
                val upDirections = parseDirections("__u".repeat(timeSignature.beatsPerBar))
                events.addAll(renderStrum(upPattern, upDirections, voicing.takeLast(3), startMs, durationMs, timeSignature, 0, 60, 60, ticksPerBeat = 3))
            }
            Genre.COUNTRY -> {
                // "Chick": upstroke on the "&" of every beat
                val pattern = parsePattern("_x".repeat(timeSignature.beatsPerBar))
                val directions = parseDirections("_u".repeat(timeSignature.beatsPerBar))
                events.addAll(renderStrum(pattern, directions, voicing.takeLast(4), startMs, durationMs, timeSignature, 0, 75, 75, ticksPerBeat = 2))
            }
            Genre.FUNK -> {
                // Scratchy: down on the "&", up on the "a"
                val downPattern = parsePattern("__x_".repeat(timeSignature.beatsPerBar))
                val downDirections = parseDirections("__d_".repeat(timeSignature.beatsPerBar))
                events.addAll(renderStrum(downPattern, downDirections, voicing.takeLast(3), startMs, durationMs, timeSignature, 0, 80, 80, ticksPerBeat = 4))

                val upPattern = parsePattern("___x".repeat(timeSignature.beatsPerBar))
                val upDirections = parseDirections("___u".repeat(timeSignature.beatsPerBar))
                events.addAll(renderStrum(upPattern, upDirections, voicing.takeLast(3), startMs, durationMs, timeSignature, 0, 60, 60, ticksPerBeat = 4))
            }
            Genre.JAZZ -> {
                // "Freddie Green" shell voicings, downstroke every beat
                val pattern = parsePattern("x".repeat(timeSignature.beatsPerBar))
                val directions = parseDirections("d".repeat(timeSignature.beatsPerBar))
                events.addAll(renderStrum(pattern, directions, voicing.take(3), startMs, durationMs, timeSignature, 0, 65, 65, ticksPerBeat = 1))
            }
        }
        return events
    }

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
