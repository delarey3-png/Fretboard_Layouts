package com.example.fretboardlayouts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.fretboardlayouts.theory.Genre
import com.example.fretboardlayouts.theory.TimeSignature

// ================================================================
// 📺 MUSIC DASHBOARD
// NEW made by Claude 17/08/2026
//
// Shared composable — same layout on Jam Lab and LoopBuilder.
// Shows current musical state at a glance and updates live as the
// user changes selections. During playback, the active chord lights
// up as the progression plays through.
//
// activeChordIndex = -1  → not playing (all chords shown at rest)
// activeChordIndex = 0+  → index of the currently sounding chord
// ================================================================

@Composable
fun MusicDashboard(
    chordNames: List<String>,       // e.g. ["C", "G", "Am", "F"]
    numerals: List<String>,         // e.g. ["I", "V", "vi", "IV"]
    activeChordIndex: Int,          // -1 = not playing, 0+ = active chord
    keyLabel: String,               // e.g. "C Major"
    timeSignature: TimeSignature,
    tempo: Int,
    genre: Genre,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFF1A1A2E), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        if (chordNames.isNotEmpty()) {

            // ── Chord names row ──────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth()) {
                chordNames.forEachIndexed { index, name ->
                    val isActive = index == activeChordIndex
                    Text(
                        text = name,
                        color = if (isActive) Color(0xFF90CAF9) else Color(0xFF888899),
                        fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal,
                        fontSize = if (isActive) 18.sp else 15.sp,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = Color(0xFF333355))
            Spacer(Modifier.height(4.dp))

            // ── Roman numerals row ───────────────────────────────
            Row(modifier = Modifier.fillMaxWidth()) {
                numerals.forEachIndexed { index, numeral ->
                    val isActive = index == activeChordIndex
                    Text(
                        text = numeral,
                        color = if (isActive) Color.White else Color(0xFF555577),
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        fontSize = if (isActive) 13.sp else 10.sp,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFF222244))
            Spacer(Modifier.height(6.dp))
        }

        // ── Info bar ─────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DashboardChip(keyLabel)
            DashboardPipe()
            DashboardChip(timeSignature.display)
            DashboardPipe()
            DashboardChip("$tempo BPM")
            DashboardPipe()
            DashboardChip(genre.displayName)
        }
    }
}

// ── Private helpers ───────────────────────────────────────────────

@Composable
private fun DashboardChip(text: String) {
    Text(
        text = text,
        fontSize = 10.sp,
        color = Color(0xFF7788AA),
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun DashboardPipe() {
    Text(
        text = "  |  ",
        fontSize = 10.sp,
        color = Color(0xFF333355)
    )
}
