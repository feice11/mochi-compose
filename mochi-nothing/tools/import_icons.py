# Copyright 2026, mochi-compose contributors
# SPDX-License-Identifier: Apache-2.0

"""Import original OTA dots and repaired regular-grid third-party glyphs; no manually authored silhouettes."""
from pathlib import Path
import copy
import hashlib
import json
import re
import shutil
import xml.etree.ElementTree as ET
from fontTools.pens.boundsPen import BoundsPen
from fontTools.svgLib.path import parse_path
import sys

ROOT = Path(__file__).resolve().parents[2]
MODULE = ROOT / 'mochi-nothing'
BASE = ROOT / 'artifacts/nothing-resource-research/decoded'
OUT = MODULE / 'src/main/res/drawable'
A = '{http://schemas.android.com/apk/res/android}'
ET.register_namespace('android', A[1:-1])
S, L, W, U = 'NothingSettings', 'NothingLauncher3', 'NothingWeather', 'SystemUI-apktool'
MAP = {
    'document-text': (L, 'nt_ic_custom_reading'), 'moon': (W, 'ic_clear_night'),
    'heart': (L, 'nt_ic_custom_favourites'), 'sunny': (W, 'ic_sunny'),
    'toggle': (L, 'nt_widgets_quick_settings_icon'),
    'chatbubble-ellipses': (L, 'nt_ic_custom_social'),
    'reader': (L, 'nt_ic_custom_reading'), 'flash': ('NothingX', 'nothing_widget_battery_charging_image'),
    'ellipsis-horizontal': (L, 'ic_more_horiz_24'),
    'apps': (S, 'ic_apps'),
    'wifi': (W, 'ic_main_no_connection'),
    'camera': ('NTCamera', 'widget_camera_icon'),
    'close': ('NothingX', 'widget_list_icon'),
    'chevron-forward': (S, 'ic_double_summary_arrow'),
    'chevron-back': (S, 'ic_double_summary_arrow'),
    'list.chevron': (S, 'ic_double_summary_arrow'),
    'chevron-down': (U, 'nt_biometric_arrow'),
    'options': (L, 'nt_ic_custom_tools'),
    'chatbox': (L, 'nt_ic_custom_social'),
}
DOT_KEYS = set(MAP)
ALIASES = {'list.chevron': 'chevron-forward', 'control.plus': 'add', 'control.minus': 'remove'}
MDI = {
    'share': 'share_variant', 'wifi': 'wifi', 'person': 'account_outline', 'pin': 'pin_outline',
    'lock-closed': 'lock_outline', 'star': 'star', 'notifications': 'bell_outline', 'trash': 'delete_outline',
    'camera': 'camera_outline', 'volume-high': 'volume_high', 'volume-low': 'volume_low',
    'search': 'magnify', 'scan': 'scan_helper', 'add': 'plus', 'remove': 'minus',
    'airplane': 'airplane', 'mic': 'microphone', 'link': 'link_variant', 'home': 'home_outline',
    'create': 'pencil', 'copy': 'content_copy', 'flag': 'flag_outline', 'bluetooth': 'bluetooth',
    'laptop': 'laptop', 'phone-portrait': 'cellphone', 'desktop': 'monitor', 'pulse': 'pulse',
    'chevron-back': 'chevron_left', 'chevron-forward': 'chevron_right', 'close': 'close',
    'checkmark': 'check', 'chevron-down-circle': 'chevron_down_circle_outline',
    'radio-button-on': 'radiobox_marked', 'list': 'format_list_bulleted',
    'compass': 'compass_outline', 'layers': 'layers_outline', 'albums': 'card_multiple_outline',
    'expand': 'arrow_expand_all', 'swap-horizontal': 'swap_horizontal', 'options': 'tune',
    'chatbox': 'message_outline', 'water': 'water', 'swap-vertical': 'swap_vertical',
    'sparkles': 'creation', 'apps': 'apps', 'color-palette': 'palette_outline',
    'chevron-down': 'chevron_down', 'ellipsis-horizontal': 'dots_horizontal',
}
VENDOR = MODULE/'tools/vendor/ndot-mdi'
VENDOR.mkdir(parents=True, exist_ok=True)
glyph_index = json.loads((VENDOR/'index.json').read_text(encoding='utf-8')) if (VENDOR/'index.json').exists() else {}
sys.path.insert(0, str(VENDOR))
from upstream_dotmatrix import ContoursPen
PIXELART = {'checkmark': 'check'}
PIXEL_VENDOR = MODULE/'tools/vendor/pixelarticons'
pixel_index = json.loads((PIXEL_VENDOR/'index.json').read_text(encoding='utf-8'))


