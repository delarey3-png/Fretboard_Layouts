package com.example.fretboardlayouts
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fretboardlayouts.theory.ChordOverlayMode
import com.example.fretboardlayouts.theory.ChordTonePosition
import com.example.fretboardlayouts.theory.FretboardPosition
import com.example.fretboardlayouts.theory.Genre
import com.example.fretboardlayouts.theory.JamTimeline
import com.example.fretboardlayouts.theory.MusicKey
import com.example.fretboardlayouts.theory.NOTE_NAMES
import com.example.fretboardlayouts.theory.PresetOption
import com.example.fretboardlayouts.theory.Progressions
import com.example.fretboardlayouts.theory.ScaleType
import com.example.fretboardlayouts.theory.StrumPreset
import com.example.fretboardlayouts.theory.TimeSignature
import com.example.fretboardlayouts.theory.allGuitarPresets
import com.example.fretboardlayouts.theory.buildJamTimeline
import com.example.fretboardlayouts.theory.buildPresetOptions
import com.example.fretboardlayouts.ui.theme.FretboardLayoutsTheme
import kotlin.math.pow
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.res.painterResource
import com.example.fretboardlayouts.theory.ProgressionOption
import com.example.fretboardlayouts.theory.buildProgressionOptions
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: MainViewModel = viewModel()
            val state by viewModel.currentScreenState
            val audioStatus = remember { mutableStateOf("Initializing...") }
            LaunchedEffect(Unit) {
                while (true) {
                    audioStatus.value = viewModel.midiPlayerStatus()
                    kotlinx.coroutines.delay(500)
                }
            }
            FretboardLayoutsTheme {
                when (val current = state) {
                    is AppState.Setup -> SetupScreen(
                        selectedKey = viewModel.selectedKey.value,
                        onKeySelected = { viewModel.selectedKey.value = it },
                        selectedTimeSignature = viewModel.selectedTimeSignature.value,
                        onTimeSignatureSelected = {
                            viewModel.selectedTimeSignature.value = it
                        },
                        selectedProgression = viewModel.selectedProgression.value,
                        onProgressionSelected = { viewModel.selectedProgression.value = it },
                        selectedScaleOverlay = viewModel.selectedScaleOverlay.value,
                        onScaleOverlaySelected = { viewModel.selectedScaleOverlay.value = it },
                        selectedChordMode = viewModel.selectedChordMode.value,
                        onChordModeSelected = { viewModel.selectedChordMode.value = it },
                        selectedGenre = viewModel.selectedGenre.value,
                        onGenreSelected = { viewModel.selectedGenre.value = it },
                        selectedGuitarPreset = viewModel.selectedGuitarPreset.value,
                        onGuitarPresetSelected = { viewModel.selectedGuitarPreset.value = it },
                        customStrumMode = viewModel.customStrumMode.value,
                        onCustomStrumModeToggled = { viewModel.customStrumMode.value = it },
                        selectedTempo = viewModel.selectedTempo.intValue,
                        onTempoSelected = { viewModel.selectedTempo.intValue = it },
                        audioStatus = audioStatus.value,
                        onJamClick = { viewModel.startGeneratingTrack() }
                    )
                    is AppState.Loading -> LoadingScreen(message = current.message)
                    is AppState.Playback -> PlaybackScreen(
                        timeline = current.timeline,
                        currentChordIndex = viewModel.currentChordIndex.value, // NEW
                        onStopAudio = { viewModel.stopAudio() },
                        onBackClick = { viewModel.resetToSetup() },
                        liveScaleOverlay = viewModel.liveScaleOverlay,
                        liveChordToneOverlay = viewModel.liveChordToneOverlay,
                        scaleOverlayVisible = viewModel.scaleOverlayVisible.value,
                        chordOverlayVisible = viewModel.chordOverlayVisible.value,
                        liveScaleType = viewModel.liveScaleType.value,
                        onScaleTypeChanged = { viewModel.liveScaleType.value = it },
                        onScaleOverlayToggled = { viewModel.scaleOverlayVisible.value = it },
                        onChordOverlayToggled = { viewModel.chordOverlayVisible.value = it }
                    )
                }
            }
        }
    }
}
// ================================================================
// SETUP SCREEN
// ================================================================
@Composable
fun SetupScreen(
    selectedKey: String,
    onKeySelected: (String) -> Unit,
    selectedTimeSignature: TimeSignature,
    onTimeSignatureSelected: (TimeSignature) -> Unit,
    selectedProgression: String,
    onProgressionSelected: (String) -> Unit,
    selectedScaleOverlay: ScaleType,
    onScaleOverlaySelected: (ScaleType) -> Unit,
    selectedChordMode: ChordOverlayMode,
    onChordModeSelected: (ChordOverlayMode) -> Unit,
    selectedGenre: Genre,
    onGenreSelected: (Genre) -> Unit,
    selectedGuitarPreset: StrumPreset?,
    onGuitarPresetSelected: (StrumPreset) -> Unit,
    customStrumMode: Boolean,
    onCustomStrumModeToggled: (Boolean) -> Unit,
    selectedTempo: Int,
    onTempoSelected: (Int) -> Unit,
    audioStatus: String,
    onJamClick: () -> Unit
) {
    val context = LocalContext.current // NEW
    // made by Gemini 27/06: Get progression options based on modality
    val currentKey = remember(selectedKey) { MusicKey.fromString(selectedKey) }
    val progressionOptions = remember(currentKey) {
        com.example.fretboardlayouts.theory.buildProgressionOptions(currentKey)
    }
    // made by Gemini 27/06: Auto-select first valid progression if current one becomes invalid
    LaunchedEffect(currentKey) {
        val currentValid =
            progressionOptions.find { it.name == selectedProgression }?.enabled ?: false
        if (!currentValid) {
            val firstValid = progressionOptions.firstOrNull { it.enabled }
            if (firstValid != null) onProgressionSelected(firstValid.name)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Setup Your Fretboard", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "Audio: $audioStatus",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                SetupDropdown(
                    label = "Key",
                    selected = selectedKey,
                    options = NOTE_NAMES.flatMap { listOf("$it Major", "$it Minor") },
                    onSelected = onKeySelected
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                SetupDropdown(
                    label = "Genre",
                    selected = selectedGenre.displayName,
                    options = Genre.values().map { it.displayName },
                    onSelected = { name ->
                        onGenreSelected(Genre.values().find { it.displayName == name }
                            ?: Genre.ROCK)
                    }
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                SetupDropdown(
                    label = "Time Signature",
                    selected = selectedTimeSignature.display,
                    options = TimeSignature.values().map { it.display },
                    onSelected = { display ->
                        onTimeSignatureSelected(
                            TimeSignature.values().find { it.display == display }
                                ?: TimeSignature.FOUR_FOUR
                        )
                    }
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                PresetDropdown(
                    label = "Strum Pattern",
                    options = buildPresetOptions(
                        allGuitarPresets, selectedGenre, selectedTimeSignature, customStrumMode
                    ),
                    selectedName = selectedGuitarPreset?.name ?: "Default",
                    onSelected = onGuitarPresetSelected
                )
            }
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
            Switch(checked = customStrumMode, onCheckedChange = onCustomStrumModeToggled)
        }
        // made by Gemini 27/06: Context-aware progression dropdown
        ProgressionDropdown(
            label = "Progression",
            selected = selectedProgression,
            options = progressionOptions,
            onSelected = onProgressionSelected
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                SetupDropdown(
                    label = "Scale Overlay",
                    selected = selectedScaleOverlay.name,
                    options = ScaleType.values().map { it.name },
                    onSelected = { onScaleOverlaySelected(ScaleType.valueOf(it)) }
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                SetupDropdown(
                    label = "Chord Display",
                    selected = selectedChordMode.name,
                    options = ChordOverlayMode.values().map { it.name },
                    onSelected = { onChordModeSelected(ChordOverlayMode.valueOf(it)) }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Tempo: $selectedTempo BPM",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            androidx.compose.material3.Slider(
                value = selectedTempo.toFloat(),
                onValueChange = { onTempoSelected(it.roundToInt()) },
                valueRange = 60f..200f,
                steps = 140
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onJamClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4D00)),
            contentPadding = PaddingValues(0.dp) // Removes default button padding so the logo can fill the height nicely
        ) {
            Image(
                painter = painterResource(id = R.drawable.lets_jam_logo),
                contentDescription = "Let's Jam!",
                modifier = Modifier
                    .fillMaxHeight()
                    // ButtonDefaults.shape ensures the image clips perfectly to the button's rounded corners
                    .clip(ButtonDefaults.shape)
                    .padding(vertical = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp)) // NEW
        Button( // NEW
            onClick = { // NEW
                context.startActivity(Intent(context, com.example.fretboardlayouts.JamLabActivity::class.java)) // NEW
            }, // NEW
            modifier = Modifier.fillMaxWidth().height(56.dp) // NEW Delarey 08/07 changed Let's Jam! Button size
        ) { // NEW
            Text("\uD83E\uDDEA Jam Lab", style = MaterialTheme.typography.titleLarge) // NEW Delarey 08/07 button colour change attempt
        } // NEW
    }
}
@Composable
fun ProgressionDropdown(
    label: String,
    selected: String,
    options: List<ProgressionOption>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
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
            Text(selected, style = MaterialTheme.typography.bodyLarge)
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option.name,
                                color = if (option.enabled) Color.Unspecified else Color.Gray
                            )
                        },
                        enabled = option.enabled,
                        onClick = { onSelected(option.name); expanded = false }
                    )
                }
            }
        }
        HorizontalDivider()
    }
}
@Composable
fun SetupDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
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
            Text(selected, style = MaterialTheme.typography.bodyLarge)
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = { onSelected(option); expanded = false }
                    )
                }
            }
        }
        HorizontalDivider()
    }
}
@Composable
fun PresetDropdown(
    label: String,
    options: List<PresetOption>,
    selectedName: String,
    onSelected: (StrumPreset) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
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
@Composable
fun LoadingScreen(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(message, textAlign = TextAlign.Center)
    }
}
// ================================================================
// PLAYBACK SCREEN
// ================================================================
@Composable
fun PlaybackScreen(
    timeline: JamTimeline,
    currentChordIndex: Int, // NEW — chord tracking moved to ViewModel; no more withFrameMillis here
    onStopAudio: () -> Unit,
    onBackClick: () -> Unit,
    liveScaleOverlay: List<FretboardPosition>,
    liveChordToneOverlay: List<ChordTonePosition>,
    scaleOverlayVisible: Boolean,
    chordOverlayVisible: Boolean,
    liveScaleType: ScaleType,
    onScaleTypeChanged: (ScaleType) -> Unit,
    onScaleOverlayToggled: (Boolean) -> Unit,
    onChordOverlayToggled: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showTheoryGrid by remember { mutableStateOf(false) }
    DisposableEffect(key1 = Unit) {
        val activity = context.findActivity()
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        val window = activity?.window
        val insetsController = window?.let {
            androidx.core.view.WindowCompat.getInsetsController(it, it.decorView)
        }
        insetsController?.let {
            it.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            it.systemBarsBehavior =
                androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose {
            onStopAudio()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            insetsController?.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
    }
    // MODIFIED — currentEvent now comes from the ViewModel's chord index,
    // which is updated by the 8ms loop in MainViewModel.startPlaybackLoop().
    // withFrameMillis and elapsedTime are gone; audio continues when screen is off.
    val currentEvent = timeline.events.getOrNull(currentChordIndex) ?: timeline.events.first() // NEW
    val chordColor = when (currentEvent.chord.degree) {
        1 -> Color(0xFF4CAF50)
        4 -> Color(0xFFFF9800)
        5 -> Color(0xFF2196F3)
        2 -> Color(0xFFFFC107)
        3 -> Color(0xFF009688)
        6 -> Color(0xFF9C27B0)
        else -> Color(0xFFE91E63)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
    ) {
        // ── MAIN VISUAL AREA ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (showTheoryGrid) {
                TheoryGridView(
                    liveScaleOverlay = liveScaleOverlay,
                    liveChordToneOverlay = liveChordToneOverlay,
                    chordColor = chordColor,
                    keyRootPitchClass = timeline.key.rootPitchClass,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val density = LocalDensity.current
                    val screenWidthPx = with(density) { maxWidth.toPx() }
                    val heightPx = with(density) { maxHeight.toPx() }
                    val nutZoneWidthPx = with(density) { NUT_ZONE_WIDTH_DP.dp.toPx() }
                    val fretboardAreaWidthPx = screenWidthPx - nutZoneWidthPx
                    val scale = remember(screenWidthPx, heightPx, nutZoneWidthPx) {
                        computeSharedScale(fretboardAreaWidthPx, heightPx)
                    }
                    val geometry = remember(fretboardAreaWidthPx, heightPx, scale) {
                        FretboardGeometry(fretboardAreaWidthPx, heightPx, 0, 24, scale)
                    }
                    val markerSize = geometry.stringSpacingPx(12) * 0.65f
                    val pickupZonePx = with(density) { 80.dp.toPx() }
                    val totalContentWidthDp = with(density) {
                        (nutZoneWidthPx + geometry.usedWidthPx + pickupZonePx).toDp()
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(scrollState)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(totalContentWidthDp)
                                .fillMaxHeight()
                        ) {
                            // Open string chord tones in nut zone
                            liveChordToneOverlay.filter { it.fret == 0 }.forEach { pos ->
                                FretMarker(
                                    position = Offset(
                                        x = nutZoneWidthPx / 2f,
                                        y = geometry.stringYAt(pos.stringIndex, 0f)
                                    ),
                                    diameterPx = markerSize,
                                    color = chordColor,
                                    shape = MarkerShape.Circle,
                                    text = NOTE_NAMES[pos.pitchClass],
                                    isBoldText = pos.isRoot
                                )
                            }
                            // Fretboard canvas offset by nut zone
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(nutZoneWidthPx.roundToInt(), 0) }
                                    .width(with(density) { geometry.usedWidthPx.toDp() })
                                    .fillMaxHeight()
                            ) {
                                FretboardCanvas(
                                    geometry = geometry,
                                    modifier = Modifier.fillMaxSize()
                                )
                                liveScaleOverlay.filter { it.fret > 0 }.forEach { pos ->
                                    val isKeyRoot =
                                        pos.pitchClass == timeline.key.rootPitchClass
                                    FretMarker(
                                        position = geometry.markerPosition(
                                            pos.stringIndex,
                                            pos.fret
                                        ),
                                        diameterPx = markerSize,
                                        color = if (isKeyRoot) Color(0xFF606060) else Color(
                                            0xAAFFFFFF
                                        ),
                                        shape = MarkerShape.RoundedSquare,
                                        showBorder = isKeyRoot
                                    )
                                }
                                liveChordToneOverlay.filter { it.fret > 0 }.forEach { pos ->
                                    FretMarker(
                                        position = geometry.markerPosition(
                                            pos.stringIndex,
                                            pos.fret
                                        ),
                                        diameterPx = markerSize,
                                        color = chordColor,
                                        shape = MarkerShape.Circle,
                                        text = NOTE_NAMES[pos.pitchClass],
                                        isBoldText = pos.isRoot
                                    )
                                }
                            }
                            // Pickup zone
                            Box(
                                modifier = Modifier
                                    .offset {
                                        IntOffset(
                                            (nutZoneWidthPx + geometry.usedWidthPx).roundToInt(),
                                            0
                                        )
                                    }
                                    .width(80.dp)
                                    .fillMaxHeight()
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawRect(color = Color(0xFF1E0E04))
                                    drawLine(
                                        color = Color(0xFF3A2012),
                                        start = Offset(0f, 0f),
                                        end = Offset(0f, size.height),
                                        strokeWidth = 6f
                                    )
                                    val pWidth = size.width * 0.65f
                                    val pHeight = size.height * 0.38f
                                    val pX = (size.width - pWidth) / 2f
                                    val pY = (size.height - pHeight) / 2f
                                    drawRoundRect(
                                        color = Color(0xFF111111),
                                        topLeft = Offset(pX, pY),
                                        size = Size(pWidth, pHeight),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                                            8f
                                        )
                                    )
                                    for (s in 0..5) {
                                        drawCircle(
                                            color = Color(0xFF888888),
                                            radius = 5f,
                                            center = Offset(
                                                size.width / 2f,
                                                geometry.stringYAt(s, geometry.usedWidthPx)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // ── CONTROLS AREA ─────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Row 1: Chord name + view toggles + exit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${currentEvent.chord.name} (${currentEvent.chord.romanLabel})",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (!showTheoryGrid) Color(0xFF444444) else Color(
                                    0xFF222222
                                )
                            )
                            .clickable { showTheoryGrid = false }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) { Text("🎸", style = MaterialTheme.typography.labelMedium) }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (showTheoryGrid) Color(0xFF444444) else Color(
                                    0xFF222222
                                )
                            )
                            .clickable { showTheoryGrid = true }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) { Text("⊞", style = MaterialTheme.typography.labelMedium) }
                    Button(
                        onClick = onBackClick,
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Exit", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            // Row 2: Overlay controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Scale", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                Switch(
                    checked = scaleOverlayVisible,
                    onCheckedChange = onScaleOverlayToggled,
                    modifier = Modifier.height(24.dp)
                )
                Text("Chord", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                Switch(
                    checked = chordOverlayVisible,
                    onCheckedChange = onChordOverlayToggled,
                    modifier = Modifier.height(24.dp)
                )
                var scaleDropdownExpanded by remember { mutableStateOf(false) }
                Box {
                    Text(
                        text = liveScaleType.name,
                        color = Color(0xFF90CAF9),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .clickable { scaleDropdownExpanded = true }
                            .background(Color(0xFF333333), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    DropdownMenu(
                        expanded = scaleDropdownExpanded,
                        onDismissRequest = { scaleDropdownExpanded = false }
                    ) {
                        ScaleType.entries.forEach { scaleType ->
                            DropdownMenuItem(
                                text = { Text(scaleType.name) },
                                onClick = {
                                    onScaleTypeChanged(scaleType)
                                    scaleDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            // Row 3: Progression visualizer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                timeline.progressionLabels.forEachIndexed { index, label ->
                    val isActive = currentEvent.barIndex == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primary
                                else Color(0xFF333333)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isActive) Color.White else Color.Gray,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}
// ================================================================
// THEORY GRID VIEW
// ================================================================
@Composable
fun TheoryGridView(
    liveScaleOverlay: List<FretboardPosition>,
    liveChordToneOverlay: List<ChordTonePosition>,
    chordColor: Color,
    keyRootPitchClass: Int,
    modifier: Modifier = Modifier
) {
    val totalFrets = 24
    val totalStrings = 6
    val stringNames = listOf("e", "B", "G", "D", "A", "E")
    val scalePcs = liveScaleOverlay.map { it.fret to it.stringIndex }.toSet()
    val chordTonePcs = liveChordToneOverlay.map { it.fret to it.stringIndex }.toSet()
    val chordToneMap = liveChordToneOverlay.associateBy { it.fret to it.stringIndex }
    val scaleMap = liveScaleOverlay.associateBy { it.fret to it.stringIndex }
    val singleInlayFrets = setOf(3, 5, 7, 9, 15, 17, 19, 21)
    val doubleInlayFrets = setOf(12, 24)
    BoxWithConstraints(modifier = modifier.background(Color(0xFF1E2A4A))) {
        val density = LocalDensity.current
        val totalWidthPx = with(density) { maxWidth.toPx() }
        val totalHeightPx = with(density) { maxHeight.toPx() }
        val labelColWidthPx = totalWidthPx * 0.04f
        val labelRowHeightPx = totalHeightPx * 0.15f
        val gridWidthPx = totalWidthPx - labelColWidthPx
        val gridHeightPx = totalHeightPx - labelRowHeightPx
        val cellWidthPx = gridWidthPx / totalFrets
        val cellHeightPx = gridHeightPx / totalStrings
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = Color(0xFF1A1A2E))
            for (fret in 1..totalFrets) {
                val cellLeft = labelColWidthPx + (fret - 1) * cellWidthPx
                val cellCenterX = cellLeft + cellWidthPx / 2f
                val inlayRadius = cellWidthPx * 0.12f
                when (fret) {
                    in singleInlayFrets -> {
                        drawCircle(
                            color = Color(0xFF3A3A5C),
                            radius = inlayRadius,
                            center = Offset(cellCenterX, labelRowHeightPx + gridHeightPx / 2f)
                        )
                    }
                    in doubleInlayFrets -> {
                        val gap = gridHeightPx * 0.22f
                        val centerY = labelRowHeightPx + gridHeightPx / 2f
                        drawCircle(
                            color = Color(0xFF3A3A5C),
                            radius = inlayRadius,
                            center = Offset(cellCenterX, centerY - gap)
                        )
                        drawCircle(
                            color = Color(0xFF3A3A5C),
                            radius = inlayRadius,
                            center = Offset(cellCenterX, centerY + gap)
                        )
                    }
                }
            }
            for (s in 0..totalStrings) {
                val y = labelRowHeightPx + s * cellHeightPx
                drawLine(
                    color = Color(0xFF3A3A5C),
                    start = Offset(labelColWidthPx, y),
                    end = Offset(totalWidthPx, y),
                    strokeWidth = if (s == 0 || s == totalStrings) 2f else 1f
                )
            }
            for (f in 0..totalFrets) {
                val x = labelColWidthPx + f * cellWidthPx
                drawLine(
                    color = if (f == 0) Color(0xFF8888AA) else Color(0xFF3A3A5C),
                    start = Offset(x, labelRowHeightPx),
                    end = Offset(x, totalHeightPx),
                    strokeWidth = if (f == 0) 3f else 1f
                )
            }
        }
        // Fret number labels
        for (fret in 1..totalFrets) {
            val cellLeft = labelColWidthPx + (fret - 1) * cellWidthPx
            val cellCenterX = cellLeft + cellWidthPx / 2f
            if (fret % 2 != 0 || fret == 12 || fret == 24) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset((cellCenterX - cellWidthPx / 2f).roundToInt(), 0) }
                        .width(with(density) { cellWidthPx.toDp() })
                        .height(with(density) { labelRowHeightPx.toDp() }),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = fret.toString(),
                        color = if (fret == 12 || fret == 24) Color(0xFF90CAF9) else Color(
                            0xFF666688
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = with(density) { (cellWidthPx * 0.35f).toSp() },
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        // String name labels
        for (s in 0 until totalStrings) {
            val cellTop = labelRowHeightPx + s * cellHeightPx
            val cellCenterY = cellTop + cellHeightPx / 2f
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, (cellCenterY - cellHeightPx / 2f).roundToInt()) }
                    .width(with(density) { labelColWidthPx.toDp() })
                    .height(with(density) { cellHeightPx.toDp() }),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringNames[s],
                    color = Color(0xFF666688),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = with(density) { (cellHeightPx * 0.35f).toSp() },
                    textAlign = TextAlign.Center
                )
            }
        }
        // Scale and chord tone markers
        for (s in 0 until totalStrings) {
            for (fret in 1..totalFrets) {
                val key = fret to s
                val cellLeft = labelColWidthPx + (fret - 1) * cellWidthPx
                val cellTop = labelRowHeightPx + s * cellHeightPx
                val cellCenterX = cellLeft + cellWidthPx / 2f
                val cellCenterY = cellTop + cellHeightPx / 2f
                val dotRadius = minOf(cellWidthPx, cellHeightPx) * 0.32f
                when {
                    key in chordTonePcs -> {
                        val pos = chordToneMap[key]!!
                        val isRoot = pos.pitchClass == keyRootPitchClass
                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        (cellCenterX - dotRadius).roundToInt(),
                                        (cellCenterY - dotRadius).roundToInt()
                                    )
                                }
                                .size(with(density) { (dotRadius * 2f).toDp() })
                                .clip(CircleShape)
                                .background(if (isRoot) chordColor else chordColor.copy(alpha = 0.75f))
                                .then(
                                    if (isRoot) Modifier.border(2.dp, Color.White, CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = NOTE_NAMES[pos.pitchClass],
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = with(density) { (dotRadius * 0.9f).toSp() },
                                fontWeight = if (isRoot) FontWeight.ExtraBold else FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    key in scalePcs -> {
                        val pos = scaleMap[key]!!
                        val isKeyRoot = pos.pitchClass == keyRootPitchClass
                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        (cellCenterX - dotRadius).roundToInt(),
                                        (cellCenterY - dotRadius).roundToInt()
                                    )
                                }
                                .size(with(density) { (dotRadius * 2f).toDp() })
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isKeyRoot) Color(0x44FFFFFF) else Color(
                                        0x22FFFFFF
                                    )
                                )
                                .border(
                                    1.dp,
                                    if (isKeyRoot) Color.White else Color(0x88FFFFFF),
                                    RoundedCornerShape(4.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = NOTE_NAMES[pos.pitchClass],
                                color = if (isKeyRoot) Color.White else Color(0xAAFFFFFF),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = with(density) { (dotRadius * 0.85f).toSp() },
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
// ================================================================
// GUITAR FRETBOARD IMPLEMENTATION (Supporting Components)
// ================================================================
object GuitarSpec {
    const val SCALE_LENGTH_MM = 647.7f
    const val TOTAL_FRETS = 24
    const val NUT_WIDTH_MM = 43f
    const val END_WIDTH_MM = 57f
    const val NUT_STRING_SPACING_MM = 7f
    const val END_STRING_SPACING_MM = 10.5f
    val STRING_THICKNESS_MM = floatArrayOf(0.25f, 0.33f, 0.43f, 0.66f, 0.91f, 1.17f)
    val SINGLE_INLAY_FRETS = setOf(3, 5, 7, 9, 15, 17, 19, 21)
    val DOUBLE_INLAY_FRETS = setOf(12, 24)
    fun fretDistanceFromNut(n: Int): Float =
        SCALE_LENGTH_MM * (1f - 1f / 2f.pow(n / 12f))
}
data class SharedScale(val xScale: Float, val yScale: Float)
fun computeSharedScale(fretboardAreaWidthPx: Float, canvasHeightPx: Float): SharedScale {
    val dist0 = GuitarSpec.fretDistanceFromNut(0)
    val dist12 = GuitarSpec.fretDistanceFromNut(12)
    val xScale = fretboardAreaWidthPx / (dist12 - dist0)
    val yScale = canvasHeightPx / GuitarSpec.END_WIDTH_MM
    return SharedScale(xScale, yScale)
}
class FretboardGeometry(
    val canvasWidthPx: Float,
    val canvasHeightPx: Float,
    val startFret: Int,
    val endFret: Int,
    val scale: SharedScale
) {
    private val fretDistances = FloatArray(GuitarSpec.TOTAL_FRETS + 1) {
        GuitarSpec.fretDistanceFromNut(it)
    }
    private val totalDistanceRef = fretDistances[GuitarSpec.TOTAL_FRETS]
    private val startDist = fretDistances[startFret]
    val usedWidthPx = (fretDistances[endFret] - startDist) * scale.xScale
    private fun neckWidthMmAt(fret: Int): Float {
        val t = fretDistances[fret] / totalDistanceRef
        return GuitarSpec.NUT_WIDTH_MM + (GuitarSpec.END_WIDTH_MM - GuitarSpec.NUT_WIDTH_MM) * t
    }
    fun fretX(n: Int): Float = (fretDistances[n] - startDist) * scale.xScale
    fun neckWidthPx(n: Int): Float = neckWidthMmAt(n) * scale.yScale
    fun stringSpacingPx(n: Int): Float {
        val t = fretDistances[n] / totalDistanceRef
        val spacingMm = GuitarSpec.NUT_STRING_SPACING_MM +
                (GuitarSpec.END_STRING_SPACING_MM - GuitarSpec.NUT_STRING_SPACING_MM) * t
        return spacingMm * scale.yScale
    }
    fun stringThicknessPx(stringIndex: Int): Float =
        GuitarSpec.STRING_THICKNESS_MM[stringIndex] * scale.yScale
    val topEdgeLeft = canvasHeightPx / 2f - neckWidthPx(startFret) / 2f
    val topEdgeRight = canvasHeightPx / 2f - neckWidthPx(endFret) / 2f
    val bottomEdgeLeft = canvasHeightPx / 2f + neckWidthPx(startFret) / 2f
    val bottomEdgeRight = canvasHeightPx / 2f + neckWidthPx(endFret) / 2f
    fun topEdgeAt(x: Float): Float {
        val t = (x / usedWidthPx).coerceIn(0f, 1f)
        return topEdgeLeft + (topEdgeRight - topEdgeLeft) * t
    }
    fun bottomEdgeAt(x: Float): Float {
        val t = (x / usedWidthPx).coerceIn(0f, 1f)
        return bottomEdgeLeft + (bottomEdgeRight - bottomEdgeLeft) * t
    }
    fun stringYAt(stringIndex: Int, x: Float): Float {
        val t = (x / usedWidthPx).coerceIn(0f, 1f)
        val spacing = stringSpacingPx(startFret) +
                (stringSpacingPx(endFret) - stringSpacingPx(startFret)) * t
        val center = canvasHeightPx / 2f
        val offset = (stringIndex - 2.5f) * spacing
        return center + offset
    }
    fun fretCenterX(fret: Int): Float {
        require(fret > startFret && fret <= endFret) {
            "fret $fret is outside this page's range ($startFret..$endFret)"
        }
        return (fretX(fret - 1) + fretX(fret)) / 2f
    }
    fun markerPosition(stringIndex: Int, fret: Int): Offset {
        val x = fretCenterX(fret)
        return Offset(x, stringYAt(stringIndex, x))
    }
}
@Composable
fun FretboardCanvas(
    geometry: FretboardGeometry,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = geometry.usedWidthPx
        val neckPath = Path().apply {
            moveTo(0f, geometry.topEdgeLeft)
            lineTo(w, geometry.topEdgeRight)
            lineTo(w, geometry.bottomEdgeRight)
            lineTo(0f, geometry.bottomEdgeLeft)
            close()
        }
        drawPath(neckPath, color = Color(0xFF5A3A22))
        if (w < geometry.canvasWidthPx) {
            drawRect(
                color = Color(0xFF3A2012),
                topLeft = Offset(w, 0f),
                size = Size(geometry.canvasWidthPx - w, geometry.canvasHeightPx)
            )
        }
        for (f in geometry.startFret..geometry.endFret) {
            if (f == geometry.startFret) continue
            val cx = geometry.fretCenterX(f)
            val cy = geometry.canvasHeightPx / 2f
            val inlayRadius = geometry.neckWidthPx(f) * 0.05f
            if (f in GuitarSpec.SINGLE_INLAY_FRETS) {
                drawCircle(Color(0xFFEDEDED), radius = inlayRadius, center = Offset(cx, cy))
            } else if (f in GuitarSpec.DOUBLE_INLAY_FRETS) {
                val gap = geometry.neckWidthPx(f) * 0.18f
                drawCircle(
                    Color(0xFFEDEDED),
                    radius = inlayRadius,
                    center = Offset(cx, cy - gap)
                )
                drawCircle(
                    Color(0xFFEDEDED),
                    radius = inlayRadius,
                    center = Offset(cx, cy + gap)
                )
            }
        }
        for (f in geometry.startFret..geometry.endFret) {
            val x = geometry.fretX(f)
            val top = geometry.topEdgeAt(x)
            val bottom = geometry.bottomEdgeAt(x)
            val isNut = f == 0
            drawLine(
                color = if (isNut) Color(0xFFF5F5F0) else Color(0xFFBFBFBF),
                start = Offset(x, top),
                end = Offset(x, bottom),
                strokeWidth = if (isNut) 14f else 5f
            )
        }
        for (s in 0..5) {
            val thickness = geometry.stringThicknessPx(s)
            val yLeft = geometry.stringYAt(s, 0f)
            val yRight = geometry.stringYAt(s, w)
            val color = if (s >= 3) Color(0xFFCFC9A8) else Color(0xFFE8E4D8)
            drawLine(
                color = color,
                start = Offset(0f, yLeft),
                end = Offset(w, yRight),
                strokeWidth = thickness,
                cap = StrokeCap.Round
            )
        }
    }
}
enum class MarkerShape { Circle, RoundedSquare }
@Composable
fun FretMarker(
    position: Offset,
    diameterPx: Float,
    color: Color,
    shape: MarkerShape = MarkerShape.Circle,
    text: String = "",
    isBoldText: Boolean = false,
    showBorder: Boolean = false,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val diameterDp = with(density) { diameterPx.toDp() }
    val brush = Brush.radialGradient(
        colors = listOf(
            color.copy(alpha = 0.8f),
            color,
            Color.Black.copy(alpha = 0.25f)
        ),
        center = Offset(diameterPx * 0.35f, diameterPx * 0.35f),
        radius = diameterPx * 0.9f
    )
    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    (position.x - diameterPx / 2f).roundToInt(),
                    (position.y - diameterPx / 2f).roundToInt()
                )
            }
            .size(diameterDp)
            .then(
                when (shape) {
                    MarkerShape.Circle -> Modifier.clip(CircleShape)
                    MarkerShape.RoundedSquare -> Modifier.clip(RoundedCornerShape(4.dp))
                }
            )
            .background(brush)
            .then(
                if (showBorder) Modifier.border(
                    1.5.dp, Color.Black,
                    if (shape == MarkerShape.Circle) CircleShape else RoundedCornerShape(4.dp)
                ) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (text.isNotEmpty()) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isBoldText) FontWeight.ExtraBold else FontWeight.Bold,
                    fontSize = (diameterDp.value * (if (isBoldText) 0.55f else 0.35f)).sp,
                    color = Color.White
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}
data class ScaleTone(val page: Int, val stringIndex: Int, val fret: Int)
data class ChordTone(val page: Int, val stringIndex: Int, val fret: Int, val color: Color)
data class OpenTone(val stringIndex: Int, val color: Color)
const val NUT_ZONE_WIDTH_DP = 28
fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
@Preview(showBackground = true, name = "Setup Screen", apiLevel = 36)
@Composable
fun SetupScreenPreview() {
    FretboardLayoutsTheme {
        SetupScreen(
            selectedKey = "C Major",
            onKeySelected = {},
            selectedTimeSignature = TimeSignature.FOUR_FOUR,
            onTimeSignatureSelected = {},
            selectedProgression = "I-V-vi-IV (Pop/Country/Rock)",
            onProgressionSelected = {},
            selectedScaleOverlay = ScaleType.PENTATONIC,
            onScaleOverlaySelected = {},
            selectedChordMode = ChordOverlayMode.ALL_CHORD_TONES,
            onChordModeSelected = {},
            selectedGenre = Genre.ROCK,
            onGenreSelected = {},
            selectedGuitarPreset = null,
            onGuitarPresetSelected = {},
            customStrumMode = false,
            onCustomStrumModeToggled = {},
            selectedTempo = 100,
            onTempoSelected = {},
            audioStatus = "FluidSynth (Preview)",
            onJamClick = {}
        )
    }
}
@Preview(
    showBackground = true,
    widthDp = 640,
    heightDp = 360,
    name = "Playback Landscape Screen",
    apiLevel = 36
)
@Composable
fun PlaybackScreenPreview() {
    val mockTimeline = remember {
        buildJamTimeline(
            key = MusicKey.fromString("C Major"),
            progressionSlots = Progressions.ALL.values.first(),
            scaleType = ScaleType.PENTATONIC,
            chordOverlayMode = ChordOverlayMode.ALL_CHORD_TONES,
            tempoBpm = 100,
            timeSignature = TimeSignature.FOUR_FOUR
        )
    }
    FretboardLayoutsTheme {
        PlaybackScreen(
            timeline = mockTimeline,
            currentChordIndex = 0, // NEW
            onStopAudio = {},
            onBackClick = {},
            liveScaleOverlay = mockTimeline.scaleOverlay,
            liveChordToneOverlay = emptyList(),
            scaleOverlayVisible = true,
            chordOverlayVisible = true,
            liveScaleType = ScaleType.PENTATONIC,
            onScaleTypeChanged = {},
            onScaleOverlayToggled = {},
            onChordOverlayToggled = {}
        )
    }
}