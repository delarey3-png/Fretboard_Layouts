package com.example.fretboardlayouts

import android.R
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fretboardlayouts.audio.JamLabAudioEngine
import com.example.fretboardlayouts.theory.ChordOverlayMode
import com.example.fretboardlayouts.theory.Genre
import com.example.fretboardlayouts.theory.HumanisationLevel
import com.example.fretboardlayouts.theory.JamTimeline
import com.example.fretboardlayouts.theory.MusicKey
import com.example.fretboardlayouts.theory.PresetOption
import com.example.fretboardlayouts.theory.Progressions
import com.example.fretboardlayouts.theory.ProgressionOption
import com.example.fretboardlayouts.theory.PickingPreset
import com.example.fretboardlayouts.theory.ScaleType
import com.example.fretboardlayouts.theory.StrumPreset
import com.example.fretboardlayouts.theory.TimeSignature
import com.example.fretboardlayouts.theory.VisualStrumAction
import com.example.fretboardlayouts.theory.allGuitarPresets
import com.example.fretboardlayouts.theory.allPickingPresets
import com.example.fretboardlayouts.theory.buildJamTimeline
import com.example.fretboardlayouts.theory.buildPresetOptions
import com.example.fretboardlayouts.theory.buildProgressionOptions
import com.example.fretboardlayouts.theory.buildVisualStrumState
import com.example.fretboardlayouts.ui.theme.FretboardLayoutsTheme
import kotlin.math.roundToInt
import com.example.fretboardlayouts.theory.InstrumentRole // made by Claude 11/07
import kotlinx.coroutines.delay // NEW made by Claude 08/08/2026

// ================================================================
// TOP-LEVEL DEFINITIONS
// made by Claude 10/07: Instrument role matrix definitions
// ================================================================
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
    InstrumentDef("guitar",  "Guitar",       "🎸", 0, InstrumentRole.STRUM_CHORD),
    InstrumentDef("bass",    "Bass",          "🎸", 1, InstrumentRole.STRUM_CHORD),
    InstrumentDef("drums",   "Drums",         "🥁", 9, InstrumentRole.STRUM_CHORD,
        supportsPickArpeggio = false, supportsHybrid = false),
    InstrumentDef("piano",   "Piano / Synth", "🎹", 2),
    InstrumentDef("strings", "Strings",       "🎻", 3),
    InstrumentDef("winds",   "Winds / Brass", "🎺", 4, supportsHybrid = false)
)

// NEW made by Claude 05/08/2026
// PatchOption replaces bare Int program numbers — carries bank + program together
data class PatchOption(val name: String, val bank: Int, val program: Int)