def write(path, text):
    data = text.encode('utf-8')
    if not path.exists() or path.read_bytes() != data:
        path.write_bytes(data)


def source(app, name):
    sub = 'res' if app.endswith('-apktool') else 'resources/res'
    found = list((BASE / app / sub).glob(f'drawable*/{name}.xml'))
    if not found:
        found = list((BASE / app).glob(f'**/drawable*/{name}.xml'))
    found.sort(key=lambda p: (p.parent.name != 'drawable', len(str(p))))
    if not found:
        raise ValueError('No extracted resource')
    p = found[0]
    node = ET.parse(p).getroot()
    if node.tag != 'vector':
        ref = node.get(A + 'drawable')
        if ref and ref.startswith('@drawable/'):
            return source(app, ref.split('/')[-1])
        raise ValueError('Not a static vector')
    return p, node


def convert(key, app, name):
    p, node = source(app, name)
    node = copy.deepcopy(node)
    original_viewport = [float(node.get(A+'viewportWidth')), float(node.get(A+'viewportHeight'))]
    node.attrib.pop(A + 'tint', None)
    bounds = []
    for group in node.iter('group'):
        if any(A + k in group.attrib for k in ['rotation', 'scaleX', 'scaleY', 'translateX', 'translateY']):
            raise ValueError('Unreviewed source transform')
    for path in node.iter('path'):
        pen = BoundsPen(None)
        parse_path(path.get(A + 'pathData', ''), pen)
        if pen.bounds:
            half = float(path.get(A + 'strokeWidth', '0')) / 2 if path.get(A + 'strokeColor') else 0
            l, t, r, b = pen.bounds
            bounds.append((l-half, t-half, r+half, b+half))
        for attr in ['fillColor', 'strokeColor']:
            v = path.get(A + attr)
            if v and v not in ['#00000000', '@android:color/transparent']:
                path.set(A + attr, '#FF000000')
    if not bounds:
        raise ValueError('Empty resource')
    dots = []
    for path in node.iter('path'):
        pen = ContoursPen(None)
        parse_path(path.get(A+'pathData', ''), pen)
        for poly in pen.result():
            low, high = poly.min(axis=0), poly.max(axis=0)
            dots.append([float((low[0]+high[0])/2), float((low[1]+high[1])/2), float((high[0]-low[0])/2)])
    l, t = min(b[0] for b in bounds), min(b[1] for b in bounds)
    r, b = max(b[2] for b in bounds), max(b[3] for b in bounds)
    vw, vh, extent = 24, 24, 20
    if key.startswith('control.'):
        vw, vh, extent = 46, 34, 15
    elif key == 'list.chevron':
        vw, vh = r-l, b-t
        extent = max(vw, vh)
    scale = {L: 20/88, W: 20/36, U: 20/72}.get(app, 1)
    if name in {'nt_widgets_quick_settings_icon', 'ic_more_horiz_24', 'ic_apps', 'ic_double_summary_arrow', 'nt_biometric_arrow', 'widget_camera_icon', 'widget_list_icon'}:
        scale = 1
    # Original Settings list-chevron keeps its tight 8x14 source viewport.
    # Other small native assets retain 1:1 dot size inside the existing 24dp slot.
    if name == 'ic_main_no_connection':
        scale = 20/198  # Original large illustration: 198-unit Wi-Fi ink span.
    if name == 'nothing_widget_battery_charging_image':
        scale = 24/16  # Native 16dp artwork adapted to the 24dp semantic frame.
    sx = -scale if key == 'chevron-back' else scale
    dx, dy = vw/2 - sx*(l+r)/2, vh/2 - scale*(t+b)/2
    node.set(A + 'width', f'{vw:.6f}dp')
    node.set(A + 'height', f'{vh:.6f}dp')
    node.set(A + 'viewportWidth', f'{vw:.6f}')
    node.set(A + 'viewportHeight', f'{vh:.6f}')
    group = ET.Element('group', {A+'scaleX': f'{sx:.8f}', A+'scaleY': f'{scale:.8f}', A+'translateX': f'{dx:.8f}', A+'translateY': f'{dy:.8f}'})
    for child in list(node):
        node.remove(child)
        group.append(child)
    node.append(group)
    res = 'nothing_' + re.sub('[^a-z0-9_]', '_', key)
    ET.indent(node)
    # Firmware artwork is not relicensed by the repository's source-code license.
    xml = '<?xml version="1.0" encoding="utf-8"?>\n' + ET.tostring(node, encoding='unicode') + '\n'
    write(OUT / (res + '.xml'), xml)
    return {'key': key, 'resource': res, 'source': p.relative_to(ROOT).as_posix(),
            'sourceSha256': hashlib.sha256(p.read_bytes()).hexdigest(),
            'resourceSha256': hashlib.sha256(xml.encode()).hexdigest(),
            'style': 'official-firmware-dot',
            'originalDotCount': len(dots), 'originalDotGeometry': dots,
            'originalViewport': original_viewport,
            'license': 'Original firmware rights; redistribution not established',
            'inkBounds': [l,t,r,b], 'scale': scale, 'translation': [dx,dy],
            'mirrored': key == 'chevron-back',
            'normalization': 'Native small-icon scale or shared large-artwork family scale; original per-asset paths, dot count, dot diameter ratios and spacing retained; back arrow mirrors the same source'}


