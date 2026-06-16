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
                // Shuffle feel (triplet based)
                for (beat in 0 until timeSignature.beatsPerBar) {
                    events.add(BackingTrackGenerator.MidiNoteEvent(startMs + tripletToMs(beat, 0, timeSignature, durationMs), 9, 42, 75, 50)) // Ride/Hat
                    events.add(BackingTrackGenerator.MidiNoteEvent(startMs + tripletToMs(beat, 2, timeSignature, durationMs), 9, 42, 60, 50)) // Ride/Hat "and"
                }

                events.add(BackingTrackGenerator.MidiNoteEvent(startMs + tripletToMs(0, 0, timeSignature, durationMs), 9, 36, 100, 100)) // Kick
                if (timeSignature.beatsPerBar >= 4) {
                    events.add(BackingTrackGenerator.MidiNoteEvent(startMs + tripletToMs(2, 0, timeSignature, durationMs), 9, 36, 90, 100)) // Kick
                }

                events.add(BackingTrackGenerator.MidiNoteEvent(startMs + tripletToMs(1, 0, timeSignature, durationMs), 9, 38, 90, 100)) // Snare
                if (timeSignature.beatsPerBar >= 4) {
                    events.add(BackingTrackGenerator.MidiNoteEvent(startMs + tripletToMs(3, 0, timeSignature, durationMs), 9, 38, 90, 100)) // Snare
                } else if (timeSignature.beatsPerBar == 3) {
                    events.add(BackingTrackGenerator.MidiNoteEvent(startMs + tripletToMs(2, 0, timeSignature, durationMs), 9, 38, 90, 100)) // Snare
                }
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
                // Swing ride cymbal with backbeat hi-hat pedal
                val backbeats = if (timeSignature.beatsPerBar >= 4) listOf(1, 3) else listOf(1, 2)
                for (beat in 0 until timeSignature.beatsPerBar) {
                    events.add(BackingTrackGenerator.MidiNoteEvent(startMs + tripletToMs(beat, 0, timeSignature, durationMs), 9, 51, 70, 100)) // Ride
                    if (beat in backbeats) {
                        events.add(BackingTrackGenerator.MidiNoteEvent(startMs + tripletToMs(beat, 0, timeSignature, durationMs), 9, 44, 80, 50)) // Hi-Hat Pedal
                        events.add(BackingTrackGenerator.MidiNoteEvent(startMs + tripletToMs(beat - 1, 2, timeSignature, durationMs), 9, 51, 55, 100)) // Swing "a"
                    }
                }
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
                // Alternating Root-Fifth, one per beat
                val grid = timeSignature.subdivisionCount
                val slotsPerBeat = grid / timeSignature.beatsPerBar
                for (beat in 0 until timeSignature.beatsPerBar) {
                    val pitch = if (beat % 2 == 0) root else fifth
                    val velocity = when (beat) { 0 -> 95; 2 -> 90; else -> 85 }
                    events.add(BackingTrackGenerator.MidiNoteEvent(startMs + slotToMs(beat * slotsPerBeat, timeSignature, durationMs), 1, pitch, velocity, 400))
                }
            }
            Genre.BLUES -> {
                // Walking bass fragment (1-3-5-6), one note per beat
                val third = findBassPitch((chord.rootPitchClass + chord.quality.intervals[1]) % 12)
                val sixth = findBassPitch((chord.rootPitchClass + 9) % 12)
                val walkNotes = listOf(root, third, fifth, sixth)
                val grid = timeSignature.subdivisionCount
                val slotsPerBeat = grid / timeSignature.beatsPerBar
                for (beat in 0 until timeSignature.beatsPerBar) {
                    val velocity = if (beat == 0) 95 else 90
                    events.add(BackingTrackGenerator.MidiNoteEvent(startMs + slotToMs(beat * slotsPerBeat, timeSignature, durationMs), 1, walkNotes[beat % walkNotes.size], velocity, 400))
                }
            }
            Genre.FUNK -> {
                // Syncopated "Slap" Bass
                val grid = timeSignature.subdivisionCount
                val slotsPerBeat = grid / timeSignature.beatsPerBar
                events.add(BackingTrackGenerator.MidiNoteEvent(startMs + slotToMs(0, timeSignature, durationMs), 1, root, 100, 200))
                events.add(BackingTrackGenerator.MidiNoteEvent(startMs + slotToMs(slotsPerBeat - 1, timeSignature, durationMs), 1, root, 80, 100))
                events.add(BackingTrackGenerator.MidiNoteEvent(startMs + slotToMs(slotsPerBeat, timeSignature, durationMs), 1, root + 12, 110, 150)) // Octave pop
                events.add(BackingTrackGenerator.MidiNoteEvent(startMs + slotToMs(slotsPerBeat + slotsPerBeat - 1, timeSignature, durationMs), 1, fifth, 90, 150))
            }
            Genre.JAZZ -> {
                // Walking bass, one note per beat
                val third = findBassPitch((chord.rootPitchClass + chord.quality.intervals[1]) % 12)
                val sixth = findBassPitch((chord.rootPitchClass + 9) % 12)
                val walkNotes = listOf(root, third, fifth, sixth)
                val grid = timeSignature.subdivisionCount
                val slotsPerBeat = grid / timeSignature.beatsPerBar
                for (beat in 0 until timeSignature.beatsPerBar) {
                    val velocity = if (beat == 0) 90 else 85
                    events.add(BackingTrackGenerator.MidiNoteEvent(startMs + slotToMs(beat * slotsPerBeat, timeSignature, durationMs), 1, walkNotes[beat % walkNotes.size], velocity, 400))
                }
            }
            else -> {
                // Rock: Pedal root notes (8th notes)
                val eighth = durationMs / 8
                for (i in 0 until 8) {
                    events.add(BackingTrackGenerator.MidiNoteEvent(startMs + i * eighth, 1, root, 85, 200))
                }
            }
        }
        return events
    }

    private fun generateGuitar(startMs: Long, durationMs: Long, chord: ResolvedChord, genre: Genre, timeSignature: TimeSignature): List<BackingTrackGenerator.MidiNoteEvent> {
        val events = mutableListOf<BackingTrackGenerator.MidiNoteEvent>()
        val voicing = findGuitarVoicing(chord)

        when (genre) {
            Genre.ROCK -> {
                // Driving downstrokes on every beat
                val grid = timeSignature.subdivisionCount
                val slotsPerBeat = grid / timeSignature.beatsPerBar
                for (beat in 0 until timeSignature.beatsPerBar) {
                    val slot = beat * slotsPerBeat
                    addStrum(events, startMs + slotToMs(slot, timeSignature, durationMs), 0, voicing, 80 - (beat * 5), isDownstroke = true)
                }
            }
            Genre.BLUES -> {
                // Shuffle rhythm: down on each beat, short "up" on the shuffle tail
                for (beat in 0 until timeSignature.beatsPerBar) {
                    addStrum(events, startMs + tripletToMs(beat, 0, timeSignature, durationMs), 0, voicing, 85, isDownstroke = true)
                    addStrum(events, startMs + tripletToMs(beat, 2, timeSignature, durationMs), 0, voicing.takeLast(3), 60, isDownstroke = false)
                }
            }
            Genre.COUNTRY -> {
                // Boom-Chicka: Bass note then high strings
                // (Bass handles the "Boom", Guitar handles the "Chicka")
                val grid = timeSignature.subdivisionCount
                val slotsPerBeat = grid / timeSignature.beatsPerBar
                val halfBeat = slotsPerBeat / 2
                for (beat in 0 until timeSignature.beatsPerBar) {
                    val slot = beat * slotsPerBeat + halfBeat
                    // High string "chick" on the off-beat
                    addStrum(events, startMs + slotToMs(slot, timeSignature, durationMs), 0, voicing.takeLast(4), 75, isDownstroke = false)
                }
            }
            Genre.FUNK -> {
                // Scratchy syncopated rhythm: hits on the "&" and "a" of every beat
                val grid = timeSignature.subdivisionCount
                val slotsPerBeat = grid / timeSignature.beatsPerBar
                for (slot in 0 until grid) {
                    val offset = slot % slotsPerBeat
                    if (offset == 2 || offset == 3) {
                        addStrum(events, startMs + slotToMs(slot, timeSignature, durationMs), 0, voicing.takeLast(3), if (offset == 2) 80 else 60, slot % 2 == 0)
                    }
                }
            }
            Genre.JAZZ -> {
                // "Freddie Green" style shell voicings on every beat
                val grid = timeSignature.subdivisionCount
                val slotsPerBeat = grid / timeSignature.beatsPerBar
                for (beat in 0 until timeSignature.beatsPerBar) {
                    addStrum(events, startMs + slotToMs(beat * slotsPerBeat, timeSignature, durationMs), 0, voicing.take(3), 65, true)
                }
            }
        }
        return events
    }

    private fun addStrum(events: MutableList<BackingTrackGenerator.MidiNoteEvent>, time: Long, channel: Int, pitches: List<Int>, velocity: Int, isDownstroke: Boolean) {
        val sortedPitches = if (isDownstroke) pitches.sorted() else pitches.sortedDescending()
        sortedPitches.forEachIndexed { i, pitch ->
            events.add(BackingTrackGenerator.MidiNoteEvent(time + (i * 20), channel, pitch, velocity, 800))
        }
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
