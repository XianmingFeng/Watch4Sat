package com.xianming.watch4sat.wear.state

import com.xianming.watch4sat.domain.freshness.TleFreshnessAssessment
import com.xianming.watch4sat.wear.WatchRoute

data class DashboardRouteItem(
    val route: WatchRoute,
    val iconKey: String
) {
    val titleRes: Int = route.titleRes
}

object DashboardRouteItems {

    fun routes(): List<DashboardRouteItem> {
        return listOf(
            DashboardRouteItem(WatchRoute.Passes, "list"),
            DashboardRouteItem(WatchRoute.OrbitMap, "public"),
            DashboardRouteItem(WatchRoute.Qth, "place"),
            DashboardRouteItem(WatchRoute.Data, "cloud_download"),
            DashboardRouteItem(WatchRoute.Satellites, "star"),
            DashboardRouteItem(WatchRoute.Settings, "settings")
        )
    }
}

object DashboardDataStatusPolicy {
    fun model(
        satelliteCount: Int,
        selectedCount: Int,
        tleFreshness: TleFreshnessAssessment,
        isRefreshing: Boolean
    ): DashboardDataStatus = DashboardDataStatus(
        satelliteCount = satelliteCount,
        selectedCount = selectedCount,
        freshness = TleFreshnessUiPolicy.model(tleFreshness),
        isRefreshing = isRefreshing
    )
}

data class DashboardDataStatus(
    val satelliteCount: Int,
    val selectedCount: Int,
    val freshness: TleFreshnessUiModel,
    val isRefreshing: Boolean
)

object DataRefreshRequestPolicy {
    fun canStartRefresh(refreshInFlight: Boolean): Boolean = !refreshInFlight
}
