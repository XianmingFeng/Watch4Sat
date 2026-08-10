package com.xianming.watch4sat.location

import com.xianming.watch4sat.domain.model.LocationSource
import com.xianming.watch4sat.domain.model.StationLocation
import com.xianming.watch4sat.domain.qth.MaidenheadLocator
import java.util.Locale

internal fun LocationFix.toStationLocation(): StationLocation {
    val qth = MaidenheadLocator
        .fromCoordinates(latitude = latitude, longitude = longitude)
        ?.uppercase(Locale.US)
    return StationLocation(
        latitude = latitude,
        longitude = longitude,
        altitudeMeters = altitudeMeters,
        qthLocator = qth,
        timestampMillis = timestampMillis,
        source = provider.toLocationSource(),
        accuracyMeters = accuracyMeters
    )
}

private fun LocationFixProvider.toLocationSource(): LocationSource {
    return when (this) {
        LocationFixProvider.FUSED -> LocationSource.FUSED
        LocationFixProvider.GPS -> LocationSource.GPS
        LocationFixProvider.NETWORK,
        LocationFixProvider.PASSIVE -> LocationSource.NETWORK
    }
}
