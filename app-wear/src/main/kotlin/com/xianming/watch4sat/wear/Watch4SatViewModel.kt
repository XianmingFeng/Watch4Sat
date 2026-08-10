package com.xianming.watch4sat.wear

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.xianming.watch4sat.BuildConfig
import com.xianming.watch4sat.R
import com.xianming.watch4sat.data.Watch4SatDataLayer
import com.xianming.watch4sat.data.network.NetworkClientIdentity
import com.xianming.watch4sat.data.repository.DataRefreshResult
import com.xianming.watch4sat.data.repository.DataRefreshSummary
import com.xianming.watch4sat.data.repository.SatelliteCatalog
import com.xianming.watch4sat.data.repository.SatelliteDataRepository
import com.xianming.watch4sat.data.settings.AppThemePreset
import com.xianming.watch4sat.data.settings.MapTileMode
import com.xianming.watch4sat.data.settings.RadarForwardAxis
import com.xianming.watch4sat.data.settings.RadarWristSide
import com.xianming.watch4sat.data.settings.SetupCompletionResult
import com.xianming.watch4sat.data.settings.Watch4SatSettings
import com.xianming.watch4sat.data.settings.Watch4SatSettingsStore
import com.xianming.watch4sat.domain.doppler.DopplerCalculator
import com.xianming.watch4sat.domain.freshness.TleEpochSample
import com.xianming.watch4sat.domain.freshness.TleFreshnessAssessment
import com.xianming.watch4sat.domain.freshness.TleFreshnessPolicy
import com.xianming.watch4sat.domain.footprint.SatelliteFootprint
import com.xianming.watch4sat.domain.footprint.SatelliteFootprintCalculator
import com.xianming.watch4sat.domain.groundtrack.GroundTrackSegmenter
import com.xianming.watch4sat.domain.model.DopplerReading
import com.xianming.watch4sat.domain.model.GroundTrackPoint
import com.xianming.watch4sat.domain.model.LocationSource
import com.xianming.watch4sat.domain.model.OrbitalPosition
import com.xianming.watch4sat.domain.model.PassCardUi
import com.xianming.watch4sat.domain.model.RadarTrackPoint
import com.xianming.watch4sat.domain.model.SatellitePass
import com.xianming.watch4sat.domain.model.SatelliteRecord
import com.xianming.watch4sat.domain.model.StationLocation
import com.xianming.watch4sat.domain.model.TransmitterRecord
import com.xianming.watch4sat.domain.pass.PassCardMapper
import com.xianming.watch4sat.domain.pass.PassPredictionService
import com.xianming.watch4sat.domain.qth.MaidenheadLocator
import com.xianming.watch4sat.location.AndroidLocationProvider
import com.xianming.watch4sat.location.FusedLocationProvider
import com.xianming.watch4sat.location.LocationDiagnostics
import com.xianming.watch4sat.location.LocationRepository
import com.xianming.watch4sat.location.LocationResultState
import com.xianming.watch4sat.location.PrioritizedLocationProvider
import com.xianming.watch4sat.qth.QthInputValidator
import com.xianming.watch4sat.time.AndroidClockTimeFormatter
import com.xianming.watch4sat.wear.state.OrbitMapRequestKey
import com.xianming.watch4sat.wear.state.OrbitMapRequestPolicy
import com.xianming.watch4sat.wear.state.OrbitMapDetailPositionResolver
import com.xianming.watch4sat.wear.state.OrbitMapDetailRequestKey
import com.xianming.watch4sat.wear.state.OrbitMapDetailRequestPolicy
import com.xianming.watch4sat.wear.state.OrbitMapDetailRuntimeReducer
import com.xianming.watch4sat.wear.state.OrbitMapDetailRuntimeState
import com.xianming.watch4sat.wear.state.OrbitMapDetailUiState
import com.xianming.watch4sat.wear.state.OrbitMapDetailUpdateLoop
import com.xianming.watch4sat.wear.state.OrbitMapDetailUpdatePolicy
import com.xianming.watch4sat.wear.state.OrbitMapSavedSelectionStore
import com.xianming.watch4sat.wear.state.OrbitMapSatelliteSelector
import com.xianming.watch4sat.wear.state.OrbitMapSelectionReducer
import com.xianming.watch4sat.wear.state.OrbitMapUiState
import com.xianming.watch4sat.wear.state.OrbitMapUpdatePolicy
import com.xianming.watch4sat.wear.state.PassAlertAdvancePolicy
import com.xianming.watch4sat.wear.state.PassPlanningInput
import com.xianming.watch4sat.wear.state.PassPlanningClock
import com.xianming.watch4sat.wear.state.PassPlanningProgress
import com.xianming.watch4sat.wear.state.PassPlanningStatus
import com.xianming.watch4sat.wear.state.PassSnapshotRenewalInputPolicy
import com.xianming.watch4sat.wear.state.PassSnapshotRenewalInputTracker
import com.xianming.watch4sat.wear.state.PassUiClockPolicy
import com.xianming.watch4sat.wear.state.ProgressivePassPlanner
import com.xianming.watch4sat.wear.state.QthGpsFailureDialogPolicy
import com.xianming.watch4sat.wear.state.QthGpsRequestPolicy
import com.xianming.watch4sat.wear.state.QthGpsRequestStatus
import com.xianming.watch4sat.wear.state.QthGpsStatusKind
import com.xianming.watch4sat.wear.state.DataRefreshRequestPolicy
import com.xianming.watch4sat.wear.state.DataFreshnessPolicy
import com.xianming.watch4sat.wear.state.FirstRunSetupStep
import com.xianming.watch4sat.wear.state.MinimumElevationPolicy
import com.xianming.watch4sat.wear.radar.RadarPowerPolicy
import com.xianming.watch4sat.wear.radar.RadarUpdateMode
import com.xianming.watch4sat.wear.state.RadarTransmitterSelector
import com.xianming.watch4sat.wear.state.StarterSelectionPolicy
import com.xianming.watch4sat.wear.state.TleFreshnessScopePolicy
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class Watch4SatViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    companion object {
        val Factory = viewModelFactory {
            initializer {
                Watch4SatViewModel(
                    application = checkNotNull(this[APPLICATION_KEY]),
                    savedStateHandle = createSavedStateHandle()
                )
            }
        }
    }

    private val dependencies = Watch4SatDataLayer.create(
        context = application,
        networkClientIdentity = NetworkClientIdentity(
            versionName = BuildConfig.VERSION_NAME,
            applicationId = BuildConfig.APPLICATION_ID
        )
    )
    private val repository: SatelliteDataRepository = dependencies.satelliteDataRepository
    private val settingsStore: Watch4SatSettingsStore = dependencies.settingsStore
    private val passCardTextFormatter = AndroidPassCardTextFormatter(application)
    private val passStartAlarmStateStore = PassStartAlarmStateStore(application)
    private val stationDataDeletionCoordinator = StationDataDeletionCoordinator(
        beginStationDataDeletion = {
            settingsStore.beginStationDataDeletion()
        },
        cancelRenewalWork = {
            PassSnapshotRenewalEnqueuer.cancelAll(application)
        },
        cancelPassAlarm = {
            PassStartAlarmScheduler(
                application,
                passStartAlarmStateStore,
                stationDataScheduleGuard(application)
            ).cancel()
        },
        clearAlarmState = passStartAlarmStateStore::clearAll,
        clearPassSnapshot = dependencies.passSnapshotCache::clear,
        completeStationDataDeletion = settingsStore::completeStationDataDeletion
    )
    private val locationRepository = LocationRepository(
        PrioritizedLocationProvider(
            preferred = FusedLocationProvider(
                context = application,
                permissionChecker = { application.hasLocationPermission() }
            ),
            fallback = AndroidLocationProvider(
                locationManager =
                    application.getSystemService(Context.LOCATION_SERVICE) as LocationManager,
                permissionChecker = { application.hasLocationPermission() }
            )
        )
    )
    private val passPlanner = ProgressivePassPlanner(dependencies.passSnapshotCache) { satellites, station, startMillis, window ->
        PassPredictionService.predictPasses(satellites, station, startMillis, window)
    }
    private val satelliteCatalogState = repository.satelliteCatalog
        .map { catalog -> SatelliteCatalogLoadState(catalog = catalog, loaded = true) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = SatelliteCatalogLoadState()
        )
    private val satelliteRecordsFlow = satelliteCatalogState
        .map { state -> state.catalog.records }
        .distinctUntilChanged()

    private val interactionState = MutableStateFlow(
        WatchInteractionState(
            refreshMessage = application.getString(R.string.vm_ready),
            locationMessage = application.getString(R.string.vm_set_qth)
        )
    )
    private val passPlanningState = MutableStateFlow(
        PassPlanningProgress(
            status = PassPlanningStatus.Idle
        )
    )
    private var radarJob: Job? = null
    private var radarUpdateMode: RadarUpdateMode = RadarUpdateMode.Interactive
    private var minuteJob: Job? = null
    private val minuteClockWakeSignal = Channel<Unit>(Channel.CONFLATED)
    private var orbitMapJob: Job? = null
    private var orbitMapImmediateJob: Job? = null
    private var orbitMapDetailJob: Job? = null
    private var gpsRequestJob: Job? = null
    private val locationSaveMutex = Mutex()
    private val setupCompletionMutex = Mutex()
    private var lastAutoFreshnessFailureElapsedRealtime: Long? = null
    private var cachedTrackKey: TrackCacheKey? = null
    private var cachedTrack: List<RadarTrackPoint> = emptyList()
    private val orbitMapSavedSelection = OrbitMapSavedSelectionStore(savedStateHandle)
    private val orbitMapRuntimeState = MutableStateFlow(
        OrbitMapRuntimeState(
            selectedCatalogNumber = orbitMapSavedSelection.selectedCatalogNumber,
            message = application.getString(R.string.vm_refresh_tle_first)
        )
    )
    private val latestSettingsState = MutableStateFlow<Watch4SatSettings?>(null)
    private val orbitMapDetailRuntimeState = MutableStateFlow(
        OrbitMapDetailRuntimeState()
    )
    private val orbitMapDetailUpdateLoop = OrbitMapDetailUpdateLoop()

    init {
        resumePendingStationDataDeletion()
        observeLatestSettings()
        observePassSnapshotRenewalInputs()
        observePassPlanningInputs()
    }

    private fun resumePendingStationDataDeletion() {
        viewModelScope.launch {
            if (!settingsStore.getSettings().stationDataDeletionInProgress) return@launch
            runCatching {
                locationSaveMutex.withLock {
                    stationDataDeletionCoordinator.clear()
                }
            }
        }
    }

    private val appStateInputs = combine(
        settingsStore.settings,
        satelliteCatalogState,
        repository.transmitters,
        interactionState,
        passPlanningState
    ) { settings, satelliteCatalog, transmitters, interaction, passPlanning ->
        WatchAppStateInputs(settings, satelliteCatalog, transmitters, interaction, passPlanning)
    }

    val uiState: StateFlow<WatchUiState> = combine(
        appStateInputs,
        orbitMapRuntimeState
    ) { inputs, orbitMapRuntime ->
        buildUiState(
            settings = inputs.settings,
            satellites = inputs.satelliteCatalog.catalog.records,
            satelliteCatalogLoaded = inputs.satelliteCatalog.loaded,
            datasetRetrievedAtMillis =
                inputs.satelliteCatalog.catalog.dataset?.retrievedAtMillis,
            transmitters = inputs.transmitters,
            interaction = inputs.interaction,
            passPlanning = inputs.passPlanning,
            orbitMapRuntime = orbitMapRuntime
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WatchUiState()
    )
    val orbitMapDetailState: StateFlow<OrbitMapDetailUiState> =
        orbitMapDetailRuntimeState
            .map(OrbitMapDetailRuntimeState::toUiState)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = OrbitMapDetailUiState()
            )

    fun refreshAll() {
        if (!DataRefreshRequestPolicy.canStartRefresh(interactionState.value.refreshInFlight)) return
        interactionState.update {
            it.copy(
                refreshMessage = getApplication<Application>().getString(
                    R.string.data_refreshing
                ),
                refreshInFlight = true
            )
        }
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val summary = repository.refreshAll()
                    if (
                        summary.satellites.succeeded &&
                        settingsStore.getSettings().selectedSatelliteIds.isEmpty()
                    ) {
                        repository.setSelectedSatelliteIds(
                            StarterSelectionPolicy.pickStarterSelection(repository.getCachedSatellites())
                        )
                    }
                    summary
                }
            }
            interactionState.update { state ->
                result.fold(
                    onSuccess = { summary ->
                        if (summary.failedCompletely) {
                            state.copy(
                                refreshMessage = summary.resolveMessage(getApplication()),
                                refreshFailureEventId = state.refreshFailureEventId + 1L,
                                refreshInFlight = false
                            )
                        } else {
                            state.copy(
                                refreshMessage = summary.resolveMessage(getApplication()),
                                refreshSuccessEventId = state.refreshSuccessEventId + 1L,
                                refreshInFlight = false
                            )
                        }
                    },
                    onFailure = {
                        state.copy(
                            refreshMessage = getApplication<Application>().getString(
                                R.string.vm_refresh_failed
                            ),
                            refreshFailureEventId = state.refreshFailureEventId + 1L,
                            refreshInFlight = false
                        )
                    }
                )
            }
        }
    }

    fun refreshSetupData() {
        if (!DataRefreshRequestPolicy.canStartRefresh(interactionState.value.refreshInFlight)) return
        interactionState.update {
            it.copy(
                refreshMessage = getApplication<Application>().getString(
                    R.string.data_refreshing
                ),
                refreshInFlight = true
            )
        }
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val summary = repository.refreshAll()
                    summary
                }
            }
            interactionState.update { state ->
                result.fold(
                    onSuccess = { summary ->
                        if (summary.failedCompletely) {
                            state.copy(
                                refreshMessage = summary.resolveMessage(getApplication()),
                                refreshFailureEventId = state.refreshFailureEventId + 1L,
                                refreshInFlight = false
                            )
                        } else {
                            state.copy(
                                refreshMessage = summary.resolveMessage(getApplication()),
                                refreshSuccessEventId = state.refreshSuccessEventId + 1L,
                                refreshInFlight = false
                            )
                        }
                    },
                    onFailure = {
                        state.copy(
                            refreshMessage = getApplication<Application>().getString(
                                R.string.vm_refresh_failed
                            ),
                            refreshFailureEventId = state.refreshFailureEventId + 1L,
                            refreshInFlight = false
                        )
                    }
                )
            }
        }
    }

    fun refreshStaleDataIfNeeded(reason: String) {
        val settings = uiState.value.settings
        val nowMillis = System.currentTimeMillis()
        val elapsedRealtime = SystemClock.elapsedRealtime()
        val lastFailureElapsedRealtime = lastAutoFreshnessFailureElapsedRealtime
        if (
            lastFailureElapsedRealtime != null &&
            elapsedRealtime - lastFailureElapsedRealtime <
            ForegroundFreshnessFailureCooldownMillis
        ) {
            return
        }
        val decision = DataFreshnessPolicy.shouldRefresh(
            nowMillis = nowMillis,
            lastSatelliteDataUpdateMillis = settings.lastSatelliteDataUpdateMillis,
            lastTransmitterDataUpdateMillis = settings.lastTransmitterDataUpdateMillis,
            autoDataFreshnessEnabled = settings.autoDataFreshnessEnabled,
            refreshInFlight = interactionState.value.refreshInFlight,
            foreground = true,
            tleFreshness = uiState.value.tleFreshness
        )
        if (!decision.shouldRefreshAny) return
        interactionState.update {
            it.copy(
                refreshMessage = getApplication<Application>().getString(
                    R.string.vm_refreshing_with_reason,
                    reason
                ),
                refreshInFlight = true
            )
        }
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    if (decision.refreshSatellites) {
                        val summary = repository.refreshAll()
                        if (
                            summary.satellites.succeeded &&
                            settingsStore.getSettings().selectedSatelliteIds.isEmpty()
                        ) {
                            repository.setSelectedSatelliteIds(
                                StarterSelectionPolicy.pickStarterSelection(repository.getCachedSatellites())
                            )
                        }
                        summary
                    } else {
                        DataRefreshSummary(
                            satellites = DataRefreshResult(
                                recordsPersisted = 0,
                                updatedAtMillis = settings.lastSatelliteDataUpdateMillis ?: 0L
                            ),
                            transmitters = repository.refreshTransmitters()
                        )
                    }
                }
            }
            interactionState.update { state ->
                result.fold(
                    onSuccess = { summary ->
                        val requestedRefreshFailed =
                            (decision.refreshSatellites && !summary.satellites.succeeded) ||
                                (decision.refreshTransmitters && !summary.transmitters.succeeded)
                        if (summary.failedCompletely) {
                            lastAutoFreshnessFailureElapsedRealtime =
                                SystemClock.elapsedRealtime()
                            state.copy(
                                refreshMessage = summary.resolveMessage(getApplication()),
                                refreshFailureEventId = state.refreshFailureEventId + 1L,
                                refreshInFlight = false
                            )
                        } else {
                            lastAutoFreshnessFailureElapsedRealtime = if (requestedRefreshFailed) {
                                SystemClock.elapsedRealtime()
                            } else {
                                null
                            }
                            state.copy(
                                refreshMessage = summary.resolveMessage(getApplication()),
                                refreshSuccessEventId = state.refreshSuccessEventId + 1L,
                                refreshInFlight = false
                            )
                        }
                    },
                    onFailure = {
                        lastAutoFreshnessFailureElapsedRealtime =
                            SystemClock.elapsedRealtime()
                        state.copy(
                            refreshMessage = getApplication<Application>().getString(
                                R.string.vm_refresh_failed
                            ),
                            refreshFailureEventId = state.refreshFailureEventId + 1L,
                            refreshInFlight = false
                        )
                    }
                )
            }
        }
    }

    fun requestGps() {
        var shouldStart = false
        var requestGeneration = 0L
        interactionState.update { state ->
            val startDecision = QthGpsRequestPolicy.startRequest(
                gpsRequestInFlight = state.gpsRequestInFlight,
                currentGeneration = state.locationRequestGeneration
            )
            shouldStart = startDecision.shouldStart
            requestGeneration = startDecision.requestGeneration
            val startMessage = getApplication<Application>().getString(
                when (startDecision.status) {
                    QthGpsRequestStatus.AlreadyRunning ->
                        R.string.qth_request_already_running
                    QthGpsRequestStatus.WaitingForFix ->
                        R.string.qth_request_waiting_for_fix
                }
            )
            if (startDecision.shouldStart) {
                state.copy(
                    locationMessage = startMessage,
                    locationStatusKind = QthGpsStatusKind.Neutral,
                    gpsRequestInFlight = true,
                    locationRequestGeneration = startDecision.requestGeneration
                )
            } else {
                state.copy(
                    locationMessage = startMessage,
                    locationStatusKind = QthGpsStatusKind.Neutral
                )
            }
        }
        if (!shouldStart) {
            return
        }
        gpsRequestJob = viewModelScope.launch {
            val runningJob = coroutineContext[Job]
            try {
                val result = locationRepository.resolveCurrentLocation()
                locationSaveMutex.withLock {
                    val shouldApplyResult = QthGpsRequestPolicy.shouldApplyResult(
                        requestGeneration = requestGeneration,
                        currentGeneration = interactionState.value.locationRequestGeneration
                    )
                    if (shouldApplyResult) {
                        result.stationLocation?.let { settingsStore.setStationLocation(it) }
                    }
                }
                val resultMessage = result.toMessage()
                interactionState.update { state ->
                    if (!QthGpsRequestPolicy.shouldApplyResult(
                            requestGeneration = requestGeneration,
                            currentGeneration = state.locationRequestGeneration
                        )
                    ) {
                        return@update state
                    }
                    val shouldShowTimeoutDialog = QthGpsFailureDialogPolicy.shouldEmitFor(result.state)
                    state.copy(
                        gpsRequestInFlight = false,
                        locationMessage = resultMessage,
                        locationStatusKind = result.state.statusKind(),
                        gpsFailureMessage = if (shouldShowTimeoutDialog) resultMessage else state.gpsFailureMessage,
                        gpsFailureEventId = if (shouldShowTimeoutDialog) state.gpsFailureEventId + 1L else state.gpsFailureEventId
                    )
                }
            } finally {
                if (gpsRequestJob === runningJob) {
                    gpsRequestJob = null
                }
            }
        }
    }

    fun cancelGpsRequest(reason: String = "user") {
        cancelGpsRequest(
            message = getApplication<Application>().getString(
                R.string.vm_location_canceled
            ),
            reason = reason
        )
    }

    fun cancelGpsRequest(message: String?, reason: String = "user") {
        LocationDiagnostics.log("event=cancel reason=$reason")
        gpsRequestJob?.cancel()
        gpsRequestJob = null
        interactionState.update { state ->
            if (!state.gpsRequestInFlight) {
                return@update state
            }
            state.copy(
                gpsRequestInFlight = false,
                locationMessage = message ?: state.locationMessage,
                locationStatusKind = QthGpsStatusKind.Neutral,
                locationRequestGeneration = QthGpsRequestPolicy.invalidateRequest(
                    state.locationRequestGeneration
                )
            )
        }
    }

    fun reportLocationPermissionDenied() {
        interactionState.update {
            it.copy(
                gpsRequestInFlight = false,
                locationMessage = getApplication<Application>().getString(
                    R.string.vm_location_permission_needed
                ),
                locationStatusKind = QthGpsStatusKind.Failure
            )
        }
    }

    fun saveQth(qth: String) {
        viewModelScope.launch {
            val station = QthInputValidator.toStationLocation(qth)
            if (station == null) {
                interactionState.update {
                    it.copy(
                        locationMessage = getApplication<Application>().getString(
                            R.string.vm_invalid_qth
                        ),
                        locationStatusKind = QthGpsStatusKind.Failure
                    )
                }
            } else {
                val currentQth = settingsStore.getSettings().stationLocation?.qthLocator?.uppercase(Locale.US)
                if (currentQth == station.qthLocator?.uppercase(Locale.US)) {
                    interactionState.update {
                        it.copy(
                            locationMessage = getApplication<Application>().getString(
                                R.string.vm_qth_unchanged
                            ),
                            locationStatusKind = QthGpsStatusKind.Neutral
                        )
                    }
                    return@launch
                }
                cancelGpsRequest(message = null)
                locationSaveMutex.withLock {
                    interactionState.update {
                        it.copy(
                            gpsRequestInFlight = false,
                            locationRequestGeneration = QthGpsRequestPolicy.invalidateRequest(
                                it.locationRequestGeneration
                            )
                        )
                    }
                    settingsStore.setStationLocation(station)
                }
                interactionState.update {
                    it.copy(
                        gpsRequestInFlight = false,
                        locationMessage = getApplication<Application>().getString(
                            R.string.vm_saved_qth,
                            station.qthLocator
                        ),
                        locationStatusKind = QthGpsStatusKind.Success
                    )
                }
            }
        }
    }

    fun saveMapCenter(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            val qth = MaidenheadLocator.fromCoordinates(latitude, longitude)?.uppercase(Locale.US)
            cancelGpsRequest(message = null)
            locationSaveMutex.withLock {
                interactionState.update {
                    it.copy(
                        gpsRequestInFlight = false,
                        locationRequestGeneration = QthGpsRequestPolicy.invalidateRequest(
                            it.locationRequestGeneration
                        )
                    )
                }
                settingsStore.setStationLocation(
                    StationLocation(
                        latitude = latitude,
                        longitude = longitude,
                        qthLocator = qth,
                        timestampMillis = System.currentTimeMillis(),
                        source = LocationSource.MANUAL_COORDINATES
                    )
                )
            }
            interactionState.update {
                it.copy(
                    gpsRequestInFlight = false,
                    locationMessage = getApplication<Application>().getString(
                        R.string.vm_map_saved,
                        qth ?: getApplication<Application>().getString(
                            R.string.vm_coordinates
                        )
                    ),
                    locationStatusKind = QthGpsStatusKind.Success
                )
            }
        }
    }

    fun clearStationLocation() {
        cancelGpsRequest(message = null, reason = "station-data-deletion")
        viewModelScope.launch {
            val result = runCatching {
                locationSaveMutex.withLock {
                    stationDataDeletionCoordinator.clear()
                }
            }
            result.onSuccess {
                cachedTrackKey = null
                cachedTrack = emptyList()
                passPlanningState.value = PassPlanningProgress(
                    status = PassPlanningStatus.NeedsQth
                )
                interactionState.update { state ->
                    state.copy(
                        selectedPassKey = null,
                        locationMessage = getApplication<Application>().getString(
                            R.string.vm_saved_location_cleared
                        ),
                        locationStatusKind = QthGpsStatusKind.Success,
                        gpsRequestInFlight = false,
                        locationRequestGeneration = QthGpsRequestPolicy.invalidateRequest(
                            state.locationRequestGeneration
                        )
                    )
                }
                minuteClockWakeSignal.trySend(Unit)
            }.onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                interactionState.update { state ->
                    state.copy(
                        locationMessage = getApplication<Application>().getString(
                            R.string.vm_clear_location_failed
                        ),
                        locationStatusKind = QthGpsStatusKind.Failure
                    )
                }
            }
        }
    }

    fun toggleSatellite(catalogNumber: Int) {
        viewModelScope.launch {
            val settings = settingsStore.getSettings()
            repository.setSelectedSatelliteIds(settings.selectedSatelliteIds.toggle(catalogNumber))
        }
    }

    fun clearSelectedSatellites() {
        viewModelScope.launch {
            repository.setSelectedSatelliteIds(emptySet())
        }
    }

    fun applyStarterSelection(satellites: List<SatelliteRecord>) {
        viewModelScope.launch {
            repository.setSelectedSatelliteIds(StarterSelectionPolicy.pickStarterSelection(satellites))
        }
    }

    fun setPassWindow(hours: Int) {
        viewModelScope.launch {
            settingsStore.setPassWindowHours(hours.coerceIn(1, 96))
        }
    }

    fun setMapTileMode(mode: MapTileMode) {
        viewModelScope.launch {
            settingsStore.setMapTileMode(mode)
        }
    }

    fun setThemePreset(preset: AppThemePreset) {
        viewModelScope.launch {
            settingsStore.setThemePreset(preset)
        }
    }

    fun setDeveloperOptionsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setDeveloperOptionsEnabled(enabled)
        }
    }

    fun setRadarKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setRadarKeepScreenOn(enabled)
        }
    }

    fun setRadarForwardAxis(axis: RadarForwardAxis) {
        viewModelScope.launch {
            settingsStore.setRadarForwardAxis(axis)
        }
    }

    fun setRadarFallbackWristSide(side: RadarWristSide) {
        viewModelScope.launch {
            settingsStore.setRadarFallbackWristSide(side)
        }
    }

    fun setSetupStep(step: String) {
        viewModelScope.launch {
            setupCompletionMutex.withLock {
                if (step != FirstRunSetupStep.Qth.storedName) {
                    cancelGpsRequest()
                }
                settingsStore.setSetupStep(step)
            }
        }
    }

    fun skipSetupStepAndMoveTo(step: String, nextStep: String) {
        viewModelScope.launch {
            setupCompletionMutex.withLock {
                if (step == FirstRunSetupStep.Qth.storedName) {
                    cancelGpsRequest()
                }
                settingsStore.skipSetupStepAndMoveTo(step, nextStep)
            }
        }
    }

    fun completeSetup() {
        viewModelScope.launch {
            setupCompletionMutex.withLock {
                cancelGpsRequest(message = null)
                try {
                    val latestCatalogNumbers = withContext(Dispatchers.IO) {
                        repository.getCachedSatellites()
                            .asSequence()
                            .map { satellite -> satellite.catalogNumber }
                            .toSet()
                    }
                    when (settingsStore.completeSetupIfEligible(latestCatalogNumbers)) {
                        SetupCompletionResult.Completed,
                        SetupCompletionResult.AlreadyCompleted -> Unit
                        is SetupCompletionResult.Rejected -> {
                            interactionState.update { state ->
                                state.copy(
                                    refreshMessage = getApplication<Application>().getString(
                                        R.string.vm_complete_setup_first
                                    ),
                                    refreshFailureEventId = state.refreshFailureEventId + 1L
                                )
                            }
                        }
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    interactionState.update { state ->
                        state.copy(
                            refreshMessage = getApplication<Application>().getString(
                                R.string.vm_finish_setup_failed
                            ),
                            refreshFailureEventId = state.refreshFailureEventId + 1L
                        )
                    }
                }
            }
        }
    }

    fun setPassAlertAdvanceMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsStore.setPassAlertAdvanceMinutes(PassAlertAdvancePolicy.coerceMinutes(minutes))
        }
    }

    fun setAutoDataFreshnessEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setAutoDataFreshnessEnabled(enabled)
        }
    }

    fun setMinimumElevationFilterEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setMinimumElevationFilterEnabled(enabled)
        }
    }

    fun setMinimumElevationDegrees(degrees: Int) {
        viewModelScope.launch {
            settingsStore.setMinimumElevationDegrees(MinimumElevationPolicy.coerceThresholdDegrees(degrees))
        }
    }

    fun selectPass(pass: SatellitePass) {
        interactionState.update {
            it.copy(
                selectedPassKey = pass.toKey(),
                selectedRadarTransmitterUuid = null
            )
        }
    }

    fun selectRadarTransmitter(uuid: String?) {
        interactionState.update {
            it.copy(selectedRadarTransmitterUuid = uuid)
        }
    }

    fun startRadarUpdates(updateMode: RadarUpdateMode = RadarUpdateMode.Interactive) {
        if (radarJob?.isActive == true && radarUpdateMode == updateMode) return
        radarJob?.cancel()
        radarUpdateMode = updateMode
        interactionState.update {
            it.copy(radarActive = true, radarNowMillis = System.currentTimeMillis())
        }
        radarJob = viewModelScope.launch {
            while (true) {
                interactionState.update { it.copy(radarNowMillis = System.currentTimeMillis()) }
                delay(RadarPowerPolicy.radarUpdateIntervalMillis(updateMode))
            }
        }
    }

    fun stopRadarUpdates() {
        radarJob?.cancel()
        radarJob = null
        radarUpdateMode = RadarUpdateMode.Interactive
        interactionState.update { it.copy(radarActive = false) }
    }

    fun startOrbitMapUpdates() {
        if (orbitMapJob?.isActive == true) return
        orbitMapJob = viewModelScope.launch {
            while (true) {
                currentOrbitMapRequest()?.let { request ->
                    updateOrbitMapSnapshot(request)
                }
                val now = System.currentTimeMillis()
                delay(
                    OrbitMapUpdatePolicy.CurrentPositionIntervalMillis -
                        (now % OrbitMapUpdatePolicy.CurrentPositionIntervalMillis)
                )
            }
        }
    }

    fun stopOrbitMapUpdates() {
        orbitMapJob?.cancel()
        orbitMapJob = null
        orbitMapImmediateJob?.cancel()
        orbitMapImmediateJob = null
    }

    fun startOrbitMapDetailUpdates(catalogNumber: Int) {
        require(catalogNumber > 0) { "Catalog number must be positive" }
        if (
            orbitMapDetailJob?.isActive == true &&
            orbitMapDetailRuntimeState.value.catalogNumber == catalogNumber
        ) {
            return
        }
        stopOrbitMapDetailUpdates()
        val runtime = orbitMapDetailRuntimeState.updateAndGet { previous ->
            OrbitMapDetailRuntimeReducer.start(
                previous = previous,
                catalogNumber = catalogNumber
            )
        }
        val request = OrbitMapDetailRequestPolicy.requestOrNull(
            catalogNumber = runtime.catalogNumber,
            generation = runtime.generation
        ) ?: return
        orbitMapDetailJob = viewModelScope.launch {
            orbitMapDetailUpdateLoop.run(
                request = request,
                update = ::updateOrbitMapDetailSnapshot
            )
        }
    }

    fun stopOrbitMapDetailUpdates() {
        val activeJob = orbitMapDetailJob ?: return
        orbitMapDetailJob = null
        activeJob.cancel()
        orbitMapDetailRuntimeState.update(OrbitMapDetailRuntimeReducer::stop)
    }

    fun selectNextOrbitSatellite() {
        val candidates = OrbitMapSatelliteSelector.candidates(currentSatelliteRecords())
        val nextCatalogNumber = OrbitMapSelectionReducer.nextCatalogNumber(
            candidates = candidates,
            currentCatalogNumber = currentOrbitMapCatalogNumber(candidates)
        )
        selectOrbitSatellite(nextCatalogNumber)
    }

    fun selectPreviousOrbitSatellite() {
        val candidates = OrbitMapSatelliteSelector.candidates(currentSatelliteRecords())
        val previousCatalogNumber = OrbitMapSelectionReducer.previousCatalogNumber(
            candidates = candidates,
            currentCatalogNumber = currentOrbitMapCatalogNumber(candidates)
        )
        selectOrbitSatellite(previousCatalogNumber)
    }

    fun startMinuteUpdates() {
        if (minuteJob?.isActive == true) return
        minuteJob = viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                interactionState.update { it.copy(minuteNowMillis = now) }
                val delayMillis = PassUiClockPolicy.nextWakeDelayMillis(
                    nowMillis = now,
                    passes = passPlanningState.value.passes
                )
                withTimeoutOrNull(delayMillis) {
                    minuteClockWakeSignal.receive()
                }
            }
        }
    }

    fun stopMinuteUpdates() {
        minuteJob?.cancel()
        minuteJob = null
    }

    private fun observePassPlanningInputs() {
        viewModelScope.launch {
            combine(
                settingsStore.settings,
                satelliteRecordsFlow,
                interactionState
                    .map { interaction ->
                        PassPlanningClock.hourBucketStartMillis(interaction.minuteNowMillis)
                    }
                    .distinctUntilChanged()
            ) { settings, satellites, planningBucketMillis ->
                val selectedSatellites = satellites.filter { it.selected }
                val input = PassPlanningInput(
                    settings = settings,
                    satellites = selectedSatellites.ifEmpty {
                        val starterIds = StarterSelectionPolicy.pickStarterSelection(satellites)
                        satellites.filter { it.catalogNumber in starterIds }
                    },
                    nowMillis = System.currentTimeMillis(),
                    minimumElevationDegrees = MinimumElevationPolicy.effectiveMinimumElevationDegrees(settings)
                )
                planningBucketMillis to input
                }
                .distinctUntilChanged { old, new ->
                    old.first == new.first && old.second.isSamePlanningRequestAs(new.second)
                }
                .collectLatest { (_, input) ->
                    runCatching {
                        passPlanner.run(input) { progress ->
                            passPlanningState.value = progress
                            minuteClockWakeSignal.trySend(Unit)
                        }
                    }.onFailure { throwable ->
                        if (throwable is CancellationException) throw throwable
                        passPlanningState.value = PassPlanningProgress(
                            status = PassPlanningStatus.Failed,
                            targetWindowHours = input.settings.passWindowHours
                        )
                        minuteClockWakeSignal.trySend(Unit)
                    }
                }
        }
    }

    private fun observePassSnapshotRenewalInputs() {
        val tracker = PassSnapshotRenewalInputTracker()
        viewModelScope.launch {
            combine(
                settingsStore.settings,
                satelliteRecordsFlow
            ) { settings, satellites ->
                PassSnapshotRenewalInputPolicy.keyFor(settings, satellites)
            }
                .distinctUntilChanged()
                .collect { key ->
                    tracker.nextReason(key)?.let { reason ->
                        PassSnapshotRenewalEnqueuer.enqueueImmediate(
                            context = getApplication(),
                            reason = reason
                        )
                    }
                }
        }
    }

    private fun observeLatestSettings() {
        viewModelScope.launch {
            settingsStore.settings.collect { settings ->
                latestSettingsState.value = settings
            }
        }
    }

    private fun buildUiState(
        settings: Watch4SatSettings,
        satellites: List<SatelliteRecord>,
        satelliteCatalogLoaded: Boolean,
        datasetRetrievedAtMillis: Long?,
        transmitters: List<TransmitterRecord>,
        interaction: WatchInteractionState,
        passPlanning: PassPlanningProgress,
        orbitMapRuntime: OrbitMapRuntimeState
    ): WatchUiState {
        val nowMillis = if (interaction.radarActive) {
            maxOf(interaction.minuteNowMillis, interaction.radarNowMillis)
        } else {
            interaction.minuteNowMillis
        }
        val station = settings.stationLocation ?: DefaultStation
        val selectedSatellites = satellites.filter { it.selected }
        val freshnessSatellites =
            TleFreshnessScopePolicy.relevantSatellites(satellites)
        val tleFreshness = TleFreshnessPolicy.assess(
            nowMillis = nowMillis,
            retrievedAtMillis =
                datasetRetrievedAtMillis ?: settings.lastSatelliteDataUpdateMillis,
            samples = freshnessSatellites.map { satellite ->
                TleEpochSample(
                    catalogNumber = satellite.catalogNumber,
                    epoch = satellite.orbitalData.epoch
                )
            }
        )
        val passWindowEndMillis = nowMillis + settings.passWindowHours.coerceAtLeast(1) * MillisPerHour
        val passes = passPlanning.passes
            .filter { pass -> pass.losMillis > nowMillis && pass.aosMillis < passWindowEndMillis }
            .sortedWith(compareBy<SatellitePass> { it.aosMillis }.thenBy { it.catalogNumber })
        val passCards = passes.map { pass ->
            pass to PassCardMapper.map(
                pass = pass,
                transmitters = transmitters,
                nowMillis = nowMillis,
                textFormatter = passCardTextFormatter,
                zoneId = ZoneId.systemDefault(),
                clockTimeFormatter = AndroidClockTimeFormatter.create(getApplication())
            )
        }
        val orbitMap = buildOrbitMapUiState(
            satellites = satellites,
            passCards = passCards,
            runtime = orbitMapRuntime
        )
        val focusedPass = interaction.selectedPassKey?.let { key ->
            passes.firstOrNull { it.toKey() == key }
        } ?: passes.firstOrNull { nowMillis < it.losMillis }
        val focusedTransmitters = focusedPass?.let { pass ->
            RadarTransmitterSelector.optionsFor(pass.catalogNumber, transmitters)
        }.orEmpty()
        val focusedTransmitter = if (interaction.radarActive) {
            RadarTransmitterSelector.selectedTransmitter(
                options = focusedTransmitters,
                selectedUuid = interaction.selectedRadarTransmitterUuid
            )
        } else {
            null
        }
        val radarNow = interaction.radarNowMillis
        val focusedPosition = if (interaction.radarActive) focusedPass?.let { pass ->
            runCatching {
                PassPredictionService.positionAt(
                    pass = pass,
                    station = station,
                    timeMillis = radarNow.coerceIn(pass.aosMillis, pass.losMillis)
                )
            }.getOrNull()
        } else null
        val focusedTrack = if (interaction.radarActive) {
            focusedPass?.let { pass -> trackFor(pass, station) }.orEmpty()
        } else {
            emptyList()
        }
        val doppler = if (interaction.radarActive) {
            focusedPass?.let { pass ->
                focusedPosition?.let { position ->
                    DopplerCalculator.calculate(
                        baseDownlinkHz = settings.customDopplerBaseFrequencies[pass.catalogNumber]?.downlinkHz
                            ?: focusedTransmitter?.downlinkLowHz
                            ?: focusedTransmitter?.downlinkHighHz,
                        baseUplinkHz = settings.customDopplerBaseFrequencies[pass.catalogNumber]?.uplinkHz
                            ?: focusedTransmitter?.uplinkLowHz
                            ?: focusedTransmitter?.uplinkHighHz,
                        position = position
                    )
                }
            }
        } else null
        return WatchUiState(
            settings = settings,
            settingsLoaded = true,
            nowMillis = nowMillis,
            station = station,
            hasStationLocation = settings.stationLocation != null,
            satellites = satellites,
            satelliteCatalogLoaded = satelliteCatalogLoaded,
            selectedSatelliteIds = settings.selectedSatelliteIds,
            selectedSatelliteCount = selectedSatellites.size,
            tleFreshness = tleFreshness,
            transmitters = transmitters,
            passCards = passCards,
            passPlanningStatus = passPlanning.status,
            passPlanningMessage = passPlanning.resolveMessage(),
            passCoverageHours = passPlanning.coveredWindowHours,
            selectedPassKey = interaction.selectedPassKey?.toNotificationKey(),
            focusedPass = focusedPass,
            focusedTrack = focusedTrack,
            focusedPosition = focusedPosition,
            focusedTransmitters = focusedTransmitters,
            focusedTransmitter = focusedTransmitter,
            doppler = doppler,
            orbitMap = orbitMap,
            radarNowMillis = radarNow,
            refreshMessage = interaction.refreshMessage,
            refreshSuccessEventId = interaction.refreshSuccessEventId,
            refreshFailureEventId = interaction.refreshFailureEventId,
            refreshInFlight = interaction.refreshInFlight,
            locationMessage = interaction.locationMessage,
            locationStatusKind = interaction.locationStatusKind,
            gpsRequestInFlight = interaction.gpsRequestInFlight,
            gpsFailureMessage = interaction.gpsFailureMessage,
            gpsFailureEventId = interaction.gpsFailureEventId
        )
    }

    private fun buildOrbitMapUiState(
        satellites: List<SatelliteRecord>,
        passCards: List<Pair<SatellitePass, PassCardUi>>,
        runtime: OrbitMapRuntimeState
    ): OrbitMapUiState {
        val candidates = OrbitMapSatelliteSelector.candidates(satellites)
        val selectedCatalogNumber = OrbitMapSatelliteSelector.initialCatalogNumber(
            candidates = candidates,
            passCards = passCards,
            requestedCatalogNumber = runtime.selectedCatalogNumber
        )
        val selectedSatellite = candidates.firstOrNull { it.catalogNumber == selectedCatalogNumber }
        val runtimeMatchesSelection = selectedCatalogNumber != null && selectedCatalogNumber == runtime.selectedCatalogNumber
        return OrbitMapUiState(
            candidates = candidates,
            selectedCatalogNumber = selectedCatalogNumber,
            selectedSatellite = selectedSatellite,
            currentPosition = runtime.currentPosition.takeIf { runtimeMatchesSelection },
            trackSegments = runtime.trackSegments.takeIf { runtimeMatchesSelection }.orEmpty(),
            footprint = runtime.footprint.takeIf { runtimeMatchesSelection },
            message = when {
                candidates.isEmpty() -> getApplication<Application>().getString(
                    R.string.vm_refresh_tle_first
                )
                selectedSatellite == null -> getApplication<Application>().getString(
                    R.string.vm_select_satellite
                )
                runtimeMatchesSelection -> runtime.message
                else -> getApplication<Application>().getString(
                    R.string.vm_loading_satellite,
                    selectedSatellite.displayName
                )
            },
            lastUpdatedMillis = runtime.lastUpdatedMillis.takeIf { runtimeMatchesSelection } ?: 0L
        )
    }

    private fun selectOrbitSatellite(catalogNumber: Int?) {
        orbitMapSavedSelection.selectedCatalogNumber = catalogNumber
        val selection = orbitMapRuntimeState.updateAndGet { runtime ->
            runtime.copy(
                selectedCatalogNumber = catalogNumber,
                generation = OrbitMapRequestPolicy.nextGeneration(runtime.generation),
                currentPosition = null,
                trackSegments = emptyList(),
                footprint = null,
                message = catalogNumber?.let { selected ->
                    getApplication<Application>().getString(
                        R.string.vm_loading_catalog,
                        selected
                    )
                } ?: getApplication<Application>().getString(
                    R.string.vm_refresh_tle_first
                ),
                lastUpdatedMillis = 0L,
                trackBucketMillis = Long.MIN_VALUE,
                trackCatalogNumber = null
            )
        }
        orbitMapImmediateJob?.cancel()
        orbitMapImmediateJob = null
        if (orbitMapJob?.isActive == true) {
            OrbitMapRequestPolicy.requestOrNull(
                catalogNumber = selection.selectedCatalogNumber,
                generation = selection.generation
            )?.let { request ->
                orbitMapImmediateJob = viewModelScope.launch {
                    updateOrbitMapSnapshot(request)
                }
            }
        }
    }

    private fun currentOrbitMapRequest(): OrbitMapRequestKey? {
        val catalogState = satelliteCatalogState.value
        if (!catalogState.loaded) return null
        val candidates = OrbitMapSatelliteSelector.candidates(catalogState.catalog.records)
        val selection = orbitMapRuntimeState.updateAndGet { runtime ->
            val reconciliation = OrbitMapSatelliteSelector.reconcileCatalogNumber(
                candidates = candidates,
                preferredCatalogNumber = preferredOrbitMapCatalogNumber(candidates),
                requestedCatalogNumber = runtime.selectedCatalogNumber
            )
            if (!reconciliation.candidatesReady) return@updateAndGet runtime
            val selectedCatalogNumber = reconciliation.selectedCatalogNumber
            if (selectedCatalogNumber == runtime.selectedCatalogNumber) {
                runtime
            } else {
                orbitMapSavedSelection.selectedCatalogNumber = selectedCatalogNumber
                runtime.copy(
                    selectedCatalogNumber = selectedCatalogNumber,
                    generation = OrbitMapRequestPolicy.nextGeneration(runtime.generation),
                    currentPosition = null,
                    trackSegments = emptyList(),
                    footprint = null,
                    message = selectedCatalogNumber?.let {
                        getApplication<Application>().getString(
                            R.string.vm_loading_catalog,
                            it
                        )
                    } ?: getApplication<Application>().getString(
                        R.string.vm_refresh_tle_first
                    ),
                    lastUpdatedMillis = 0L,
                    trackBucketMillis = Long.MIN_VALUE,
                    trackCatalogNumber = null
                )
            }
        }
        if (candidates.isEmpty()) return null
        return OrbitMapRequestPolicy.requestOrNull(
            catalogNumber = selection.selectedCatalogNumber,
            generation = selection.generation
        )
    }

    private fun currentOrbitMapCatalogNumber(
        candidates: List<SatelliteRecord>,
        runtime: OrbitMapRuntimeState = orbitMapRuntimeState.value
    ): Int? {
        return OrbitMapSatelliteSelector.initialCatalogNumber(
            candidates = candidates,
            preferredCatalogNumber = preferredOrbitMapCatalogNumber(candidates),
            requestedCatalogNumber = runtime.selectedCatalogNumber
        )
    }

    private fun currentSatelliteRecords(): List<SatelliteRecord> {
        return satelliteCatalogState.value.catalog.records
    }

    private fun preferredOrbitMapCatalogNumber(candidates: List<SatelliteRecord>): Int? {
        val nowMillis = System.currentTimeMillis()
        val candidateIds = candidates.mapTo(mutableSetOf()) { it.catalogNumber }
        val usablePasses = passPlanningState.value.passes
            .asSequence()
            .filter { pass -> pass.catalogNumber in candidateIds && pass.losMillis > nowMillis }
            .sortedWith(compareBy<SatellitePass> { it.aosMillis }.thenBy { it.catalogNumber })
            .toList()
        return usablePasses.firstOrNull { pass -> pass.aosMillis <= nowMillis }?.catalogNumber
            ?: usablePasses.firstOrNull()?.catalogNumber
    }

    private suspend fun updateOrbitMapSnapshot(request: OrbitMapRequestKey) {
        val satellite = currentSatelliteRecords().firstOrNull {
            it.catalogNumber == request.catalogNumber
        }
        if (satellite == null) {
            orbitMapRuntimeState.update { runtime ->
                if (runtime.accepts(request)) {
                    runtime.copy(
                        currentPosition = null,
                        trackSegments = emptyList(),
                        footprint = null,
                        message = getApplication<Application>().getString(
                            R.string.vm_refresh_tle_first
                        ),
                        lastUpdatedMillis = 0L,
                        trackBucketMillis = Long.MIN_VALUE,
                        trackCatalogNumber = null
                    )
                } else {
                    runtime
                }
            }
            return
        }
        val nowMillis = System.currentTimeMillis()
        val trackBucketMillis = OrbitMapUpdatePolicy.groundTrackBucketMillis(nowMillis)
        val footprintBucketMillis = OrbitMapUpdatePolicy.footprintBucketMillis(nowMillis)
        val previous = orbitMapRuntimeState.value
        val result = withContext(Dispatchers.Default) {
            runCatching {
                val currentPosition = PassPredictionService.groundPositionAt(satellite, nowMillis).toGroundTrackPoint()
                val footprint = SatelliteFootprintCalculator.calculate(
                    point = currentPosition,
                    generatedAtMillis = footprintBucketMillis
                )
                val trackSegments = if (
                    previous.trackCatalogNumber == satellite.catalogNumber &&
                    previous.trackBucketMillis == trackBucketMillis &&
                    previous.trackSegments.isNotEmpty()
                ) {
                    previous.trackSegments
                } else {
                    GroundTrackSegmenter.splitAtAntimeridian(
                        PassPredictionService.groundTrackFor(
                            satellite = satellite,
                            centerTimeMillis = trackBucketMillis,
                            intervalSeconds = 120,
                            pointsBefore = 15,
                            pointsAfter = 30
                        )
                    )
                }
                OrbitMapSnapshot(
                    currentPosition = currentPosition,
                    trackSegments = trackSegments,
                    footprint = footprint
                )
            }
        }
        result.exceptionOrNull()?.let { throwable ->
            if (throwable is CancellationException) throw throwable
        }
        orbitMapRuntimeState.update { runtime ->
            if (!runtime.accepts(request)) {
                runtime
            } else result.fold(
                onSuccess = { snapshot ->
                    runtime.copy(
                        currentPosition = snapshot.currentPosition,
                        trackSegments = snapshot.trackSegments,
                        footprint = snapshot.footprint,
                        message = getApplication<Application>().getString(
                            R.string.vm_updated_satellite,
                            satellite.displayName
                        ),
                        lastUpdatedMillis = nowMillis,
                        trackBucketMillis = trackBucketMillis,
                        trackCatalogNumber = satellite.catalogNumber
                    )
                },
                onFailure = {
                    runtime.copy(
                        currentPosition = null,
                        trackSegments = emptyList(),
                        footprint = null,
                        message = getApplication<Application>().getString(
                            R.string.vm_orbit_map_unavailable
                        ),
                        lastUpdatedMillis = nowMillis,
                        trackBucketMillis = Long.MIN_VALUE,
                        trackCatalogNumber = null
                    )
                }
            )
        }
    }

    private suspend fun updateOrbitMapDetailSnapshot(
        request: OrbitMapDetailRequestKey
    ) {
        val satellite = currentSatelliteRecords().firstOrNull {
            it.catalogNumber == request.catalogNumber
        }
        if (satellite == null) {
            orbitMapDetailRuntimeState.update { runtime ->
                OrbitMapDetailRuntimeReducer.commitMissingSatellite(
                    previous = runtime,
                    request = request
                )
            }
            return
        }
        val savedStation = latestSettingsState.value?.stationLocation
        val nowMillis = System.currentTimeMillis()
        val result = withContext(Dispatchers.Default) {
            runCatching {
                val position = OrbitMapDetailPositionResolver.resolve(
                    satellite = satellite,
                    savedStation = savedStation,
                    timeMillis = nowMillis
                )
                OrbitMapDetailSnapshot(
                    currentPosition = position.toGroundTrackPoint(),
                    footprintRadiusKm = SatelliteFootprintCalculator
                        .radiusKmForAltitude(position.altitudeKm),
                    slantRangeKm = position.slantRangeKm
                )
            }
        }
        result.exceptionOrNull()?.let { throwable ->
            if (throwable is CancellationException) throw throwable
        }
        orbitMapDetailRuntimeState.update { runtime ->
            result.fold(
                onSuccess = { snapshot ->
                    OrbitMapDetailRuntimeReducer.commitSuccess(
                        previous = runtime,
                        request = request,
                        currentPosition = snapshot.currentPosition,
                        footprintRadiusKm = snapshot.footprintRadiusKm,
                        slantRangeKm = snapshot.slantRangeKm
                    )
                },
                onFailure = {
                    OrbitMapDetailRuntimeReducer.commitFailure(
                        previous = runtime
                    )
                }
            )
        }
    }

    private fun trackFor(pass: SatellitePass, station: StationLocation): List<RadarTrackPoint> {
        val key = TrackCacheKey(
            catalogNumber = pass.catalogNumber,
            aosMillis = pass.aosMillis,
            stationLatitude = station.latitude,
            stationLongitude = station.longitude,
            stationAltitudeMeters = station.altitudeMeters
        )
        if (cachedTrackKey == key) return cachedTrack
        cachedTrackKey = key
        cachedTrack = runCatching {
            PassPredictionService.trackFor(pass, station, intervalSeconds = 20)
        }.getOrDefault(emptyList())
        return cachedTrack
    }

    private fun Context.hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun LocationResultState.label(): String {
        return when (this) {
            LocationResultState.NO_PERMISSION -> R.string.vm_location_permission_needed
            LocationResultState.LOCATION_PROVIDER_DISABLED ->
                R.string.vm_location_unavailable
            LocationResultState.NO_LAST_KNOWN_LOCATION -> R.string.vm_no_location_fix
            LocationResultState.NO_FRESH_LOCATION -> R.string.vm_no_fresh_location_fix
            LocationResultState.LOW_ACCURACY_LOCATION ->
                R.string.vm_location_accuracy_low
            LocationResultState.TIMEOUT -> R.string.vm_location_timeout
            LocationResultState.SUCCESS -> R.string.vm_location_saved
            LocationResultState.ERROR -> R.string.vm_location_error
        }.let { resource ->
            getApplication<Application>().getString(resource)
        }
    }

    private fun LocationResultState.statusKind(): QthGpsStatusKind =
        if (this == LocationResultState.SUCCESS) {
            QthGpsStatusKind.Success
        } else {
            QthGpsStatusKind.Failure
        }

    private fun PassPlanningProgress.resolveMessage(): String {
        val application = getApplication<Application>()
        return when (status) {
            PassPlanningStatus.Idle,
            PassPlanningStatus.NeedsQth ->
                application.getString(R.string.vm_set_qth_for_passes)
            PassPlanningStatus.NoSatellites ->
                application.getString(R.string.vm_refresh_tle_select_satellites)
            PassPlanningStatus.FromSnapshot ->
                application.getString(R.string.vm_updating_passes)
            PassPlanningStatus.Calculating -> {
                if (coveredWindowHours > 0 && targetWindowHours > 0) {
                    application.getString(
                        R.string.vm_calculated_pass_hours,
                        coveredWindowHours,
                        targetWindowHours
                    )
                } else {
                    application.getString(R.string.vm_updating_passes)
                }
            }
            PassPlanningStatus.Ready ->
                application.getString(R.string.vm_passes_ready)
            PassPlanningStatus.Failed ->
                application.getString(R.string.vm_pass_calculation_failed)
        }
    }

    private fun com.xianming.watch4sat.location.LocationResult.toMessage(): String {
        return stationLocation?.qthLocator?.let { locator ->
            getApplication<Application>().getString(
                R.string.vm_location_with_locator,
                stationLocation.source.locationLabel(),
                locator
            )
        } ?: state.label()
    }

    private fun LocationSource.locationLabel(): String {
        val resource = when (this) {
            LocationSource.GPS -> R.string.vm_location_source_gps
            LocationSource.NETWORK -> R.string.vm_location_source_network
            LocationSource.FUSED -> R.string.vm_location_source_fused
            LocationSource.MANUAL_QTH,
            LocationSource.MANUAL_COORDINATES -> R.string.vm_location_source_generic
        }
        return getApplication<Application>().getString(resource)
    }
}

