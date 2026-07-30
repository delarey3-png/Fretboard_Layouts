package com.example.fretboardlayouts.brain

import kotlin.random.Random

/*
|--------------------------------------------------------------------------
| BassBrain.kt
|--------------------------------------------------------------------------
|
| Created: 25 July 2026
| Author : Claude (Anthropic)
|
| CHANGE LOG
| ----------
| 25/07 — Initial version. Pattern chosen at construction time.
| 25/07 — MODIFIED: Lazy pattern init so tempo is available at
|          first generateBar() call. Tempo-aware pattern filtering
|          prevents 1-beat motifs sounding too fast at slow tempos.
|
| WHY LAZY INIT?
| --------------
| The original design pre-selected the pattern in the constructor,
| but tempo wasn't available yet. Moving selection to first call
| of generateBar() means we know the actual tempo and can filter
| patterns appropriately.
|
| TEMPO FILTERING LOGIC
| ----------------------
| Patterns have totalBeats = sum of note durations in beats.
|
|   totalBeats = 1.0 → 1-beat motif, repeats 4× per bar
|   totalBeats = 2.0 → 2-beat motif, repeats 2× per bar (8th density)
|   totalBeats = 4.0 → full bar pattern, plays once
|
| At 70 BPM a 1-beat motif repeating 4× per bar sounds frantic
| because the individual notes stay at 16th-note spacing in a
| very slow bar. Filtering by totalBeats based on tempo fixes this.
|
|   tempo < 80  BPM → require totalBeats >= 2.0 (8th note or coarser)
|   tempo < 110 BPM → require totalBeats >= 1.0 (16th OK, 32nd filtered)
|   tempo >= 110 BPM → all patterns allowed
|
| If filtering removes all patterns for a genre, falls back to the
| full list (always better than silence).
|
|--------------------------------------------------------------------------
*/

