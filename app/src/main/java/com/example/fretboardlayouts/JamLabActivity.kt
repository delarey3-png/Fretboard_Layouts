package com.example.fretboardlayouts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fretboardlayouts.audio.BackingTrackGenerator
import com.example.fretboardlayouts.audio.JamLabAudioEngine
import com.example.fretboardlayouts.theory.Genre
import com.example.fretboardlayouts.theory.JamTimeline
import com.example.fretboardlayouts.theory.MusicKey
import com.example.fretboardlayouts.theory.Progressions
import com.example.fretboardlayouts.theory.ScaleType
import com.example.fretboardlayouts.theory.TimeSignature
import com.example.fretboardlayouts.theory.buildJamTimeline
import com.example.fretboardlayouts.theory.PresetOption
import com.example.fretboardlayouts.theory.StrumPreset
import com.example.fretboardlayouts.theory.allGuitarPresets
import com.example.fretboardlayouts.theory.buildPresetOptions
import androidx.compose.material3.HorizontalDivider
import com.example.fretboardlayouts.ui.theme.FretboardLayoutsTheme
import kotlin.math.roundToInt

/**
 * Jam Lab Activity — Sound Sandbox for testing genres and discovering presets.
 *
 * Completely standalone, independent from MainViewModel:
 * - Own local state for all settings
 * - Own audio pipeline via JamLabAudioEngine
 * - Can test sound combinations without affecting Jam screen
 * - Safe to enter and exit without corrupting shared audio state
 */
class JamLabActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FretboardLayoutsTheme {
                JamLabScreen()
            }
        }
    }
}

/**
 * Jam Lab Screen — A-2 Sound Sandbox
 *
 * Same playback capability as Jam screen, but without the fretboard visual.
 * Users can change audio settings freely, reset on sync loss, and save presets.
 */
