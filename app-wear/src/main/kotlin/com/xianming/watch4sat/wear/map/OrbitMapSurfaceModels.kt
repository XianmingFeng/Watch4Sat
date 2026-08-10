package com.xianming.watch4sat.wear.map

enum class OrbitMapEngine {
    OSM,
    OFFLINE
}

data class OrbitMapViewportSnapshot(
    val centerLatitude: Double,
    val centerLongitude: Double,
    val zoom: Double
)

data class OrbitMapViewportSnapshots(
    val osm: OrbitMapViewportSnapshot = OrbitMapViewportSnapshot(
        centerLatitude = 0.0,
        centerLongitude = 0.0,
        zoom = 0.0
    ),
    val offline: OrbitMapViewportSnapshot = OrbitMapViewportSnapshot(
        centerLatitude = 0.0,
        centerLongitude = 0.0,
        zoom = 2.0
    )
) {
    fun forEngine(engine: OrbitMapEngine): OrbitMapViewportSnapshot {
        return when (engine) {
            OrbitMapEngine.OSM -> osm
            OrbitMapEngine.OFFLINE -> offline
        }
    }

    fun updated(
        engine: OrbitMapEngine,
        viewport: OrbitMapViewportSnapshot
    ): OrbitMapViewportSnapshots {
        return when (engine) {
            OrbitMapEngine.OSM -> copy(osm = viewport)
            OrbitMapEngine.OFFLINE -> copy(offline = viewport)
        }
    }
}

data class OrbitMapViewportCommand(
    val id: Long,
    val catalogNumber: Int,
    val engine: OrbitMapEngine,
    val viewport: OrbitMapViewportSnapshot,
    val transition: OrbitMapViewportTransition = OrbitMapViewportTransition.IMMEDIATE
)

enum class OrbitMapViewportTransition {
    IMMEDIATE,
    ANIMATED
}

data class OrbitMapDisplayState(
    val engine: OrbitMapEngine,
    val viewport: OrbitMapViewportSnapshot,
    val tileState: MapTileDisplayState
)

object OrbitMapViewportPolicy {
    private const val MapLatitudeLimit = 85.0

    fun sanitize(
        viewport: OrbitMapViewportSnapshot,
        minZoom: Double,
        maxZoom: Double
    ): OrbitMapViewportSnapshot {
        require(minZoom.isFinite() && maxZoom.isFinite() && minZoom <= maxZoom)
        return OrbitMapViewportSnapshot(
            centerLatitude = viewport.centerLatitude
                .takeIf(Double::isFinite)
                ?.coerceIn(-MapLatitudeLimit, MapLatitudeLimit)
                ?: 0.0,
            centerLongitude = viewport.centerLongitude
                .takeIf(Double::isFinite)
                ?.let(::wrapLongitude)
                ?: 0.0,
            zoom = viewport.zoom
                .takeIf(Double::isFinite)
                ?.coerceIn(minZoom, maxZoom)
                ?: minZoom
        )
    }

    private fun wrapLongitude(longitude: Double): Double {
        var wrapped = longitude % 360.0
        if (wrapped > 180.0) wrapped -= 360.0
        if (wrapped < -180.0) wrapped += 360.0
        return wrapped
    }
}

object OrbitMapViewportMotionPolicy {
    fun interpolate(
        start: OrbitMapViewportSnapshot,
        end: OrbitMapViewportSnapshot,
        progress: Double
    ): OrbitMapViewportSnapshot {
        val fraction = progress.coerceIn(0.0, 1.0)
        if (fraction == 0.0) return start
        if (fraction == 1.0) return end
        return OrbitMapViewportSnapshot(
            centerLatitude = lerp(start.centerLatitude, end.centerLatitude, fraction),
            centerLongitude = wrapLongitude(
                start.centerLongitude +
                    shortestLongitudeDelta(
                        start = start.centerLongitude,
                        end = end.centerLongitude
                    ) * fraction
            ),
            zoom = lerp(start.zoom, end.zoom, fraction)
        )
    }

    fun shortestLongitudeDelta(start: Double, end: Double): Double {
        var delta = (end - start) % 360.0
        if (delta > 180.0) delta -= 360.0
        if (delta < -180.0) delta += 360.0
        return delta
    }

    private fun lerp(start: Double, end: Double, fraction: Double): Double {
        return start + (end - start) * fraction
    }

    private fun wrapLongitude(longitude: Double): Double {
        var wrapped = longitude % 360.0
        if (wrapped > 180.0) wrapped -= 360.0
        if (wrapped < -180.0) wrapped += 360.0
        return wrapped
    }
}

object OsmTileInteractionPolicy {
    fun acceptsCallback(interactionEnabled: Boolean): Boolean = interactionEnabled

    fun eventOnInteractiveResume(hasValidatedNetwork: Boolean): OsmTileEvent {
        return if (hasValidatedNetwork) {
            OsmTileEvent.Loading
        } else {
            OsmTileEvent.NoValidatedNetwork
        }
    }
}

object OrbitMapAmbientDisplayPolicy {
    fun visibleDisplay(
        interactionEnabled: Boolean,
        lastInteractiveDisplay: MapTileDisplayState,
        currentDisplay: MapTileDisplayState
    ): MapTileDisplayState {
        return if (interactionEnabled) currentDisplay else lastInteractiveDisplay
    }
}

object OrbitMapSwipeBackPolicy {
    private const val MaxComposeEdgeSwipeSdk = 35

    fun usesComposeEdgeSwipe(sdkInt: Int): Boolean {
        return sdkInt <= MaxComposeEdgeSwipeSdk
    }
}

internal class OrbitMapCommandLedger {
    private val lastAppliedIds = mutableMapOf<OrbitMapEngine, Long>()

    fun shouldApply(
        command: OrbitMapViewportCommand,
        engine: OrbitMapEngine,
        interactionEnabled: Boolean = true
    ): Boolean {
        return interactionEnabled &&
            command.engine == engine &&
            command.id > (lastAppliedIds[engine] ?: Long.MIN_VALUE)
    }

    fun markApplied(command: OrbitMapViewportCommand) {
        val lastAppliedId = lastAppliedIds[command.engine] ?: Long.MIN_VALUE
        if (command.id > lastAppliedId) {
            lastAppliedIds[command.engine] = command.id
        }
    }
}