class BassBrain(
    private val model: BassModel,
    seed: Long = System.currentTimeMillis()
) {

    private val random = Random(seed)

    /*
    | Pattern is chosen lazily on first generateBar() or generateTrack() call.
    |
    | This ensures the tempo is known before selection, allowing
    | the filter to exclude patterns that are too dense for the tempo.
    |
    | Same seed + same genre + same tempo range = always the same pattern.
    */
    private lateinit var activePattern: BassPattern

    // =====================================================================
    // PUBLIC API
    // =====================================================================

    /**
     * Generates one complete bar of bass.
     *
     * Pattern is selected on first call using the provided tempo.
     * Subsequent calls reuse the same pattern (consistent groove).
     *
     * @param barNumber     Bar index in the song (0 = first bar)
     * @param chordRootMidi MIDI note of the active chord root (28–52)
     * @param tempo         Song tempo in BPM — used for pattern selection and timing
     */
    fun generateBar(barNumber: Int, chordRootMidi: Int, tempo: Int): BassBar {
        if (!::activePattern.isInitialized) {
            activePattern = choosePattern(tempo)
        }
        return renderPatternToBar(activePattern, barNumber, chordRootMidi, tempo)
    }

    /**
     * Generates a complete bass performance for a chord progression.
     *
     * @param chordRootsPerBar MIDI root note per bar (from StyleEngine progression)
     * @param tempo            Song tempo in BPM
     */
    fun generateTrack(chordRootsPerBar: List<Int>, tempo: Int): BassTrack {
        if (!::activePattern.isInitialized) {
            activePattern = choosePattern(tempo)
        }
        val track = BassTrack()
        chordRootsPerBar.forEachIndexed { barIndex, rootMidi ->
            track.addBar(renderPatternToBar(activePattern, barIndex, rootMidi, tempo))
        }
        return track
    }

    /** Name of the active pattern — for debug display. */
    fun activePatternName(): String =
        if (::activePattern.isInitialized) activePattern.name else "not yet selected"

    /** Feel of the active pattern. */
    fun activePatternFeel(): String =
        if (::activePattern.isInitialized) activePattern.feel else "—"

    /** Total beats of the active pattern. */
    fun activePatternTotalBeats(): Float =
        if (::activePattern.isInitialized) activePattern.totalBeats else 0f

    // =====================================================================
    // PATTERN SELECTION — updated 25/07 made by Claude
    // =====================================================================
    //
    // Two-layer filter:
    //
    // Layer 1 — totalBeats gate (tempo < 80 BPM)
    //   Very slow songs: require totalBeats >= 2.0 so patterns repeat
    //   at most twice per bar (8th note density max).
    //
    // Layer 2 — maxDenominator gate (tempo <= 120 BPM)
    //   Patterns with 32nd note subdivisions (denom=32) produce notes
    //   as short as 93ms at 80 BPM and 62ms at 120 BPM — below the
    //   threshold where the ear hears them as individual notes.
    //   Filter all 32nd note patterns at moderate tempos.
    //   Above 120 BPM all patterns are allowed.
    //
    // Falls back to the full list if filtering removes all candidates
    // (e.g. a genre with only 32nd note patterns).
    //
    private fun choosePattern(tempo: Int = 120): BassPattern { // MODIFIED made by Claude 25/07
        if (model.patterns.isEmpty()) {
            throw IllegalStateException(
                "BassModel '${model.genre}' has no patterns — check BassModels.kt"
            )
        }

        val candidates = model.patterns.filter { pattern ->
            val maxDenom = pattern.rhythmDenominators.maxOrNull() ?: 4
            when {
                // Very slow: 8th note density max + no 32nd notes
                tempo < 80  -> pattern.totalBeats >= 2.0f && maxDenom <= 8

                // Moderate (includes 80 BPM): 16th note max — blocks all 32nd patterns
                tempo <= 120 -> maxDenom <= 16

                // Fast: all patterns allowed
                else         -> true
            }
        }.takeIf { it.isNotEmpty() } ?: model.patterns   // fallback: never silence the bass

        val totalWeight = candidates.sumOf { it.occurrences }
        if (totalWeight == 0) return candidates.first()

        var pick = random.nextInt(totalWeight)
        for (pattern in candidates) {
            pick -= pattern.occurrences
            if (pick <= 0) return pattern
        }
        return candidates.last()
    }

    // =====================================================================
    // PATTERN RENDERING
    // =====================================================================

    private fun renderPatternToBar(
        pattern: BassPattern,
        barNumber: Int,
        chordRootMidi: Int,
        tempo: Int
    ): BassBar {
        val bar = BassBar(barNumber = barNumber, chordRootMidi = chordRootMidi)
        if (pattern.intervals.isEmpty()) return bar

        val msPerBeat  = 60_000.0 / tempo
        val totalBarMs = msPerBeat * 4.0
        val msPerStep  = totalBarMs / 16.0

        var currentMs  = 0.0
        var patternIdx = 0
        val maxNotes   = 64

        while (currentMs < totalBarMs - (msPerStep * 0.25) && patternIdx < maxNotes) {

            val idx      = patternIdx % pattern.intervals.size
            val interval = pattern.intervals[idx]
            val denom    = pattern.rhythmDenominators.getOrElse(idx) { 4 }
            val noteDurationMs = (4.0 / denom) * msPerBeat

            if (currentMs + noteDurationMs > totalBarMs + (msPerStep * 0.5)) break

            val step          = (currentMs / msPerStep).toInt().coerceIn(0, 15)
            val stepStartMs   = step * msPerStep
            val subStepOffset = (currentMs - stepStartMs).toFloat()

            val fullSteps     = ((noteDurationMs / msPerStep) + 0.5).toInt().coerceAtLeast(1)
            val durationSteps = (fullSteps * 0.88).toInt().coerceAtLeast(1)

            val midiNote = BassModels.clampToBassRegister(chordRootMidi + interval)
            val velocity = generateVelocity(step)

            bar.add(BassEvent(
                step          = step,
                midiNote      = midiNote,
                velocity      = velocity,
                durationSteps = durationSteps,
                timingOffset  = subStepOffset,
                probability   = 1f
            ))

            currentMs  += noteDurationMs
            patternIdx++
        }

        return bar
    }

    // =====================================================================
    // VELOCITY
    // =====================================================================

    private fun generateVelocity(step: Int): Int {
        val profile   = model.velocityProfile
        val isAccent  = step % 4 == 0
        val variation = random.nextInt(-profile.stdDev, profile.stdDev + 1)
        val accent    = if (isAccent) 4 else -2
        return (profile.average + variation + accent).coerceIn(profile.minimum, profile.maximum)
    }

    // =====================================================================
    // COMPANION
    // =====================================================================

    companion object {
        fun forGenre(genre: String, seed: Long = System.currentTimeMillis()): BassBrain {
            return BassBrain(BassModels.forGenre(genre), seed)
        }
    }
}