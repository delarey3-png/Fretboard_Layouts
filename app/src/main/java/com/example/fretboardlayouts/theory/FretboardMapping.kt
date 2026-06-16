package com.example.fretboardlayouts.theory

/**
 * Open-string MIDI notes for standard tuning, in the SAME string-index
 * convention used by FretboardGeometry: 0 = high E, 5 = low E.
 * E4=64, B3=59, G3=55, D3=50, A2=45, E2=40
 */
val OPEN_STRING_MIDI = intArrayOf(64, 59, 55, 50, 45, 40)

const val MAX_FRET = 24

fun midiNote(stringIndex: Int, fret: Int): Int = OPEN_STRING_MIDI[stringIndex] + fret
fun pitchClassAt(stringIndex: Int, fret: Int): Int = midiNote(stringIndex, fret) % 12

/** Which fretboard "page" a fret belongs to, matching FretboardGeometry's 0-12 / 12-24 split */
fun pageForFret(fret: Int): Int = if (fret <= 12) 0 else 1
