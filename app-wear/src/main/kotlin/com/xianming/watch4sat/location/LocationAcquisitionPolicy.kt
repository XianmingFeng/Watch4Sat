package com.xianming.watch4sat.location

object LocationAcquisitionPolicy {
    const val LastKnownReadTimeoutMillis: Long = 1_500L
    const val FusedCurrentTimeoutMillis: Long = 5_000L
    const val FusedLastKnownTimeoutMillis: Long = 1_500L
    const val FrameworkNetworkCurrentTimeoutMillis: Long = 3_000L
    const val FrameworkGpsCurrentTimeoutMillis: Long = 7_000L
    const val FusedMaxUpdateAgeMillis: Long = 2 * 60 * 1_000L
    const val FusedUpdateFallbackGraceMillis: Long = 5_000L
    const val ActiveSessionTimeoutMillis: Long = 60_000L
    const val ActiveUpdateIntervalMillis: Long = 1_000L
    const val ActiveMinDistanceMeters: Float = 0f

    const val LegacyOneShotTimeoutMillis: Long =
        LastKnownReadTimeoutMillis +
            FusedCurrentTimeoutMillis +
            FrameworkNetworkCurrentTimeoutMillis +
            FrameworkGpsCurrentTimeoutMillis +
            LastKnownReadTimeoutMillis
    const val RepositoryTimeoutMillis: Long = ActiveSessionTimeoutMillis
}
