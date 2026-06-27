package com.example.fretboardlayouts.theory

/**
 * The five CAGED shapes. Each shape is named after the open chord
 * it resembles when played as a barre or position chord.
 */
enum class CagedShape(val displayName: String) {
    E("E Shape"),
    D("D Shape"),
    C("C Shape"),
    A("A Shape"),
    G("G Shape")
}

/**
 * One of the five CAGED positions for a specific key.
 * [rootFret] is where the root note of the key sits in this position.
 * [fretRange] is the comfortable playing window (typically 4 frets).
 * [shape] is which open chord shape this position resembles.
 */
data class CagedPosition(
    val shape: CagedShape,
    val rootFret: Int,
    val fretRange: IntRange
)

/**
 * The open-position root frets for each CAGED shape in the key of C (root = 0).
 * These are the "anchor" frets — where the root note sits for each shape
 * in the lowest-root-note position.
 *
 * E shape: root on low E string
 * A shape: root on A string
 * G shape: root on high E and low E strings
 * C shape: root on A string (higher than A shape)
 * D shape: root on B string
 */
private val CAGED_SHAPE_ROOT_OFFSETS = listOf(
    CagedShape.E to 0,   // E shape: root at fret 0 in key of C = fret 8
    CagedShape.D to 2,   // D shape follows E shape up the neck
    CagedShape.C to 3,   // C shape
    CagedShape.A to 5,   // A shape
    CagedShape.G to 7    // G shape
)

/**
 * Returns all 5 CAGED positions for a given key, ordered up the neck
 * starting from the lowest playable position.
 *
 * The fret window is 4 frets wide — enough to cover a full pentatonic
 * box shape comfortably. The root fret is calculated by adding the
 * key's root pitch class to each shape's base offset, then wrapping
 * around the 12-fret octave as needed.
 */
fun cagedPositionsForKey(key: MusicKey): List<CagedPosition> {
    val root = key.rootPitchClass

    return CAGED_SHAPE_ROOT_OFFSETS.map { (shape, offset) ->
        // Calculate where this shape's root lands for this key
        var rootFret = (root + offset) % 12
        // Push into a comfortable playing range (frets 2-17)
        if (rootFret < 2) rootFret += 12
        val fretRange = (rootFret - 1)..(rootFret + 3)
        CagedPosition(shape, rootFret, fretRange)
    }.sortedBy { it.rootFret } // order up the neck
}

/**
 * Filters an existing scale overlay down to only the positions
 * that fall within a specific CAGED position's fret window.
 * This is what drives "show me one box shape at a time."
 */
fun scaleOverlayForPosition(
    allPositions: List<FretboardPosition>,
    cagedPosition: CagedPosition
): List<FretboardPosition> {
    return allPositions.filter { it.fret in cagedPosition.fretRange }
}

/**
 * Same as [scaleOverlayForPosition] but for chord tone overlays.
 * Filters to only the chord tones visible in the current CAGED window.
 */
fun chordTonesForPosition(
    allChordTones: List<ChordTonePosition>,
    cagedPosition: CagedPosition
): List<ChordTonePosition> {
    return allChordTones.filter { it.fret in cagedPosition.fretRange }
}

/**
 * The "next" CAGED position up the neck, wrapping around after position 5.
 * Used for the auto-cycle feature ("every few seconds, shift to next shape").
 */
fun nextPosition(
    current: CagedPosition,
    allPositions: List<CagedPosition>
): CagedPosition {
    val currentIndex = allPositions.indexOfFirst { it.shape == current.shape }
    return allPositions[(currentIndex + 1) % allPositions.size]
}

/**
 * Given a elapsed time and a cycle interval in milliseconds,
 * returns which CAGED position should currently be displayed.
 * Drop this into a LaunchedEffect in the playback screen to
 * auto-cycle through shapes.
 */
fun currentPositionForTime(
    elapsedMs: Long,
    cycleIntervalMs: Long,
    allPositions: List<CagedPosition>
): CagedPosition {
    val index = ((elapsedMs / cycleIntervalMs) % allPositions.size).toInt()
    return allPositions[index]
}