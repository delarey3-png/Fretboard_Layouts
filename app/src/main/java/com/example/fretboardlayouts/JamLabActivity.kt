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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.fretboardlayouts.theory.buildVisualStrumState
import com.example.fretboardlayouts.theory.VisualStrumAction
import com.example.fretboardlayouts.theory.allPickingPresets
import com.example.fretboardlayouts.theory.PickingPreset
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.example.fretboardlayouts.ui.theme.FretboardLayoutsTheme
import kotlin.math.roundToInt
import com.example.fretboardlayouts.theory.ProgressionOption
import com.example.fretboardlayouts.theory.buildProgressionOptions
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign

// made by Claude 08/07: Instrument role matrix definitions
enum class InstrumentRole { OFF, STRUM_CHORD, PICK_ARPEGGIO, HYBRID }

data class InstrumentDef(
    val key: String,
    val displayName: String,
    val emoji: String,
    val channel: Int,
    val defaultRole: InstrumentRole = InstrumentRole.OFF,
    val supportsPickArpeggio: Boolean = true,
    val supportsHybrid: Boolean = true
)

val INSTRUMENT_DEFS = listOf(
    InstrumentDef("guitar",  "Guitar",        "🎸", 0, InstrumentRole.STRUM_CHORD),
    InstrumentDef("bass",    "Bass",           "🎸", 1, InstrumentRole.STRUM_CHORD),
    InstrumentDef("drums",   "Drums",          "🥁", 9, InstrumentRole.STRUM_CHORD,
        supportsPickArpeggio = false, supportsHybrid = false),
    InstrumentDef("piano",   "Piano / Synth",  "🎹", 2),
    InstrumentDef("strings", "Strings",        "🎻", 3),
    InstrumentDef("winds",   "Winds / Brass",  "🎺", 4, supportsHybrid = false)
)

val INSTRUMENT_PROGRAMS = mapOf(
    "guitar"  to listOf("Nylon" to 24, "Steel" to 25, "Jazz Elec" to 26,
        "Clean" to 27, "Muted" to 28, "Overdrive" to 29, "Distortion" to 30),
    "bass"    to listOf("Acoustic" to 32, "Fingered" to 33, "Picked" to 34,
        "Fretless" to 35, "Slap" to 36),
    "drums"   to listOf("Standard" to 0, "Room" to 8, "Power" to 16,
        "Electronic" to 24, "TR-808" to 25, "Jazz" to 32,
        "Brush" to 40, "Orchestra" to 48),
    "piano"   to listOf("Grand Piano" to 0, "Bright Piano" to 1, "Electric Piano" to 4,
        "Harpsichord" to 6, "Celesta" to 8, "Synth Pad" to 88,
        "Synth Choir" to 91, "Bowed Glass" to 92),
    "strings" to listOf("Violin" to 40, "Viola" to 41, "Cello" to 42,
        "Contrabass" to 43, "Tremolo" to 44, "Pizzicato" to 45,
        "Harp" to 46, "Timpani" to 47),
    "winds"   to listOf("Flute" to 73, "Recorder" to 74, "Trumpet" to 56,
        "Trombone" to 57, "Tuba" to 58, "French Horn" to 60,
        "Alto Sax" to 65, "Soprano Sax" to 64)
)

/**
 * Visual display for strumming patterns using arrows (Gemini 27/06)
 */
