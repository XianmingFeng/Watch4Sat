package com.xianming.watch4sat.wear.radar

import com.xianming.watch4sat.data.settings.RadarForwardAxis
import com.xianming.watch4sat.data.settings.RadarWristSide

data class RadarPointing(
    val azimuthDegrees: Double,
    val elevationDegrees: Double
)

data class RadarOrientationSnapshot(
    val sensorKind: RadarOrientationSensorKind,
    val status: RadarOrientationStatus,
    val accuracy: RadarSensorAccuracy,
    val pointing: RadarPointing? = null,
    val referenceAzimuthDegrees: Double? = null
)

enum class RadarOrientationSensorKind {
    RotationVector,
    GeomagneticRotationVector,
    GameRotationVector,
    None
}

enum class RadarSensorAccuracy {
    High,
    Medium,
    Low,
    Unreliable,
    Unavailable
}

enum class RadarSensorAxis {
    PositiveX,
    PositiveY,
    PositiveZ,
    NegativeX,
    NegativeY,
    NegativeZ
}

data class RadarRemapAxes(
    // SensorManager parameters: where the original device X/Y axes land in remapped coordinates.
    val deviceXDestinationAxis: RadarSensorAxis,
    val deviceYDestinationAxis: RadarSensorAxis
)

enum class RadarDisplayRotation(val clockwiseQuarterTurns: Int) {
    ROTATION_0(0),
    ROTATION_90(1),
    ROTATION_180(2),
    ROTATION_270(3)
}

enum class RadarPhysicalEdge(
    val sensorQuarterTurns: Int,
    val visualOffsetDegrees: Float
) {
    SCREEN_TOP(sensorQuarterTurns = 0, visualOffsetDegrees = 0f),
    SCREEN_RIGHT(sensorQuarterTurns = 1, visualOffsetDegrees = 90f),
    SCREEN_LEFT(sensorQuarterTurns = 3, visualOffsetDegrees = -90f)
}

enum class RadarPlotOrientationMode {
    TrackingForward,
    NorthReference
}

data class RadarHeadingFilterState(
    val displayDegrees: Float = 0f,
    val velocityDegreesPerSecond: Float = 0f,
    val lastSampleElapsedRealtimeMillis: Long? = null,
    val initialized: Boolean = false
)

data class RadarHeadingFilterResult(
    val state: RadarHeadingFilterState,
    val targetDegrees: Float,
    val snap: Boolean
)

data class RadarDisplayPoint(
    val x: Float,
    val y: Float
)

data class RadarPositionFilterState(
    val display: RadarDisplayPoint = RadarDisplayPoint(0f, 0f),
    val initialized: Boolean = false
)

data class RadarPositionFilterResult(
    val state: RadarPositionFilterState,
    val target: RadarDisplayPoint?,
    val snap: Boolean
)

enum class RadarOrientationStatus {
    NorthReference,
    PointingAssistActive,
    SensorUnavailable,
    AccuracyLow,
    MagneticCorrectionUnavailable,
    RelativeOnly
}

enum class RadarTransientHint {
    None,
    PointingAssist,
    FigureEightCalibration
}

object RadarOrientationPolicy {

    fun remapAxesFor(
        forwardEdge: RadarPhysicalEdge,
        displayRotation: RadarDisplayRotation
    ): RadarRemapAxes {
        return when (
            (displayRotation.clockwiseQuarterTurns + forwardEdge.sensorQuarterTurns) % 4
        ) {
            0 -> RadarRemapAxes(
                deviceXDestinationAxis = RadarSensorAxis.PositiveX,
                deviceYDestinationAxis = RadarSensorAxis.PositiveY
            )
            1 -> RadarRemapAxes(
                deviceXDestinationAxis = RadarSensorAxis.PositiveY,
                deviceYDestinationAxis = RadarSensorAxis.NegativeX
            )
            2 -> RadarRemapAxes(
                deviceXDestinationAxis = RadarSensorAxis.NegativeX,
                deviceYDestinationAxis = RadarSensorAxis.NegativeY
            )
            else -> RadarRemapAxes(
                deviceXDestinationAxis = RadarSensorAxis.NegativeY,
                deviceYDestinationAxis = RadarSensorAxis.PositiveX
            )
        }
    }

    fun chooseSensor(
        hasRotationVector: Boolean,
        hasGeomagneticRotationVector: Boolean,
        hasGameRotationVector: Boolean
    ): RadarOrientationSensorKind {
        return when {
            hasRotationVector -> RadarOrientationSensorKind.RotationVector
            hasGeomagneticRotationVector -> RadarOrientationSensorKind.GeomagneticRotationVector
            hasGameRotationVector -> RadarOrientationSensorKind.GameRotationVector
            else -> RadarOrientationSensorKind.None
        }
    }

