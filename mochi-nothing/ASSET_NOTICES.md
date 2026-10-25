# Firmware asset provenance

This optional local module contains resources collected from Nothing Phone (3), Nothing OS 4.1, build `Metroid_B4.1-260814-1733`. It is separate from `mochi-compose`; the base library does not depend on it. It ships fonts, colors and control palettes only; the theme draws Mochi's own icons. No Maven publication is configured for this module.

- Firmware package: https://android.googleapis.com/packages/ota-api/package/1825643456d6973c5ea22aff7c29d871cc071c04.zip
- Inventory and extraction evidence: `artifacts/nothing-resource-research/` in this workspace.
- Fonts: original Inter Variable, NType82 Headline and Ndot57 Aligned assets were copied without glyph modifications. Display/Headline and large-title roles now resolve through the device's system serif family (`FontFamily.Serif`) for Latin and Chinese; the retained NType82 asset is not selected by the default theme. Body text uses Inter with platform Chinese fallback; explicit dot displays retain Ndot57. File hashes are in [font-provenance.json](font-provenance.json). No system Chinese font is redistributed.

The repository's Apache-2.0 header applies to our Kotlin integration code, not automatically to extracted assets. Redistribution permission for the firmware-sourced NType/Ndot fonts has not been established here. Their original rights and applicable licenses remain with their owners; this local integration does not grant those rights. Do not publish this asset module as Apache-licensed artwork. Inter's upstream project is https://github.com/rsms/inter (SIL Open Font License); retain upstream notices if redistributing it.
