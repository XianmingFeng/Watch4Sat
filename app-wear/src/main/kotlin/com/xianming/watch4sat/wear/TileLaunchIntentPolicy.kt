package com.xianming.watch4sat.wear

import android.content.Intent
import com.xianming.watch4sat.tile.TileLaunchDestination
import com.xianming.watch4sat.wear.state.ExactPassIdentity
import com.xianming.watch4sat.wear.state.ExactPassLaunchRequest
import com.xianming.watch4sat.wear.state.ExactPassLaunchSource

object TileLaunchIntentPolicy {
    const val extraSource: String = "com.xianming.watch4sat.extra.LAUNCH_SOURCE"
    const val sourceTile: String = "tile"
    const val extraDestination: String = "com.xianming.watch4sat.extra.TILE_DESTINATION"
    const val extraCatalogNumber: String = "com.xianming.watch4sat.extra.TILE_CATALOG_NUMBER"
    const val extraAosMillis: String = "com.xianming.watch4sat.extra.TILE_AOS_MILLIS"
    const val extraLosMillis: String = "com.xianming.watch4sat.extra.TILE_LOS_MILLIS"

    fun requestFrom(intent: Intent?): TileLaunchRequest? {
        if (intent == null) return null
        return requestFromExtras(
            source = intent.getStringExtra(extraSource),
            destination = intent.getStringExtra(extraDestination),
            catalogNumber = intent.getIntExtra(extraCatalogNumber, MissingCatalogNumber),
            aosMillis = intent.getLongExtra(extraAosMillis, MissingAosMillis),
            losMillis = intent.getLongExtra(extraLosMillis, MissingLosMillis),
            launchId = System.identityHashCode(intent).toString()
        )
    }

    fun requestFromExtras(
        source: String?,
        destination: String?,
        catalogNumber: Int,
        aosMillis: Long,
        losMillis: Long = MissingLosMillis,
        launchId: String? = null
    ): TileLaunchRequest? {
        if (source != sourceTile) return null
        val parsedDestination = destination
            ?.let { runCatching { TileLaunchDestination.valueOf(it) }.getOrNull() }
            ?: return null
        return TileLaunchRequest(
            destination = parsedDestination,
            catalogNumber = catalogNumber.takeIf { it != MissingCatalogNumber },
            aosMillis = aosMillis.takeIf { it != MissingAosMillis },
            losMillis = losMillis.takeIf { it != MissingLosMillis },
            launchId = launchId
        )
    }

    fun routeFor(destination: TileLaunchDestination): WatchRoute {
        return when (destination) {
            TileLaunchDestination.Dashboard -> WatchRoute.Dashboard
            TileLaunchDestination.Radar -> WatchRoute.Radar
            TileLaunchDestination.Qth -> WatchRoute.Qth
            TileLaunchDestination.Data -> WatchRoute.Data
            TileLaunchDestination.Satellites -> WatchRoute.Satellites
        }
    }

    private const val MissingCatalogNumber = -1
    private const val MissingAosMillis = Long.MIN_VALUE
    private const val MissingLosMillis = Long.MIN_VALUE
}

data class TileLaunchRequest(
    val destination: TileLaunchDestination,
    val catalogNumber: Int? = null,
    val aosMillis: Long? = null,
    val losMillis: Long? = null,
    val launchId: String? = null
) {
    val source: ExactPassLaunchSource = ExactPassLaunchSource.Tile
    val exactPassIdentity: ExactPassIdentity? = ExactPassIdentity.from(
        catalogNumber = catalogNumber,
        aosMillis = aosMillis,
        losMillis = losMillis
    )
    val exactPassLaunchRequest: ExactPassLaunchRequest = ExactPassLaunchRequest(
        source = source,
        identity = exactPassIdentity
    )
    val key: String = listOfNotNull(
        destination.name,
        catalogNumber?.toString(),
        aosMillis?.toString(),
        losMillis?.toString(),
        launchId
    ).joinToString(":")
}