def optical_translation(raw, bounds, scale_x, scale_y, dx, dy, vw, vh):
    # Equal-area circles make the dot-center mean the actual ink-area centroid.
    # Use a bounded partial correction, not a full centroid recentering that
    # would erase deliberate asymmetry or push the long check arm out of bounds.
    pen = ContoursPen(None)
    parse_path(raw, pen)
    circles = pen.result()
    cx = sum((p[:,0].min()+p[:,0].max())/2 for p in circles)/len(circles)
    cy = sum((p[:,1].min()+p[:,1].max())/2 for p in circles)/len(circles)
    l,t,r,b = bounds
    xs = sorted([l*scale_x+dx,r*scale_x+dx])
    ys = sorted([t*scale_y+dy,b*scale_y+dy])
    shift_x = max(-.75, min(.75, (vw/2-(cx*scale_x+dx))*.3))
    shift_y = max(-.75, min(.75, (vh/2-(cy*scale_y+dy))*.3))
    shift_x = max(-xs[0], min(vw-xs[1], shift_x))
    shift_y = max(-ys[0], min(vh-ys[1], shift_y))
    return dx+shift_x, dy+shift_y, [shift_x,shift_y]


def native_pixel(key, name):
    svg=PIXEL_VENDOR/(name+'.svg')
    raw=ET.parse(svg).getroot().find('{http://www.w3.org/2000/svg}path').get('d')
    pen=BoundsPen(None)
    parse_path(raw,pen)
    bounds=pen.bounds
    # Keep the author's native 24x24 frame and original stroke proportions.
    scale=(25 if key=='wifi' else 27)/24
    dx=dy=12-12*scale
    vw=vh=24
    if key=='list.chevron':
        l,t,r,b=bounds
        vw,vh=(r-l)*scale,(b-t)*scale
        dx,dy=-l*scale,-t*scale
    dx,dy,offset=optical_translation(raw,bounds,scale,scale,dx,dy,vw,vh)
    node=ET.Element('vector',{A+'width':f'{vw:.6f}dp',A+'height':f'{vh:.6f}dp',A+'viewportWidth':f'{vw:.6f}',A+'viewportHeight':f'{vh:.6f}'})
    group=ET.SubElement(node,'group',{A+'scaleX':f'{scale:.8f}',A+'scaleY':f'{scale:.8f}',A+'translateX':f'{dx:.8f}',A+'translateY':f'{dy:.8f}'})
    ET.SubElement(group,'path',{A+'fillColor':'#FF000000',A+'pathData':raw})
    ET.indent(node)
    xml='<?xml version="1.0" encoding="utf-8"?>\n'+ET.tostring(node,encoding='unicode')+'\n'
    res='nothing_'+re.sub('[^a-z0-9_]','_',key)
    write(OUT/(res+'.xml'),xml)
    return {'key':key,'resource':res,'source':svg.relative_to(ROOT).as_posix(),
            'sourceSha256':hashlib.sha256(svg.read_bytes()).hexdigest(),
            'resourceSha256':hashlib.sha256(xml.encode()).hexdigest(),
            'style':'third-party-native-grid-dot','license':'Pixelarticons MIT; converter MIT',
            'inkBounds':list(bounds),'scale':scale,'translation':[dx,dy],'opticalOffset':offset,
            'normalization':'Original 24x24 source frame; uniform role scale; bounded partial ink-centroid compensation',
            **pixel_index[name]}


