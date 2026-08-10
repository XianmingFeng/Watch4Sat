package com.xianming.watch4sat.location

import android.util.Log

object LocationDiagnostics {
    private const val Tag = "Watch4SatGps"

    fun log(message: String) {
        runCatching {
            Log.i(Tag, message)
        }
    }

    fun sessionMessage(event: String, elapsedMillis: Long, timeoutMillis: Long, state: LocationResultState?): String {
        return "event=$event elapsedMs=$elapsedMillis timeoutMs=$timeoutMillis state=${state?.name ?: "running"}"
    }

    fun phaseMessage(phase: String, event: String): String {
        return "phase=$phase event=$event"
    }

    fun providerStateMessage(
        hasGooglePlayServicesPackage: Boolean,
        googlePlayServicesAvailable: Boolean,
        systemLocationEnabled: Boolean,
        allProviders: List<String>,
        enabledProviders: List<String>
    ): String {
        return "gmsPackage=$hasGooglePlayServicesPackage " +
            "gmsAvailable=$googlePlayServicesAvailable " +
            "systemLocationEnabled=$systemLocationEnabled " +
            "allProviders=${allProviders.joinToString(",")} " +
            "enabledProviders=${enabledProviders.joinToString(",")}"
    }

    fun activeRegistrationMessage(providers: List<String>, event: String): String {
        return "phase=active-updates event=$event providers=${providers.joinToString(",")}"
    }

    fun fixCandidateMessage(
        phase: String,
        fix: LocationFix,
        nowElapsedRealtimeMillis: Long,
        accepted: Boolean,
        reason: String
    ): String {
        val ageMillis = (nowElapsedRealtimeMillis - fix.elapsedRealtimeMillis).coerceAtLeast(0L)
        return "phase=$phase " +
            "provider=${fix.provider.name} " +
            "ageMs=$ageMillis " +
            "accuracyM=${fix.accuracyMeters?.toString() ?: "unknown"} " +
            "accepted=$accepted " +
            "reason=$reason"
    }
}
