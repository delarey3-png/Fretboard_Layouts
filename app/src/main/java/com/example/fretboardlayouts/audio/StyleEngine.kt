package com.example.fretboardlayouts.audio
import com.example.fretboardlayouts.theory.ChordNoteBuilder
import com.example.fretboardlayouts.theory.ChordType
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
import com.example.fretboardlayouts.theory.humaniseVelocity   // made by Claude 11/07
import com.example.fretboardlayouts.theory.humaniseTiming     // made by Claude 11/07
import com.example.fretboardlayouts.theory.humaniseDuration   // made by Claude 11/07
import com.example.fretboardlayouts.theory.GrooveType         // made by Claude 11/07
import com.example.fretboardlayouts.theory.GuitarChordLibrary
import com.example.fretboardlayouts.theory.grooveOffsetMs     // made by Claude 11/07
import com.example.fretboardlayouts.theory.InstrumentRole     // made by Claude 11/07
import com.example.fretboardlayouts.theory.VoiceLeadingEngine

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
 *
 * CHANNEL MAP (keep in sync with INSTRUMENT_DEFS in JamLabActivity.kt):
 * ─────────────────────────────────────────────────────────────────
 *   Ch  0  Guitar      programs 24-31
 *   Ch  1  Bass        programs 32-39
 *   Ch  2  Piano       programs  0-7
 *   Ch  3  Organ       programs 16-23   (added 09/08/2026)
 *   Ch  4  Strings     programs 40-47   (shifted from 3 on 09/08/2026)
 *   Ch  5  Ensemble    programs 48-55   (added 09/08/2026)
 *   Ch  6  Brass       programs 56-63   (added 09/08/2026)
 *   Ch  7  Reed        programs 64-71   (added 09/08/2026)
 *   Ch  8  Pipe        programs 72-79   (added 09/08/2026)
 *   Ch  9  Drums       bank 128, fixed
 *   Ch 10  Synth       programs 80-95   (added 09/08/2026)
 *   Ch 11  Ethnic      programs 104-111 (added 09/08/2026)
 */
object StyleEngine {
    // MODIFIED made by Claude 09/08/2026
    // Extended for full GM group channel map. Channel 3 = Organ (was Strings).
    // Strings now on channel 4. Starting values — tune by ear per genre.
    private val channelVolumeScale = mapOf(
        0 to 0.78f,  // Guitar    — pulled back into the band mix
        1 to 0.98f,  // Bass      — slightly under guitar
        2 to 0.80f,  // Piano
        3 to 0.75f,  // Organ     — same level as old Strings (ch3 reassigned 09/08/2026)
        4 to 0.75f,  // Strings   — shifted from ch3
        5 to 0.70f,  // Ensemble  — pads sit under strings
        6 to 0.78f,  // Brass     — section needs presence
        7 to 0.75f,  // Reed      — solo instrument
        8 to 0.72f,  // Pipe      — light instrument
        9 to 1.00f,  // Drums
        10 to 0.65f,  // Synth     — pads sit under everything
        11 to 0.75f   // Ethnic
    )

