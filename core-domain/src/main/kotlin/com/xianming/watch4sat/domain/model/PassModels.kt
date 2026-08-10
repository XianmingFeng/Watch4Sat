package com.xianming.watch4sat.domain.model

data class PassWindow(
    val hoursAhead: Int = 12,
    val minimumElevationDegrees: Double = 0.0
)

enum class PassBoundary {
    ACTUAL,
    WINDOW_CLIPPED
}

enum class PassVisibility {
    DISCRETE,
    CONTINUOUS
}

data class SatellitePass(
    val catalogNumber: Int,
    val satelliteName: String,
    val aosMillis: Long,
    val losMillis: Long,
    val tcaMillis: Long,
    val maxElevationDegrees: Double,
    val aosAzimuthDegrees: Double,
    val losAzimuthDegrees: Double,
    val altitudeKm: Int? = null,
    val orbitalData: OrbitalData? = null,
    val aosBoundary: PassBoundary = PassBoundary.ACTUAL,
    val losBoundary: PassBoundary = PassBoundary.ACTUAL
) {
    val durationMillis: Long = losMillis - aosMillis
    val visibility: PassVisibility = if (
        aosBoundary == PassBoundary.WINDOW_CLIPPED &&
        losBoundary == PassBoundary.WINDOW_CLIPPED
    ) {
        PassVisibility.CONTINUOUS
    } else {
        PassVisibility.DISCRETE
    }
    val isContinuouslyVisible: Boolean = visibility == PassVisibility.CONTINUOUS
    val isPassStartCandidate: Boolean = aosBoundary == PassBoundary.ACTUAL

    fun isActiveAt(nowMillis: Long): Boolean {
        return nowMillis >= aosMillis && nowMillis < losMillis
    }
}

data class PassCardUi(
    val catalogNumber: Int,
    val satelliteName: String,
    val aosCountdown: String,
    val aosTime: String,
    val losTime: String,
    val tcaTime: String,
    val maxElevation: String,
    val aosAzimuth: String,
    val losAzimuth: String,
    val duration: String,
    val modeFrequencyHint: String?,
    val isActive: Boolean,
    val isUpcoming: Boolean
)