    fun statusFor(
        sensorKind: RadarOrientationSensorKind,
        accuracy: RadarSensorAccuracy,
        hasMagneticCorrection: Boolean
    ): RadarOrientationStatus {
        if (sensorKind == RadarOrientationSensorKind.None) {
            return RadarOrientationStatus.SensorUnavailable
        }
        if (accuracy == RadarSensorAccuracy.Unavailable) {
            return RadarOrientationStatus.SensorUnavailable
        }
        if (sensorKind == RadarOrientationSensorKind.GameRotationVector) {
            return RadarOrientationStatus.RelativeOnly
        }
        if (accuracy == RadarSensorAccuracy.Low || accuracy == RadarSensorAccuracy.Unreliable) {
            return RadarOrientationStatus.AccuracyLow
        }
        return if (hasMagneticCorrection) {
            RadarOrientationStatus.PointingAssistActive
        } else {
            RadarOrientationStatus.MagneticCorrectionUnavailable
        }
    }

    fun shouldShowFigureEightCalibrationHint(
        sensorKind: RadarOrientationSensorKind,
        accuracy: RadarSensorAccuracy,
        forceShow: Boolean = false
    ): Boolean {
        if (forceShow) return true
        return canDriveCompassReference(sensorKind) &&
            (accuracy == RadarSensorAccuracy.Low || accuracy == RadarSensorAccuracy.Unreliable)
    }

    fun transientHintFor(
        hasFocusedPass: Boolean,
        chromeVisible: Boolean,
        pointingAssistVisible: Boolean,
        overlayOpen: Boolean,
        sensorKind: RadarOrientationSensorKind,
        accuracy: RadarSensorAccuracy,
        forceCalibrationHint: Boolean
    ): RadarTransientHint {
        if (!chromeVisible || overlayOpen) return RadarTransientHint.None
        if (
            shouldShowFigureEightCalibrationHint(
                sensorKind = sensorKind,
                accuracy = accuracy,
                forceShow = forceCalibrationHint
            ) &&
            (!pointingAssistVisible || forceCalibrationHint)
        ) {
            return RadarTransientHint.FigureEightCalibration
        }
        return if (hasFocusedPass && pointingAssistVisible) {
            RadarTransientHint.PointingAssist
        } else {
            RadarTransientHint.None
        }
    }

    fun normalizePointing(azimuthDegrees: Double, elevationDegrees: Double): RadarPointing {
        return RadarPointing(
            azimuthDegrees = normalizeDegrees(azimuthDegrees),
            elevationDegrees = elevationDegrees.coerceIn(0.0, 90.0)
        )
    }

    fun canDriveCompassReference(sensorKind: RadarOrientationSensorKind): Boolean {
        return sensorKind == RadarOrientationSensorKind.RotationVector ||
            sensorKind == RadarOrientationSensorKind.GeomagneticRotationVector
    }

    fun referenceAzimuthFor(
        sensorKind: RadarOrientationSensorKind,
        azimuthDegrees: Double
    ): Double? {
        return if (canDriveCompassReference(sensorKind)) {
            normalizeDegrees(azimuthDegrees)
        } else {
            null
        }
    }

    fun shouldListen(active: Boolean, resumed: Boolean): Boolean {
        return active && resumed
    }

    fun shouldPublishSnapshot(
        updateMode: RadarUpdateMode,
        eventElapsedRealtimeMillis: Long,
        lastPublishedElapsedRealtimeMillis: Long?
    ): Boolean {
        val publishInterval = RadarPowerPolicy.orientationPublishIntervalMillis(updateMode)
            ?: return true
        val lastPublished = lastPublishedElapsedRealtimeMillis ?: return true
        return eventElapsedRealtimeMillis - lastPublished >= publishInterval
    }

    fun accuracyFromSensorManagerStatus(status: Int): RadarSensorAccuracy {
        return when (status) {
            3 -> RadarSensorAccuracy.High
            2 -> RadarSensorAccuracy.Medium
            1 -> RadarSensorAccuracy.Low
            0 -> RadarSensorAccuracy.Unreliable
            else -> RadarSensorAccuracy.Unavailable
        }
    }

    fun applyMagneticDeclination(azimuthDegrees: Double, declinationDegrees: Double): Double {
        return normalizeDegrees(azimuthDegrees + declinationDegrees)
    }

    private fun normalizeDegrees(degrees: Double): Double {
        return ((degrees % 360.0) + 360.0) % 360.0
    }
}

