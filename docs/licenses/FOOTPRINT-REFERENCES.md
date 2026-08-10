# Orbit Footprint References

Last updated: 2026-06-08

Watch4Sat 0.6.0 adds an Orbit Map satellite footprint overlay. The
implementation is independent Kotlin code in `core-domain`.

## Reference Boundary

- Gpredict `gtk-sat-map.c` was reviewed for the public idea of sampling a
  satellite footprint as great-circle destination points around the current
  sub-satellite point.
- Gpredict is GPLv2. It is used as reference only.
- Watch4Sat does not copy, translate, or port Gpredict C source, function
  structure, constants, UI code, or map overlay implementation.
- The implemented geometry uses the existing Watch4Sat/Look4Sat-derived ground
  position and altitude, then computes:
  `centralAngle = acos(R / (R + altitudeKm))`, with `R = 6378.137 km`, sampled
  at 96 bearings.

## Project License Context

Watch4Sat already contains Look4Sat GPLv3-derived orbital prediction logic and
keeps the GPLv3 attribution/license handling documented in
`docs/licenses/LOOK4SAT-GPLv3.txt`.
