package com.example.fretboardlayouts

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fretboardlayouts.audio.BackingTrackGenerator
import com.example.fretboardlayouts.audio.JamLabAudioEngine // NEW
import com.example.fretboardlayouts.audio.StyleEngine
import com.example.fretboardlayouts.theory.ChordOverlayMode
import com.example.fretboardlayouts.theory.ChordTonePosition
import com.example.fretboardlayouts.theory.FretboardPosition
import com.example.fretboardlayouts.theory.Genre
import com.example.fretboardlayouts.theory.JamTimeline
import com.example.fretboardlayouts.theory.MusicKey
import com.example.fretboardlayouts.theory.Progressions
import com.example.fretboardlayouts.theory.ScaleType
import com.example.fretboardlayouts.theory.StrumPreset
import com.example.fretboardlayouts.theory.TimeSignature
import com.example.fretboardlayouts.theory.allGuitarPresets
import com.example.fretboardlayouts.theory.buildJamTimeline
import com.example.fretboardlayouts.theory.buildPresetOptions
import com.example.fretboardlayouts.theory.generateChordToneOverlay
import com.example.fretboardlayouts.theory.generateScaleOverlay
import com.example.fretboardlayouts.theory.overlayScalePitchClasses
import com.example.fretboardlayouts.theory.resolveSelection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job // NEW
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 1. Define the possible screens/states of our app
sealed class AppState {
    object Setup : AppState()
    data class Loading(val message: String) : AppState()
    data class Playback(val timeline: JamTimeline) : AppState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val audioEngine = JamLabAudioEngine(application) // NEW
    private var lastPlayedEventIndex = -1

    // --- SETUP STATE ---
    var selectedKey = mutableStateOf("C Major")
    var selectedProgression = mutableStateOf("I-V-vi-IV (Pop/Country/Rock)")
    var selectedScaleOverlay = mutableStateOf(ScaleType.PENTATONIC)
    var selectedChordMode = mutableStateOf(ChordOverlayMode.ALL_CHORD_TONES)
    var selectedTempo = mutableIntStateOf(100)
    var selectedGenre = mutableStateOf(Genre.ROCK)
    var selectedTimeSignature = mutableStateOf(TimeSignature.FOUR_FOUR)
    var selectedGuitarPreset = mutableStateOf<StrumPreset?>(null)
    var customStrumMode = mutableStateOf(false)

    // --- LIVE DISPLAY STATE (mid-jam, no rebuild needed) ---
    var liveScaleType = mutableStateOf(ScaleType.PENTATONIC)
    var liveOverlayMode = mutableStateOf(ChordOverlayMode.ALL_CHORD_TONES)
    var scaleOverlayVisible = mutableStateOf(true)
    var chordOverlayVisible = mutableStateOf(true)
    var currentJamTimeline = mutableStateOf<JamTimeline?>(null)
    var currentChordIndex = mutableStateOf(0)

    // --- LIVE OVERLAY CALCULATIONS ---
    val liveScaleOverlay: List<FretboardPosition>
        get() {
            val timeline = currentJamTimeline.value ?: return emptyList()
            if (!scaleOverlayVisible.value) return emptyList()
            return generateScaleOverlay(timeline.key, liveScaleType.value)
        }

    val liveChordToneOverlay: List<ChordTonePosition>
        get() {
            val timeline = currentJamTimeline.value ?: return emptyList()
            if (!chordOverlayVisible.value) return emptyList()
            val events = timeline.events
            if (events.isEmpty()) return emptyList()
            val currentEvent = events[currentChordIndex.value.coerceIn(0, events.size - 1)]
            val scalePcs = overlayScalePitchClasses(timeline.key, liveScaleType.value)
            return generateChordToneOverlay(currentEvent.chord, liveOverlayMode.value, scalePcs)
        }

    // This holds the current screen state that Compose will watch
    var currentScreenState = mutableStateOf<AppState>(AppState.Setup)
        private set

    // The pre-generated MIDI events for the current jam
    private var backingTrackEvents = listOf<BackingTrackGenerator.MidiNoteEvent>()
    private var lastSequencerLoopTime = -1L
    private var playbackJob: Job? = null // NEW
    private data class PendingNoteOff(val channel: Int, val pitch: Int, val offAtMs: Long)
    private val pendingNoteOffs = mutableListOf<PendingNoteOff>()

    // Loading messages
    private val loadingMessages = listOf(
        "Chop-chopping the mahogany for the fretboard...",
        "Tuning the bass player's E-string (takes a while)...",
        "Hiring a drummer who can actually play on beat 1...",
        "Polishing the digital frets..."
    )

