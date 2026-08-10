package com.xianming.watch4sat.wear.state

object DataRefreshDialogPolicy {
    const val successDialogComponent: String = "SuccessConfirmationDialog"
    const val failureDialogComponent: String = "FailureConfirmationDialog"
    const val successContentComponent: String = "SuccessConfirmationDialogContent"
    const val failureContentComponent: String = "FailureConfirmationDialogContent"
    const val successUsesThemeColors: Boolean = true
    const val failureUsesThemeColors: Boolean = false
    const val failureColorRole: String = "SemanticError"
    const val confirmationContentLayout: String = "IconWithCurvedText"
    const val textPlacement: String = "curvedText"
    const val successAutoDismissMillis: Long = 1_000L
    const val failureAutoDismissMillis: Long = 1_000L

    fun shouldShowSuccess(message: String): Boolean {
        return message.startsWith("Saved ") && " TLE" in message && " TX" in message
    }

    fun shouldShowFailure(message: String): Boolean {
        return message.isNotBlank() &&
            !shouldShowSuccess(message) &&
            message != "Ready" &&
            message != "Refreshing..."
    }

    fun shouldShowSuccessEvent(
        message: String,
        currentEventId: Long,
        lastShownEventId: Long,
        setupActive: Boolean = false
    ): Boolean {
        return !setupActive &&
            currentEventId > lastShownEventId &&
            shouldShowSuccess(message)
    }

    fun shouldShowFailureEvent(
        message: String,
        currentEventId: Long,
        lastShownEventId: Long
    ): Boolean {
        return currentEventId > lastShownEventId && shouldShowFailure(message)
    }

    fun failureKind(message: String): DataRefreshFailureKind {
        val normalized = message.lowercase()
        return when {
            "timeout" in normalized || "timed out" in normalized -> DataRefreshFailureKind.Timeout
            "unable to resolve host" in normalized ||
                "unknownhost" in normalized ||
                "no address associated" in normalized ||
                "network unavailable" in normalized ||
                "no network" in normalized -> DataRefreshFailureKind.NoNetwork
            "http" in normalized ||
                "api" in normalized ||
                "response code" in normalized ||
                "server error" in normalized -> DataRefreshFailureKind.HttpApi
            "parse" in normalized ||
                "json" in normalized ||
                "csv" in normalized ||
                "tle" in normalized ||
                "malformed" in normalized -> DataRefreshFailureKind.Parse
            else -> DataRefreshFailureKind.Unknown
        }
    }
}

enum class DataRefreshFailureKind {
    Timeout,
    NoNetwork,
    HttpApi,
    Parse,
    Unknown
}

object DataRefreshIndicatorPolicy {
    const val component: String = "None"
    const val mode: String = "TextFeedbackOnly"
    const val placement: String = "NoSpinner"
    const val paddingSource: String = "None"
    const val followsThemeColors: Boolean = false
    const val isInlineListItem: Boolean = false

    fun visible(refreshInFlight: Boolean): Boolean = false
}
