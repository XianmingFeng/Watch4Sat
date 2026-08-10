package com.xianming.watch4sat.wear.map

object MapBadgeVisibilityPolicy {
    const val TransientBadgeMillis: Long = 3_000L

    fun isTransientStatus(status: MapTileStatus): Boolean {
        return status == MapTileStatus.OfflineWorld ||
            status == MapTileStatus.OsmLoaded ||
            status == MapTileStatus.LoadingOsm
    }

    fun shouldShow(status: MapTileStatus, elapsedMillis: Long): Boolean {
        return !isTransientStatus(status) || elapsedMillis < TransientBadgeMillis
    }
}
