package com.example.fretboardlayouts.theory

fun StrumPreset.supportsShape(shape: MeterShape): Boolean =
    layers.all { it.patternByShape.containsKey(shape) && it.directionsByShape.containsKey(shape) }

fun StrumPreset.supports(timeSignature: TimeSignature): Boolean = supportsShape(timeSignature.shape())

/** One row the dropdown can render: the preset itself, plus whether it's currently selectable. */
data class PresetOption(
    val preset: StrumPreset,
    val enabled: Boolean
)

/**
 * Builds the full dropdown list for a given context. Presets that don't support the current
 * time signature's shape are excluded entirely -- selecting one would produce a broken pattern
 * regardless of mode. In normal mode, only presets tagged for [genre] are enabled (others are
 * shown disabled); in custom mode, every shape-compatible preset is enabled.
 */
fun buildPresetOptions(
    allPresets: List<StrumPreset>,
    genre: Genre,
    timeSignature: TimeSignature,
    customMode: Boolean
): List<PresetOption> {
    return allPresets
        .filter { it.supports(timeSignature) }
        .map { preset -> PresetOption(preset, enabled = customMode || isApplicable(preset, genre)) }
}

/** The preset a genre + time signature combination should fall back to. */
fun defaultPresetFor(allPresets: List<StrumPreset>, genre: Genre, timeSignature: TimeSignature): StrumPreset? =
    allPresets.firstOrNull { it.supports(timeSignature) && isApplicable(it, genre) }

/**
 * Decides what the preset selection should become after genre and/or time signature change.
 * Leaves the current selection alone if it's still valid; otherwise falls back to the new
 * context's default.
 */
fun resolveSelection(
    currentPreset: StrumPreset?,
    allPresets: List<StrumPreset>,
    genre: Genre,
    timeSignature: TimeSignature,
    customMode: Boolean
): StrumPreset? {
    val stillValid = currentPreset != null &&
            currentPreset.supports(timeSignature) &&
            (customMode || isApplicable(currentPreset, genre))
    return if (stillValid) currentPreset else defaultPresetFor(allPresets, genre, timeSignature)
}