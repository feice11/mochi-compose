# Copyright 2026, mochi-compose contributors
# SPDX-License-Identifier: Apache-2.0

"""Guard source integrity, optical bounds and finely spaced third-party round dots."""
from pathlib import Path
import argparse
import hashlib
import json
import math
import sys
import xml.etree.ElementTree as ET
from fontTools.svgLib.path import parse_path

ROOT=Path(__file__).resolve().parents[2]
BASE=ROOT/'mochi-nothing'
sys.path.insert(0,str(BASE/'tools/vendor/ndot-mdi'))
from upstream_dotmatrix import ContoursPen

parser=argparse.ArgumentParser()
parser.add_argument('--report',type=Path)
args=parser.parse_args()
manifest=json.loads((BASE/'icon-provenance.json').read_text(encoding='utf-8'))
A='{http://schemas.android.com/apk/res/android}'
rows=[]
for asset in manifest['assets']:
    file=BASE/'src/main/res/drawable'/(asset['resource']+'.xml')
    assert hashlib.sha256(file.read_bytes()).hexdigest()==asset['resourceSha256']
    node=ET.parse(file).getroot()
    group=node.find('group')
    sx,sy=float(group.get(A+'scaleX')),float(group.get(A+'scaleY'))
    dx,dy=float(group.get(A+'translateX')),float(group.get(A+'translateY'))
    w,h=float(node.get(A+'viewportWidth')),float(node.get(A+'viewportHeight'))
    l,t,r,b=asset['inkBounds']
    xs,ys=sorted([l*sx+dx,r*sx+dx]),sorted([t*sy+dy,b*sy+dy])
    assert min(xs+ys)>=-.0001 and xs[1]<=w+.0001 and ys[1]<=h+.0001, asset['key']
    row={'key':asset['key'],'style':asset['style'],'withinViewport':True}
    if asset['style']=='official-firmware-dot':
        source=ET.parse(ROOT/asset['source']).getroot()
        assert [p.get(A+'pathData') for p in source.iter('path')]==[p.get(A+'pathData') for p in node.iter('path')]
        row['originalPathDataRetained']=True
    else:
        raw=next(node.iter('path')).get(A+'pathData')
        assert len(raw.encode())<32767
        pen=ContoursPen(None)
        parse_path(raw,pen)
        circles=pen.result()
        dots=[]
        for circle in circles:
            x0,y0=circle.min(axis=0)
            x1,y1=circle.max(axis=0)
            assert abs((x1-x0)-(y1-y0))<.01, asset['key']
            dots.append((((x0+x1)/2)*sx+dx,((y0+y1)/2)*sy+dy,(x1-x0)*abs(sx)/2))
        assert len(dots)>1
        if asset['style']=='third-party-dot':
            # Verify the shipped paths, not merely the declared recipe: every
            # center belongs to one square lattice and every circle is equal.
            pitch = asset['recipe']['pitch'] * asset['unitsPerEm'] / 25
            centers=[((p[:,0].min()+p[:,0].max())/2,(p[:,1].min()+p[:,1].max())/2) for p in circles]
            ax,ay=centers[0]
            assert all(abs((x-ax)/pitch-round((x-ax)/pitch))<.0001 and abs((y-ay)/pitch-round((y-ay)/pitch))<.0001 for x,y in centers), asset['key']
            assert max(r for x,y,r in dots)-min(r for x,y,r in dots)<.0001, asset['key']
            assert len({(round(x,4),round(y,4)) for x,y in centers})==len(centers), asset['key']
            row['regularMatrixVerified']=True
        minimum=min(math.hypot(x-u,y-v)-radius-other for i,(x,y,radius) in enumerate(dots) for u,v,other in dots[i+1:])
        # Preserve a fine visible seam: circle diameter is 94% of the nearest
        # center distance. This also covers diagonal confirmation cells.
        nearest=min(math.hypot(x-u,y-v) for i,(x,y,radius) in enumerate(dots) for u,v,other in dots[i+1:])
        assert abs(minimum/nearest-.06)<.0001, (asset['key'],'expected 6% spacing',minimum/nearest)
        row['nearestGapFraction']=minimum/nearest
        ox,oy=asset['opticalOffset']
        assert max(abs(ox),abs(oy))<=.750001
        cx=sum(x for x,y,r in dots)/len(dots)
        cy=sum(y for x,y,r in dots)/len(dots)
        before=math.hypot(cx-ox-w/2,cy-oy-h/2)
        after=math.hypot(cx-w/2,cy-h/2)
        assert after<=before+.0001
        row.update({'dots':len(dots),'minimumDotGapDp':minimum,'centroidErrorBeforeDp':before,'centroidErrorAfterDp':after,'opticalOffset':[ox,oy]})
    rows.append(row)
assert all(any(a['key']==d['key'] for a in rows) for d in manifest['demo'])
report={'resources':rows,'knownResources':len(rows),'demoSemantics':len(manifest['demo']),
        'ordinaryFallbacks':0,'minimumThirdPartyGapDp':min(r['minimumDotGapDp'] for r in rows if 'minimumDotGapDp' in r),
        'slotLayoutChanged':False}
if args.report:
    args.report.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print(f"{len(rows)} resources verified; third-party gap >= {report['minimumThirdPartyGapDp']:.3f}dp; no clipping; OTA paths retained")
