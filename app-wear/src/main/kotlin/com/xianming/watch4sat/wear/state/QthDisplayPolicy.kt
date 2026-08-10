package com.xianming.watch4sat.wear.state

sealed interface QthDisplayUi {
    data object Unset : QthDisplayUi
    data class Saved(
        val locator: String,
        val coordinates: String
    ) : QthDisplayUi
}

object QthDisplayPolicy {
    fun display(
        hasStationLocation: Boolean,
        stationQth: String?,
        stationCoordinates: String
    ): QthDisplayUi {
        val savedQth = stationQth.takeIf { hasStationLocation }
        return if (savedQth == null) {
            QthDisplayUi.Unset
        } else {
            QthDisplayUi.Saved(
                locator = savedQth,
                coordinates = stationCoordinates
            )
        }
    }
}
