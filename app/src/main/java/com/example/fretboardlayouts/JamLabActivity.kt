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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fretboardlayouts.audio.GenreInstruments // NEW made by Claude 09/08/2026
import com.example.fretboardlayouts.audio.JamLabAudioEngine
import com.example.fretboardlayouts.theory.ChordOverlayMode
import com.example.fretboardlayouts.theory.Genre
import com.example.fretboardlayouts.theory.GenreChordStyles
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
import com.example.fretboardlayouts.theory.applyGenreChordStyle
import kotlinx.coroutines.delay // made by Claude 08/08/2026

// ================================================================
// TOP-LEVEL DEFINITIONS
// made by Claude 10/07: Instrument role matrix definitions
// MODIFIED made by Claude 09/08/2026: expanded to full GM group channel map
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

// Channel map (matches StyleEngine channel convention — keep in sync):
// Ch 0  Guitar      programs 24–31
// Ch 1  Bass        programs 32–39
// Ch 2  Piano       programs 0–7
// Ch 3  Organ       programs 16–23   ← NEW (09/08/2026)
// Ch 4  Strings     programs 40–47   ← shifted from 3 (09/08/2026)
// Ch 5  Ensemble    programs 48–55   ← NEW (09/08/2026)
// Ch 6  Brass       programs 56–63   ← split from old Winds (09/08/2026)
// Ch 7  Reed        programs 64–71   ← split from old Winds (09/08/2026)
// Ch 8  Pipe        programs 72–79   ← split from old Winds (09/08/2026)
// Ch 9  Drums       bank 128, fixed
// Ch 10 Synth       programs 80–95   ← NEW, SF2-aware only (09/08/2026)
// Ch 11 Ethnic      programs 104–111 ← NEW, SF2-aware only (09/08/2026)
// SKIPPED: Chromatic Perc 8-15, Synth Effects 96-103, Percussive 112-119, Sound Effects 120-127
val INSTRUMENT_DEFS = listOf(
    InstrumentDef("guitar", "Guitar", "🎸", 0, InstrumentRole.STRUM_CHORD),
    InstrumentDef("bass", "Bass", "🎸", 1, InstrumentRole.STRUM_CHORD),
    InstrumentDef(
        "drums", "Drums", "🥁", 9, InstrumentRole.STRUM_CHORD,
        supportsPickArpeggio = false, supportsHybrid = false
    ),
    InstrumentDef("piano", "Piano", "🎹", 2),
    // NEW made by Claude 09/08/2026 — split from old "Piano / Synth" and "Winds / Brass"
    InstrumentDef("organ", "Organ", "🎹", 3, supportsHybrid = false),
    InstrumentDef("strings", "Strings", "🎻", 4),  // MODIFIED channel 3→4
    InstrumentDef(
        "ensemble", "Ensemble", "🎻", 5,
        supportsPickArpeggio = false, supportsHybrid = false
    ),
    InstrumentDef("brass", "Brass", "🎺", 6, supportsHybrid = false),
    InstrumentDef("reed", "Reed", "🎷", 7, supportsHybrid = false),
    InstrumentDef("pipe", "Pipe", "🪈", 8, supportsHybrid = false),
    InstrumentDef("synth", "Synth", "🎹", 10, supportsHybrid = false),
    InstrumentDef("ethnic", "Ethnic", "🪗", 11, supportsHybrid = false)
)

// NEW made by Claude 09/08/2026
// Genre → which instrument rows are visible by default.
// SF2-aware rows (synth, ensemble) are controlled separately by SF2_ONLY_INSTRUMENTS.
val genreInstrumentVisibility: Map<Genre, Set<String>> = mapOf(
    Genre.ROCK to setOf("guitar", "bass", "piano", "strings", "drums"),
    Genre.BLUES to setOf("guitar", "bass", "piano", "organ", "brass", "reed", "drums"),
    Genre.JAZZ to setOf("guitar", "bass", "piano", "organ", "strings", "brass", "reed", "drums"),
    Genre.COUNTRY to setOf("guitar", "bass", "piano", "strings", "pipe", "ethnic", "drums"),
    Genre.FUNK to setOf("guitar", "bass", "piano", "organ", "brass", "reed", "drums"),
    Genre.DISCO to setOf("guitar", "bass", "piano", "strings", "brass", "drums"),
    Genre.SKA to setOf("guitar", "bass", "piano", "brass", "reed", "drums"),
    Genre.REGGAE to setOf("guitar", "bass", "piano", "organ", "brass", "reed", "drums")
)

