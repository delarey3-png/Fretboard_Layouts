package com.example.fretboardlayouts.theory

/** One slot's state within a rhythm grid. */
enum class SlotState { REST, HIT, ACCENT }

/** A full bar's worth of slot states, one per subdivisionCount slot. */
typealias RhythmPattern = List<SlotState>

/**
 * Parses a human-readable rhythm notation string into a [RhythmPattern].
 *
 * - Any letter or digit (e.g. "1", "e", "&", "a") = a normal hit.
 * - "_" = a rest (no hit).
 * - A character wrapped in "<>" (e.g. "<1>") = an accented hit.
 * - Spaces and commas are ignored, so beats can be grouped for readability,
 *   e.g. "<1>e_a _e&a 3e&_ 4e&a".
 *
 * The resulting list's size must match the target TimeSignature's
 * subdivisionCount for slot positions to line up correctly.
 */
fun parsePattern(notation: String): RhythmPattern {
    val cleaned = notation.filter { it != ' ' && it != ',' }
    val result = mutableListOf<SlotState>()
    var i = 0
    while (i < cleaned.length) {
        when (cleaned[i]) {
            '<' -> {
                result.add(SlotState.ACCENT)
                i += 2 // skip the wrapped character
                if (i < cleaned.length && cleaned[i] == '>') i++ // skip closing bracket
            }
            '_' -> {
                result.add(SlotState.REST)
                i++
            }
            else -> {
                result.add(SlotState.HIT)
                i++
            }
        }
    }
    return result
}

/**
 * Parses a strum-direction notation string into a list of booleans (true = downstroke).
 * Any character works as "downstroke" except 'u', which means upstroke.
 * Must be the same length as the [RhythmPattern] it pairs with; positions where
 * the pattern is REST are never read, so any character is fine there.
 */
fun parseDirections(notation: String): List<Boolean> {
    val cleaned = notation.filter { it != ' ' && it != ',' }
    return cleaned.map { it != 'u' }
}