# Roboto Flex Font

Last updated: 2026-06-17

Historical record: Watch4Sat `0.11.0` embedded Roboto Flex as:

```text
app-wear/src/main/res/font/roboto_flex.ttf
```

Current `0.11.1+` builds no longer embed or use this font resource. The active
font record is:

```text
docs/licenses/GOOGLE-SANS-FLEX.md
```

Source:

```text
https://github.com/googlefonts/roboto-flex
```

The historical embedded file was downloaded from the official Google Fonts
Roboto Flex repository. Roboto Flex is licensed under the SIL Open Font License
1.1.

Known limitation from the historical 0.11.0 build: Roboto Flex did not cover CJK
glyphs. If Chinese or other CJK UI text is introduced later, Android will use
system fallback fonts for those glyphs unless the active font family covers
them; that must be visually checked for line height, weight, and curved text fit
on round Wear screens.
