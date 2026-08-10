package com.xianming.watch4sat.domain.parser

import com.xianming.watch4sat.domain.fixture.SyntheticOrbitFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CelestrakParserTest {

    @Test
    fun `parseCsv converts CelesTrak amateur CSV rows to satellite records`() {
        val csv = """
            OBJECT_NAME,OBJECT_ID,EPOCH,MEAN_MOTION,ECCENTRICITY,INCLINATION,RA_OF_ASC_NODE,ARG_OF_PERICENTER,MEAN_ANOMALY,EPHEMERIS_TYPE,CLASSIFICATION_TYPE,NORAD_CAT_ID,ELEMENT_SET_NO,REV_AT_EPOCH,BSTAR,MEAN_MOTION_DOT,MEAN_MOTION_DDOT
            SYNTHETIC LEO,SYNTHETIC-LEO,2026-07-19T12:00:00.000000,15.50000000,.0010000,52.0000,120.0000,45.0000,180.0000,0,U,99901,1,1,.10000E-4,.1000E-4,0
            SYNTHETIC POLAR,,2026-07-19T12:00:00.000000,14.80000000,.0200000,98.0000,140.0000,90.0000,270.0000,0,U,99905,1,1,.20000E-4,.2000E-5,0
        """.trimIndent()

        val records = CelestrakParser.parseCsv(csv)

        assertEquals(2, records.size)
        assertEquals("SYNTHETIC LEO", records[0].displayName)
        assertEquals(SyntheticOrbitFixtures.LOW_EARTH_CATALOG, records[0].catalogNumber)
        assertEquals("SYNTHETIC-LEO", records[0].objectId)
        assertEquals(null, records[1].objectId)
        assertEquals(26200.5, records[0].orbitalData.epoch, 0.0)
        assertEquals(15.5, records[0].orbitalData.meanMotion, 0.0)
        assertEquals(0.001, records[0].orbitalData.eccentricity, 0.0)
        assertEquals(52.0, records[0].orbitalData.inclinationDegrees, 0.0)
        assertEquals(120.0, records[0].orbitalData.rightAscensionAscendingNodeDegrees, 0.0)
        assertEquals(45.0, records[0].orbitalData.argumentOfPerigeeDegrees, 0.0)
        assertEquals(180.0, records[0].orbitalData.meanAnomalyDegrees, 0.0)
        assertEquals(0.1E-4, records[0].orbitalData.bstar, 0.0)
        assertEquals(0.1E-4, records[0].orbitalData.meanMotionDot, 0.0)
    }

    @Test
    fun `parseCsv skips malformed data rows`() {
        val csv = """
            OBJECT_NAME,OBJECT_ID,EPOCH,MEAN_MOTION,ECCENTRICITY,INCLINATION,RA_OF_ASC_NODE,ARG_OF_PERICENTER,MEAN_ANOMALY,EPHEMERIS_TYPE,CLASSIFICATION_TYPE,NORAD_CAT_ID,ELEMENT_SET_NO,REV_AT_EPOCH,BSTAR,MEAN_MOTION_DOT,MEAN_MOTION_DDOT
            BROKEN
        """.trimIndent()

        assertTrue(CelestrakParser.parseCsv(csv).isEmpty())
    }

    @Test
    fun `parseCsvResult reports malformed rows instead of silently losing them`() {
        val csv = """
            OBJECT_NAME,OBJECT_ID,EPOCH,MEAN_MOTION,ECCENTRICITY,INCLINATION,RA_OF_ASC_NODE,ARG_OF_PERICENTER,MEAN_ANOMALY,EPHEMERIS_TYPE,CLASSIFICATION_TYPE,NORAD_CAT_ID,ELEMENT_SET_NO,REV_AT_EPOCH,BSTAR,MEAN_MOTION_DOT,MEAN_MOTION_DDOT
            SYNTHETIC LEO,,2026-07-19T12:00:00.000000,15.50000000,.0010000,52.0000,120.0000,45.0000,180.0000,0,U,99901,1,1,.10000E-4,.1000E-4,0
            BROKEN
        """.trimIndent()

        val result = CelestrakParser.parseCsvResult(csv)

        assertEquals(2, result.inputRecordCount)
        assertEquals(1, result.acceptedRecordCount)
        assertEquals(1, result.rejectedRecordCount)
        assertTrue(result.rejections.single().reasons.isNotEmpty())
    }

    @Test
    fun `parseCsvResult distinguishes empty input and a fully invalid batch`() {
        val empty = CelestrakParser.parseCsvResult("")
        val fullyInvalid = CelestrakParser.parseCsvResult(
            csvRow().lineSequence().first() + "\nBROKEN"
        )

        assertEquals(0, empty.inputRecordCount)
        assertTrue(empty.syntaxErrors.isNotEmpty())
        assertEquals(1, fullyInvalid.inputRecordCount)
        assertEquals(0, fullyInvalid.acceptedRecordCount)
        assertEquals(1, fullyInvalid.rejectedRecordCount)
    }

    @Test
    fun `parseCsvResult rejects non finite and out of range orbital values`() {
        val invalidValues = listOf("NaN", "Infinity", "-Infinity")

        invalidValues.forEach { invalid ->
            val result = CelestrakParser.parseCsvResult(csvRow(meanMotion = invalid))
            assertEquals(1, result.rejectedRecordCount)
            assertTrue(result.rejections.single().reasons.any { it.field == "MEAN_MOTION" })
        }

        val eccentricity = CelestrakParser.parseCsvResult(csvRow(eccentricity = "1.0"))
        val inclination = CelestrakParser.parseCsvResult(csvRow(inclination = "180.1"))

        assertTrue(eccentricity.rejections.single().reasons.any { it.field == "ECCENTRICITY" })
        assertTrue(inclination.rejections.single().reasons.any { it.field == "INCLINATION" })
    }

    @Test
    fun `parseCsvResult validates catalog mean motion and bounded angles`() {
        val invalidCatalog = CelestrakParser.parseCsvResult(csvRow(catalogNumber = "0"))
        val invalidMeanMotion = CelestrakParser.parseCsvResult(csvRow(meanMotion = "0"))
        val invalidAngle = CelestrakParser.parseCsvResult(csvRow(raan = "-0.1"))

        assertTrue(invalidCatalog.rejections.single().reasons.any { it.field == "NORAD_CAT_ID" })
        assertTrue(invalidMeanMotion.rejections.single().reasons.any { it.field == "MEAN_MOTION" })
        assertTrue(invalidAngle.rejections.single().reasons.any { it.field == "RA_OF_ASC_NODE" })
    }

    @Test
    fun `parseCsvResult accepts valid orbital boundary values`() {
        val lowerBoundaries = CelestrakParser.parseCsvResult(
            csvRow(
                catalogNumber = "1",
                meanMotion = Double.MIN_VALUE.toString(),
                eccentricity = "0.0",
                inclination = "0.0",
                raan = "0.0",
                argumentOfPerigee = "0.0",
                meanAnomaly = "0.0"
            )
        )
        val upperBoundaries = CelestrakParser.parseCsvResult(
            csvRow(
                eccentricity = "0.9999999999999999",
                inclination = "180.0",
                raan = "360.0",
                argumentOfPerigee = "360.0",
                meanAnomaly = "360.0"
            )
        )

        assertEquals(1, lowerBoundaries.acceptedRecordCount)
        assertEquals(1, upperBoundaries.acceptedRecordCount)
    }

    @Test
    fun `parseCsvResult reports duplicate catalog ids and truncated rows`() {
        val header = csvRow().lineSequence().first()
        val row = csvRow().lineSequence().drop(1).single()
        val result = CelestrakParser.parseCsvResult("$header\n$row\n$row\nTRUNCATED")

        assertEquals(setOf("99901"), result.duplicateStableIds)
        assertEquals(1, result.rejectedRecordCount)
        assertFalse(result.syntaxErrors.isNotEmpty())
    }

    @Test
    fun `parseCsvResult rejects a truncated quoted record`() {
        val result = CelestrakParser.parseCsvResult(csvRow() + "\n\"TRUNCATED")

        assertEquals(1, result.acceptedRecordCount)
        assertEquals(1, result.rejectedRecordCount)
        assertTrue(result.rejections.single().reasons.any { it.message.contains("unclosed quote") })
    }

    @Test
    fun `parseTle converts three line TLE blocks to satellite records`() {
        val tle = SyntheticOrbitFixtures.lowEarthTle + "\n" + SyntheticOrbitFixtures.polarTle

        val records = CelestrakParser.parseTle(tle)

        assertEquals(2, records.size)
        assertEquals("SYNTHETIC LEO", records[0].displayName)
        assertEquals(SyntheticOrbitFixtures.LOW_EARTH_CATALOG, records[0].catalogNumber)
        assertEquals(26200.5, records[0].orbitalData.epoch, 0.0)
        assertEquals(15.5, records[0].orbitalData.meanMotion, 0.0)
        assertEquals(0.001, records[0].orbitalData.eccentricity, 0.0)
        assertEquals(0.00001, records[0].orbitalData.meanMotionDot, 0.0)
    }

    private fun csvRow(
        catalogNumber: String = "99901",
        meanMotion: String = "15.50000000",
        eccentricity: String = ".0010000",
        inclination: String = "52.0000",
        raan: String = "120.0000",
        argumentOfPerigee: String = "45.0000",
        meanAnomaly: String = "180.0000"
    ): String {
        return """
            OBJECT_NAME,OBJECT_ID,EPOCH,MEAN_MOTION,ECCENTRICITY,INCLINATION,RA_OF_ASC_NODE,ARG_OF_PERICENTER,MEAN_ANOMALY,EPHEMERIS_TYPE,CLASSIFICATION_TYPE,NORAD_CAT_ID,ELEMENT_SET_NO,REV_AT_EPOCH,BSTAR,MEAN_MOTION_DOT,MEAN_MOTION_DDOT
            SYNTHETIC LEO,,2026-07-19T12:00:00.000000,$meanMotion,$eccentricity,$inclination,$raan,$argumentOfPerigee,$meanAnomaly,0,U,$catalogNumber,1,1,.10000E-4,.1000E-4,0
        """.trimIndent()
    }
}
