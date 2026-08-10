package com.xianming.watch4sat.qth

import com.xianming.watch4sat.domain.model.LocationSource
import com.xianming.watch4sat.domain.model.StationLocation
import com.xianming.watch4sat.domain.qth.MaidenheadLocator
import java.util.Locale

object QthInputValidator {
    private val QthRegex = "[A-R][A-R][0-9][0-9][A-X][A-X]".toRegex()

    fun normalize(input: String): String {
        return input.uppercase(Locale.US).filter { it.isLetterOrDigit() }
    }

    fun isValid(input: String): Boolean {
        return QthRegex.matches(normalize(input))
    }

    fun toStationLocation(
        input: String,
        timestampMillis: Long = System.currentTimeMillis()
    ): StationLocation? {
        val normalized = normalize(input)
        if (!QthRegex.matches(normalized)) return null
        val coordinates = MaidenheadLocator.toCoordinates(normalized) ?: return null
        return StationLocation(
            latitude = coordinates.latitude,
            longitude = coordinates.longitude,
            altitudeMeters = 0.0,
            qthLocator = normalized,
            timestampMillis = timestampMillis,
            source = LocationSource.MANUAL_QTH
        )
    }
}
