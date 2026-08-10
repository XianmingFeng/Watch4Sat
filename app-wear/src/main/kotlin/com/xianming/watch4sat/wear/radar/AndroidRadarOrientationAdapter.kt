package com.xianming.watch4sat.wear.radar

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import android.view.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.xianming.watch4sat.domain.model.StationLocation

@Composable
fun rememberRadarOrientationState(
    station: StationLocation?,
    active: Boolean,
    forwardEdge: RadarPhysicalEdge,
    updateMode: RadarUpdateMode = RadarUpdateMode.Interactive
): State<RadarOrientationSnapshot> {
    val context = LocalContext.current.applicationContext
    val displayRotation = LocalView.current.display?.rotation
        .toRadarDisplayRotation()
    val lifecycleOwner = LocalLifecycleOwner.current
    val state = remember(forwardEdge, displayRotation) {
        mutableStateOf(
            RadarOrientationSnapshot(
                sensorKind = RadarOrientationSensorKind.None,
                status = RadarOrientationStatus.NorthReference,
                accuracy = RadarSensorAccuracy.Unavailable
            )
        )
    }
    val resumed = remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            resumed.value = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(
        context,
        station,
        active,
        forwardEdge,
        displayRotation,
        updateMode,
        resumed.value
    ) {
        if (!RadarOrientationPolicy.shouldListen(active = active, resumed = resumed.value)) {
            state.value = state.value.copy(
                status = RadarOrientationStatus.NorthReference,
                pointing = null,
                referenceAzimuthDegrees = null
            )
            onDispose {}
        } else {
            val adapter = AndroidRadarOrientationAdapter(
                sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager,
                station = station,
                forwardEdge = forwardEdge,
                displayRotation = displayRotation,
                updateMode = updateMode,
                onSnapshot = { snapshot -> state.value = snapshot }
            )
            adapter.start()
            onDispose {
                adapter.stop()
            }
        }
    }
    return state
}

internal class AndroidRadarOrientationAdapter(
    private val sensorManager: SensorManager,
    private val station: StationLocation?,
    private val forwardEdge: RadarPhysicalEdge,
    private val displayRotation: RadarDisplayRotation,
    private val updateMode: RadarUpdateMode,
    private val onSnapshot: (RadarOrientationSnapshot) -> Unit
) : SensorEventListener {

    private val rotationMatrix = FloatArray(9)
    private val remappedMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private val magneticDeclinationDegrees = station?.let {
        GeomagneticField(
            it.latitude.toFloat(),
            it.longitude.toFloat(),
            it.altitudeMeters.toFloat(),
            System.currentTimeMillis()
        ).declination.toDouble()
    }
    private var sensorKind: RadarOrientationSensorKind = RadarOrientationSensorKind.None
    private var accuracy: RadarSensorAccuracy = RadarSensorAccuracy.Unavailable
    private var lastPublishedElapsedRealtimeMillis: Long? = null

    fun start() {
        val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val geomagneticRotationVector = sensorManager.getDefaultSensor(
            Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR
        )
        val gameRotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        sensorKind = RadarOrientationPolicy.chooseSensor(
            hasRotationVector = rotationVector != null,
            hasGeomagneticRotationVector = geomagneticRotationVector != null,
            hasGameRotationVector = gameRotationVector != null
        )
        val sensor = when (sensorKind) {
            RadarOrientationSensorKind.RotationVector -> rotationVector
            RadarOrientationSensorKind.GeomagneticRotationVector -> geomagneticRotationVector
            RadarOrientationSensorKind.GameRotationVector -> gameRotationVector
            RadarOrientationSensorKind.None -> null
        }
        if (sensor == null) {
            publishUnavailable()
            return
        }
        val sensorDelay = RadarPowerPolicy.orientationSensorDelayMicros(updateMode)
        val registered = sensorManager.registerListener(this, sensor, sensorDelay)
        if (!registered) {
            publishUnavailable()
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val eventElapsedRealtimeMillis = SystemClock.elapsedRealtime()
        if (!RadarOrientationPolicy.shouldPublishSnapshot(
                updateMode = updateMode,
                eventElapsedRealtimeMillis = eventElapsedRealtimeMillis,
                lastPublishedElapsedRealtimeMillis = lastPublishedElapsedRealtimeMillis
            )
        ) {
            return
        }
        lastPublishedElapsedRealtimeMillis = eventElapsedRealtimeMillis
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        val axes = RadarOrientationPolicy.remapAxesFor(
            forwardEdge = forwardEdge,
            displayRotation = displayRotation
        )
        SensorManager.remapCoordinateSystem(
            rotationMatrix,
            axes.deviceXDestinationAxis.toSensorManagerAxis(),
            axes.deviceYDestinationAxis.toSensorManagerAxis(),
            remappedMatrix
        )
        SensorManager.getOrientation(remappedMatrix, orientationAngles)
        val azimuth = RadarOrientationPolicy.applyMagneticDeclination(
            azimuthDegrees = Math.toDegrees(orientationAngles[0].toDouble()),
            declinationDegrees = magneticDeclinationDegrees ?: 0.0
        )
        val elevation = Math.toDegrees(-orientationAngles[1].toDouble())
        val pointing = RadarOrientationPolicy.normalizePointing(
            azimuthDegrees = azimuth,
            elevationDegrees = elevation
        )
        val referenceAzimuth = RadarOrientationPolicy.referenceAzimuthFor(
            sensorKind = sensorKind,
            azimuthDegrees = azimuth
        )
        val hasMagneticCorrection = magneticDeclinationDegrees != null &&
            sensorKind != RadarOrientationSensorKind.GameRotationVector
        onSnapshot(
            RadarOrientationSnapshot(
                sensorKind = sensorKind,
                status = RadarOrientationPolicy.statusFor(
                    sensorKind = sensorKind,
                    accuracy = accuracy,
                    hasMagneticCorrection = hasMagneticCorrection
                ),
                accuracy = accuracy,
                pointing = pointing,
                referenceAzimuthDegrees = referenceAzimuth
            )
        )
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        this.accuracy = RadarOrientationPolicy.accuracyFromSensorManagerStatus(accuracy)
    }

    private fun publishUnavailable() {
        onSnapshot(
            RadarOrientationSnapshot(
                sensorKind = RadarOrientationSensorKind.None,
                status = RadarOrientationStatus.SensorUnavailable,
                accuracy = RadarSensorAccuracy.Unavailable
            )
        )
    }
}

private fun Int?.toRadarDisplayRotation(): RadarDisplayRotation {
    return when (this) {
        Surface.ROTATION_90 -> RadarDisplayRotation.ROTATION_90
        Surface.ROTATION_180 -> RadarDisplayRotation.ROTATION_180
        Surface.ROTATION_270 -> RadarDisplayRotation.ROTATION_270
        else -> RadarDisplayRotation.ROTATION_0
    }
}

internal fun RadarSensorAxis.toSensorManagerAxis(): Int {
    return when (this) {
        RadarSensorAxis.PositiveX -> SensorManager.AXIS_X
        RadarSensorAxis.PositiveY -> SensorManager.AXIS_Y
        RadarSensorAxis.PositiveZ -> SensorManager.AXIS_Z
        RadarSensorAxis.NegativeX -> SensorManager.AXIS_MINUS_X
        RadarSensorAxis.NegativeY -> SensorManager.AXIS_MINUS_Y
        RadarSensorAxis.NegativeZ -> SensorManager.AXIS_MINUS_Z
    }
}