data class WatchUiState(
    val settings: Watch4SatSettings = Watch4SatSettings(),
    val settingsLoaded: Boolean = false,
    val nowMillis: Long = System.currentTimeMillis(),
    val station: StationLocation = DefaultStation,
    val hasStationLocation: Boolean = false,
    val satellites: List<SatelliteRecord> = emptyList(),
    val satelliteCatalogLoaded: Boolean = false,
    val selectedSatelliteIds: Set<Int> = emptySet(),
    val selectedSatelliteCount: Int = 0,
    val tleFreshness: TleFreshnessAssessment = TleFreshnessPolicy.assess(
        nowMillis = System.currentTimeMillis(),
        retrievedAtMillis = null,
        samples = emptyList()
    ),
    val transmitters: List<TransmitterRecord> = emptyList(),
    val passCards: List<Pair<SatellitePass, PassCardUi>> = emptyList(),
    val passPlanningStatus: PassPlanningStatus = PassPlanningStatus.Idle,
    val passPlanningMessage: String = "",
    val passCoverageHours: Int = 0,
    val selectedPassKey: String? = null,
    val focusedPass: SatellitePass? = null,
    val focusedTrack: List<RadarTrackPoint> = emptyList(),
    val focusedPosition: OrbitalPosition? = null,
    val focusedTransmitters: List<TransmitterRecord> = emptyList(),
    val focusedTransmitter: TransmitterRecord? = null,
    val doppler: DopplerReading? = null,
    val orbitMap: OrbitMapUiState = OrbitMapUiState(),
    val radarNowMillis: Long = System.currentTimeMillis(),
    val refreshMessage: String = "",
    val refreshSuccessEventId: Long = 0L,
    val refreshFailureEventId: Long = 0L,
    val refreshInFlight: Boolean = false,
    val locationMessage: String = "",
    val locationStatusKind: QthGpsStatusKind = QthGpsStatusKind.Neutral,
    val gpsRequestInFlight: Boolean = false,
    val gpsFailureMessage: String? = null,
    val gpsFailureEventId: Long = 0L
)

