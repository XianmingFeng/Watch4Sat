package com.xianming.watch4sat.wear.state

data class QthGpsStatusUi(
    val message: String,
    val disableGpsButton: Boolean,
    val kind: QthGpsStatusKind
)

enum class QthGpsStatusKind {
    Neutral,
    Success,
    Failure
}

object QthGpsStatusPolicy {
    const val errorColorRole: String = "SemanticError"
    const val successColorRole: String = "ThemePrimary"
    const val neutralColorRole: String = "MutedText"

    fun statusMessage(
        locationMessage: String,
        gpsRequestInFlight: Boolean,
        kind: QthGpsStatusKind
    ): QthGpsStatusUi {
        return QthGpsStatusUi(
            message = locationMessage,
            disableGpsButton = gpsRequestInFlight,
            kind = kind
        )
    }

    fun colorRoleFor(kind: QthGpsStatusKind): String {
        return when (kind) {
            QthGpsStatusKind.Failure -> errorColorRole
            QthGpsStatusKind.Success -> successColorRole
            QthGpsStatusKind.Neutral -> neutralColorRole
        }
    }

    fun shouldShowFirstRunStatus(
        kind: QthGpsStatusKind,
        gpsRequestInFlight: Boolean,
    ): Boolean {
        return gpsRequestInFlight ||
            kind != QthGpsStatusKind.Neutral
    }
}
