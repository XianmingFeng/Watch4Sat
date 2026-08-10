package com.xianming.watch4sat.wear.firstRun

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CheckboxButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SplitCheckboxButton
import androidx.wear.compose.material3.SwipeToDismissBox
import androidx.wear.compose.material3.Text
import com.xianming.watch4sat.R
import com.xianming.watch4sat.domain.model.SatelliteRecord
import com.xianming.watch4sat.wear.InfoCard
import com.xianming.watch4sat.wear.ReportTimeTextVisibility
import com.xianming.watch4sat.wear.RoundAction
import com.xianming.watch4sat.wear.RoundListTransformationProvider
import com.xianming.watch4sat.wear.StatusTextBlock
import com.xianming.watch4sat.wear.WatchUiMetrics
import com.xianming.watch4sat.wear.WatchUiState
import com.xianming.watch4sat.wear.WearScrollIndicator
import com.xianming.watch4sat.wear.location.QthGpsKeepScreenOn
import com.xianming.watch4sat.wear.location.QthGpsPowerPolicy
import com.xianming.watch4sat.wear.roundListSurfaceTransformation
import com.xianming.watch4sat.wear.roundListTransformedHeight
import com.xianming.watch4sat.wear.state.FirstRunSetupDecision
import com.xianming.watch4sat.wear.state.FirstRunSetupPolicy
import com.xianming.watch4sat.wear.state.FirstRunSetupStep
import com.xianming.watch4sat.wear.state.FirstRunSetupStepStatus
import com.xianming.watch4sat.wear.state.DataRefreshAttemptStatus
import com.xianming.watch4sat.wear.state.QthGpsStatusPolicy
import com.xianming.watch4sat.wear.state.RoundListSurface
import com.xianming.watch4sat.wear.state.satelliteDataRefreshStatus
import com.xianming.watch4sat.wear.theme.LocalWatchThemeColors
import com.xianming.watch4sat.wear.theme.WatchSemanticColors

private const val FirstRunStepSlideMillis = 180
private const val FirstRunStepEnterFadeMillis = 120
private const val FirstRunStepExitFadeMillis = 90

object FirstRunSetupTestTags {
    const val Screen = "first_run_setup_screen"
}