private data class WatchAppStateInputs(
    val settings: Watch4SatSettings,
    val satelliteCatalog: SatelliteCatalogLoadState,
    val transmitters: List<TransmitterRecord>,
    val interaction: WatchInteractionState,
    val passPlanning: PassPlanningProgress
)

private data class SatelliteCatalogLoadState(
    val catalog: SatelliteCatalog = SatelliteCatalog(records = emptyList()),
    val loaded: Boolean = false
)

private data class WatchInteractionState(
    val selectedPassKey: PassKey? = null,
    val selectedRadarTransmitterUuid: String? = null,
    val radarActive: Boolean = false,
    val minuteNowMillis: Long = System.currentTimeMillis(),
    val radarNowMillis: Long = System.currentTimeMillis(),
    val refreshMessage: String = "",
    val refreshSuccessEventId: Long = 0L,
    val refreshFailureEventId: Long = 0L,
    val refreshInFlight: Boolean = false,
    val locationMessage: String = "",
    val locationStatusKind: QthGpsStatusKind = QthGpsStatusKind.Neutral,
    val gpsRequestInFlight: Boolean = false,
    val gpsFailureMessage: String? = null,
    val gpsFailureEventId: Long = 0L,
    val locationRequestGeneration: Long = 0L
)

private data class OrbitMapRuntimeState(
    val selectedCatalogNumber: Int? = null,
    val generation: Long = 0L,
    val currentPosition: GroundTrackPoint? = null,
    val trackSegments: List<List<GroundTrackPoint>> = emptyList(),
    val footprint: SatelliteFootprint? = null,
    val message: String = "",
    val lastUpdatedMillis: Long = 0L,
    val trackBucketMillis: Long = Long.MIN_VALUE,
    val trackCatalogNumber: Int? = null
) {
    fun accepts(request: OrbitMapRequestKey): Boolean {
        return OrbitMapRequestPolicy.canCommit(
            request = request,
            selectedCatalogNumber = selectedCatalogNumber,
            currentGeneration = generation
        )
    }
}

