package com.xianming.watch4sat.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SatnogsTransmitterParserTest {

    @Test
    fun `parseActiveTransmitters maps SatNOGS JSON transmitter fields`() {
        val json = """
            [
              {
                "uuid": "UzPz4gcsNBPKPKAFPmer7g",
                "description": "Upper side band (drifting)",
                "alive": true,
                "type": "Transmitter",
                "uplink_low": 145900000,
                "uplink_high": 146000000,
                "downlink_low": 136658500,
                "downlink_high": 136700000,
                "mode": "USB",
                "mode_id": 9,
                "uplink_mode": "FM",
                "invert": true,
                "sat_id": "SCHX-0895-2361-9925-0309",
                "norad_cat_id": 965,
                "status": "active",
                "updated": "2019-04-18T05:39:53.343316Z",
                "extra_field": "ignored"
              }
            ]
        """.trimIndent()

        val transmitter = SatnogsTransmitterParser.parseActiveTransmitters(json).single()

        assertEquals("UzPz4gcsNBPKPKAFPmer7g", transmitter.uuid)
        assertEquals("Upper side band (drifting)", transmitter.description)
        assertTrue(transmitter.isAlive)
        assertEquals(965, transmitter.catalogNumber)
        assertEquals(136658500L, transmitter.downlinkLowHz)
        assertEquals(136700000L, transmitter.downlinkHighHz)
        assertEquals("USB", transmitter.downlinkMode)
        assertEquals(145900000L, transmitter.uplinkLowHz)
        assertEquals(146000000L, transmitter.uplinkHighHz)
        assertEquals("FM", transmitter.uplinkMode)
        assertTrue(transmitter.isInverted)
    }

    @Test
    fun `parseActiveTransmitters keeps nullable optional uplink and mode fields`() {
        val json = """
            [
              {
                "uuid": "abc123",
                "description": "Beacon",
                "alive": false,
                "type": "Transmitter",
                "uplink_low": null,
                "uplink_high": null,
                "downlink_low": 145800000,
                "downlink_high": null,
                "mode": null,
                "uplink_mode": null,
                "invert": false,
                "norad_cat_id": 12345,
                "status": "active"
              }
            ]
        """.trimIndent()

        val transmitter = SatnogsTransmitterParser.parseActiveTransmitters(json).single()

        assertEquals("abc123", transmitter.uuid)
        assertFalse(transmitter.isAlive)
        assertEquals(145800000L, transmitter.downlinkLowHz)
        assertNull(transmitter.downlinkHighHz)
        assertNull(transmitter.downlinkMode)
        assertNull(transmitter.uplinkLowHz)
        assertNull(transmitter.uplinkHighHz)
        assertNull(transmitter.uplinkMode)
        assertFalse(transmitter.isInverted)
    }

    @Test
    fun `parseActiveTransmitters drops inactive records from mixed JSON`() {
        val json = """
            [
              {"uuid":"active","description":"FM","alive":true,"downlink_low":145800000,"mode":"FM","invert":false,"norad_cat_id":1,"status":"active"},
              {"uuid":"inactive","description":"Old","alive":true,"downlink_low":145900000,"mode":"FM","invert":false,"norad_cat_id":2,"status":"inactive"}
            ]
        """.trimIndent()

        val transmitters = SatnogsTransmitterParser.parseActiveTransmitters(json)

        assertEquals(listOf("active"), transmitters.map { it.uuid })
    }

    @Test
    fun `structured result distinguishes invalid and ignored records`() {
        val json = """
            [
              {"uuid":"active","description":"FM","alive":true,"downlink_low":145800000,"norad_cat_id":1,"status":"active"},
              {"uuid":"inactive","description":"Old","alive":true,"norad_cat_id":2,"status":"inactive"},
              {"uuid":"","description":"Broken","alive":true,"norad_cat_id":3,"status":"active"}
            ]
        """.trimIndent()

        val result = SatnogsTransmitterParser.parseActiveTransmittersResult(json)

        assertEquals(3, result.inputRecordCount)
        assertEquals(1, result.acceptedRecordCount)
        assertEquals(1, result.ignoredRecordCount)
        assertEquals(1, result.rejectedRecordCount)
    }

    @Test
    fun `structured result reports duplicate stable identities and invalid ranges`() {
        val json = """
            [
              {"uuid":"duplicate","description":"FM","alive":true,"downlink_low":145900000,"downlink_high":145800000,"norad_cat_id":1,"status":"active"},
              {"uuid":"duplicate","description":"FM","alive":true,"downlink_low":145800000,"norad_cat_id":1,"status":"active"}
            ]
        """.trimIndent()

        val result = SatnogsTransmitterParser.parseActiveTransmittersResult(json)

        assertEquals(setOf("duplicate"), result.duplicateStableIds)
        assertEquals(1, result.rejectedRecordCount)
        assertTrue(result.rejections.single().reasons.any { it.field == "downlink_high" })
    }

    @Test
    fun `structured result reports malformed root syntax`() {
        val result = SatnogsTransmitterParser.parseActiveTransmittersResult("""{"not":"an array"}""")

        assertTrue(result.records.isEmpty())
        assertTrue(result.syntaxErrors.isNotEmpty())
    }

    @Test
    fun `structured result distinguishes empty fully invalid and truncated payloads`() {
        val empty = SatnogsTransmitterParser.parseActiveTransmittersResult("[]")
        val fullyInvalid = SatnogsTransmitterParser.parseActiveTransmittersResult(
            """[{"uuid":"","description":"","status":"active"}]"""
        )
        val truncated = SatnogsTransmitterParser.parseActiveTransmittersResult("""[{"uuid":"cut"""")

        assertEquals(0, empty.inputRecordCount)
        assertEquals(0, empty.acceptedRecordCount)
        assertEquals(1, fullyInvalid.inputRecordCount)
        assertEquals(1, fullyInvalid.rejectedRecordCount)
        assertTrue(truncated.syntaxErrors.isNotEmpty())
    }

    @Test
    fun `structured result accepts valid frequency boundary values`() {
        val result = SatnogsTransmitterParser.parseActiveTransmittersResult(
            """
            [
              {
                "uuid": "boundary",
                "description": "Boundary",
                "uplink_low": 0,
                "uplink_high": 0,
                "downlink_low": 0,
                "downlink_high": 0,
                "norad_cat_id": 1,
                "status": "active"
              }
            ]
            """.trimIndent()
        )

        assertEquals(1, result.acceptedRecordCount)
        assertEquals(0, result.rejectedRecordCount)
    }
}
