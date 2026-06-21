package com.example.fretboardlayouts.audio

import com.example.fretboardlayouts.theory.Genre
import com.example.fretboardlayouts.theory.JamTimeline
import com.example.fretboardlayouts.theory.ResolvedChord
import com.example.fretboardlayouts.theory.midiNote
import com.example.fretboardlayouts.theory.pitchClassAt
import com.example.fretboardlayouts.theory.TimeSignature
import com.example.fretboardlayouts.theory.parsePattern
import com.example.fretboardlayouts.theory.StrumPreset


/**
 * The "Band-in-a-Box" style engine.
 * Takes a Chord Timeline and a Genre, and produces a multi-track MIDI performance.
 */
object StyleEngine {

    fun generateAccompaniment(timeline: JamTimeline, genre: Genre, guitarPreset: StrumPreset): List<BackingTrackGenerator.MidiNoteEvent> {
        val allEvents = mutableListOf<BackingTrackGenerator.MidiNoteEvent>()
        val timeSignature = timeline.timeSignature

        timeline.events.forEach { event ->
            val chord = event.chord
            val startMs = event.startMs
            val durationMs = event.durationMs

            allEvents.addAll(generateDrums(startMs, durationMs, genre, timeSignature))
            allEvents.addAll(generateBass(startMs, durationMs, chord, genre, timeSignature))
            allEvents.addAll(generateGuitar(startMs, durationMs, chord, guitarPreset, timeSignature))
        }

        return allEvents.sortedBy { it.timeMs }
    }

