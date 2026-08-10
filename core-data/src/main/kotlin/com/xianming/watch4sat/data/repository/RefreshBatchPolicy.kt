package com.xianming.watch4sat.data.repository

import com.xianming.watch4sat.domain.model.SatelliteRecord
import com.xianming.watch4sat.domain.model.TransmitterRecord
import com.xianming.watch4sat.domain.parser.FeedParseResult

sealed interface BatchAcceptance {
    data object Accepted : BatchAcceptance

    data class Rejected(val reasons: List<String>) : BatchAcceptance
}

object RefreshBatchPolicy {
    const val MinimumInitialTleRecordCount: Int = 20
    const val MinimumInitialTransmitterRecordCount: Int = 20

    private const val MinimumRetentionPercent: Int = 80
    private const val MaximumTransmitterRejectionPercent: Int = 5

    fun evaluateTle(
        result: FeedParseResult<SatelliteRecord>,
        previousAcceptedCount: Int
    ): BatchAcceptance {
        val reasons = commonReasons(result)
        if (result.rejectedRecordCount > 0) {
            reasons += "TLE response contains ${result.rejectedRecordCount} invalid data records."
        }
        if (previousAcceptedCount == 0) {
            if (result.acceptedRecordCount < MinimumInitialTleRecordCount) {
                reasons += "First TLE refresh requires at least $MinimumInitialTleRecordCount records."
            }
        } else if (!retainsEnough(result.acceptedRecordCount, previousAcceptedCount)) {
            reasons += "TLE response contains fewer than $MinimumRetentionPercent% of cached records."
        }
        return reasons.toAcceptance()
    }

    fun evaluateTransmitters(
        result: FeedParseResult<TransmitterRecord>,
        previousAcceptedCount: Int
    ): BatchAcceptance {
        val reasons = commonReasons(result)
        if (
            result.inputRecordCount > 0 &&
            result.rejectedRecordCount * 100 >
            result.inputRecordCount * MaximumTransmitterRejectionPercent
        ) {
            reasons += "SatNOGS response exceeds the $MaximumTransmitterRejectionPercent% invalid-record limit."
        }
        if (previousAcceptedCount == 0) {
            if (result.acceptedRecordCount < MinimumInitialTransmitterRecordCount) {
                reasons += "First SatNOGS refresh requires at least $MinimumInitialTransmitterRecordCount active records."
            }
        } else if (!retainsEnough(result.acceptedRecordCount, previousAcceptedCount)) {
            reasons += "SatNOGS response contains fewer than $MinimumRetentionPercent% of cached records."
        }
        return reasons.toAcceptance()
    }

    private fun commonReasons(result: FeedParseResult<*>): MutableList<String> {
        return buildList {
            addAll(result.syntaxErrors)
            if (result.duplicateStableIds.isNotEmpty()) {
                add("Response contains duplicate stable IDs: ${result.duplicateStableIds.sorted().joinToString()}.")
            }
            if (result.acceptedRecordCount == 0) {
                add("Response contains no accepted records.")
            }
        }.toMutableList()
    }

    private fun retainsEnough(accepted: Int, previous: Int): Boolean {
        return accepted.toLong() * 100L >= previous.toLong() * MinimumRetentionPercent
    }

    private fun List<String>.toAcceptance(): BatchAcceptance {
        return if (isEmpty()) BatchAcceptance.Accepted else BatchAcceptance.Rejected(distinct())
    }
}

class RefreshBatchRejectedException(
    source: String,
    reasons: List<String>
) : IllegalArgumentException("$source refresh rejected: ${reasons.joinToString(" ")}")
