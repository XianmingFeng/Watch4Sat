package com.xianming.watch4sat.tile

import com.xianming.watch4sat.wear.state.ExactPassIdentity

enum class TileLaunchDestination {
    Dashboard,
    Radar,
    Qth,
    Data,
    Satellites
}

data class TileLaunchAction(
    val destination: TileLaunchDestination,
    val catalogNumber: Int? = null,
    val aosMillis: Long? = null,
    val losMillis: Long? = null
) {
    val exactPassIdentity: ExactPassIdentity? = ExactPassIdentity.from(
        catalogNumber = catalogNumber,
        aosMillis = aosMillis,
        losMillis = losMillis
    )
}

object TileLaunchPolicy {
    fun actionFor(
        kind: NextPassTileKind,
        catalogNumber: Int? = null,
        aosMillis: Long? = null,
        losMillis: Long? = null
    ): TileLaunchAction {
        return when (kind) {
            NextPassTileKind.UpcomingPass,
            NextPassTileKind.ActivePass -> TileLaunchAction(
                destination = TileLaunchDestination.Radar,
                catalogNumber = catalogNumber,
                aosMillis = aosMillis,
                losMillis = losMillis
            )
            NextPassTileKind.NoQth -> TileLaunchAction(TileLaunchDestination.Qth)
            NextPassTileKind.NoTle -> TileLaunchAction(TileLaunchDestination.Data)
            NextPassTileKind.NoSatellites -> TileLaunchAction(TileLaunchDestination.Satellites)
            NextPassTileKind.NoPassSoon,
            NextPassTileKind.TileOffline -> TileLaunchAction(TileLaunchDestination.Dashboard)
        }
    }
}