val INSTRUMENT_PROGRAMS = mapOf(
    "guitar"  to listOf(
        PatchOption("SGM Nylon",        0, 24),
        PatchOption("Steel Sammy",      0, 25),
        PatchOption("Fluid Jazz",       0, 26),
        PatchOption("MK Clean",         0, 27),
        PatchOption("Crisis Muted",     0, 28),
        PatchOption("Arachno OD",       0, 29),
        PatchOption("MK Jazz",          1, 26), // NEW made by Claude 05/08/2026
        PatchOption("GS Chorused Cln",  1, 27), // NEW made by Claude 05/08/2026
        PatchOption("Muted Metal",      1, 28), // NEW made by Claude 05/08/2026
        PatchOption("Strix Shadowed",   2, 27), // NEW made by Claude 05/08/2026
        PatchOption("Strix Brt Chorus", 3, 27)  // NEW made by Claude 05/08/2026
    ),
    "bass"    to listOf(
        PatchOption("Crisis Acoustic",  0, 32),
        PatchOption("Crisis Finger",    0, 33),
        PatchOption("Crisis Pick",      0, 34),
        PatchOption("Crisis Fretless",  0, 35),
        PatchOption("Crisis Slap",      0, 36),
        PatchOption("Fluid Pop",        0, 37),
        PatchOption("GS Synth 1",       0, 38),
        PatchOption("GS Synth 2",       0, 39),
        PatchOption("Arachno Finger",   1, 33)  // NEW made by Claude 05/08/2026
    ),
    "drums"   to listOf(
        PatchOption("Standard",   0,   0), // MODIFIED made by Claude 05/08/2026 — bank 128 auto-selected by FluidSynth on ch9
        PatchOption("Room",       0,   8),
        PatchOption("Power",      0,  16),
        PatchOption("TR-808",     0,  25),
        PatchOption("Jazz",       0,  32),
        PatchOption("Brush",      0,  40),
        PatchOption("Orchestra",  0,  48)
    ),
    "piano"   to listOf(
        PatchOption("Grand Piano", 0,  0), PatchOption("Bright Piano", 0,  1),
        PatchOption("Elec Piano",  0,  4), PatchOption("Harpsichord",  0,  6),
        PatchOption("Celesta",     0,  8), PatchOption("Synth Pad",    0, 88),
        PatchOption("Synth Choir", 0, 91), PatchOption("Bowed Glass",  0, 92)
    ),
    "strings" to listOf(
        PatchOption("Violin",    0, 40), PatchOption("Viola",     0, 41),
        PatchOption("Cello",     0, 42), PatchOption("Contrabass",0, 43),
        PatchOption("Tremolo",   0, 44), PatchOption("Pizzicato", 0, 45),
        PatchOption("Harp",      0, 46), PatchOption("Timpani",   0, 47)
    ),
    "winds"   to listOf(
        PatchOption("Flute",      0, 73), PatchOption("Recorder",   0, 74),
        PatchOption("Trumpet",    0, 56), PatchOption("Trombone",   0, 57),
        PatchOption("Tuba",       0, 58), PatchOption("French Horn",0, 60),
        PatchOption("Alto Sax",   0, 65), PatchOption("Soprano Sax",0, 64)
    )
)

// NEW made by Claude 08/08/2026
// Parses the raw pipe-delimited preset string from nativeGetPresets() into
// a map of instrument key → patch list, filtered by GM program ranges.
fun parsePresetsFromSF2(raw: String): Map<String, List<PatchOption>> {
    if (raw.isEmpty()) return emptyMap()
    val all = raw.split("|").mapNotNull { entry ->
        val parts = entry.split(":", limit = 3)
        if (parts.size < 3) null
        else {
            val bank    = parts[0].toIntOrNull() ?: return@mapNotNull null
            val program = parts[1].toIntOrNull() ?: return@mapNotNull null
            val name    = parts[2].trim().ifEmpty { "${bank}:${program}" }
            PatchOption(name, bank, program)
        }
    }
    return mapOf(
        "guitar"  to all.filter { it.bank !in listOf(127, 128) && it.program in 24..31 },
        "bass"    to all.filter { it.bank !in listOf(127, 128) && it.program in 32..39 },
        "strings" to all.filter { it.bank !in listOf(127, 128) && it.program in 40..55 }, // MODIFIED made by Claude 08/08/2026 — added ensemble 48-55
        "winds"   to all.filter { it.bank !in listOf(127, 128) && it.program in 56..79 },
        "piano"   to all.filter { it.bank !in listOf(127, 128) && it.program in 0..23 },   // MODIFIED made by Claude 08/08/2026 — added organs 16-23
        "drums"   to all.filter { it.bank == 128 }
    )
}

// ================================================================
// VISUAL STRUMMING DISPLAY
// made by Gemini 27/06
// ================================================================

@Composable
fun StrummingVisualDisplay(
    preset: StrumPreset,
    timeSignature: TimeSignature,
    modifier: Modifier = Modifier
) {
    val visualState = remember(preset, timeSignature) {
        buildVisualStrumState(preset, timeSignature)
    }
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val ticksPerBeat = preset.layers.firstOrNull()?.ticksPerBeat ?: 4
        visualState.forEachIndexed { index, action ->
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
                Text(
                    text = if (action.isAccent) ">" else " ",
                    color = Color.Red,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (!action.isHit) " " else if (action.isDown) "↓" else "↑",
                    fontSize = 24.sp,
                    color = if (action.isAccent) Color.Red else Color.White,
                    fontWeight = if (action.isAccent) FontWeight.ExtraBold else FontWeight.Normal
                )
                Text(
                    text = action.label.ifEmpty { " " },
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
                Text(
                    text = if (!action.isHit) " " else if (action.isDown) "D" else "U",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (action.isHit) Color.Red else Color.Transparent
                )
            }
        }
    }
}