    fun startGeneratingTrack() {
        Log.i("MainViewModel", ">>> START GENERATING TRACK - Genre: ${selectedGenre.value} <<<") // MODIFIED
        lastPlayedEventIndex = -1
        lastSequencerLoopTime = -1L
        backingTrackEvents = emptyList()
        pendingNoteOffs.clear()

        currentScreenState.value = AppState.Setup
        viewModelScope.launch {
            // Initialise live display state from setup selections
            liveScaleType.value = selectedScaleOverlay.value
            liveOverlayMode.value = selectedChordMode.value
            scaleOverlayVisible.value = true
            chordOverlayVisible.value = true
            currentChordIndex.value = 0

            withContext(Dispatchers.Main) {
                currentScreenState.value = AppState.Loading(loadingMessages.first())
            }

            val timeline = withContext(Dispatchers.Default) {
                val builtTimeline = buildJamTimeline(
                    key = MusicKey.fromString(selectedKey.value),
                    progressionSlots = Progressions.ALL[selectedProgression.value]
                        ?: Progressions.ALL.values.first(),
                    scaleType = selectedScaleOverlay.value,
                    chordOverlayMode = selectedChordMode.value,
                    tempoBpm = selectedTempo.intValue,
                    timeSignature = selectedTimeSignature.value
                )

                for (i in 1 until loadingMessages.size) {
                    delay(800)
                    withContext(Dispatchers.Main) {
                        currentScreenState.value = AppState.Loading(loadingMessages[i])
                    }
                }

                builtTimeline
            }

            // NOW timeline exists — safe to assign
            currentJamTimeline.value = timeline

            val resolvedPreset = resolveSelection(
                selectedGuitarPreset.value,
                allGuitarPresets,
                selectedGenre.value,
                selectedTimeSignature.value,
                customStrumMode.value
            ) ?: allGuitarPresets.first()
            selectedGuitarPreset.value = resolvedPreset
            backingTrackEvents = StyleEngine.generateAccompaniment(
                timeline, selectedGenre.value, resolvedPreset
            )

            audioEngine.loadGenrePatches(selectedGenre.value) // NEW — replaces midiPlayer.setupInstruments()

            startPlaybackLoop(timeline) // NEW — launches the 8ms loop; also resets lastSequencerLoopTime

            withContext(Dispatchers.Main) {
                currentScreenState.value = AppState.Playback(timeline)
            }
        }
    }

    // NEW — 8ms polling loop, runs for the lifetime of a jam session.
    // Mirrors PlaybackLoopJamLabHandler in JamLabActivity.
    // Runs on Dispatchers.Default — does not block the UI thread.
    // Screen-off audio works because this is a coroutine delay, not withFrameMillis.
    private fun startPlaybackLoop(timeline: JamTimeline) { // NEW
        playbackJob?.cancel() // NEW
        lastSequencerLoopTime = -1L // NEW
        val startMs = System.currentTimeMillis() // NEW
        playbackJob = viewModelScope.launch(Dispatchers.Default) { // NEW
            while (true) { // NEW
                val elapsed = System.currentTimeMillis() - startMs // NEW
                tickSequencer(timeline, elapsed) // NEW
                delay(8L) // NEW
            } // NEW
        } // NEW
    } // NEW

    private fun triggerNoteOn(event: BackingTrackGenerator.MidiNoteEvent, atTimeMs: Long) {
        audioEngine.noteOn(event.channel, event.pitch, event.velocity) // MODIFIED
        pendingNoteOffs.add(PendingNoteOff(event.channel, event.pitch, atTimeMs + event.durationMs))
    }

    // MODIFIED — was playChord(); now private, called only by startPlaybackLoop()
    private fun tickSequencer(timeline: JamTimeline, currentTimeMs: Long) { // MODIFIED
        val loopTime = currentTimeMs % timeline.loopDurationMs

        // 1. DETERMINISTIC SEQUENCER
        if (lastSequencerLoopTime == -1L) {
            Log.i("MainViewModel", ">>> SEQUENCER STARTING at ${loopTime}ms <<<") // MODIFIED
            lastSequencerLoopTime = loopTime
            if (loopTime < 100) {
                backingTrackEvents.filter { it.timeMs == 0L }.forEach { event ->
                    triggerNoteOn(event, currentTimeMs)
                }
            }
        }

        // Handle loop wrap-around
        if (loopTime < lastSequencerLoopTime) {
            Log.i("MainViewModel", ">>> LOOP WRAP DETECTED <<<") // MODIFIED
            backingTrackEvents.filter { it.timeMs > lastSequencerLoopTime }.forEach { event ->
                triggerNoteOn(event, currentTimeMs)
            }
            lastSequencerLoopTime = -1L
        }

        // Standard frame check
        backingTrackEvents
            .filter { it.timeMs > lastSequencerLoopTime && it.timeMs <= loopTime }
            .forEach { event ->
                Log.i("MainViewModel", "Sequencer triggering note: ch=${event.channel} pitch=${event.pitch}") // MODIFIED
                triggerNoteOn(event, currentTimeMs)
            }
        lastSequencerLoopTime = loopTime

        // 2. Fire any note-offs that are due, so notes don't ring forever
        if (pendingNoteOffs.isNotEmpty()) {
            val dueOffs = pendingNoteOffs.filter { it.offAtMs <= currentTimeMs }
            if (dueOffs.isNotEmpty()) {
                dueOffs.forEach { audioEngine.noteOff(it.channel, it.pitch) } // MODIFIED
                pendingNoteOffs.removeAll(dueOffs)
            }
        }

        // 3. Chord index tracking for live overlay
        val currentEvent = timeline.events.find {
            loopTime >= it.startMs && loopTime < it.startMs + it.durationMs
        } ?: return

        val eventIndex = timeline.events.indexOf(currentEvent)
        if (eventIndex != lastPlayedEventIndex) {
            lastPlayedEventIndex = eventIndex
            currentChordIndex.value = eventIndex
        }
    }

    fun stopAudio() {
        playbackJob?.cancel() // NEW — kill the 8ms loop first
        playbackJob = null // NEW
        lastPlayedEventIndex = -1
        lastSequencerLoopTime = -1L
        pendingNoteOffs.forEach { audioEngine.noteOff(it.channel, it.pitch) } // MODIFIED
        pendingNoteOffs.clear()
        audioEngine.stopAudio() // MODIFIED
    }

    fun resetToSetup() {
        stopAudio()
        backingTrackEvents = emptyList()
        currentScreenState.value = AppState.Setup
    }

    fun midiPlayerStatus(): String = audioEngine.engineName // MODIFIED

    override fun onCleared() {
        super.onCleared()
        audioEngine.release() // MODIFIED
    }
}