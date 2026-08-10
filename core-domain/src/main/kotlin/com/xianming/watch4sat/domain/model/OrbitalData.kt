package com.xianming.watch4sat.domain.model

data class OrbitalData(
    val name: String,
    val catalogNumber: Int,
    val epoch: Double,
    val meanMotion: Double,
    val eccentricity: Double,
    val inclinationDegrees: Double,
    val rightAscensionAscendingNodeDegrees: Double,
    val argumentOfPerigeeDegrees: Double,
    val meanAnomalyDegrees: Double,
    val bstar: Double,
    val meanMotionDot: Double = 0.0
)