// ================================================================
// ACTIVITY
// ================================================================

/**
 * Jam Lab Activity — Sound Sandbox for testing genres and discovering presets.
 * Completely standalone, independent from MainViewModel.
 */
class JamLabActivity : ComponentActivity() {
    // NEW made by Claude 08/08/2026
    // Keeps CPU running when screen turns off so audio continues during jamming
    private lateinit var wakeLock: android.os.PowerManager.WakeLock

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Acquire wake lock — allows screen off but keeps audio running
        val powerManager = getSystemService(POWER_SERVICE) as android.os.PowerManager
        wakeLock = powerManager.newWakeLock(
            android.os.PowerManager.PARTIAL_WAKE_LOCK,
            "LetsJam::JamLabAudioWakeLock"
        )
        wakeLock.acquire(4 * 60 * 60 * 1000L) // 4 hour max — covers any reasonable jam session
        setContent {
            FretboardLayoutsTheme {
                JamLabScreen()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release wake lock when activity closes — don't drain battery unnecessarily
        if (::wakeLock.isInitialized && wakeLock.isHeld) {
            wakeLock.release()
        }
    }
}

// ================================================================
// JAM LAB SCREEN
// ================================================================

@Composable
fun JamLabScreen() {
    val context = LocalContext.current
    val audioEngine = remember { JamLabAudioEngine(context) }

    // NEW made by Claude 08/08/2026
    // Read all presets from the loaded SF2 once at startup — no hardcoding needed
    val availablePatches = remember(audioEngine) {
        parsePresetsFromSF2(audioEngine.getRawPresets())
    }

    // ══ LOCAL STATE (completely independent from MainViewModel) ══
    var currentGenre by remember { mutableStateOf(Genre.ROCK) }
    var currentKey by remember { mutableStateOf("C Major") }
    var currentProgression by remember { mutableStateOf("I - V - vi - IV (Pop/Country/Rock)") }
    var currentTempo by remember { mutableStateOf(100) }
    var currentTimeSignature by remember { mutableStateOf(TimeSignature.FOUR_FOUR) }
    var currentScale by remember { mutableStateOf(ScaleType.FULL) }
    var currentStrumPreset by remember { mutableStateOf(allGuitarPresets[0]) }
    var currentPickingPreset by remember { mutableStateOf(allPickingPresets[0]) }
    var customStrumMode by remember { mutableStateOf(false) }
    var currentNoteLength by remember { mutableStateOf("1/4") }
    var currentHumanisation by remember { mutableStateOf(HumanisationLevel.OFF) } // made by Claude 11/07
    // made by Claude 11/07: Tracks current bar for progression display
    var currentBarIndex by remember { mutableStateOf(0) }
    // made by Gemini 27/06: Context-aware progression options
    val currentKeyObj = remember(currentKey) { MusicKey.fromString(currentKey) }
    val progressionOptions = remember(currentKeyObj) {
        buildProgressionOptions(currentKeyObj)
    }

    LaunchedEffect(currentKeyObj) {
        val currentValid =
            progressionOptions.find { it.name == currentProgression }?.enabled ?: false
        if (!currentValid) {
            progressionOptions.firstOrNull { it.enabled }?.let { currentProgression = it.name }
        }
    }

    val strumPatternOptions = remember(currentGenre, customStrumMode, currentTimeSignature) {
        buildPresetOptions(allGuitarPresets, currentGenre, currentTimeSignature, customStrumMode)
    }

    var selectedPatchByChannel by remember { mutableStateOf(mapOf<Int, PatchOption>()) } // MODIFIED made by Claude 05/08/2026
    var isPlaying by remember { mutableStateOf(false) }
    var showGeneratingMessage by remember { mutableStateOf(false) }
    var currentTimeline by remember { mutableStateOf<JamTimeline?>(null) }

    // made by Claude 10/07: Instrument role and panel state
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

        // ══ MUSIC SETUP ══
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

        // made by Gemini 27/06: Modality-aware progression dropdown
        Text("Progression", style = MaterialTheme.typography.labelSmall)
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
                currentTimeSignature =
                    TimeSignature.values().find { it.display == display }
                        ?: TimeSignature.FOUR_FOUR
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
// made by Claude 11/07: Note Length selector
        Text("Note Length", style = MaterialTheme.typography.labelSmall)
        SimpleDropdown(
            selected = currentNoteLength,
            options = listOf("1/2", "1/4", "1/8", "1/16"),
            onSelected = { currentNoteLength = it },
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
                currentPickingPreset =
                    allPickingPresets.find { it.name == name } ?: allPickingPresets[0]
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Visual strumming arrow display
        // made by Claude 11/07: Hide strum display when guitar is in picking mode
        val guitarRole = instrumentRoles["guitar"] ?: InstrumentRole.STRUM_CHORD
        if (guitarRole != InstrumentRole.PICK_ARPEGGIO) {
            StrummingVisualDisplay(
                preset = currentStrumPreset,
                timeSignature = currentTimeSignature,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Custom Strum Mode",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = customStrumMode,
                onCheckedChange = { customStrumMode = it }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        Text("Humanisation", style = MaterialTheme.typography.labelSmall)
        SimpleDropdown(
            selected = currentHumanisation.name.lowercase()
                .replaceFirstChar { it.uppercase() },
            options = HumanisationLevel.values().map { level ->
                level.name.lowercase().replaceFirstChar { it.uppercase() }
            },
            onSelected = { selected ->
                currentHumanisation = HumanisationLevel.values()
                    .find { it.name.equals(selected, ignoreCase = true) }
                    ?: HumanisationLevel.OFF
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
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
        // made by Claude 11/07: Live progression display
        currentTimeline?.let { timeline ->
            JamLabProgressionDisplay(
                timeline = timeline,
                currentBarIndex = currentBarIndex,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }
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
                    val key = MusicKey.fromString(currentKey)
                    val progression =
                        Progressions.ALL[currentProgression] ?: Progressions.ALL.values.first()
                    val timeline = buildJamTimeline(
                        key = key,
                        progressionSlots = progression,
                        scaleType = currentScale,
                        chordOverlayMode = ChordOverlayMode.ALL_CHORD_TONES,
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

        // ══ INSTRUMENT MATRIX (made by Claude 10/07) ══
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
                selectedPatch = selectedPatchByChannel[selectedDef.channel],
                onPatchSelected = { patch ->
                    selectedPatchByChannel =
                        selectedPatchByChannel + (selectedDef.channel to patch)
                },
                availablePatches = availablePatches, // NEW made by Claude 08/08/2026
                audioEngine = audioEngine
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ══ PLAYBACK LOOP ══
        if (isPlaying && currentTimeline != null) {
            PlaybackLoopJamLabHandler(
                timeline = currentTimeline!!,
                audioEngine = audioEngine,
                genre = currentGenre,
                preset = currentStrumPreset,
                pickingPreset = currentPickingPreset,
                instrumentRoles = instrumentRoles,  // made by Claude 10/07
                onBarChanged = { currentBarIndex = it }  // made by Claude 11/07
            )
        }
    } // end Column
} // end JamLabScreen

// ================================================================
// PLAYBACK HANDLER
// ================================================================

@Composable
private fun PlaybackLoopJamLabHandler(
    timeline: JamTimeline,
    audioEngine: JamLabAudioEngine,
    genre: Genre,
    preset: StrumPreset,
    pickingPreset: PickingPreset?,
    instrumentRoles: Map<String, InstrumentRole>,  // made by Claude 10/07
    humanisationLevel: HumanisationLevel = HumanisationLevel.OFF,  // made by Claude 11/07
    onBarChanged: (Int) -> Unit  // made by Claude 11/07
) {
    // made by Claude 10/07: Only fire MIDI events for active channels
    val activeChannels = remember(instrumentRoles) {
        INSTRUMENT_DEFS
            .filter { def ->
                (instrumentRoles[def.key] ?: InstrumentRole.OFF) != InstrumentRole.OFF
            }
            .map { it.channel }
            .toSet()
    }

    var lastSequencerLoopTime by remember { mutableLongStateOf(-1L) }
    val pendingNoteOffs = remember { mutableListOf<PendingNoteOff>() } // made by Claude 11/07: mutableListOf avoids recomposition
    val lastBarIndexRef = remember { intArrayOf(-1) } // made by Claude 11/07: plain array avoids recomposition

    val backingTrackEvents = remember(timeline, genre, preset, pickingPreset, humanisationLevel, instrumentRoles) { // made by Claude 11/07
        com.example.fretboardlayouts.audio.StyleEngine.generateAccompaniment(
            timeline, genre, preset, pickingPreset, humanisationLevel, instrumentRoles
        )
    }

    LaunchedEffect(Unit) {
        // MODIFIED made by Claude 08/08/2026
        // Replaced withFrameMillis (display-tied, stops when screen off) with
        // System.currentTimeMillis() + delay() — keeps running with screen off
        val startTime = System.currentTimeMillis()
        while (true) {
            val frameTime = System.currentTimeMillis()
            val currentTimeMs = frameTime - startTime
            val loopTime = currentTimeMs % timeline.loopDurationMs
            // Surface current bar index to UI
            val currentBar = timeline.events
                .firstOrNull { loopTime >= it.startMs && loopTime < it.startMs + it.durationMs }
                ?.barIndex ?: 0
            if (currentBar != lastBarIndexRef[0]) {
                lastBarIndexRef[0] = currentBar
                onBarChanged(currentBar)
            }
            if (lastSequencerLoopTime == -1L) {
                lastSequencerLoopTime = loopTime
                if (loopTime < 100) {
                    backingTrackEvents
                        .filter { it.timeMs == 0L && it.channel in activeChannels }
                        .forEach { event ->
                            audioEngine.noteOn(event.channel, event.pitch, event.velocity)
                            pendingNoteOffs.add(PendingNoteOff(
                                event.channel, event.pitch, currentTimeMs + event.durationMs
                            ))
                        }
                }
            }
            if (loopTime < lastSequencerLoopTime) {
                backingTrackEvents
                    .filter {
                        it.timeMs > lastSequencerLoopTime && it.channel in activeChannels
                    }
                    .forEach { event ->
                        audioEngine.noteOn(event.channel, event.pitch, event.velocity)
                        pendingNoteOffs.add(PendingNoteOff(
                            event.channel, event.pitch, currentTimeMs + event.durationMs
                        ))
                    }
                lastSequencerLoopTime = -1L
            }
            backingTrackEvents
                .filter {
                    it.timeMs > lastSequencerLoopTime &&
                            it.timeMs <= loopTime &&
                            it.channel in activeChannels
                }
                .forEach { event ->
                    audioEngine.noteOn(event.channel, event.pitch, event.velocity)
                    pendingNoteOffs.add(PendingNoteOff(
                        event.channel, event.pitch, currentTimeMs + event.durationMs
                    ))
                }
            lastSequencerLoopTime = loopTime
            if (pendingNoteOffs.isNotEmpty()) {
                val dueOffs = pendingNoteOffs.filter { it.offAtMs <= currentTimeMs }
                if (dueOffs.isNotEmpty()) {
                    dueOffs.forEach { audioEngine.noteOff(it.channel, it.pitch) }
                    pendingNoteOffs.removeAll(dueOffs.toSet())
                }
            }
            delay(8L) // ~120 polls/sec — tight enough for musical timing, runs with screen off
        }
    }
}

// ================================================================
// HELPER DATA CLASSES
// ================================================================

private data class PendingNoteOff(val channel: Int, val pitch: Int, val offAtMs: Long)

// ================================================================
// COMPOSABLE HELPERS
// ================================================================

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
                    onClick = { onSelected(option); expanded = false }
                )
            }
        }
    }
}

// made by Claude 08/07
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
                    onClick = { onSelected(option.name); expanded = false }
                )
            }
        }
    }
}

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
                    onClick = { onSelected(option.preset); expanded = false }
                )
            }
        }
    }
}

