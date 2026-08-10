package com.xianming.watch4sat.wear.state

import com.xianming.watch4sat.domain.freshness.TleFreshnessAssessment
import com.xianming.watch4sat.domain.freshness.TleFreshnessSeverity

data class TleFreshnessUiModel(
    val kind: TleFreshnessUiKind,
    val retrievalAge: TleRelativeAge?,
    val oldestEpochAge: TleRelativeAge?
)

enum class TleFreshnessUiKind {
    ClockSkew,
    Unknown,
    Fresh,
    Stale,
    VeryStale
}

sealed interface TleRelativeAge {
    data object Unknown : TleRelativeAge
    data object JustNow : TleRelativeAge
    data class Minutes(val value: Long) : TleRelativeAge
    data class Hours(val value: Long) : TleRelativeAge
    data class Days(val value: Long) : TleRelativeAge
}

object TleFreshnessUiPolicy {
    fun model(assessment: TleFreshnessAssessment): TleFreshnessUiModel {
        if (assessment.clockSkewDetected) {
            return TleFreshnessUiModel(
                kind = TleFreshnessUiKind.ClockSkew,
                retrievalAge = age(assessment.retrievalAgeMillis),
                oldestEpochAge = age(assessment.oldestEpochAgeMillis)
            )
        }
        if (assessment.metadataMissing) {
            return TleFreshnessUiModel(
                kind = TleFreshnessUiKind.Unknown,
                retrievalAge = null,
                oldestEpochAge = age(assessment.oldestEpochAgeMillis)
            )
        }
        val kind = when (assessment.severity) {
            TleFreshnessSeverity.FRESH -> TleFreshnessUiKind.Fresh
            TleFreshnessSeverity.STALE -> TleFreshnessUiKind.Stale
            TleFreshnessSeverity.VERY_STALE -> TleFreshnessUiKind.VeryStale
        }
        return TleFreshnessUiModel(
            kind = kind,
            retrievalAge = age(assessment.retrievalAgeMillis),
            oldestEpochAge = age(assessment.oldestEpochAgeMillis)
        )
    }

    fun age(ageMillis: Long?): TleRelativeAge {
        val age = ageMillis ?: return TleRelativeAge.Unknown
        val minutes = age / 60_000L
        return when {
            minutes < 1L -> TleRelativeAge.JustNow
            minutes < 60L -> TleRelativeAge.Minutes(minutes)
            minutes < 24L * 60L -> TleRelativeAge.Hours(minutes / 60L)
            else -> TleRelativeAge.Days(minutes / (24L * 60L))
        }
    }
}
