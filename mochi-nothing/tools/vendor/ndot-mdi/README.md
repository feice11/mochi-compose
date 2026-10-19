# Third-party dot derivatives

Unchanged original MDI outlines were extracted from FiraCodeNerdFont-Regular.ttf in GeneralKaos666/ndot-firacode-nerd commit a2c370b24ba6f21a20bb50e42866ffb7345da99d. originals/ and index.json preserve sources, codepoints and hashes. MDI artwork is Apache-2.0, source font OFL-1.1, upstream converter MIT. No full source font enters the APK.

build_third_party_dots.py uses unmodified upstream_dotmatrix.py sampler/circles. Recipe: 10 cells/em, .4 pitch radius, 5x5 sampling, .25 coverage, center-dot phase. Oversized outlines are capped to .9em; apps is normalized to .9em before sampling to preserve nine separated tiles. Rendering uses fixed 25dp/em for pictograms, 22dp/em for add/remove, with bounded partial ink-centroid compensation. Completed dot art is never individually stretched to fit.

This replaces the dense nearly touching 12-cell build. These are third-party derivatives, not Nothing-original or unchanged prebuilt glyphs, and no silhouettes are manually authored. Full licenses and notices are in src/main/assets/licenses.