// ================================================================
// INSTRUMENT ROLE MATRIX
// made by Claude 10/07
// ================================================================

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
            .border(
                0.5.dp,
                MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
    ) {
        // Header row
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
            val isActive = role != InstrumentRole.OFF
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (selectedKey == def.key)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
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
                // Strum / Chord
                RoleRadio(
                    selected = role == InstrumentRole.STRUM_CHORD,
                    enabled = true,
                    modifier = Modifier.weight(1f)
                ) { onRoleChanged(def.key, InstrumentRole.STRUM_CHORD); onInstrumentSelected(def.key) }
                // Pick / Arpeggio
                RoleRadio(
                    selected = role == InstrumentRole.PICK_ARPEGGIO,
                    enabled = def.supportsPickArpeggio,
                    modifier = Modifier.weight(1f)
                ) { onRoleChanged(def.key, InstrumentRole.PICK_ARPEGGIO); onInstrumentSelected(def.key) }
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
// made by Claude 11/07: Live progression display for Jam Lab
@Composable
private fun JamLabProgressionDisplay(
    timeline: JamTimeline,
    currentBarIndex: Int,
    modifier: Modifier = Modifier
) {
    val progressionChords = remember(timeline) {
        timeline.events
            .sortedBy { it.barIndex }
            .distinctBy { it.barIndex }
            .map { it.chord }
    }

    Column(
        modifier = modifier
            .background(Color(0xFF1A1A2E), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        // Chord names row
        Row(modifier = Modifier.fillMaxWidth()) {
            progressionChords.forEachIndexed { index, chord ->
                val isActive = index == currentBarIndex
                Text(
                    text = chord.name,
                    color = if (isActive) Color(0xFF90CAF9) else Color(0xFF888899),
                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal,
                    fontSize = if (isActive) 18.sp else 14.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(color = Color(0xFF333355))
        Spacer(modifier = Modifier.height(4.dp))
        // Roman numerals row
        Row(modifier = Modifier.fillMaxWidth()) {
            progressionChords.forEachIndexed { index, chord ->
                val isActive = index == currentBarIndex
                Text(
                    text = chord.romanLabel,
                    color = if (isActive) Color.White else Color(0xFF555577),
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    fontSize = if (isActive) 13.sp else 10.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
// ================================================================
// INSTRUMENT PATTERN PANEL
// made by Claude 10/07
// ================================================================

@Composable
private fun InstrumentPatternPanel(
    instrumentKey: String,
    role: InstrumentRole,
    strumOptions: List<PresetOption>,
    selectedStrumPreset: StrumPreset,
    onStrumSelected: (StrumPreset) -> Unit,
    selectedPickingPreset: PickingPreset,
    onPickingSelected: (PickingPreset) -> Unit,
    selectedPatch: PatchOption?,
    onPatchSelected: (PatchOption) -> Unit,
    availablePatches: Map<String, List<PatchOption>>, // NEW made by Claude 08/08/2026
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
        // Panel header
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

            // Sound section — always visible
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                "Sound",
                fontSize = 10.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            // MODIFIED made by Claude 08/08/2026 — dynamic SF2 list, falls back to hardcoded if empty
            val programs = availablePatches[instrumentKey]
                ?.takeIf { it.isNotEmpty() }
                ?: (INSTRUMENT_PROGRAMS[instrumentKey] ?: emptyList())
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(programs.size) { index ->
                    val patch = programs[index]
                    val isSelected = patch.bank == selectedPatch?.bank &&
                            patch.program == selectedPatch?.program
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Bank:Program badge — NEW made by Claude 05/08/2026
                        // Format: 000:027 so patch addresses are readable during auditioning
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${patch.bank.toString().padStart(3, '0')}:${patch.program.toString().padStart(3, '0')}",
                                fontSize = 8.sp,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        if (isSelected) {
                            Button(
                                onClick = {
                                    audioEngine.changePatchOnChannel(def.channel, patch.bank, patch.program)
                                    onPatchSelected(patch)
                                },
                                modifier = Modifier.height(36.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)
                            ) {
                                Text(patch.name, fontSize = 10.sp)
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    audioEngine.changePatchOnChannel(def.channel, patch.bank, patch.program)
                                    onPatchSelected(patch)
                                },
                                modifier = Modifier.height(36.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)
                            ) {
                                Text(patch.name, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}