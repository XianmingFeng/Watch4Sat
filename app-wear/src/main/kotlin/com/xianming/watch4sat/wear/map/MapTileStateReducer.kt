package com.xianming.watch4sat.wear.map

import com.xianming.watch4sat.data.settings.MapTileMode

enum class OsmTileEvent {
    Loading,
    Success,
    NoValidatedNetwork,
    Unavailable
}

data class MapTileDisplayState(
    val status: MapTileStatus,
    val useOsm: Boolean,
    val showOfflineWorld: Boolean,
    val showOsmAttribution: Boolean
)

enum class MapTileStatus {
    OfflineWorld,
    OsmLoaded,
    OsmUnavailableOffline,
    LoadingOsm,
    NoValidatedNetwork,
    OsmUnavailable
}

object MapTileStateReducer {

    fun reduce(
        mode: MapTileMode,
        hasValidatedNetwork: Boolean,
        event: OsmTileEvent
    ): MapTileDisplayState {
        return when (mode) {
            MapTileMode.OFFLINE_WORLD -> offline(MapTileStatus.OfflineWorld)
            MapTileMode.OSM_ONLY -> osmOnly(hasValidatedNetwork, event)
            MapTileMode.AUTO -> auto(hasValidatedNetwork, event)
        }
    }

    private fun auto(
        hasValidatedNetwork: Boolean,
        event: OsmTileEvent
    ): MapTileDisplayState {
        if (!hasValidatedNetwork || event == OsmTileEvent.NoValidatedNetwork) {
            return offline(MapTileStatus.OfflineWorld)
        }
        return when (event) {
            OsmTileEvent.Success -> osm(MapTileStatus.OsmLoaded)
            OsmTileEvent.Unavailable -> offline(MapTileStatus.OsmUnavailableOffline)
            OsmTileEvent.Loading -> osm(MapTileStatus.LoadingOsm)
            OsmTileEvent.NoValidatedNetwork -> offline(MapTileStatus.OfflineWorld)
        }
    }

    private fun osmOnly(
        hasValidatedNetwork: Boolean,
        event: OsmTileEvent
    ): MapTileDisplayState {
        if (!hasValidatedNetwork || event == OsmTileEvent.NoValidatedNetwork) {
            return osm(MapTileStatus.NoValidatedNetwork)
        }
        return when (event) {
            OsmTileEvent.Success -> osm(MapTileStatus.OsmLoaded)
            OsmTileEvent.Unavailable -> osm(MapTileStatus.OsmUnavailable)
            OsmTileEvent.Loading -> osm(MapTileStatus.LoadingOsm)
            OsmTileEvent.NoValidatedNetwork -> osm(MapTileStatus.NoValidatedNetwork)
        }
    }

    private fun osm(status: MapTileStatus): MapTileDisplayState {
        return MapTileDisplayState(
            status = status,
            useOsm = true,
            showOfflineWorld = false,
            showOsmAttribution = true
        )
    }

    private fun offline(status: MapTileStatus): MapTileDisplayState {
        return MapTileDisplayState(
            status = status,
            useOsm = false,
            showOfflineWorld = true,
            showOsmAttribution = false
        )
    }
}
