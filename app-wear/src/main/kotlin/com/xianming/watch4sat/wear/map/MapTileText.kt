package com.xianming.watch4sat.wear.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.xianming.watch4sat.R

@Composable
fun MapTileStatus.resolveText(): String {
    return stringResource(
        when (this) {
            MapTileStatus.OfflineWorld -> R.string.map_status_offline_world
            MapTileStatus.OsmLoaded -> R.string.map_status_osm_loaded
            MapTileStatus.OsmUnavailableOffline ->
                R.string.map_status_osm_unavailable_offline
            MapTileStatus.LoadingOsm -> R.string.map_status_loading_osm
            MapTileStatus.NoValidatedNetwork -> R.string.map_status_no_validated_network
            MapTileStatus.OsmUnavailable -> R.string.map_status_osm_unavailable
        }
    )
}
