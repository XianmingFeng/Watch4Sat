package com.xianming.watch4sat.wear.radar

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.xianming.watch4sat.R
import com.xianming.watch4sat.domain.freshness.TleFreshnessAssessment
import com.xianming.watch4sat.domain.freshness.TleFreshnessSeverity
import com.xianming.watch4sat.domain.model.LocationSource
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class RadarTextKey {
    NoFocusedPass,
    Unknown,
    UnknownTitle,
    SemanticsSummary,
    PassTiming,
    AosIn,
    LosIn,
    Ended,
    MinutesShort,
    SecondsShort,
    MinutesSecondsShort,
    SecondsCompact,
    TimeStatus,
    AngleLine,
    NoTransmitter,
    TransmitterFallback,
    TransmitterShort,
    NoFrequency,
    NoBaseFrequency,
    NoDownlink,
    NoUplink,
    Missing,
    DopplerWaiting,
    DopplerWaitingFrequency,
    DownlinkPrefix,
    UplinkPrefix,
    CorrectedFrequency,
    Summary,
    Range,
    QthSourceValue,
    LinkBoth,
    FrequencyMhz,
    FrequencyKhz,
    RadioFrequencyMhz,
    RadioFrequencyKhz,
    NoPass,
    PointingGood,
    MaxAtTca,
    MaxUnknown,
    LosAt,
    LosUnknown,
    Today,
    Tomorrow,
    DatePattern,
    JustNow,
    MinutesAgo,
    HoursAgo,
    DaysAgo,
    RawDownlink,
    RawUplink,
    RadioSource,
    TleAge,
    Sensor,
    QthSource,
    Calibration,
    Catalog,
    Satnogs,
    TleMissing,
    TleCheckDeviceTime,
    TleAgeUnknown,
    TleFresh,
    TleStale,
    TleVeryStale,
    SensorHigh,
    SensorMedium,
    SensorLow,
    SensorUnavailable,
    RotationVector,
    GeomagneticRotationVector,
    GameRotationVector,
    Unavailable,
    HighAccuracy,
    MediumAccuracy,
    LowAccuracy,
    Calibrate,
    SourceGps,
    SourceNetwork,
    SourceFused,
    SourceManual,
    NorthReference,
    PointingAssist,
    AccuracyLow,
    MagneticCorrectionUnavailable,
    RelativePointing,
    Downlink,
    Uplink
}

class RadarTextCatalog(
    private val values: Map<RadarTextKey, String>
) {
    private val locale = Locale.US

    fun text(key: RadarTextKey, vararg arguments: Any): String {
        val template = checkNotNull(values[key]) { "Missing Radar text for $key" }
        return if (arguments.isEmpty()) {
            template
        } else {
            String.format(locale, template, *arguments)
        }
    }

    fun orientationStatus(status: RadarOrientationStatus): String {
        return text(
            when (status) {
                RadarOrientationStatus.NorthReference -> RadarTextKey.NorthReference
                RadarOrientationStatus.PointingAssistActive -> RadarTextKey.PointingAssist
                RadarOrientationStatus.SensorUnavailable -> RadarTextKey.SensorUnavailable
                RadarOrientationStatus.AccuracyLow -> RadarTextKey.AccuracyLow
                RadarOrientationStatus.MagneticCorrectionUnavailable ->
                    RadarTextKey.MagneticCorrectionUnavailable
                RadarOrientationStatus.RelativeOnly -> RadarTextKey.RelativePointing
            }
        )
    }

    fun sensorKind(kind: RadarOrientationSensorKind): String {
        return text(
            when (kind) {
                RadarOrientationSensorKind.RotationVector -> RadarTextKey.RotationVector
                RadarOrientationSensorKind.GeomagneticRotationVector ->
                    RadarTextKey.GeomagneticRotationVector
                RadarOrientationSensorKind.GameRotationVector -> RadarTextKey.GameRotationVector
                RadarOrientationSensorKind.None -> RadarTextKey.Unavailable
            }
        )
    }

    fun sensorAccuracy(accuracy: RadarSensorAccuracy): String {
        return text(
            when (accuracy) {
                RadarSensorAccuracy.High -> RadarTextKey.HighAccuracy
                RadarSensorAccuracy.Medium -> RadarTextKey.MediumAccuracy
                RadarSensorAccuracy.Low -> RadarTextKey.LowAccuracy
                RadarSensorAccuracy.Unreliable -> RadarTextKey.Calibrate
                RadarSensorAccuracy.Unavailable -> RadarTextKey.Unavailable
            }
        )
    }

    fun sensorQuality(accuracy: RadarSensorAccuracy): String {
        return text(
            when (accuracy) {
                RadarSensorAccuracy.High -> RadarTextKey.SensorHigh
                RadarSensorAccuracy.Medium -> RadarTextKey.SensorMedium
                RadarSensorAccuracy.Low,
                RadarSensorAccuracy.Unreliable -> RadarTextKey.SensorLow
                RadarSensorAccuracy.Unavailable -> RadarTextKey.SensorUnavailable
            }
        )
    }

    fun locationSource(source: LocationSource): String {
        return text(
            when (source) {
                LocationSource.GPS -> RadarTextKey.SourceGps
                LocationSource.NETWORK -> RadarTextKey.SourceNetwork
                LocationSource.FUSED -> RadarTextKey.SourceFused
                LocationSource.MANUAL_QTH,
                LocationSource.MANUAL_COORDINATES -> RadarTextKey.SourceManual
            }
        )
    }

    fun tleQuality(assessment: TleFreshnessAssessment): String {
        val key = when {
            assessment.clockSkewDetected -> RadarTextKey.TleCheckDeviceTime
            assessment.metadataMissing -> RadarTextKey.TleAgeUnknown
            assessment.severity == TleFreshnessSeverity.FRESH -> RadarTextKey.TleFresh
            assessment.severity == TleFreshnessSeverity.STALE -> RadarTextKey.TleStale
            else -> RadarTextKey.TleVeryStale
        }
        return text(key)
    }

    fun age(ageMillis: Long?): String {
        val age = ageMillis ?: return text(RadarTextKey.Unknown)
        val minutes = age / 60_000L
        return when {
            minutes < 1L -> text(RadarTextKey.JustNow)
            minutes < 60L -> text(RadarTextKey.MinutesAgo, minutes)
            minutes < 24L * 60L -> text(RadarTextKey.HoursAgo, minutes / 60L)
            else -> text(RadarTextKey.DaysAgo, minutes / (24L * 60L))
        }
    }

    fun dateFormatter(): DateTimeFormatter {
        return DateTimeFormatter.ofPattern(text(RadarTextKey.DatePattern), locale)
    }
}

