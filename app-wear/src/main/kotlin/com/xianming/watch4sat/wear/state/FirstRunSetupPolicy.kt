package com.xianming.watch4sat.wear.state

object FirstRunSetupPolicy {

    fun hasUsableTleData(satelliteCount: Int): Boolean = satelliteCount > 0

    fun decision(
        setupCompleted: Boolean,
        storedStep: String,
        skippedSteps: Set<String> = emptySet(),
        hasStationLocation: Boolean,
        hasTleData: Boolean,
        selectedSatelliteCount: Int,
        notificationsAvailable: Boolean,
        exactAlarmAvailable: Boolean
    ): FirstRunSetupDecision {
        val stepStatuses = FirstRunSetupStep.entries.associateWith { step ->
            statusFor(
                step = step,
                skippedSteps = skippedSteps,
                hasStationLocation = hasStationLocation,
                hasTleData = hasTleData,
                selectedSatelliteCount = selectedSatelliteCount,
                notificationsAvailable = notificationsAvailable,
                exactAlarmAvailable = exactAlarmAvailable
            )
        }
        val blockingRequirements = buildList {
            if (stepStatuses[FirstRunSetupStep.Qth] == FirstRunSetupStepStatus.UNRESOLVED) {
                add(FirstRunSetupRequirement.Qth)
            }
            if (stepStatuses[FirstRunSetupStep.Data] == FirstRunSetupStepStatus.UNRESOLVED) {
                add(FirstRunSetupRequirement.Tle)
            }
            if (stepStatuses[FirstRunSetupStep.Satellites] == FirstRunSetupStepStatus.UNRESOLVED) {
                add(FirstRunSetupRequirement.Satellites)
            }
        }
        val advisoryRequirements = buildList {
            if (!notificationsAvailable) add(FirstRunSetupRequirement.Notifications)
            if (!exactAlarmAvailable) add(FirstRunSetupRequirement.ExactAlarm)
        }
        val stored = FirstRunSetupStep.fromStoredName(storedStep)
        val activeStep = if (setupCompleted) {
            FirstRunSetupStep.Done
        } else {
            nearestReachableStep(stored, stepStatuses)
        }
        val canMarkComplete = RequiredSetupSteps.all { step ->
            stepStatuses[step]?.isResolved == true
        }
        return FirstRunSetupDecision(
            shouldShowSetup = !setupCompleted,
            canMarkComplete = canMarkComplete,
            step = activeStep,
            blockingRequirements = blockingRequirements,
            advisoryRequirements = advisoryRequirements,
            stepStatuses = stepStatuses
        )
    }

    fun qthActions(
        hasStationLocation: Boolean,
        skipped: Boolean,
        gpsRequestInFlight: Boolean,
        locationStatusKind: QthGpsStatusKind
    ): FirstRunQthActions {
        val completeOrSkipped = hasStationLocation || skipped
        return FirstRunQthActions(
            showContinue = completeOrSkipped,
            showUseGps = !completeOrSkipped,
            showSkip = !completeOrSkipped,
            showGpsStatus = !completeOrSkipped &&
                QthGpsStatusPolicy.shouldShowFirstRunStatus(
                    kind = locationStatusKind,
                    gpsRequestInFlight = gpsRequestInFlight
                )
        )
    }

    fun satelliteActions(
        hasTle: Boolean,
        selectedSatelliteCount: Int,
        skipped: Boolean
    ): FirstRunSatelliteActions {
        if (!hasTle) {
            return FirstRunSatelliteActions(
                showContinue = skipped,
                showStarter = false,
                showReview = false,
                showSkip = !skipped
            )
        }
        val hasSelection = selectedSatelliteCount > 0
        return FirstRunSatelliteActions(
            showContinue = hasSelection || skipped,
            showStarter = !hasSelection,
            showReview = !hasSelection,
            showSkip = !hasSelection && !skipped
        )
    }

    fun permissionActions(
        notificationPermissionGranted: Boolean,
        exactAlarmAvailable: Boolean,
        exactAlarmSettingsAvailable: Boolean
    ): FirstRunPermissionActions {
        val allGranted = notificationPermissionGranted && exactAlarmAvailable
        return FirstRunPermissionActions(
            showContinue = allGranted,
            showNotificationAction = !notificationPermissionGranted,
            showAlarmAction = exactAlarmSettingsAvailable && !exactAlarmAvailable,
            showSkip = !allGranted
        )
    }

    private fun nearestReachableStep(
        step: FirstRunSetupStep?,
        stepStatuses: Map<FirstRunSetupStep, FirstRunSetupStepStatus>
    ): FirstRunSetupStep {
        val requestedStep = step ?: return FirstRunSetupStep.Welcome
        return FirstRunSetupStep.entries
            .asSequence()
            .take(FirstRunSetupStep.entries.indexOf(requestedStep) + 1)
            .filter { candidate -> isReachableStep(candidate, stepStatuses) }
            .lastOrNull()
            ?: FirstRunSetupStep.Welcome
    }

