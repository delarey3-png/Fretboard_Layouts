package com.example.fretboardlayouts

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fretboardlayouts.audio.BackingTrackGenerator
import com.example.fretboardlayouts.audio.GenreInstruments
import com.example.fretboardlayouts.audio.MidiPlayer
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

    private val midiPlayer = MidiPlayer(application)
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

    // Loading messages
    private val loadingMessages = listOf(
        "Chop-chopping the mahogany for the fretboard...",
        "Tuning the bass player's E-string (takes a while)...",
        "Hiring a drummer who can actually play on beat 1...",
        "Polishing the digital frets..."
    )

    fun startGeneratingTrack() {
        Log.i("MidiPlayer", ">>> START GENERATING TRACK - Genre: ${selectedGenre.value} <<<")
        lastPlayedEventIndex = -1
        lastSequencerLoopTime = -1L
        backingTrackEvents = emptyList()

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

            if (midiPlayer.isMidiAvailable()) {
                midiPlayer.setupInstruments(GenreInstruments.forGenre(selectedGenre.value))
            }

            lastSequencerLoopTime = -1L

            withContext(Dispatchers.Main) {
                currentScreenState.value = AppState.Playback(timeline)
            }
        }
    }

    fun playChord(timeline: JamTimeline, currentTimeMs: Long) {
        val loopTime = currentTimeMs % timeline.loopDurationMs

        // 1. DETERMINISTIC SEQUENCER
        if (lastSequencerLoopTime == -1L) {
            Log.i("MidiPlayer", ">>> SEQUENCER STARTING at ${loopTime}ms <<<")
            lastSequencerLoopTime = loopTime
            if (loopTime < 100) {
                backingTrackEvents.filter { it.timeMs == 0L }.forEach { event ->
                    midiPlayer.noteOn(event.channel, event.pitch, event.velocity)
                }
            }
        }

        // Handle loop wrap-around
        if (loopTime < lastSequencerLoopTime) {
            Log.i("MidiPlayer", ">>> LOOP WRAP DETECTED <<<")
            backingTrackEvents.filter { it.timeMs > lastSequencerLoopTime }.forEach { event ->
                midiPlayer.noteOn(event.channel, event.pitch, event.velocity)
            }
            lastSequencerLoopTime = -1L
        }

        // Standard frame check
        backingTrackEvents
            .filter { it.timeMs > lastSequencerLoopTime && it.timeMs <= loopTime }
            .forEach { event ->
                Log.i("MidiPlayer", "Sequencer triggering note: ch=${event.channel} pitch=${event.pitch}")
                midiPlayer.noteOn(event.channel, event.pitch, event.velocity)
            }
        lastSequencerLoopTime = loopTime

        // 2. Chord index tracking for live overlay
        val currentEvent = timeline.events.find {
            currentTimeMs >= it.startMs && currentTimeMs < it.startMs + it.durationMs
        } ?: return

        val eventIndex = timeline.events.indexOf(currentEvent)
        if (eventIndex != lastPlayedEventIndex) {
            lastPlayedEventIndex = eventIndex
            currentChordIndex.value = eventIndex
        }
    }

    fun stopAudio() {
        lastPlayedEventIndex = -1
        lastSequencerLoopTime = -1L
        midiPlayer.stopAllNotes()
    }

    fun resetToSetup() {
        stopAudio()
        backingTrackEvents = emptyList()
        currentScreenState.value = AppState.Setup
    }

    fun midiPlayerStatus(): String = midiPlayer.currentEngineName

    override fun onCleared() {
        super.onCleared()
        midiPlayer.release()
    }
}