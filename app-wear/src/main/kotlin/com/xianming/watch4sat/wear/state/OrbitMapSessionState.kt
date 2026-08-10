package com.xianming.watch4sat.wear.state

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import com.xianming.watch4sat.domain.model.GroundTrackPoint
import com.xianming.watch4sat.wear.map.OrbitMapEngine
import com.xianming.watch4sat.wear.map.OrbitMapViewportCommand
import com.xianming.watch4sat.wear.map.OrbitMapViewportSnapshot
import com.xianming.watch4sat.wear.map.OrbitMapViewportSnapshots
import com.xianming.watch4sat.wear.map.OrbitMapViewportTransition

data class OrbitMapSessionState(
    val selectedCatalogNumber: Int? = null,
    val chrome: OrbitMapChromeState = OrbitMapChromeState(),
    val viewports: OrbitMapViewportSnapshots = OrbitMapViewportSnapshots(),
    val viewportInitialization: OrbitMapViewportInitialization =
        OrbitMapViewportInitialization(),
    val lastCenteredCatalogNumber: Int? = null,
    val viewportCommandSequence: Long = 0L
) {
    companion object {
        val Saver: Saver<OrbitMapSessionState, Any> = listSaver(
            save = { state -> saveValues(state) },
            restore = { values -> restoreValues(values) }
        )

        internal fun saveValues(state: OrbitMapSessionState): List<Any> {
            return listOf(
                state.selectedCatalogNumber ?: MissingCatalogNumber,
                state.chrome.interactiveVisible,
                state.viewports.osm.centerLatitude,
                state.viewports.osm.centerLongitude,
                state.viewports.osm.zoom,
                state.viewports.offline.centerLatitude,
                state.viewports.offline.centerLongitude,
                state.viewports.offline.zoom,
                state.lastCenteredCatalogNumber ?: MissingCatalogNumber,
                state.viewportCommandSequence,
                state.viewportInitialization.osmInitialized,
                state.viewportInitialization.offlineInitialized
            )
        }

        internal fun restoreValues(values: List<Any>): OrbitMapSessionState {
            val viewports = OrbitMapViewportSnapshots(
                osm = OrbitMapViewportSnapshot(
                    centerLatitude = values[2] as Double,
                    centerLongitude = values[3] as Double,
                    zoom = values[4] as Double
                ),
                offline = OrbitMapViewportSnapshot(
                    centerLatitude = values[5] as Double,
                    centerLongitude = values[6] as Double,
                    zoom = values[7] as Double
                )
            )
            val initialization = if (values.size >= SavedValueCountWithInitialization) {
                OrbitMapViewportInitialization(
                    osmInitialized = values[10] as Boolean,
                    offlineInitialized = values[11] as Boolean
                )
            } else {
                val defaults = OrbitMapViewportSnapshots()
                OrbitMapViewportInitialization(
                    osmInitialized = viewports.osm != defaults.osm,
                    offlineInitialized = viewports.offline != defaults.offline
                )
            }
            return OrbitMapSessionState(
                selectedCatalogNumber = (values[0] as Int).takeUnless {
                    it == MissingCatalogNumber
                },
                chrome = OrbitMapChromeState(values[1] as Boolean),
                viewports = viewports,
                viewportInitialization = initialization,
                lastCenteredCatalogNumber = (values[8] as Int).takeUnless {
                    it == MissingCatalogNumber
                },
                viewportCommandSequence = values[9] as Long
            )
        }

        private const val SavedValueCountWithInitialization = 12
        private const val MissingCatalogNumber = Int.MIN_VALUE
    }
}

data class OrbitMapViewportInitialization(
    val osmInitialized: Boolean = false,
    val offlineInitialized: Boolean = false
) {
    fun isInitialized(engine: OrbitMapEngine): Boolean {
        return when (engine) {
            OrbitMapEngine.OSM -> osmInitialized
            OrbitMapEngine.OFFLINE -> offlineInitialized
        }
    }

    fun initialized(engine: OrbitMapEngine): OrbitMapViewportInitialization {
        return when (engine) {
            OrbitMapEngine.OSM -> copy(osmInitialized = true)
            OrbitMapEngine.OFFLINE -> copy(offlineInitialized = true)
        }
    }
}

