package com.example.fretboardlayouts.theory

// ================================================================
// VOICE LEADING ENGINE
// MODIFIED made by Claude 25/08/2026 — rewrote leadTo() algorithm.
//
// Previous algorithm mapped voice i → interval i (index-based).
// This forced voice 0 to always target the root, voice 1 the 3rd, etc.
// Result: notes jumped by octave instead of moving by minimal semitones.
//
// New algorithm — nearest pitch class (correct voice leading):
//   1. For each current voice, find the closest occurrence of ANY target
//      pitch class within range (above or below, closest wins).
//   2. Verify all target PCs are represented; insert any missing near midpoint.
//   3. Deduplicate, sort, return.
//
// This implements the "minimal movement" principle properly: common tones
// stay put, moving voices travel the shortest available path, regardless
// of which interval they're targeting.
// ================================================================

import kotlin.math.abs

object VoiceLeadingEngine {

    const val GUITAR_MIN = 40
    const val GUITAR_MAX = 76
    const val PIANO_MIN  = 48
    const val PIANO_MAX  = 72

    fun leadTo(
        currentVoicing: List<Int>,
        nextRootPitchClass: Int,
        nextQuality: ChordQuality,
        chordType: ChordType = ChordType.FULL,
        rangeMin: Int,
        rangeMax: Int
    ): List<Int> {
        // No previous voicing — plain root-position build (unchanged)
        if (currentVoicing.isEmpty()) {
            val rootMidi = ChordNoteBuilder.nearestMidi(nextRootPitchClass, rangeMin)
                .let { if (it > rangeMin + 15) it - 12 else it }
            return ChordNoteBuilder.buildNotes(rootMidi, nextQuality, chordType)
                .filter { it in rangeMin..rangeMax }
        }

        // Unique target pitch classes for the incoming chord
        val targetPCs = ChordNoteBuilder.intervalsFor(nextQuality, chordType)
            .map { (nextRootPitchClass + it) % 12 }
            .distinct()

        val sorted = currentVoicing.sorted()

        // Step 1 — each current voice moves to the nearest occurrence of ANY target PC.
        // Generates three candidates per PC (one octave below, same octave, one above)
        // then picks whichever is closest to that voice. Common tones naturally stay put
        // because their distance is 0.
        val voiceLedNotes = sorted.map { prevNote ->
            val octaveBase = (prevNote / 12) * 12
            targetPCs
                .flatMap { pc ->
                    listOf(octaveBase - 12 + pc, octaveBase + pc, octaveBase + 12 + pc)
                }
                .filter { it in rangeMin..rangeMax }
                .minByOrNull { abs(it - prevNote) }
                ?: prevNote  // fallback: stay put if nothing in range
        }.toMutableList()

        // Step 2 — ensure all target PCs are represented.
        // Two voices can legally converge on the same PC (different octaves = fine).
        // But if a PC is completely absent, insert it near the register midpoint.
        val midpoint = sorted[sorted.size / 2]
        val coveredPCs = voiceLedNotes.map { it % 12 }.toSet()
        val missingPCs = targetPCs.filter { it !in coveredPCs }

        for (pc in missingPCs) {
            val octaveBase = (midpoint / 12) * 12
            val candidate = listOf(octaveBase - 12 + pc, octaveBase + pc, octaveBase + 12 + pc)
                .filter { it in rangeMin..rangeMax }
                .minByOrNull { abs(it - midpoint) }
            if (candidate != null && candidate !in voiceLedNotes) {
                voiceLedNotes.add(candidate)
            }
        }

        return voiceLedNotes.distinct().sorted()
    }

    fun leadToGuitar(
        currentVoicing: List<Int>,
        nextRootPitchClass: Int,
        nextQuality: ChordQuality,
        chordType: ChordType = ChordType.FULL
    ): List<Int> = leadTo(currentVoicing, nextRootPitchClass, nextQuality, chordType, GUITAR_MIN, GUITAR_MAX)

    fun leadToPiano(
        currentVoicing: List<Int>,
        nextRootPitchClass: Int,
        nextQuality: ChordQuality,
        chordType: ChordType = ChordType.FULL
    ): List<Int> = leadTo(currentVoicing, nextRootPitchClass, nextQuality, chordType, PIANO_MIN, PIANO_MAX)

    // ── Helpers (unchanged) ───────────────────────────────────────

    fun closestMidi(pitchClass: Int, nearMidi: Int): Int {
        val base  = (nearMidi / 12) * 12 + pitchClass
        val above = if (base >= nearMidi) base else base + 12
        val below = above - 12
        return if (abs(nearMidi - below) <= abs(nearMidi - above)) below else above
    }

    fun totalMovement(from: List<Int>, to: List<Int>): Int {
        val f = from.sorted()
        val t = to.sorted()
        return f.zip(t).sumOf { (a, b) -> abs(a - b) }
    }

    fun isCommonTone(pitchClass: Int, voicing: List<Int>): Boolean =
        voicing.any { it % 12 == pitchClass }
}