@Composable
fun FirstRunSetupScreen(
    state: WatchUiState,
    setupDecision: FirstRunSetupDecision,
    notificationPermissionGranted: Boolean,
    exactAlarmAvailable: Boolean,
    onMoveToStep: (FirstRunSetupStep) -> Unit,
    onRefreshTle: () -> Unit,
    onRequestGps: () -> Unit,
    onCancelGps: () -> Unit,
    onToggleSatellite: (Int) -> Unit,
    onApplyStarterSelection: () -> Unit,
    onRequestNotifications: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onSkipStep: (FirstRunSetupStep, FirstRunSetupStep) -> Unit,
    onFinish: () -> Unit,
    onExit: () -> Unit
) {
    val activeStep = setupDecision.step
    var satellitePickerOpen by rememberSaveable { mutableStateOf(false) }
    QthGpsKeepScreenOn(
        enabled = QthGpsPowerPolicy.shouldKeepScreenOn(
            gpsRequestInFlight = state.gpsRequestInFlight,
            qthSurfaceVisible = activeStep == FirstRunSetupStep.Qth
        )
    )
    val navigateBack = {
        when {
            satellitePickerOpen -> satellitePickerOpen = false
            activeStep.previous() != null -> onMoveToStep(checkNotNull(activeStep.previous()))
            else -> onExit()
        }
    }
    BackHandler(enabled = true) {
        navigateBack()
    }
    SwipeToDismissBox(
        onDismissed = navigateBack,
        modifier = Modifier.testTag(FirstRunSetupTestTags.Screen)
    ) { isBackground ->
        if (isBackground) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LocalWatchThemeColors.current.appBackground)
            )
        } else {
            AnimatedFirstRunStepHost(activeStep = activeStep) { animatedStep ->
                when (animatedStep) {
                    FirstRunSetupStep.Welcome -> WelcomeStep(
                        onNext = { onMoveToStep(FirstRunSetupStep.Data) }
                    )
                    FirstRunSetupStep.Data -> TleStep(
                        state = state,
                        skipped = setupDecision.statusFor(FirstRunSetupStep.Data) ==
                            FirstRunSetupStepStatus.EXPLICITLY_SKIPPED,
                        onRefresh = onRefreshTle,
                        onSkip = { onSkipStep(FirstRunSetupStep.Data, FirstRunSetupStep.Qth) },
                        onNext = { onMoveToStep(FirstRunSetupStep.Qth) }
                    )
                    FirstRunSetupStep.Qth -> QthStep(
                        state = state,
                        skipped = setupDecision.statusFor(FirstRunSetupStep.Qth) ==
                            FirstRunSetupStepStatus.EXPLICITLY_SKIPPED,
                        onRequestGps = onRequestGps,
                        onCancelGps = onCancelGps,
                        onSkip = { onSkipStep(FirstRunSetupStep.Qth, FirstRunSetupStep.Satellites) },
                        onNext = { onMoveToStep(FirstRunSetupStep.Satellites) }
                    )
                    FirstRunSetupStep.Satellites -> SatellitesStep(
                        state = state,
                        skipped = setupDecision.statusFor(FirstRunSetupStep.Satellites) ==
                            FirstRunSetupStepStatus.EXPLICITLY_SKIPPED,
                        satellitePickerOpen = satellitePickerOpen,
                        onSatellitePickerOpenChange = { satellitePickerOpen = it },
                        onRefreshTle = onRefreshTle,
                        onApplyStarterSelection = onApplyStarterSelection,
                        onToggleSatellite = onToggleSatellite,
                        onSkip = {
                            onSkipStep(
                                FirstRunSetupStep.Satellites,
                                FirstRunSetupStep.Notifications
                            )
                        },
                        onNext = { onMoveToStep(FirstRunSetupStep.Notifications) }
                    )
                    FirstRunSetupStep.Notifications -> PermissionsStep(
                        notificationPermissionGranted = notificationPermissionGranted,
                        exactAlarmAvailable = exactAlarmAvailable,
                        onRequestNotifications = onRequestNotifications,
                        onOpenExactAlarmSettings = onOpenExactAlarmSettings,
                        onSkip = {
                            onSkipStep(
                                FirstRunSetupStep.Notifications,
                                FirstRunSetupStep.Done
                            )
                        },
                        onNext = { onMoveToStep(FirstRunSetupStep.Done) }
                    )
                    FirstRunSetupStep.Done -> FinishStep(
                        enabled = setupDecision.canMarkComplete,
                        onFinish = onFinish
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedFirstRunStepHost(
    activeStep: FirstRunSetupStep,
    content: @Composable (FirstRunSetupStep) -> Unit
) {
    AnimatedContent(
        targetState = activeStep,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            val initialIndex = FirstRunSetupStep.entries.indexOf(initialState)
            val targetIndex = FirstRunSetupStep.entries.indexOf(targetState)
            val forward = targetIndex >= initialIndex
            val direction = if (forward) {
                AnimatedContentTransitionScope.SlideDirection.Left
            } else {
                AnimatedContentTransitionScope.SlideDirection.Right
            }
            (
                slideIntoContainer(
                    towards = direction,
                    animationSpec = tween(FirstRunStepSlideMillis)
                ) + fadeIn(animationSpec = tween(FirstRunStepEnterFadeMillis))
                ).togetherWith(
                slideOutOfContainer(
                    towards = direction,
                    animationSpec = tween(FirstRunStepSlideMillis)
                ) + fadeOut(animationSpec = tween(FirstRunStepExitFadeMillis))
            ).using(SizeTransform(clip = false))
        },
        label = "first-run-step-transition"
    ) { step ->
        content(step)
    }
}

@Composable
private fun AnimatedSatellitePickerHost(
    satellitePickerOpen: Boolean,
    content: @Composable (Boolean) -> Unit
) {
    AnimatedContent(
        targetState = satellitePickerOpen,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            val direction = if (targetState) {
                AnimatedContentTransitionScope.SlideDirection.Left
            } else {
                AnimatedContentTransitionScope.SlideDirection.Right
            }
            (
                slideIntoContainer(
                    towards = direction,
                    animationSpec = tween(FirstRunStepSlideMillis)
                ) + fadeIn(animationSpec = tween(FirstRunStepEnterFadeMillis))
                ).togetherWith(
                slideOutOfContainer(
                    towards = direction,
                    animationSpec = tween(FirstRunStepSlideMillis)
                ) + fadeOut(animationSpec = tween(FirstRunStepExitFadeMillis))
            ).using(SizeTransform(clip = false))
        },
        label = "first-run-satellite-picker-transition"
    ) { pickerOpen ->
        content(pickerOpen)
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    WizardVisualStepPage(
        progress = firstRunProgress(step = 1),
        title = stringResource(R.string.first_run_welcome_title),
        heroIcon = Icons.Rounded.Public,
        heroSize = 84.dp,
        heroIconSize = 44.dp,
        actionContentDescription = stringResource(R.string.first_run_continue),
        actionIcon = Icons.AutoMirrored.Rounded.ArrowForward,
        onAction = onNext
    )
}

@Composable
private fun TleStep(
    state: WatchUiState,
    skipped: Boolean,
    onRefresh: () -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit
) {
    val dataReady = FirstRunSetupPolicy.hasUsableTleData(state.satellites.size)
    val hasFailure = !dataReady &&
        !state.refreshInFlight &&
        state.settings.satelliteDataRefreshStatus() == DataRefreshAttemptStatus.Failed
    SetupStepPage(
        progress = firstRunProgress(step = 2),
        title = stringResource(R.string.update_tle),
        subtitle = if (dataReady) {
            null
        } else {
            stringResource(R.string.first_run_tle_subtitle)
        },
        bottomSpacerHeight = if (dataReady) 0.dp else 12.dp,
        edgeButton = if (dataReady || skipped) {
            { listState ->
                FirstRunEdgeButton(
                    contentDescription = stringResource(R.string.first_run_continue),
                    listState = listState,
                    onClick = onNext
                )
            }
        } else {
            null
        }
    ) {
        when {
            dataReady -> {
                item {
                    WizardHeroIcon(heroIcon = Icons.Rounded.Check)
                }
                item {
                    Text(
                        text = stringResource(R.string.first_run_tle_data_ready),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            hasFailure || state.refreshInFlight || skipped -> {
                item {
                    StatusTextBlock(
                        title = when {
                            hasFailure -> stringResource(R.string.first_run_tle_update_failed)
                            state.refreshInFlight -> stringResource(R.string.first_run_tle_updating)
                            else -> stringResource(R.string.first_run_skipped)
                        },
                        subtitle = when {
                            hasFailure -> stringResource(R.string.first_run_tle_retry_or_skip)
                            state.refreshInFlight -> state.refreshMessage
                            else -> stringResource(R.string.first_run_tle_update_later)
                        }
                    )
                }
            }
        }
        if (!dataReady) {
            item {
                RoundAction(
                    label = if (hasFailure) {
                        stringResource(R.string.first_run_retry)
                    } else {
                        stringResource(R.string.first_run_update)
                    },
                    icon = if (hasFailure) Icons.Rounded.Refresh else Icons.Rounded.CloudDownload,
                    enabled = !state.refreshInFlight,
                    itemScope = this,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onRefresh
                )
            }
            item {
                RoundAction(
                    label = stringResource(R.string.first_run_skip),
                    icon = Icons.AutoMirrored.Rounded.ArrowForward,
                    itemScope = this,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onSkip
                )
            }
        }
    }
}

@Composable
private fun QthStep(
    state: WatchUiState,
    skipped: Boolean,
    onRequestGps: () -> Unit,
    onCancelGps: () -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit
) {
    val gpsStatus = QthGpsStatusPolicy.statusMessage(
        locationMessage = state.locationMessage,
        gpsRequestInFlight = state.gpsRequestInFlight,
        kind = state.locationStatusKind
    )
    val actions = FirstRunSetupPolicy.qthActions(
        hasStationLocation = state.hasStationLocation,
        skipped = skipped,
        gpsRequestInFlight = state.gpsRequestInFlight,
        locationStatusKind = state.locationStatusKind
    )
    SetupStepPage(
        progress = firstRunProgress(step = 3),
        title = stringResource(R.string.first_run_qth_title),
        subtitle = if (state.hasStationLocation) {
            state.station.qthLocator ?: stringResource(R.string.first_run_qth_location_saved)
        } else {
            stringResource(R.string.first_run_qth_not_set)
        },
        edgeButton = if (actions.showContinue) {
            { listState ->
                FirstRunEdgeButton(
                    contentDescription = stringResource(R.string.first_run_continue),
                    listState = listState,
                    onClick = onNext
                )
            }
        } else {
            null
        }
    ) {
        if (actions.showUseGps) {
            item {
                RoundAction(
                    label = if (state.gpsRequestInFlight) {
                        stringResource(R.string.cancel)
                    } else {
                        stringResource(R.string.use_gps)
                    },
                    icon = Icons.Rounded.GpsFixed,
                    itemScope = this,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = if (state.gpsRequestInFlight) onCancelGps else onRequestGps
                )
            }
        }
        if (actions.showGpsStatus) {
            item {
                Text(
                    gpsStatus.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (QthGpsStatusPolicy.colorRoleFor(gpsStatus.kind)) {
                        QthGpsStatusPolicy.errorColorRole -> WatchSemanticColors.ErrorForeground
                        QthGpsStatusPolicy.successColorRole -> LocalWatchThemeColors.current.primary
                        else -> LocalWatchThemeColors.current.mutedText
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (actions.showSkip) {
            item {
                RoundAction(
                    label = stringResource(R.string.first_run_qth_skip_for_now),
                    icon = Icons.AutoMirrored.Rounded.ArrowForward,
                    itemScope = this,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onSkip
                )
            }
        }
    }
}

@Composable
private fun SatellitesStep(
    state: WatchUiState,
    skipped: Boolean,
    satellitePickerOpen: Boolean,
    onSatellitePickerOpenChange: (Boolean) -> Unit,
    onRefreshTle: () -> Unit,
    onApplyStarterSelection: () -> Unit,
    onToggleSatellite: (Int) -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit
) {
    val hasTle = FirstRunSetupPolicy.hasUsableTleData(state.satellites.size)
    val actions = FirstRunSetupPolicy.satelliteActions(
        hasTle = hasTle,
        selectedSatelliteCount = state.selectedSatelliteCount,
        skipped = skipped
    )
    AnimatedSatellitePickerHost(satellitePickerOpen = satellitePickerOpen) { pickerOpen ->
        if (pickerOpen) {
            SetupStepPage(
                progress = firstRunProgress(step = 4),
                title = stringResource(R.string.first_run_satellites_title),
                subtitle = pluralStringResource(
                    R.plurals.first_run_satellites_selected,
                    state.selectedSatelliteCount,
                    state.selectedSatelliteCount
                ),
                edgeButton = { listState ->
                    FirstRunEdgeButton(
                        contentDescription = stringResource(
                            R.string.first_run_satellites_back_to_setup
                        ),
                        listState = listState,
                        onClick = { onSatellitePickerOpenChange(false) }
                    )
                }
            ) {
                state.satellites.forEach { satellite ->
                    item(key = "setup-picker-sat-${satellite.catalogNumber}") {
                        SatelliteSelectRow(
                            satellite = satellite,
                            selected = state.selectedSatelliteIds.contains(satellite.catalogNumber),
                            itemScope = this,
                            onCheckedChange = { onToggleSatellite(satellite.catalogNumber) }
                        )
                    }
                }
            }
        } else {
            SetupStepPage(
                progress = firstRunProgress(step = 4),
                title = stringResource(R.string.first_run_satellites_title),
                subtitle = if (state.selectedSatelliteCount > 0) {
                    pluralStringResource(
                        R.plurals.first_run_satellites_selected,
                        state.selectedSatelliteCount,
                        state.selectedSatelliteCount
                    )
                } else {
                    stringResource(R.string.first_run_satellites_select_to_track)
                },
                bottomSpacerHeight = if (state.selectedSatelliteCount > 0) 0.dp else 12.dp,
                edgeButton = if (actions.showContinue) {
                    { listState ->
                        FirstRunEdgeButton(
                            contentDescription = stringResource(R.string.first_run_continue),
                            listState = listState,
                            onClick = onNext
                        )
                    }
                } else {
                    null
                }
            ) {
                if (!hasTle) {
                    item {
                        StatusTextBlock(
                            title = stringResource(R.string.first_run_satellites_no_data),
                            subtitle = stringResource(
                                R.string.first_run_satellites_no_data_guidance
                            )
                        )
                    }
                    item {
                        RoundAction(
                            label = stringResource(R.string.update_tle),
                            icon = Icons.Rounded.CloudDownload,
                            enabled = !state.refreshInFlight,
                            itemScope = this,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onRefreshTle
                        )
                    }
                    if (actions.showSkip) {
                        item {
                            RoundAction(
                                label = stringResource(R.string.first_run_skip),
                                icon = Icons.AutoMirrored.Rounded.ArrowForward,
                                itemScope = this,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = onSkip
                            )
                        }
                    }
                    return@SetupStepPage
                }
                if (state.selectedSatelliteCount > 0) {
                    item {
                        WizardHeroIcon(heroIcon = Icons.Rounded.Check)
                    }
                }
                if (actions.showStarter) {
                    item {
                        RoundAction(
                            label = stringResource(R.string.first_run_satellites_use_starter_picks),
                            icon = Icons.Rounded.Star,
                            itemScope = this,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onApplyStarterSelection
                        )
                    }
                }
                if (actions.showReview) {
                    item {
                        RoundAction(
                            label = stringResource(R.string.first_run_satellites_pick_myself),
                            icon = Icons.Rounded.Public,
                            itemScope = this,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onSatellitePickerOpenChange(true) }
                        )
                    }
                }
                if (actions.showSkip) {
                    item {
                        RoundAction(
                            label = stringResource(R.string.first_run_skip),
                            icon = Icons.AutoMirrored.Rounded.ArrowForward,
                            itemScope = this,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onSkip
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionsStep(
    notificationPermissionGranted: Boolean,
    exactAlarmAvailable: Boolean,
    onRequestNotifications: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit
) {
    val actions = FirstRunSetupPolicy.permissionActions(
        notificationPermissionGranted = notificationPermissionGranted,
        exactAlarmAvailable = exactAlarmAvailable,
        exactAlarmSettingsAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    )
    SetupStepPage(
        progress = firstRunProgress(step = 5),
        title = stringResource(R.string.first_run_alerts_title),
        edgeButton = if (actions.showContinue) {
            { listState ->
                FirstRunEdgeButton(
                    contentDescription = stringResource(R.string.first_run_continue),
                    listState = listState,
                    onClick = onNext
                )
            }
        } else {
            null
        }
    ) {
        item {
            StatusTextBlock(
                title = if (actions.showContinue) {
                    stringResource(R.string.first_run_alerts_ready)
                } else {
                    stringResource(R.string.first_run_notifications)
                },
                subtitle = if (actions.showContinue) {
                    stringResource(R.string.first_run_alerts_ready_summary)
                } else {
                    stringResource(R.string.first_run_alerts_enable_summary)
                }
            )
        }
        if (actions.showNotificationAction) {
            item {
                RoundAction(
                    label = stringResource(R.string.first_run_enable),
                    icon = Icons.Rounded.NotificationsActive,
                    itemScope = this,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onRequestNotifications
                )
            }
        }
        if (!exactAlarmAvailable) {
            item {
                InfoCard(
                    title = stringResource(R.string.first_run_alarm_precision),
                    subtitle = stringResource(R.string.first_run_alarm_precision_summary),
                    itemScope = this,
                    onClick = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        onOpenExactAlarmSettings
                    } else {
                        null
                    }
                )
            }
        }
        if (actions.showAlarmAction) {
            item {
                RoundAction(
                    label = stringResource(R.string.first_run_alarm_settings),
                    icon = Icons.Rounded.Alarm,
                    itemScope = this,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenExactAlarmSettings
                )
            }
        }
        if (actions.showSkip) {
            item {
                RoundAction(
                    label = stringResource(R.string.first_run_alerts_skip),
                    icon = Icons.AutoMirrored.Rounded.ArrowForward,
                    itemScope = this,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onSkip
                )
            }
        }
    }
}

@Composable
private fun FinishStep(
    enabled: Boolean,
    onFinish: () -> Unit
) {
    WizardVisualStepPage(
        progress = firstRunProgress(step = 6),
        title = stringResource(R.string.first_run_finish_title),
        heroIcon = Icons.Rounded.Check,
        heroSize = 72.dp,
        heroIconSize = 36.dp,
        titleStyle = MaterialTheme.typography.titleMedium,
        actionContentDescription = stringResource(R.string.first_run_finish_enter_app),
        actionIcon = Icons.AutoMirrored.Rounded.ArrowForward,
        actionEnabled = enabled,
        onAction = onFinish
    )
}

@Composable
private fun SatelliteSelectRow(
    satellite: SatelliteRecord,
    selected: Boolean,
    itemScope: TransformingLazyColumnItemScope?,
    onCheckedChange: () -> Unit
) {
    val colors = LocalWatchThemeColors.current
    val contentColor = MaterialTheme.colorScheme.onSurface
    val toggleContentDescription = stringResource(
        if (selected) {
            R.string.first_run_satellite_unselect
        } else {
            R.string.first_run_satellite_select
        }
    )
    SplitCheckboxButton(
        checked = selected,
        onCheckedChange = { onCheckedChange() },
        toggleContentDescription = toggleContentDescription,
        onContainerClick = { onCheckedChange() },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = WatchUiMetrics.ActionButtonHeight)
            .roundListTransformedHeight(itemScope, RoundListSurface.SPLIT_CHECKBOX_BUTTON)
            .semantics { contentDescription = satellite.displayName },
        colors = CheckboxButtonDefaults.splitCheckboxButtonColors(
            checkedContainerColor = colors.surfaceVariant,
            checkedContentColor = contentColor,
            checkedSecondaryContentColor = colors.mutedText,
            checkedSplitContainerColor = colors.surface,
            checkedBoxColor = colors.primary,
            checkedCheckmarkColor = colors.onPrimary,
            uncheckedContainerColor = colors.surface,
            uncheckedContentColor = contentColor,
            uncheckedSecondaryContentColor = colors.mutedText,
            uncheckedSplitContainerColor = colors.surfaceVariant,
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
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        },
        secondaryLabel = {
            Text(
                stringResource(
                    R.string.first_run_satellite_catalog_number,
                    satellite.catalogNumber
                ),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}

@Composable
private fun firstRunProgress(step: Int): String {
    return stringResource(
        R.string.first_run_progress,
        step,
        FirstRunSetupStep.entries.size
    )
}

@Composable
private fun SetupStepPage(
    progress: String,
    title: String,
    subtitle: String? = null,
    bottomSpacerHeight: Dp = 12.dp,
    edgeButton: (@Composable (TransformingLazyColumnState) -> Unit)? = null,
    content: TransformingLazyColumnScope.() -> Unit
) {
    val listState = rememberTransformingLazyColumnState()
    val colors = LocalWatchThemeColors.current
    val showTimeText by remember(listState) {
        derivedStateOf { !listState.canScrollBackward }
    }
    ReportTimeTextVisibility(showTimeText)

    @Composable
    fun PageContent(contentPadding: PaddingValues) {
        RoundListTransformationProvider {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.appBackground)
            ) {
                val horizontalPadding = WatchUiMetrics.roundListHorizontalPadding(maxWidth)
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
                    item(key = "setup-progress-$progress") {
                        WizardStepProgressLabel(
                            progress = progress,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item(key = "setup-title-$title") {
                        Text(
                            text = title,
                            style = if (title.length > 18) {
                                MaterialTheme.typography.titleMedium
                            } else {
                                MaterialTheme.typography.titleLarge
                            },
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (subtitle != null) {
                        item(key = "setup-subtitle-$title-$subtitle") {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = LocalWatchThemeColors.current.mutedText,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    content()
                    if (bottomSpacerHeight > 0.dp) {
                        item { Spacer(Modifier.height(bottomSpacerHeight)) }
                    }
                }
            }
        }
    }

    if (edgeButton == null) {
        ScreenScaffold(
            scrollState = listState,
            scrollIndicator = { WearScrollIndicator(state = listState) }
        ) { contentPadding ->
            PageContent(contentPadding)
        }
    } else {
        ScreenScaffold(
            scrollState = listState,
            edgeButton = { edgeButton(listState) },
            scrollIndicator = { WearScrollIndicator(state = listState) }
        ) { contentPadding ->
            PageContent(contentPadding)
        }
    }
}

@Composable
private fun WizardVisualStepPage(
    progress: String,
    title: String,
    heroIcon: ImageVector,
    heroSize: Dp = 56.dp,
    heroIconSize: Dp = 28.dp,
    titleStyle: TextStyle = MaterialTheme.typography.titleSmall,
    body: String? = null,
    actionContentDescription: String,
    actionIcon: ImageVector,
    actionEnabled: Boolean = true,
    onAction: () -> Unit
) {
    ReportTimeTextVisibility(true)
    val listState = rememberTransformingLazyColumnState()
    val colors = LocalWatchThemeColors.current

    ScreenScaffold(
        scrollState = listState,
        edgeButton = {
            FirstRunEdgeButton(
                contentDescription = actionContentDescription,
                listState = listState,
                icon = actionIcon,
                enabled = actionEnabled,
                onClick = onAction
            )
        },
        scrollIndicator = {}
    ) { contentPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.appBackground)
        ) {
            val horizontalPadding = WatchUiMetrics.roundListHorizontalPadding(maxWidth)
            TransformingLazyColumn(
                state = listState,
                contentPadding = contentPadding,
                rotaryScrollableBehavior = RotaryScrollableDefaults.behavior(scrollableState = listState),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding)
            ) {
                item(key = "visual-progress-$progress") {
                    WizardStepProgressLabel(progress = progress, modifier = Modifier.fillMaxWidth())
                }
                item(key = "visual-hero-$progress") {
                    WizardHeroIcon(
                        heroIcon = heroIcon,
                        size = heroSize,
                        iconSize = heroIconSize
                    )
                }
                item(key = "visual-title-$title") {
                    Text(
                        text = title,
                        style = titleStyle,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (body != null) {
                    item(key = "visual-body-$progress") {
                        Text(
                            text = body,
                            style = MaterialTheme.typography.bodyExtraSmall,
                            color = LocalWatchThemeColors.current.mutedText,
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WizardStepProgressLabel(
    progress: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = progress,
        style = MaterialTheme.typography.labelSmall,
        color = LocalWatchThemeColors.current.mutedText,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
private fun WizardHeroIcon(
    heroIcon: ImageVector,
    size: Dp = 56.dp,
    iconSize: Dp = 28.dp
) {
    val colors = LocalWatchThemeColors.current
    Box(
        modifier = Modifier
            .size(size)
            .background(
                color = colors.primary.copy(alpha = 0.22f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = heroIcon,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun FirstRunEdgeButton(
    contentDescription: String,
    listState: TransformingLazyColumnState,
    icon: ImageVector = Icons.AutoMirrored.Rounded.ArrowForward,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val colors = LocalWatchThemeColors.current
    val overscroll = rememberOverscrollEffect()
    EdgeButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .semantics { this.contentDescription = contentDescription }
            .scrollable(
                state = listState,
                orientation = Orientation.Vertical,
                reverseDirection = true,
                overscrollEffect = overscroll
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            disabledContainerColor = colors.surfaceVariant.copy(alpha = 0.62f),
            disabledContentColor = colors.mutedText.copy(alpha = 0.64f)
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
    }
}