@Composable
internal fun radarTextCatalog(): RadarTextCatalog {
    return RadarTextCatalog(
        values = mapOf(
            RadarTextKey.NoFocusedPass to stringResource(R.string.radar_no_focused_pass),
            RadarTextKey.Unknown to stringResource(R.string.radar_unknown),
            RadarTextKey.UnknownTitle to stringResource(R.string.radar_unknown_title),
            RadarTextKey.SemanticsSummary to stringResource(R.string.radar_semantics_summary),
            RadarTextKey.PassTiming to stringResource(R.string.radar_pass_timing),
            RadarTextKey.AosIn to stringResource(R.string.radar_aos_in),
            RadarTextKey.LosIn to stringResource(R.string.radar_los_in),
            RadarTextKey.Ended to stringResource(R.string.radar_ended),
            RadarTextKey.MinutesShort to stringResource(R.string.radar_minutes_short),
            RadarTextKey.SecondsShort to stringResource(R.string.radar_seconds_short),
            RadarTextKey.MinutesSecondsShort to stringResource(R.string.radar_minutes_seconds_short),
            RadarTextKey.SecondsCompact to stringResource(R.string.radar_seconds_compact),
            RadarTextKey.TimeStatus to stringResource(R.string.radar_time_status),
            RadarTextKey.AngleLine to stringResource(R.string.radar_angle_line),
            RadarTextKey.NoTransmitter to stringResource(R.string.radar_no_transmitter),
            RadarTextKey.TransmitterFallback to stringResource(R.string.radar_transmitter_fallback),
            RadarTextKey.TransmitterShort to stringResource(R.string.radar_transmitter_short),
            RadarTextKey.NoFrequency to stringResource(R.string.radar_no_frequency),
            RadarTextKey.NoBaseFrequency to stringResource(R.string.radar_no_base_frequency),
            RadarTextKey.NoDownlink to stringResource(R.string.radar_no_downlink),
            RadarTextKey.NoUplink to stringResource(R.string.radar_no_uplink),
            RadarTextKey.Missing to stringResource(R.string.radar_missing),
            RadarTextKey.DopplerWaiting to stringResource(R.string.radar_doppler_waiting),
            RadarTextKey.DopplerWaitingFrequency to
                stringResource(R.string.radar_doppler_waiting_frequency),
            RadarTextKey.DownlinkPrefix to stringResource(R.string.radar_downlink_prefix),
            RadarTextKey.UplinkPrefix to stringResource(R.string.radar_uplink_prefix),
            RadarTextKey.CorrectedFrequency to stringResource(R.string.radar_corrected_frequency),
            RadarTextKey.Summary to stringResource(R.string.radar_summary),
            RadarTextKey.Range to stringResource(R.string.radar_range),
            RadarTextKey.QthSourceValue to stringResource(R.string.radar_qth_source_value),
            RadarTextKey.LinkBoth to stringResource(R.string.radar_link_both),
            RadarTextKey.FrequencyMhz to stringResource(R.string.radar_frequency_mhz),
            RadarTextKey.FrequencyKhz to stringResource(R.string.radar_frequency_khz),
            RadarTextKey.RadioFrequencyMhz to
                stringResource(R.string.radar_radio_frequency_mhz),
            RadarTextKey.RadioFrequencyKhz to
                stringResource(R.string.radar_radio_frequency_khz),
            RadarTextKey.NoPass to stringResource(R.string.radar_no_pass),
            RadarTextKey.PointingGood to stringResource(R.string.radar_pointing_good),
            RadarTextKey.MaxAtTca to stringResource(R.string.radar_max_at_tca),
            RadarTextKey.MaxUnknown to stringResource(R.string.radar_max_unknown),
            RadarTextKey.LosAt to stringResource(R.string.radar_los_at),
            RadarTextKey.LosUnknown to stringResource(R.string.radar_los_unknown),
            RadarTextKey.Today to stringResource(R.string.radar_today),
            RadarTextKey.Tomorrow to stringResource(R.string.radar_tomorrow),
            RadarTextKey.DatePattern to stringResource(R.string.radar_date_pattern),
            RadarTextKey.JustNow to stringResource(R.string.radar_just_now),
            RadarTextKey.MinutesAgo to stringResource(R.string.radar_minutes_ago),
            RadarTextKey.HoursAgo to stringResource(R.string.radar_hours_ago),
            RadarTextKey.DaysAgo to stringResource(R.string.radar_days_ago),
            RadarTextKey.RawDownlink to stringResource(R.string.radar_raw_downlink),
            RadarTextKey.RawUplink to stringResource(R.string.radar_raw_uplink),
            RadarTextKey.RadioSource to stringResource(R.string.radar_radio_source),
            RadarTextKey.TleAge to stringResource(R.string.radar_tle_age),
            RadarTextKey.Sensor to stringResource(R.string.radar_sensor),
            RadarTextKey.QthSource to stringResource(R.string.radar_qth_source),
            RadarTextKey.Calibration to stringResource(R.string.radar_calibration),
            RadarTextKey.Catalog to stringResource(R.string.radar_catalog),
            RadarTextKey.Satnogs to stringResource(R.string.radar_satnogs),
            RadarTextKey.TleMissing to stringResource(R.string.radar_tle_missing),
            RadarTextKey.TleCheckDeviceTime to
                stringResource(R.string.radar_tle_check_device_time),
            RadarTextKey.TleAgeUnknown to stringResource(R.string.radar_tle_age_unknown),
            RadarTextKey.TleFresh to stringResource(R.string.radar_tle_fresh),
            RadarTextKey.TleStale to stringResource(R.string.radar_tle_stale),
            RadarTextKey.TleVeryStale to stringResource(R.string.radar_tle_very_stale),
            RadarTextKey.SensorHigh to stringResource(R.string.radar_sensor_high),
            RadarTextKey.SensorMedium to stringResource(R.string.radar_sensor_medium),
            RadarTextKey.SensorLow to stringResource(R.string.radar_sensor_low),
            RadarTextKey.SensorUnavailable to stringResource(R.string.radar_sensor_unavailable),
            RadarTextKey.RotationVector to stringResource(R.string.radar_rotation_vector),
            RadarTextKey.GeomagneticRotationVector to
                stringResource(R.string.radar_geomagnetic_rotation_vector),
            RadarTextKey.GameRotationVector to
                stringResource(R.string.radar_game_rotation_vector),
            RadarTextKey.Unavailable to stringResource(R.string.radar_unavailable),
            RadarTextKey.HighAccuracy to stringResource(R.string.radar_high_accuracy),
            RadarTextKey.MediumAccuracy to stringResource(R.string.radar_medium_accuracy),
            RadarTextKey.LowAccuracy to stringResource(R.string.radar_low_accuracy),
            RadarTextKey.Calibrate to stringResource(R.string.radar_calibrate),
            RadarTextKey.SourceGps to stringResource(R.string.radar_source_gps),
            RadarTextKey.SourceNetwork to stringResource(R.string.radar_source_network),
            RadarTextKey.SourceFused to stringResource(R.string.radar_source_fused),
            RadarTextKey.SourceManual to stringResource(R.string.radar_source_manual),
            RadarTextKey.NorthReference to stringResource(R.string.radar_north_reference),
            RadarTextKey.PointingAssist to stringResource(R.string.radar_pointing_assist),
            RadarTextKey.AccuracyLow to stringResource(R.string.radar_sensor_accuracy_low),
            RadarTextKey.MagneticCorrectionUnavailable to
                stringResource(R.string.radar_magnetic_correction_unavailable),
            RadarTextKey.RelativePointing to stringResource(R.string.radar_relative_pointing),
            RadarTextKey.Downlink to stringResource(R.string.radar_downlink),
            RadarTextKey.Uplink to stringResource(R.string.radar_uplink)
        )
    )
}