// NEW made by Claude 09/08/2026
// These rows appear only when the loaded SF2 actually has patches for them — genre does not force them visible.
// Synth and Ensemble are non-essential; they self-hide on any font that lacks them.
val SF2_ONLY_INSTRUMENTS: Set<String> = setOf("synth", "ensemble")

// NEW made by Claude 05/08/2026
// PatchOption replaces bare Int program numbers — carries bank + program together
data class PatchOption(val name: String, val bank: Int, val program: Int)

// MODIFIED made by Claude 09/08/2026
// Hardcoded fallback used when SF2 returns no data.
// Groups now match the GM family split — each key maps to exactly one channel.
val INSTRUMENT_PROGRAMS = mapOf(
    "guitar" to listOf(
        PatchOption("SGM Nylon", 0, 24),
        PatchOption("Steel Sammy", 0, 25),
        PatchOption("Fluid Jazz", 0, 26),
        PatchOption("MK Clean", 0, 27),
        PatchOption("Crisis Muted", 0, 28),
        PatchOption("Arachno OD", 0, 29),
        PatchOption("MK Jazz", 1, 26),
        PatchOption("GS Chorused Cln", 1, 27),
        PatchOption("Muted Metal", 1, 28),
        PatchOption("Strix Shadowed", 2, 27),
        PatchOption("Strix Brt Chorus", 3, 27)
    ),
    "bass" to listOf(
        PatchOption("Crisis Acoustic", 0, 32),
        PatchOption("Crisis Finger", 0, 33),
        PatchOption("Crisis Pick", 0, 34),
        PatchOption("Crisis Fretless", 0, 35),
        PatchOption("Crisis Slap", 0, 36),
        PatchOption("Fluid Pop", 0, 37),
        PatchOption("GS Synth 1", 0, 38),
        PatchOption("GS Synth 2", 0, 39),
        PatchOption("Arachno Finger", 1, 33)
    ),
    "drums" to listOf(
        PatchOption("Standard", 0, 0),
        PatchOption("Room", 0, 8),
        PatchOption("Power", 0, 16),
        PatchOption("TR-808", 0, 25),
        PatchOption("Jazz", 0, 32),
        PatchOption("Brush", 0, 40),
        PatchOption("Orchestra", 0, 48)
    ),
    // Programs 0–7: GM Piano family only (organs now in own group)
    "piano" to listOf(
        PatchOption("Grand Piano", 0, 0),
        PatchOption("Bright Piano", 0, 1),
        PatchOption("Electric Grand", 0, 2),
        PatchOption("Honky-Tonk", 0, 3),
        PatchOption("Elec Piano 1", 0, 4),
        PatchOption("Elec Piano 2", 0, 5),
        PatchOption("Harpsichord", 0, 6),
        PatchOption("Clavinet", 0, 7)
    ),
    // NEW made by Claude 09/08/2026 — Programs 16–23: GM Organ family
    "organ" to listOf(
        PatchOption("Drawbar Organ", 0, 16),
        PatchOption("Percussive Org", 0, 17),
        PatchOption("Rock Organ", 0, 18),
        PatchOption("Church Organ", 0, 19),
        PatchOption("Reed Organ", 0, 20),
        PatchOption("Accordion", 0, 21),
        PatchOption("Harmonica", 0, 22),
        PatchOption("Tango Accord", 0, 23)
    ),
    // Programs 40–47: GM Strings family (ensemble 48-55 now in own group)
    "strings" to listOf(
        PatchOption("Violin", 0, 40),
        PatchOption("Viola", 0, 41),
        PatchOption("Cello", 0, 42),
        PatchOption("Contrabass", 0, 43),
        PatchOption("Tremolo", 0, 44),
        PatchOption("Pizzicato", 0, 45),
        PatchOption("Harp", 0, 46),
        PatchOption("Timpani", 0, 47)
    ),
    // NEW made by Claude 09/08/2026 — Programs 48–55: GM Ensemble family
    "ensemble" to listOf(
        PatchOption("String Ens 1", 0, 48),
        PatchOption("String Ens 2", 0, 49),
        PatchOption("Synth Str 1", 0, 50),
        PatchOption("Synth Str 2", 0, 51),
        PatchOption("Choir Aahs", 0, 52),
        PatchOption("Voice Oohs", 0, 53),
        PatchOption("Synth Voice", 0, 54),
        PatchOption("Orchestra Hit", 0, 55)
    ),
    // NEW made by Claude 09/08/2026 — Programs 56–63: GM Brass family
    "brass" to listOf(
        PatchOption("Trumpet", 0, 56),
        PatchOption("Trombone", 0, 57),
        PatchOption("Tuba", 0, 58),
        PatchOption("Muted Trumpet", 0, 59),
        PatchOption("French Horn", 0, 60),
        PatchOption("Brass Section", 0, 61),
        PatchOption("Synth Brass 1", 0, 62),
        PatchOption("Synth Brass 2", 0, 63)
    ),
    // NEW made by Claude 09/08/2026 — Programs 64–71: GM Reed family
    "reed" to listOf(
        PatchOption("Soprano Sax", 0, 64),
        PatchOption("Alto Sax", 0, 65),
        PatchOption("Tenor Sax", 0, 66),
        PatchOption("Baritone Sax", 0, 67),
        PatchOption("Oboe", 0, 68),
        PatchOption("English Horn", 0, 69),
        PatchOption("Bassoon", 0, 70),
        PatchOption("Clarinet", 0, 71)
    ),
    // NEW made by Claude 09/08/2026 — Programs 72–79: GM Pipe family
    "pipe" to listOf(
        PatchOption("Piccolo", 0, 72),
        PatchOption("Flute", 0, 73),
        PatchOption("Recorder", 0, 74),
        PatchOption("Pan Flute", 0, 75),
        PatchOption("Blown Bottle", 0, 76),
        PatchOption("Shakuhachi", 0, 77),
        PatchOption("Whistle", 0, 78),
        PatchOption("Ocarina", 0, 79)
    ),
    // NEW made by Claude 09/08/2026 — Programs 80–95: GM Synth Lead + Synth Pad families
    "synth" to listOf(
        PatchOption("Sq Lead", 0, 80),
        PatchOption("Saw Lead", 0, 81),
        PatchOption("Calliope", 0, 82),
        PatchOption("Chiff Lead", 0, 83),
        PatchOption("Charang", 0, 84),
        PatchOption("Voice Lead", 0, 85),
        PatchOption("Fifths Lead", 0, 86),
        PatchOption("Bass+Lead", 0, 87),
        PatchOption("New Age Pad", 0, 88),
        PatchOption("Warm Pad", 0, 89),
        PatchOption("Polysynth", 0, 90),
        PatchOption("Choir Pad", 0, 91),
        PatchOption("Bowed Pad", 0, 92),
        PatchOption("Metallic Pad", 0, 93),
        PatchOption("Halo Pad", 0, 94),
        PatchOption("Sweep Pad", 0, 95)
    ),
    // NEW made by Claude 09/08/2026 — Programs 104–111: GM Ethnic family
    "ethnic" to listOf(
        PatchOption("Sitar", 0, 104),
        PatchOption("Banjo", 0, 105),
        PatchOption("Shamisen", 0, 106),
        PatchOption("Koto", 0, 107),
        PatchOption("Kalimba", 0, 108),
        PatchOption("Bagpipe", 0, 109),
        PatchOption("Fiddle", 0, 110),
        PatchOption("Shanai", 0, 111)
    )
)

