# Copyright 2026, mochi-compose contributors
# SPDX-License-Identifier: Apache-2.0

"""Replace each existing 2x2 Pixelarticons cell with an upstream NDOT circle.

The source already supplies pixel positions and stroke widths. No silhouette
sampling, hand drawing, or independent x/y scaling is involved.
"""
from pathlib import Path
import hashlib
import math
import json
import sys
import xml.etree.ElementTree as ET
import numpy as np
from fontTools.svgLib.path import parse_path
from fontTools.pens.svgPathPen import SVGPathPen

BASE = Path(__file__).resolve().parent / 'vendor/pixelarticons'
sys.path.insert(0, str(BASE.parent/'ndot-mdi'))
from upstream_dotmatrix import ContoursPen, draw_circle_quad

source = json.loads((BASE/'source.json').read_text(encoding='utf-8'))
index = {}
for name in ['check', 'wifi', 'close', 'chevron-left', 'chevron-right', 'chevron-down']:
    original = BASE/(name+'-original.svg')
    node = ET.parse(original).getroot()
    assert node.get('viewBox') == '0 0 24 24'
    centers = set()
    for path in node.findall('{http://www.w3.org/2000/svg}path'):
        pen = ContoursPen(None)
        parse_path(path.get('d'), pen)
        for polygon in pen.result():
            l,t = polygon.min(axis=0)
            r,b = polygon.max(axis=0)
            area = abs(np.sum(polygon[:,0]*np.roll(polygon[:,1],-1)-polygon[:,1]*np.roll(polygon[:,0],-1)))/2
            assert abs(area-(r-l)*(b-t)) < .0001, (name, 'source cell is not a rectangle')
            assert all(abs(v/2-round(v/2)) < .0001 for v in [r-l,b-t])
            for x in np.arange(l+1,r,2):
                for y in np.arange(t+1,b,2):
                    centers.add((float(x),float(y)))
    out = SVGPathPen(None, ntos=lambda value: format(value,'.6f').rstrip('0').rstrip('.') or '0')
    neighbor_distance=min(math.dist(a,b) for a in centers for b in centers if a!=b)
    radius=neighbor_distance*.47
    for x,y in sorted(centers):
        draw_circle_quad(out,x,y,radius)
    target=BASE/(name+'.svg')
    target.write_text(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path d="{out.getCommands()}" /></svg>\n',encoding='utf-8')
    index[name] = {**source, 'sourceUrl':f'https://raw.githubusercontent.com/halfmage/pixelarticons/{source["commit"]}/svg/{name}.svg',
                   'originalSha256':hashlib.sha256(original.read_bytes()).hexdigest(),
                   'dotContours':len(centers),'dotCenters':sorted(centers),
                   'recipe':'Exact native 2x2 cell centers; diameter is 94% of the nearest neighbor distance, including diagonal check cells; 6% fine spacing, no resampling', 'radius':radius, 'minimumDotGapDp':neighbor_distance*.06, 'dotDiameterOverNeighborDistance':.94}
assert len(index['check']['dotCenters']) == 8
# Every leg cell retains the same single dot, instead of a thick bend and a thin tail.
wifi_centers = set(map(tuple,index['wifi']['dotCenters']))
assert all((24-x,y) in wifi_centers for x,y in wifi_centers), 'Wi-Fi source lost mirror symmetry'
(BASE/'index.json').write_text(json.dumps(index,indent=2)+'\n',encoding='utf-8')
print({name:row['dotContours'] for name,row in index.items()})
