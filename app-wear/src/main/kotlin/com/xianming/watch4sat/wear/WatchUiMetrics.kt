package com.xianming.watch4sat.wear

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.sqrt

data class PassWindowAdjusterSlots(
    val totalWidth: Dp,
    val sideSlotWidth: Dp,
    val valueSlotWidth: Dp
)

data class PassWindowAdjusterButtonCenters(
    val leftCenterX: Dp,
    val rightCenterX: Dp
)

object WatchUiMetrics {
    fun roundListHorizontalPadding(maxWidth: Dp): Dp = when {
        maxWidth >= 225.dp -> 2.dp
        maxWidth >= 205.dp -> 4.dp
        else -> 6.dp
    }

    val MinimumSemanticTouchTarget = 48.dp
    val ActionButtonHeight = MinimumSemanticTouchTarget
    val CardPadding = 1.dp
    val HeroHorizontalPadding = 2.dp
    val HeroVerticalPadding = 2.dp
    val QthInputHeight = 56.dp
    val SearchInputHeight = 50.dp
    val CompactPickerHeight = 76.dp
    val QthCharacterPickerHeight = 114.dp
    val CompactPickerVerticalSpacing = 2.dp
    val CompactPickerBottomActionReserve = 52.dp
    val CompactPickerApplyBottomPadding = 4.dp
    val EdgeActionContentCenterOffset = 14.dp
    val RadarControlEdgePadding = 8.dp
    val RadarOverlayTopPadding = 26.dp
    val RadarOverlayBottomPadding = 18.dp
    val RadarHorizonRingInset = 8.dp
    val RadarOuterGridStrokeWidth = 2.dp
    val RadarInnerGridStrokeWidth = 1.dp
    val RadarTrajectoryStrokeWidth = 3.0.dp
    val RadarTrajectoryArrowStrokeWidth = 2.5.dp
    val RadarCompassLabelInset = 18.dp
    val RadarTrackArrowSize = 8.dp
    val RadarTrackArrowMinSegmentLength = 14.dp
    val RadarTrackArrowMaxSegmentLength = 120.dp
    val RadarReticleCueRadius = 22.dp
    val RadarSatelliteCueRadius = 28.dp
    val RadarSatelliteCueCoreRadius = 14.dp
    val RadarDetailsButtonVerticalOffset = 36.dp
    const val RadarPassBadgeVisibleMillis = 3_200L
    val RadarPassBadgeBottomPadding = 34.dp
    val RadarSatelliteEdgeButtonBottomPadding = 4.dp
    val RadarTransientHintBottomPadding = 82.dp
    val RadarTransientHintMaxWidth = 156.dp
    val RadarOrientationIconSize = 22.dp
    val RadarOrientationIconBackdropExtraSize = 4.dp
    val RadarOrientationIconBackdropSize =
        RadarOrientationIconSize + RadarOrientationIconBackdropExtraSize
    val RadarCalibrationHintBottomPadding = 10.dp
    const val RadarOverlayWidthFraction = 1f
    val SideIndicatorStrokeWidth = 6.dp
    const val SideIndicatorSweepAngle = 32f
    val DashboardTopVisibilitySlack = 6.dp
    val VisualPageTopSafe = 20.dp
    val VisualPageBottomSafe = 26.dp
    val QthBottomActionSafeSpacer = 52.dp
    val PassWindowAdjusterButtonWidth = MinimumSemanticTouchTarget
    val PassWindowAdjusterButtonHeight = 55.dp
    val PassWindowAdjusterControlHeight = 92.dp
    val PassWindowAdjusterSpacing = 4.dp
    val PassWindowAdjusterIconSize = 24.dp
    const val PassWindowAdjusterButtonShapePercent = 50
    val PassWindowAdjusterButtonContentPadding = 0.dp
    val PassWindowUnitBottomPadding = 0.dp
    val PassWindowUnitSlotWidth = 24.dp
    val PassWindowUnitGap = 3.dp
    val PassStartCountdownRingStrokeWidth = 8.dp
    const val PassStartCountdownRingStartAngle = 130f
    const val PassStartCountdownRingEndAngle = 50f
    val OrbitMapTrackArrowSize = 7.dp
    val OrbitMapTrackArrowMinSegmentLength = 6.dp
    val OrbitMapTrackArrowMaxSegmentLength = 120.dp
    val OrbitMapControlEdgePadding = 8.dp
    val OrbitMapControlTouchTargetSize = 48.dp
    val OrbitMapSystemBackGestureInset = 24.dp
    val OrbitMapSourceStatusTopPadding = 27.dp
    val OrbitMapPersistentStatusTopPadding = 52.dp
    val OrbitMapAttributionBottomPadding = 57.dp
    val OrbitMapAttributionEdgeButtonSpacing = 4.dp
    val OrbitMapAttributionSideControlClearance =
        OrbitMapControlEdgePadding + OrbitMapControlTouchTargetSize
    val OrbitMapAmbientSatelliteBottomPadding = 22.dp
    val SatellitePageIndicatorBottomPadding = 4.dp
    const val QthMapAspectRatio = 2.1f