def third_party(key, name):
    svg = VENDOR/(name+'.svg')
    assert svg.exists(), 'Run build_third_party_dots.py first'
    raw = ET.parse(svg).getroot().find('{http://www.w3.org/2000/svg}path').get('d')
    # AAPT replaces oversized UTF-8 strings with STRING_TOO_LARGE instead of
    # failing compilation. Catch that before shipping a vector that cannot load.
    assert len(raw.encode('utf-8')) < 32767, (name, 'Android path string too large')
    pen = BoundsPen(None)
    parse_path(raw, pen)
    l,t,r,b = pen.bounds
    # One font-em scale across all semantics; never enlarge each ink box to fill.
    # Component aliases share the same glyph scale, with their existing slot sizes.
    vw,vh = 24,24
    # These role scales apply only to this third-party family. Official artwork
    # retains its own density; it is never resampled onto this grid.
    display_em = 22 if key in {'add', 'remove', 'control.plus', 'control.minus'} else 25
    scale = display_em/glyph_index[name]['unitsPerEm']
    role_scale = display_em/25
    if key.startswith('control.'):
        vw,vh = 46,34
    elif key == 'list.chevron':
        vw,vh = (r-l)*scale,(b-t)*scale
    dx,dy = vw/2-scale*(l+r)/2, vh/2+scale*(t+b)/2
    dx,dy,offset = optical_translation(raw,(l,t,r,b),scale,-scale,dx,dy,vw,vh)
    node = ET.Element('vector', {A+'width': f'{vw:.6f}dp', A+'height': f'{vh:.6f}dp',
                                A+'viewportWidth': f'{vw:.6f}', A+'viewportHeight': f'{vh:.6f}'})
    group = ET.SubElement(node,'group', {A+'scaleX': f'{scale:.8f}', A+'scaleY': f'{-scale:.8f}',
                                       A+'translateX': f'{dx:.8f}', A+'translateY': f'{dy:.8f}'})
    ET.SubElement(group,'path',{A+'fillColor':'#FF000000',A+'pathData':raw})
    ET.indent(node)
    xml = '<?xml version="1.0" encoding="utf-8"?>\n'+ET.tostring(node,encoding='unicode')+'\n'
    res = 'nothing_'+re.sub('[^a-z0-9_]','_',key)
    write(OUT/(res+'.xml'),xml)
    return {'key':key,'resource':res,'source':svg.relative_to(ROOT).as_posix(),
            'sourceSha256':hashlib.sha256(svg.read_bytes()).hexdigest(),
            'resourceSha256':hashlib.sha256(xml.encode()).hexdigest(),
            'style':'third-party-dot','license':'MDI Apache-2.0; source font OFL-1.1',
            'inkBounds':[l,t,r,b], 'scale':scale, 'roleScale':role_scale, 'translation':[dx,dy], 'opticalOffset':offset,
            'normalization':'Fixed role scale; bounded partial ink-centroid compensation, Y flip. No per-icon fit',
            **glyph_index[name]}


previous = json.loads((MODULE/'icon-provenance.json').read_text(encoding='utf-8'))
records, failed = [], {}
for key in list(dict.fromkeys(list(MAP)+list(MDI)+list(ALIASES))):
    if key in DOT_KEYS:
        records.append(convert(key,*MAP[key]))
    elif key in PIXELART:
        records.append(native_pixel(key,PIXELART[key]))
    else:
        records.append(third_party(key,MDI[ALIASES.get(key,key)]))
write(VENDOR/'index.json',json.dumps(glyph_index,indent=2)+'\n')
resources = {r['key']: r['resource'] for r in records}
for row in previous['assets']:
    if row['resource'] not in resources.values():
        target = OUT / (row['resource'] + '.xml')
        assert target.resolve().is_relative_to(OUT.resolve())
        target.unlink(missing_ok=True)
code = '''// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.nothing

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import ui.feice.mochi.theme.MochiIconProvider

/** Complete dot artwork: OTA dots, native-cell third-party dots and per-glyph regular-grid dots. Unknown app keys retain their fallback. */
public object NothingIcons : MochiIconProvider {
    @Composable
    override fun resolve(key: String, filled: Boolean): ImageVector? {
        val resource = when (key) {
''' + ''.join(f'            "{k}" -> R.drawable.{v}\n' for k,v in resources.items()) + '''            else -> return null
        }
        return ImageVector.vectorResource(resource)
    }
}
'''
write(MODULE/'src/main/java/ui/feice/mochi/nothing/NothingIcons.kt', code)
entries = re.findall(r'val ([A-Z_]+) = DemoIcon\("([^"]+)"', (ROOT/'example/src/main/java/ui/feice/mochi/demo/common/IonIcons.kt').read_text(encoding='utf-8-sig'))
assert len(entries)==56 and all(k in resources for _,k in entries)
demo = [{'name': n, 'key': k, 'status': 'official-dot' if k in DOT_KEYS else 'third-party-dot'} for n,k in entries]
manifest = {'firmware': 'Metroid_B4.1-260814-1733',
            'firmwareUrl': 'https://android.googleapis.com/packages/ota-api/package/1825643456d6973c5ea22aff7c29d871cc071c04.zip',
            'policy': 'All known semantics are dots. Original OTA dot resources take precedence and retain their own sparse/dense layouts; no common firmware grid, dot radius or dot count is imposed. Settings supplies the seven-dot navigation arrow and nine-dot apps glyph; SystemUI supplies the five-dot downward arrow; Camera supplies its 31-dot camera; Nothing X supplies the 13-dot close and seven-dot lightning. Pixelarticons native 2x2 cells preserve the unmatched check stroke weights; Existing MDI derivatives are repaired on regular per-glyph matrices (9/10/11/12/13 columns per 24dp frame, dot diameter 94% of lattice pitch; 6% fine axial spacing). Sampling or source-stroke guides select integer cells; no free curve-following point positions. Original source silhouettes and upstream circle construction are retained. These are local third-party derivatives, not official Nothing artwork. Fixed role scales and bounded partial ink-centroid compensation replace per-icon fitting and bounding-box-only centering. Unknown application keys retain API fallback.',
            'assets': records, 'demo': demo, 'failedImports': failed}
