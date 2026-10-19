# Pixelarticons native-cell dot derivatives

Sources: halfmage/pixelarticons commit c9ecc2da61afde85dbe539025e3d931c6152ad75, MIT (Gerrit Halfmann). The *-original.svg files are unchanged upstream SVGs; LICENSE retains the upstream license.

Each existing 2x2 rectangular source cell becomes one circular dot using the unchanged MIT NDOT upstream circle function vendored in ../ndot-mdi/upstream_dotmatrix.py. This preserves the source author's stroke widths, check arms, Wi-Fi arc gaps and native 24x24 frame; it does not rasterize a thick silhouette or hand-author new icon shapes. Check contains 8 dots; Wi-Fi contains 25 mirror-symmetric dots; close and chevrons retain one dot per native stroke cell. Wi-Fi uses .8-radius circles for horizontal gaps; diagonal symbols use .98-radius circles. source.json and index.json record pinned URLs, hashes and dot centers.

Generated check.svg and wifi.svg are third-party derivatives, not Nothing-official artwork. Rebuild with python mochi-nothing/tools/build_pixelart_dots.py followed by python mochi-nothing/tools/import_icons.py.
