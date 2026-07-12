package com.example.fretboardlayouts.theory

// made by Claude 11/07

enum class HumanisationLevel {
    OFF, LIGHT, MEDIUM, HEAVY
}

data class HumanisationProfile(
    val accentVariationPct: Float,      // velocity variation on accented beats
    val nonAccentVariationPct: Float,   // velocity variation on non-accented beats
    val timingVariationMs: Float,       // made by Claude 11/07: max ms offset from grid
    val durationVariationPct: Float     // made by Claude 11/07: note length variation
)

// Global variation windows per setting
fun humanisationProfile(level: HumanisationLevel): HumanisationProfile = when (level) {
    HumanisationLevel.OFF    -> HumanisationProfile(0f,    0f,    0f,   0f)
    HumanisationLevel.LIGHT  -> HumanisationProfile(0.02f, 0.05f, 5f,   0.05f)
    HumanisationLevel.MEDIUM -> HumanisationProfile(0.03f, 0.10f, 10f,  0.10f)
    HumanisationLevel.HEAVY  -> HumanisationProfile(0.05f, 0.18f, 20f,  0.18f)
}

// Per-instrument personality multipliers
// Each instrument rolls independently — same setting, different feel
fun instrumentHumanisationMultiplier(channel: Int): Float = when (channel) {
    0    -> 0.9f   // Guitar — close to drums
    1    -> 0.7f   // Bass — tightest, locks in
    2    -> 0.8f   // Piano — fairly tight
    3    -> 0.8f   // Strings — fairly tight
    4    -> 1.1f   // Winds — loosest, breath instrument
    9    -> 1.0f   // Drums — reference
    else -> 1.0f
}
// made by Claude 11/07: Groove templates — consistent intentional timing shifts per genre
// Different from micro-variation: these are systematic, not random
enum class GrooveType { STRAIGHT, LAID_BACK, PUSHED }

fun grooveOffsetMs(
    eventTimeMs: Long,
    beatDurationMs: Long,
    grooveType: GrooveType,
    channel: Int
): Long {
    if (grooveType == GrooveType.STRAIGHT) return 0L
    val posInBeat = (eventTimeMs % beatDurationMs).toFloat() / beatDurationMs
    val isOnBeat = posInBeat < 0.12f || posInBeat > 0.88f
    val multiplier = instrumentHumanisationMultiplier(channel)
    return when (grooveType) {
        GrooveType.LAID_BACK -> if (isOnBeat) (5 * multiplier).toLong()
        else          (14 * multiplier).toLong()
        GrooveType.PUSHED    -> if (isOnBeat) (-3 * multiplier).toLong()
        else          (-8 * multiplier).toLong()
        GrooveType.STRAIGHT  -> 0L
    }
}
// Apply humanisation to a velocity value
// isAccent: whether this note falls on an accented beat
fun humaniseVelocity(
    baseVelocity: Int,
    channel: Int,
    isAccent: Boolean,
    profile: HumanisationProfile
): Int {
    if (profile.accentVariationPct == 0f && profile.nonAccentVariationPct == 0f) {
        return baseVelocity
    }
    val variationPct = if (isAccent) profile.accentVariationPct else profile.nonAccentVariationPct
    val multiplier = instrumentHumanisationMultiplier(channel)
    val range = (baseVelocity * variationPct * multiplier)
    val offset = (Math.random() * range * 2 - range).toInt()
    return (baseVelocity + offset).coerceIn(1, 127)
}
// made by Claude 11/07: Timing micro-variation — notes push/pull slightly off the grid
// Accented beats get 30% of the variation range to stay rhythmically solid
fun humaniseTiming(
    baseTimeMs: Long,
    channel: Int,
    isAccent: Boolean,
    profile: HumanisationProfile
): Long {
    if (profile.timingVariationMs == 0f) return baseTimeMs
    val multiplier = instrumentHumanisationMultiplier(channel)
    val range = if (isAccent)
        profile.timingVariationMs * 0.3f
    else
        profile.timingVariationMs
    val offsetMs = ((Math.random() * range * 2 - range) * multiplier).toLong()
    return (baseTimeMs + offsetMs).coerceAtLeast(0L)
}

// made by Claude 11/07: Duration variation — notes ring slightly shorter or longer
// Minimum 50ms enforced so notes don't become inaudible
fun humaniseDuration(
    baseDurationMs: Int,
    channel: Int,
    profile: HumanisationProfile
): Int {
    if (profile.durationVariationPct == 0f) return baseDurationMs
    val multiplier = instrumentHumanisationMultiplier(channel)
    val range = baseDurationMs * profile.durationVariationPct * multiplier
    val offset = (Math.random() * range * 2 - range).toInt()
    return (baseDurationMs + offset).coerceAtLeast(50)
}