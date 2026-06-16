package com.example.fretboardlayouts.audio

import com.example.fretboardlayouts.theory.JamTimeline
import com.example.fretboardlayouts.theory.ResolvedChord
import com.example.fretboardlayouts.theory.midiNote
import com.example.fretboardlayouts.theory.pitchClassAt

/**
 * Generates musical MIDI events for the entire jam session.
 * This is the "brain" that turns chord progressions into multi-instrument tracks.
 */
object BackingTrackGenerator {

    /** 
     * Represents a single note to be played at a specific time.
     */
    data class MidiNoteEvent(
        val timeMs: Long,
        val channel: Int, // 0=Guitar, 1=Bass, 9=Drums (standard MIDI)
        val pitch: Int,
        val velocity: Int,
        val durationMs: Int
    )

    /**
     * Builds a list of all midi events for one loop of the timeline.
     */
    fun generateLoopEvents(timeline: JamTimeline): List<MidiNoteEvent> {
        val events = mutableListOf<MidiNoteEvent>()
        
        timeline.events.forEach { timelineEvent ->
            val chord = timelineEvent.chord
            val startTime = timelineEvent.startMs
            val duration = timelineEvent.durationMs
            
            // 1. GENERATE RHYTHM GUITAR (Channel 0)
            // We'll play a "Down-Up-Down-DownUp" style pattern or similar
            // For now, let's stick to a solid downstroke on beats 1 and 3
            val guitarVoicing = findGuitarVoicing(chord)
            
            // Beat 1 Strum
            addStrum(events, startTime, 0, guitarVoicing, 80)
            
            // Beat 3 Strum (mid-way through chord)
            addStrum(events, startTime + (duration / 2), 0, guitarVoicing, 70)

            // 2. GENERATE BASS (Channel 1)
            // Play the root on beat 1 and beat 3
            val rootPitch = findBassPitch(chord.rootPitchClass)
            events.add(MidiNoteEvent(startTime, 1, rootPitch, 90, (duration/2).toInt()))
            events.add(MidiNoteEvent(startTime + (duration/2), 1, rootPitch, 85, (duration/2).toInt()))

            // 3. GENERATE DRUMS (Channel 9)
            // Simple Kick-Snare-Kick-Snare
            val quarter = duration / 4
            events.add(MidiNoteEvent(startTime, 9, 36, 100, 100)) // Kick (C1)
            events.add(MidiNoteEvent(startTime + quarter, 9, 38, 90, 100)) // Snare (D1)
            events.add(MidiNoteEvent(startTime + 2 * quarter, 9, 36, 95, 100)) // Kick
            events.add(MidiNoteEvent(startTime + 3 * quarter, 9, 38, 90, 100)) // Snare
        }
        
        return events.sortedBy { it.timeMs }
    }

    private fun addStrum(
        events: MutableList<MidiNoteEvent>,
        time: Long,
        channel: Int,
        pitches: List<Int>,
        velocity: Int
    ) {
        pitches.forEachIndexed { i, pitch ->
            // 25ms delay between strings for a realistic strum
            events.add(MidiNoteEvent(time + (i * 25), channel, pitch, velocity, 1000))
        }
    }

    /** Finds a musical 4-6 string voicing for a chord in the open/cowboy position */
    private fun findGuitarVoicing(chord: ResolvedChord): List<Int> {
        val chordPcs = chord.chordTonePitchClasses.toSet()
        return (0..5).mapNotNull { stringIndex ->
            // Search frets 0-5 for the "sweet spot" of rhythm playing
            (0..5).map { fret -> 
                fret to pitchClassAt(stringIndex, fret)
            }.filter { it.second in chordPcs }
             .minByOrNull { it.first }
             ?.let { (fret, _) -> midiNote(stringIndex, fret) }
        }.sorted() // Low to high
    }

    private fun findBassPitch(pitchClass: Int): Int {
        // Find a low root note (E1 to Eb2 range, MIDI 28-39 approx)
        var pitch = 28 + pitchClass
        while (pitch < 28) pitch += 12
        while (pitch > 40) pitch -= 12
        return pitch
    }
}
