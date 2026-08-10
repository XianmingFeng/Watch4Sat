package com.xianming.watch4sat.domain.freshness

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TleFreshnessPolicyTest {
    private val nowMillis = Instant.parse("2026-07-31T12:00:00Z").toEpochMilli()
    private val freshEpoch = TleEpochSample(
        catalogNumber = 25_544,
        epoch = 26212.5
    )

    @Test
    fun `24 hour threshold is stale at the boundary`() {
        val fresh = assess(
            retrievedAtMillis = nowMillis - TleFreshnessPolicy.StaleAfterMillis + 1L
        )
        assertEquals(
            TleFreshnessSeverity.FRESH,
            fresh.severity
        )
        assertEquals(nowMillis + 1L, fresh.nextBoundaryMillis)
        assertEquals(
            TleFreshnessSeverity.STALE,
            assess(retrievedAtMillis = nowMillis - TleFreshnessPolicy.StaleAfterMillis).severity
        )
    }

    @Test
    fun `seven day threshold is very stale at the boundary`() {
        assertEquals(
            TleFreshnessSeverity.STALE,
            assess(retrievedAtMillis = nowMillis - TleFreshnessPolicy.VeryStaleAfterMillis + 1L).severity
        )
        assertEquals(
            TleFreshnessSeverity.VERY_STALE,
            assess(retrievedAtMillis = nowMillis - TleFreshnessPolicy.VeryStaleAfterMillis).severity
        )
    }

    @Test
    fun `fresh download with old epoch uses the worse severity`() {
        val assessment = TleFreshnessPolicy.assess(
            nowMillis = nowMillis,
            retrievedAtMillis = nowMillis,
            samples = listOf(TleEpochSample(25_544, 26200.0))
        )

        assertEquals(TleFreshnessSeverity.FRESH, assessment.retrievalSeverity)
        assertEquals(TleFreshnessSeverity.VERY_STALE, assessment.epochSeverity)
        assertEquals(TleFreshnessSeverity.VERY_STALE, assessment.severity)
        assertEquals(25_544, assessment.oldestEpochCatalogNumber)
        assertFalse(assessment.shouldRefresh)
    }

    @Test
    fun `mixed selected satellites use the worst supplied epoch`() {
        val assessment = TleFreshnessPolicy.assess(
            nowMillis = nowMillis,
            retrievedAtMillis = nowMillis,
            samples = listOf(
                TleEpochSample(25_544, 26212.5),
                TleEpochSample(7_530, 26210.0),
                TleEpochSample(24_278, 26200.0)
            )
        )

        assertEquals(TleFreshnessSeverity.VERY_STALE, assessment.severity)
        assertEquals(24_278, assessment.oldestEpochCatalogNumber)
        assertEquals(1, assessment.affectedSatelliteCount)
        assertEquals(3, assessment.evaluatedSatelliteCount)
    }

    @Test
    fun `clock rollback is never reported as fresh`() {
        val assessment = assess(retrievedAtMillis = nowMillis + 1L)

        assertTrue(assessment.clockSkewDetected)
        assertEquals(TleFreshnessSeverity.STALE, assessment.retrievalSeverity)
        assertEquals(TleFreshnessSeverity.STALE, assessment.severity)
        assertNull(assessment.retrievalAgeMillis)
        assertTrue(assessment.shouldRefresh)
    }

    @Test
    fun `future TLE epoch is tracked without pretending the device clock rolled back`() {
        val assessment = TleFreshnessPolicy.assess(
            nowMillis = nowMillis,
            retrievedAtMillis = nowMillis,
            samples = listOf(TleEpochSample(25_544, 26213.0))
        )

        assertFalse(assessment.clockSkewDetected)
        assertEquals(1, assessment.futureEpochCount)
        assertEquals(TleFreshnessSeverity.FRESH, assessment.severity)
    }

    @Test
    fun `missing metadata or invalid epoch produces a conservative warning`() {
        val assessment = TleFreshnessPolicy.assess(
            nowMillis = nowMillis,
            retrievedAtMillis = null,
            samples = listOf(TleEpochSample(25_544, Double.NaN))
        )

        assertTrue(assessment.metadataMissing)
        assertEquals(1, assessment.invalidEpochCount)
        assertEquals(TleFreshnessSeverity.VERY_STALE, assessment.severity)
    }

    @Test
    fun `freshness never disables cached predictions or alerts`() {
        listOf(
            nowMillis,
            nowMillis - TleFreshnessPolicy.StaleAfterMillis,
            nowMillis - TleFreshnessPolicy.VeryStaleAfterMillis
        ).forEach { retrievedAtMillis ->
            val assessment = assess(retrievedAtMillis)
            assertTrue(assessment.allowsPrediction)
            assertTrue(assessment.allowsAlerts)
        }
    }

    @Test
    fun `TLE epoch conversion uses UTC leap days and standard year pivot`() {
        assertEquals(
            Instant.parse("2026-07-31T12:00:00Z").toEpochMilli(),
            TleEpochConverter.toEpochMillisOrNull(26212.5)
        )
        assertEquals(
            Instant.parse("2000-02-29T00:00:00Z").toEpochMilli(),
            TleEpochConverter.toEpochMillisOrNull(60.0)
        )
        assertEquals(
            Instant.parse("1957-01-01T00:00:00Z").toEpochMilli(),
            TleEpochConverter.toEpochMillisOrNull(57001.0)
        )
        assertNull(TleEpochConverter.toEpochMillisOrNull(26367.0))
        assertNull(TleEpochConverter.toEpochMillisOrNull(Double.POSITIVE_INFINITY))
    }

    private fun assess(retrievedAtMillis: Long): TleFreshnessAssessment {
        return TleFreshnessPolicy.assess(
            nowMillis = nowMillis,
            retrievedAtMillis = retrievedAtMillis,
            samples = listOf(freshEpoch)
        )
    }
}
