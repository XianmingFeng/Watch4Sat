# Synthetic Orbital Fixtures

The orbital records in `SyntheticOrbitFixtures.kt` were authored for Watch4Sat
tests on 2026-07-31. They describe no real spacecraft: names and catalog
identifiers are explicitly synthetic, international designators are blank, and
the orbital parameters were selected by the project solely to exercise these
algorithm classes:

- a near-circular low-Earth orbit;
- two high-eccentricity deep-space orbits;
- a near-geostationary orbit;
- a near-polar low-Earth orbit.

No record was downloaded, copied, or derived from CelesTrak or another orbital
dataset. TLE checksums were computed locally from the authored lines. Fixed
propagation references used by the boundary and position tests were calculated
independently with `satellite.js 6.0.1` from these synthetic inputs.