    private fun isReachableStep(
        step: FirstRunSetupStep,
        stepStatuses: Map<FirstRunSetupStep, FirstRunSetupStepStatus>
    ): Boolean {
        fun resolved(requiredStep: FirstRunSetupStep): Boolean {
            return stepStatuses[requiredStep]?.isResolved == true
        }
        return when (step) {
            FirstRunSetupStep.Welcome,
            FirstRunSetupStep.Data -> true
            FirstRunSetupStep.Qth -> resolved(FirstRunSetupStep.Data)
            FirstRunSetupStep.Satellites ->
                resolved(FirstRunSetupStep.Data) && resolved(FirstRunSetupStep.Qth)
            FirstRunSetupStep.Notifications,
            FirstRunSetupStep.Done -> RequiredSetupSteps.all(::resolved)
        }
    }

    private fun statusFor(
        step: FirstRunSetupStep,
        skippedSteps: Set<String>,
        hasStationLocation: Boolean,
        hasTleData: Boolean,
        selectedSatelliteCount: Int,
        notificationsAvailable: Boolean,
        exactAlarmAvailable: Boolean
    ): FirstRunSetupStepStatus {
        val ready = when (step) {
            FirstRunSetupStep.Welcome -> true
            FirstRunSetupStep.Data -> hasTleData
            FirstRunSetupStep.Qth -> hasStationLocation
            FirstRunSetupStep.Satellites -> selectedSatelliteCount > 0
            FirstRunSetupStep.Notifications -> notificationsAvailable && exactAlarmAvailable
            FirstRunSetupStep.Done -> true
        }
        return when {
            ready -> FirstRunSetupStepStatus.READY
            step.matchesStoredNames(skippedSteps) ->
                FirstRunSetupStepStatus.EXPLICITLY_SKIPPED
            else -> FirstRunSetupStepStatus.UNRESOLVED
        }
    }

    private val RequiredSetupSteps = listOf(
        FirstRunSetupStep.Data,
        FirstRunSetupStep.Qth,
        FirstRunSetupStep.Satellites
    )
}

data class FirstRunQthActions(
    val showContinue: Boolean,
    val showUseGps: Boolean,
    val showSkip: Boolean,
    val showGpsStatus: Boolean
)

data class FirstRunSatelliteActions(
    val showContinue: Boolean,
    val showStarter: Boolean,
    val showReview: Boolean,
    val showSkip: Boolean
)

data class FirstRunPermissionActions(
    val showContinue: Boolean,
    val showNotificationAction: Boolean,
    val showAlarmAction: Boolean,
    val showSkip: Boolean
)

data class FirstRunSetupDecision(
    val shouldShowSetup: Boolean,
    val canMarkComplete: Boolean,
    val step: FirstRunSetupStep,
    val blockingRequirements: List<FirstRunSetupRequirement>,
    val advisoryRequirements: List<FirstRunSetupRequirement>,
    val stepStatuses: Map<FirstRunSetupStep, FirstRunSetupStepStatus>
) {
    fun statusFor(step: FirstRunSetupStep): FirstRunSetupStepStatus {
        return stepStatuses[step] ?: FirstRunSetupStepStatus.UNRESOLVED
    }
}

enum class FirstRunSetupStep(
    val storedName: String,
    private vararg val aliases: String
) {
    Welcome("welcome"),
    Data("data", "tle"),
    Qth("qth"),
    Satellites("satellites"),
    Notifications("notifications"),
    Done("done");

    fun next(): FirstRunSetupStep {
        return when (this) {
            Welcome -> Data
            Data -> Qth
            Qth -> Satellites
            Satellites -> Notifications
            Notifications -> Done
            Done -> Done
        }
    }

    fun previous(): FirstRunSetupStep? {
        return when (this) {
            Welcome -> null
            Data -> Welcome
            Qth -> Data
            Satellites -> Qth
            Notifications -> Satellites
            Done -> Notifications
        }
    }

    fun matchesStoredNames(values: Set<String>): Boolean {
        return values.any { value -> value == storedName || value in aliases }
    }

    companion object {
        fun fromStoredName(value: String): FirstRunSetupStep? {
            return entries.firstOrNull { step ->
                step.storedName == value || value in step.aliases
            }
        }
    }
}

enum class FirstRunSetupStepStatus {
    UNRESOLVED,
    READY,
    EXPLICITLY_SKIPPED;

    val isResolved: Boolean
        get() = this != UNRESOLVED

    companion object {
        @Deprecated("Use UNRESOLVED.", ReplaceWith("UNRESOLVED"))
        val Pending: FirstRunSetupStepStatus = UNRESOLVED

        @Deprecated("Use READY.", ReplaceWith("READY"))
        val Ready: FirstRunSetupStepStatus = READY

        @Deprecated("Use EXPLICITLY_SKIPPED.", ReplaceWith("EXPLICITLY_SKIPPED"))
        val Skipped: FirstRunSetupStepStatus = EXPLICITLY_SKIPPED
    }
}

enum class FirstRunSetupRequirement {
    Qth,
    Tle,
    Satellites,
    Notifications,
    ExactAlarm
}
