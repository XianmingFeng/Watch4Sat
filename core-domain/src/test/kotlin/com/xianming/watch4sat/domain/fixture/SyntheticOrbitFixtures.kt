package com.xianming.watch4sat.domain.fixture

/**
 * Artificial orbital elements authored for Watch4Sat tests.
 *
 * These records do not describe real spacecraft and were not copied or derived from a
 * CelesTrak (or other third-party) dataset. See the test-resource provenance note.
 */
internal object SyntheticOrbitFixtures {
    const val LOW_EARTH_CATALOG = 99_901
    const val HIGH_ECCENTRICITY_CATALOG = 99_902
    const val MOLNIYA_LIKE_CATALOG = 99_903
    const val GEOSTATIONARY_CATALOG = 99_904
    const val POLAR_CATALOG = 99_905

    val lowEarthTle = """
        SYNTHETIC LEO
        1 99901U          26200.50000000  .00001000  00000+0  10000-4 0  0012
        2 99901  52.0000 120.0000 0010000  45.0000 180.0000 15.50000000    11
    """.trimIndent()

    val highEccentricityTle = """
        SYNTHETIC HEO
        1 99902U          26200.50000000  .00000000  00000+0  00000+0 0  0016
        2 99902  25.0000 210.0000 6000000 125.0000 305.0000  2.05000000    11
    """.trimIndent()

    val molniyaLikeTle = """
        SYNTHETIC MOLNIYA
        1 99903U          26200.50000000  .00000000  00000+0  00000+0 0  0017
        2 99903  63.4000 150.0000 6800000 275.0000  15.0000  2.00000000    18
    """.trimIndent()

    val geostationaryTle = """
        SYNTHETIC GEO
        1 99904U          26200.50000000  .00000000  00000+0  00000+0 0  0018
        2 99904   0.5000  85.0000 0001000  85.0000 112.0000  1.00270000    10
    """.trimIndent()

    val polarTle = """
        SYNTHETIC POLAR
        1 99905U          26200.50000000  .00000200  00000+0  20000-4 0  0018
        2 99905  98.0000 140.0000 0200000  90.0000 270.0000 14.80000000    10
    """.trimIndent()
}
