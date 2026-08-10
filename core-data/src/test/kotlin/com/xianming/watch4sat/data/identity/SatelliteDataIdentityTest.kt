package com.xianming.watch4sat.data.identity

import com.xianming.watch4sat.domain.model.OrbitalData
import com.xianming.watch4sat.domain.model.SatelliteRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SatelliteDataIdentityTest {

    @Test
    fun `hash is stable across input ordering`() {
        val first = satellite(1, 14.0)
        val second = satellite(2, 15.0)

        assertEquals(
            SatelliteDataIdentity.sha256(listOf(first, second)),
            SatelliteDataIdentity.sha256(listOf(second, first))
        )
    }

    @Test
    fun `same timestamp cannot hide changed orbital content`() {
        val original = satellite(1, 14.0)
        val changed = satellite(1, 14.1)

        assertNotEquals(
            SatelliteDataIdentity.sha256(listOf(original)),
            SatelliteDataIdentity.sha256(listOf(changed))
        )
    }

    @Test
    fun `normalization treats signed zero as equivalent`() {
        val positiveZero = satellite(1, 14.0)
        val negativeZero = positiveZero.copy(
            orbitalData = positiveZero.orbitalData.copy(bstar = -0.0)
        )

        assertEquals(
            SatelliteDataIdentity.sha256(listOf(positiveZero)),
            SatelliteDataIdentity.sha256(listOf(negativeZero))
        )
    }

    private fun satellite(catalogNumber: Int, meanMotion: Double): SatelliteRecord {
        return SatelliteRecord(
            catalogNumber = catalogNumber,
            displayName = "SAT-$catalogNumber",
            objectId = "2026-00$catalogNumber",
            orbitalData = OrbitalData(
                name = "SAT-$catalogNumber",
                catalogNumber = catalogNumber,
                epoch = 26_200.0,
                meanMotion = meanMotion,
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
