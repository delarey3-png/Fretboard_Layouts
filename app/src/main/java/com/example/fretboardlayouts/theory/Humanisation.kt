package com.example.fretboardlayouts.theory

// made by Claude 11/07

enum class HumanisationLevel {
    OFF, LIGHT, MEDIUM, HEAVY
}

data class HumanisationProfile(
    val accentVariationPct: Float,    // variation on accented beats
    val nonAccentVariationPct: Float  // variation on non-accented beats
)

// Global variation windows per setting
fun humanisationProfile(level: HumanisationLevel): HumanisationProfile = when (level) {
    HumanisationLevel.OFF    -> HumanisationProfile(0f, 0f)
    HumanisationLevel.LIGHT  -> HumanisationProfile(0.02f, 0.05f)
    HumanisationLevel.MEDIUM -> HumanisationProfile(0.03f, 0.10f)
    HumanisationLevel.HEAVY  -> HumanisationProfile(0.05f, 0.18f)
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