object RadarWristOrientationPolicy {

    fun systemWristSide(storedValue: String?): RadarWristSide? {
        return when (storedValue) {
            "0", "1" -> RadarWristSide.LEFT
            "2", "3" -> RadarWristSide.RIGHT
            else -> null
        }
    }

    fun resolveForwardEdge(
        forwardAxis: RadarForwardAxis,
        systemWristSide: RadarWristSide?,
        fallbackWristSide: RadarWristSide
    ): RadarPhysicalEdge {
        if (forwardAxis == RadarForwardAxis.SCREEN_TOP) {
            return RadarPhysicalEdge.SCREEN_TOP
        }
        return when (systemWristSide ?: fallbackWristSide) {
            RadarWristSide.LEFT -> RadarPhysicalEdge.SCREEN_RIGHT
            RadarWristSide.RIGHT -> RadarPhysicalEdge.SCREEN_LEFT
        }
    }
}

object RadarPlotRotationPolicy {

    fun rotationDegrees(
        mode: RadarPlotOrientationMode,
        forwardEdge: RadarPhysicalEdge,
        renderedReferenceAzimuthDegrees: Float?
    ): Float {
        val heading = when (mode) {
            RadarPlotOrientationMode.TrackingForward -> renderedReferenceAzimuthDegrees ?: 0f
            RadarPlotOrientationMode.NorthReference -> 0f
        }
        return forwardEdge.visualOffsetDegrees - heading
    }
}

object RadarHeadingSmoothingPolicy {

    const val adaptiveMinCutoffHz: Double = 1.2
    const val adaptiveBeta: Double = 0.08
    const val adaptiveDerivativeCutoffHz: Double = 1.0
    const val adaptiveMinDeltaMillis: Long = 16L
    const val adaptiveMaxDeltaMillis: Long = 200L
    const val adaptiveSnapDeltaDegrees: Float = 120f
    const val adaptiveFrameTweenMillis: Int = 48

    fun targetDisplayAzimuthDegrees(
        currentDisplayDegrees: Float,
        rawReferenceAzimuthDegrees: Double,
        updateMode: RadarUpdateMode
    ): Float {
        val normalizedTarget = normalizedDisplayAzimuthDegrees(
            rawReferenceAzimuthDegrees.toFloat()
        ).toFloat()
        if (updateMode == RadarUpdateMode.AmbientOneHz) return normalizedTarget

        val currentNormalized = normalizedDisplayAzimuthDegrees(currentDisplayDegrees)
        val delta = shortestDeltaDegrees(
            fromDegrees = currentNormalized,
            toDegrees = normalizedTarget.toDouble()
        )
        return currentDisplayDegrees + delta.toFloat()
    }

    fun adaptiveDisplayTarget(
        previous: RadarHeadingFilterState,
        rawReferenceAzimuthDegrees: Double,
        sampleElapsedRealtimeMillis: Long,
        updateMode: RadarUpdateMode
    ): RadarHeadingFilterResult {
        if (updateMode == RadarUpdateMode.AmbientOneHz) {
            val normalized = normalizedDisplayAzimuthDegrees(
                rawReferenceAzimuthDegrees.toFloat()
            ).toFloat()
            return RadarHeadingFilterResult(
                state = RadarHeadingFilterState(
                    displayDegrees = normalized,
                    velocityDegreesPerSecond = 0f,
                    lastSampleElapsedRealtimeMillis = sampleElapsedRealtimeMillis,
                    initialized = true
                ),
                targetDegrees = normalized,
                snap = true
            )
        }

        val currentDisplay = if (previous.initialized) {
            previous.displayDegrees
        } else {
            normalizedDisplayAzimuthDegrees(rawReferenceAzimuthDegrees.toFloat()).toFloat()
        }
        val rawTarget = targetDisplayAzimuthDegrees(
            currentDisplayDegrees = currentDisplay,
            rawReferenceAzimuthDegrees = rawReferenceAzimuthDegrees,
            updateMode = RadarUpdateMode.Interactive
        )
        if (!previous.initialized) {
            return RadarHeadingFilterResult(
                state = RadarHeadingFilterState(
                    displayDegrees = rawTarget,
                    velocityDegreesPerSecond = 0f,
                    lastSampleElapsedRealtimeMillis = sampleElapsedRealtimeMillis,
                    initialized = true
                ),
                targetDegrees = rawTarget,
                snap = true
            )
        }

        val delta = rawTarget - previous.displayDegrees
        if (kotlin.math.abs(delta) >= adaptiveSnapDeltaDegrees) {
            return RadarHeadingFilterResult(
                state = RadarHeadingFilterState(
                    displayDegrees = rawTarget,
                    velocityDegreesPerSecond = 0f,
                    lastSampleElapsedRealtimeMillis = sampleElapsedRealtimeMillis,
                    initialized = true
                ),
                targetDegrees = rawTarget,
                snap = true
            )
        }

        val deltaSeconds = deltaSecondsFor(
            sampleElapsedRealtimeMillis = sampleElapsedRealtimeMillis,
            lastSampleElapsedRealtimeMillis = previous.lastSampleElapsedRealtimeMillis
        )
        val rawVelocity = delta / deltaSeconds.toFloat()
        val velocityAlpha = smoothingAlpha(
            deltaSeconds = deltaSeconds,
            cutoffHz = adaptiveDerivativeCutoffHz
        )
        val filteredVelocity = lerpDegrees(
            from = previous.velocityDegreesPerSecond,
            to = rawVelocity,
            alpha = velocityAlpha
        )
        val cutoff = adaptiveMinCutoffHz + adaptiveBeta * kotlin.math.abs(filteredVelocity)
        val headingAlpha = smoothingAlpha(
            deltaSeconds = deltaSeconds,
            cutoffHz = cutoff
        )
        val display = lerpDegrees(
            from = previous.displayDegrees,
            to = rawTarget,
            alpha = headingAlpha
        )

        return RadarHeadingFilterResult(
            state = RadarHeadingFilterState(
                displayDegrees = display,
                velocityDegreesPerSecond = filteredVelocity,
                lastSampleElapsedRealtimeMillis = sampleElapsedRealtimeMillis,
                initialized = true
            ),
            targetDegrees = display,
            snap = false
        )
    }