@Composable
fun StrummingVisualDisplay(
    preset: StrumPreset,
    timeSignature: TimeSignature,
    modifier: Modifier = Modifier
) {
    val visualState = remember(preset, timeSignature) { buildVisualStrumState(preset, timeSignature) }
    
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val ticksPerBeat = preset.layers.firstOrNull()?.ticksPerBeat ?: 4
        
        visualState.forEachIndexed { index, action ->
            // Insert visual beat divider before every new beat (except the first)
            if (index > 0 && index % ticksPerBeat == 0) {
                Text(
                    text = "|",
                    color = Color.DarkGray,
                    fontSize = 32.sp,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .align(Alignment.CenterVertically)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(32.dp)
            ) {
                // Accent Marker
                Text(
                    text = if (action.isAccent) ">" else " ",
                    color = Color.Red,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                
                // Arrow
                Text(
                    text = if (!action.isHit) " " else if (action.isDown) "↓" else "↑",
                    fontSize = 24.sp,
                    color = if (action.isAccent) Color.Red else Color.White,
                    fontWeight = if (action.isAccent) FontWeight.ExtraBold else FontWeight.Normal
                )
                
                // Beat Label (1, +, 2, etc)
                Text(
                    text = action.label.ifEmpty { " " },
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
                
                // D/U Text
                Text(
                    text = if (!action.isHit) " " else if (action.isDown) "D" else "U",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (action.isHit) Color.Red else Color.Transparent
                )
            }
        }
    }
}


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
    var currentProgression by remember { mutableStateOf("I - V - vi - IV (Pop/Country/Rock)") }
    var currentTempo by remember { mutableStateOf(100) }
    var currentTimeSignature by remember { mutableStateOf(TimeSignature.FOUR_FOUR) }
    var currentScale by remember { mutableStateOf(ScaleType.FULL) }
    var currentStrumPreset by remember { mutableStateOf(allGuitarPresets.firstOrNull() ?: allGuitarPresets[0]) }
    var currentPickingPreset by remember { mutableStateOf(allPickingPresets[0]) }
    var customStrumMode by remember { mutableStateOf(false) }

    // made by Gemini 27/06: Context-aware progression options
    val currentKeyObj = remember(currentKey) { MusicKey.fromString(currentKey) }
    val progressionOptions = remember(currentKeyObj) {
        com.example.fretboardlayouts.theory.buildProgressionOptions(currentKeyObj)
    }

    // Auto-select valid progression
    LaunchedEffect(currentKeyObj) {
        val currentValid = progressionOptions.find { it.name == currentProgression }?.enabled ?: false
        if (!currentValid) {
            progressionOptions.firstOrNull { it.enabled }?.let { currentProgression = it.name }
        }
    }

    // Get filtered strum pattern options based on genre and custom mode
    val strumPatternOptions = remember(currentGenre, customStrumMode) {
        buildPresetOptions(allGuitarPresets, currentGenre, currentTimeSignature, customStrumMode)
    }

    // Track selected program per channel for visual feedback
    var selectedProgramByChannel by remember { mutableStateOf(mapOf<Int, Int>()) }

    var isPlaying by remember { mutableStateOf(false) }
    var showGeneratingMessage by remember { mutableStateOf(false) }
    var currentTimeline by remember { mutableStateOf<JamTimeline?>(null) }

    // made by Claude 08/07: Instrument role and panel state
    var instrumentRoles by remember {
        mutableStateOf(INSTRUMENT_DEFS.associate { it.key to it.defaultRole })
    }
    var selectedInstrumentKey by remember { mutableStateOf("guitar") }

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
        // made by Gemini 27/06: converted selections to buttons
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
                "C Major", "C Minor", "Db Major", "Db Minor", "D Major", "D Minor",
                "Eb Major", "Eb Minor", "E Major", "E Minor", "F Major", "F Minor",
                "Gb Major", "Gb Minor", "G Major", "G Minor", "Ab Major", "Ab Minor",
                "A Major", "A Minor", "Bb Major", "Bb Minor", "B Major", "B Minor"
            ),
            onSelected = { currentKey = it },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text("Progression", style = MaterialTheme.typography.labelSmall)
        // made by Gemini 27/06: Modality-aware progression dropdown
        SimpleProgressionDropdown(
            selected = currentProgression,
            options = progressionOptions,
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
        Spacer(modifier = Modifier.height(8.dp))

        Text("Picking Pattern", style = MaterialTheme.typography.labelSmall)
        SimpleDropdown(
            selected = currentPickingPreset.name,
            options = allPickingPresets.map { it.name },
            onSelected = { name ->
                currentPickingPreset = allPickingPresets.find { it.name == name } ?: allPickingPresets[0]
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // NEW: Visual Strumming Arrow Display (Gemini 27/06)
        StrummingVisualDisplay(
            preset = currentStrumPreset,
            timeSignature = currentTimeSignature,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
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

        // ══ INSTRUMENT MATRIX (made by Claude 08/07) ══
        Spacer(modifier = Modifier.height(12.dp))
        Text("Instruments", style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(6.dp))

        InstrumentRoleMatrix(
            instrumentRoles = instrumentRoles,
            selectedKey = selectedInstrumentKey,
            onRoleChanged = { key, role ->
                instrumentRoles = instrumentRoles + (key to role)
            },
            onInstrumentSelected = { selectedInstrumentKey = it }
        )

        Spacer(modifier = Modifier.height(8.dp))

        val selectedDef = INSTRUMENT_DEFS.find { it.key == selectedInstrumentKey }
        if (selectedDef != null) {
            InstrumentPatternPanel(
                instrumentKey = selectedInstrumentKey,
                role = instrumentRoles[selectedInstrumentKey] ?: InstrumentRole.OFF,
                strumOptions = strumPatternOptions,
                selectedStrumPreset = currentStrumPreset,
                onStrumSelected = { currentStrumPreset = it },
                selectedPickingPreset = currentPickingPreset,
                onPickingSelected = { currentPickingPreset = it },
                selectedProgram = selectedProgramByChannel[selectedDef.channel],
                onProgramSelected = { program ->
                    selectedProgramByChannel = selectedProgramByChannel + (selectedDef.channel to program)
                },
                audioEngine = audioEngine
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ══ PLAYBACK LOOP (if playing) ══
        if (isPlaying && currentTimeline != null) {
            PlaybackLoopJamLabHandler(
                timeline = currentTimeline!!,
                audioEngine = audioEngine,
                genre = currentGenre,
                preset = currentStrumPreset,
                pickingPreset = currentPickingPreset
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
    audioEngine: JamLabAudioEngine,
    genre: Genre,
    preset: StrumPreset,
    pickingPreset: PickingPreset?
) {
    var lastSequencerLoopTime by remember { mutableLongStateOf(-1L) }
    var pendingNoteOffs by remember { mutableStateOf(listOf<PendingNoteOff>()) }

    // MODIFIED: Use StyleEngine to respect presets and subdivisions (1e&a)
    val backingTrackEvents = remember(timeline, genre, preset, pickingPreset) {
        com.example.fretboardlayouts.audio.StyleEngine.generateAccompaniment(timeline, genre, preset, pickingPreset)
    }


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

    Button(onClick = { expanded = true }, modifier = modifier) {
        Text(selectedName, maxLines = 1)
    }

    if (expanded) {
        DropdownMenu(expanded = true, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.preset.name,
                            color = if (option.enabled) Color.Unspecified else Color.Gray,
                            fontSize = 11.sp
                        )
                    },
                    enabled = option.enabled,
                    onClick = {
                        onSelected(option.preset)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Modality-aware progression dropdown for JamLab.
 * Mirrors the ProgressionDropdown in MainActivity but uses the Button style
 * consistent with other JamLab dropdowns.
 * // made by Claude 08/07
 */
@Composable
private fun SimpleProgressionDropdown(
    selected: String,
    options: List<ProgressionOption>,
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
                    text = {
                        Text(
                            text = option.name,
                            color = if (option.enabled) Color.Unspecified else Color.Gray,
                            fontSize = 11.sp
                        )
                    },
                    enabled = option.enabled,
                    onClick = {
                        onSelected(option.name)
                        expanded = false
                    }
                )
            }
        }
    }
}

// made by Claude 08/07: Instrument role matrix
@Composable
private fun InstrumentRoleMatrix(
    instrumentRoles: Map<String, InstrumentRole>,
    selectedKey: String,
    onRoleChanged: (String, InstrumentRole) -> Unit,
    onInstrumentSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(vertical = 6.dp)
        ) {
            Text("", modifier = Modifier.weight(2f))
            listOf("Off", "Strum/Chord", "Pick/Arp", "Hybrid").forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        INSTRUMENT_DEFS.forEach { def ->
            val role = instrumentRoles[def.key] ?: InstrumentRole.OFF
            val isSelected = selectedKey == def.key
            val isActive = role != InstrumentRole.OFF
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        else Color.Transparent
                    )
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${def.emoji} ${def.displayName}",
                    modifier = Modifier
                        .weight(2f)
                        .padding(start = 10.dp)
                        .clickable { onInstrumentSelected(def.key) },
                    fontSize = 11.sp,
                    color = if (isActive) MaterialTheme.colorScheme.onSurface else Color.Gray,
                    fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
                )
                // Off
                RoleRadio(
                    selected = role == InstrumentRole.OFF,
                    enabled = true,
                    modifier = Modifier.weight(1f)
                ) { onRoleChanged(def.key, InstrumentRole.OFF); onInstrumentSelected(def.key) }
                // Strum/Chord
                RoleRadio(
                    selected = role == InstrumentRole.STRUM_CHORD,
                    enabled = true,
                    modifier = Modifier.weight(1f)
                ) {
                    onRoleChanged(
                        def.key,
                        InstrumentRole.STRUM_CHORD
                    ); onInstrumentSelected(def.key)
                }
                // Pick/Arpeggio
                RoleRadio(
                    selected = role == InstrumentRole.PICK_ARPEGGIO,
                    enabled = def.supportsPickArpeggio,
                    modifier = Modifier.weight(1f)
                ) {
                    onRoleChanged(
                        def.key,
                        InstrumentRole.PICK_ARPEGGIO
                    ); onInstrumentSelected(def.key)
                }
                // Hybrid
                RoleRadio(
                    selected = role == InstrumentRole.HYBRID,
                    enabled = def.supportsHybrid,
                    modifier = Modifier.weight(1f)
                ) { onRoleChanged(def.key, InstrumentRole.HYBRID); onInstrumentSelected(def.key) }
            }
        }
    }
}

@Composable
private fun RoleRadio(
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(
                    when {
                        !enabled -> Color.Transparent
                        selected -> MaterialTheme.colorScheme.primary
                        else -> Color.Transparent
                    }
                )
                .border(
                    1.5.dp,
                    when {
                        !enabled -> Color.Gray.copy(alpha = 0.25f)
                        selected -> MaterialTheme.colorScheme.primary
                        else -> Color.Gray
                    },
                    CircleShape
                )
                .then(if (enabled) Modifier.clickable { onClick() } else Modifier),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }
    }
}

