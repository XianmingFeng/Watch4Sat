package com.xianming.watch4sat.wear

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.PagerDefaults
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.foundation.AmbientMode
import androidx.wear.compose.foundation.LocalAmbientModeManager
import androidx.wear.compose.foundation.rememberSwipeToDismissBoxState
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.HorizontalPageIndicator
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.AnimatedPage
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonGroup
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CheckboxButtonDefaults
import androidx.wear.compose.material3.ConfirmationDialogDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.CircularProgressIndicatorDefaults
import androidx.wear.compose.material3.FailureConfirmationDialog
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.LevelIndicator
import androidx.wear.compose.material3.LevelIndicatorDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Picker
import androidx.wear.compose.material3.PagerScaffoldDefaults
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.RadioButtonDefaults
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SplitCheckboxButton
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.SwitchButtonDefaults
import androidx.wear.compose.material3.SuccessConfirmationDialog
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.confirmationDialogCurvedText
import androidx.wear.compose.material3.dynamicColorScheme
import androidx.wear.compose.material3.rememberPickerState
import androidx.wear.compose.material3.timeTextCurvedText
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.wear.compose.navigation.rememberSwipeDismissableNavHostState
import androidx.wear.tiles.TileService
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.xianming.watch4sat.BuildConfig
import com.xianming.watch4sat.ExternalLaunchEnvelope
import com.xianming.watch4sat.R
import com.xianming.watch4sat.data.settings.AppThemePreset
import com.xianming.watch4sat.data.settings.MapTileMode
import com.xianming.watch4sat.data.settings.RadarForwardAxis
import com.xianming.watch4sat.data.settings.RadarWristSide
import com.xianming.watch4sat.domain.model.PassCardUi
import com.xianming.watch4sat.domain.model.SatellitePass
import com.xianming.watch4sat.domain.model.SatelliteRecord
import com.xianming.watch4sat.domain.model.StationLocation
import com.xianming.watch4sat.domain.qth.MaidenheadLocator
import com.xianming.watch4sat.tile.NextPassTileService
import com.xianming.watch4sat.wear.firstRun.FirstRunSetupScreen
import com.xianming.watch4sat.wear.location.QthGpsKeepScreenOn
import com.xianming.watch4sat.wear.location.QthGpsPowerPolicy
import com.xianming.watch4sat.wear.legal.LegalDocument
import com.xianming.watch4sat.wear.legal.LegalDocumentCatalog
import com.xianming.watch4sat.wear.legal.LegalRoutes
import com.xianming.watch4sat.wear.legal.OfflineLegalDocumentLoader
import com.xianming.watch4sat.wear.legal.legalDocumentChunks
import com.xianming.watch4sat.wear.legal.readOffMainThread
import com.xianming.watch4sat.wear.orbit.OrbitMapDetailMapper
import com.xianming.watch4sat.wear.orbit.OrbitMapDetailScreen
import com.xianming.watch4sat.wear.orbit.OrbitMapScreen
import com.xianming.watch4sat.wear.radar.RadarScreen
import com.xianming.watch4sat.wear.radar.RadarPowerPolicy
import com.xianming.watch4sat.wear.radar.RadarUiText
import com.xianming.watch4sat.wear.radar.rememberSystemRadarWristSide
import com.xianming.watch4sat.wear.state.AboutInfoLabel
import com.xianming.watch4sat.wear.state.AboutPagePolicy
import com.xianming.watch4sat.wear.state.AppChromePolicy
import com.xianming.watch4sat.wear.state.DashboardHeroAction
import com.xianming.watch4sat.wear.state.DashboardHeroActionSelector
import com.xianming.watch4sat.wear.state.DashboardHeroProgressSelector
import com.xianming.watch4sat.wear.state.DashboardHeroProgressVisibilitySelector
import com.xianming.watch4sat.wear.state.DashboardHeroSubtitleSelector
import com.xianming.watch4sat.wear.state.DashboardProgressAnimationPolicy
import com.xianming.watch4sat.wear.state.DashboardDataStatusPolicy
import com.xianming.watch4sat.wear.state.DashboardRouteItems
import com.xianming.watch4sat.wear.state.DataRefreshDialogPolicy
import com.xianming.watch4sat.wear.state.satelliteDataRefreshStatus
import com.xianming.watch4sat.wear.state.transmitterDataRefreshStatus
import com.xianming.watch4sat.wear.state.DataRefreshFailureKind
import com.xianming.watch4sat.wear.state.DeveloperOptionAction
import com.xianming.watch4sat.wear.state.DeveloperOptionFailure
import com.xianming.watch4sat.wear.state.DeveloperOptionsPolicy
import com.xianming.watch4sat.wear.state.DataFreshnessSettingsPolicy
import com.xianming.watch4sat.wear.state.EdgeButtonContent
import com.xianming.watch4sat.wear.state.TleFreshnessUiPolicy
import com.xianming.watch4sat.wear.state.FirstRunSetupPolicy
import com.xianming.watch4sat.wear.state.FirstRunSetupStep
import com.xianming.watch4sat.wear.state.ExternalLaunchCoordinator
import com.xianming.watch4sat.wear.state.ExternalLaunchDecision
import com.xianming.watch4sat.wear.state.MapSourceSelectionPolicy
import com.xianming.watch4sat.wear.state.MinimumElevationPolicy
import com.xianming.watch4sat.wear.state.MinimumElevationSettingsPolicy
import com.xianming.watch4sat.wear.state.PassCardAnimationPolicy
import com.xianming.watch4sat.wear.state.PassCardInteractionReducer
import com.xianming.watch4sat.wear.state.PassWindowAdjusterPolicy
import com.xianming.watch4sat.wear.state.PassAlertAdvancePolicy
import com.xianming.watch4sat.wear.state.PassAlertStatusPolicy
import com.xianming.watch4sat.wear.state.PassAlertStatusRow
import com.xianming.watch4sat.wear.state.PassAlertsSettingsPolicy
import com.xianming.watch4sat.wear.state.PassStartAlertState
import com.xianming.watch4sat.wear.state.PassStartCountdownSnapshot
import com.xianming.watch4sat.wear.state.PassStartNotificationPolicy
import com.xianming.watch4sat.wear.PendingForegroundPassAlarm
import com.xianming.watch4sat.wear.state.PassStartAlarmUiScheduleKey
import com.xianming.watch4sat.wear.state.PassStartSchedulePolicy
import com.xianming.watch4sat.wear.state.RadarKeepScreenOnPolicy
import com.xianming.watch4sat.wear.state.RadarForwardAxisSettingsPolicy
import com.xianming.watch4sat.wear.state.RadarFallbackWristSideSettingsPolicy
import com.xianming.watch4sat.wear.state.RadarOngoingActivityDecision
import com.xianming.watch4sat.wear.state.RadarOngoingActivityPolicy
import com.xianming.watch4sat.wear.state.RouteUpdateLifecyclePolicy
import com.xianming.watch4sat.wear.state.PassStartReminderDecision
import com.xianming.watch4sat.wear.state.PassStartReminderPolicy
import com.xianming.watch4sat.wear.state.QthGpsFailureDialogPolicy
import com.xianming.watch4sat.wear.state.QthGpsStatusPolicy
import com.xianming.watch4sat.wear.state.QthDisplayPolicy
import com.xianming.watch4sat.wear.state.QthPickerPolicy
import com.xianming.watch4sat.wear.state.RoundListSurface
import com.xianming.watch4sat.wear.state.SatelliteListPolicy
import com.xianming.watch4sat.wear.state.SatelliteDetailActionPolicy
import com.xianming.watch4sat.wear.state.SatellitePagerPage
import com.xianming.watch4sat.wear.state.SatellitePagerPolicy
import com.xianming.watch4sat.wear.state.SatelliteRemovalAnimationPolicy
import com.xianming.watch4sat.wear.state.SatelliteSplitCheckboxColorPolicy
import com.xianming.watch4sat.wear.state.SatelliteSplitCheckboxTone
import com.xianming.watch4sat.wear.state.SatelliteDetailMapper
import com.xianming.watch4sat.wear.state.SettingsMenuPolicy
import com.xianming.watch4sat.wear.state.SettingsMenuKey
import com.xianming.watch4sat.wear.state.StartupDrawnPolicy
import com.xianming.watch4sat.wear.theme.LocalWatchThemeColors
import com.xianming.watch4sat.wear.theme.WatchSemanticColors
import com.xianming.watch4sat.wear.theme.WatchThemeCatalog
import com.xianming.watch4sat.wear.theme.WatchThemeResolver
import com.xianming.watch4sat.wear.theme.WatchTypography
import com.xianming.watch4sat.wear.theme.googleSansFlexConfirmationCurvedTextStyle
import com.xianming.watch4sat.wear.theme.googleSansFlexTimeTextStyle
import com.xianming.watch4sat.domain.time.ClockTimeFormatter
import com.xianming.watch4sat.time.rememberAndroidClockTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Composable
fun Watch4SatApp(
    viewModel: Watch4SatViewModel = viewModel(factory = Watch4SatViewModel.Factory),
    externalLaunch: ExternalLaunchEnvelope? = null,
    onExternalLaunchConsumed: (Long) -> Boolean = { true },
    onExitSetup: () -> Unit = {}
) {
    val notificationIntent = externalLaunch?.intent
    val state by viewModel.uiState.collectAsState()
    val navController = rememberSwipeDismissableNavController()
    val swipeToDismissBoxState = rememberSwipeToDismissBoxState()
    val swipeDismissableNavHostState = rememberSwipeDismissableNavHostState(
        swipeToDismissBoxState = swipeToDismissBoxState
    )
    val currentBackStackEntry by navController.currentBackStackEntryFlow.collectAsState(initial = null)
    val currentRoute = currentBackStackEntry?.destination?.route
    val currentOrbitMapDetailCatalogNumber = currentBackStackEntry
        ?.takeIf { currentRoute == WatchRoute.OrbitMapDetail.route }
        ?.arguments
        ?.getInt(OrbitMapRoutes.CatalogNumberArg)
    val context = LocalContext.current
    val clockTimeFormatter = rememberAndroidClockTimeFormatter()
    val ambientMode = LocalAmbientModeManager.current?.currentAmbientMode
    val isAmbient = ambientMode is AmbientMode.Ambient
    val radarUpdateMode = RadarPowerPolicy.updateMode(isAmbient)
    val lifecycleOwner = LocalLifecycleOwner.current
    var appInForeground by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(
        lifecycleOwner,
        currentRoute,
        currentOrbitMapDetailCatalogNumber,
        radarUpdateMode,
        isAmbient
    ) {
        fun syncRouteUpdates() {
            val lifecycleStarted = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            val decision = RouteUpdateLifecyclePolicy.decide(
                route = currentRoute,
                lifecycleStarted = lifecycleStarted,
                radarUpdateMode = radarUpdateMode,
                isAmbient = isAmbient
            )
            if (decision.runMinuteUpdates) {
                viewModel.startMinuteUpdates()
            } else {
                viewModel.stopMinuteUpdates()
            }
            if (decision.runOrbitMapUpdates) {
                viewModel.startOrbitMapUpdates()
            } else {
                viewModel.stopOrbitMapUpdates()
            }
            if (
                decision.runOrbitMapDetailUpdates &&
                currentOrbitMapDetailCatalogNumber != null
            ) {
                viewModel.startOrbitMapDetailUpdates(
                    currentOrbitMapDetailCatalogNumber
                )
            } else {
                viewModel.stopOrbitMapDetailUpdates()
            }
            decision.radarUpdateMode?.let(viewModel::startRadarUpdates)
                ?: viewModel.stopRadarUpdates()
        }
        val observer = LifecycleEventObserver { _, _ ->
            val isForeground = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            appInForeground = isForeground
            if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                viewModel.cancelGpsRequest(message = null, reason = "lifecycle-not-started")
            }
            syncRouteUpdates()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        syncRouteUpdates()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopMinuteUpdates()
            viewModel.stopOrbitMapUpdates()
            viewModel.stopOrbitMapDetailUpdates()
            viewModel.stopRadarUpdates()
        }
    }
    var focusedSatelliteDetailCatalogNumber by remember { mutableStateOf<Int?>(null) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) viewModel.requestGps() else viewModel.reportLocationPermissionDenied()
    }
    var notificationPermissionGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationPermissionGranted = granted
    }
    LaunchedEffect(appInForeground) {
        if (appInForeground) {
            notificationPermissionGranted =
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestGps() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.requestGps()
        } else {
            viewModel.reportLocationPermissionDenied()
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val dynamicWearColorScheme = if (state.settings.themePreset == AppThemePreset.SYSTEM) {
        dynamicColorScheme(context)
    } else {
        null
    }
    val resolvedTheme = WatchThemeResolver.resolve(
        preset = state.settings.themePreset,
        dynamicColorScheme = dynamicWearColorScheme
    )
    val themeColors = resolvedTheme.watchColors
    val setupDecision = FirstRunSetupPolicy.decision(
        setupCompleted = state.settings.setupCompleted,
        storedStep = state.settings.setupStep,
        skippedSteps = state.settings.setupSkippedSteps,
        hasStationLocation = state.hasStationLocation,
        hasTleData = FirstRunSetupPolicy.hasUsableTleData(state.satellites.size),
        selectedSatelliteCount = state.selectedSatelliteCount,
        notificationsAvailable = notificationPermissionGranted,
        exactAlarmAvailable = context.canScheduleWatch4SatExactAlarms()
    )
    val appStartDestination = if (setupDecision.shouldShowSetup) {
        WatchRoute.FirstRunSetup.route
    } else {
        WatchRoute.Dashboard.route
    }
    ReportDrawnWhen {
        StartupDrawnPolicy.isFullyDrawn(
            settingsLoaded = state.settingsLoaded,
            setupActive = setupDecision.shouldShowSetup,
            currentRoute = currentRoute,
            passPlanningStatus = state.passPlanningStatus
        )
    }
    var showTleSuccessDialog by remember { mutableStateOf(false) }
    var lastTleSuccessEventId by remember { mutableStateOf(0L) }
    var showTleFailureDialog by remember { mutableStateOf(false) }
    var lastTleFailureMessage by remember { mutableStateOf<String?>(null) }
    var lastTleFailureEventId by remember { mutableStateOf(0L) }
    var showGpsFailureDialog by remember { mutableStateOf(false) }
    var lastGpsFailureEventId by remember { mutableStateOf(0L) }
    var showDeveloperOptionsDialog by remember { mutableStateOf(false) }
    var showDeveloperNotificationDialog by remember { mutableStateOf(false) }
    var showDeveloperNotificationPermissionDialog by remember { mutableStateOf(false) }
    var showDeveloperNoPassDialog by remember { mutableStateOf(false) }
    val developerOptionsUnlockedText = stringResource(R.string.developer_options_unlocked)
    val developerNotificationText =
        DeveloperOptionAction.TriggerPassNotification.resolveLabel()
    val developerNotificationPermissionText =
        DeveloperOptionFailure.NotificationPermission.resolveTitle()
    val developerNoPassText = DeveloperOptionFailure.NoPass.resolveTitle()
    var showPassUnavailableDialog by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.refreshSuccessEventId, state.refreshMessage) {
        if (DataRefreshDialogPolicy.shouldShowSuccessEvent(
                message = state.refreshMessage,
                currentEventId = state.refreshSuccessEventId,
                lastShownEventId = lastTleSuccessEventId,
                setupActive = setupDecision.shouldShowSetup
            )
        ) {
            lastTleSuccessEventId = state.refreshSuccessEventId
            showTleSuccessDialog = true
        }
    }
    LaunchedEffect(state.refreshFailureEventId, state.refreshMessage) {
        if (DataRefreshDialogPolicy.shouldShowFailureEvent(
                message = state.refreshMessage,
                currentEventId = state.refreshFailureEventId,
                lastShownEventId = lastTleFailureEventId
            )
        ) {
            lastTleFailureEventId = state.refreshFailureEventId
            lastTleFailureMessage = state.refreshMessage
            showTleFailureDialog = true
        }
    }
    LaunchedEffect(state.gpsFailureEventId) {
        if (QthGpsFailureDialogPolicy.shouldShowEvent(
                currentEventId = state.gpsFailureEventId,
                lastShownEventId = lastGpsFailureEventId
            )
        ) {
            lastGpsFailureEventId = state.gpsFailureEventId
            showGpsFailureDialog = true
        }
    }
    val alertPass = PassStartReminderPolicy.selectAlertPass(
        passes = state.passCards.map { it.first },
        nowMillis = state.nowMillis,
        passAlertAdvanceMinutes = state.settings.passAlertAdvanceMinutes
    )
    val alertPassPair = alertPass?.let { pass ->
        state.passCards.firstOrNull { it.first.notificationKey() == pass.notificationKey() }
    }
    val alertPassKey = alertPassPair?.first?.notificationKey()
    val radarFocusedPassKey = state.focusedPass?.notificationKey()
    val passStartNotifier = remember(context) { PassStartNotifier(context.applicationContext) }
    val passStartAlarmStateStore = remember(context) {
        PassStartAlarmStateStore(context.applicationContext)
    }
    val passStartAlarmScheduler = remember(context) {
        PassStartAlarmScheduler(
            context.applicationContext,
            passStartAlarmStateStore,
            stationDataScheduleGuard(context.applicationContext)
        )
    }
    val passStartAlarmPasses = state.passCards.map { it.first }
    val passStartAlarmUiScheduleKey = PassStartAlarmUiScheduleKey.from(
        passes = passStartAlarmPasses,
        passAlertAdvanceMinutes = state.settings.passAlertAdvanceMinutes,
        setupActive = setupDecision.shouldShowSetup,
        shouldUpdate = PassStartSchedulePolicy.shouldUpdateAlarmFromUiState(state.passPlanningStatus.name)
    )
    var passAlertScheduledTriggerAtMillis by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(currentRoute, state.settings.passAlertAdvanceMinutes, state.passCards) {
        if (currentRoute != WatchRoute.SettingsPassAlerts.route) return@LaunchedEffect
        passAlertScheduledTriggerAtMillis = passStartAlarmStateStore.read().scheduledTriggerAtMillis
    }
    val passAlertStatusRows = PassAlertStatusPolicy.rows(
        runtimePermissionGranted = notificationPermissionGranted,
        appNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled(),
        channelEnabled = context.isWatch4SatNotificationChannelEnabled(PassStartNotificationPolicy.channelId),
        exactAlarmAvailable = context.canScheduleWatch4SatExactAlarms(),
        scheduledTriggerAtMillis = passAlertScheduledTriggerAtMillis,
        nowMillis = state.nowMillis
    )
    val radarOngoingActivityController = remember(context) {
        RadarOngoingActivityController(context.applicationContext)
    }
    var lastHandledActiveNotificationKey by remember { mutableStateOf<String?>(null) }
    var pendingPassStartAlert by remember { mutableStateOf<PassStartAlertState?>(null) }
    val latestPassCards by rememberUpdatedState(state.passCards)
    val latestFirstRunState by rememberUpdatedState(state)
    val latestSettingsLoaded by rememberUpdatedState(state.settingsLoaded)
    val latestSetupDecision by rememberUpdatedState(setupDecision)
    val latestNotificationPermissionGranted by rememberUpdatedState(notificationPermissionGranted)
    val latestExactAlarmAvailable by rememberUpdatedState(context.canScheduleWatch4SatExactAlarms())
    var pageTimeTextVisible by remember { mutableStateOf(true) }
    var radarOverlayOpen by rememberSaveable { mutableStateOf(false) }
    var radarCalibrationHintRequestId by rememberSaveable { mutableStateOf(0) }
    LaunchedEffect(appInForeground) {
        PassStartAppVisibility.setForeground(appInForeground)
    }
    DisposableEffect(Unit) {
        onDispose { PassStartAppVisibility.setForeground(false) }
    }
    LaunchedEffect(
        appInForeground,
        currentRoute,
        setupDecision.shouldShowSetup
    ) {
        if (!appInForeground) return@LaunchedEffect
        val setupAllowedRoutes = setOf(
            WatchRoute.FirstRunSetup.route
        )
        if (setupDecision.shouldShowSetup && currentRoute !in setupAllowedRoutes) {
            navController.navigate(WatchRoute.FirstRunSetup.route) {
                launchSingleTop = true
            }
        } else if (!setupDecision.shouldShowSetup && currentRoute == WatchRoute.FirstRunSetup.route) {
            navController.navigate(WatchRoute.Dashboard.route) {
                launchSingleTop = true
                popUpTo(WatchRoute.FirstRunSetup.route) { inclusive = true }
            }
        }
    }
    LaunchedEffect(
        appInForeground,
        currentRoute,
        state.settingsLoaded,
        state.satelliteCatalogLoaded,
        state.settings.autoDataFreshnessEnabled,
        state.settings.lastSatelliteDataUpdateMillis,
        state.settings.lastTransmitterDataUpdateMillis,
        state.tleFreshness.severity,
        state.tleFreshness.oldestEpochMillis,
        state.tleFreshness.clockSkewDetected,
        state.refreshFailureEventId
    ) {
        if (
            appInForeground &&
            state.settingsLoaded &&
            state.satelliteCatalogLoaded &&
            currentRoute in setOf(WatchRoute.Dashboard.route, WatchRoute.Data.route)
        ) {
            viewModel.refreshStaleDataIfNeeded("foreground")
        }
    }
    LaunchedEffect(passStartAlarmUiScheduleKey) {
        if (!passStartAlarmUiScheduleKey.setupActive && !passStartAlarmUiScheduleKey.shouldUpdate) {
            return@LaunchedEffect
        }
        val alarmState = passStartAlarmStateStore.read()
        if (passStartAlarmUiScheduleKey.setupActive) {
            passStartAlarmScheduler.schedule(null, alarmState)
            passAlertScheduledTriggerAtMillis = null
            return@LaunchedEffect
        }
        val candidate = PassStartSchedulePolicy.nextScheduleCandidate(
            passes = passStartAlarmPasses,
            nowMillis = state.nowMillis,
            handledPassKeys = alarmState.handledPassKeys,
            allowCatchUp = false,
            passAlertAdvanceMinutes = passStartAlarmUiScheduleKey.passAlertAdvanceMinutes
        )
        passStartAlarmScheduler.schedule(candidate, alarmState)
        passAlertScheduledTriggerAtMillis = candidate?.triggerAtMillis
    }
    LaunchedEffect(
        alertPassKey,
        currentRoute,
        radarFocusedPassKey,
        notificationPermissionGranted,
        appInForeground,
        setupDecision.shouldShowSetup
    ) {
        val pair = alertPassPair ?: return@LaunchedEffect
        val key = alertPassKey ?: return@LaunchedEffect
        if (lastHandledActiveNotificationKey == key) return@LaunchedEffect
        if (key in passStartAlarmStateStore.read().handledPassKeys) {
            lastHandledActiveNotificationKey = key
            return@LaunchedEffect
        }
        if (System.currentTimeMillis() >= pair.first.losMillis) {
            lastHandledActiveNotificationKey = key
            passStartAlarmStateStore.markHandled(key)
            return@LaunchedEffect
        }
        val decision = PassStartReminderPolicy.decision(
            currentRoute = currentRoute,
            activePassKey = key,
            radarFocusedPassKey = radarFocusedPassKey,
            isAppForeground = appInForeground,
            setupActive = setupDecision.shouldShowSetup
        )
        if (decision == PassStartReminderDecision.PostSystemNotification && !notificationPermissionGranted) {
            return@LaunchedEffect
        }
        val didPost = when (decision) {
            PassStartReminderDecision.ShowInAppAlert -> {
                pendingPassStartAlert = PassStartAlertState(
                    alertPass = pair.first,
                    card = pair.second,
                    radarPass = pair.first
                )
                false
            }
            PassStartReminderDecision.PostSystemNotification -> passStartNotifier.notify(pair.first, pair.second)
            PassStartReminderDecision.WaitForKnownRoute,
            PassStartReminderDecision.SuppressAndMarkHandled -> false
        }
        if (PassStartReminderPolicy.shouldMarkHandled(decision, didPost)) {
            lastHandledActiveNotificationKey = key
            passStartAlarmStateStore.markHandled(key)
        }
    }
    suspend fun drainPendingForegroundAlarm() {
        if (!latestSettingsLoaded) return
        val pending = passStartAlarmStateStore.read().pendingForegroundAlarm ?: return
        if (pending.passKey in passStartAlarmStateStore.read().handledPassKeys) {
            passStartAlarmStateStore.clearPendingForegroundAlarm(pending.passKey)
            return
        }
        if (latestSetupDecision.shouldShowSetup) {
            lastHandledActiveNotificationKey = pending.passKey
            passStartAlarmStateStore.markHandled(pending.passKey)
            passStartAlarmStateStore.clearPendingForegroundAlarm(pending.passKey)
            return
        }
        val matchingPair = latestPassCards.firstOrNull {
            it.first.matchesPendingForegroundAlarm(pending)
        } ?: return
        if (System.currentTimeMillis() >= matchingPair.first.losMillis) {
            lastHandledActiveNotificationKey = pending.passKey
            passStartAlarmStateStore.markHandled(pending.passKey)
            passStartAlarmStateStore.clearPendingForegroundAlarm(pending.passKey)
            return
        }
        pendingPassStartAlert = PassStartAlertState(
            alertPass = matchingPair.first,
            card = matchingPair.second,
            radarPass = matchingPair.first
        )
        lastHandledActiveNotificationKey = pending.passKey
        passStartAlarmStateStore.markHandled(pending.passKey)
        passStartAlarmStateStore.clearPendingForegroundAlarm(pending.passKey)
    }
    LaunchedEffect(appInForeground, state.settingsLoaded, setupDecision.shouldShowSetup, state.passCards) {
        if (appInForeground) drainPendingForegroundAlarm()
    }
    LaunchedEffect(Unit) {
        PassStartForegroundAlarmEvents.events.collectLatest {
            drainPendingForegroundAlarm()
        }
    }
    val notificationRequest = passStartNotificationRequestFrom(notificationIntent)
    val tileLaunchRequest = TileLaunchIntentPolicy.requestFrom(notificationIntent)
    val externalLaunchTarget = ExternalLaunchCoordinator.target(
        tileRequest = tileLaunchRequest,
        passRequest = notificationRequest
    )
    var handledExternalLaunchEventId by rememberSaveable { mutableStateOf<Long?>(null) }
    LaunchedEffect(
        externalLaunch?.eventId,
        state.settingsLoaded,
        setupDecision.shouldShowSetup,
        state.passPlanningStatus,
        state.passCards,
        state.nowMillis
    ) {
        val launch = externalLaunch ?: return@LaunchedEffect
        if (handledExternalLaunchEventId == launch.eventId) return@LaunchedEffect
        val decision = ExternalLaunchCoordinator.decideAtCurrentTime(
            target = externalLaunchTarget,
            settingsLoaded = state.settingsLoaded,
            setupIncomplete = setupDecision.shouldShowSetup,
            passPlanningStatus = state.passPlanningStatus,
            passes = state.passCards.map { (pass, _) -> pass }
        )
        if (decision == ExternalLaunchDecision.Wait) return@LaunchedEffect
        if (!onExternalLaunchConsumed(launch.eventId)) return@LaunchedEffect
        handledExternalLaunchEventId = launch.eventId

        when (decision) {
            ExternalLaunchDecision.Wait,
            ExternalLaunchDecision.Consume -> Unit
            is ExternalLaunchDecision.Navigate -> {
                navController.navigate(decision.route.route) {
                    launchSingleTop = true
                }
            }
            is ExternalLaunchDecision.OpenPass -> {
                viewModel.selectPass(decision.pass)
                navController.navigate(WatchRoute.Radar.route) {
                    launchSingleTop = true
                }
            }
            ExternalLaunchDecision.PassUnavailable -> {
                navController.navigate(WatchRoute.Passes.route) {
                    launchSingleTop = true
                }
                showPassUnavailableDialog = true
                TileService.getUpdater(context)
                    .requestUpdate(NextPassTileService::class.java)
            }
        }
    }
    val radarOngoingStatus = state.focusedPass?.let { pass ->
        RadarUiText.ambientMinuteBucket(pass, state.radarNowMillis)
    }
    LaunchedEffect(
        currentRoute,
        radarFocusedPassKey,
        state.selectedPassKey,
        state.focusedPass?.losMillis,
        radarOngoingStatus,
        notificationPermissionGranted
    ) {
        val decision = RadarOngoingActivityPolicy.decision(
            currentRoute = currentRoute,
            focusedPassKey = radarFocusedPassKey,
            selectedPassKey = state.selectedPassKey,
            nowMillis = state.radarNowMillis,
            losMillis = state.focusedPass?.losMillis
        )
        when (decision) {
            RadarOngoingActivityDecision.StartOrUpdate -> {
                val pass = state.focusedPass ?: return@LaunchedEffect
                if (notificationPermissionGranted) {
                    radarOngoingActivityController.startOrUpdate(pass, state.radarNowMillis)
                }
            }
            RadarOngoingActivityDecision.Cancel -> radarOngoingActivityController.cancel()
        }
    }
    val modalChromeHidden = pendingPassStartAlert != null ||
        showTleSuccessDialog ||
        showTleFailureDialog ||
        showGpsFailureDialog ||
        showDeveloperOptionsDialog ||
        showDeveloperNotificationDialog ||
        showDeveloperNotificationPermissionDialog ||
        showDeveloperNoPassDialog ||
        showPassUnavailableDialog
    val showTimeText = AppChromePolicy.shouldShowRootTimeText(
        currentRoute = currentRoute,
        pageReportedVisible = pageTimeTextVisible,
        modalChromeHidden = modalChromeHidden
    )
    val radarSwipeDismissEnabled = currentRoute != WatchRoute.Radar.route || !radarOverlayOpen
    val setupSwipeDismissEnabled = !setupDecision.shouldShowSetup
    MaterialTheme(
        colorScheme = resolvedTheme.wearColorScheme,
        typography = WatchTypography
    ) {
        CompositionLocalProvider(LocalWatchThemeColors provides themeColors) {
            AppScaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                timeText = {
                    if (showTimeText) {
                        WatchRootTimeText()
                    }
                },
                containerColor = Color.Black,
                contentColor = Color.White
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    AppTimeTextVisibilityProvider(
                        onVisibilityChanged = { pageTimeTextVisible = it }
                    ) {
                        SwipeDismissableNavHost(
                            navController = navController,
                            startDestination = appStartDestination,
                            state = swipeDismissableNavHostState,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black),
                            userSwipeEnabled = radarSwipeDismissEnabled && setupSwipeDismissEnabled
                        ) {
                        composable(WatchRoute.FirstRunSetup.route) {
                            FirstRunSetupScreen(
                                state = latestFirstRunState,
                                setupDecision = latestSetupDecision,
                                notificationPermissionGranted = latestNotificationPermissionGranted,
                                exactAlarmAvailable = latestExactAlarmAvailable,
                                onMoveToStep = { step ->
                                    viewModel.setSetupStep(step.storedName)
                                },
                                onRefreshTle = {
                                    viewModel.refreshSetupData()
                                },
                                onRequestGps = {
                                    requestGps()
                                },
                                onCancelGps = { viewModel.cancelGpsRequest() },
                                onToggleSatellite = { catalogNumber ->
                                    viewModel.toggleSatellite(catalogNumber)
                                },
                                onApplyStarterSelection = {
                                    viewModel.applyStarterSelection(latestFirstRunState.satellites)
                                },
                                onRequestNotifications = {
                                    viewModel.setSetupStep(FirstRunSetupStep.Notifications.storedName)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        notificationPermissionGranted = true
                                    }
                                },
                                onOpenExactAlarmSettings = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        runCatching {
                                            context.startActivity(
                                                Intent(
                                                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                                    Uri.parse("package:${context.packageName}")
                                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            )
                                        }
                                    }
                                },
                                onSkipStep = { skippedStep, nextStep ->
                                    viewModel.skipSetupStepAndMoveTo(skippedStep.storedName, nextStep.storedName)
                                },
                                onFinish = {
                                    viewModel.completeSetup()
                                },
                                onExit = onExitSetup
                            )
                        }
                        composable(WatchRoute.Dashboard.route) {
                            DashboardScreen(
                                state = state,
                                onNavigate = { route ->
                                    navController.navigate(route.route) {
                                        launchSingleTop = true
                                    }
                                },
                                onOpenRadar = { pass ->
                                    viewModel.selectPass(pass)
                                    navController.navigate(WatchRoute.Radar.route) {
                                        launchSingleTop = true
                                    }
                                },
                                onRefresh = viewModel::refreshAll
                            )
                        }
                        composable(WatchRoute.Passes.route) {
                            PassesScreen(
                                state = state,
                                onOpenRadar = { pass ->
                                    viewModel.selectPass(pass)
                                    navController.navigate(WatchRoute.Radar.route) {
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                        composable(WatchRoute.OrbitMap.route) {
                            OrbitMapScreen(
                                state = state,
                                swipeToDismissBoxState = swipeToDismissBoxState,
                                onPrevious = viewModel::selectPreviousOrbitSatellite,
                                onNext = viewModel::selectNextOrbitSatellite,
                                onOpenDetail = { catalogNumber ->
                                    navController.navigate(
                                        OrbitMapRoutes.detail(catalogNumber)
                                    ) {
                                        launchSingleTop = true
                                    }
                                },
                                onOpenData = {
                                    navController.navigate(WatchRoute.Data.route) {
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                        composable(
                            route = WatchRoute.OrbitMapDetail.route,
                            arguments = listOf(
                                navArgument(OrbitMapRoutes.CatalogNumberArg) {
                                    type = NavType.IntType
                                }
                            )
                        ) { backStackEntry ->
                            val liveDetailState by viewModel.orbitMapDetailState
                                .collectAsStateWithLifecycle()
                            val catalogNumber = backStackEntry.arguments?.getInt(
                                OrbitMapRoutes.CatalogNumberArg
                            )
                            val satellite = catalogNumber?.let { requestedCatalog ->
                                state.satellites.firstOrNull {
                                    it.catalogNumber == requestedCatalog
                                }
                            }
                            val snapshotMatchesRoute =
                                catalogNumber != null &&
                                    liveDetailState.catalogNumber == catalogNumber
                            val detail = satellite?.let {
                                OrbitMapDetailMapper.map(
                                    satellite = it,
                                    transmitters = state.transmitters,
                                    currentPosition = liveDetailState.currentPosition
                                        .takeIf { snapshotMatchesRoute },
                                    footprintRadiusKm = liveDetailState.footprintRadiusKm
                                        .takeIf { snapshotMatchesRoute }
                                        ?: 0.0,
                                    slantRangeKm = liveDetailState.slantRangeKm
                                        .takeIf { snapshotMatchesRoute },
                                    lastUpdatedMillis = liveDetailState.lastUpdatedMillis
                                        .takeIf { snapshotMatchesRoute }
                                        ?: 0L,
                                    clockTimeFormatter = clockTimeFormatter
                                )
                            }
                            OrbitMapDetailScreen(detail = detail)
                        }
                        composable(WatchRoute.Qth.route) {
                            QthGpsKeepScreenOn(
                                enabled = QthGpsPowerPolicy.shouldKeepScreenOn(
                                    gpsRequestInFlight = state.gpsRequestInFlight,
                                    qthSurfaceVisible = true
                                )
                            )
                            DisposableEffect(WatchRoute.Qth.route) {
                                onDispose { viewModel.cancelGpsRequest(message = null) }
                            }
                            QthScreen(
                                state = state,
                                onGps = ::requestGps,
                                onCancelGps = { viewModel.cancelGpsRequest() },
                                onEditLocator = {
                                    navController.navigate(WatchRoute.QthPicker.route) {
                                        launchSingleTop = true
                                    }
                                },
                                onSaveMapCenter = viewModel::saveMapCenter,
                                onClearStationLocation = viewModel::clearStationLocation
                            )
                        }
                        composable(WatchRoute.QthPicker.route) {
                            QthPickerScreen(
                                state = state,
                                onApply = { qth ->
                                    viewModel.saveQth(qth)
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable(WatchRoute.Data.route) {
                            DataScreen(state = state, onRefresh = viewModel::refreshAll)
                        }
                        composable(WatchRoute.Satellites.route) {
                            SatellitesScreen(
                                state = state,
                                onToggle = viewModel::toggleSatellite,
                                onOpenDetail = { satellite ->
                                    focusedSatelliteDetailCatalogNumber = satellite.catalogNumber
                                    navController.navigate(WatchRoute.SatelliteDetail.route) {
                                        launchSingleTop = true
                                    }
                                },
                                onStarter = { viewModel.applyStarterSelection(state.satellites) },
                                onOpenClearConfirm = {
                                    navController.navigate(WatchRoute.SatellitesClearConfirm.route) {
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                        composable(WatchRoute.SatellitesClearConfirm.route) {
                            SatellitesClearConfirmScreen(
                                selectedCount = state.selectedSatelliteCount,
                                onClear = {
                                    viewModel.clearSelectedSatellites()
                                    navController.popBackStack(WatchRoute.Satellites.route, inclusive = false)
                                },
                                onDismiss = { navController.popBackStack() }
                            )
                        }
                        composable(WatchRoute.SatelliteDetail.route) {
                            SatelliteDetailScreen(
                                state = state,
                                catalogNumber = focusedSatelliteDetailCatalogNumber,
                                onToggle = viewModel::toggleSatellite
                            )
                        }
                        composable(WatchRoute.Settings.route) {
                            SettingsScreen(
                                state = state,
                                onRadarKeepScreenOnChange = viewModel::setRadarKeepScreenOn,
                                onRadarForwardAxisChange = viewModel::setRadarForwardAxis,
                                onRadarFallbackWristSideChange =
                                    viewModel::setRadarFallbackWristSide,
                                onNavigate = { route ->
                                    navController.navigate(route.route) {
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                        composable(WatchRoute.SettingsAppearance.route) {
                            AppearanceScreen(
                                state = state,
                                onThemeChange = viewModel::setThemePreset
                            )
                        }
                        composable(WatchRoute.SettingsPassWindowAdjuster.route) {
                            PassWindowAdjusterScreen(
                                state = state,
                                onApply = { hours ->
                                    viewModel.setPassWindow(hours)
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable(WatchRoute.SettingsPassAlerts.route) {
                            PassAlertsScreen(
                                state = state,
                                statusRows = passAlertStatusRows,
                                onSelect = { minutes ->
                                    viewModel.setPassAlertAdvanceMinutes(minutes)
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable(WatchRoute.SettingsDataFreshness.route) {
                            DataFreshnessScreen(
                                state = state,
                                onCheckedChange = viewModel::setAutoDataFreshnessEnabled
                            )
                        }
                        composable(WatchRoute.SettingsMinimumElevation.route) {
                            MinimumElevationScreen(
                                state = state,
                                onEnabledChange = viewModel::setMinimumElevationFilterEnabled,
                                onThresholdSelected = viewModel::setMinimumElevationDegrees
                            )
                        }
                        composable(WatchRoute.SettingsMapSource.route) {
                            MapSourceScreen(
                                state = state,
                                onMapTileModeSelected = { mode ->
                                    viewModel.setMapTileMode(mode)
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable(WatchRoute.SettingsAbout.route) {
                            AboutScreen(
                                state = state,
                                onOpenPrivacy = {
                                    navController.navigate(WatchRoute.SettingsPrivacy.route) {
                                        launchSingleTop = true
                                    }
                                },
                                onOpenLegal = {
                                    navController.navigate(WatchRoute.SettingsLegal.route) {
                                        launchSingleTop = true
                                    }
                                },
                                onDeveloperOptionsUnlocked = {
                                    viewModel.setDeveloperOptionsEnabled(true)
                                    showDeveloperOptionsDialog = true
                                }
                            )
                        }
                        composable(WatchRoute.SettingsPrivacy.route) {
                            LegalDocumentScreen(document = LegalDocument.PrivacyPolicy)
                        }
                        composable(WatchRoute.SettingsLegal.route) {
                            LegalNoticesScreen(
                                onOpenDocument = { document ->
                                    navController.navigate(LegalRoutes.document(document)) {
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                        composable(
                            route = WatchRoute.SettingsLegalDocument.route,
                            arguments = listOf(
                                navArgument(LegalRoutes.DocumentIdArg) {
                                    type = NavType.StringType
                                }
                            )
                        ) { backStackEntry ->
                            LegalDocumentScreen(
                                document = LegalDocument.fromId(
                                    backStackEntry.arguments?.getString(LegalRoutes.DocumentIdArg)
                                )
                            )
                        }
                        composable(WatchRoute.SettingsDeveloperOptions.route) {
                            DeveloperOptionsScreen(
                                onTriggerPassAlert = {
                                    val selection = DeveloperOptionsPolicy.selectDebugPassAlert(
                                        passCards = state.passCards,
                                        nowMillis = state.nowMillis
                                    )
                                    if (selection == null) {
                                        showDeveloperNoPassDialog = true
                                    } else {
                                        pendingPassStartAlert = selection
                                    }
                                },
                                onTriggerPassNotification = {
                                    val selection = DeveloperOptionsPolicy.selectDebugPassAlert(
                                        passCards = state.passCards,
                                        nowMillis = state.nowMillis
                                    )
                                    if (selection == null) {
                                        showDeveloperNoPassDialog = true
                                    } else if (passStartNotifier.notify(selection.radarPass, selection.card)) {
                                        notificationPermissionGranted = true
                                        showDeveloperNotificationDialog = true
                                    } else {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                            ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.POST_NOTIFICATIONS
                                            ) != PackageManager.PERMISSION_GRANTED
                                        ) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                        showDeveloperNotificationPermissionDialog = true
                                    }
                                },
                                onTriggerCalibrationHint = {
                                    radarCalibrationHintRequestId += 1
                                    navController.navigate(WatchRoute.Radar.route) {
                                        launchSingleTop = true
                                    }
                                },
                                onDisableDeveloperOptions = {
                                    viewModel.setDeveloperOptionsEnabled(false)
                                    navController.popBackStack(WatchRoute.Settings.route, inclusive = false)
                                }
                            )
                        }
                        composable(WatchRoute.Radar.route) {
                            DisposableEffect(Unit) {
                                radarOverlayOpen = false
                                onDispose {
                                    radarOverlayOpen = false
                                }
                            }
                            RadarScreen(
                                state = state,
                                overlayOpen = radarOverlayOpen,
                                onOverlayOpenChange = { radarOverlayOpen = it },
                                onSelectRadarTransmitter = viewModel::selectRadarTransmitter,
                                onSelectRadarPass = viewModel::selectPass,
                                calibrationHintRequestId = radarCalibrationHintRequestId,
                                onCalibrationHintConsumed = {
                                    radarCalibrationHintRequestId = 0
                                }
                            )
                        }
                    }
                    }
                    PassStartAlertDialog(
                        passAlert = pendingPassStartAlert,
                        onDismiss = { pendingPassStartAlert = null },
                        onTrack = { pass ->
                            pendingPassStartAlert = null
                            viewModel.selectPass(pass)
                            navController.navigate(WatchRoute.Radar.route) {
                                launchSingleTop = true
                            }
                        }
                    )
                    val confirmationCurvedTextStyle = googleSansFlexConfirmationCurvedTextStyle()
                    val passUnavailableText = stringResource(
                        R.string.pass_no_longer_available
                    )
                    val qthGpsFailureTitle = stringResource(
                        R.string.qth_gps_failure_title
                    )
                    val tleSuccessTitle = stringResource(
                        R.string.refresh_success_title
                    )
                    val tleFailureKind = DataRefreshDialogPolicy.failureKind(
                        lastTleFailureMessage.orEmpty()
                    )
                    val tleFailureTitle = stringResource(
                        when (tleFailureKind) {
                            DataRefreshFailureKind.Timeout ->
                                R.string.refresh_failure_timeout
                            DataRefreshFailureKind.NoNetwork ->
                                R.string.refresh_failure_no_network
                            DataRefreshFailureKind.HttpApi ->
                                R.string.refresh_failure_api
                            DataRefreshFailureKind.Parse ->
                                R.string.refresh_failure_parse
                            DataRefreshFailureKind.Unknown ->
                                R.string.refresh_failure_unknown
                        }
                    )
                    SuccessConfirmationDialog(
                        visible = showTleSuccessDialog,
                        onDismissRequest = { showTleSuccessDialog = false },
                        curvedText = {
                            confirmationDialogCurvedText(
                                tleSuccessTitle,
                                confirmationCurvedTextStyle
                            )
                        },
                        durationMillis = DataRefreshDialogPolicy.successAutoDismissMillis,
                        colors = ConfirmationDialogDefaults.successColors(
                            iconColor = themeColors.primary,
                            iconContainerColor = themeColors.surfaceVariant,
                            textColor = Color.White
                        )
                    ) {
                        ConfirmationDialogDefaults.SuccessIcon()
                    }
                    FailureConfirmationDialog(
                        visible = showTleFailureDialog,
                        onDismissRequest = { showTleFailureDialog = false },
                        curvedText = {
                            confirmationDialogCurvedText(
                                tleFailureTitle,
                                confirmationCurvedTextStyle
                            )
                        },
                        durationMillis = DataRefreshDialogPolicy.failureAutoDismissMillis,
                        colors = ConfirmationDialogDefaults.failureColors(
                            iconColor = WatchSemanticColors.ErrorForeground,
                            iconContainerColor = WatchSemanticColors.ErrorContainer,
                            textColor = Color.White
                        )
                        ) {
                            DataRefreshFailureIcon(
                                kind = tleFailureKind,
                            iconColor = WatchSemanticColors.ErrorForeground
                        )
                    }
                    FailureConfirmationDialog(
                        visible = showGpsFailureDialog,
                        onDismissRequest = { showGpsFailureDialog = false },
                        curvedText = {
                            confirmationDialogCurvedText(
                                qthGpsFailureTitle,
                                confirmationCurvedTextStyle
                            )
                        },
                        durationMillis = QthGpsFailureDialogPolicy.autoDismissMillis,
                        colors = ConfirmationDialogDefaults.failureColors(
                            iconColor = WatchSemanticColors.ErrorForeground,
                            iconContainerColor = WatchSemanticColors.ErrorContainer,
                            textColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = null,
                            tint = WatchSemanticColors.ErrorForeground
                        )
                    }
                    FailureConfirmationDialog(
                        visible = showPassUnavailableDialog,
                        onDismissRequest = { showPassUnavailableDialog = false },
                        curvedText = {
                            confirmationDialogCurvedText(
                                passUnavailableText,
                                confirmationCurvedTextStyle
                            )
                        },
                        durationMillis = DataRefreshDialogPolicy.failureAutoDismissMillis,
                        colors = ConfirmationDialogDefaults.failureColors(
                            iconColor = WatchSemanticColors.WarningForeground,
                            iconContainerColor = WatchSemanticColors.WarningContainer,
                            textColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = null,
                            tint = WatchSemanticColors.WarningForeground
                        )
                    }
                    SuccessConfirmationDialog(
                        visible = showDeveloperOptionsDialog,
                        onDismissRequest = { showDeveloperOptionsDialog = false },
                        curvedText = {
                            confirmationDialogCurvedText(
                                developerOptionsUnlockedText,
                                confirmationCurvedTextStyle
                            )
                        },
                        durationMillis = DataRefreshDialogPolicy.successAutoDismissMillis,
                        colors = ConfirmationDialogDefaults.successColors(
                            iconColor = themeColors.primary,
                            iconContainerColor = themeColors.surfaceVariant,
                            textColor = Color.White
                        )
                    ) {
                        ConfirmationDialogDefaults.SuccessIcon()
                    }
                    SuccessConfirmationDialog(
                        visible = showDeveloperNotificationDialog,
                        onDismissRequest = { showDeveloperNotificationDialog = false },
                        curvedText = {
                            confirmationDialogCurvedText(
                                developerNotificationText,
                                confirmationCurvedTextStyle
                            )
                        },
                        durationMillis = DataRefreshDialogPolicy.successAutoDismissMillis,
                        colors = ConfirmationDialogDefaults.successColors(
                            iconColor = themeColors.primary,
                            iconContainerColor = themeColors.surfaceVariant,
                            textColor = Color.White
                        )
                    ) {
                        ConfirmationDialogDefaults.SuccessIcon()
                    }
                    FailureConfirmationDialog(
                        visible = showDeveloperNotificationPermissionDialog,
                        onDismissRequest = { showDeveloperNotificationPermissionDialog = false },
                        curvedText = {
                            confirmationDialogCurvedText(
                                developerNotificationPermissionText,
                                confirmationCurvedTextStyle
                            )
                        },
                        durationMillis = DeveloperOptionsPolicy.debugFailureAutoDismissMillis,
                        colors = ConfirmationDialogDefaults.failureColors(
                            iconColor = WatchSemanticColors.WarningForeground,
                            iconContainerColor = WatchSemanticColors.WarningContainer,
                            textColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = WatchSemanticColors.WarningForeground
                        )
                    }
                    FailureConfirmationDialog(
                        visible = showDeveloperNoPassDialog,
                        onDismissRequest = { showDeveloperNoPassDialog = false },
                        curvedText = {
                            confirmationDialogCurvedText(
                                developerNoPassText,
                                confirmationCurvedTextStyle
                            )
                        },
                        durationMillis = DeveloperOptionsPolicy.debugFailureAutoDismissMillis,
                        colors = ConfirmationDialogDefaults.failureColors(
                            iconColor = WatchSemanticColors.ErrorForeground,
                            iconContainerColor = WatchSemanticColors.ErrorContainer,
                            textColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = null,
                            tint = WatchSemanticColors.ErrorForeground
                        )
                    }
                }
        }
    }
    }
}

@Composable
private fun WatchRootTimeText() {
    val style = googleSansFlexTimeTextStyle()
    TimeText {
        timeTextCurvedText(it, style = style)
    }
}

@Composable
private fun PassStartAlertDialog(
    passAlert: PassStartAlertState?,
    onDismiss: () -> Unit,
    onTrack: (SatellitePass) -> Unit
) {
    val alert = passAlert ?: return
    val pass = alert.alertPass
    val clockTimeFormatter = rememberAndroidClockTimeFormatter()
    val autoCloseDescription = stringResource(
        R.string.pass_alert_auto_close_description
    )
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val initialWallClockMillis = remember(alert.alertKey) { System.currentTimeMillis() }
    val alertDurationMillis = remember(alert.alertKey) {
        (pass.losMillis - initialWallClockMillis)
            .coerceIn(0L, PassStartReminderPolicy.inAppAutoDismissMillis)
    }
    var countdownSnapshot by remember(alert.alertKey) {
        mutableStateOf(
            PassStartReminderPolicy.countdownSnapshot(
                elapsedMillis = 0L,
                startWallClockMillis = initialWallClockMillis,
                durationMillis = alertDurationMillis
            )
        )
    }
    LaunchedEffect(alert.alertKey) {
        if (alertDurationMillis == 0L) {
            currentOnDismiss()
            return@LaunchedEffect
        }
        val startFrameNanos = withFrameNanos { it }
        while (true) {
            val elapsedMillis = (withFrameNanos { it } - startFrameNanos) / 1_000_000L
            countdownSnapshot = PassStartReminderPolicy.countdownSnapshot(
                elapsedMillis = elapsedMillis,
                startWallClockMillis = initialWallClockMillis,
                durationMillis = alertDurationMillis
            )
            if (countdownSnapshot.remainingMillis == 0L) {
                currentOnDismiss()
                break
            }
        }
    }
    val colors = LocalWatchThemeColors.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val ringSize = minOf(maxWidth, maxHeight)
            CircularProgressIndicator(
                progress = { countdownSnapshot.progress },
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(ringSize)
                    .padding(CircularProgressIndicatorDefaults.FullScreenPadding),
                strokeWidth = WatchUiMetrics.PassStartCountdownRingStrokeWidth,
                startAngle = WatchUiMetrics.PassStartCountdownRingStartAngle,
                endAngle = WatchUiMetrics.PassStartCountdownRingEndAngle,
                colors = ProgressIndicatorDefaults.colors(
                    indicatorColor = colors.primary,
                    trackColor = colors.surfaceVariant.copy(
                        alpha = PassStartReminderPolicy.countdownTrackAlpha
                    )
                )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 36.dp)
                    .semantics { contentDescription = autoCloseDescription },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    pass.satelliteName,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    maxLines = PassStartReminderPolicy.satelliteNameMaxLines,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    countdownSnapshot.secondsRemaining.toString(),
                    style = MaterialTheme.typography.numeralMedium,
                    color = colors.primary,
                    textAlign = TextAlign.Center
                )
                Text(
                    stringResource(R.string.pass_notification_title_started),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedText,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    pass.countdownLabel(
                        nowMillis = countdownSnapshot.nowMillis,
                        clockTimeFormatter = clockTimeFormatter
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedText,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            ThemedEdgeButton(
                label = stringResource(R.string.action_track),
                content = EdgeButtonContent.Track,
                modifier = Modifier.align(Alignment.BottomCenter),
                onClick = { onTrack(alert.radarPass) }
            )
        }
    }
}

@Composable
private fun DataRefreshFailureIcon(
    kind: DataRefreshFailureKind,
    iconColor: Color
) {
    val icon = when (kind) {
        DataRefreshFailureKind.Timeout -> Icons.Rounded.Timer
        DataRefreshFailureKind.NoNetwork -> Icons.Rounded.WifiOff
        DataRefreshFailureKind.HttpApi -> Icons.Rounded.CloudOff
        DataRefreshFailureKind.Parse -> Icons.Rounded.DataObject
        DataRefreshFailureKind.Unknown -> Icons.Rounded.Error
    }
    Icon(
        imageVector = icon,
        contentDescription = when (kind) {
            DataRefreshFailureKind.Timeout -> stringResource(R.string.refresh_failure_timeout)
            DataRefreshFailureKind.NoNetwork -> stringResource(R.string.refresh_failure_no_network)
            DataRefreshFailureKind.HttpApi -> stringResource(R.string.refresh_failure_api)
            DataRefreshFailureKind.Parse -> stringResource(R.string.refresh_failure_parse)
            DataRefreshFailureKind.Unknown -> stringResource(R.string.refresh_failure_unknown)
        },
        tint = iconColor,
        modifier = Modifier.size(34.dp)
    )
}

@Composable
private fun DashboardScreen(
    state: WatchUiState,
    onNavigate: (WatchRoute) -> Unit,
    onOpenRadar: (SatellitePass) -> Unit,
    onRefresh: () -> Unit
) {
    RoundListPage(
        title = stringResource(R.string.dashboard_title),
        titleKey = DashboardHeroProgressVisibilitySelector.dashboardTitleItemKey,
        edgeButton = {
            ThemedEdgeButton(
                stringResource(R.string.action_refresh),
                onClick = onRefresh
            )
        },
        overlay = { listState ->
            val progress = DashboardHeroProgressSelector.progressFor(state)
            val hasActiveProgress = progress != null
            val topSlackPx = with(LocalDensity.current) {
                WatchUiMetrics.DashboardTopVisibilitySlack.roundToPx()
            }
            val showProgress by remember(listState, hasActiveProgress, topSlackPx) {
                derivedStateOf {
                    val firstVisibleItem = listState.layoutInfo.visibleItems.firstOrNull()
                    DashboardHeroProgressVisibilitySelector.shouldShow(
                        hasActiveProgress = hasActiveProgress,
                        firstVisibleItemKey = firstVisibleItem?.key,
                        firstVisibleItemOffsetPx = firstVisibleItem?.offset ?: Int.MIN_VALUE,
                        topSlackPx = topSlackPx
                    )
                }
            }
            AnimatedVisibility(
                visible = progress != null && showProgress,
                modifier = Modifier.align(Alignment.CenterStart),
                enter = fadeIn(tween(DashboardProgressAnimationPolicy.enterMillis)),
                exit = fadeOut(tween(DashboardProgressAnimationPolicy.exitMillis))
            ) {
                DashboardActiveProgressIndicator(
                    progress = progress ?: 0f
                )
            }
        }
    ) {
        item(key = DashboardHeroProgressVisibilitySelector.heroItemKey) {
            NextPassHero(
                state = state,
                onOpenRadar = onOpenRadar,
                onOpenQth = { onNavigate(WatchRoute.Qth) },
                onRefresh = onRefresh,
                itemScope = this
            )
        }
        DashboardRouteItems.routes().forEach { routeItem ->
            item(key = routeItem.route.route) {
                RoundAction(
                    label = stringResource(routeItem.titleRes),
                    icon = dashboardIconFor(routeItem.iconKey),
                    modifier = Modifier.fillMaxWidth(),
                    itemScope = this
                ) {
                    onNavigate(routeItem.route)
                }
            }
        }
        item {
            StatusTextBlock(
                title = stringResource(R.string.dashboard_data_title),
                subtitle = DashboardDataStatusPolicy.model(
                    satelliteCount = state.satellites.size,
                    selectedCount = state.selectedSatelliteCount,
                    tleFreshness = state.tleFreshness,
                    isRefreshing = state.refreshInFlight
                ).resolveText(),
            )
        }
    }
}

@Composable
private fun DashboardActiveProgressIndicator(progress: Float, modifier: Modifier = Modifier) {
    val colors = LocalWatchThemeColors.current
    LevelIndicator(
        value = { progress },
        modifier = modifier
            .width(18.dp),
        strokeWidth = WatchUiMetrics.SideIndicatorStrokeWidth,
        sweepAngle = WatchUiMetrics.SideIndicatorSweepAngle,
        colors = LevelIndicatorDefaults.colors(
            indicatorColor = colors.primary,
            trackColor = colors.surfaceVariant.copy(alpha = 0.54f)
        )
    )
}

private fun dashboardIconFor(iconKey: String): ImageVector {
    return when (iconKey) {
        "list" -> Icons.AutoMirrored.Rounded.List
        "public" -> Icons.Rounded.Public
        "place" -> Icons.Rounded.Place
        "cloud_download" -> Icons.Rounded.CloudDownload
        "star" -> Icons.Rounded.Star
        "settings" -> Icons.Rounded.Settings
        else -> Icons.AutoMirrored.Rounded.List
    }
}

@Composable
private fun NextPassHero(
    state: WatchUiState,
    onOpenRadar: (SatellitePass) -> Unit,
    onOpenQth: () -> Unit,
    onRefresh: () -> Unit,
    itemScope: TransformingLazyColumnItemScope? = null
) {
    val active = state.passCards.firstOrNull { (_, card) -> card.isActive }
    val next = state.passCards.firstOrNull { (_, card) -> !card.isActive }
    val display = active ?: next
    val action = DashboardHeroActionSelector.select(state)
    val onClick = when (action) {
        is DashboardHeroAction.OpenRadar -> ({ onOpenRadar(action.pass) })
        DashboardHeroAction.OpenQth -> ({ onOpenQth() })
        DashboardHeroAction.RefreshData -> ({ onRefresh() })
        DashboardHeroAction.None -> null
    }
    val modifier = Modifier
        .fillMaxWidth()
        .roundListTransformedHeight(itemScope, RoundListSurface.STANDARD_CARD)
    val transformation = roundListSurfaceTransformation(itemScope, RoundListSurface.STANDARD_CARD)
    val colors = LocalWatchThemeColors.current

    @Composable
    fun HeroContent() {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = WatchUiMetrics.HeroHorizontalPadding,
                    vertical = WatchUiMetrics.HeroVerticalPadding
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (!state.hasStationLocation) {
                    stringResource(R.string.dashboard_set_qth)
                } else {
                    display?.second?.satelliteName
                        ?: stringResource(R.string.dashboard_no_pass)
                },
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = DashboardHeroSubtitleSelector.modelFor(state).resolveText(),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (state.hasStationLocation) {
                    stringResource(
                        R.string.dashboard_station_summary,
                        state.station.qthLocator
                            ?: stringResource(R.string.dashboard_qth_unknown),
                        state.station.latitude.formatCoord(),
                        state.station.longitude.formatCoord()
                    )
                } else {
                    stringResource(R.string.dashboard_qth_not_set)
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.primary,
                textAlign = TextAlign.Center
            )
        }
    }

    if (onClick == null) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(
                containerColor = colors.surface,
                contentColor = Color.White
            ),
            transformation = transformation
        ) {
            HeroContent()
        }
    } else {
        Card(
            modifier = modifier,
            onClick = onClick,
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(
                containerColor = colors.surface,
                contentColor = Color.White
            ),
            transformation = transformation
        ) {
            HeroContent()
        }
    }
}

@Composable
private fun PassesScreen(
    state: WatchUiState,
    onOpenRadar: (SatellitePass) -> Unit
) {
    var expandedPassKey by remember { mutableStateOf<String?>(null) }
    RoundListPage(
        title = stringResource(
            R.string.passes_title,
            state.settings.passWindowHours
        )
    ) {
        if (state.passCards.isEmpty()) {
            item {
                InfoCard(
                    stringResource(R.string.passes_empty_title),
                    when {
                        !state.hasStationLocation ->
                            stringResource(R.string.passes_set_qth_guidance)
                        state.satellites.isEmpty() ->
                            stringResource(R.string.passes_refresh_tle_guidance)
                        else -> state.passPlanningMessage
                    },
                    itemScope = this
                )
            }
        } else {
            state.passCards.forEach { (pass, card) ->
                val passKey = pass.cardKey()
                item(key = passKey) {
                    PassCard(
                        modifier = Modifier.animateItem(),
                        card = card,
                        expanded = expandedPassKey == passKey,
                        itemScope = this,
                        onClick = {
                            val result = PassCardInteractionReducer.onCardTap(
                                expandedPassKey = expandedPassKey,
                                tappedPassKey = passKey
                            )
                            expandedPassKey = result.expandedPassKey
                            if (result.openRadar) onOpenRadar(pass)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PassCard(
    modifier: Modifier = Modifier,
    card: PassCardUi,
    expanded: Boolean,
    itemScope: TransformingLazyColumnItemScope? = null,
    onClick: () -> Unit
) {
    val colors = LocalWatchThemeColors.current
    Card(
        modifier = modifier
            .animateContentSize(
                animationSpec = tween(durationMillis = PassCardAnimationPolicy.contentSizeMillis)
            )
            .fillMaxWidth()
            .roundListTransformedHeight(itemScope, RoundListSurface.STANDARD_CARD),
        transformation = roundListSurfaceTransformation(itemScope, RoundListSurface.STANDARD_CARD),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface,
            contentColor = Color.White
        ),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(WatchUiMetrics.CardPadding), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            PassCardSummary(card)
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(
                    animationSpec = tween(durationMillis = PassCardAnimationPolicy.detailExpandMillis)
                ) + fadeIn(
                    animationSpec = tween(durationMillis = PassCardAnimationPolicy.detailFadeMillis)
                ),
                exit = shrinkVertically(
                    animationSpec = tween(durationMillis = PassCardAnimationPolicy.detailExpandMillis)
                ) + fadeOut(
                    animationSpec = tween(durationMillis = PassCardAnimationPolicy.detailFadeMillis)
                )
            ) {
                PassCardDetails(card)
            }
        }
    }
}

@Composable
private fun PassCardDetails(card: PassCardUi) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        PassMetricRow(
            stringResource(R.string.passes_aos_los_label),
            "${card.aosTime} -> ${card.losTime}"
        )
        PassMetricRow(
            stringResource(R.string.passes_tca_max_label),
            "${card.tcaTime} · ${card.maxElevation}"
        )
        PassMetricRow(
            stringResource(R.string.passes_azimuth_label),
            "${card.aosAzimuth} -> ${card.losAzimuth}"
        )
        PassMetricRow(
            stringResource(R.string.passes_duration_label),
            card.duration
        )
        card.modeFrequencyHint?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = LocalWatchThemeColors.current.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PassCardSummary(card: PassCardUi) {
    val colors = LocalWatchThemeColors.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            card.satelliteName,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            stringResource(
                R.string.passes_countdown_max,
                card.aosCountdown,
                card.maxElevation
            ),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = if (card.isActive) colors.primary else colors.secondary
        )
    }
}

@Composable
private fun PassMetricRow(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = LocalWatchThemeColors.current.mutedText,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
        Text(
            text = value,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}

@Composable
private fun QthScreen(
    state: WatchUiState,
    onGps: () -> Unit,
    onCancelGps: () -> Unit,
    onEditLocator: () -> Unit,
    onSaveMapCenter: (Double, Double) -> Unit,
    onClearStationLocation: () -> Unit
) {
    var showClearLocationDialog by rememberSaveable { mutableStateOf(false) }
    var mapLatitude by remember { mutableDoubleStateOf(state.station.latitude) }
    var mapLongitude by remember { mutableDoubleStateOf(state.station.longitude) }
    val mapQth = MaidenheadLocator.fromCoordinates(mapLatitude, mapLongitude) ?: "--"
    val stationQth = state.station.qthLocator.takeIf { state.hasStationLocation }
    val stationCoordinates = "${state.station.latitude.formatCoord()} / ${state.station.longitude.formatCoord()}"
    val qthDisplay = QthDisplayPolicy.display(
        hasStationLocation = state.hasStationLocation,
        stationQth = stationQth,
        stationCoordinates = stationCoordinates
    ).resolveText()
    val gpsStatus = QthGpsStatusPolicy.statusMessage(
        locationMessage = state.locationMessage,
        gpsRequestInFlight = state.gpsRequestInFlight,
        kind = state.locationStatusKind
    )
    val setQthLabel = stringResource(R.string.dashboard_set_qth)

    RoundListPage(
        title = stringResource(R.string.qth_title),
        bottomSpacer = WatchUiMetrics.QthBottomActionSafeSpacer
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = WatchUiMetrics.MinimumSemanticTouchTarget)
                    .clickable(onClick = onEditLocator)
                    .padding(horizontal = 4.dp, vertical = 1.dp)
                    .semantics { contentDescription = qthDisplay.contentDescription },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    qthDisplay.primaryText,
                    style = MaterialTheme.typography.displayMedium,
                    color = LocalWatchThemeColors.current.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    qthDisplay.secondaryText,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalWatchThemeColors.current.mutedText,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        item {
            QthMapView(
                station = state.station,
                mapTileMode = state.settings.mapTileMode,
                onCenterChanged = { lat, lon ->
                    mapLatitude = lat
                    mapLongitude = lon
                }
            )
        }
        item {
            Text(
                "${mapLatitude.formatCoord()} / ${mapLongitude.formatCoord()} · $mapQth",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = LocalWatchThemeColors.current.mutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (
            gpsStatus.message != setQthLabel ||
            gpsStatus.disableGpsButton
        ) {
            item {
                Text(
                    gpsStatus.message,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = when (QthGpsStatusPolicy.colorRoleFor(gpsStatus.kind)) {
                        QthGpsStatusPolicy.errorColorRole -> WatchSemanticColors.ErrorForeground
                        QthGpsStatusPolicy.successColorRole -> LocalWatchThemeColors.current.primary
                        else -> LocalWatchThemeColors.current.mutedText
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                PeerActionButton(
                    label = if (state.gpsRequestInFlight) {
                        stringResource(R.string.cancel)
                    } else {
                        stringResource(R.string.action_gps)
                    },
                    modifier = Modifier.weight(1f),
                    contentDescription = if (state.gpsRequestInFlight) {
                        stringResource(R.string.action_cancel_gps)
                    } else {
                        stringResource(R.string.action_gps)
                    },
                    onClick = if (state.gpsRequestInFlight) onCancelGps else onGps
                )
                PeerActionButton(
                    label = stringResource(R.string.action_map),
                    modifier = Modifier.weight(1f),
                    contentDescription = stringResource(R.string.action_map),
                    onClick = { onSaveMapCenter(mapLatitude, mapLongitude) }
                )
            }
        }
        if (state.hasStationLocation) {
            item(key = "clear-saved-location") {
                RoundAction(
                    label = stringResource(R.string.clear_saved_location),
                    icon = Icons.Rounded.Place,
                    modifier = Modifier.fillMaxWidth(),
                    itemScope = this,
                    onClick = { showClearLocationDialog = true }
                )
            }
        }
    }
    AlertDialog(
        visible = showClearLocationDialog,
        onDismissRequest = { showClearLocationDialog = false },
        icon = {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                tint = WatchSemanticColors.WarningForeground,
                modifier = Modifier.size(30.dp)
            )
        },
        title = { Text(stringResource(R.string.clear_saved_location_title)) },
        text = {
            Text(
                text = stringResource(R.string.clear_saved_location_message),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            AlertDialogDefaults.ConfirmButton(
                onClick = {
                    showClearLocationDialog = false
                    onClearStationLocation()
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = WatchSemanticColors.ErrorContainer,
                    contentColor = WatchSemanticColors.OnErrorContainer
                )
            )
        },
        dismissButton = {
            AlertDialogDefaults.DismissButton(
                onClick = { showClearLocationDialog = false }
            )
        }
    )
}

@Composable
private fun QthPickerScreen(
    state: WatchUiState,
    onApply: (String) -> Unit
) {
    val mapQth = MaidenheadLocator.fromCoordinates(state.station.latitude, state.station.longitude)
    val initialQth = QthPickerPolicy.initialQth(
        currentQth = state.station.qthLocator.takeIf { state.hasStationLocation },
        mapQth = mapQth
    )
    val initialIndexes = remember(initialQth) { QthPickerPolicy.indexesForQth(initialQth) }
    val picker0 = rememberPickerState(QthPickerPolicy.optionsForPosition(0).size, initialIndexes[0], true)
    val picker1 = rememberPickerState(QthPickerPolicy.optionsForPosition(1).size, initialIndexes[1], true)
    val picker2 = rememberPickerState(QthPickerPolicy.optionsForPosition(2).size, initialIndexes[2], true)
    val picker3 = rememberPickerState(QthPickerPolicy.optionsForPosition(3).size, initialIndexes[3], true)
    val picker4 = rememberPickerState(QthPickerPolicy.optionsForPosition(4).size, initialIndexes[4], true)
    val picker5 = rememberPickerState(QthPickerPolicy.optionsForPosition(5).size, initialIndexes[5], true)
    val pickerStates = listOf(picker0, picker1, picker2, picker3, picker4, picker5)
    LaunchedEffect(initialQth) {
        initialIndexes.forEachIndexed { index, selected ->
            pickerStates[index].scrollToOption(selected)
        }
    }
    val selectedQth = QthPickerPolicy.qthForIndexes(
        IntArray(QthPickerPolicy.positionCount) { index -> pickerStates[index].selectedOptionIndex }
    )
    CompactPickerPage(
        title = stringResource(R.string.qth_edit_locator_title),
        value = selectedQth,
        showValue = false,
        helper = stringResource(R.string.qth_picker_helper),
        applyLabel = stringResource(R.string.action_apply),
        onApply = { onApply(selectedQth) }
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(WatchUiMetrics.QthCharacterPickerHeight)
        ) {
            pickerStates.forEachIndexed { position, pickerState ->
                QthCharacterPicker(
                    position = position,
                    state = pickerState,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QthCharacterPicker(
    position: Int,
    state: androidx.wear.compose.material3.PickerState,
    modifier: Modifier = Modifier
) {
    val options = QthPickerPolicy.optionsForPosition(position)
    val pickerDescription = stringResource(
        R.string.qth_character_description,
        position + 1
    )
    Picker(
        state = state,
        contentDescription = { pickerDescription },
        modifier = modifier.height(WatchUiMetrics.QthCharacterPickerHeight),
        verticalSpacing = WatchUiMetrics.CompactPickerVerticalSpacing,
        gradientColor = LocalWatchThemeColors.current.appBackground,
        option = { index ->
            Text(
                options[index].toString(),
                style = MaterialTheme.typography.numeralExtraSmall,
                textAlign = TextAlign.Center,
                color = Color.White
            )
        }
    )
}

@Composable
private fun DataScreen(state: WatchUiState, onRefresh: () -> Unit) {
    val freshness = TleFreshnessUiPolicy.model(state.tleFreshness).resolveText()
    val satelliteRefreshStatus = state.settings.satelliteDataRefreshStatus()
    val transmitterRefreshStatus = state.settings.transmitterDataRefreshStatus()
    RoundListPage(
        title = stringResource(R.string.tle_title),
        edgeButton = {
            ThemedEdgeButton(
                stringResource(R.string.action_refresh),
                onClick = onRefresh
            )
        }
    ) {
        item {
            StatusTextBlock(
                stringResource(R.string.tle_source_title),
                stringResource(
                    R.string.tle_cache_summary,
                    state.satellites.size,
                    freshness.statusLabel,
                    satelliteRefreshStatus.resolveFailureSuffix()
                )
            )
        }
        item {
            StatusTextBlock(
                stringResource(R.string.tle_freshness_title),
                stringResource(
                    R.string.tle_freshness_detail,
                    freshness.detail,
                    freshness.guidance
                )
            )
        }
        item {
            StatusTextBlock(
                stringResource(R.string.transmitters_title),
                stringResource(
                    R.string.transmitters_cache_summary,
                    state.transmitters.size,
                    state.settings.lastTransmitterDataUpdateMillis.relativeAge(
                        state.nowMillis
                    ),
                    transmitterRefreshStatus.resolveFailureSuffix()
                )
            )
        }
        item {
            StatusTextBlock(
                stringResource(R.string.state_title),
                state.refreshMessage
            )
        }
    }
}

@Composable
private fun SatellitesScreen(
    state: WatchUiState,
    onToggle: (Int) -> Unit,
    onOpenDetail: (SatelliteRecord) -> Unit,
    onStarter: () -> Unit,
    onOpenClearConfirm: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val pages = SatellitePagerPolicy.pages
    var removingSatelliteIds by remember { mutableStateOf(emptySet<Int>()) }
    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f,
        pageCount = { pages.size }
    )
    val allListState = rememberTransformingLazyColumnState()
    val selectedListState = rememberTransformingLazyColumnState()
    val settledPage = pages.getOrElse(pagerState.settledPage) { SatellitePagerPage.ALL }
    val colors = LocalWatchThemeColors.current
    val settledListState = when (settledPage) {
        SatellitePagerPage.ALL -> allListState
        SatellitePagerPage.SELECTED -> selectedListState
    }
    val showTimeText by remember(settledListState) {
        derivedStateOf { !settledListState.canScrollBackward }
    }
    ReportTimeTextVisibility(showTimeText)

    @Composable
    fun SatellitePage(
        page: SatellitePagerPage,
        listState: androidx.wear.compose.foundation.lazy.TransformingLazyColumnState,
        contentPadding: PaddingValues
    ) {
        RoundListTransformationProvider {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
            ) {
                val horizontalPadding = WatchUiMetrics.roundListHorizontalPadding(maxWidth)
                val visible = SatellitePagerPolicy.visibleSatellites(
                    page = page,
                    satellites = state.satellites,
                    selectedSatelliteIds = state.selectedSatelliteIds,
                    query = query
                )
                TransformingLazyColumn(
                    state = listState,
                    contentPadding = contentPadding,
                    rotaryScrollableBehavior = RotaryScrollableDefaults.behavior(scrollableState = listState),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = horizontalPadding)
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.satellites_title),
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Text(
                            when (page) {
                                SatellitePagerPage.ALL ->
                                    stringResource(R.string.satellites_page_all)
                                SatellitePagerPage.SELECTED ->
                                    stringResource(R.string.satellites_page_selected)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        SearchInput(query = query, onQueryChange = { query = it })
                    }
                    item {
                        val subtitle = when (page) {
                            SatellitePagerPage.ALL -> stringResource(
                                R.string.satellites_all_summary,
                                visible.size,
                                state.selectedSatelliteCount
                            )
                            SatellitePagerPage.SELECTED -> stringResource(
                                R.string.satellites_selected_summary,
                                visible.size
                            )
                        }
                        StatusTextBlock(
                            title = stringResource(R.string.satellites_results_title),
                            subtitle = subtitle
                        )
                    }
                    if (page == SatellitePagerPage.ALL) {
                        item {
                            val itemScope = this
                            RoundAction(
                                label = stringResource(R.string.satellites_starter_action),
                                modifier = Modifier.fillMaxWidth(),
                                enabled = state.satellites.isNotEmpty(),
                                itemScope = itemScope,
                                onClick = onStarter
                            )
                        }
                    }
                    when {
                        state.satellites.isEmpty() -> {
                            item {
                                StatusTextBlock(
                                    stringResource(R.string.satellites_no_cache),
                                    stringResource(R.string.passes_refresh_tle_guidance)
                                )
                            }
                        }

                        visible.isEmpty() -> {
                            item {
                                val title = if (page == SatellitePagerPage.SELECTED) {
                                    stringResource(R.string.satellites_selected_empty)
                                } else {
                                    stringResource(R.string.satellites_no_match)
                                }
                                val subtitle = if (query.isBlank()) {
                                    stringResource(R.string.satellites_use_all_guidance)
                                } else {
                                    stringResource(R.string.satellites_search_guidance)
                                }
                                StatusTextBlock(title, subtitle)
                            }
                        }

                        else -> {
                            visible.forEach { satellite ->
                                item(key = "${page.name}-${satellite.catalogNumber}") {
                                    val itemScope = this
                                    val selected = state.selectedSatelliteIds.contains(satellite.catalogNumber)
                                    val isPendingRemoval = removingSatelliteIds.contains(satellite.catalogNumber)
                                    LaunchedEffect(isPendingRemoval) {
                                        if (isPendingRemoval) {
                                            delay(SatelliteRemovalAnimationPolicy.exitDurationMillis.toLong())
                                            onToggle(satellite.catalogNumber)
                                            removingSatelliteIds = removingSatelliteIds - satellite.catalogNumber
                                        }
                                    }
                                    AnimatedVisibility(
                                        visible = !isPendingRemoval,
                                        enter = fadeIn(tween(SatelliteRemovalAnimationPolicy.exitDurationMillis)) +
                                            expandVertically(tween(SatelliteRemovalAnimationPolicy.exitDurationMillis)),
                                        exit = fadeOut(tween(SatelliteRemovalAnimationPolicy.exitDurationMillis)) +
                                            shrinkVertically(tween(SatelliteRemovalAnimationPolicy.exitDurationMillis)),
                                        modifier = Modifier.animateItem()
                                    ) {
                                        SatelliteCheckboxRow(
                                            satellite = satellite,
                                            selected = selected,
                                            modifier = Modifier.fillMaxWidth(),
                                            itemScope = itemScope,
                                            onCheckedChange = {
                                                if (page == SatellitePagerPage.SELECTED && selected) {
                                                    removingSatelliteIds = removingSatelliteIds + satellite.catalogNumber
                                                } else {
                                                    onToggle(satellite.catalogNumber)
                                                }
                                            },
                                            onOpenDetail = { onOpenDetail(satellite) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun PagerPage(page: SatellitePagerPage) {
        val listState = when (page) {
            SatellitePagerPage.ALL -> allListState
            SatellitePagerPage.SELECTED -> selectedListState
        }
        val showClearEdgeButton = page == SatellitePagerPage.SELECTED &&
            SatellitePagerPolicy.shouldShowClearEdgeButton(
                settledPage = settledPage,
                selectedCount = state.selectedSatelliteCount
            )
        if (showClearEdgeButton) {
            ScreenScaffold(
                scrollState = listState,
                edgeButton = {
                    ThemedEdgeButton(
                        stringResource(R.string.action_clear),
                        content = EdgeButtonContent.Clear,
                        onClick = onOpenClearConfirm
                    )
                },
                scrollIndicator = { WearScrollIndicator(state = listState) }
            ) { contentPadding ->
                SatellitePage(page, listState, contentPadding)
            }
        } else {
            ScreenScaffold(
                scrollState = listState,
                scrollIndicator = { WearScrollIndicator(state = listState) }
            ) { contentPadding ->
                SatellitePage(page, listState, contentPadding)
            }
        }
    }

    HorizontalPagerScaffold(
        pagerState = pagerState,
        pageIndicator = {
            HorizontalPageIndicator(
                pagerState = pagerState,
                selectedColor = colors.primary,
                unselectedColor = colors.mutedText.copy(alpha = 0.48f),
                backgroundColor = Color.Transparent,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = WatchUiMetrics.SatellitePageIndicatorBottomPadding)
            )
        }
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(0.dp),
            beyondViewportPageCount = PagerDefaults.BeyondViewportPageCount,
            flingBehavior = PagerScaffoldDefaults.snapWithSpringFlingBehavior(pagerState),
            gestureInclusion = PagerDefaults.gestureInclusion(pagerState)
        ) { pageIndex ->
            AnimatedPage(pageIndex = pageIndex, pagerState = pagerState) {
                PagerPage(pages[pageIndex])
            }
        }
    }
}

@Composable
private fun SatellitesClearConfirmScreen(
    selectedCount: Int,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    ReportTimeTextVisibility(false)
    AlertDialog(
        visible = true,
        onDismissRequest = onDismiss,
        title = {},
        icon = {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = stringResource(
                    R.string.satellites_clear_warning_description
                ),
                tint = WatchSemanticColors.WarningForeground,
                modifier = Modifier.size(30.dp)
            )
        },
        confirmButton = {
            val colors = LocalWatchThemeColors.current
            AlertDialogDefaults.ConfirmButton(
                onClick = onClear,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = WatchSemanticColors.ErrorContainer,
                    contentColor = WatchSemanticColors.OnErrorContainer,
                    disabledContainerColor = colors.surfaceVariant.copy(alpha = 0.62f),
                    disabledContentColor = colors.mutedText.copy(alpha = 0.64f)
                )
            )
        },
        dismissButton = {
            val colors = LocalWatchThemeColors.current
            AlertDialogDefaults.DismissButton(
                onClick = onDismiss,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = colors.surfaceVariant,
                    contentColor = Color.White
                )
            )
        },
        text = {
            Text(
                stringResource(
                    R.string.satellites_clear_confirmation,
                    selectedCount
                ),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    )
}

@Composable
private fun SatelliteCheckboxRow(
    satellite: SatelliteRecord,
    selected: Boolean,
    modifier: Modifier = Modifier,
    itemScope: TransformingLazyColumnItemScope?,
    onCheckedChange: () -> Unit,
    onOpenDetail: () -> Unit
) {
    val colors = LocalWatchThemeColors.current
    val toggleDescription = if (selected) {
        stringResource(R.string.satellite_unselect_description)
    } else {
        stringResource(R.string.satellite_select_description)
    }
    val detailsDescription = stringResource(
        R.string.satellite_open_details_description,
        satellite.displayName
    )
    val checkedTones = SatelliteSplitCheckboxColorPolicy.segmentTones(selected = true)
    val uncheckedTones = SatelliteSplitCheckboxColorPolicy.segmentTones(selected = false)
    SplitCheckboxButton(
        checked = selected,
        onCheckedChange = { onCheckedChange() },
        toggleContentDescription = toggleDescription,
        onContainerClick = onOpenDetail,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = WatchUiMetrics.ActionButtonHeight)
            .roundListTransformedHeight(itemScope, RoundListSurface.SPLIT_CHECKBOX_BUTTON)
            .semantics { contentDescription = detailsDescription },
        colors = CheckboxButtonDefaults.splitCheckboxButtonColors(
            checkedContainerColor = checkedTones.main.toColor(),
            checkedContentColor = Color.White,
            checkedSecondaryContentColor = colors.mutedText,
            checkedSplitContainerColor = checkedTones.split.toColor(),
            checkedBoxColor = colors.primary,
            checkedCheckmarkColor = Color.White,
            uncheckedContainerColor = uncheckedTones.main.toColor(),
            uncheckedContentColor = Color.White,
            uncheckedSecondaryContentColor = colors.mutedText,
            uncheckedSplitContainerColor = uncheckedTones.split.toColor(),
            uncheckedBoxColor = colors.mutedText
        ),
        transformation = roundListSurfaceTransformation(
            itemScope,
            RoundListSurface.SPLIT_CHECKBOX_BUTTON
        ),
        label = {
            Text(
                satellite.displayName,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        secondaryLabel = {
            Text(
                "#${satellite.catalogNumber}",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}

@Composable
private fun SatelliteSplitCheckboxTone.toColor(): Color {
    val colors = LocalWatchThemeColors.current
    return when (this) {
        SatelliteSplitCheckboxTone.Surface -> colors.surface
        SatelliteSplitCheckboxTone.SurfaceVariant -> colors.surfaceVariant
    }
}

@Composable
private fun SatelliteDetailScreen(
    state: WatchUiState,
    catalogNumber: Int?,
    onToggle: (Int) -> Unit
) {
    val satellite = catalogNumber?.let { id ->
        state.satellites.firstOrNull { it.catalogNumber == id }
    }
    val detail = satellite?.let {
        SatelliteDetailMapper.map(
            satellite = it,
            selectedSatelliteIds = state.selectedSatelliteIds,
            transmitters = state.transmitters
        )
    }

    RoundListPage(
        title = stringResource(R.string.satellite_title),
        edgeButton = if (satellite != null && detail != null) {
            {
                ThemedEdgeButton(
                    label = if (detail.selected) {
                        stringResource(R.string.satellite_unselect_description)
                    } else {
                        stringResource(R.string.satellite_select_description)
                    },
                    onClick = { onToggle(satellite.catalogNumber) }
                )
            }
        } else {
            null
        }
    ) {
        if (satellite == null || detail == null) {
            item {
                StatusTextBlock(
                    title = stringResource(R.string.satellite_empty_title),
                    subtitle = stringResource(R.string.satellite_empty_guidance)
                )
            }
        } else {
            item {
                StatusTextBlock(
                    title = detail.displayName,
                    subtitle = stringResource(
                        R.string.satellite_detail_header,
                        stringResource(
                            R.string.satellite_detail_catalog_number,
                            detail.catalogNumber
                        ),
                        stringResource(
                            if (detail.selected) {
                                R.string.satellite_detail_selected
                            } else {
                                R.string.satellite_detail_not_selected
                            }
                        )
                    )
                )
            }
            detail.orbitRows.forEach { row ->
                item {
                    val text = row.resolveText()
                    StatusTextBlock(
                        title = text.label,
                        subtitle = text.value
                    )
                }
            }
            item {
                StatusTextBlock(
                    title = stringResource(R.string.satellite_transmitters_title),
                    subtitle = if (detail.transmitters.isEmpty()) {
                        stringResource(R.string.satellite_no_active_transmitters)
                    } else {
                        pluralStringResource(
                            R.plurals.satellite_cached_transmitter_records,
                            detail.transmitters.size,
                            detail.transmitters.size
                        )
                    }
                )
            }
            detail.transmitters.forEach { transmitter ->
                item {
                    val downlink = transmitter.downlink?.resolveText()?.let {
                        stringResource(R.string.satellite_transmitter_downlink, it)
                    }
                    val uplink = transmitter.uplink?.resolveText()?.let {
                        stringResource(R.string.satellite_transmitter_uplink, it)
                    }
                    val status = transmitter.status ?: stringResource(
                        if (transmitter.isAlive) {
                            R.string.satellite_transmitter_active
                        } else {
                            R.string.satellite_transmitter_inactive
                        }
                    )
                    val inverted = if (transmitter.isInverted) {
                        stringResource(R.string.satellite_transmitter_inverted)
                    } else {
                        null
                    }
                    StatusTextBlock(
                        title = transmitter.title,
                        subtitle = listOfNotNull(
                            status,
                            downlink,
                            uplink,
                            inverted
                        ).joinToString("\n")
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchInput(query: String, onQueryChange: (String) -> Unit) {
    val colors = LocalWatchThemeColors.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchDescription = stringResource(R.string.satellite_search_description)
    BasicTextField(
        value = query,
        onValueChange = { raw -> onQueryChange(raw.take(16)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = {
                keyboardController?.hide()
                focusManager.clearFocus()
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(WatchUiMetrics.SearchInputHeight)
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) keyboardController?.show()
            }
            .testTag("satellite_search_input")
            .semantics { contentDescription = searchDescription },
        textStyle = MaterialTheme.typography.labelLarge.copy(
            color = Color.White,
            textAlign = TextAlign.Center
        ),
        decorationBox = { inner ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxSize()
                    .background(colors.surfaceVariant, RoundedCornerShape(22.dp))
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                if (query.isBlank()) {
                    Text(
                        stringResource(R.string.satellite_search_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.mutedText
                    )
                }
                inner()
            }
        }
    )
}

@Composable
private fun SettingsScreen(
    state: WatchUiState,
    onRadarKeepScreenOnChange: (Boolean) -> Unit,
    onRadarForwardAxisChange: (RadarForwardAxis) -> Unit,
    onRadarFallbackWristSideChange: (RadarWristSide) -> Unit,
    onNavigate: (WatchRoute) -> Unit
) {
    val systemWristSide by rememberSystemRadarWristSide()
    RoundListPage(title = stringResource(R.string.settings_title)) {
        SettingsMenuPolicy.topLevelItems(state.settings.developerOptionsEnabled).forEach { menuItem ->
            item(key = menuItem.key.name) {
                val title = menuItem.key.localizedLabel()
                val subtitle = when (menuItem.key) {
                    SettingsMenuKey.Appearance -> state.settings.themePreset.localizedLabel()
                    SettingsMenuKey.PassWindow -> stringResource(
                        R.string.pass_window_hours,
                        state.settings.passWindowHours
                    )
                    SettingsMenuKey.PassAlerts ->
                        PassAlertsSettingsPolicy.summary(
                            state.settings.passAlertAdvanceMinutes
                        ).resolveText()
                    SettingsMenuKey.DataFreshness -> DataFreshnessSettingsPolicy.summary(
                        enabled = state.settings.autoDataFreshnessEnabled,
                        freshness = TleFreshnessUiPolicy.model(state.tleFreshness)
                    ).resolveText()
                    SettingsMenuKey.MinimumElevation ->
                        MinimumElevationSettingsPolicy.summary(
                            enabled = state.settings.minimumElevationFilterEnabled,
                            thresholdDegrees = state.settings.minimumElevationDegrees
                        ).resolveText()
                    SettingsMenuKey.MapSource -> state.settings.mapTileMode.localizedLabel()
                    SettingsMenuKey.About -> BuildConfig.VERSION_NAME
                    SettingsMenuKey.DeveloperOptions ->
                        stringResource(R.string.settings_debug_tools)
                    SettingsMenuKey.Location,
                    SettingsMenuKey.Doppler -> ""
                }
                InfoCard(
                    title = title,
                    subtitle = subtitle,
                    onClick = { onNavigate(menuItem.route!!) },
                    itemScope = this
                )
            }
            if (menuItem.key == SettingsMenuKey.MapSource) {
                item(key = "radar-keep-screen-on") {
                    RadarKeepScreenOnSwitch(
                        checked = state.settings.radarKeepScreenOn,
                        itemScope = this,
                        onCheckedChange = onRadarKeepScreenOnChange
                    )
                }
                item(key = "radar-forward-axis") {
                    RadarForwardAxisSwitch(
                        forwardAxis = state.settings.radarForwardAxis,
                        systemWristSide = systemWristSide,
                        itemScope = this,
                        onForwardAxisChange = onRadarForwardAxisChange
                    )
                }
                if (systemWristSide == null) {
                    item(key = "radar-fallback-wrist-side") {
                        RadarFallbackWristSideSwitch(
                            wristSide = state.settings.radarFallbackWristSide,
                            itemScope = this,
                            onWristSideChange = onRadarFallbackWristSideChange
                        )
                    }
                }
            }
        }
        SettingsMenuPolicy.statusItems.forEach { menuItem ->
            item(key = "status-${menuItem.key.name}") {
                val subtitle = when (menuItem.key) {
                    SettingsMenuKey.Location -> state.station.source.name
                    SettingsMenuKey.Doppler ->
                        stringResource(R.string.settings_doppler_summary)
                    else -> ""
                }
                StatusTextBlock(menuItem.key.localizedLabel(), subtitle)
            }
        }
    }
}

@Composable
private fun SettingsMenuKey.localizedLabel(): String {
    val resource = when (this) {
        SettingsMenuKey.Appearance -> R.string.settings_appearance
        SettingsMenuKey.PassWindow -> R.string.settings_pass_window
        SettingsMenuKey.PassAlerts -> R.string.settings_pass_alerts
        SettingsMenuKey.DataFreshness -> R.string.settings_data_freshness
        SettingsMenuKey.MinimumElevation -> R.string.settings_minimum_elevation
        SettingsMenuKey.MapSource -> R.string.settings_map_source
        SettingsMenuKey.About -> R.string.settings_about
        SettingsMenuKey.DeveloperOptions -> R.string.settings_developer_options
        SettingsMenuKey.Location -> R.string.settings_location
        SettingsMenuKey.Doppler -> R.string.settings_doppler
    }
    return stringResource(resource)
}

@Composable
private fun RadarKeepScreenOnSwitch(
    checked: Boolean,
    itemScope: TransformingLazyColumnItemScope?,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalWatchThemeColors.current
    SwitchButton(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = WatchUiMetrics.ActionButtonHeight)
            .roundListTransformedHeight(itemScope, RoundListSurface.SWITCH_BUTTON),
        checked = checked,
        onCheckedChange = onCheckedChange,
        transformation = roundListSurfaceTransformation(
            itemScope,
            RoundListSurface.SWITCH_BUTTON
        ),
        colors = SwitchButtonDefaults.switchButtonColors(
            checkedContainerColor = colors.surfaceVariant,
            checkedContentColor = Color.White,
            checkedSecondaryContentColor = colors.mutedText,
            checkedIconColor = colors.primary,
            checkedThumbColor = colors.primary,
            checkedThumbIconColor = Color.Black,
            checkedTrackColor = colors.primary,
            checkedTrackBorderColor = colors.primary,
            uncheckedContainerColor = colors.surface,
            uncheckedContentColor = Color.White,
            uncheckedSecondaryContentColor = colors.mutedText,
            uncheckedIconColor = colors.mutedText,
            uncheckedThumbColor = colors.mutedText,
            uncheckedTrackColor = colors.surfaceVariant,
            uncheckedTrackBorderColor = colors.mutedText.copy(alpha = 0.42f)
        ),
        label = {
            Text(
                stringResource(R.string.settings_radar_keep_screen_on),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        secondaryLabel = {
            Text(
                stringResource(R.string.settings_radar_keep_screen_on_summary),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}

@Composable
private fun RadarForwardAxisSwitch(
    forwardAxis: RadarForwardAxis,
    systemWristSide: RadarWristSide?,
    itemScope: TransformingLazyColumnItemScope?,
    onForwardAxisChange: (RadarForwardAxis) -> Unit
) {
    val colors = LocalWatchThemeColors.current
    val checked = RadarForwardAxisSettingsPolicy.isChecked(forwardAxis)
    SwitchButton(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = WatchUiMetrics.ActionButtonHeight)
            .roundListTransformedHeight(itemScope, RoundListSurface.SWITCH_BUTTON),
        checked = checked,
        onCheckedChange = { isChecked ->
            onForwardAxisChange(RadarForwardAxisSettingsPolicy.axisForChecked(isChecked))
        },
        transformation = roundListSurfaceTransformation(
            itemScope,
            RoundListSurface.SWITCH_BUTTON
        ),
        colors = SwitchButtonDefaults.switchButtonColors(
            checkedContainerColor = colors.surfaceVariant,
            checkedContentColor = Color.White,
            checkedSecondaryContentColor = colors.mutedText,
            checkedIconColor = colors.primary,
            checkedThumbColor = colors.primary,
            checkedThumbIconColor = Color.Black,
            checkedTrackColor = colors.primary,
            checkedTrackBorderColor = colors.primary,
            uncheckedContainerColor = colors.surface,
            uncheckedContentColor = Color.White,
            uncheckedSecondaryContentColor = colors.mutedText,
            uncheckedIconColor = colors.mutedText,
            uncheckedThumbColor = colors.mutedText,
            uncheckedTrackColor = colors.surfaceVariant,
            uncheckedTrackBorderColor = colors.mutedText.copy(alpha = 0.42f)
        ),
        label = {
            Text(
                text = stringResource(R.string.radar_forward_axis_label),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        secondaryLabel = {
            Text(
                text = stringResource(
                    if (checked) {
                        when (systemWristSide) {
                            RadarWristSide.LEFT ->
                                R.string.radar_forward_axis_toward_hand_left_wrist
                            RadarWristSide.RIGHT ->
                                R.string.radar_forward_axis_toward_hand_right_wrist
                            null -> R.string.radar_forward_axis_toward_hand
                        }
                    } else {
                        R.string.radar_forward_axis_screen_top
                    }
                ),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}

@Composable
private fun RadarFallbackWristSideSwitch(
    wristSide: RadarWristSide,
    itemScope: TransformingLazyColumnItemScope?,
    onWristSideChange: (RadarWristSide) -> Unit
) {
    val colors = LocalWatchThemeColors.current
    val checked = RadarFallbackWristSideSettingsPolicy.isChecked(wristSide)
    SwitchButton(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = WatchUiMetrics.ActionButtonHeight)
            .roundListTransformedHeight(itemScope, RoundListSurface.SWITCH_BUTTON),
        checked = checked,
        onCheckedChange = { isChecked ->
            onWristSideChange(
                RadarFallbackWristSideSettingsPolicy.sideForChecked(isChecked)
            )
        },
        transformation = roundListSurfaceTransformation(
            itemScope,
            RoundListSurface.SWITCH_BUTTON
        ),
        colors = SwitchButtonDefaults.switchButtonColors(
            checkedContainerColor = colors.surfaceVariant,
            checkedContentColor = Color.White,
            checkedSecondaryContentColor = colors.mutedText,
            checkedIconColor = colors.primary,
            checkedThumbColor = colors.primary,
            checkedThumbIconColor = Color.Black,
            checkedTrackColor = colors.primary,
            checkedTrackBorderColor = colors.primary,
            uncheckedContainerColor = colors.surface,
            uncheckedContentColor = Color.White,
            uncheckedSecondaryContentColor = colors.mutedText,
            uncheckedIconColor = colors.mutedText,
            uncheckedThumbColor = colors.mutedText,
            uncheckedTrackColor = colors.surfaceVariant,
            uncheckedTrackBorderColor = colors.mutedText.copy(alpha = 0.42f)
        ),
        label = {
            Text(
                text = stringResource(R.string.radar_fallback_wrist_label),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        secondaryLabel = {
            Text(
                text = stringResource(
                    if (checked) {
                        R.string.radar_fallback_wrist_right
                    } else {
                        R.string.radar_fallback_wrist_left
                    }
                ),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}

@Composable
private fun PassAlertsScreen(
    state: WatchUiState,
    statusRows: List<PassAlertStatusRow>,
    onSelect: (Int) -> Unit
) {
    RoundListPage(title = stringResource(R.string.pass_alerts_title)) {
        PassAlertAdvancePolicy.options.forEach { option ->
            item(key = "pass-alert-${option.minutes}") {
                SettingsRadioRow(
                    label = passAlertAdvanceText(option.minutes),
                    selected = PassAlertAdvancePolicy.coerceMinutes(state.settings.passAlertAdvanceMinutes) == option.minutes,
                    itemScope = this,
                    onClick = { onSelect(option.minutes) }
                )
            }
        }
        statusRows.forEach { row ->
            item(key = "pass-alert-status-${row.label}") {
                val text = row.resolveText()
                StatusTextBlock(
                    text.label,
                    text.value
                )
            }
        }
        item {
            StatusTextBlock(
                stringResource(R.string.pass_alert_delivery_title),
                stringResource(R.string.pass_alert_delivery_summary)
            )
        }
    }
}

@Composable
private fun DataFreshnessScreen(
    state: WatchUiState,
    onCheckedChange: (Boolean) -> Unit
) {
    val freshness = TleFreshnessUiPolicy.model(state.tleFreshness).resolveText()
    RoundListPage(title = stringResource(R.string.data_freshness_title)) {
        item(key = "foreground-data-freshness") {
            SettingsSwitchRow(
                checked = state.settings.autoDataFreshnessEnabled,
                label = stringResource(R.string.settings_foreground_data_freshness),
                secondaryLabel = stringResource(
                    R.string.settings_foreground_data_freshness_summary
                ),
                itemScope = this,
                onCheckedChange = onCheckedChange
            )
        }
        item {
            StatusTextBlock(
                stringResource(R.string.data_freshness_current_tle),
                stringResource(
                    R.string.data_freshness_current_summary,
                    freshness.statusLabel,
                    freshness.detail
                )
            )
        }
        item {
            StatusTextBlock(
                stringResource(R.string.data_freshness_offline_use),
                freshness.guidance
            )
        }
        item {
            StatusTextBlock(
                stringResource(R.string.data_freshness_foreground_only),
                stringResource(R.string.data_freshness_foreground_summary)
            )
        }
        item {
            StatusTextBlock(
                stringResource(R.string.data_freshness_transmitters),
                stringResource(
                    R.string.data_freshness_satnogs_age,
                    state.settings.lastTransmitterDataUpdateMillis.relativeAge(
                        state.nowMillis
                    )
                )
            )
        }
    }
}

@Composable
private fun MinimumElevationScreen(
    state: WatchUiState,
    onEnabledChange: (Boolean) -> Unit,
    onThresholdSelected: (Int) -> Unit
) {
    RoundListPage(title = stringResource(R.string.minimum_elevation_title)) {
        item(key = "minimum-elevation-enabled") {
            SettingsSwitchRow(
                checked = state.settings.minimumElevationFilterEnabled,
                label = stringResource(R.string.settings_minimum_elevation),
                secondaryLabel = stringResource(R.string.settings_minimum_elevation_summary),
                itemScope = this,
                onCheckedChange = onEnabledChange
            )
        }
        MinimumElevationSettingsPolicy.thresholds.forEach { threshold ->
            item(key = "minimum-elevation-$threshold") {
                SettingsRadioRow(
                    label = stringResource(
                        R.string.minimum_elevation_value,
                        threshold
                    ),
                    selected = MinimumElevationPolicy.coerceThresholdDegrees(
                        state.settings.minimumElevationDegrees
                    ) == threshold,
                    enabled = state.settings.minimumElevationFilterEnabled,
                    itemScope = this,
                    onClick = {
                        onThresholdSelected(threshold)
                        if (!state.settings.minimumElevationFilterEnabled) {
                            onEnabledChange(true)
                        }
                    }
                )
            }
        }
        item {
            StatusTextBlock(
                stringResource(R.string.minimum_elevation_planning_title),
                stringResource(R.string.minimum_elevation_planning_summary)
            )
        }
    }
}

@Composable
private fun SettingsRadioRow(
    label: String,
    selected: Boolean,
    itemScope: TransformingLazyColumnItemScope?,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val colors = LocalWatchThemeColors.current
    RadioButton(
        selected = selected,
        onSelect = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(WatchUiMetrics.ActionButtonHeight)
            .roundListTransformedHeight(itemScope, RoundListSurface.RADIO_BUTTON),
        colors = RadioButtonDefaults.radioButtonColors(
            selectedContainerColor = colors.surfaceVariant,
            selectedContentColor = Color.White,
            selectedSecondaryContentColor = colors.mutedText,
            selectedIconColor = colors.primary,
            selectedControlColor = colors.primary,
            unselectedContainerColor = colors.surface,
            unselectedContentColor = Color.White,
            unselectedSecondaryContentColor = colors.mutedText,
            unselectedIconColor = colors.mutedText,
            unselectedControlColor = colors.mutedText,
            disabledSelectedContainerColor = colors.surface.copy(alpha = 0.62f),
            disabledSelectedContentColor = colors.mutedText,
            disabledSelectedSecondaryContentColor = colors.mutedText.copy(alpha = 0.72f),
            disabledSelectedIconColor = colors.mutedText,
            disabledSelectedControlColor = colors.mutedText,
            disabledUnselectedContainerColor = colors.surface.copy(alpha = 0.46f),
            disabledUnselectedContentColor = colors.mutedText,
            disabledUnselectedSecondaryContentColor = colors.mutedText.copy(alpha = 0.72f),
            disabledUnselectedIconColor = colors.mutedText,
            disabledUnselectedControlColor = colors.mutedText
        ),
        transformation = roundListSurfaceTransformation(
            itemScope,
            RoundListSurface.RADIO_BUTTON
        ),
        label = {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}

@Composable
private fun SettingsSwitchRow(
    checked: Boolean,
    label: String,
    secondaryLabel: String,
    itemScope: TransformingLazyColumnItemScope?,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalWatchThemeColors.current
    SwitchButton(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = WatchUiMetrics.ActionButtonHeight)
            .roundListTransformedHeight(itemScope, RoundListSurface.SWITCH_BUTTON),
        checked = checked,
        onCheckedChange = onCheckedChange,
        transformation = roundListSurfaceTransformation(
            itemScope,
            RoundListSurface.SWITCH_BUTTON
        ),
        colors = SwitchButtonDefaults.switchButtonColors(
            checkedContainerColor = colors.surfaceVariant,
            checkedContentColor = Color.White,
            checkedSecondaryContentColor = colors.mutedText,
            checkedIconColor = colors.primary,
            checkedThumbColor = colors.primary,
            checkedThumbIconColor = Color.Black,
            checkedTrackColor = colors.primary,
            checkedTrackBorderColor = colors.primary,
            uncheckedContainerColor = colors.surface,
            uncheckedContentColor = Color.White,
            uncheckedSecondaryContentColor = colors.mutedText,
            uncheckedIconColor = colors.mutedText,
            uncheckedThumbColor = colors.mutedText,
            uncheckedTrackColor = colors.surfaceVariant,
            uncheckedTrackBorderColor = colors.mutedText.copy(alpha = 0.42f)
        ),
        label = {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        secondaryLabel = {
            Text(
                secondaryLabel,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}

@Composable
private fun AppearanceScreen(
    state: WatchUiState,
    onThemeChange: (AppThemePreset) -> Unit
) {
    RoundListPage(title = stringResource(R.string.appearance_title)) {
        AppThemePreset.entries.forEach { preset ->
            item(key = preset.name) {
                ThemePresetRow(
                    preset = preset,
                    selected = state.settings.themePreset == preset,
                    itemScope = this,
                    onClick = { onThemeChange(preset) }
                )
            }
        }
    }
}

@Composable
private fun ThemePresetRow(
    preset: AppThemePreset,
    selected: Boolean,
    itemScope: TransformingLazyColumnItemScope?,
    onClick: () -> Unit
) {
    val colors = LocalWatchThemeColors.current
    val presetColors = if (preset == AppThemePreset.SYSTEM) {
        colors
    } else {
        WatchThemeCatalog.colorsFor(preset)
    }
    RadioButton(
        selected = selected,
        onSelect = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(WatchUiMetrics.ActionButtonHeight)
            .roundListTransformedHeight(itemScope, RoundListSurface.RADIO_BUTTON),
        colors = RadioButtonDefaults.radioButtonColors(
            selectedContainerColor = colors.surfaceVariant,
            selectedContentColor = Color.White,
            selectedSecondaryContentColor = colors.mutedText,
            selectedIconColor = colors.primary,
            selectedControlColor = colors.primary,
            unselectedContainerColor = colors.surface,
            unselectedContentColor = Color.White,
            unselectedSecondaryContentColor = colors.mutedText,
            unselectedIconColor = colors.mutedText,
            unselectedControlColor = colors.mutedText
        ),
        transformation = roundListSurfaceTransformation(
            itemScope,
            RoundListSurface.RADIO_BUTTON
        ),
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(presetColors.primary, RoundedCornerShape(50))
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    preset.localizedLabel(),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1
                )
            }
        }
    )
}

@Composable
private fun MapSourceScreen(
    state: WatchUiState,
    onMapTileModeSelected: (MapTileMode) -> Unit
) {
    RoundListPage(title = stringResource(R.string.map_source_title)) {
        MapSourceSelectionPolicy.options.forEach { mode ->
            item(key = mode.name) {
                MapSourceRadioRow(
                    mode = mode,
                    selected = mode == state.settings.mapTileMode,
                    itemScope = this,
                    onClick = { onMapTileModeSelected(mode) }
                )
            }
        }
    }
}

@Composable
private fun AboutScreen(
    state: WatchUiState,
    onOpenPrivacy: () -> Unit,
    onOpenLegal: () -> Unit,
    onDeveloperOptionsUnlocked: () -> Unit
) {
    var unlockTapCount by remember(state.settings.developerOptionsEnabled) { mutableStateOf(0) }
    RoundListPage(title = stringResource(R.string.about_title)) {
        AboutPagePolicy.rowsFor(
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE
        ).forEach { row ->
            item(key = "about-${row.label}") {
                val text = row.resolveText()
                val modifier = if (row.label == AboutInfoLabel.Version) {
                    Modifier
                        .heightIn(min = WatchUiMetrics.MinimumSemanticTouchTarget)
                        .clickable {
                            if (DeveloperOptionsPolicy.shouldEnableDeveloperOptions(
                                    currentTapCount = unlockTapCount,
                                    alreadyEnabled = state.settings.developerOptionsEnabled
                                )
                            ) {
                                unlockTapCount = 0
                                onDeveloperOptionsUnlocked()
                            } else {
                                unlockTapCount = DeveloperOptionsPolicy.nextUnlockTapCount(
                                    currentTapCount = unlockTapCount,
                                    alreadyEnabled = state.settings.developerOptionsEnabled
                                )
                            }
                        }
                } else {
                    Modifier
                }
                StatusTextBlock(text.label, text.value, modifier = modifier)
            }
        }
        item(key = "about-privacy-policy") {
            InfoCard(
                title = stringResource(R.string.legal_privacy_policy),
                subtitle = stringResource(R.string.legal_privacy_policy_summary),
                onClick = onOpenPrivacy,
                itemScope = this
            )
        }
        item(key = "about-legal-notices") {
            InfoCard(
                title = stringResource(R.string.legal_notices),
                subtitle = stringResource(R.string.legal_notices_summary),
                onClick = onOpenLegal,
                itemScope = this
            )
        }
    }
}

@Composable
private fun LegalNoticesScreen(
    onOpenDocument: (LegalDocument) -> Unit
) {
    RoundListPage(title = stringResource(R.string.legal_notices)) {
        LegalDocumentCatalog.noticesDocuments.forEach { document ->
            item(key = "legal-${document.id}") {
                InfoCard(
                    title = stringResource(document.titleRes),
                    subtitle = stringResource(R.string.legal_packaged_offline),
                    onClick = { onOpenDocument(document) },
                    itemScope = this
                )
            }
        }
    }
}

@Composable
private fun LegalDocumentScreen(document: LegalDocument?) {
    val context = LocalContext.current
    val unavailable = stringResource(R.string.legal_document_unavailable)
    val title = document?.let { stringResource(it.titleRes) } ?: unavailable
    var documentText by remember(document) { mutableStateOf<String?>(null) }
    var loadFailed by remember(document) { mutableStateOf(document == null) }

    LaunchedEffect(document) {
        if (document == null) return@LaunchedEffect
        OfflineLegalDocumentLoader.forContext(context)
            .readOffMainThread(document)
            .onSuccess {
                documentText = it
                loadFailed = false
            }
            .onFailure {
                documentText = null
                loadFailed = true
            }
    }

    val chunks = remember(documentText) {
        documentText?.let(::legalDocumentChunks).orEmpty()
    }
    RoundListPage(title = title) {
        when {
            loadFailed -> item(key = "legal-load-failed") {
                Text(
                    text = unavailable,
                    color = WatchSemanticColors.ErrorForeground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            documentText == null -> item(key = "legal-loading") {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            }
            else -> chunks.forEachIndexed { index, chunk ->
                item(key = "legal-chunk-$index") {
                    Text(
                        text = chunk,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun DeveloperOptionsScreen(
    onTriggerPassAlert: () -> Unit,
    onTriggerPassNotification: () -> Unit,
    onTriggerCalibrationHint: () -> Unit,
    onDisableDeveloperOptions: () -> Unit
) {
    RoundListPage(title = stringResource(R.string.developer_options_title)) {
        item(key = DeveloperOptionAction.TriggerPassAlert.name) {
            RoundAction(
                label = DeveloperOptionAction.TriggerPassAlert.resolveLabel(),
                icon = Icons.Rounded.Timer,
                modifier = Modifier.fillMaxWidth(),
                itemScope = this,
                onClick = onTriggerPassAlert
            )
        }
        item(key = DeveloperOptionAction.TriggerPassNotification.name) {
            RoundAction(
                label = DeveloperOptionAction.TriggerPassNotification.resolveLabel(),
                icon = Icons.Rounded.NotificationsActive,
                modifier = Modifier.fillMaxWidth(),
                itemScope = this,
                onClick = onTriggerPassNotification
            )
        }
        item(key = DeveloperOptionAction.TriggerCalibrationHint.name) {
            RoundAction(
                label = DeveloperOptionAction.TriggerCalibrationHint.resolveLabel(),
                icon = Icons.Rounded.TrackChanges,
                modifier = Modifier.fillMaxWidth(),
                itemScope = this,
                onClick = onTriggerCalibrationHint
            )
        }
        item(key = DeveloperOptionAction.DisableDeveloperOptions.name) {
            RoundAction(
                label = DeveloperOptionAction.DisableDeveloperOptions.resolveLabel(),
                icon = Icons.Rounded.Code,
                modifier = Modifier.fillMaxWidth(),
                itemScope = this,
                onClick = onDisableDeveloperOptions
            )
        }
    }
}

@Composable
private fun MapSourceRadioRow(
    mode: MapTileMode,
    selected: Boolean,
    itemScope: TransformingLazyColumnItemScope?,
    onClick: () -> Unit
) {
    val colors = LocalWatchThemeColors.current
    RadioButton(
        selected = selected,
        onSelect = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(WatchUiMetrics.ActionButtonHeight)
            .roundListTransformedHeight(itemScope, RoundListSurface.RADIO_BUTTON),
        colors = RadioButtonDefaults.radioButtonColors(
            selectedContainerColor = colors.surfaceVariant,
            selectedContentColor = Color.White,
            selectedSecondaryContentColor = colors.mutedText,
            selectedIconColor = colors.primary,
            selectedControlColor = colors.primary,
            unselectedContainerColor = colors.surface,
            unselectedContentColor = Color.White,
            unselectedSecondaryContentColor = colors.mutedText,
            unselectedIconColor = colors.mutedText,
            unselectedControlColor = colors.mutedText
        ),
        transformation = roundListSurfaceTransformation(
            itemScope,
            RoundListSurface.RADIO_BUTTON
        ),
        label = {
            Text(
                mode.localizedLabel(),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
    )
}

@Composable
private fun PassWindowAdjusterScreen(
    state: WatchUiState,
    onApply: (Int) -> Unit
) {
    var selectedHours by remember { mutableStateOf(PassWindowAdjusterPolicy.coerceHours(state.settings.passWindowHours)) }
    LaunchedEffect(state.settings.passWindowHours) {
        selectedHours = PassWindowAdjusterPolicy.coerceHours(state.settings.passWindowHours)
    }
    val scaffoldState = rememberLazyListState()
    ReportTimeTextVisibility(true)

    ScreenScaffold(
        scrollState = scaffoldState,
        edgeButton = {
            ThemedEdgeButton(
                label = stringResource(R.string.action_apply),
                content = EdgeButtonContent.Apply,
                modifier = Modifier.edgeButtonScrollable(scaffoldState),
                onClick = { onApply(selectedHours) }
            )
        },
        scrollIndicator = {}
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            LazyColumn(
                state = scaffoldState,
                userScrollEnabled = false,
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding
            ) {
                item {
                    Spacer(Modifier.fillParentMaxSize())
                }
            }
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val horizontalPadding = WatchUiMetrics.roundListHorizontalPadding(maxWidth)
                Text(
                    stringResource(R.string.pass_window_title),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding)
                        .padding(top = contentPadding.calculateTopPadding())
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding),
                    contentAlignment = Alignment.Center
                ) {
                    PassWindowHorizontalAdjuster(
                        hours = selectedHours,
                        onDecrease = { selectedHours = PassWindowAdjusterPolicy.decrease(selectedHours) },
                        onIncrease = { selectedHours = PassWindowAdjusterPolicy.increase(selectedHours) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PassWindowHorizontalAdjuster(
    hours: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    val colors = LocalWatchThemeColors.current
    val adjusterDescription = stringResource(
        R.string.pass_window_adjuster_description,
        hours
    )
    val decreaseDescription = stringResource(
        R.string.pass_window_decrease_description
    )
    val increaseDescription = stringResource(
        R.string.pass_window_increase_description
    )
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val slots = WatchUiMetrics.passWindowAdjusterSlots(maxWidth)
        ButtonGroup(
            modifier = Modifier
                .width(slots.totalWidth)
                .height(WatchUiMetrics.PassWindowAdjusterControlHeight)
                .semantics { contentDescription = adjusterDescription },
            spacing = WatchUiMetrics.PassWindowAdjusterSpacing,
            contentPadding = PaddingValues(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(slots.sideSlotWidth.value)
                    .height(WatchUiMetrics.PassWindowAdjusterControlHeight),
                contentAlignment = Alignment.Center
            ) {
                PassWindowAdjusterButton(
                    contentDescription = decreaseDescription,
                    enabled = PassWindowAdjusterPolicy.canDecrease(hours),
                    onClick = onDecrease,
                    icon = Icons.Rounded.Remove
                )
            }
            Box(
                modifier = Modifier
                    .weight(slots.valueSlotWidth.value)
                    .height(WatchUiMetrics.PassWindowAdjusterControlHeight),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        hours.toString(),
                        style = MaterialTheme.typography.numeralLarge,
                        color = Color.White,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(
                        modifier = Modifier.width(WatchUiMetrics.PassWindowUnitGap)
                    )
                    Text(
                        stringResource(R.string.unit_hours_short),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(
                            bottom = WatchUiMetrics.PassWindowUnitBottomPadding
                        ).width(WatchUiMetrics.PassWindowUnitSlotWidth)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(slots.sideSlotWidth.value)
                    .height(WatchUiMetrics.PassWindowAdjusterControlHeight),
                contentAlignment = Alignment.Center
            ) {
                PassWindowAdjusterButton(
                    contentDescription = increaseDescription,
                    enabled = PassWindowAdjusterPolicy.canIncrease(hours),
                    onClick = onIncrease,
                    icon = Icons.Rounded.Add
                )
            }
        }
    }
}

@Composable
internal fun PassWindowAdjusterButton(
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    icon: ImageVector
) {
    val colors = LocalWatchThemeColors.current
    Button(
        onClick = onClick,
        modifier = Modifier
            .requiredWidth(WatchUiMetrics.PassWindowAdjusterButtonWidth)
            .requiredHeight(WatchUiMetrics.PassWindowAdjusterButtonHeight)
            .semantics { this.contentDescription = contentDescription },
        enabled = enabled,
        shape = RoundedCornerShape(percent = WatchUiMetrics.PassWindowAdjusterButtonShapePercent),
        contentPadding = PaddingValues(WatchUiMetrics.PassWindowAdjusterButtonContentPadding),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            disabledContainerColor = colors.surface.copy(alpha = 0.52f),
            disabledContentColor = colors.mutedText.copy(alpha = 0.52f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(WatchUiMetrics.PassWindowAdjusterIconSize)
            )
        }
    }
}

@Composable
private fun SatellitePass.countdownLabel(
    nowMillis: Long,
    clockTimeFormatter: ClockTimeFormatter
): String {
    return when {
        nowMillis < aosMillis -> stringResource(
            R.string.pass_countdown_before,
            (aosMillis - nowMillis).formatCountdown(),
            clockTimeFormatter.formatMinutes(tcaMillis)
        )
        nowMillis < losMillis -> stringResource(
            R.string.pass_countdown_active,
            (losMillis - nowMillis).formatCountdown(),
            clockTimeFormatter.formatMinutes(tcaMillis)
        )
        else -> stringResource(
            R.string.pass_countdown_ended,
            clockTimeFormatter.formatMinutes(losMillis)
        )
    }
}

@Composable
private fun Long?.relativeAge(nowMillis: Long): String {
    val timestamp = this ?: return stringResource(R.string.relative_age_never)
    if (timestamp > nowMillis) {
        return stringResource(R.string.relative_age_clock_error)
    }
    val ageMillis = nowMillis - timestamp
    val minutes = ageMillis / 60_000L
    return when {
        minutes < 1 -> stringResource(R.string.relative_age_now)
        minutes < 60 -> stringResource(R.string.relative_age_minutes, minutes)
        minutes < 24 * 60 -> stringResource(
            R.string.relative_age_hours,
            minutes / 60
        )
        else -> stringResource(
            R.string.relative_age_days,
            minutes / (24 * 60)
        )
    }
}

private fun Context.canScheduleWatch4SatExactAlarms(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val alarmManager = getSystemService(AlarmManager::class.java) ?: return false
    return runCatching { alarmManager.canScheduleExactAlarms() }.getOrDefault(false)
}

private fun Context.isWatch4SatNotificationChannelEnabled(channelId: String): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
    val notificationManager = getSystemService(NotificationManager::class.java) ?: return false
    val channel = notificationManager.getNotificationChannel(channelId) ?: return true
    return channel.importance != NotificationManager.IMPORTANCE_NONE
}

@Composable
private fun Long.formatCountdown(): String {
    val seconds = (this / 1000L).coerceAtLeast(0L)
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return if (minutes >= 60) {
        stringResource(
            R.string.duration_hours_minutes_short,
            minutes / 60,
            minutes % 60
        )
    } else {
        stringResource(
            R.string.duration_clock_short,
            minutes,
            remainingSeconds
        )
    }
}

private fun Long.formatMhz(): String {
    return "%.3f MHz".format(Locale.US, this / 1_000_000.0)
}

private fun Double.formatKhz(): String {
    return "%+.1f kHz".format(Locale.US, this)
}

private fun Double.formatCoord(): String {
    return "%.2f".format(Locale.US, this)
}

private fun Double.roundToDegreeLabel(): String {
    return "${roundToInt()}°"
}

private fun SatellitePass.cardKey(): String {
    return "${catalogNumber}-${aosMillis}"
}

private fun SatellitePass.notificationKey(): String {
    return PassStartNotificationPolicy.passKey(catalogNumber = catalogNumber, aosMillis = aosMillis)
}

private fun SatellitePass.matchesPendingForegroundAlarm(pending: PendingForegroundPassAlarm): Boolean {
    return catalogNumber == pending.catalogNumber && aosMillis == pending.aosMillis
}