write(MODULE/'icon-provenance.json', json.dumps(manifest,indent=2)+'\n')
md = '# Nothing all-dot icon mapping / 全点阵图标映射\n\n56/56 demo semantics use dot artwork, including every component alias; no known ordinary-icon fallback. Official OTA dot artwork is preserved with each asset’s native density, dot positions and diameter ratios. Settings supplies the seven-dot forward/list arrow (mirrored for back) and nine-dot apps glyph; SystemUI supplies the five-dot down arrow; Weather supplies its 25-dot unslashed Wi-Fi; Camera supplies its 31-dot camera and Nothing X the 13-dot close and seven-dot lightning; Launcher tools and social glyphs also replace third-party options/chat art. No uniform grid, point count, point radius, or optical centroid correction is imposed on official sources. Unmatched confirmation uses original MIT [Pixelarticons](https://github.com/halfmage/pixelarticons/tree/c9ecc2da61afde85dbe539025e3d931c6152ad75) 2x2 cell centers converted directly to equal circular dots with diameter 94% of the nearest diagonal-neighbor distance, without area sampling. Remaining existing third-party glyphs are repaired using their original MDI outlines from the [NDOT FiraCode Nerd source](https://github.com/GeneralKaos666/ndot-firacode-nerd/tree/a2c370b24ba6f21a20bb50e42866ffb7345da99d). Each glyph uses one regular square matrix and one circle radius; the per-glyph recipe selects 9/10/11/12/13 columns per 24dp frame and makes the dot diameter 94% of the pitch, leaving a fine 6% seam between axial neighbors. Coverage selects filled bodies; source stroke guides preserve fine rods, loops and arrows. Circular source holes set the lattice phase; no point is placed freely along a curve. Circled-down and compass use outline variants to preserve their interior symbols. These remain local third-party derivatives (MDI Apache-2.0, source font OFL-1.1), not Nothing-original artwork or unmodified upstream release glyphs. The source silhouettes are not hand-drawn. Add/remove preserve their existing 22dp/em role size, other MDI glyphs 25dp/em. Native firmware navigation symbols retain their authored point layouts; the list arrow keeps its tight 8x14 viewport. Third-party artwork applies 30% ink-area-centroid compensation capped at 0.75dp and bounded by the viewport; slot geometry never moves. Native Pixelarticons keep their author-supplied 24x24 frame and stroke-cell positions. Both variant requests retain the same source dot glyph. Unknown custom app keys retain the caller fallback.\n\n| Demo semantic | Key | Result | Source |\n|---|---|---|---|\n'
for row in demo:
    match = next((r for r in records if r['key']==row['key']), None)
    md += f'| {row["name"]} | `{row["key"]}` | {row["status"]} | {Path(match["source"]).name if match else row["reason"]} |\n'
md += '\nFull paths, source hashes, bounds and transformations: [icon-provenance.json](icon-provenance.json). Rebuild derived assets: `python mochi-nothing/tools/build_third_party_dots.py`, and `python mochi-nothing/tools/build_pixelart_dots.py`, then `python mochi-nothing/tools/import_icons.py`.\n'
write(MODULE/'ICON_MAPPING.md', md)
shutil.copyfile(MODULE/'icon-provenance.json', MODULE/'src/main/assets/nothing/icon-provenance.json')
print('Imported',len(records),'dot resources;',len(demo),'dot semantics;',sum(r['status']=='official-dot' for r in demo),'official,',sum(r['status']=='third-party-dot' for r in demo),'third-party; no ordinary fallback')