// made by Claude 08/07: Pattern + sound panel for selected instrument
@Composable
private fun InstrumentPatternPanel(
    instrumentKey: String,
    role: InstrumentRole,
    strumOptions: List<PresetOption>,
    selectedStrumPreset: StrumPreset,
    onStrumSelected: (StrumPreset) -> Unit,
    selectedPickingPreset: PickingPreset,
    onPickingSelected: (PickingPreset) -> Unit,
    selectedProgram: Int?,
    onProgramSelected: (Int) -> Unit,
    audioEngine: JamLabAudioEngine
) {
    val def = INSTRUMENT_DEFS.find { it.key == instrumentKey } ?: return
    val roleLabel = when (role) {
        InstrumentRole.OFF -> "off"
        InstrumentRole.STRUM_CHORD ->
            if (instrumentKey in listOf("piano", "strings", "winds")) "chord patterns"
            else "strum patterns"
        InstrumentRole.PICK_ARPEGGIO ->
            if (instrumentKey in listOf("piano", "strings", "winds")) "arpeggio patterns"
            else "picking patterns"
        InstrumentRole.HYBRID -> "hybrid patterns"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                0.5.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp))
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Text(
                text = "${def.emoji} ${def.displayName} — $roleLabel",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
        Column(modifier = Modifier.padding(10.dp)) {
            // Pattern section
            if (role == InstrumentRole.OFF) {
                Text(
                    "Select a role above to see patterns",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            } else {
                Text("Pattern", fontSize = 10.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                when {
                    instrumentKey == "guitar" && role == InstrumentRole.STRUM_CHORD -> {
                        PresetDropdownJamLab(
                            label = "Strum Pattern",
                            options = strumOptions,
                            selectedName = selectedStrumPreset.name,
                            onSelected = onStrumSelected,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    instrumentKey == "guitar" && role == InstrumentRole.PICK_ARPEGGIO -> {
                        SimpleDropdown(
                            selected = selectedPickingPreset.name,
                            options = allPickingPresets.map { it.name },
                            onSelected = { name ->
                                onPickingSelected(
                                    allPickingPresets.find { it.name == name }
                                        ?: allPickingPresets[0]
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    else -> {
                        Text(
                            "Patterns coming soon",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            // Sound / program selection — always visible
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text("Sound", fontSize = 10.sp, color = Color.Gray,
                modifier = Modifier.padding(bottom = 6.dp))
            val programs = INSTRUMENT_PROGRAMS[instrumentKey] ?: emptyList()
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(programs.size) { index ->
                    val (name, program) = programs[index]
                    val isSelected = program == selectedProgram
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = program.toString().padStart(3, '0'),
                                fontSize = 8.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        if (isSelected) {
                            Button(
                                onClick = {
                                    audioEngine.changeProgramOnChannel(def.channel, program)
                                    onProgramSelected(program)
                                },
                                modifier = Modifier.height(36.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)
                            ) { Text(name, fontSize = 10.sp) }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    audioEngine.changeProgramOnChannel(def.channel, program)
                                    onProgramSelected(program)
                                },
                                modifier = Modifier.height(36.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)
                            ) { Text(name, fontSize = 10.sp) }
                        }
                    }
                }
            }
        }
    }
}