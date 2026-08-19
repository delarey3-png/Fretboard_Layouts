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

// ================================================================
// 📺 MUSIC DASHBOARD
// NEW made by Claude 17/08/2026
// MODIFIED made by Claude 18/08/2026 — accepts DashboardState
//   instead of individual params. activeChordIndex stays separate
//   (ephemeral playback state, not part of shared session).
//
// activeChordIndex = -1  → not playing (all chords shown at rest)
// activeChordIndex = 0+  → index of the currently sounding chord
// ================================================================

@Composable
fun MusicDashboard(
    state: DashboardState,
    activeChordIndex: Int = -1,
    compact: Boolean = false,           // NEW made by Claude 18/08/2026
    modifier: Modifier = Modifier
) {
    // Compact mode used on the landscape Jam Screen where vertical space is tight
    val hPad      = if (compact) 8.dp  else 14.dp
    val vPad      = if (compact) 4.dp  else 10.dp
    val nameSize  = if (compact) 11.sp else 15.sp
    val nameActive= if (compact) 13.sp else 18.sp
    val numSize   = if (compact) 8.sp  else 10.sp
    val numActive = if (compact) 9.sp  else 13.sp
    val chipSize  = if (compact) 8.sp  else 10.sp
    val spacerSm  = if (compact) 2.dp  else 4.dp
    val spacerMd  = if (compact) 3.dp  else 8.dp
    val spacerLg  = if (compact) 2.dp  else 6.dp

    Column(
        modifier = modifier
            .background(Color(0xFF1A1A2E), RoundedCornerShape(8.dp))
            .padding(horizontal = hPad, vertical = vPad)
    ) {
        if (state.chordNames.isNotEmpty()) {

            // ── Chord names row ──────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth()) {
                state.chordNames.forEachIndexed { index, name ->
                    val isActive = index == activeChordIndex
                    Text(
                        text = name,
                        color = if (isActive) Color(0xFF90CAF9) else Color(0xFF888899),
                        fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal,
                        fontSize = if (isActive) nameActive else nameSize,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(spacerSm))
            HorizontalDivider(color = Color(0xFF333355))
            Spacer(Modifier.height(spacerSm))

            // ── Roman numerals row ───────────────────────────────
            Row(modifier = Modifier.fillMaxWidth()) {
                state.numerals.forEachIndexed { index, numeral ->
                    val isActive = index == activeChordIndex
                    Text(
                        text = numeral,
                        color = if (isActive) Color.White else Color(0xFF555577),
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        fontSize = if (isActive) numActive else numSize,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(spacerMd))
            HorizontalDivider(color = Color(0xFF222244))
            Spacer(Modifier.height(spacerLg))
        }

        // ── Info bar ─────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DashboardChip(state.keyLabel, chipSize)
            DashboardPipe(chipSize)
            DashboardChip(state.timeSignature.display, chipSize)
            DashboardPipe(chipSize)
            DashboardChip("${state.tempo} BPM", chipSize)
            DashboardPipe(chipSize)
            DashboardChip(state.genre.displayName, chipSize)
        }
    }
}

// ── Private helpers ───────────────────────────────────────────────

@Composable
private fun DashboardChip(text: String, size: androidx.compose.ui.unit.TextUnit = 10.sp) {
    Text(
        text = text,
        fontSize = size,
        color = Color(0xFF7788AA),
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun DashboardPipe(size: androidx.compose.ui.unit.TextUnit = 10.sp) {
    Text(
        text = "  |  ",
        fontSize = size,
        color = Color(0xFF333355)
    )
}