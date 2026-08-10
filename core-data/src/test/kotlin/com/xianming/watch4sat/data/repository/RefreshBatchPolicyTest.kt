package com.xianming.watch4sat.data.repository

import com.xianming.watch4sat.domain.model.OrbitalData
import com.xianming.watch4sat.domain.model.SatelliteRecord
import com.xianming.watch4sat.domain.model.TransmitterRecord
import com.xianming.watch4sat.domain.parser.FeedParseResult
import com.xianming.watch4sat.domain.parser.FeedRecordRejection
import com.xianming.watch4sat.domain.parser.FeedValidationIssue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshBatchPolicyTest {

    @Test
    fun `tle first refresh requires centralized minimum and no invalid records`() {
        val accepted = RefreshBatchPolicy.evaluateTle(
            result = tleResult(RefreshBatchPolicy.MinimumInitialTleRecordCount),
            previousAcceptedCount = 0
        )
        val tooSmall = RefreshBatchPolicy.evaluateTle(
            result = tleResult(RefreshBatchPolicy.MinimumInitialTleRecordCount - 1),
            previousAcceptedCount = 0
        )
        val partial = RefreshBatchPolicy.evaluateTle(
            result = tleResult(
                count = RefreshBatchPolicy.MinimumInitialTleRecordCount,
                rejected = 1
            ),
            previousAcceptedCount = 0
        )

        assertEquals(BatchAcceptance.Accepted, accepted)
        assertTrue(tooSmall is BatchAcceptance.Rejected)
        assertTrue(partial is BatchAcceptance.Rejected)
    }

    @Test
    fun `tle rejects duplicates and valid count below eighty percent of cache`() {
        val duplicate = RefreshBatchPolicy.evaluateTle(
            result = tleResult(count = 80, duplicates = setOf("1")),
            previousAcceptedCount = 100
        )
        val boundary = RefreshBatchPolicy.evaluateTle(
            result = tleResult(count = 80),
            previousAcceptedCount = 100
        )
        val below = RefreshBatchPolicy.evaluateTle(
            result = tleResult(count = 79),
            previousAcceptedCount = 100
        )

        assertTrue(duplicate is BatchAcceptance.Rejected)
        assertEquals(BatchAcceptance.Accepted, boundary)
        assertTrue(below is BatchAcceptance.Rejected)
    }

    @Test
    fun `satnogs accepts five percent invalid and rejects more than five percent`() {
        val boundary = RefreshBatchPolicy.evaluateTransmitters(
            result = transmitterResult(accepted = 19, rejected = 1),
            previousAcceptedCount = 20
        )
        val above = RefreshBatchPolicy.evaluateTransmitters(
            result = transmitterResult(accepted = 18, rejected = 2),
            previousAcceptedCount = 20
        )

        assertEquals(BatchAcceptance.Accepted, boundary)
        assertTrue(above is BatchAcceptance.Rejected)
    }

    @Test
    fun `satnogs first refresh requires centralized minimum and rejects duplicates`() {
        val accepted = RefreshBatchPolicy.evaluateTransmitters(
            result = transmitterResult(
                accepted = RefreshBatchPolicy.MinimumInitialTransmitterRecordCount,
                rejected = 0
            ),
            previousAcceptedCount = 0
        )
        val tooSmall = RefreshBatchPolicy.evaluateTransmitters(
            result = transmitterResult(
                accepted = RefreshBatchPolicy.MinimumInitialTransmitterRecordCount - 1,
                rejected = 0
            ),
            previousAcceptedCount = 0
        )
        val duplicate = RefreshBatchPolicy.evaluateTransmitters(
            result = transmitterResult(
                accepted = RefreshBatchPolicy.MinimumInitialTransmitterRecordCount,
                rejected = 0,
                duplicates = setOf("tx-1")
            ),
            previousAcceptedCount = 0
        )

        assertEquals(BatchAcceptance.Accepted, accepted)
        assertTrue(tooSmall is BatchAcceptance.Rejected)
        assertTrue(duplicate is BatchAcceptance.Rejected)
    }

    @Test
    fun `satnogs accepts eighty percent retention and rejects below it`() {
        val boundary = RefreshBatchPolicy.evaluateTransmitters(
            result = transmitterResult(accepted = 80, rejected = 0),
            previousAcceptedCount = 100
        )
        val below = RefreshBatchPolicy.evaluateTransmitters(
            result = transmitterResult(accepted = 79, rejected = 0),
            previousAcceptedCount = 100
        )

        assertEquals(BatchAcceptance.Accepted, boundary)
        assertTrue(below is BatchAcceptance.Rejected)
    }

    private fun tleResult(
        count: Int,
        rejected: Int = 0,
        duplicates: Set<String> = emptySet()
    ): FeedParseResult<SatelliteRecord> {
        return FeedParseResult(
            records = (1..count).map(::satellite),
            inputRecordCount = count + rejected,
            rejections = (1..rejected).map {
                FeedRecordRejection(it, listOf(FeedValidationIssue("row", "invalid")))
            },
            duplicateStableIds = duplicates
        )
    }

    private fun transmitterResult(
        accepted: Int,
        rejected: Int,
        duplicates: Set<String> = emptySet()
    ): FeedParseResult<TransmitterRecord> {
        return FeedParseResult(
            records = (1..accepted).map { index ->
                TransmitterRecord(
                    uuid = "tx-$index",
                    catalogNumber = index,
                    description = "TX $index",
                    isAlive = true,
                    status = "active",
                    downlinkLowHz = 145_800_000L,
                    downlinkHighHz = null,
                    downlinkMode = "FM",
                    uplinkLowHz = null,
                    uplinkHighHz = null,
                    uplinkMode = null,
                    isInverted = false
                )
            },
            inputRecordCount = accepted + rejected,
            rejections = (1..rejected).map {
                FeedRecordRejection(it, listOf(FeedValidationIssue("row", "invalid")))
            },
            duplicateStableIds = duplicates
        )
    }

    private fun satellite(catalogNumber: Int): SatelliteRecord {
        return SatelliteRecord(
            catalogNumber = catalogNumber,
            displayName = "SAT-$catalogNumber",
            orbitalData = OrbitalData(
                name = "SAT-$catalogNumber",
                catalogNumber = catalogNumber,
                epoch = 26_200.0,
                meanMotion = 14.0,
                eccentricity = 0.001,
                inclinationDegrees = 50.0,
                rightAscensionAscendingNodeDegrees = 100.0,
                argumentOfPerigeeDegrees = 200.0,
                meanAnomalyDegrees = 300.0,
                bstar = 0.0
            )
        )
    }
}
