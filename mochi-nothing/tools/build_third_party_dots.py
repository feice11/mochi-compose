# Copyright 2026, mochi-compose contributors
# SPDX-License-Identifier: Apache-2.0

"""Repair existing MDI dot derivatives on a regular matrix per glyph.

These remain third-party derivatives, not Nothing-original artwork. Original
silhouettes are preserved as inputs; no hand-authored dot positions or outlines.
"""
from pathlib import Path
import hashlib
import json
import sys
from fontTools.pens.svgPathPen import SVGPathPen
from matrix_dot_layout import build

BASE = Path(__file__).resolve().parent / 'vendor/ndot-mdi'
sys.path.insert(0, str(BASE))
from upstream_dotmatrix import draw_circle_quad

index = json.loads((BASE / 'index.json').read_text(encoding='utf-8'))
for name, meta in index.items():
    points, recipe = build(name)
    assert len(points) > 1, name
    original = BASE / 'originals' / (name + '.svg')
    out = SVGPathPen(None, ntos=lambda value: format(value, '.3f').rstrip('0').rstrip('.') or '0')
    # Preserve the importer's existing 25dp/em role scale and Y-up source frame.
    units = meta['unitsPerEm'] / 25
    assert max(x+r for x,y,r in points)-min(x-r for x,y,r in points)<=24.0001, name
    assert max(y+r for x,y,r in points)-min(y-r for x,y,r in points)<=24.0001, name
    for x, y, radius in points:
        draw_circle_quad(out, (x-12)*units, (12-y)*units, radius*units)
    raw = out.getCommands()
    assert len(raw.encode('utf-8')) < 32767, name
    (BASE / (name + '.svg')).write_text(
        f'<svg xmlns="http://www.w3.org/2000/svg"><path d="{raw}" /></svg>\n', encoding='utf-8')
    meta.update({
        'dotContours': len(points),
        'sourceProject': 'GeneralKaos666/ndot-firacode-nerd a2c370b24ba6f21a20bb50e42866ffb7345da99d',
        'variant': 'Local regular-matrix repair of existing MDI derivative; not Nothing original',
        'recipe': {**recipe, 'minimumDotGapDp': recipe['pitch']*.06, 'dotDiameterOverPitch': .94,
                   'sourceExtentCapEm': .84,
                   'gridOriginRule': 'Frame center; matched circular hole center for circular features',
                   'sourceOutlineUnchanged': True},
        'originalSvgSha256': hashlib.sha256(original.read_bytes()).hexdigest(),
        'converterSha256': hashlib.sha256((BASE/'upstream_dotmatrix.py').read_bytes()).hexdigest(),
        'layoutGeneratorSha256': hashlib.sha256((BASE.parents[1]/'matrix_dot_layout.py').read_bytes()).hexdigest(),
    })
    meta.pop('fontUrl', None)
    meta.pop('fontSha256', None)
(BASE / 'index.json').write_text(json.dumps(index, indent=2)+'\n', encoding='utf-8')
print(f'Built {len(index)} regular-matrix MDI derivatives')
