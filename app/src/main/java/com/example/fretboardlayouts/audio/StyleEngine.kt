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
import com.example.fretboardlayouts.theory.HumanisationLevel // made by Claude 11/07
import com.example.fretboardlayouts.theory.humanisationProfile // made by Claude 11/07
import com.example.fretboardlayouts.theory.humaniseVelocity // made by Claude 11/07
import com.example.fretboardlayouts.theory.humaniseTiming   // made by Claude 11/07
import com.example.fretboardlayouts.theory.humaniseDuration // made by Claude 11/07
import com.example.fretboardlayouts.theory.GrooveType       // made by Claude 11/07
import com.example.fretboardlayouts.theory.grooveOffsetMs   // made by Claude 11/07
import com.example.fretboardlayouts.theory.InstrumentRole  // made by Claude 11/07

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

    // NEW made by Claude 05/08/2026
// Per-channel velocity multiplier — tuned by ear after initial values
    private val channelVolumeScale = mapOf(
        0 to 1.00f,  // Guitar  — reference level
        1 to 0.88f,  // Bass    — slightly under guitar
        2 to 0.80f,  // Piano
        3 to 0.75f,  // Strings
        9 to 0.95f   // Drums
    )

    fun generateAccompaniment( // made by Claude 11/07: added humanisation
        timeline: JamTimeline,
        genre: Genre,
        guitarPreset: StrumPreset,
        pickingPreset: PickingPreset? = null,
        humanisationLevel: HumanisationLevel = HumanisationLevel.OFF,
        instrumentRoles: Map<String, InstrumentRole> = emptyMap() // made by Claude 11/07
    ): List<BackingTrackGenerator.MidiNoteEvent> {
        val allEvents = mutableListOf<BackingTrackGenerator.MidiNoteEvent>()
        val timeSignature = timeline.timeSignature

        timeline.events.forEachIndexed { barIndex, event ->        // MODIFIED — forEach → forEachIndexed
            val chord = event.chord
            val startMs = event.startMs
            val durationMs = event.durationMs

            // made by Claude 11/07: Role-aware generation — only active channels produce events
            val drumsRole  = instrumentRoles["drums"]   ?: InstrumentRole.STRUM_CHORD
            val bassRole   = instrumentRoles["bass"]    ?: InstrumentRole.STRUM_CHORD
            val guitarRole = instrumentRoles["guitar"]  ?: InstrumentRole.STRUM_CHORD
            val pianoRole  = instrumentRoles["piano"]   ?: InstrumentRole.OFF
            val stringsRole = instrumentRoles["strings"] ?: InstrumentRole.OFF

            // REVERTED 31/07 made by Claude — back past the whole Brain era (BassBrain,
            // DrumBrain, density filters, kick-lock) AND past the 23/07 DrumPreset (193
            // presets) system, to the original genre-keyed generateDrums()/generateBass().
            // Reasoning: ensemble cohesion needs to be designed in via matched, deterministic
            // patterns (kick/bass sharing beat 1&3, same principle ako/backing-tracks uses),
            // not reconciled after the fact between independently-selected real recordings.
            // See CLAUDE.md "Data Pipeline & Brain Architecture" and
            // NEXT_SESSION_HANDOFF_BRAIN_REVERT.md for full reasoning. The brain/ package
            // and theory/DrumPreset_clean.kt were fully deleted the same day, once the
            // revert was confirmed working — recoverable from git history (commit
            // c26a60e "Created brain package...") if ever needed again.
            if (drumsRole != InstrumentRole.OFF)
                allEvents.addAll(generateDrums(startMs, durationMs, genre, timeSignature))

            if (bassRole != InstrumentRole.OFF)
                allEvents.addAll(generateBass(startMs, durationMs, chord, genre, timeSignature))

            if (guitarRole != InstrumentRole.OFF) {
                if (pickingPreset != null && pickingPreset.layers.isNotEmpty()
                    && guitarRole == InstrumentRole.PICK_ARPEGGIO) {
                    allEvents.addAll(generateGuitarPicking(startMs, durationMs, chord, pickingPreset, timeSignature))
                } else {
                    allEvents.addAll(generateGuitar(startMs, durationMs, chord, guitarPreset, timeSignature))
                }
            }

            if (pianoRole != InstrumentRole.OFF)  // made by Claude 11/07
                allEvents.addAll(generatePiano(startMs, durationMs, chord, genre, timeSignature, pianoRole))

            if (stringsRole != InstrumentRole.OFF)  // made by Claude 11/07
                allEvents.addAll(generateStrings(startMs, durationMs, chord))
        }

        // made by Claude 11/07: Apply humanisation as post-processing step
        // Accent threshold >= 95 matches kick/accent velocities in all genre patterns
        // Each instrument rolls independently — same setting, different feel per channel
        // MODIFIED made by Claude 05/08/2026 — apply channel volume scaling even without humanisation
        if (humanisationLevel == HumanisationLevel.OFF) return allEvents.map { event ->
            event.copy(velocity = (event.velocity * (channelVolumeScale[event.channel] ?: 1.0f)).toInt().coerceIn(1, 127))
        }
        val humanProfile = humanisationProfile(humanisationLevel)

        // made by Claude 11/07: Groove template — consistent genre feel on top of random variation
        val grooveType = genreGroove(genre)
        val beatDurationMs = if (timeline.events.isNotEmpty())
            timeline.events.first().durationMs / timeline.timeSignature.beatsPerBar
        else 500L

        return allEvents.map { event ->
            val isAccent = event.velocity >= 95
            val groove = grooveOffsetMs(event.timeMs, beatDurationMs, grooveType, event.channel)
            event.copy(
                // MODIFIED made by Claude 05/08/2026 — scale after humanisation so channel balance survives velocity variation
                velocity = (humaniseVelocity(
                    baseVelocity = event.velocity,
                    channel = event.channel,
                    isAccent = isAccent,
                    profile = humanProfile
                ) * (channelVolumeScale[event.channel] ?: 1.0f)).toInt().coerceIn(1, 127),
                timeMs = (humaniseTiming(
                    baseTimeMs = event.timeMs,
                    channel = event.channel,
                    isAccent = isAccent,
                    profile = humanProfile
                ) + groove).coerceAtLeast(0L),
                durationMs = if (event.channel == 0) {
                    (humaniseDuration(event.durationMs, event.channel, humanProfile) * 1.15f).toInt()
                } else {
                    humaniseDuration(event.durationMs, event.channel, humanProfile)
                }
            )
        }
    }
    // made by Claude 11/07: Genre groove type mapping
    private fun genreGroove(genre: Genre): GrooveType = when (genre) {
        Genre.JAZZ    -> GrooveType.LAID_BACK  // classic laid-back swing feel
        Genre.BLUES   -> GrooveType.LAID_BACK  // laid-back blues feel
        Genre.FUNK    -> GrooveType.LAID_BACK  // subtle pocket feel
        Genre.ROCK    -> GrooveType.STRAIGHT   // tight on the beat
        Genre.COUNTRY -> GrooveType.PUSHED     // train-beat forward drive
        Genre.DISCO   -> GrooveType.STRAIGHT   // four-on-the-floor is metronomically rigid // NEW made by Claude 05/08/2026
        Genre.SKA     -> GrooveType.STRAIGHT   // ska is mechanically precise // NEW made by Claude 05/08/2026
        Genre.REGGAE  -> GrooveType.LAID_BACK  // behind-the-beat spacious feel // NEW made by Claude 05/08/2026
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

            // ── BLUES ─────────────────────────────────────────────────────────────
// Feel: shuffle/swing triplet grid. tpb=3 (triplet subdivisions).
// Each beat = 3 slots: slot 0 = downbeat, slot 1 = skipped, slot 2 = upbeat "and"
// beat1=slot0, beat2=slot3, beat3=slot6, beat4=slot9
            Genre.BLUES -> {
                // Ride cymbal (closed HH): classic shuffle — downbeat + upbeat of each beat
                val ride = parsePattern(when (b) {
                    3    -> "<x>_x<x>_x<x>_x"
                    5    -> "<x>_x<x>_x<x>_x<x>_x<x>_x"
                    else -> "<x>_x<x>_x<x>_x<x>_x"
                })
                events.addAll(renderVoice(ride, startMs, durationMs, timeSignature, 9, 42, 60, 75, ticksPerBeat = 3))
                // Open hi-hat: triplet upbeat of each beat — the shuffle shimmer // NEW made by Claude 05/08
                val openHH = parsePattern(when (b) {
                    3    -> "__x__x__x"
                    5    -> "__x__x__x__x__x"
                    else -> "__x__x__x__x"
                })
                events.addAll(renderVoice(openHH, startMs, durationMs, timeSignature, 9, 46, 68, 68, noteLengthMs = 80, ticksPerBeat = 3))
                // Kick: beats 1 and 3
                val kick = parsePattern(when (b) {
                    3    -> "<x>________"
                    5    -> "<x>_____x_____x__"
                    else -> "<x>_____x_____"
                })
                events.addAll(renderVoice(kick, startMs, durationMs, timeSignature, 9, 36, 90, 100, noteLengthMs = 100, ticksPerBeat = 3))
                // Snare: beats 2 and 4
                val snare = parsePattern(when (b) {
                    3    -> "___x_____"
                    5    -> "___x_____x_____"
                    else -> "___x_____x__"
                })
                events.addAll(renderVoice(snare, startMs, durationMs, timeSignature, 9, 38, 90, 90, noteLengthMs = 100, ticksPerBeat = 3))
                // Ghost snare: soft hit on upbeat before beats 2 and 4 // NEW made by Claude 05/08
                val ghostSnare = parsePattern(when (b) {
                    3    -> "__x______"
                    5    -> "__x_____x______"
                    else -> "__x_____x___"
                })
                events.addAll(renderVoice(ghostSnare, startMs, durationMs, timeSignature, 9, 38, 32, 32, noteLengthMs = 50, ticksPerBeat = 3))
            }

            // ── JAZZ ─────────────────────────────────────────────────────────────
            // Feel: swing ride pattern, hihat pedal on 2&4. tpb=3 (triplet grid).
            Genre.JAZZ -> {
                // Ride: classic jazz swing — "ding-da-ding ... ding-da-ding"
                val ride = parsePattern(when (b) {
                    3    -> "<x>_x<x>_____"
                    5    -> "<x>_x<x>__<x>_x<x>__<x>__"
                    else -> "<x>_x<x>__<x>_x<x>__"
                })
                events.addAll(renderVoice(ride, startMs, durationMs, timeSignature, 9, 51, 55, 70, noteLengthMs = 100, ticksPerBeat = 3))
                // Hi-hat pedal: beats 2 and 4
                val hihat = parsePattern(when (b) {
                    3    -> "___x_____"
                    5    -> "___x_____x_____"
                    else -> "___x_____x__"
                })
                events.addAll(renderVoice(hihat, startMs, durationMs, timeSignature, 9, 44, 80, 80, noteLengthMs = 50, ticksPerBeat = 3))
                // Sparse kick: beat 1 only — jazz convention // NEW made by Claude 05/08
                val kick = parsePattern(when (b) {
                    3    -> "<x>________"
                    5    -> "<x>_____x_______"
                    else -> "<x>_________"
                })
                events.addAll(renderVoice(kick, startMs, durationMs, timeSignature, 9, 36, 65, 75, noteLengthMs = 100, ticksPerBeat = 3))
            }
            // ── DISCO ────────────────────────────────────────────────────────────
            // Feel: four-on-the-floor kick, dense 16th HH, open HH on & of each beat.
            // tpb=4 (sixteenth-note grid)
            // NEW made by Claude 05/08/2026
            Genre.DISCO -> {
                // Kick: every beat — the four-on-the-floor foundation
                val kick = parsePattern(when (b) {
                    3    -> "<x>___<x>___<x>___"              // 12 slots ✓
                    5    -> "<x>___<x>___<x>___<x>___<x>___"  // 20 slots ✓
                    else -> "<x>___<x>___<x>___<x>___"        // 16 slots ✓
                })
                events.addAll(renderVoice(kick, startMs, durationMs, timeSignature, 9, 36, 100, 105, noteLengthMs = 100, ticksPerBeat = 4))
                // Snare: beats 2 and 4
                val snare = parsePattern(when (b) {
                    3    -> "____<x>_______"                  // 12 slots ✓
                    5    -> "____<x>_______<x>_______"        // 20 slots ✓
                    else -> "____<x>_______<x>___"            // 16 slots ✓
                })
                events.addAll(renderVoice(snare, startMs, durationMs, timeSignature, 9, 38, 90, 90, noteLengthMs = 100, ticksPerBeat = 4))
                // Closed HH: every 16th note — the disco engine room
                val hhClosed = parsePattern(when (b) {
                    3    -> "xxxxxxxxxxxx"                    // 12 slots ✓
                    5    -> "xxxxxxxxxxxxxxxxxxxx"            // 20 slots ✓
                    else -> "xxxxxxxxxxxxxxxx"                // 16 slots ✓
                })
                events.addAll(renderVoice(hhClosed, startMs, durationMs, timeSignature, 9, 42, 55, 55, noteLengthMs = 40, ticksPerBeat = 4))
                // Open HH: & of each beat — the shimmer that defines disco
                val hhOpen = parsePattern(when (b) {
                    3    -> "__x___x___x_"                   // 12 slots ✓
                    5    -> "__x___x___x___x___x_"           // 20 slots ✓
                    else -> "__x___x___x___x_"               // 16 slots ✓
                })
                events.addAll(renderVoice(hhOpen, startMs, durationMs, timeSignature, 9, 46, 82, 82, noteLengthMs = 70, ticksPerBeat = 4))
            }
            // ── SKA ──────────────────────────────────────────────────────────────
            // Feel: rock kick/snare placement, but HH off-beats are LOUDER than downbeats.
            // tpb=4 (sixteenth-note grid)
            // NEW made by Claude 05/08/2026
            Genre.SKA -> {
                // Kick: beats 1 and 3 (same positions as rock)
                val kick = parsePattern(when (b) {
                    3    -> "<x>___________"                  // 12 slots ✓
                    5    -> "<x>_______x___________"          // 20 slots ✓
                    else -> "<x>_______x_______"              // 16 slots ✓
                })
                events.addAll(renderVoice(kick, startMs, durationMs, timeSignature, 9, 36, 95, 105, noteLengthMs = 100, ticksPerBeat = 4))
                // Snare: beats 2 and 4, slightly harder than rock
                val snare = parsePattern(when (b) {
                    3    -> "____<x>_______"                  // 12 slots ✓
                    5    -> "____<x>_______<x>_______"        // 20 slots ✓
                    else -> "____<x>_______<x>___"            // 16 slots ✓
                })
                events.addAll(renderVoice(snare, startMs, durationMs, timeSignature, 9, 38, 100, 100, noteLengthMs = 100, ticksPerBeat = 4))
                // Hi-hat: 8th notes, off-beats LOUDER than downbeats (inverted accent)
                // x=downbeat (normalVelocity=62), <x>=off-beat (accentVelocity=85)
                val hihat = parsePattern(when (b) {
                    3    -> "x_<x>_x_<x>_x_<x>_"             // 12 slots ✓
                    5    -> "x_<x>_x_<x>_x_<x>_x_<x>_x_<x>_" // 20 slots ✓
                    else -> "x_<x>_x_<x>_x_<x>_x_<x>_"       // 16 slots ✓
                })
                events.addAll(renderVoice(hihat, startMs, durationMs, timeSignature, 9, 42, 62, 85, noteLengthMs = 60, ticksPerBeat = 4))
            }
            // ── REGGAE ───────────────────────────────────────────────────────────
            // Feel: one-drop — kick+snare on beat 3 ONLY. Beat 1 kick is ABSENT.
            // Off-beat HH only. Maximum space. tpb=4 (sixteenth-note grid)
            // NEW made by Claude 05/08/2026
            Genre.REGGAE -> {
                // Kick: beat 3 ONLY — the silence on beat 1 is the whole point
                val kick = parsePattern(when (b) {
                    3    -> "________<x>___"                  // 12 slots ✓
                    5    -> "________<x>___________"          // 20 slots ✓
                    else -> "________<x>_______"              // 16 slots ✓
                })
                events.addAll(renderVoice(kick, startMs, durationMs, timeSignature, 9, 36, 88, 95, noteLengthMs = 100, ticksPerBeat = 4))
                // Snare: beat 3 (lands with kick — the one-drop hit)
                val snare = parsePattern(when (b) {
                    3    -> "________x___"                    // 12 slots ✓
                    5    -> "________x___________"            // 20 slots ✓
                    else -> "________x_______"                // 16 slots ✓
                })
                events.addAll(renderVoice(snare, startMs, durationMs, timeSignature, 9, 38, 90, 90, noteLengthMs = 100, ticksPerBeat = 4))
                // Ghost snare: soft hit on beat 4 (not applicable in 3/4)
                if (b != 3) {
                    val ghostSnare = parsePattern(when (b) {
                        5    -> "____________x_______"        // 20 slots ✓
                        else -> "____________x___"            // 16 slots ✓
                    })
                    events.addAll(renderVoice(ghostSnare, startMs, durationMs, timeSignature, 9, 38, 50, 50, noteLengthMs = 80, ticksPerBeat = 4))
                }
                // Hi-hat: off-beats ONLY — no downbeat hats (adds to the spacious feel)
                val hihat = parsePattern(when (b) {
                    3    -> "__x___x___x_"                    // 12 slots ✓
                    5    -> "__x___x___x___x___x_"            // 20 slots ✓
                    else -> "__x___x___x___x_"                // 16 slots ✓
                })
                events.addAll(renderVoice(hihat, startMs, durationMs, timeSignature, 9, 42, 70, 70, noteLengthMs = 60, ticksPerBeat = 4))
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
                // MODIFIED made by Claude 05/08/2026
                // Boogie bass: R-R-5th-6th-b7th-6th-5th-5th (John Lee Hooker driving pattern)
                // Replaces walking quarter-note bass which was too jazz-flavoured for blues
                val sixth    = findBassPitch((chord.rootPitchClass + 9) % 12)
                val bSeventh = findBassPitch((chord.rootPitchClass + 10) % 12)
                val pattern  = parsePattern("<x>" + "x".repeat(timeSignature.beatsPerBar * 2 - 1))
                val pitches  = listOf(root, root, fifth, sixth, bSeventh, sixth, fifth, fifth)
                events.addAll(renderPitchSequence(pattern, pitches, startMs, durationMs, timeSignature, 1, 75, 88, noteLengthMs = 200, ticksPerBeat = 2))
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
            Genre.ROCK -> {
                // NEW made by Claude 05/08/2026
                // Alternating root-fifth, one hit per beat — same shape as Country but heavier touch
                val pattern = parsePattern("<x>" + "x".repeat(timeSignature.beatsPerBar - 1))
                events.addAll(renderPitchSequence(pattern, listOf(root, fifth), startMs, durationMs, timeSignature, 1, 92, 100, noteLengthMs = 400, ticksPerBeat = 1))
            }
            Genre.DISCO -> {
                // NEW made by Claude 05/08/2026
                // Pumping octave bass: alternating root / root+octave on 8th grid
                val pitches = (0 until timeSignature.beatsPerBar).flatMap { listOf(root, root + 12) }
                val pattern = parsePattern("<x>x".repeat(timeSignature.beatsPerBar))
                events.addAll(renderPitchSequence(pattern, pitches, startMs, durationMs, timeSignature, 1, 75, 92, noteLengthMs = 180, ticksPerBeat = 2))
            }
            Genre.SKA -> {
                // NEW made by Claude 05/08/2026
                // Walking bass with octave jump: R-5th-R+oct-5th cycle on 8th grid
                val pattern = parsePattern("<x>x".repeat(timeSignature.beatsPerBar))
                events.addAll(renderPitchSequence(pattern, listOf(root, fifth, root + 12, fifth), startMs, durationMs, timeSignature, 1, 75, 87, noteLengthMs = 300, ticksPerBeat = 2))
            }
            Genre.REGGAE -> {
                // NEW made by Claude 05/08/2026
                // One-drop bass: root on beat 1, fifth on beat 3, maximum space
                val pattern = parsePattern(when (timeSignature.beatsPerBar) {
                    3    -> "<x>_x"   // 3 slots ✓
                    5    -> "<x>_x__" // 5 slots ✓
                    else -> "<x>_x_"  // 4 slots ✓
                })
                events.addAll(renderPitchSequence(pattern, listOf(root, fifth), startMs, durationMs, timeSignature, 1, 80, 87, noteLengthMs = 600, ticksPerBeat = 1))
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
    // ─── PIANO ───────────────────────────────────────────────────────────────
    // made by Claude 11/07: Piano generation — comping or arpeggio based on role

    private fun findPianoChordNotes(chord: ResolvedChord): List<Int> {
        var root = chord.rootPitchClass + 48 // Start at C3
        while (root < 48) root += 12
        while (root > 60) root -= 12   // Keep root in C3-C4

        return chord.quality.intervals.map { interval ->
            val note = root + interval
            if (note > 72) note - 12 else note  // Keep within C3-C5
        }.distinct().take(4)  // Max 4 notes — clean piano voicing
    }

    private fun generatePiano(
        startMs: Long,
        durationMs: Long,
        chord: ResolvedChord,
        genre: Genre,
        timeSignature: TimeSignature,
        role: InstrumentRole
    ): List<BackingTrackGenerator.MidiNoteEvent> {
        val events = mutableListOf<BackingTrackGenerator.MidiNoteEvent>()
        val chordNotes = findPianoChordNotes(chord)
        val b = timeSignature.beatsPerBar
        val beatMs = durationMs / b

        when (role) {
            InstrumentRole.STRUM_CHORD -> {
                // Genre-aware comping — hit times and velocity vary per genre
                val hits: List<Pair<Long, Int>> = when (genre) {
                    Genre.JAZZ -> // Sparse comp: beats 2 and 4
                        listOf(2, 4).filter { it <= b }
                            .map { beat -> Pair(startMs + (beat - 1) * beatMs, 62) }

                    Genre.BLUES -> // Every beat — boogie feel
                        (1..b).map { beat ->
                            Pair(startMs + (beat - 1) * beatMs, 70)
                        }

                    Genre.FUNK -> // Upbeat stabs — "and" of each beat
                        (1..b).map { beat ->
                            Pair(startMs + (beat - 1) * beatMs + beatMs / 2, 75)
                        }

                    Genre.COUNTRY -> // Beats 2 and 4 matching snare
                        listOf(2, 4).filter { it <= b }
                            .map { beat -> Pair(startMs + (beat - 1) * beatMs, 65) }

                    Genre.ROCK -> // Beats 1 and 3 — downbeat emphasis
                        listOf(1, 3).filter { it <= b }
                            .map { beat -> Pair(startMs + (beat - 1) * beatMs, 70) }
                    Genre.DISCO -> // Every beat — stabs lock with the four-on-the-floor kick // NEW made by Claude 05/08/2026
                        (1..b).map { beat ->
                            Pair(startMs + (beat - 1) * beatMs, 72)
                        }

                    Genre.SKA -> // Off-beat stabs — match the guitar skank // NEW made by Claude 05/08/2026
                        (1..b).map { beat ->
                            Pair(startMs + (beat - 1) * beatMs + beatMs / 2, 68)
                        }

                    Genre.REGGAE -> // Sparse off-beat chops — beats 2 and 4 only // NEW made by Claude 05/08/2026
                        listOf(2, 4).filter { it <= b }
                            .map { beat -> Pair(startMs + (beat - 1) * beatMs + beatMs / 2, 60) }
                }
                val noteDurationMs = (beatMs * 0.9f).toInt().coerceAtLeast(80)
                hits.forEach { (timeMs, velocity) ->
                    chordNotes.forEach { pitch ->
                        events.add(BackingTrackGenerator.MidiNoteEvent(
                            timeMs, 2, pitch, velocity, noteDurationMs
                        ))
                    }
                }
            }

            InstrumentRole.PICK_ARPEGGIO -> {
                // Ascending broken chord — one note per 8th note slot
                val intervalMs = beatMs / 2  // 8th note spacing
                chordNotes.forEachIndexed { i, pitch ->
                    val timeMs = startMs + (i * intervalMs)
                    if (timeMs < startMs + durationMs) {
                        events.add(BackingTrackGenerator.MidiNoteEvent(
                            timeMs, 2, pitch, 65, intervalMs.toInt()
                        ))
                    }
                }
            }

            else -> {} // OFF and HYBRID — no events
        }
        return events
    }

    // ─── STRINGS ─────────────────────────────────────────────────────────────
    // made by Claude 11/07: Strings — sustained pad underneath, root + fifth

    private fun findStringsPitch(pitchClass: Int): Int {
        var pitch = 48 + pitchClass  // Start at C3
        while (pitch < 48) pitch += 12
        while (pitch > 60) pitch -= 12
        return pitch
    }

    private fun generateStrings(
        startMs: Long,
        durationMs: Long,
        chord: ResolvedChord
    ): List<BackingTrackGenerator.MidiNoteEvent> {
        val delayMs = 8L  // Slight delayed attack — sits behind guitar
        val root  = findStringsPitch(chord.rootPitchClass)
        val fifth = findStringsPitch((chord.rootPitchClass + 7) % 12)
        return listOf(
            BackingTrackGenerator.MidiNoteEvent(
                startMs + delayMs, 3, root,  52, durationMs.toInt()
            ),
            BackingTrackGenerator.MidiNoteEvent(
                startMs + delayMs, 3, fifth, 48, durationMs.toInt()
            )
        )
    }
}