    fun orbitMapAttributionRoundSafeHorizontalPadding(screenDiameter: Dp): Dp {
        val radius = screenDiameter.value / 2f
        val attributionBottomFromCenter = abs(
            radius - OrbitMapAttributionBottomPadding.value
        )
        val halfChord = sqrt(
            (radius * radius -
                attributionBottomFromCenter * attributionBottomFromCenter)
                .coerceAtLeast(0f)
        )
        return maxOf(
            OrbitMapControlEdgePadding,
            (radius - halfChord + OrbitMapRoundSafeGeometrySlack.value).dp
        )
    }

    private val OrbitMapRoundSafeGeometrySlack = 1.dp

    fun passWindowAdjusterSlots(maxWidth: Dp): PassWindowAdjusterSlots {
        val desiredTotal = when {
            maxWidth >= 225.dp -> (maxWidth - 4.dp).coerceAtMost(230.dp)
            maxWidth >= 205.dp -> maxWidth - 6.dp
            else -> maxWidth - 6.dp
        }.coerceAtMost(maxWidth)
        val desiredSideSlot = when {
            maxWidth >= 225.dp -> 52.dp
            maxWidth >= 205.dp -> 50.dp
            else -> 48.dp
        }
        val availableSlotWidth = desiredTotal - (PassWindowAdjusterSpacing * 2)
        val minimumValueSlot = 56.dp
        val sideSlot = desiredSideSlot
            .coerceAtMost((availableSlotWidth - minimumValueSlot) / 2)
            .coerceAtLeast(PassWindowAdjusterButtonWidth)
        val valueSlot = availableSlotWidth - (sideSlot * 2)
        return PassWindowAdjusterSlots(
            totalWidth = desiredTotal,
            sideSlotWidth = sideSlot,
            valueSlotWidth = valueSlot
        )
    }

    fun passWindowAdjusterButtonCenters(
        maxWidth: Dp,
        @Suppress("UNUSED_PARAMETER") hours: Int
    ): PassWindowAdjusterButtonCenters {
        val slots = passWindowAdjusterSlots(maxWidth)
        return PassWindowAdjusterButtonCenters(
            leftCenterX = slots.sideSlotWidth / 2,
            rightCenterX = slots.totalWidth - (slots.sideSlotWidth / 2)
        )
    }

    fun passWindowUnitLeftX(
        maxWidth: Dp,
        @Suppress("UNUSED_PARAMETER") hours: Int
    ): Dp {
        val slots = passWindowAdjusterSlots(maxWidth)
        return slots.sideSlotWidth +
            PassWindowAdjusterSpacing +
            slots.valueSlotWidth -
            PassWindowUnitSlotWidth
    }
}

object WatchTypographyTokens {
    // Map badges are dense overlay text on a 466px round map. They inherit the
    // Wear M3 bodyExtraSmall style and only override size for map legibility.
    val MapStatus = 10.sp
    val MapAttribution = 9.sp
}
