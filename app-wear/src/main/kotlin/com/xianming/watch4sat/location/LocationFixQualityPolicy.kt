package com.xianming.watch4sat.location

object LocationFixQualityPolicy {
    const val MAX_LAST_KNOWN_AGE_MILLIS = 10 * 60 * 1000L
    const val MAX_UNKNOWN_ACCURACY_LAST_KNOWN_AGE_MILLIS = 30 * 1000L
    const val MAX_GPS_OR_FUSED_ACCURACY_METERS = 1_000f
    const val MAX_NETWORK_OR_PASSIVE_ACCURACY_METERS = 3_000f

    fun isAcceptableLastKnown(
        fix: LocationFix,
        nowElapsedMillis: Long
    ): Boolean {
        val ageMillis = nowElapsedMillis - fix.elapsedRealtimeMillis
        if (ageMillis !in 0L..MAX_LAST_KNOWN_AGE_MILLIS) {
            return false
        }
        val accuracyMeters = fix.accuracyMeters
        if (accuracyMeters == null) {
            return fix.provider.isGpsOrFused() && ageMillis <= MAX_UNKNOWN_ACCURACY_LAST_KNOWN_AGE_MILLIS
        }
        return accuracyMeters <= fix.provider.maxAccuracyMeters
    }

    fun isAcceptableCurrent(fix: LocationFix): Boolean {
        val accuracyMeters = fix.accuracyMeters ?: return true
        return accuracyMeters <= fix.provider.maxAccuracyMeters
    }

    fun bestAcceptableLastKnown(
        fixes: List<LocationFix>,
        nowElapsedMillis: Long
    ): LocationFix? {
        return fixes
            .filter { isAcceptableLastKnown(it, nowElapsedMillis) }
            .maxWithOrNull(compareBy<LocationFix> { it.provider.priority }.thenBy { it.elapsedRealtimeMillis })
    }

    fun hasLowAccuracyFix(fixes: Iterable<LocationFix>): Boolean {
        return fixes.any { fix ->
            fix.accuracyMeters?.let { accuracyMeters ->
                accuracyMeters > fix.provider.maxAccuracyMeters
            } ?: false
        }
    }

    private val LocationFixProvider.priority: Int
        get() = when (this) {
            LocationFixProvider.FUSED -> 4
            LocationFixProvider.GPS -> 3
            LocationFixProvider.NETWORK -> 2
            LocationFixProvider.PASSIVE -> 1
        }

    private val LocationFixProvider.maxAccuracyMeters: Float
        get() = when (this) {
            LocationFixProvider.FUSED,
            LocationFixProvider.GPS -> MAX_GPS_OR_FUSED_ACCURACY_METERS
            LocationFixProvider.NETWORK,
            LocationFixProvider.PASSIVE -> MAX_NETWORK_OR_PASSIVE_ACCURACY_METERS
        }

    private fun LocationFixProvider.isGpsOrFused(): Boolean {
        return this == LocationFixProvider.GPS || this == LocationFixProvider.FUSED
    }
}