data class OrbitMapRecenterDecision(
    val session: OrbitMapSessionState,
    val command: OrbitMapViewportCommand?
)

object OrbitMapSessionReducer {
    private const val DefaultOsmSatelliteZoom = 3.0
    private const val DefaultOfflineSatelliteZoom = 2.0

    fun selectCatalog(
        state: OrbitMapSessionState,
        catalogNumber: Int?,
        pendingCommand: OrbitMapViewportCommand? = null
    ): OrbitMapSessionState {
        val supersededCatalogNumber = pendingCommand
            ?.catalogNumber
            ?.takeIf { pendingCatalogNumber ->
                pendingCatalogNumber != catalogNumber
            }
        return state.copy(
            selectedCatalogNumber = catalogNumber,
            lastCenteredCatalogNumber =
                supersededCatalogNumber ?: state.lastCenteredCatalogNumber
        )
    }

    fun shouldCancelPendingViewportCommand(
        pendingCommand: OrbitMapViewportCommand?,
        selectedCatalogNumber: Int?
    ): Boolean {
        return pendingCommand != null &&
            pendingCommand.catalogNumber != selectedCatalogNumber
    }

    fun toggleChrome(state: OrbitMapSessionState): OrbitMapSessionState {
        return state.copy(chrome = OrbitMapChromePolicy.onConfirmedMapTap(state.chrome))
    }

    fun updateViewport(
        state: OrbitMapSessionState,
        engine: OrbitMapEngine,
        viewport: OrbitMapViewportSnapshot
    ): OrbitMapSessionState {
        return state.copy(
            viewports = state.viewports.updated(engine, viewport),
            viewportInitialization = state.viewportInitialization.initialized(engine)
        )
    }

    fun recenterIfNeeded(
        state: OrbitMapSessionState,
        position: GroundTrackPoint?,
        engine: OrbitMapEngine?
    ): OrbitMapRecenterDecision {
        val catalogNumber = state.selectedCatalogNumber
        if (
            catalogNumber == null ||
            position == null ||
            engine == null
        ) {
            return OrbitMapRecenterDecision(session = state, command = null)
        }
        if (
            state.lastCenteredCatalogNumber == catalogNumber &&
            state.viewportInitialization.isInitialized(engine)
        ) {
            return OrbitMapRecenterDecision(session = state, command = null)
        }
        val currentViewport = state.viewports.forEngine(engine)
        val defaultZoom = when (engine) {
            OrbitMapEngine.OSM -> DefaultOsmSatelliteZoom
            OrbitMapEngine.OFFLINE -> DefaultOfflineSatelliteZoom
        }
        val commandId = state.viewportCommandSequence + 1L
        val transition = if (
            state.lastCenteredCatalogNumber != null &&
            state.lastCenteredCatalogNumber != catalogNumber &&
            state.viewportInitialization.isInitialized(engine)
        ) {
            OrbitMapViewportTransition.ANIMATED
        } else {
            OrbitMapViewportTransition.IMMEDIATE
        }
        val target = currentViewport.copy(
            centerLatitude = position.latitudeDegrees,
            centerLongitude = position.longitudeDegrees,
            zoom = currentViewport.zoom.coerceAtLeast(defaultZoom)
        )
        return OrbitMapRecenterDecision(
            session = state.copy(
                viewportCommandSequence = commandId
            ),
            command = OrbitMapViewportCommand(
                id = commandId,
                catalogNumber = catalogNumber,
                engine = engine,
                viewport = target,
                transition = transition
            )
        )
    }

    fun acknowledgeViewportCommand(
        state: OrbitMapSessionState,
        command: OrbitMapViewportCommand
    ): OrbitMapSessionState {
        if (
            command.catalogNumber != state.selectedCatalogNumber ||
            command.id != state.viewportCommandSequence
        ) {
            return state
        }
        return state.copy(
            lastCenteredCatalogNumber = command.catalogNumber,
            viewportInitialization =
                state.viewportInitialization.initialized(command.engine)
        )
    }
}
