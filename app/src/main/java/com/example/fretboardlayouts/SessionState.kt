package com.example.fretboardlayouts

// ================================================================
// SESSION STATE
// NEW made by Claude 18/08/2026
//
// Single source of truth for the Music Dashboard across all screens.
// Held at Application scope so both MainActivity and JamLabActivity
// read from and write to the same instance.
//
// Flow:
//   Each screen pushes its current musical state via updateDashboard()
//   whenever its relevant state changes (genre, key, tempo, etc.).
//   Each screen's MusicDashboard reads dashboardState directly.
//
// LaunchedEffect behaviour ensures this works correctly:
//   When the user returns to LoopBuilder from Jam Lab, LoopBuilder's
//   LaunchedEffect keys have not changed (the user didn't touch anything),
//   so it does NOT re-fire and does NOT overwrite Jam Lab's last push.
//   The dashboard therefore stays on Jam Lab's state until the user
//   actively changes something in LoopBuilder.
// ================================================================

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.fretboardlayouts.theory.Genre
import com.example.fretboardlayouts.theory.TimeSignature

// ── Dashboard state data class ────────────────────────────────────
// activeChordIndex is intentionally excluded — it is ephemeral
// playback state, owned locally by whichever screen is playing.

data class DashboardState(
    val chordNames: List<String>     = listOf("C", "G", "Am", "F"),
    val numerals: List<String>       = listOf("I", "V", "vi", "IV"),
    val keyLabel: String             = "C Major",
    val timeSignature: TimeSignature = TimeSignature.FOUR_FOUR,
    val tempo: Int                   = 100,
    val genre: Genre                 = Genre.ROCK
)

// ── Session state — held at Application scope ─────────────────────

class SessionState {
    var dashboard by mutableStateOf(DashboardState())
        private set

    fun updateDashboard(state: DashboardState) {
        dashboard = state
    }
}