    fun normalizedDisplayAzimuthDegrees(displayDegrees: Float): Double {
        return ((displayDegrees.toDouble() % 360.0) + 360.0) % 360.0
    }

    fun renderedDisplayAzimuthDegrees(
        rawReferenceAzimuthDegrees: Double,
        animatedDisplayDegrees: Float,
        updateMode: RadarUpdateMode
    ): Float {
        return if (updateMode == RadarUpdateMode.AmbientOneHz) {
            normalizedDisplayAzimuthDegrees(rawReferenceAzimuthDegrees.toFloat()).toFloat()
        } else {
            animatedDisplayDegrees
        }
    }

    private fun shortestDeltaDegrees(fromDegrees: Double, toDegrees: Double): Double {
        val delta = ((toDegrees - fromDegrees + 540.0) % 360.0) - 180.0
        return if (delta == -180.0) 180.0 else delta
    }

    private fun smoothingAlpha(deltaSeconds: Double, cutoffHz: Double): Float {
        val tau = 1.0 / (2.0 * kotlin.math.PI * cutoffHz)
        return (1.0 / (1.0 + tau / deltaSeconds)).toFloat()
    }

    private fun lerpDegrees(from: Float, to: Float, alpha: Float): Float {
        return from + (to - from) * alpha.coerceIn(0f, 1f)
    }

    private fun deltaSecondsFor(
        sampleElapsedRealtimeMillis: Long,
        lastSampleElapsedRealtimeMillis: Long?
    ): Double {
        val last = lastSampleElapsedRealtimeMillis ?: return adaptiveMinDeltaMillis / 1_000.0
        val deltaMillis = (sampleElapsedRealtimeMillis - last)
            .coerceIn(adaptiveMinDeltaMillis, adaptiveMaxDeltaMillis)
        return deltaMillis / 1_000.0
    }
}

object RadarPositionSmoothingPolicy {

    const val markerTweenMillis: Int = 220
    const val markerSnapDistanceFraction: Float = 1.25f

    fun displayTarget(
        previous: RadarPositionFilterState,
        target: RadarDisplayPoint?,
        updateMode: RadarUpdateMode
    ): RadarPositionFilterResult {
        if (target == null) {
            return RadarPositionFilterResult(
                state = RadarPositionFilterState(),
                target = null,
                snap = true
            )
        }

        if (!previous.initialized || updateMode == RadarUpdateMode.AmbientOneHz) {
            return RadarPositionFilterResult(
                state = RadarPositionFilterState(display = target, initialized = true),
                target = target,
                snap = true
            )
        }

        val distance = kotlin.math.hypot(
            (target.x - previous.display.x).toDouble(),
            (target.y - previous.display.y).toDouble()
        ).toFloat()
        val snap = distance >= markerSnapDistanceFraction

        return RadarPositionFilterResult(
            state = RadarPositionFilterState(display = target, initialized = true),
            target = target,
            snap = snap
        )
    }
}
