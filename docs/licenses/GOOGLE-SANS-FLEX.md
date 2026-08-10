# Google Sans Flex Font

Last updated: 2026-06-17

Watch4Sat embeds Google Sans Flex as one Android font resource:

```text
app-wear/src/main/res/font/google_sans_flex_variable.ttf
```

Why this changed in `0.11.3`:

- The previous `google_sans_flex_400/500/600/700.ttf` files came from the
  Google Fonts CSS API and were static TTF instances.
- Those static files contained `STAT` axis metadata names such as `ROND`, but
  did not contain `fvar` or `gvar`, so Compose `FontVariation.Setting("ROND",
  100.0f)` could not change glyph outlines.
- The current file is a true variable TTF; local policy tests parse its SFNT
  tables and require `fvar`, `gvar`, `STAT`, and a `ROND` axis whose range
  includes `100`.

The `0.11.4` typography pass keeps the same variable font file and ROND=100
configuration. It only normalizes how Watch4Sat maps explicit `fontWeight`
values to Wear Material3 role tokens; the license/source provenance above is
unchanged.

Source and license evidence:

```text
Google Fonts specimen:
https://fonts.google.com/specimen/Google+Sans+Flex

Google Fonts metadata:
https://fonts.google.com/metadata/fonts/Google%20Sans%20Flex

Google Fonts FAQ:
https://developers.google.com/fonts/faq#can_i_use_the_google_sans_and_google_sans_flex_fonts

Fontsource variable package:
https://www.npmjs.com/package/@fontsource-variable/google-sans-flex
https://github.com/fontsource/font-files/tree/main/fonts/variable/google-sans-flex
```

The Google Fonts metadata records `Google Sans Flex` with `license: "ofl"`,
`lastModified: "2026-05-21"`, and variable axes `GRAD`, `ROND`, `opsz`,
`slnt`, `wdth`, and `wght`. The Google Fonts FAQ states that Google Sans and
Google Sans Flex are available under the SIL Open Font License.

The complete upstream copyright notice and SIL Open Font License 1.1 text from
`@fontsource-variable/google-sans-flex@5.2.3` is preserved in:

```text
docs/licenses/GOOGLE-SANS-FLEX-OFL-1.1.txt
app-wear/src/main/assets/legal/OFL-1.1-GOOGLE-SANS-FLEX.txt
```

Those two files have SHA-256:
`7168a081fbcea8dbe975e3a015c4e340761b3b4ddf8de0c8a818543f773a29e0`.

Embedded file source and SHA-256:

```text
Source package: @fontsource-variable/google-sans-flex@5.2.3
Source WOFF2: package/files/google-sans-flex-latin-full-normal.woff2
Conversion: wawoff2@2.0.1 woff2_decompress.js -> google_sans_flex_variable.ttf
Embedded TTF SHA-256: 404993fbfc6942b9100a28c80c71732c4c5da752ef705bb48ba7baee71d74299
Embedded TTF size: 2,801,684 bytes
```

The project uses this file through Wear Compose Material3 `Typography`,
following Android Knowledge Base guidance to place local fonts in `res/font`,
define a `FontFamily`, set `fontFamily` on Material typography styles, and pass
that typography into `MaterialTheme`.

Runtime font configuration includes:

```text
FontVariation.grade(0)
FontVariation.weight(...)
FontVariation.slant(0f)
FontVariation.width(100f)
FontVariation.opticalSizing(...)
FontVariation.Setting("ROND", 100.0f)
```

This follows Android Knowledge Base Compose variable-font guidance for custom
axes and the GoogleSansFlexTypography source reference that uses
`FontVariation.Setting("ROND", 100.0f)` for Google Sans Flex.

Weight policy:

```text
body axis weight: 520
label axis weight: 650
title/display/numeral axis weight: 750
```

See `docs/typography-weight-guidelines.md` for the active Wear M3 role-token
rules used by app text.

Known limitation: the embedded Fontsource file is the Latin full subset. Google
Sans Flex metadata does not list common Chinese/CJK coverage. If Chinese or
other CJK UI text is introduced later, Android will use system fallback fonts
for those glyphs; that must be visually checked for line height, weight, and
curved text fit on round Wear screens.
