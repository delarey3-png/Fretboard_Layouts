package com.example.fretboardlayouts

// ADDED 10/08/2026 — New ViewModel for JamLabScreen.
//
// Fixes three related bugs found in the audit:
//
// 1. JamLabAudioEngine was constructed inside remember{} with no onDispose,
//    so the engine was never released. Every visit to the screen (including
//    every rotation) leaked a new instance.
//
// 2. All screen state (genre, key, tempo, roles, etc.) lived in plain remember{}
//    vars that were wiped on every configuration change (rotation). The main app
//    correctly uses MainViewModel for this — JamLab now follows the same pattern.
//
// 3. FluidSynthEngine.stop() was called from MidiPlayer.release(), killing the
//    shared singleton even while the main screen was still using it.
//    The engine is now reference-counted (see FluidSynthEngine.kt) and
//    MidiPlayer.release() correctly calls FluidSynthEngine.release() instead.
//
// Usage in JamLabActivity:
//   val viewModel: JamLabViewModel = viewModel()
//   Replace every `remember { ... }` state var in JamLabScreen with
//   the corresponding property from the ViewModel (e.g. viewModel.currentGenre).

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.fretboardlayouts.audio.JamLabAudioEngine
import com.example.fretboardlayouts.theory.Genre
import com.example.fretboardlayouts.theory.HumanisationLevel
import com.example.fretboardlayouts.theory.JamTimeline
import com.example.fretboardlayouts.theory.ScaleType
import com.example.fretboardlayouts.theory.TimeSignature
import com.example.fretboardlayouts.theory.allGuitarPresets
import com.example.fretboardlayouts.theory.allPickingPresets
import com.example.fretboardlayouts.theory.InstrumentRole

class JamLabViewModel(application: Application) : AndroidViewModel(application) {

    // Audio engine — created once per ViewModel instance (survives rotation).
    // Released in onCleared() which is called only when the screen is truly finished.
    val audioEngine: JamLabAudioEngine = JamLabAudioEngine(application)

    // SF2 preset map — read once from the loaded soundfont.
    val availablePatches: Map<String, List<PatchOption>> by lazy {
        parsePresetsFromSF2(audioEngine.getRawPresets())
    }

    // ── Screen state (survives rotation) ─────────────────────────────────────
    var currentGenre         by mutableStateOf(Genre.ROCK)
    var currentKey           by mutableStateOf("C Major")
    var currentProgression   by mutableStateOf("I - V - vi - IV (Pop/Country/Rock)")
    var currentTempo         by mutableIntStateOf(100)
    var currentTimeSignature by mutableStateOf(TimeSignature.FOUR_FOUR)
    var currentScale         by mutableStateOf(ScaleType.FULL)
    var currentStrumPreset   by mutableStateOf(allGuitarPresets[0])
    var currentPickingPreset by mutableStateOf(allPickingPresets[0])
    var customStrumMode      by mutableStateOf(false)
    var currentNoteLength    by mutableStateOf("1/4")
    var currentHumanisation  by mutableStateOf(HumanisationLevel.OFF)
    var currentBarIndex      by mutableIntStateOf(0)
    var selectedInstrumentKey by mutableStateOf("guitar")
    var instrumentRoles      by mutableStateOf(
        INSTRUMENT_DEFS.associate { it.key to it.defaultRole }
    )
    var selectedPatchByChannel by mutableStateOf(mapOf<Int, PatchOption>())
    var isPlaying            by mutableStateOf(false)
    var showGeneratingMessage by mutableStateOf(false)
    var currentTimeline      by mutableStateOf<JamTimeline?>(null)

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    override fun onCleared() {
        super.onCleared()
        // ADDED 10/08/2026 — This is the single guaranteed cleanup point.
        // Called only when the Activity is truly finishing (back press / process death),
        // NOT on rotation — so the engine is never prematurely destroyed.
        audioEngine.release()
    }
}
