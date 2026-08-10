# CelesTrak Data-Use Statement

Reviewed: 2026-07-31

Watch4Sat retrieves amateur-satellite general perturbations data at runtime:

```text
https://celestrak.org/NORAD/elements/gp.php?GROUP=amateur&FORMAT=csv
```

The release APK does not bundle a CelesTrak dataset. Downloaded records are
validated and cached locally for pass prediction and display.

Sources reviewed:

- CelesTrak home: https://celestrak.org/
- Current GP query endpoint:
  https://celestrak.org/NORAD/elements/gp.php?GROUP=amateur&FORMAT=csv
- CelesTrak GP data format documentation:
  https://celestrak.org/NORAD/documentation/gp-data-formats.php
- CelesTrak column describing the GP data query interface:
  https://celestrak.org/columns/v04n03/

Watch4Sat has not identified an authoritative public license that expressly
grants redistribution of the CelesTrak dataset in an application package or
standalone data product. This repository therefore does not claim such a
right. Do not bundle, republish, sell, or provide a mirror of cached CelesTrak
records based on this statement.

For Watch4Sat 1.0.0-rc.1, the maintainer confirmed the deliberately narrow use
described here: runtime application access only. Watch4Sat will not bundle,
mirror, sell, republish, or otherwise redistribute a CelesTrak dataset. This
confirmation does not claim that CelesTrak granted a redistribution license.
Any future distribution model that goes beyond runtime access requires a new
terms review before implementation or release.

The confirmation and the reviewed sources are bound to the active external
release-identity evidence report.

One documentation URL presented an invalid TLS certificate during the
2026-07-31 review when accessed through the `www.celestrak.org` hostname. The
canonical `celestrak.org` pages and runtime endpoint were used instead; TLS
verification was not bypassed to manufacture evidence.