    private fun generateDrums(startMs: Long, durationMs: Long, genre: Genre, timeSignature: TimeSignature): List<BackingTrackGenerator.MidiNoteEvent> {
        val events = mutableListOf<BackingTrackGenerator.MidiNoteEvent>()

        when (genre) {
            Genre.ROCK -> {
                val hihatPattern = parsePattern(when (timeSignature.beatsPerBar) {
                    3 -> "<1>_&_<2>_&_<3>_&_"
                    5 -> "<1>_&_<2>_&_<3>_&_<4>_&_<5>_&_"
                    else -> "<1>_&_<2>_&_<3>_&_<4>_&_"
                })
                events.addAll(renderVoice(hihatPattern, startMs, durationMs, timeSignature, 9, 42, 65, 80))

                val kickPattern = parsePattern(when (timeSignature.beatsPerBar) {
                    3 -> "<1>___ ____ ____"
                    5 -> "<1>___ ____ ____ x___ ____"
                    else -> "<1>___ ____ 3___ ____"
                })
                events.addAll(renderVoice(kickPattern, startMs, durationMs, timeSignature, 9, 36, 95, 105, noteLengthMs = 100))

                val snarePattern = parsePattern(when (timeSignature.beatsPerBar) {
                    3 -> "____ <2>___ 3___"
                    5 -> "____ x___ ____ ____ x___"
                    else -> "____ 2___ ____ 4___"
                })
                events.addAll(renderVoice(snarePattern, startMs, durationMs, timeSignature, 9, 38, 95, 95, noteLengthMs = 100))
            }

            Genre.COUNTRY -> {
                val hihatPattern = parsePattern(when (timeSignature.beatsPerBar) {
                    3 -> "<1>_&_<2>_&_<3>_&_"
                    5 -> "<1>_&_<2>_&_<3>_&_<4>_&_<5>_&_"
                    else -> "<1>_&_<2>_&_<3>_&_<4>_&_"
                })
                events.addAll(renderVoice(hihatPattern, startMs, durationMs, timeSignature, 9, 42, 50, 70))

                val kickPattern = parsePattern(when (timeSignature.beatsPerBar) {
                    3 -> "<1>___ ____ ____"
                    5 -> "<1>___ ____ ____ x___ ____"
                    else -> "<1>___ ____ 3___ ____"
                })
                events.addAll(renderVoice(kickPattern, startMs, durationMs, timeSignature, 9, 36, 95, 100, noteLengthMs = 100))

                val snarePattern = parsePattern(when (timeSignature.beatsPerBar) {
                    3 -> "__x_ ____ ____"
                    5 -> "__x_ ____ ____ __x_ ____"
                    else -> "__x_ ____ __x_ ____"
                })
                events.addAll(renderVoice(snarePattern, startMs, durationMs, timeSignature, 9, 38, 70, 70, noteLengthMs = 100))
            }

            Genre.FUNK -> {
                val hihatPattern = parsePattern(when (timeSignature.beatsPerBar) {
                    3 -> "<1>xxx<2>xxx<3>xxx"
                    5 -> "<1>xxx<2>xxx<3>xxx<4>xxx<5>xxx"
                    else -> "<1>xxx<2>xxx<3>xxx<4>xxx"
                })
                events.addAll(renderVoice(hihatPattern, startMs, durationMs, timeSignature, 9, 42, 50, 80, noteLengthMs = 40))

                val kickPattern = parsePattern(when (timeSignature.beatsPerBar) {
                    3 -> "<1>__x ____ __x_"
                    5 -> "<1>__x ____ __x_ ____ ____"
                    else -> "<1>__x ____ __x_ ____"
                })
                events.addAll(renderVoice(kickPattern, startMs, durationMs, timeSignature, 9, 36, 85, 100, noteLengthMs = 100))

                val snarePattern = parsePattern(when (timeSignature.beatsPerBar) {
                    3 -> "____ x___ x___"
                    5 -> "____ x___ ____ x___ ____"
                    else -> "____ x___ ____ x___"
                })
                events.addAll(renderVoice(snarePattern, startMs, durationMs, timeSignature, 9, 38, 100, 100, noteLengthMs = 100))
            }

            Genre.BLUES -> {
                val ridePattern = parsePattern(when (timeSignature.beatsPerBar) {
                    3 -> "<x>_x<x>_x<x>_x"
                    5 -> "<x>_x<x>_x<x>_x<x>_x<x>_x"
                    else -> "<x>_x<x>_x<x>_x<x>_x"
                })
                events.addAll(renderVoice(ridePattern, startMs, durationMs, timeSignature, 9, 42, 60, 75, ticksPerBeat = 3))

                val kickPattern = parsePattern(when (timeSignature.beatsPerBar) {
                    3 -> "<x>__ ___ ___"
                    5 -> "<x>__ ___ x__ ___ ___"
                    else -> "<x>__ ___ x__ ___"
                })
                events.addAll(renderVoice(kickPattern, startMs, durationMs, timeSignature, 9, 36, 90, 100, noteLengthMs = 100, ticksPerBeat = 3))

                val snarePattern = parsePattern(when (timeSignature.beatsPerBar) {
                    3 -> "___ x__ x__"
                    5 -> "___ x__ ___ x__ ___"
                    else -> "___ x__ ___ x__"
                })
                events.addAll(renderVoice(snarePattern, startMs, durationMs, timeSignature, 9, 38, 90, 90, noteLengthMs = 100, ticksPerBeat = 3))
            }

            Genre.JAZZ -> {
                val ridePattern = parsePattern(when (timeSignature.beatsPerBar) {
                    3 -> "<x>_x<x>_x<x>__"
                    5 -> "<x>_x<x>__<x>_x<x>__<x>__"
                    else -> "<x>_x<x>__<x>_x<x>__"
                })
                events.addAll(renderVoice(ridePattern, startMs, durationMs, timeSignature, 9, 51, 55, 70, noteLengthMs = 100, ticksPerBeat = 3))

                val hihatPedalPattern = parsePattern(when (timeSignature.beatsPerBar) {
                    3 -> "___ x__ x__"
                    5 -> "___ x__ ___ x__ ___"
                    else -> "___ x__ ___ x__"
                })
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

    private fun generateGuitar(startMs: Long, durationMs: Long, chord: ResolvedChord, preset: StrumPreset, timeSignature: TimeSignature): List<BackingTrackGenerator.MidiNoteEvent> {
        val voicing = findGuitarVoicing(chord)
        return renderPreset(preset, voicing, startMs, durationMs, timeSignature, channel = 0)
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
