package com.xianming.watch4sat.wear.state

data class QthGpsRequestDecision(
    val shouldStart: Boolean,
    val status: QthGpsRequestStatus,
    val requestGeneration: Long
)

enum class QthGpsRequestStatus {
    AlreadyRunning,
    WaitingForFix
}

object QthGpsRequestPolicy {
    fun startRequest(
        gpsRequestInFlight: Boolean,
        currentGeneration: Long
    ): QthGpsRequestDecision {
        return if (gpsRequestInFlight) {
            QthGpsRequestDecision(
                shouldStart = false,
                status = QthGpsRequestStatus.AlreadyRunning,
                requestGeneration = currentGeneration
            )
        } else {
            val nextGeneration = currentGeneration + 1L
            QthGpsRequestDecision(
                shouldStart = true,
                status = QthGpsRequestStatus.WaitingForFix,
                requestGeneration = nextGeneration
            )
        }
    }

    fun shouldApplyResult(
        requestGeneration: Long,
        currentGeneration: Long
    ): Boolean {
        return requestGeneration == currentGeneration
    }

    fun invalidateRequest(currentGeneration: Long): Long {
        return currentGeneration + 1L
    }
}