private data class OrbitMapSnapshot(
    val currentPosition: GroundTrackPoint,
    val trackSegments: List<List<GroundTrackPoint>>,
    val footprint: SatelliteFootprint
)

private data class OrbitMapDetailSnapshot(
    val currentPosition: GroundTrackPoint,
    val footprintRadiusKm: Double?,
    val slantRangeKm: Double?
)

private data class PassKey(
    val catalogNumber: Int,
    val aosMillis: Long
)

private data class TrackCacheKey(
    val catalogNumber: Int,
    val aosMillis: Long,
    val stationLatitude: Double,
    val stationLongitude: Double,
    val stationAltitudeMeters: Double
)

private fun SatellitePass.toKey(): PassKey {
    return PassKey(catalogNumber = catalogNumber, aosMillis = aosMillis)
}

private fun PassKey.toNotificationKey(): String {
    return "${catalogNumber}:${aosMillis}"
}

private fun OrbitalPosition.toGroundTrackPoint(): GroundTrackPoint {
    return GroundTrackPoint(
        timeMillis = timeMillis,
        latitudeDegrees = requireNotNull(latitudeDegrees),
        longitudeDegrees = requireNotNull(longitudeDegrees),
        altitudeKm = altitudeKm
    )
}

private fun Set<Int>.toggle(catalogNumber: Int): Set<Int> {
    return if (contains(catalogNumber)) this - catalogNumber else this + catalogNumber
}

private const val MillisPerMinute = 60_000L
private const val MillisPerHour = 60L * 60L * 1000L
private const val ForegroundFreshnessFailureCooldownMillis: Long = 30 * 60_000L

val DefaultStation = StationLocation(
    latitude = 31.23,
    longitude = 121.47,
    qthLocator = "OM89XX",
    source = LocationSource.MANUAL_QTH
)
