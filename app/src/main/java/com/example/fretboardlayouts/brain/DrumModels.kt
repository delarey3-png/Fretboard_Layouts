package com.example.fretboardlayouts.brain

/**
 * Core groove learned from real drummers.
 * Each string represents one 16-step bar.
 */
data class CoreGroove(
    val kick: String,
    val snare: String,
    val hihat: String,
    val tom: String = "________________",
    val cymbal: String = "________________",
    val weight: Int
)

/**
 * Probability of a hit occurring on each step.
 */
data class ProbabilityModel(
    val values: FloatArray
)

/**
 * Average velocity profile for an instrument.
 */
data class VelocityProfile(
    val average: Float,
    val minimum: Int,
    val maximum: Int
)

/**
 * Average timing offsets (human feel).
 */
data class TimingProfile(
    val offsets: FloatArray
)

/**
 * Complete learned drum model.
 *
 * Everything the Drum Brain needs lives here.
 */
data class DrumModel(

    val genre: String,

    val coreGrooves: List<CoreGroove>,

    val kickProbability: ProbabilityModel,
    val snareProbability: ProbabilityModel,
    val hihatProbability: ProbabilityModel,

    val velocities: Map<String, VelocityProfile>,

    val timing: Map<String, TimingProfile>
)