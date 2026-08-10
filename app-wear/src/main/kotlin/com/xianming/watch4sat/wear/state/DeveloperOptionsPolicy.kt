package com.xianming.watch4sat.wear.state

import com.xianming.watch4sat.domain.model.PassCardUi
import com.xianming.watch4sat.domain.model.SatellitePass

enum class DebugPassAlertSource {
    ActivePass,
    ShiftedNextPass
}

enum class DeveloperOptionAction {
    TriggerPassAlert,
    TriggerPassNotification,
    TriggerCalibrationHint,
    DisableDeveloperOptions
}

enum class DeveloperOptionFailure {
    NoPass,
    NotificationPermission
}

data class PassStartAlertState(
    val alertPass: SatellitePass,
    val card: PassCardUi,
    val radarPass: SatellitePass,
    val source: DebugPassAlertSource? = null
) {
    val alertKey: String =
        "${alertPass.catalogNumber}:${alertPass.aosMillis}:${alertPass.losMillis}"
    val debugShifted: Boolean = source == DebugPassAlertSource.ShiftedNextPass
}

object DeveloperOptionsPolicy {
    const val unlockTapCount: Int = 7
    val actions: List<DeveloperOptionAction> = DeveloperOptionAction.entries
    const val debugFailureAutoDismissMillis: Long = 1_000L

    fun nextUnlockTapCount(currentTapCount: Int, alreadyEnabled: Boolean): Int {
        if (alreadyEnabled) return 0
        val boundedTapCount = currentTapCount.coerceIn(0, unlockTapCount)
        return (boundedTapCount + 1).coerceAtMost(unlockTapCount)
    }

    fun shouldEnableDeveloperOptions(currentTapCount: Int, alreadyEnabled: Boolean): Boolean {
        return !alreadyEnabled && currentTapCount >= unlockTapCount - 1
    }

    fun selectDebugPassAlert(
        passCards: List<Pair<SatellitePass, PassCardUi>>,
        nowMillis: Long
    ): PassStartAlertState? {
        val activePass = passCards.firstOrNull { (_, card) -> card.isActive }
        if (activePass != null) {
            return PassStartAlertState(
                alertPass = activePass.first,
                card = activePass.second,
                radarPass = activePass.first,
                source = DebugPassAlertSource.ActivePass
            )
        }

        val nextPass = passCards
            .filter { (pass, _) -> pass.aosMillis > nowMillis }
            .minByOrNull { (pass, _) -> pass.aosMillis }
            ?: return null
        return PassStartAlertState(
            alertPass = shiftToNow(nextPass.first, nowMillis),
            card = nextPass.second.copy(
                aosCountdown = "NOW",
                isActive = true,
                isUpcoming = false
            ),
            radarPass = nextPass.first,
            source = DebugPassAlertSource.ShiftedNextPass
        )
    }

    private fun shiftToNow(pass: SatellitePass, nowMillis: Long): SatellitePass {
        val durationMillis = pass.durationMillis.coerceAtLeast(60_000L)
        val offsetMillis = nowMillis - pass.aosMillis
        val losMillis = nowMillis + durationMillis
        val tcaMillis = (pass.tcaMillis + offsetMillis).coerceIn(nowMillis, losMillis)

        return pass.copy(
            aosMillis = nowMillis,
            losMillis = losMillis,
            tcaMillis = tcaMillis
        )
    }
}
