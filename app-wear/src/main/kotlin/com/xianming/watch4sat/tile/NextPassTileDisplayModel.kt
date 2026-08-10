package com.xianming.watch4sat.tile

import com.xianming.watch4sat.domain.freshness.TleFreshnessSeverity

enum class NextPassTileKind {
    UpcomingPass,
    ActivePass,
    NoQth,
    NoTle,
    NoSatellites,
    NoPassSoon,
    TileOffline
}

enum class NextPassTileTone {
    Primary,
    Info,
    Warning,
    Error
}

data class NextPassTileDisplayModel(
    val kind: NextPassTileKind,
    val header: String,
    val title: String,
    val countdown: String? = null,
    val meta: String,
    val ctaLabel: String,
    val tone: NextPassTileTone,
    val showProgress: Boolean = false,
    val progress: Float? = null,
    val countdownTargetMillis: Long? = null,
    val progressStartMillis: Long? = null,
    val progressEndMillis: Long? = null,
    val nextTransitionMillis: Long? = null,
    val maxElevationDegrees: Double? = null,
    val tleFreshnessSeverity: TleFreshnessSeverity? = null,
    val accessibilityFreshnessDescription: String? = null,
    val accessibilityDescription: String = title,
    val launchAction: TileLaunchAction
)
