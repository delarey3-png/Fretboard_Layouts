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
import com.example.fretboardlayouts.theory.Genre
import com.example.fretboardlayouts.theory.JamTimeline
import com.example.fretboardlayouts.theory.MusicKey
import com.example.fretboardlayouts.theory.Progressions
import com.example.fretboardlayouts.theory.ScaleType
import com.example.fretboardlayouts.theory.TimeSignature
import com.example.fretboardlayouts.theory.buildJamTimeline
import com.example.fretboardlayouts.theory.pitchClassAt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.fretboardlayouts.theory.StrumPreset
import com.example.fretboardlayouts.theory.allGuitarPresets
import com.example.fretboardlayouts.theory.resolveSelection
import com.example.fretboardlayouts.theory.buildPresetOptions
import com.example.fretboardlayouts.theory.PresetOption

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

    // This holds the current screen state that Compose will watch
    var currentScreenState = mutableStateOf<AppState>(AppState.Setup)
        private set

    // NEW: The pre-generated MIDI events for the current jam
    private var backingTrackEvents = listOf<BackingTrackGenerator.MidiNoteEvent>()
    private var lastSequencerLoopTime = -1L

    // A list of your funny loading messages
    private val loadingMessages = listOf(
        "Chop-chopping the mahogany for the fretboard...",
        "Tuning the bass player's E-string (takes a while)...",
        "Hiring a drummer who can actually play on beat 1...",
        "Polishing the digital frets..."
    )

    fun startGeneratingTrack() {
        Log.i("MidiPlayer", ">>> START GENERATING TRACK - Genre: ${selectedGenre.value} <<<")
        // Clear previous state before starting
        lastPlayedEventIndex = -1
        lastSequencerLoopTime = -1L
        backingTrackEvents = emptyList()

        // Ensure state is at Setup before launching
        currentScreenState.value = AppState.Setup
        viewModelScope.launch {
            // STEP A: Switch to Loading State
            withContext(Dispatchers.Main) {
                currentScreenState.value = AppState.Loading(loadingMessages.first())
            }

            // STEP B: Simulate the heavy math/file loading on a background thread
            // This is where we call our new Music Engine!
            val timeline = withContext(Dispatchers.Default) {
                // Pre-build the timeline based on user settings
                val builtTimeline = buildJamTimeline(
                    key = MusicKey.fromString(selectedKey.value),
                    progressionSlots = Progressions.ALL[selectedProgression.value] ?: Progressions.ALL.values.first(),
                    scaleType = selectedScaleOverlay.value,
                    chordOverlayMode = selectedChordMode.value,
                    tempoBpm = selectedTempo.intValue,
                    timeSignature = selectedTimeSignature.value
                )

                // Still keep the funny messages for atmosphere
                for (i in 1 until loadingMessages.size) {
                    delay(800) // Reduced delay slightly for better UX
                    withContext(Dispatchers.Main) {
                        currentScreenState.value = AppState.Loading(loadingMessages[i])
                    }
                }
                
                builtTimeline
            }
            val resolvedPreset = resolveSelection(
                selectedGuitarPreset.value, allGuitarPresets, selectedGenre.value, selectedTimeSignature.value, customStrumMode.value
            ) ?: allGuitarPresets.first()
            selectedGuitarPreset.value = resolvedPreset
            backingTrackEvents = StyleEngine.generateAccompaniment(timeline, selectedGenre.value, resolvedPreset)
            
            // Set up instruments for the MIDI path
            if (midiPlayer.isMidiAvailable()) {
                midiPlayer.setupInstruments(GenreInstruments.forGenre(selectedGenre.value))
            }

            lastSequencerLoopTime = -1L

            // STEP C: Construction complete! Move to Screen 2 with the timeline
            withContext(Dispatchers.Main) {
                currentScreenState.value = AppState.Playback(timeline)
            }
        }
    }

    fun playChord(timeline: JamTimeline, currentTimeMs: Long) {
        val loopTime = currentTimeMs % timeline.loopDurationMs
        
        // 1. DETERMINISTIC SEQUENCER
        // Initialize at the start of playback
        if (lastSequencerLoopTime == -1L) {
            Log.i("MidiPlayer", ">>> SEQUENCER STARTING at ${loopTime}ms <<<")
            lastSequencerLoopTime = loopTime
            // Play initial notes at time 0 if we just started
            if (loopTime < 100) {
                backingTrackEvents.filter { it.timeMs == 0L }.forEach { event ->
                    midiPlayer.noteOn(event.channel, event.pitch, event.velocity)
                }
            }
        }

        // Handle loop wrap-around
        if (loopTime < lastSequencerLoopTime) {
            Log.i("MidiPlayer", ">>> LOOP WRAP DETECTED <<<")
            // Catch events from last mark to end of loop
            backingTrackEvents.filter { it.timeMs > lastSequencerLoopTime }.forEach { event ->
                midiPlayer.noteOn(event.channel, event.pitch, event.velocity)
            }
            lastSequencerLoopTime = -1L // Force start of next loop
        }

        // Standard frame check
        backingTrackEvents.filter { it.timeMs > lastSequencerLoopTime && it.timeMs <= loopTime }.forEach { event ->
            Log.i("MidiPlayer", "Sequencer triggering note: ch=${event.channel} pitch=${event.pitch}")
            midiPlayer.noteOn(event.channel, event.pitch, event.velocity)
        }
        lastSequencerLoopTime = loopTime

        // 2. Chord logic for UI synchronization (dots)
        val currentEvent = timeline.events.find { 
            currentTimeMs >= it.startMs && currentTimeMs < it.startMs + it.durationMs 
        } ?: return

        val eventIndex = timeline.events.indexOf(currentEvent)
        if (eventIndex != lastPlayedEventIndex) {
            lastPlayedEventIndex = eventIndex
            // UI trigger for visual sync if needed (currently dots are driven by 'currentEvent' in the Composable)
        }
    }

    fun stopAudio() {
        lastPlayedEventIndex = -1
        lastSequencerLoopTime = -1L
        // We only want to stop the hardware, don't clear the backingTrackEvents list here
        // as it might be needed if the screen recreates.
        midiPlayer.stopAllNotes()
    }

    fun resetToSetup() {
        stopAudio()
        backingTrackEvents = emptyList() // Clear events only when explicitly exiting
        currentScreenState.value = AppState.Setup
    }

    fun midiPlayerStatus(): String = midiPlayer.currentEngineName

    override fun onCleared() {
        super.onCleared()
        midiPlayer.release()
    }
}