// MODIFIED made by Claude 09/08/2026
// Filter ranges now match the per-GM-family channel split.
// Piano = 0-7 only (organs separated). Strings = 40-47 only (ensemble separated).
// Winds split into brass (56-63), reed (64-71), pipe (72-79).
// Programs 8-15 (Chromatic Perc), 96-103 (Synth Effects),
// 112-119 (Percussive), 120-127 (Sound Effects) are intentionally excluded.
fun parsePresetsFromSF2(raw: String): Map<String, List<PatchOption>> {
    if (raw.isEmpty()) return emptyMap()
    val all = raw.split("|").mapNotNull { entry ->
        val parts = entry.split(":", limit = 3)
        if (parts.size < 3) null
        else {
            val bank = parts[0].toIntOrNull() ?: return@mapNotNull null
            val program = parts[1].toIntOrNull() ?: return@mapNotNull null
            val name = parts[2].trim().ifEmpty { "${bank}:${program}" }
            PatchOption(name, bank, program)
        }
    }
    // MODIFIED made by Claude 09/08/2026 — also exclude bank 120 (used for
    // percussion/SFX in some soundfonts e.g. GeneralUser GS) so drum patches
    // don't bleed into melodic instrument slots like piano (0-7 range)
    val melodic = all.filter { it.bank !in listOf(120, 127, 128) }
    return mapOf(
        "guitar" to melodic.filter { it.program in 24..31 },
        "bass" to melodic.filter { it.program in 32..39 },
        "piano" to melodic.filter { it.program in 0..7 },
        "organ" to melodic.filter { it.program in 16..23 },
        "strings" to melodic.filter { it.program in 40..47 },
        "ensemble" to melodic.filter { it.program in 48..55 },
        "brass" to melodic.filter { it.program in 56..63 },
        "reed" to melodic.filter { it.program in 64..71 },
        "pipe" to melodic.filter { it.program in 72..79 },
        "synth" to melodic.filter { it.program in 80..95 },
        "ethnic" to melodic.filter { it.program in 104..111 },
        "drums" to all.filter { it.bank == 128 }
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
    // made by Claude 08/08/2026
    // Keeps CPU running when screen turns off so audio continues during jamming
    private lateinit var wakeLock: android.os.PowerManager.WakeLock

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val powerManager = getSystemService(POWER_SERVICE) as android.os.PowerManager
        wakeLock = powerManager.newWakeLock(
            android.os.PowerManager.PARTIAL_WAKE_LOCK,
            "LetsJam::JamLabAudioWakeLock"
        )
        wakeLock.acquire(4 * 60 * 60 * 1000L) // 4 hour max
        setContent {
            FretboardLayoutsTheme {
                JamLabScreen()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
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
    var currentBarIndex by remember { mutableStateOf(0) }                      // made by Claude 11/07

    var currentGenreChordStyle by remember(currentGenre) { // NEW made by Claude 17/08/2026
        mutableStateOf(GenreChordStyles.defaultFor(currentGenre)) // NEW
    } // NEW

    val currentKeyObj = remember(currentKey) { MusicKey.fromString(currentKey) }
    val progressionOptions = remember(currentKeyObj) {
        buildProgressionOptions(currentKeyObj)
    }

    // Chord data for Music Dashboard — resolved from current key + progression
    // so the dashboard updates live as the user changes selections, before Generate is hit.
    // NEW made by Claude 17/08/2026
    val dashboardChords =
        remember(currentKeyObj, currentProgression, currentGenreChordStyle, currentTimeSignature) {
            val slots = Progressions.ALL[currentProgression] ?: Progressions.ALL.values.first()
            val styled = applyGenreChordStyle(slots, currentGenreChordStyle)
            buildJamTimeline(
                key = currentKeyObj,
                progressionSlots = styled,
                scaleType = ScaleType.FULL,
                chordOverlayMode = ChordOverlayMode.ALL_CHORD_TONES,
                tempoBpm = 100,
                timeSignature = currentTimeSignature
            ).events
                .sortedBy { it.barIndex }
                .distinctBy { it.barIndex }
                .map { it.chord.name to it.chord.romanLabel }
        }

    // Application-scope session — feeds the shared Music Dashboard
    // NEW made by Claude 18/08/2026
    val app = LocalContext.current.applicationContext as FretboardLayoutsApplication
    // Skip the very first fire (first render) so we don't overwrite session with
    // Jam Lab's local defaults when the user navigates here from LoopBuilder.
    // MODIFIED made by Claude 18/08/2026
    var jamLabSessionPushed by remember { mutableStateOf(false) }
    LaunchedEffect(currentKeyObj, currentProgression, currentGenreChordStyle, currentTimeSignature, currentTempo, currentGenre) {
        if (jamLabSessionPushed) {
            app.session.updateDashboard(
                DashboardState(
                    chordNames = dashboardChords.map { it.first },
                    numerals = dashboardChords.map { it.second },
                    keyLabel = currentKey,
                    timeSignature = currentTimeSignature,
                    tempo = currentTempo,
                    genre = currentGenre
                )
            )
        } else {
            jamLabSessionPushed = true
        }
    }

    // Auto-select first valid progression when key modality changes
    LaunchedEffect(currentKeyObj) {
        val currentValid =
            progressionOptions.find { it.name == currentProgression }?.enabled ?: false
        if (!currentValid) {
            progressionOptions.firstOrNull { it.enabled }?.let { currentProgression = it.name }
        }
    }

    var selectedPatchByChannel by remember { mutableStateOf(mapOf<Int, PatchOption>()) }

    // NEW made by Claude 09/08/2026
    // On genre change: reset panel selection if instrument is now hidden, auto-load
    // genre patches on the audio engine, and sync selectedPatchByChannel so the UI
    // reflects the new patches without the user having to tap anything manually.
    var selectedInstrumentKey by remember { mutableStateOf("guitar") }
    LaunchedEffect(currentGenre) {
        // 1. Reset panel if selected instrument is no longer visible
        val genreVisible = genreInstrumentVisibility[currentGenre]
            ?: INSTRUMENT_DEFS.map { it.key }.toSet()
        val isVisible = when (selectedInstrumentKey) {
            in SF2_ONLY_INSTRUMENTS -> availablePatches[selectedInstrumentKey]?.isNotEmpty() == true
            else -> selectedInstrumentKey in genreVisible
        }
        if (!isVisible) selectedInstrumentKey = "guitar"

        // 2. Fire patch changes on the audio engine for all genre-appropriate channels
        audioEngine.loadGenrePatches(currentGenre)

        // 3. Sync selectedPatchByChannel so the UI shows the correct patch selection.
        //    Looks up each channel's new program in availablePatches (SF2) or the
        //    hardcoded fallback, bank 0 only (all GenreInstrumentation programs are bank 0).
        val g = GenreInstruments.forGenre(currentGenre)
        val newPatches = mutableMapOf<Int, PatchOption>()
        listOf(
            Triple(0, "guitar", g.guitarProgram),
            Triple(1, "bass", g.bassProgram),
            Triple(3, "organ", g.organProgram),
            Triple(4, "strings", g.stringsProgram),
            Triple(5, "ensemble", g.ensembleProgram),
            Triple(6, "brass", g.brassProgram),
            Triple(7, "reed", g.reedProgram),
            Triple(8, "pipe", g.pipeProgram),
            Triple(9, "drums", g.drumKitProgram),
            Triple(10, "synth", g.synthProgram),
            Triple(11, "ethnic", g.ethnicProgram)
        ).forEach { (channel, key, program) ->
            if (program == -1) return@forEach
            val patches = availablePatches[key]?.takeIf { it.isNotEmpty() }
                ?: INSTRUMENT_PROGRAMS[key] ?: return@forEach
            patches.firstOrNull { it.bank == 0 && it.program == program }
                ?.let { newPatches[channel] = it }
        }
        selectedPatchByChannel = newPatches
    }

    val strumPatternOptions = remember(currentGenre, customStrumMode, currentTimeSignature) {
        buildPresetOptions(allGuitarPresets, currentGenre, currentTimeSignature, customStrumMode)
    }

    var isPlaying by remember { mutableStateOf(false) }
    var showGeneratingMessage by remember { mutableStateOf(false) }
    var currentTimeline by remember { mutableStateOf<JamTimeline?>(null) }

    // made by Claude 10/07: Instrument role state
    var instrumentRoles by remember {
        mutableStateOf(INSTRUMENT_DEFS.associate { it.key to it.defaultRole })
    }

    // NEW made by Claude 09/08/2026 — per-genre channel volume mixer
    // Stored per-genre so switching Blues→Rock→Blues restores the Blues mix.
    // Default 1.0f = use StyleEngine's built-in channelVolumeScale as-is.
    var channelVolumeByGenre by remember {
        mutableStateOf(
            Genre.values().associate { g ->
                g to INSTRUMENT_DEFS.associate { it.channel to 1.0f }
            }
        )
    }
    var showVolumeMixer by remember { mutableStateOf(false) }
    val currentChannelVolume = channelVolumeByGenre[currentGenre]
        ?: INSTRUMENT_DEFS.associate { it.channel to 1.0f }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Sticky: header + dashboard (never scrolls) ───────────
        Text(
            "🧪 Jam Lab — Sound Sandbox",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
        )
        // 📺 Music Dashboard — MODIFIED made by Claude 17/08/2026
        MusicDashboard(
            state = app.session.dashboard,
            activeChordIndex = if (isPlaying) currentBarIndex else -1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp)
        )

        // ── Scrollable body (everything below the dashboard) ─────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {

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

            // NEW made by Claude 17/08/2026 — chord style selector
            Text("Chord Style", style = MaterialTheme.typography.labelSmall)
            val chordStyleOptions =
                remember(currentGenre) { GenreChordStyles.stylesFor(currentGenre) }
            SimpleDropdown(
                selected = currentGenreChordStyle.displayName,
                options = chordStyleOptions.map { it.displayName },
                onSelected = { name ->
                    currentGenreChordStyle = chordStyleOptions.find { it.displayName == name }
                        ?: GenreChordStyles.defaultFor(currentGenre)
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
                        val styledProgression =
                            applyGenreChordStyle(progression, currentGenreChordStyle) // NEW
                        val timeline = buildJamTimeline(
                            key = key,
                            progressionSlots = styledProgression, // MODIFIED — was `progression`
                            scaleType = currentScale,
                            chordOverlayMode = ChordOverlayMode.ALL_CHORD_TONES,
                            tempoBpm = currentTempo,
                            timeSignature = currentTimeSignature
                        )
                        currentTimeline = timeline
                        showGeneratingMessage = false
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Generate & Play") }

                Button(
                    onClick = {
                        isPlaying = false
                        audioEngine.stopAudio()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = currentTimeline != null
                ) { Text("Stop") }

                OutlinedButton(
                    onClick = {
                        isPlaying = false
                        audioEngine.stopAudio()
                        currentTimeline = null
                        showGeneratingMessage = false
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Reset") }
            }

            if (showGeneratingMessage) {
                Text(
                    "Generating...",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // ══ INSTRUMENT MATRIX ══
            // made by Claude 10/07, expanded 09/08/2026 for full GM group map
            // NEW made by Claude 09/08/2026 — Mix button opens per-genre volume popup
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Instruments",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = { showVolumeMixer = true },
                    modifier = Modifier.height(32.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 10.dp,
                        vertical = 4.dp
                    )
                ) {
                    Text("🎚 Mix", fontSize = 11.sp)
                }
            }
            if (showVolumeMixer) {
                VolumeMixerPopup(
                    genre = currentGenre,
                    availablePatches = availablePatches,
                    channelVolume = currentChannelVolume,
                    onVolumeChanged = { channel, value ->
                        channelVolumeByGenre = channelVolumeByGenre + (currentGenre to
                                currentChannelVolume + (channel to value))
                    },
                    onReset = {
                        channelVolumeByGenre = channelVolumeByGenre + (currentGenre to
                                INSTRUMENT_DEFS.associate { it.channel to 1.0f })
                    },
                    onDismiss = { showVolumeMixer = false }
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            InstrumentRoleMatrix(
                instrumentRoles = instrumentRoles,
                selectedKey = selectedInstrumentKey,
                genre = currentGenre,                     // NEW made by Claude 09/08/2026
                availablePatches = availablePatches,      // NEW made by Claude 09/08/2026
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
                    availablePatches = availablePatches,
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
                    instrumentRoles = instrumentRoles,
                    humanisationLevel = currentHumanisation,
                    channelVolume = currentChannelVolume,   // NEW made by Claude 09/08/2026
                    onBarChanged = { currentBarIndex = it }
                )
            }
        } // end scrollable Column
    } // end outer Column
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
    humanisationLevel: HumanisationLevel = HumanisationLevel.OFF,
    channelVolume: Map<Int, Float> = emptyMap(),   // NEW made by Claude 09/08/2026
    onBarChanged: (Int) -> Unit                    // made by Claude 11/07
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
    val pendingNoteOffs = remember { mutableListOf<PendingNoteOff>() }
    val lastBarIndexRef = remember { intArrayOf(-1) }

    // NEW made by Claude 09/08/2026 — channelVolume is a remember key so moving a
    // slider recomputes velocities immediately without restarting the playback loop.
    // StyleEngine's internal channelVolumeScale is applied first; user's slider
    // multiplier is applied on top as a second pass here.
    val backingTrackEvents = remember(
        timeline, genre, preset, pickingPreset, humanisationLevel, instrumentRoles, channelVolume
    ) {
        com.example.fretboardlayouts.audio.StyleEngine.generateAccompaniment(
            timeline, genre, preset, pickingPreset, humanisationLevel, instrumentRoles
        ).map { event ->
            event.copy(
                velocity = (event.velocity * (channelVolume[event.channel] ?: 1.0f))
                    .toInt().coerceIn(1, 127)
            )
        }
    }

    LaunchedEffect(Unit) {
        // MODIFIED made by Claude 08/08/2026
        // System.currentTimeMillis() + delay() keeps running with screen off
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
                            pendingNoteOffs.add(
                                PendingNoteOff(
                                    event.channel,
                                    event.pitch,
                                    currentTimeMs + event.durationMs
                                )
                            )
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
                        pendingNoteOffs.add(
                            PendingNoteOff(
                                event.channel,
                                event.pitch,
                                currentTimeMs + event.durationMs
                            )
                        )
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
                    pendingNoteOffs.add(
                        PendingNoteOff(event.channel, event.pitch, currentTimeMs + event.durationMs)
                    )
                }
            lastSequencerLoopTime = loopTime

            if (pendingNoteOffs.isNotEmpty()) {
                val dueOffs = pendingNoteOffs.filter { it.offAtMs <= currentTimeMs }
                if (dueOffs.isNotEmpty()) {
                    dueOffs.forEach { audioEngine.noteOff(it.channel, it.pitch) }
                    pendingNoteOffs.removeAll(dueOffs.toSet())
                }
            }

            // NEW made by Claude 09/08/2026
            // Immediately silence channels that just went inactive — prevents sustained
            // pads (strings, ensemble, organ) from ringing out when the user turns
            // them OFF. Runs every 8ms so cut-off is imperceptible.
            val inactiveNoteOffs = pendingNoteOffs.filter { it.channel !in activeChannels }
            if (inactiveNoteOffs.isNotEmpty()) {
                inactiveNoteOffs.forEach { audioEngine.noteOff(it.channel, it.pitch) }
                pendingNoteOffs.removeAll(inactiveNoteOffs.toSet())
            }

            delay(8L) // ~120 polls/sec, runs with screen off
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
// MODIFIED made by Claude 09/08/2026: genre-aware + SF2-aware row visibility
// ================================================================
@Composable
private fun InstrumentRoleMatrix(
    instrumentRoles: Map<String, InstrumentRole>,
    selectedKey: String,
    genre: Genre,                                       // NEW made by Claude 09/08/2026
    availablePatches: Map<String, List<PatchOption>>,   // NEW made by Claude 09/08/2026
    onRoleChanged: (String, InstrumentRole) -> Unit,
    onInstrumentSelected: (String) -> Unit
) {
    // NEW made by Claude 09/08/2026
    // Determine which rows to show:
    //   - SF2_ONLY_INSTRUMENTS: visible only if the loaded SF2 has patches for them
    //   - All others: visible if the genre includes them, AND (SF2 has patches OR we're
    //     in fallback mode — availablePatches empty means SF2 returned nothing)
    val isInFallbackMode = availablePatches.isEmpty()
    val genreVisible = genreInstrumentVisibility[genre]
        ?: INSTRUMENT_DEFS.map { it.key }.toSet()
    val visibleDefs = INSTRUMENT_DEFS.filter { def ->
        val hasSF2Patches = availablePatches[def.key]?.isNotEmpty() == true
        when (def.key) {
            in SF2_ONLY_INSTRUMENTS -> hasSF2Patches
            else -> def.key in genreVisible && (isInFallbackMode || hasSF2Patches)
        }
    }

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

        visibleDefs.forEach { def ->
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
                ) {
                    onRoleChanged(
                        def.key,
                        InstrumentRole.STRUM_CHORD
                    ); onInstrumentSelected(def.key)
                }
                // Pick / Arpeggio
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
// MODIFIED made by Claude 09/08/2026: updated role labels for expanded instrument set
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
    availablePatches: Map<String, List<PatchOption>>,
    audioEngine: JamLabAudioEngine
) {
    val def = INSTRUMENT_DEFS.find { it.key == instrumentKey } ?: return

    // MODIFIED made by Claude 09/08/2026 — updated for full instrument set
    // Guitar/bass: "strum" / "picking" (hands on strings)
    // Keyboard/string instruments: "chord" / "arpeggio" (what the notes form)
    // Wind/single-note instruments: "chord" / "melody" (what they play)
    val chordInstruments = setOf("piano", "organ", "strings", "ensemble", "synth")
    val melodicInstruments = setOf("brass", "reed", "pipe", "ethnic")
    val roleLabel = when (role) {
        InstrumentRole.OFF -> "off"
        InstrumentRole.STRUM_CHORD -> when (instrumentKey) {
            in chordInstruments -> "chord patterns"
            in melodicInstruments -> "line patterns"
            else -> "strum patterns"   // guitar, bass
        }

        InstrumentRole.PICK_ARPEGGIO -> when (instrumentKey) {
            in chordInstruments -> "arpeggio patterns"
            in melodicInstruments -> "melody patterns"
            else -> "picking patterns"  // guitar, bass
        }

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

            // Dynamic SF2 list, falls back to hardcoded if empty
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
                        // Bank:Program badge — format 000:027 for auditioning
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
                                text = "${
                                    patch.bank.toString().padStart(3, '0')
                                }:${patch.program.toString().padStart(3, '0')}",
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
                                    audioEngine.changePatchOnChannel(
                                        def.channel,
                                        patch.bank,
                                        patch.program
                                    )
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
                                    audioEngine.changePatchOnChannel(
                                        def.channel,
                                        patch.bank,
                                        patch.program
                                    )
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

// ================================================================
// VOLUME MIXER POPUP
// NEW made by Claude 09/08/2026
// Shows one slider per visible instrument for the current genre.
// Sliders are additional multipliers on top of StyleEngine's internal
// channelVolumeScale — 100% = "use StyleEngine's default as-is".
// Settings are stored per-genre in JamLabScreen so switching genres
// preserves each genre's individual mix.
// ================================================================
@Composable
private fun VolumeMixerPopup(
    genre: Genre,
    availablePatches: Map<String, List<PatchOption>>,
    channelVolume: Map<Int, Float>,
    onVolumeChanged: (channel: Int, value: Float) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    // Same visibility logic as InstrumentRoleMatrix — only show relevant rows
    val isInFallbackMode = availablePatches.isEmpty()
    val genreVisible = genreInstrumentVisibility[genre]
        ?: INSTRUMENT_DEFS.map { it.key }.toSet()
    val visibleDefs = INSTRUMENT_DEFS.filter { def ->
        val hasSF2Patches = availablePatches[def.key]?.isNotEmpty() == true
        when (def.key) {
            in SF2_ONLY_INSTRUMENTS -> hasSF2Patches
            else -> def.key in genreVisible && (isInFallbackMode || hasSF2Patches)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "🎚 Channel Mix — ${genre.displayName}",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "100% = genre default. Adjustments are remembered per genre.",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                visibleDefs.forEach { def ->
                    val volume = channelVolume[def.channel] ?: 1.0f
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${def.emoji} ${def.displayName}",
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${(volume * 100).roundToInt()}%",
                                fontSize = 11.sp,
                                color = if (volume != 1.0f)
                                    MaterialTheme.colorScheme.primary
                                else
                                    Color.Gray,
                                fontWeight = if (volume != 1.0f) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.width(42.dp),
                                textAlign = TextAlign.End
                            )
                        }
                        Slider(
                            value = volume,
                            onValueChange = { onVolumeChanged(def.channel, it) },
                            valueRange = 0f..1.5f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        },
        dismissButton = {
            OutlinedButton(onClick = onReset) { Text("Reset genre") }
        }
    )
}