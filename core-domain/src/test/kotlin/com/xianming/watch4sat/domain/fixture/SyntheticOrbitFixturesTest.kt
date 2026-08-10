package com.xianming.watch4sat.domain.fixture

import com.xianming.watch4sat.domain.parser.CelestrakParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyntheticOrbitFixturesTest {

    @Test
    fun `published orbital fixtures retain synthetic identity and valid TLE checksums`() {
        allFixtures.forEach { tle ->
            val lines = tle.lineSequence().toList()
            val record = CelestrakParser.parseTle(tle).single()

            assertTrue(lines.first().startsWith("SYNTHETIC "))
            assertTrue(record.catalogNumber in 99_901..99_905)
            assertTrue(lines[1].substring(9, 17).isBlank())
            assertEquals(checksum(lines[1]), lines[1].last().digitToInt())
            assertEquals(checksum(lines[2]), lines[2].last().digitToInt())
        }
    }

    private fun checksum(line: String): Int {
        return line.dropLast(1).sumOf { character ->
            when {
                character.isDigit() -> character.digitToInt()
                character == '-' -> 1
                else -> 0
            }
        } % 10
    }

    private companion object {
        val allFixtures = listOf(
            SyntheticOrbitFixtures.lowEarthTle,
            SyntheticOrbitFixtures.highEccentricityTle,
            SyntheticOrbitFixtures.molniyaLikeTle,
            SyntheticOrbitFixtures.geostationaryTle,
            SyntheticOrbitFixtures.polarTle
        )
    }
}