@Composable
fun JamLabScreen() {
    val context = LocalContext.current
    val audioEngine = remember { JamLabAudioEngine(context) }

    // ══ LOCAL STATE (completely independent from MainViewModel) ══
    var currentGenre by remember { mutableStateOf(Genre.ROCK) }
    var currentKey by remember { mutableStateOf("C Major") }
    var currentProgression by remember { mutableStateOf("I-V-vi-IV (Pop/Country/Rock)") }
    var currentTempo by remember { mutableStateOf(100) }
    var currentTimeSignature by remember { mutableStateOf(TimeSignature.FOUR_FOUR) }
    var currentScale by remember { mutableStateOf(ScaleType.FULL) }
    var currentStrumPreset by remember { mutableStateOf(allGuitarPresets.firstOrNull() ?: allGuitarPresets[0]) }
    var customStrumMode by remember { mutableStateOf(false) }

    // Get filtered strum pattern options based on genre and custom mode
    val strumPatternOptions = remember(currentGenre, customStrumMode) {
        buildPresetOptions(allGuitarPresets, currentGenre, currentTimeSignature, customStrumMode)
    }

    // Track selected program per channel for visual feedback
    var selectedProgramByChannel by remember { mutableStateOf(mapOf<Int, Int>()) }

    var isPlaying by remember { mutableStateOf(false) }
    var showGeneratingMessage by remember { mutableStateOf(false) }
    var currentTimeline by remember { mutableStateOf<JamTimeline?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // ══ HEADER ══
        Text(
            "🧪 Jam Lab — Sound Sandbox",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // ══ SETUP CONTROLS (A1 + A2 Combined) ══
        Text("Genre", style = MaterialTheme.typography.labelSmall)
        SimpleDropdown(
            selected = currentGenre.displayName,
            options = Genre.values().map { it.displayName },
            onSelected = { name ->
                currentGenre = Genre.values().find { it.displayName == name } ?: Genre.ROCK
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text("Key", style = MaterialTheme.typography.labelSmall)
        SimpleDropdown(
            selected = currentKey,
            options = listOf(
                "C Major", "C Minor", "G Major", "G Minor", "D Major", "D Minor",
                "A Major", "A Minor", "E Major", "E Minor", "B Major", "B Minor",
                "F# Major", "F# Minor", "F Major", "F Minor", "Bb Major", "Bb Minor",
                "Eb Major", "Eb Minor", "Ab Major", "Ab Minor", "Db Major", "Db Minor",
                "Gb Major", "Gb Minor", "C# Major", "C# Minor"
            ),
            onSelected = { currentKey = it },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text("Progression", style = MaterialTheme.typography.labelSmall)
        SimpleDropdown(
            selected = currentProgression.take(30) + (if (currentProgression.length > 30) "..." else ""),
            options = Progressions.ALL.keys.toList(),
            onSelected = { currentProgression = it },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text("Time Signature", style = MaterialTheme.typography.labelSmall)
        SimpleDropdown(
            selected = currentTimeSignature.display,
            options = TimeSignature.values().map { it.display },
            onSelected = { display ->
                currentTimeSignature = TimeSignature.values().find { it.display == display }
                    ?: TimeSignature.FOUR_FOUR
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text("Strum Pattern", style = MaterialTheme.typography.labelSmall)
        PresetDropdownJamLab(
            label = "Strum Pattern",
            options = strumPatternOptions,
            selectedName = currentStrumPreset.name,
            onSelected = { currentStrumPreset = it },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                "Custom Strum Mode",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            androidx.compose.material3.Switch(checked = customStrumMode, onCheckedChange = { customStrumMode = it })
        }
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "Tempo: $currentTempo BPM",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Slider(
            value = currentTempo.toFloat(),
            onValueChange = { currentTempo = it.roundToInt() },
            valueRange = 40f..200f,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // ══ PLAYBACK CONTROLS ══
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    isPlaying = true
                    showGeneratingMessage = true
                    audioEngine.stopAudio()

                    // Generate backing track with current settings
                    val key = MusicKey.fromString(currentKey)
                    val progression = Progressions.ALL[currentProgression] ?: Progressions.ALL.values.first()
                    val timeline = buildJamTimeline(
                        key = key,
                        progressionSlots = progression,
                        scaleType = currentScale,
                        chordOverlayMode = com.example.fretboardlayouts.theory.ChordOverlayMode.ALL_CHORD_TONES,
                        tempoBpm = currentTempo,
                        timeSignature = currentTimeSignature
                    )
                    currentTimeline = timeline
                    showGeneratingMessage = false
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Generate & Play")
            }

            Button(
                onClick = {
                    isPlaying = false
                    audioEngine.stopAudio()
                },
                modifier = Modifier.weight(1f),
                enabled = currentTimeline != null
            ) {
                Text("Stop")
            }

            OutlinedButton(
                onClick = {
                    isPlaying = false
                    audioEngine.stopAudio()
                    currentTimeline = null
                    showGeneratingMessage = false
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Reset")
            }
        }

        if (showGeneratingMessage) {
            Text(
                "Generating...",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ══ INSTRUMENT GROUPS (A2 - Sound) ══
        InstrumentGroupJamLab(
            title = "🎸 Guitars (Ch 0)",
            channel = 0,
            instruments = listOf(
                "Nylon" to 24, "Steel" to 25, "Jazz Electric" to 26,
                "Clean" to 27, "Muted" to 28, "Overdriven" to 29, "Distortion" to 30
            ),
            selectedProgram = selectedProgramByChannel[0],
            onSelect = { program ->
                audioEngine.changeProgramOnChannel(0, program)
                selectedProgramByChannel = selectedProgramByChannel + (0 to program)
            }
        )
        Spacer(modifier = Modifier.height(12.dp))

        InstrumentGroupJamLab(
            title = "🎸 Bass (Ch 1)",
            channel = 1,
            instruments = listOf(
                "Acoustic" to 32, "Fingered" to 33, "Picked" to 34,
                "Fretless" to 35, "Slap" to 36
            ),
            selectedProgram = selectedProgramByChannel[1],
            onSelect = { program ->
                audioEngine.changeProgramOnChannel(1, program)
                selectedProgramByChannel = selectedProgramByChannel + (1 to program)
            }
        )
        Spacer(modifier = Modifier.height(12.dp))

        InstrumentGroupJamLab(
            title = "🥁 Drums (Ch 9)",
            channel = 9,
            instruments = listOf(
                "Standard" to 0, "Room" to 8, "Power" to 16,
                "Electronic" to 24, "TR-808" to 25, "Jazz" to 32, "Brush" to 40, "Orchestra" to 48
            ),
            selectedProgram = selectedProgramByChannel[9],
            onSelect = { program ->
                audioEngine.changeProgramOnChannel(9, program)
                selectedProgramByChannel = selectedProgramByChannel + (9 to program)
            }
        )
        Spacer(modifier = Modifier.height(12.dp))

        InstrumentGroupJamLab(
            title = "🎹 Keys & Pads (Ch 2)",
            channel = 2,
            instruments = listOf(
                "Grand Piano" to 0, "Bright Piano" to 1, "Electric Piano" to 4,
                "Harpsichord" to 6, "Celesta" to 8, "Synth Pad" to 88,
                "Synth Choir" to 91, "Bowed Glass" to 92
            ),
            selectedProgram = selectedProgramByChannel[2],
            onSelect = { program ->
                audioEngine.changeProgramOnChannel(2, program)
                selectedProgramByChannel = selectedProgramByChannel + (2 to program)
            }
        )
        Spacer(modifier = Modifier.height(12.dp))

        InstrumentGroupJamLab(
            title = "🎻 Strings (Ch 3)",
            channel = 3,
            instruments = listOf(
                "Violin" to 40, "Viola" to 41, "Cello" to 42, "Contrabass" to 43,
                "Tremolo Strings" to 44, "Pizzicato Strings" to 45, "Harp" to 46, "Timpani" to 47
            ),
            selectedProgram = selectedProgramByChannel[3],
            onSelect = { program ->
                audioEngine.changeProgramOnChannel(3, program)
                selectedProgramByChannel = selectedProgramByChannel + (3 to program)
            }
        )
        Spacer(modifier = Modifier.height(12.dp))

        InstrumentGroupJamLab(
            title = "🎺 Winds (Ch 4)",
            channel = 4,
            instruments = listOf(
                "Flute" to 73, "Recorder" to 74, "Trumpet" to 56,
                "Trombone" to 57, "Tuba" to 58, "French Horn" to 60,
                "Alto Sax" to 65, "Soprano Sax" to 64
            ),
            selectedProgram = selectedProgramByChannel[4],
            onSelect = { program ->
                audioEngine.changeProgramOnChannel(4, program)
                selectedProgramByChannel = selectedProgramByChannel + (4 to program)
            }
        )
        Spacer(modifier = Modifier.height(20.dp))

        // ══ PLAYBACK LOOP (if playing) ══
        if (isPlaying && currentTimeline != null) {
            PlaybackLoopJamLabHandler(
                timeline = currentTimeline!!,
                audioEngine = audioEngine
            )
        }
    }
}

/**
 * Playback loop handler — generates backing track and plays it via audioEngine
 */
@Composable
private fun PlaybackLoopJamLabHandler(
    timeline: JamTimeline,
    audioEngine: JamLabAudioEngine
) {
    var lastSequencerLoopTime by remember { mutableLongStateOf(-1L) }
    var pendingNoteOffs by remember { mutableStateOf(listOf<PendingNoteOff>()) }

    val backingTrackEvents = remember { BackingTrackGenerator.generateLoopEvents(timeline) }

    LaunchedEffect(Unit) {
        val startTime = withFrameMillis { it }
        while (true) {
            withFrameMillis { frameTime ->
                val currentTimeMs = frameTime - startTime
                val loopTime = currentTimeMs % timeline.loopDurationMs

                // 1. DETERMINISTIC SEQUENCER
                if (lastSequencerLoopTime == -1L) {
                    lastSequencerLoopTime = loopTime
                    if (loopTime < 100) {
                        backingTrackEvents.filter { it.timeMs == 0L }.forEach { event ->
                            audioEngine.noteOn(event.channel, event.pitch, event.velocity)
                            pendingNoteOffs = pendingNoteOffs + PendingNoteOff(
                                event.channel, event.pitch, currentTimeMs + event.durationMs
                            )
                        }
                    }
                }

                // Handle loop wrap-around
                if (loopTime < lastSequencerLoopTime) {
                    backingTrackEvents.filter { it.timeMs > lastSequencerLoopTime }.forEach { event ->
                        audioEngine.noteOn(event.channel, event.pitch, event.velocity)
                        pendingNoteOffs = pendingNoteOffs + PendingNoteOff(
                            event.channel, event.pitch, currentTimeMs + event.durationMs
                        )
                    }
                    lastSequencerLoopTime = -1L
                }

                // Standard frame check
                backingTrackEvents
                    .filter { it.timeMs > lastSequencerLoopTime && it.timeMs <= loopTime }
                    .forEach { event ->
                        audioEngine.noteOn(event.channel, event.pitch, event.velocity)
                        pendingNoteOffs = pendingNoteOffs + PendingNoteOff(
                            event.channel, event.pitch, currentTimeMs + event.durationMs
                        )
                    }
                lastSequencerLoopTime = loopTime

                // Fire note-offs that are due
                if (pendingNoteOffs.isNotEmpty()) {
                    val dueOffs = pendingNoteOffs.filter { it.offAtMs <= currentTimeMs }
                    if (dueOffs.isNotEmpty()) {
                        dueOffs.forEach { audioEngine.noteOff(it.channel, it.pitch) }
                        pendingNoteOffs = pendingNoteOffs - dueOffs.toSet()
                    }
                }
            }
        }
    }
}

// ══ HELPER DATA CLASSES ══

private data class PendingNoteOff(val channel: Int, val pitch: Int, val offAtMs: Long)

// ══ COMPOSABLE HELPERS ══

/**
 * Simple dropdown for JamLab — no external viewModel dependency
 */
@Composable
private fun SimpleDropdown(
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Button(onClick = { expanded = true }, modifier = modifier) {
        Text(selected, maxLines = 1)
    }

    if (expanded) {
        DropdownMenu(expanded = true, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, fontSize = 11.sp) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Preset dropdown with smart genre-based filtering
 * Shows all presets but greys out ones that don't apply to current genre (unless custom mode is on)
 */
@Composable
private fun PresetDropdownJamLab(
    label: String,
    options: List<PresetOption>,
    selectedName: String,
    onSelected: (StrumPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(vertical = 12.dp)
        ) {
            Text(selectedName, style = MaterialTheme.typography.bodyLarge)
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.preset.name) },
                        enabled = option.enabled,
                        onClick = { onSelected(option.preset); expanded = false }
                    )
                }
            }
        }
        HorizontalDivider()
    }
}

/**
 * Instrument group with horizontal scrolling buttons
 * Shows selected instrument in a different color
 */
@Composable
private fun InstrumentGroupJamLab(
    title: String,
    channel: Int,
    instruments: List<Pair<String, Int>>,
    selectedProgram: Int?,
    onSelect: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.labelSmall)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            items(instruments.size) { index ->
                val (name, program) = instruments[index]
                val isSelected = program == selectedProgram

                if (isSelected) {
                    // Selected button — filled with primary color
                    Button(
                        onClick = { onSelect(program) },
                        modifier = Modifier.height(36.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)
                    ) {
                        Text(name, fontSize = 10.sp)
                    }
                } else {
                    // Unselected button — outlined style
                    OutlinedButton(
                        onClick = { onSelect(program) },
                        modifier = Modifier.height(36.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)
                    ) {
                        Text(name, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}