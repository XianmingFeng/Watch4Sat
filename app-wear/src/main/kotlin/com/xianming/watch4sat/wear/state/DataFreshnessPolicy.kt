package com.xianming.watch4sat.wear.state

import com.xianming.watch4sat.domain.freshness.TleFreshnessAssessment

object DataFreshnessPolicy {
    const val tleStaleAfterMillis: Long = 24 * 60 * 60_000L
    const val transmitterStaleAfterMillis: Long = 7 * 24 * 60 * 60_000L

    fun shouldRefresh(
        nowMillis: Long,
        lastSatelliteDataUpdateMillis: Long?,
        lastTransmitterDataUpdateMillis: Long?,
        autoDataFreshnessEnabled: Boolean,
        refreshInFlight: Boolean,
        foreground: Boolean,
        tleFreshness: TleFreshnessAssessment? = null
    ): DataFreshnessDecision {
        if (!autoDataFreshnessEnabled || refreshInFlight || !foreground) {
            return DataFreshnessDecision(refreshSatellites = false, refreshTransmitters = false)
        }
        val satelliteNeedsRefresh = tleFreshness?.shouldRefresh ?: isMissingOrStale(
            timestampMillis = lastSatelliteDataUpdateMillis,
            nowMillis = nowMillis,
            staleAfterMillis = tleStaleAfterMillis
        )
        return DataFreshnessDecision(
            refreshSatellites = satelliteNeedsRefresh,
            refreshTransmitters = isMissingOrStale(
                timestampMillis = lastTransmitterDataUpdateMillis,
                nowMillis = nowMillis,
                staleAfterMillis = transmitterStaleAfterMillis
            )
        )
    }

    fun isMissingOrStale(
        timestampMillis: Long?,
        nowMillis: Long,
        staleAfterMillis: Long
    ): Boolean {
        val timestamp = timestampMillis ?: return true
        if (timestamp > nowMillis) return true
        return nowMillis - timestamp >= staleAfterMillis
    }
}

data class DataFreshnessDecision(
    val refreshSatellites: Boolean,
    val refreshTransmitters: Boolean
) {
    val shouldRefreshAny: Boolean = refreshSatellites || refreshTransmitters
}