    fun generateAccompaniment(
        timeline: JamTimeline,
        genre: Genre,
        guitarPreset: StrumPreset,
        pickingPreset: PickingPreset? = null,
        humanisationLevel: HumanisationLevel = HumanisationLevel.OFF,
        instrumentRoles: Map<String, InstrumentRole> = emptyMap(), // made by Claude 11/07
        voiceLeadingEnabled: Boolean = false  // NEW made by Claude 19/08/2026
    ): List<BackingTrackGenerator.MidiNoteEvent> {
        val allEvents = mutableListOf<BackingTrackGenerator.MidiNoteEvent>()
        val timeSignature = timeline.timeSignature
        // Voice leading state — previous voicing tracked across chord changes
        // NEW made by Claude 19/08/2026
        var prevGuitarVoicing: List<Int> = emptyList()
        var prevPianoVoicing: List<Int> = emptyList()
        timeline.events.forEachIndexed { barIndex, event ->
            val chord = event.chord
            val startMs = event.startMs
            val durationMs = event.durationMs
            // made by Claude 11/07: Role-aware generation — only active channels produce events
            val drumsRole = instrumentRoles["drums"] ?: InstrumentRole.STRUM_CHORD
            val bassRole = instrumentRoles["bass"] ?: InstrumentRole.STRUM_CHORD
            val guitarRole = instrumentRoles["guitar"] ?: InstrumentRole.STRUM_CHORD
            val pianoRole = instrumentRoles["piano"] ?: InstrumentRole.OFF
            val stringsRole = instrumentRoles["strings"] ?: InstrumentRole.OFF
            // NEW made by Claude 09/08/2026 — expanded instrument set
            val organRole = instrumentRoles["organ"] ?: InstrumentRole.OFF
            val ensembleRole = instrumentRoles["ensemble"] ?: InstrumentRole.OFF
            val brassRole = instrumentRoles["brass"] ?: InstrumentRole.OFF
            val reedRole = instrumentRoles["reed"] ?: InstrumentRole.OFF
            val pipeRole = instrumentRoles["pipe"] ?: InstrumentRole.OFF
            val synthRole = instrumentRoles["synth"] ?: InstrumentRole.OFF
            val ethnicRole = instrumentRoles["ethnic"] ?: InstrumentRole.OFF

            if (drumsRole != InstrumentRole.OFF)
                allEvents.addAll(generateDrums(startMs, durationMs, genre, timeSignature))
            if (bassRole != InstrumentRole.OFF)
                allEvents.addAll(generateBass(startMs, durationMs, chord, genre, timeSignature))
            if (guitarRole != InstrumentRole.OFF) {
                // MODIFIED made by Claude 19/08/2026 — voice leading computes voicing once,
                // shared by whichever guitar generator runs (strum or picking)
                // MODIFIED made by Claude 25/08/2026 — first chord seeded from library,
// not from VoiceLeadingEngine empty fallback. Ensures voice leading starts
// from a guitar-realistic spread voicing rather than a closed-position triad.
                val guitarVoicing: List<Int>? = if (voiceLeadingEnabled) {
                    if (prevGuitarVoicing.isEmpty()) {
                        findGuitarVoicing(chord).also { prevGuitarVoicing = it }
                    } else {
                        VoiceLeadingEngine.leadToGuitar(
                            prevGuitarVoicing, chord.rootPitchClass, chord.quality
                        ).also { prevGuitarVoicing = it }
                    }
                } else null
                if (pickingPreset != null && pickingPreset.layers.isNotEmpty()
                    && guitarRole == InstrumentRole.PICK_ARPEGGIO
                ) {
                    allEvents.addAll(
                        generateGuitarPicking(
                            startMs, durationMs, chord, pickingPreset, timeSignature,
                            precomputedVoicing = guitarVoicing
                        )
                    )
                } else {
                    allEvents.addAll(
                        generateGuitar(
                            startMs, durationMs, chord, guitarPreset, timeSignature,
                            precomputedVoicing = guitarVoicing
                        )
                    )
                }
            }
            if (pianoRole != InstrumentRole.OFF) {  // MODIFIED made by Claude 19/08/2026
                // MODIFIED made by Claude 25/08/2026 — first chord seeded from findPianoChordNotes
                val pianoVoicing: List<Int>? = if (voiceLeadingEnabled) {
                    if (prevPianoVoicing.isEmpty()) {
                        findPianoChordNotes(chord).also { prevPianoVoicing = it }
                    } else {
                        VoiceLeadingEngine.leadToPiano(
                            prevPianoVoicing, chord.rootPitchClass, chord.quality
                        ).also { prevPianoVoicing = it }
                    }
                } else null
                allEvents.addAll(
                    generatePiano(
                        startMs, durationMs, chord, genre, timeSignature, pianoRole,
                        precomputedVoicing = pianoVoicing
                    )
                )
            }
            if (stringsRole != InstrumentRole.OFF)  // made by Claude 11/07
                allEvents.addAll(generateStrings(startMs, durationMs, chord))
            // NEW made by Claude 09/08/2026 — new instrument generators
            if (organRole != InstrumentRole.OFF)
                allEvents.addAll(
                    generateOrgan(
                        startMs,
                        durationMs,
                        chord,
                        genre,
                        timeSignature,
                        organRole
                    )
                )
            if (ensembleRole != InstrumentRole.OFF)
                allEvents.addAll(generateEnsemble(startMs, durationMs, chord))
            if (brassRole != InstrumentRole.OFF)
                allEvents.addAll(
                    generateBrass(
                        startMs,
                        durationMs,
                        chord,
                        genre,
                        timeSignature,
                        brassRole
                    )
                )
            if (reedRole != InstrumentRole.OFF)
                allEvents.addAll(generateReed(startMs, durationMs, chord, timeSignature, reedRole))
            if (pipeRole != InstrumentRole.OFF)
                allEvents.addAll(generatePipe(startMs, durationMs, chord, timeSignature, pipeRole))
            if (synthRole != InstrumentRole.OFF)
                allEvents.addAll(generateSynth(startMs, durationMs, chord))
            if (ethnicRole != InstrumentRole.OFF)
                allEvents.addAll(
                    generateEthnic(
                        startMs,
                        durationMs,
                        chord,
                        timeSignature,
                        ethnicRole
                    )
                )
        }
        // Apply humanisation as post-processing step.
        // MODIFIED made by Claude 05/08/2026 — apply channel volume scaling even without humanisation
        if (humanisationLevel == HumanisationLevel.OFF) return allEvents.map { event ->
            event.copy(
                velocity = (event.velocity * (channelVolumeScale[event.channel] ?: 1.0f)).toInt()
                    .coerceIn(1, 127)
            )
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
                    (humaniseDuration(
                        event.durationMs,
                        event.channel,
                        humanProfile
                    ) * 1.15f).toInt()
                } else {
                    humaniseDuration(event.durationMs, event.channel, humanProfile)
                }
            )
        }
    }

    // made by Claude 11/07: Genre groove type mapping
    private fun genreGroove(genre: Genre): GrooveType = when (genre) {
        Genre.JAZZ -> GrooveType.LAID_BACK  // classic laid-back swing feel
        Genre.BLUES -> GrooveType.LAID_BACK  // laid-back blues feel
        Genre.FUNK -> GrooveType.LAID_BACK  // subtle pocket feel
        Genre.ROCK -> GrooveType.STRAIGHT   // tight on the beat
        Genre.COUNTRY -> GrooveType.PUSHED     // train-beat forward drive
        Genre.DISCO -> GrooveType.STRAIGHT   // four-on-the-floor is metronomically rigid
        Genre.SKA -> GrooveType.STRAIGHT   // ska is mechanically precise
        Genre.REGGAE -> GrooveType.LAID_BACK  // behind-the-beat spacious feel
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
            Genre.ROCK -> {
                val hihat = parsePattern(
                    when (b) {
                        3 -> "<x>_x_<x>_x_<x>_x_"
                        5 -> "<x>_x_<x>_x_<x>_x_<x>_x_<x>_x_"
                        else -> "<x>_x_<x>_x_<x>_x_<x>_x_"
                    }
                )
                events.addAll(
                    renderVoice(
                        hihat,
                        startMs,
                        durationMs,
                        timeSignature,
                        9,
                        42,
                        65,
                        80,
                        ticksPerBeat = 4
                    )
                )
                val kick = parsePattern(
                    when (b) {
                        3 -> "<x>___________"
                        5 -> "<x>_______x___________"
                        else -> "<x>_______x_______"
                    }
                )
                events.addAll(
                    renderVoice(
                        kick,
                        startMs,
                        durationMs,
                        timeSignature,
                        9,
                        36,
                        95,
                        105,
                        noteLengthMs = 100,
                        ticksPerBeat = 4
                    )
                )
                val snare = parsePattern(
                    when (b) {
                        3 -> "____<x>_______"
                        5 -> "____<x>___________<x>___"
                        else -> "____<x>_______<x>___"
                    }
                )
                events.addAll(
                    renderVoice(
                        snare,
                        startMs,
                        durationMs,
                        timeSignature,
                        9,
                        38,
                        95,
                        95,
                        noteLengthMs = 100,
                        ticksPerBeat = 4
                    )
                )
            }
            // ── COUNTRY ──────────────────────────────────────────────────────
            Genre.COUNTRY -> {
                val hihat = parsePattern(
                    when (b) {
                        3 -> "<x>_x_<x>_x_<x>_x_"
                        5 -> "<x>_x_<x>_x_<x>_x_<x>_x_<x>_x_"
                        else -> "<x>_x_<x>_x_<x>_x_<x>_x_"
                    }
                )
                events.addAll(
                    renderVoice(
                        hihat,
                        startMs,
                        durationMs,
                        timeSignature,
                        9,
                        42,
                        50,
                        70,
                        ticksPerBeat = 4
                    )
                )
                val kick = parsePattern(
                    when (b) {
                        3 -> "<x>___________"
                        5 -> "<x>_______x___________"
                        else -> "<x>_______x_______"
                    }
                )
                events.addAll(
                    renderVoice(
                        kick,
                        startMs,
                        durationMs,
                        timeSignature,
                        9,
                        36,
                        95,
                        100,
                        noteLengthMs = 100,
                        ticksPerBeat = 4
                    )
                )
                val snare = parsePattern(
                    when (b) {
                        3 -> "____<x>_______"
                        5 -> "____<x>___________<x>___"
                        else -> "____<x>_______<x>___"
                    }
                )
                events.addAll(
                    renderVoice(
                        snare,
                        startMs,
                        durationMs,
                        timeSignature,
                        9,
                        38,
                        70,
                        70,
                        noteLengthMs = 100,
                        ticksPerBeat = 4
                    )
                )
            }
            // ── FUNK ─────────────────────────────────────────────────────────
            Genre.FUNK -> {
                val hihat = parsePattern(
                    when (b) {
                        3 -> "<x>xxx<x>xxx<x>xxx"
                        5 -> "<x>xxx<x>xxx<x>xxx<x>xxx<x>xxx"
                        else -> "<x>xxx<x>xxx<x>xxx<x>xxx"
                    }
                )
                events.addAll(
                    renderVoice(
                        hihat,
                        startMs,
                        durationMs,
                        timeSignature,
                        9,
                        42,
                        50,
                        80,
                        noteLengthMs = 40,
                        ticksPerBeat = 4
                    )
                )
                val kick = parsePattern(
                    when (b) {
                        3 -> "<x>___________"
                        5 -> "<x>_______x___________"
                        else -> "<x>_______x_______"
                    }
                )
                events.addAll(
                    renderVoice(
                        kick,
                        startMs,
                        durationMs,
                        timeSignature,
                        9,
                        36,
                        85,
                        100,
                        noteLengthMs = 100,
                        ticksPerBeat = 4
                    )
                )
                val snare = parsePattern(
                    when (b) {
                        3 -> "____<x>_______"
                        5 -> "____<x>___________<x>___"
                        else -> "____<x>_______<x>___"
                    }
                )
                events.addAll(
                    renderVoice(
                        snare,
                        startMs,
                        durationMs,
                        timeSignature,
                        9,
                        38,
                        100,
                        100,
                        noteLengthMs = 100,
                        ticksPerBeat = 4
                    )
                )
            }
            // ── BLUES ─────────────────────────────────────────────────────────
            // Shuffle/swing triplet grid. tpb=3.
            Genre.BLUES -> {
                val ride = parsePattern(
                    when (b) {
                        3 -> "<x>_x<x>_x<x>_x"
                        5 -> "<x>_x<x>_x<x>_x<x>_x<x>_x"
                        else -> "<x>_x<x>_x<x>_x<x>_x"
                    }
                )
                events.addAll(
                    renderVoice(
                        ride,
                        startMs,
                        durationMs,
                        timeSignature,
                        9,
                        42,
                        60,
                        75,
                        ticksPerBeat = 3
                    )
                )
                // Open hi-hat on triplet upbeats — the shuffle shimmer
                val openHH = parsePattern(
                    when (b) {
                        3 -> "__x__x__x"
                        5 -> "__x__x__x__x__x"
                        else -> "__x__x__x__x"
                    }
                )
                events.addAll(
                    renderVoice(
                        openHH,
                        startMs,
                        durationMs,
                        timeSignature,
                        9,
                        46,
                        68,
                        68,
                        noteLengthMs = 80,
                        ticksPerBeat = 3
                    )
                )
                val kick = parsePattern(
                    when (b) {
                        3 -> "<x>________"
                        5 -> "<x>_____x_____x__"
                        else -> "<x>_____x_____"
                    }
                )
                events.addAll(
                    renderVoice(
                        kick,
                        startMs,
                        durationMs,
                        timeSignature,
                        9,
                        36,
                        90,
                        100,
                        noteLengthMs = 100,
                        ticksPerBeat = 3
                    )
                )
                val snare = parsePattern(
                    when (b) {
                        3 -> "___x_____"
                        5 -> "___x_____x_____"
                        else -> "___x_____x__"
                    }
                )
                events.addAll(
                    renderVoice(
                        snare,
                        startMs,
                        durationMs,
                        timeSignature,
                        9,
                        38,
                        90,
                        90,
                        noteLengthMs = 100,
                        ticksPerBeat = 3
                    )
                )
                // Ghost snare on upbeat before 2&4
                val ghostSnare = parsePattern(
                    when (b) {
                        3 -> "__x______"
                        5 -> "__x_____x______"
                        else -> "__x_____x___"
                    }
                )
                events.addAll(
                    renderVoice(
                        ghostSnare,
                        startMs,
                        durationMs,
                        timeSignature,
                        9,
                        38,
                        32,
                        32,
                        noteLengthMs = 50,
                        ticksPerBeat = 3
                    )
                )
            }
            // ── JAZZ ─────────────────────────────────────────────────────────
            Genre.JAZZ -> {
                val ride = parsePattern(
                    when (b) {
                        3 -> "<x>_x<x>_____"
                        5 -> "<x>_x<x>__<x>_x<x>__<x>__"
                        else -> "<x>_x<x>__<x>_x<x>__"
                    }
                )
                events.addAll(
                    renderVoice(
                        ride,
                        startMs,
                        durationMs,
                        timeSignature,
                        9,
                        51,
                        55,
                        70,
                        noteLengthMs = 100,
                        ticksPerBeat = 3
                    )
                )
                val hihat = parsePattern(
                    when (b) {
                        3 -> "___x_____"
                        5 -> "___x_____x_____"
                        else -> "___x_____x__"
                    }
                )
                events.addAll(
                    renderVoice(
                        hihat,
                        startMs,
                        durationMs,
                        timeSignature,
                        9,
                        44,
                        80,
                        80,
                        noteLengthMs = 50,
                        ticksPerBeat = 3
                    )
                )
                // Sparse kick on beat 1
                val kick = parsePattern(
                    when (b) {
                        3 -> "<x>________"
                        5 -> "<x>_____x_______"
                        else -> "<x>_________"
                    }
                )
                events.addAll(
                    renderVoice(
                        kick,
                        startMs,
                        durationMs,
                        timeSignature,
                        9,
                        36,
                        65,
                        75,
                        noteLengthMs = 100,
                        ticksPerBeat = 3
                    )
                )
            }
            // ── DISCO ────────────────────────────────────────────────────────
            // Four-on-the-floor kick, open HH on & of each beat.
            Genre.DISCO -> {
                val kick = parsePattern(
                    when (b) {
                        3 -> "<x>___<x>___<x>___"
                        5 -> "<x>___<x>___<x>___<x>___<x>___"
                        else -> "<x>___<x>___<x>___<x>___"
                    }
                )
                events.addAll(
                    renderVoice(
                        kick,
                        startMs,
                        durationMs,
                        timeSignature,
                        9,
                        36,
                        100,
                        105,
                        noteLengthMs = 100,
                        ticksPerBeat = 4
                    )
                )
                val snare = parsePattern(
                    when (b) {
                        3 -> "____<x>_______"
                        5 -> "____<x>_______<x>_______"
                        else -> "____<x>_______<x>___"
                    }
                )
                events.addAll(
                    renderVoice(
                        snare,
                        startMs,
                        durationMs,
                        timeSignature,
                        9,
                        38,
                        90,
                        90,
                        noteLengthMs = 100,
                        ticksPerBeat = 4
                    )
                )
                val hhClosed = parsePattern(
                    when (b) {
                        3 -> "xxxxxxxxxxxx"
                        5 -> "xxxxxxxxxxxxxxxxxxxx"
                        else -> "xxxxxxxxxxxxxxxx"
                    }
                )
                events.addAll(
                    renderVoice(
                        hhClosed,
                        startMs,
                        durationMs,
                        timeSignature,
                        9,
                        42,
                        55,
                        55,
                        noteLengthMs = 40,
                        ticksPerBeat = 4
                    )
                )
                // Open HH on & of each beat — the shimmer that defines disco
                val hhOpen = parsePattern(
                    when (b) {
                        3 -> "__x___x___x_"
                        5 -> "__x___x___x___x___x_"
                        else -> "__x___x___x___x_"
                    }
                )
                events.addAll(
                    renderVoice(
                        hhOpen,
                        startMs,
                        durationMs,
                        timeSignature,
                        9,
                        46,
                        82,
                        82,
                        noteLengthMs = 70,
                        ticksPerBeat = 4
                    )
                )
            }
            // ── SKA ──────────────────────────────────────────────────────────
            // Rock placement but HH off-beats louder than downbeats.
            Genre.SKA -> {
                val kick = parsePattern(
                    when (b) {
                        3 -> "<x>___________"
                        5 -> "<x>_______x___________"
                        else -> "<x>_______x_______"
                    }
                )
                events.addAll(
                    renderVoice(
                        kick,
                        startMs,
                        durationMs,
                        timeSignature,
                        9,
                        36,
                        95,
                        105,
                        noteLengthMs = 100,
                        ticksPerBeat = 4
                    )
                )
                val snare = parsePattern(
                    when (b) {
                        3 -> "____<x>_______"
                        5 -> "____<x>_______<x>_______"
                        else -> "____<x>_______<x>___"
                    }
                )
                events.addAll(
                    renderVoice(
                        snare,
                        startMs,
                        durationMs,
                        timeSignature,
                        9,
                        38,
                        100,
                        100,
                        noteLengthMs = 100,
                        ticksPerBeat = 4
                    )
                )
                // x=downbeat (normal=62), <x>=off-beat (accent=85) — inverted
                val hihat = parsePattern(
                    when (b) {
                        3 -> "x_<x>_x_<x>_x_<x>_"
                        5 -> "x_<x>_x_<x>_x_<x>_x_<x>_x_<x>_"
                        else -> "x_<x>_x_<x>_x_<x>_x_<x>_"
                    }
                )
                events.addAll(
                    renderVoice(
                        hihat,
                        startMs,
                        durationMs,
                        timeSignature,
                        9,
                        42,
                        62,
                        85,
                        noteLengthMs = 60,
                        ticksPerBeat = 4
                    )
                )
            }
            // ── REGGAE ───────────────────────────────────────────────────────
            // One-drop: kick+snare on beat 3 ONLY. Beat 1 is silent.
            Genre.REGGAE -> {
                val kick = parsePattern(
                    when (b) {
                        3 -> "________<x>___"
                        5 -> "________<x>___________"
                        else -> "________<x>_______"
                    }
                )
                events.addAll(
                    renderVoice(
                        kick,
                        startMs,
                        durationMs,
                        timeSignature,
                        9,
                        36,
                        88,
                        95,
                        noteLengthMs = 100,
                        ticksPerBeat = 4
                    )
                )
                val snare = parsePattern(
                    when (b) {
                        3 -> "________x___"
                        5 -> "________x___________"
                        else -> "________x_______"
                    }
                )
                events.addAll(
                    renderVoice(
                        snare,
                        startMs,
                        durationMs,
                        timeSignature,
                        9,
                        38,
                        90,
                        90,
                        noteLengthMs = 100,
                        ticksPerBeat = 4
                    )
                )
                if (b != 3) {
                    val ghostSnare = parsePattern(
                        when (b) {
                            5 -> "____________x_______"
                            else -> "____________x___"
                        }
                    )
                    events.addAll(
                        renderVoice(
                            ghostSnare,
                            startMs,
                            durationMs,
                            timeSignature,
                            9,
                            38,
                            50,
                            50,
                            noteLengthMs = 80,
                            ticksPerBeat = 4
                        )
                    )
                }
                // Off-beats only — no downbeat hats
                val hihat = parsePattern(
                    when (b) {
                        3 -> "__x___x___x_"
                        5 -> "__x___x___x___x___x_"
                        else -> "__x___x___x___x_"
                    }
                )
                events.addAll(
                    renderVoice(
                        hihat,
                        startMs,
                        durationMs,
                        timeSignature,
                        9,
                        42,
                        70,
                        70,
                        noteLengthMs = 60,
                        ticksPerBeat = 4
                    )
                )
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
        val root = findBassPitch(chord.rootPitchClass)
        val fifth = findBassPitch((chord.rootPitchClass + 7) % 12)
        when (genre) {
            Genre.COUNTRY -> {
                val pattern = parsePattern("<x>" + "x".repeat(timeSignature.beatsPerBar - 1))
                events.addAll(
                    renderPitchSequence(
                        pattern,
                        listOf(root, fifth),
                        startMs,
                        durationMs,
                        timeSignature,
                        1,
                        87,
                        95,
                        noteLengthMs = 400,
                        ticksPerBeat = 1
                    )
                )
            }

            Genre.BLUES -> {
                // Boogie bass: R-R-5th-6th-b7th-6th-5th-5th (John Lee Hooker driving pattern)
                val sixth = findBassPitch((chord.rootPitchClass + 9) % 12)
                val bSeventh = findBassPitch((chord.rootPitchClass + 10) % 12)
                val pattern = parsePattern("<x>" + "x".repeat(timeSignature.beatsPerBar * 2 - 1))
                val pitches = listOf(root, root, fifth, sixth, bSeventh, sixth, fifth, fifth)
                events.addAll(
                    renderPitchSequence(
                        pattern,
                        pitches,
                        startMs,
                        durationMs,
                        timeSignature,
                        1,
                        75,
                        88,
                        noteLengthMs = 200,
                        ticksPerBeat = 2
                    )
                )
            }

            Genre.FUNK -> {
                val pattern =
                    parsePattern("x__x" + "<x>__x" + "____".repeat(timeSignature.beatsPerBar - 2))
                events.addAll(
                    renderPitchSequence(
                        pattern,
                        listOf(root, root, root + 12, fifth),
                        startMs,
                        durationMs,
                        timeSignature,
                        1,
                        90,
                        110,
                        noteLengthMs = 150,
                        ticksPerBeat = 4
                    )
                )
            }

            Genre.JAZZ -> {
                val third = findBassPitch((chord.rootPitchClass + chord.quality.intervals[1]) % 12)
                val sixth = findBassPitch((chord.rootPitchClass + 9) % 12)
                val pattern = parsePattern("<x>" + "x".repeat(timeSignature.beatsPerBar - 1))
                events.addAll(
                    renderPitchSequence(
                        pattern,
                        listOf(root, third, fifth, sixth),
                        startMs,
                        durationMs,
                        timeSignature,
                        1,
                        85,
                        90,
                        noteLengthMs = 400,
                        ticksPerBeat = 1
                    )
                )
            }

            Genre.ROCK -> {
                // Alternating root-fifth, heavier touch than Country
                val pattern = parsePattern("<x>" + "x".repeat(timeSignature.beatsPerBar - 1))
                events.addAll(
                    renderPitchSequence(
                        pattern,
                        listOf(root, fifth),
                        startMs,
                        durationMs,
                        timeSignature,
                        1,
                        92,
                        100,
                        noteLengthMs = 400,
                        ticksPerBeat = 1
                    )
                )
            }

            Genre.DISCO -> {
                // Pumping octave bass: alternating root / root+octave on 8th grid
                val pitches =
                    (0 until timeSignature.beatsPerBar).flatMap { listOf(root, root + 12) }
                val pattern = parsePattern("<x>x".repeat(timeSignature.beatsPerBar))
                events.addAll(
                    renderPitchSequence(
                        pattern,
                        pitches,
                        startMs,
                        durationMs,
                        timeSignature,
                        1,
                        75,
                        92,
                        noteLengthMs = 180,
                        ticksPerBeat = 2
                    )
                )
            }

            Genre.SKA -> {
                // Walking bass with octave jump: R-5th-R+oct-5th cycle
                val pattern = parsePattern("<x>x".repeat(timeSignature.beatsPerBar))
                events.addAll(
                    renderPitchSequence(
                        pattern,
                        listOf(root, fifth, root + 12, fifth),
                        startMs,
                        durationMs,
                        timeSignature,
                        1,
                        75,
                        87,
                        noteLengthMs = 300,
                        ticksPerBeat = 2
                    )
                )
            }

            Genre.REGGAE -> {
                // One-drop bass: root beat 1, fifth beat 3, maximum space
                val pattern = parsePattern(
                    when (timeSignature.beatsPerBar) {
                        3 -> "<x>_x"
                        5 -> "<x>_x__"
                        else -> "<x>_x_"
                    }
                )
                events.addAll(
                    renderPitchSequence(
                        pattern,
                        listOf(root, fifth),
                        startMs,
                        durationMs,
                        timeSignature,
                        1,
                        80,
                        87,
                        noteLengthMs = 600,
                        ticksPerBeat = 1
                    )
                )
            }
        }
        return events
    }

    // ─── GUITAR ──────────────────────────────────────────────────────────────
    // MODIFIED made by Claude 19/08/2026 — chordType threaded through to findGuitarVoicing
    // MODIFIED made by Claude 19/08/2026 — precomputedVoicing passed in when voice leading is on
    private fun generateGuitar(
        startMs: Long,
        durationMs: Long,
        chord: ResolvedChord,
        preset: StrumPreset,
        timeSignature: TimeSignature,
        chordType: ChordType = ChordType.FULL,
        precomputedVoicing: List<Int>? = null
    ): List<BackingTrackGenerator.MidiNoteEvent> {
        val voicing = precomputedVoicing ?: findGuitarVoicing(chord, chordType)
        return renderPreset(preset, voicing, startMs, durationMs, timeSignature, channel = 0)
    }

    // made by Gemini 27/06
    // MODIFIED made by Claude 19/08/2026 — chordType + precomputedVoicing threaded through
    private fun generateGuitarPicking(
        startMs: Long,
        durationMs: Long,
        chord: ResolvedChord,
        preset: PickingPreset,
        timeSignature: TimeSignature,
        chordType: ChordType = ChordType.FULL,
        precomputedVoicing: List<Int>? = null
    ): List<BackingTrackGenerator.MidiNoteEvent> {
        val voicing = precomputedVoicing ?: findGuitarVoicing(chord, chordType)
        val shape = timeSignature.shape()
        val events = mutableListOf<BackingTrackGenerator.MidiNoteEvent>()
        preset.layers.forEach { layer ->
            val patternStr = layer.patternByShape[shape] ?: return@forEach
            val stringsStr = layer.stringsByShape[shape] ?: return@forEach
            val pattern = parsePattern(patternStr)
            val strings = com.example.fretboardlayouts.theory.parseStrings(stringsStr)
            pattern.forEachIndexed { tick, state ->
                if (state == com.example.fretboardlayouts.theory.SlotState.REST) return@forEachIndexed
                val velocity =
                    if (state == com.example.fretboardlayouts.theory.SlotState.ACCENT) layer.accentVelocity else layer.normalVelocity
                val stringIdx = strings.getOrNull(tick) ?: return@forEachIndexed
                if (stringIdx < 0 || stringIdx >= voicing.size) return@forEachIndexed
                val pitch = voicing[stringIdx]
                val beat = tick / layer.ticksPerBeat
                val tickInBeat = tick % layer.ticksPerBeat
                events.add(
                    BackingTrackGenerator.MidiNoteEvent(
                        startMs + com.example.fretboardlayouts.theory.beatTickToMs(
                            beat,
                            tickInBeat,
                            layer.ticksPerBeat,
                            timeSignature,
                            durationMs
                        ),
                        0, pitch, velocity, 200
                    )
                )
            }
        }
        return events
    }

    // ─── PIANO ───────────────────────────────────────────────────────────────
    // made by Claude 11/07: Piano generation — comping or arpeggio based on role
    // MODIFIED made by Claude 19/08/2026 — precomputedVoicing passed in when voice leading is on
    private fun generatePiano(
        startMs: Long,
        durationMs: Long,
        chord: ResolvedChord,
        genre: Genre,
        timeSignature: TimeSignature,
        role: InstrumentRole,
        chordType: ChordType = ChordType.FULL,
        precomputedVoicing: List<Int>? = null
    ): List<BackingTrackGenerator.MidiNoteEvent> {
        val events     = mutableListOf<BackingTrackGenerator.MidiNoteEvent>()
        val chordNotes = precomputedVoicing ?: findPianoChordNotes(chord, chordType)
        val b = timeSignature.beatsPerBar
        val beatMs = durationMs / b
        when (role) {
            InstrumentRole.STRUM_CHORD -> {
                val hits: List<Pair<Long, Int>> = when (genre) {
                    Genre.JAZZ ->
                        listOf(2, 4).filter { it <= b }
                            .map { Pair(startMs + (it - 1) * beatMs, 62) }

                    Genre.BLUES ->
                        (1..b).map { Pair(startMs + (it - 1) * beatMs, 70) }

                    Genre.FUNK ->
                        (1..b).map { Pair(startMs + (it - 1) * beatMs + beatMs / 2, 75) }

                    Genre.COUNTRY ->
                        listOf(2, 4).filter { it <= b }
                            .map { Pair(startMs + (it - 1) * beatMs, 65) }

                    Genre.ROCK ->
                        listOf(1, 3).filter { it <= b }
                            .map { Pair(startMs + (it - 1) * beatMs, 70) }

                    Genre.DISCO ->
                        (1..b).map { Pair(startMs + (it - 1) * beatMs, 72) }

                    Genre.SKA ->
                        (1..b).map { Pair(startMs + (it - 1) * beatMs + beatMs / 2, 68) }

                    Genre.REGGAE ->
                        listOf(2, 4).filter { it <= b }
                            .map { Pair(startMs + (it - 1) * beatMs + beatMs / 2, 60) }
                }
                val noteDurationMs = (beatMs * 0.9f).toInt().coerceAtLeast(80)
                hits.forEach { (timeMs, velocity) ->
                    chordNotes.forEach { pitch ->
                        events.add(
                            BackingTrackGenerator.MidiNoteEvent(
                                timeMs,
                                2,
                                pitch,
                                velocity,
                                noteDurationMs
                            )
                        )
                    }
                }
            }

            InstrumentRole.PICK_ARPEGGIO -> {
                val intervalMs = beatMs / 2
                chordNotes.forEachIndexed { i, pitch ->
                    val timeMs = startMs + (i * intervalMs)
                    if (timeMs < startMs + durationMs) {
                        events.add(
                            BackingTrackGenerator.MidiNoteEvent(
                                timeMs,
                                2,
                                pitch,
                                65,
                                intervalMs.toInt()
                            )
                        )
                    }
                }
            }

            else -> {}
        }
        return events
    }

    // ─── ORGAN ───────────────────────────────────────────────────────────────
    // NEW made by Claude 09/08/2026
    // Channel 3. Genre-aware comping with organ character (sustained, full chord).
    // Visible for: Jazz, Blues, Funk, Reggae (per genreInstrumentVisibility).
    private fun generateOrgan(
        startMs: Long,
        durationMs: Long,
        chord: ResolvedChord,
        genre: Genre,
        timeSignature: TimeSignature,
        role: InstrumentRole
    ): List<BackingTrackGenerator.MidiNoteEvent> {
        val events = mutableListOf<BackingTrackGenerator.MidiNoteEvent>()
        val chordNotes = findPianoChordNotes(chord)  // same C3-C4 range as piano
        val b = timeSignature.beatsPerBar
        val beatMs = durationMs / b
        when (role) {
            InstrumentRole.STRUM_CHORD -> {
                val hits: List<Pair<Long, Int>> = when (genre) {
                    Genre.JAZZ -> listOf(2, 4).filter { it <= b }
                        .map { Pair(startMs + (it - 1) * beatMs, 60) }

                    Genre.BLUES -> (1..b).map { Pair(startMs + (it - 1) * beatMs, 65) }
                    Genre.FUNK -> (1..b).map { Pair(startMs + (it - 1) * beatMs + beatMs / 2, 68) }
                    Genre.REGGAE -> listOf(2, 4).filter { it <= b }
                        .map { Pair(startMs + (it - 1) * beatMs + beatMs / 2, 58) }

                    else -> listOf(2, 4).filter { it <= b }
                        .map { Pair(startMs + (it - 1) * beatMs, 62) }
                }
                // Organ sustains longer — 95% of beat vs piano's 90%
                val noteDurationMs = (beatMs * 0.95f).toInt().coerceAtLeast(100)
                hits.forEach { (timeMs, velocity) ->
                    chordNotes.forEach { pitch ->
                        events.add(
                            BackingTrackGenerator.MidiNoteEvent(
                                timeMs,
                                3,
                                pitch,
                                velocity,
                                noteDurationMs
                            )
                        )
                    }
                }
            }

            InstrumentRole.PICK_ARPEGGIO -> {
                val intervalMs = beatMs / 2
                chordNotes.forEachIndexed { i, pitch ->
                    val timeMs = startMs + (i * intervalMs)
                    if (timeMs < startMs + durationMs) {
                        events.add(
                            BackingTrackGenerator.MidiNoteEvent(
                                timeMs,
                                3,
                                pitch,
                                62,
                                intervalMs.toInt()
                            )
                        )
                    }
                }
            }

            else -> {}
        }
        return events
    }

    // ─── STRINGS ─────────────────────────────────────────────────────────────
    // made by Claude 11/07: Strings — sustained pad underneath, root + fifth
    // MODIFIED made by Claude 09/08/2026: channel 3 → 4 (Organ now occupies ch3)
    private fun generateStrings(
        startMs: Long,
        durationMs: Long,
        chord: ResolvedChord
    ): List<BackingTrackGenerator.MidiNoteEvent> {
        val delayMs = 8L  // Slight delayed attack — sits behind guitar
        val root = findStringsPitch(chord.rootPitchClass)
        val fifth = findStringsPitch((chord.rootPitchClass + 7) % 12)
        return listOf(
            BackingTrackGenerator.MidiNoteEvent(
                startMs + delayMs,
                4,
                root,
                52,
                durationMs.toInt()
            ), // MODIFIED ch3→4
            BackingTrackGenerator.MidiNoteEvent(
                startMs + delayMs,
                4,
                fifth,
                48,
                durationMs.toInt()
            )  // MODIFIED ch3→4
        )
    }

    // ─── ENSEMBLE ────────────────────────────────────────────────────────────
    // NEW made by Claude 09/08/2026
    // Channel 5. Sustained string ensemble pad — fuller than strings (root+third+fifth).
    // Not genre-aware — pads are universal. Slightly slower attack than strings.
    private fun generateEnsemble(
        startMs: Long,
        durationMs: Long,
        chord: ResolvedChord
    ): List<BackingTrackGenerator.MidiNoteEvent> {
        val delayMs = 15L  // Slower attack than strings — ensemble blends in gently
        val chordNotes = findPianoChordNotes(chord).take(3)  // root + third + fifth only
        return chordNotes.map { pitch ->
            BackingTrackGenerator.MidiNoteEvent(startMs + delayMs, 5, pitch, 48, durationMs.toInt())
        }
    }

    // ─── BRASS ───────────────────────────────────────────────────────────────
    // NEW made by Claude 09/08/2026
    // Channel 6. Genre-aware chord stabs — short attack, punchy.
    // Visible for: Blues, Jazz, Funk, Disco, Ska, Reggae.
    private fun generateBrass(
        startMs: Long,
        durationMs: Long,
        chord: ResolvedChord,
        genre: Genre,
        timeSignature: TimeSignature,
        role: InstrumentRole
    ): List<BackingTrackGenerator.MidiNoteEvent> {
        val events = mutableListOf<BackingTrackGenerator.MidiNoteEvent>()
        val chordNotes = findBrassChordNotes(chord)
        val b = timeSignature.beatsPerBar
        val beatMs = durationMs / b
        when (role) {
            InstrumentRole.STRUM_CHORD -> {
                val hits: List<Pair<Long, Int>> = when (genre) {
                    Genre.JAZZ, Genre.BLUES ->
                        // Off-beat punches on 2&4 — classic horn section placement
                        listOf(2, 4).filter { it <= b }
                            .map { Pair(startMs + (it - 1) * beatMs + beatMs / 2, 82) }

                    Genre.FUNK ->
                        // All off-beats — tight funk horn section
                        (1..b).map { Pair(startMs + (it - 1) * beatMs + beatMs / 2, 85) }

                    Genre.DISCO ->
                        // On-beat stabs — lock with kick
                        (1..b).map { Pair(startMs + (it - 1) * beatMs, 78) }

                    Genre.SKA ->
                        // Beats 1 and 3 — ska horn stab placement
                        listOf(1, 3).filter { it <= b }
                            .map { Pair(startMs + (it - 1) * beatMs, 80) }

                    Genre.REGGAE ->
                        // Off-beat on 2&4, sparse — reggae horns stay out of the way
                        listOf(2, 4).filter { it <= b }
                            .map { Pair(startMs + (it - 1) * beatMs + beatMs / 2, 72) }

                    else ->
                        listOf(1, 3).filter { it <= b }
                            .map { Pair(startMs + (it - 1) * beatMs, 78) }
                }
                val noteDurationMs = (beatMs * 0.4f).toInt().coerceAtLeast(60)  // staccato stabs
                hits.forEach { (timeMs, velocity) ->
                    chordNotes.forEach { pitch ->
                        events.add(
                            BackingTrackGenerator.MidiNoteEvent(
                                timeMs,
                                6,
                                pitch,
                                velocity,
                                noteDurationMs
                            )
                        )
                    }
                }
            }

            InstrumentRole.PICK_ARPEGGIO -> {
                // Single-note melody on root — trumpet/trombone lead line
                val root = findMidRangePitch(chord.rootPitchClass)
                (0 until b).forEach { beat ->
                    events.add(
                        BackingTrackGenerator.MidiNoteEvent(
                            startMs + beat * beatMs, 6, root, 75, (beatMs * 0.8f).toInt()
                        )
                    )
                }
            }

            else -> {}
        }
        return events
    }

    // ─── REED ────────────────────────────────────────────────────────────────
    // NEW made by Claude 09/08/2026
    // Channel 7. Single-note melodic instrument — sax, oboe, clarinet.
    // STRUM_CHORD: root held for the bar. PICK_ARPEGGIO: root-fifth per beat.
    private fun generateReed(
        startMs: Long,
        durationMs: Long,
        chord: ResolvedChord,
        timeSignature: TimeSignature,
        role: InstrumentRole
    ): List<BackingTrackGenerator.MidiNoteEvent> {
        val events = mutableListOf<BackingTrackGenerator.MidiNoteEvent>()
        val root = findMidRangePitch(chord.rootPitchClass)
        val fifth = findMidRangePitch((chord.rootPitchClass + 7) % 12)
        val b = timeSignature.beatsPerBar
        val beatMs = durationMs / b
        when (role) {
            InstrumentRole.STRUM_CHORD -> {
                // Root held for the bar — saxophone whole note
                events.add(
                    BackingTrackGenerator.MidiNoteEvent(
                        startMs, 7, root, 70, (durationMs * 0.9f).toInt()
                    )
                )
            }

            InstrumentRole.PICK_ARPEGGIO -> {
                // Root-fifth alternating per beat
                (0 until b).forEach { beat ->
                    val pitch = if (beat % 2 == 0) root else fifth
                    events.add(
                        BackingTrackGenerator.MidiNoteEvent(
                            startMs + beat * beatMs, 7, pitch, 68, (beatMs * 0.85f).toInt()
                        )
                    )
                }
            }

            else -> {}
        }
        return events
    }

    // ─── PIPE ────────────────────────────────────────────────────────────────
    // NEW made by Claude 09/08/2026
    // Channel 8. Light single-note instrument — flute, recorder, pan flute.
    // Plays an octave higher than Reed for air and lightness.
    private fun generatePipe(
        startMs: Long,
        durationMs: Long,
        chord: ResolvedChord,
        timeSignature: TimeSignature,
        role: InstrumentRole
    ): List<BackingTrackGenerator.MidiNoteEvent> {
        val events = mutableListOf<BackingTrackGenerator.MidiNoteEvent>()
        // +12 from mid range — pipes sound best up an octave (C5 range)
        val root = findMidRangePitch(chord.rootPitchClass) + 12
        val b = timeSignature.beatsPerBar
        val beatMs = durationMs / b
        when (role) {
            InstrumentRole.STRUM_CHORD -> {
                // Root held for the bar — gentle sustained flute note
                events.add(
                    BackingTrackGenerator.MidiNoteEvent(
                        startMs, 8, root, 60, (durationMs * 0.9f).toInt()
                    )
                )
            }

            InstrumentRole.PICK_ARPEGGIO -> {
                // Root per beat — simple melodic line
                (0 until b).forEach { beat ->
                    events.add(
                        BackingTrackGenerator.MidiNoteEvent(
                            startMs + beat * beatMs, 8, root, 58, (beatMs * 0.85f).toInt()
                        )
                    )
                }
            }

            else -> {}
        }
        return events
    }

    // ─── SYNTH ───────────────────────────────────────────────────────────────
    // NEW made by Claude 09/08/2026
    // Channel 10. Sustained pad — chord tones for full duration.
    // Not genre-aware. SF2-aware only — only visible if font has synth patches.
    // Sits very low in the mix (channelVolumeScale = 0.65).
    private fun generateSynth(
        startMs: Long,
        durationMs: Long,
        chord: ResolvedChord
    ): List<BackingTrackGenerator.MidiNoteEvent> {
        val chordNotes = findPianoChordNotes(chord)
        return chordNotes.map { pitch ->
            BackingTrackGenerator.MidiNoteEvent(startMs, 10, pitch, 45, durationMs.toInt())
        }
    }

    // ─── ETHNIC ──────────────────────────────────────────────────────────────
    // NEW made by Claude 09/08/2026
    // Channel 11. Root + fifth alternating — works for sitar, banjo, koto.
    // STRUM_CHORD: root+fifth drone. PICK_ARPEGGIO: alternating per beat.
    private fun generateEthnic(
        startMs: Long,
        durationMs: Long,
        chord: ResolvedChord,
        timeSignature: TimeSignature,
        role: InstrumentRole
    ): List<BackingTrackGenerator.MidiNoteEvent> {
        val events = mutableListOf<BackingTrackGenerator.MidiNoteEvent>()
        val root = findMidRangePitch(chord.rootPitchClass)
        val fifth = findMidRangePitch((chord.rootPitchClass + 7) % 12)
        val b = timeSignature.beatsPerBar
        val beatMs = durationMs / b
        when (role) {
            InstrumentRole.STRUM_CHORD -> {
                // Root + fifth held together — open string drone
                events.add(
                    BackingTrackGenerator.MidiNoteEvent(
                        startMs,
                        11,
                        root,
                        70,
                        (durationMs * 0.9f).toInt()
                    )
                )
                events.add(
                    BackingTrackGenerator.MidiNoteEvent(
                        startMs,
                        11,
                        fifth,
                        62,
                        (durationMs * 0.9f).toInt()
                    )
                )
            }

            InstrumentRole.PICK_ARPEGGIO -> {
                // Alternating root-fifth per beat — sitar-style plucking
                (0 until b).forEach { beat ->
                    val pitch = if (beat % 2 == 0) root else fifth
                    events.add(
                        BackingTrackGenerator.MidiNoteEvent(
                            startMs + beat * beatMs, 11, pitch, 72, (beatMs * 0.8f).toInt()
                        )
                    )
                }
            }

            else -> {}
        }
        return events
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────
    // MODIFIED made by Claude 19/08/2026 — replaced fret-search with ChordNoteBuilder.
// Old approach searched frets 0-5 using chordTonePitchClasses (triad only).
// New approach uses verified interval map including 7th/9th/extended tones.
// Root placed in lower guitar register (MIDI 40-55 = low E to G string open).
// List is ascending pitch, indexed 0..n — picking generator reads by index,
// strum generator reads in order. Both work correctly with a variable-length list.
    // MODIFIED made by Claude 25/08/2026 — uses GuitarChordLibrary as primary source.
// Real guitar grips replace closed-position interval stacking.
// Fallback spread algorithm retained for TRIAD/POWER and any chord not in library.
    private fun findGuitarVoicing(
        chord: ResolvedChord,
        chordType: ChordType = ChordType.FULL
    ): List<Int> {
        if (chordType == ChordType.POWER) return ChordNoteBuilder.buildPowerChord(
            ChordNoteBuilder.nearestMidi(chord.rootPitchClass, 40)
        )

        // FULL / EXTENDED: try the real voicing library first
        if (chordType == ChordType.FULL || chordType == ChordType.EXTENDED) {
            val libraryVoicing = GuitarChordLibrary.bestVoicing(chord.rootPitchClass, chord.quality)
            if (libraryVoicing != null) return libraryVoicing
        }

        // TRIAD or library miss: algorithmic spread voicing
        val rootMidi = ChordNoteBuilder.nearestMidi(chord.rootPitchClass, 40)
            .let { if (it > 55) it - 12 else it }
        val pitchClasses = ChordNoteBuilder.buildNotes(0, chord.quality, chordType)
            .map { it % 12 }
        val notes = mutableListOf(rootMidi)
        var cursor = maxOf(rootMidi + 7, 55)
        for (pc in pitchClasses.drop(1)) {
            var candidate = ChordNoteBuilder.nearestMidi(pc, cursor)
            if (candidate < cursor) candidate += 12
            while (candidate > cursor + 14) candidate -= 12
            if (candidate in 40..76) {
                notes.add(candidate)
                cursor = candidate + 1
            }
        }
        return notes.distinct().sorted().filter { it in 40..76 }
    }

    private fun findBassPitch(pitchClass: Int): Int {
        var pitch = 28 + pitchClass
        while (pitch < 28) pitch += 12
        while (pitch > 40) pitch -= 12
        return pitch
    }

    private fun findStringsPitch(pitchClass: Int): Int {
        var pitch = 48 + pitchClass  // C3
        while (pitch < 48) pitch += 12
        while (pitch > 60) pitch -= 12
        return pitch
    }

    // NEW made by Claude 09/08/2026 — mid range C4-B4, used by Brass/Reed/Ethnic melody
    private fun findMidRangePitch(pitchClass: Int): Int {
        var pitch = 60 + pitchClass  // C4
        while (pitch < 60) pitch += 12
        while (pitch > 72) pitch -= 12
        return pitch
    }

    // NEW made by Claude 09/08/2026 — brass section chord voicing, mid range
    private fun findBrassChordNotes(chord: ResolvedChord): List<Int> {
        var root = chord.rootPitchClass + 60  // C4
        while (root < 60) root += 12
        while (root > 72) root -= 12
        return chord.quality.intervals.take(3).map { interval ->
            val note = root + interval
            if (note > 76) note - 12 else note
        }.distinct()
    }

    // MODIFIED made by Claude 19/08/2026 — replaced chord.quality.intervals with
// ChordNoteBuilder.INTERVALS which covers all 25 ChordQuality values including
// 7th/9th/extended tones. Root placed in C3-B3 range (MIDI 48-59) for
// mid-register piano comping. Notes capped at C5 (MIDI 72) to avoid shrillness.
    private fun findPianoChordNotes(
        chord: ResolvedChord,
        chordType: ChordType = ChordType.FULL
    ): List<Int> {
        val rootMidi = ChordNoteBuilder.nearestMidi(chord.rootPitchClass, 48)
            .let { if (it > 59) it - 12 else it }   // stay in C3-B3 (MIDI 48-59)
        return ChordNoteBuilder.buildNotes(rootMidi, chord.quality, chordType)
            .map { if (it > 72) it - 12 else it }    // fold anything above C5 down an octave
            .distinct()
    }
}