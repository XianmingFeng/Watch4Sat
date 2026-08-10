# Look4Sat Upstream Association

Reviewed: 2026-07-31

Watch4Sat contains and adapts GPL-3.0-or-later code from:

- Project: Look4Sat
- Copyright: Copyright (C) 2019-2026 Arty Bishop and contributors
- Repository: https://github.com/rt-bishop/Look4Sat
- Baseline release: v4.4.0
- Baseline commit: `39b786ae82bfab5cdc86a308c6bd4a684acf9cdd`
- Commit URL:
  https://github.com/rt-bishop/Look4Sat/commit/39b786ae82bfab5cdc86a308c6bd4a684acf9cdd
- License: GPL-3.0-or-later

Look4Sat's `LICENSE` at that commit is byte-for-byte identical to
`docs/licenses/LOOK4SAT-GPLv3.txt`:

```text
SHA-256 3972dc9744f6499f0f9b2dbf76696f2ae7ad8af9b23dde66d6af86c9dfb36986
```

## Current File Association

The following Watch4Sat files were compared on 2026-07-31 and are
byte-for-byte identical to their paths under Look4Sat
`core/domain/src/main/java/com/rtbishop/look4sat/core/domain/predict/` at the
baseline commit:

- `CelestialComputer.kt`
- `Constants.kt`
- `DeepSpaceObject.kt`
- `GeoPos.kt`
- `NearEarthObject.kt`
- `OrbitalData.kt`
- `OrbitalMath.kt`
- `OrbitalObject.kt`
- `OrbitalPos.kt`

The following retain Look4Sat copyright and GPL headers but are modified or
adapted in Watch4Sat:

- `core-domain/src/main/kotlin/com/rtbishop/look4sat/core/domain/predict/OrbitalPass.kt`
- `core-domain/src/main/kotlin/com/rtbishop/look4sat/core/domain/utility/GeoConverter.kt`
- `core-domain/src/main/kotlin/com/xianming/watch4sat/domain/pass/PassPredictionService.kt`,
  adapted from `SatelliteRepo.kt`
- `core-domain/src/main/kotlin/com/xianming/watch4sat/domain/qth/MaidenheadLocator.kt`,
  adapted from `QthConverter.kt`
- `core-domain/src/main/kotlin/com/xianming/watch4sat/domain/parser/CelestrakParser.kt`,
  adapted from `DataParser.kt`

Watch4Sat changes are recorded in this repository's Git history. The
corresponding source for any distributed binary must identify the exact
Watch4Sat release commit, not only this upstream baseline.

The full project remains licensed under GPL-3.0-or-later. This notice preserves
upstream authorship and does not imply Look4Sat or its contributors endorse
Watch4Sat.
