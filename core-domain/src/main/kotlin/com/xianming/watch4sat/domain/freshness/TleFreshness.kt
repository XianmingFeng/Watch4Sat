package com.xianming.watch4sat.domain.freshness

import java.time.DateTimeException
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.floor
import kotlin.math.roundToLong

enum class TleFreshnessSeverity {
    FRESH,
    STALE,
    VERY_STALE
}

data class TleEpochSample(
    val catalogNumber: Int,
    val epoch: Double
)

data class TleFreshnessAssessment(
    val severity: TleFreshnessSeverity,
    val retrievalSeverity: TleFreshnessSeverity,
    val epochSeverity: TleFreshnessSeverity,
    val retrievedAtMillis: Long?,
    val retrievalAgeMillis: Long?,
    val oldestEpochMillis: Long?,
    val oldestEpochAgeMillis: Long?,
    val oldestEpochCatalogNumber: Int?,
    val evaluatedSatelliteCount: Int,
    val affectedSatelliteCount: Int,
    val invalidEpochCount: Int,
    val futureEpochCount: Int,
    val metadataMissing: Boolean,
    val clockSkewDetected: Boolean,
    val nextBoundaryMillis: Long?
) {
    val shouldRefresh: Boolean
        get() = retrievalSeverity != TleFreshnessSeverity.FRESH ||
            metadataMissing ||
            clockSkewDetected

    // Freshness is advisory for the RC. Cached orbital data remains usable offline.
    val allowsPrediction: Boolean
        get() = true

    val allowsAlerts: Boolean
        get() = true
}

object TleFreshnessPolicy {
    const val StaleAfterMillis: Long = 24L * 60L * 60L * 1_000L
    const val VeryStaleAfterMillis: Long = 7L * StaleAfterMillis

    fun assess(
        nowMillis: Long,
        retrievedAtMillis: Long?,
        samples: List<TleEpochSample>
    ): TleFreshnessAssessment {
        val clockSkewDetected =
            retrievedAtMillis != null && retrievedAtMillis > nowMillis
        val retrievalAgeMillis = retrievedAtMillis
            ?.takeUnless { it > nowMillis }
            ?.let { nowMillis - it }
        val retrievalSeverity = when {
            retrievedAtMillis == null -> TleFreshnessSeverity.VERY_STALE
            clockSkewDetected -> TleFreshnessSeverity.STALE
            else -> severityForAge(requireNotNull(retrievalAgeMillis))
        }

        val evaluatedEpochs = samples.map { sample ->
            val epochMillis = TleEpochConverter.toEpochMillisOrNull(sample.epoch)
            val ageMillis = epochMillis
                ?.takeUnless { it > nowMillis }
                ?.let { nowMillis - it }
            EvaluatedEpoch(
                sample = sample,
                epochMillis = epochMillis,
                ageMillis = ageMillis,
                severity = when {
                    epochMillis == null -> TleFreshnessSeverity.VERY_STALE
                    epochMillis > nowMillis -> TleFreshnessSeverity.FRESH
                    else -> severityForAge(requireNotNull(ageMillis))
                }
            )
        }
        val epochSeverity = evaluatedEpochs
            .maxOfOrNull(EvaluatedEpoch::severity)
            ?: TleFreshnessSeverity.VERY_STALE
        val severity = maxOf(retrievalSeverity, epochSeverity)
        val oldestEpoch = evaluatedEpochs
            .filter { it.epochMillis != null }
            .minByOrNull { requireNotNull(it.epochMillis) }
        val affectedSatelliteCount = when {
            samples.isEmpty() -> 0
            retrievalSeverity == severity && severity != TleFreshnessSeverity.FRESH ->
                samples.size
            else -> evaluatedEpochs.count { it.severity == severity }
        }
        val nextBoundaryMillis = buildList {
            retrievedAtMillis
                ?.takeUnless { clockSkewDetected }
                ?.nextFreshnessBoundary(nowMillis)
                ?.let(::add)
            evaluatedEpochs.forEach { evaluated ->
                evaluated.epochMillis
                    ?.nextFreshnessBoundary(nowMillis)
                    ?.let(::add)
            }
        }.minOrNull()

        return TleFreshnessAssessment(
            severity = severity,
            retrievalSeverity = retrievalSeverity,
            epochSeverity = epochSeverity,
            retrievedAtMillis = retrievedAtMillis,
            retrievalAgeMillis = retrievalAgeMillis,
            oldestEpochMillis = oldestEpoch?.epochMillis,
            oldestEpochAgeMillis = oldestEpoch?.ageMillis,
            oldestEpochCatalogNumber = oldestEpoch?.sample?.catalogNumber,
            evaluatedSatelliteCount = samples.size,
            affectedSatelliteCount = affectedSatelliteCount,
            invalidEpochCount = evaluatedEpochs.count { it.epochMillis == null },
            futureEpochCount = evaluatedEpochs.count {
                it.epochMillis != null && requireNotNull(it.epochMillis) > nowMillis
            },
            metadataMissing = retrievedAtMillis == null,
            clockSkewDetected = clockSkewDetected,
            nextBoundaryMillis = nextBoundaryMillis
        )
    }

    fun severityForAge(ageMillis: Long): TleFreshnessSeverity {
        return when {
            ageMillis >= VeryStaleAfterMillis -> TleFreshnessSeverity.VERY_STALE
            ageMillis >= StaleAfterMillis -> TleFreshnessSeverity.STALE
            else -> TleFreshnessSeverity.FRESH
        }
    }

    private data class EvaluatedEpoch(
        val sample: TleEpochSample,
        val epochMillis: Long?,
        val ageMillis: Long?,
        val severity: TleFreshnessSeverity
    )

    private fun Long.nextFreshnessBoundary(nowMillis: Long): Long? {
        val staleBoundary = saturatedAdd(StaleAfterMillis)
        val veryStaleBoundary = saturatedAdd(VeryStaleAfterMillis)
        return when {
            nowMillis < staleBoundary -> staleBoundary
            nowMillis < veryStaleBoundary -> veryStaleBoundary
            else -> null
        }
    }

    private fun Long.saturatedAdd(value: Long): Long {
        return if (this > Long.MAX_VALUE - value) Long.MAX_VALUE else this + value
    }
}

object TleEpochConverter {
    private const val TleYearPivot = 57
    private const val MillisPerDay = 24L * 60L * 60L * 1_000L

    fun toEpochMillisOrNull(epoch: Double): Long? {
        if (!epoch.isFinite() || epoch < 0.0) return null
        val compactYear = floor(epoch / 1_000.0).toInt()
        if (compactYear !in 0..99) return null
        val dayWithFraction = epoch - compactYear * 1_000.0
        val dayOfYear = floor(dayWithFraction).toInt()
        val fractionOfDay = dayWithFraction - dayOfYear
        if (dayOfYear <= 0 || fractionOfDay < 0.0 || fractionOfDay >= 1.0) {
            return null
        }
        val year = if (compactYear < TleYearPivot) {
            2_000 + compactYear
        } else {
            1_900 + compactYear
        }
        return try {
            val dayStartMillis = LocalDate
                .ofYearDay(year, dayOfYear)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
            val offsetMillis = (fractionOfDay * MillisPerDay)
                .roundToLong()
                .coerceIn(0L, MillisPerDay - 1L)
            dayStartMillis + offsetMillis
        } catch (_: DateTimeException) {
            null
        }
    }
}
