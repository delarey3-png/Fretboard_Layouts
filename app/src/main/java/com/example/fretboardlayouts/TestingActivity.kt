package com.example.fretboardlayouts

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue // NEW
import androidx.compose.runtime.setValue // NEW
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fretboardlayouts.audio.TestingAudioEngine
import com.example.fretboardlayouts.theory.Genre
import com.example.fretboardlayouts.ui.theme.FretboardLayoutsTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.foundation.layout.PaddingValues
import androidx.lifecycle.ViewModelProvider
import androidx.compose.foundation.layout.size
import com.example.fretboardlayouts.theory.Progressions

class TestingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel = androidx.lifecycle.ViewModelProvider(this).get(MainViewModel::class.java)
        setContent {
            FretboardLayoutsTheme {
                TestingScreen(viewModel)
            }
        }
    }}

enum class TestInstrument {
    GUITAR, BASS, PIANO, ORGAN, STRINGS
}

// PASTE THIS INTO TestingActivity.kt, REPLACING THE ENTIRE TestingScreen() FUNCTION
// This creates a full "Sound Sandbox" mode — Jam screen without the fretboard, with live instrument swapping

@Composable
fun TestingScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val audioEngine = remember { TestingAudioEngine(context) }

    var isPlaying by remember { mutableStateOf(false) }
    var showLoadingMessage by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "Sound Sandbox",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Text("Genre", style = MaterialTheme.typography.labelSmall)
        GenreDropdown(viewModel)
        Spacer(modifier = Modifier.height(8.dp))

        Text("Key", style = MaterialTheme.typography.labelSmall)
        KeyDropdown(viewModel)
        Spacer(modifier = Modifier.height(8.dp))

        Text("Progression", style = MaterialTheme.typography.labelSmall)
        ProgressionDropdown(viewModel)
        Spacer(modifier = Modifier.height(8.dp))

        Text("Tempo", style = MaterialTheme.typography.labelSmall)
        TempoSlider(viewModel)
        Spacer(modifier = Modifier.height(8.dp))

        Text("Scale", style = MaterialTheme.typography.labelSmall)
        ScaleDropdown(viewModel)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    isPlaying = true
                    viewModel.stopAudio()
                    showLoadingMessage = true
                    viewModel.startGeneratingTrack()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Generate & Play")
            }

            Button(
                onClick = {
                    isPlaying = false
                    viewModel.stopAudio()  // NEW — actually silence the audio
                },
                modifier = Modifier.weight(1f),
                enabled = viewModel.currentJamTimeline.value != null
            ) {
                Text("Stop")
            }

            androidx.compose.material3.OutlinedButton(
                onClick = {
                    isPlaying = false
                    viewModel.stopAudio()
                    showLoadingMessage = false
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Reset")
            }
        }

        if (showLoadingMessage) {
            Text(
                "Generating...",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        InstrumentGroup(
            title = "Guitars (Ch 0)",
            instruments = listOf(
                "Nylon" to 24,
                "Steel" to 25,
                "Jazz Electric" to 26,
                "Clean Electric" to 27,
                "Muted Electric" to 28,
                "Overdriven" to 29,
                "Distortion" to 30
            ),
            onSelect = { program -> audioEngine.changeProgramOnChannel(0, program) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        InstrumentGroup(
            title = "Bass (Ch 1)",
            instruments = listOf(
                "Acoustic" to 32,
                "Fingered" to 33,
                "Picked" to 34,
                "Fretless" to 35,
                "Slap" to 36
            ),
            onSelect = { program -> audioEngine.changeProgramOnChannel(1, program) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        InstrumentGroup(
            title = "Drums (Ch 9)",
            instruments = listOf(
                "Standard" to 0,
                "Room" to 8,
                "Power" to 16,
                "Electronic" to 24,
                "TR-808" to 25,
                "Jazz" to 32,
                "Brush" to 40,
                "Orchestra" to 48
            ),
            onSelect = { program -> audioEngine.changeProgramOnChannel(9, program) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        InstrumentGroup(
            title = "Keys & Pads (Ch 2)",
            instruments = listOf(
                "Grand Piano" to 0,
                "Bright Piano" to 1,
                "Electric Piano" to 4,
                "Harpsichord" to 6,
                "Celesta" to 8,
                "Synth Pad" to 88,
                "Synth Choir" to 91,
                "Bowed Glass" to 92
            ),
            onSelect = { program -> audioEngine.changeProgramOnChannel(2, program) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        InstrumentGroup(
            title = "Strings (Ch 3)",
            instruments = listOf(
                "Violin" to 40,
                "Viola" to 41,
                "Cello" to 42,
                "Contrabass" to 43,
                "Tremolo Strings" to 44,
                "Pizzicato Strings" to 45,
                "Harp" to 46,
                "Timpani" to 47
            ),
            onSelect = { program -> audioEngine.changeProgramOnChannel(3, program) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        InstrumentGroup(
            title = "Winds (Ch 4)",
            instruments = listOf(
                "Flute" to 73,
                "Recorder" to 74,
                "Trumpet" to 56,
                "Trombone" to 57,
                "Tuba" to 58,
                "French Horn" to 60,
                "Alto Sax" to 65,
                "Soprano Sax" to 64
            ),
            onSelect = { program -> audioEngine.changeProgramOnChannel(4, program) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (isPlaying && viewModel.currentJamTimeline.value != null) {
            PlaybackLoopHandler(viewModel, audioEngine)
        }
    }
}

@Composable
private fun PlaybackLoopHandler(viewModel: MainViewModel, audioEngine: TestingAudioEngine) {
    var elapsedTime by remember { androidx.compose.runtime.mutableLongStateOf(0L) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        val startTime = androidx.compose.runtime.withFrameMillis { it }
        while (true) {
            androidx.compose.runtime.withFrameMillis { frameTime ->
                elapsedTime = frameTime - startTime
                val timeline = viewModel.currentJamTimeline.value
                if (timeline != null) {
                    viewModel.playChord(timeline, frameTime - startTime)
                }
            }
        }
    }

    androidx.compose.foundation.layout.Box(modifier = Modifier.size(0.dp))
}

@Composable
private fun GenreDropdown(viewModel: MainViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val genres = listOf(Genre.ROCK, Genre.BLUES, Genre.COUNTRY, Genre.FUNK, Genre.JAZZ)

    Button(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
        Text(viewModel.selectedGenre.value.name)
    }

    androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        genres.forEach { genre ->
            androidx.compose.material3.DropdownMenuItem(
                text = { Text(genre.name) },
                onClick = {
                    viewModel.selectedGenre.value = genre
                    expanded = false
                }
            )
        }
    }
}

@Composable
private fun KeyDropdown(viewModel: MainViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val keys = listOf(
        "C Major", "C Minor",
        "G Major", "G Minor",
        "D Major", "D Minor",
        "A Major", "A Minor",
        "E Major", "E Minor",
        "B Major", "B Minor",
        "F# Major", "F# Minor",
        "F Major", "F Minor",
        "Bb Major", "Bb Minor",
        "Eb Major", "Eb Minor",
        "Ab Major", "Ab Minor",
        "Db Major", "Db Minor",
        "Gb Major", "Gb Minor",
        "C# Major", "C# Minor"
    )

    Button(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
        Text(viewModel.selectedKey.value)
    }

    if (expanded) {
        androidx.compose.material3.DropdownMenu(expanded = true, onDismissRequest = { expanded = false }) {
            keys.forEach { key ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(key) },
                    onClick = {
                        viewModel.selectedKey.value = key
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ProgressionDropdown(viewModel: MainViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val progressions = Progressions.ALL.keys.toList()

    Button(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
        Text(viewModel.selectedProgression.value.take(20) + "...")
    }

    if (expanded) {
        androidx.compose.material3.DropdownMenu(expanded = true, onDismissRequest = { expanded = false }) {
            progressions.forEach { prog ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(prog, fontSize = 10.sp) },
                    onClick = {
                        viewModel.selectedProgression.value = prog
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun TempoSlider(viewModel: MainViewModel) {
    Column(modifier = Modifier.fillMaxWidth()) {
        androidx.compose.material3.Slider(
            value = viewModel.selectedTempo.intValue.toFloat(),
            onValueChange = { viewModel.selectedTempo.intValue = it.toInt() },
            valueRange = 40f..200f,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "${viewModel.selectedTempo.intValue} BPM",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ScaleDropdown(viewModel: MainViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val scales = listOf(
        com.example.fretboardlayouts.theory.ScaleType.FULL,
        com.example.fretboardlayouts.theory.ScaleType.PENTATONIC,
        com.example.fretboardlayouts.theory.ScaleType.BLUES
    )

    Button(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
        Text(viewModel.selectedScaleOverlay.value.name)
    }

    androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        scales.forEach { scale ->
            androidx.compose.material3.DropdownMenuItem(
                text = { Text(scale.name) },
                onClick = {
                    viewModel.selectedScaleOverlay.value = scale
                    expanded = false
                }
            )
        }
    }
}

@Composable
private fun InstrumentGroup(
    title: String,
    instruments: List<Pair<String, Int>>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(title, style = MaterialTheme.typography.labelSmall)
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            items(instruments.size) { index ->
                val (name, program) = instruments[index]
                androidx.compose.material3.OutlinedButton(
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