#!/usr/bin/env python3
"""
ndot_nerd_symbols.py — restyle the Nerd Font icons of a merged font into
NDOT 47's dot-matrix look.

NDOT 47 draws every glyph out of circles sitting on a regular grid (its letters
use touching dots at a pitch of em/9). This script downsamples each Nerd Font
icon (private-use-area glyph) of the input font onto the same kind of grid and
redraws it as circles, so the symbols pick up NDOT's "screen door" aesthetic
instead of FiraCode's smooth outlines.

FontForge's native script language cannot sample outlines, so this step runs in
Python (fontTools + numpy). Typical build:

    fontforge -script native_merge.pe ndot-47-inspired-by-nothing.ttf \\
        FiraCodeNerdFont-Regular.ttf            # -> ndot-firacode-nerd.otf
    python3 ndot_nerd_symbols.py                # dot-matrixes the icons in place

The second command rewrites the merged .otf in place. Use -o to write elsewhere.
"""
from __future__ import annotations

import argparse
import math
import sys

import numpy as np
from fontTools.pens.basePen import BasePen
from fontTools.pens.boundsPen import BoundsPen
from fontTools.pens.t2CharStringPen import T2CharStringPen
from fontTools.pens.ttGlyphPen import TTGlyphPen
from fontTools.ttLib import TTFont

# --------------------------------------------------------------------------- #
# Private-use ranges that carry Nerd Font icons.
# --------------------------------------------------------------------------- #
PUA_RANGES = (
    (0xE000, 0xF8FF),      # BMP PUA  (Powerline, Devicons, Font Awesome, ...)
    (0xF0000, 0xFFFFD),    # Plane 15 (Material Design Icons, ...)
    (0x100000, 0x10FFFD),  # Plane 16
)


def is_nerd_codepoint(cp: int) -> bool:
    return any(lo <= cp <= hi for lo, hi in PUA_RANGES)


# --------------------------------------------------------------------------- #
# Outline -> flattened polygon contours (drawn once, sampled many times).
# --------------------------------------------------------------------------- #
def _quad(p0, p1, p2, t):
    mt = 1.0 - t
    return (
        mt * mt * p0[0] + 2 * mt * t * p1[0] + t * t * p2[0],
        mt * mt * p0[1] + 2 * mt * t * p1[1] + t * t * p2[1],
    )


def _cube(p0, p1, p2, p3, t):
    mt = 1.0 - t
    a, b, c, d = mt**3, 3 * mt * mt * t, 3 * mt * t * t, t**3
    return (
        a * p0[0] + b * p1[0] + c * p2[0] + d * p3[0],
        a * p0[1] + b * p1[1] + c * p2[1] + d * p3[1],
    )


class ContoursPen(BasePen):
    """Turns a glyph outline into a list of flattened polygon contours."""

    def __init__(self, glyphSet, steps=8):
        super().__init__(glyphSet)
        self.steps = steps
        self.contours: list[list[tuple[float, float]]] = []
        self._cur: list[tuple[float, float]] = []

    def _moveTo(self, pt):
        if self._cur:
            self.contours.append(self._cur)
        self._cur = [pt]

    def _lineTo(self, pt):
        self._cur.append(pt)

    def _curveToOne(self, p1, p2, p3):
        p0 = self._cur[-1]
        for i in range(1, self.steps + 1):
            self._cur.append(_cube(p0, p1, p2, p3, i / self.steps))

    def _qCurveToOne(self, p1, p2):
        p0 = self._cur[-1]
        for i in range(1, self.steps + 1):
            self._cur.append(_quad(p0, p1, p2, i / self.steps))

    def _closePath(self):
        if self._cur:
            self.contours.append(self._cur)
            self._cur = []

    def _endPath(self):
        self._closePath()

    def result(self):
        self._closePath()
        return [np.asarray(c, dtype=float) for c in self.contours if len(c) >= 2]


def winding_number(contours, qx, qy):
    """Nonzero winding number of every query point against all contours."""
    qx = np.asarray(qx, dtype=float)
    qy = np.asarray(qy, dtype=float)
    wn = np.zeros(qx.shape, dtype=np.int32)
    for poly in contours:
        x, y = poly[:, 0], poly[:, 1]
        x1, y1 = np.roll(x, -1), np.roll(y, -1)
        isleft = (x1 - x)[:, None] * (qy[None, :] - y[:, None]) - \
                 (y1 - y)[:, None] * (qx[None, :] - x[:, None])
        up = (y[:, None] <= qy[None, :]) & (y1[:, None] > qy[None, :])
        down = (y[:, None] > qy[None, :]) & (y1[:, None] <= qy[None, :])
        wn += (up & (isleft > 0)).sum(axis=0)
        wn -= (down & (isleft < 0)).sum(axis=0)
    return wn


def grid_cells(contours, bounds, pitch, subsamples, threshold):
    """Return the set of (i, j) grid cells whose coverage >= threshold."""
    x0, y0, x1, y1 = bounds
    i0, i1 = math.floor(x0 / pitch), math.floor(x1 / pitch)
    j0, j1 = math.floor(y0 / pitch), math.floor(y1 / pitch)
    if i1 < i0:
        i0, i1 = i1, i0
    if j1 < j0:
        j0, j1 = j1, j0

    off = np.array([(k + 0.5) / subsamples for k in range(subsamples)])
    qx, qy = [], []
    for j in range(j0, j1 + 1):
        for i in range(i0, i1 + 1):
            for fy in off:
                for fx in off:
                    qx.append((i + fx) * pitch)
                    qy.append((j + fy) * pitch)
    if not qx:
        return set()

    wn = winding_number(contours, qx, qy)
    wn = wn.reshape(j1 - j0 + 1, i1 - i0 + 1, subsamples, subsamples)
    cover = (wn != 0).mean(axis=(2, 3))

    cells = set()
    rows, cols = np.nonzero(cover >= threshold)
    for r, c in zip(rows.tolist(), cols.tolist()):
        cells.add((i0 + c, j0 + r))
    return cells


# --------------------------------------------------------------------------- #
# Drawing the dots back into the glyph.
# --------------------------------------------------------------------------- #
KAPPA = 0.5522847498307936  # cubic circle constant


def draw_circle_cubic(pen, cx, cy, r):
    k = KAPPA * r
    pen.moveTo((cx + r, cy))
    pen.curveTo((cx + r, cy + k), (cx + k, cy + r), (cx, cy + r))
    pen.curveTo((cx - k, cy + r), (cx - r, cy + k), (cx - r, cy))
    pen.curveTo((cx - r, cy - k), (cx - k, cy - r), (cx, cy - r))
    pen.curveTo((cx + k, cy - r), (cx + r, cy - k), (cx + r, cy))
    pen.closePath()


def draw_circle_quad(pen, cx, cy, r):
    """NDOT-style circle: 8 quadratic segments (control on the arc bisector)."""
    c = r / math.cos(math.radians(22.5))
    pts = []
    for step in range(8):
        a = math.radians(step * 45)
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    pen.moveTo(pts[0])
    for step in range(8):
        a_end = math.radians((step + 1) * 45)
        a_mid = math.radians(step * 45 + 22.5)
        ctrl = (cx + c * math.cos(a_mid), cy + c * math.sin(a_mid))
        end = (cx + r * math.cos(a_end), cy + r * math.sin(a_end))
        pen.qCurveTo(ctrl, end)
    pen.closePath()


def round_pt(x, y):
    return (int(round(x)), int(round(y)))


# --------------------------------------------------------------------------- #
def dotmatrix_font(path, out_path, cells, radius_frac, subsamples, threshold,
                   preview, limit):
    font = TTFont(path)
    upm = font["head"].unitsPerEm
    pitch = upm / cells
    radius = pitch * radius_frac
    glyph_set = font.getGlyphSet()
    cmap = font.getBestCmap()
    is_cff = "CFF " in font
    if is_cff:
        cff = font["CFF "].cff
        top = cff[cff.fontNames[0]]
    elif "glyf" not in font:
        sys.exit("error: %s has neither CFF nor glyf outlines" % path)

    targets = sorted((cp, gn) for cp, gn in cmap.items() if is_nerd_codepoint(cp))
    if limit:
        targets = targets[:limit]

    print("font      : %s" % path)
    print("upm       : %d   grid: %d cells  pitch: %.1f  dot r: %.1f"
          % (upm, cells, pitch, radius))
    print("icons     : %d private-use glyphs to restyle" % len(targets))

    changed = 0
    emptied = 0
    for idx, (cp, name) in enumerate(targets, 1):
        bp = BoundsPen(glyph_set)
        glyph_set[name].draw(bp)
        if bp.bounds is None or name not in glyph_set:
            continue
        pen = ContoursPen(glyph_set)
        glyph_set[name].draw(pen)
        contours = pen.result()
        if not contours:
            continue
        cells_set = grid_cells(contours, bp.bounds, pitch, subsamples, threshold)

        if preview and (idx <= 4 or cp in preview):
            _print_preview(cp, name, cells_set, bp.bounds, pitch)

        if not cells_set:
            emptied += 1          # would erase the glyph; keep the original
            continue

        width = font["hmtx"][name][0]
        if is_cff:
            out_pen = T2CharStringPen(width, glyph_set)
            drawer = draw_circle_cubic
        else:
            out_pen = TTGlyphPen(glyph_set)
            drawer = draw_circle_quad

        for i, j in sorted(cells_set):
            drawer(out_pen, *round_pt((i + 0.5) * pitch, (j + 0.5) * pitch), radius)

        if is_cff:
            top.CharStrings[name] = out_pen.getCharString(
                private=top.Private, globalSubrs=cff.GlobalSubrs)
        else:
            glyph = out_pen.glyph()
            glyph.recalcBounds(font["glyf"])
            font["glyf"][name] = glyph
            font["hmtx"][name] = (width, glyph.xMin)
        changed += 1

        if idx % 500 == 0:
            print("  ... %d/%d" % (idx, len(targets)))

    print("restyled  : %d glyphs%s" %
          (changed, ("  (%d left unchanged, would have gone empty)" % emptied)
           if emptied else ""))
    if out_path == path:
        print("writing   : %s (in place)" % out_path)
    else:
        print("writing   : %s" % out_path)
    font.save(out_path)


def _print_preview(cp, name, cells, bounds, pitch):
    if not cells:
        print("  U+%04X %s: (empty)" % (cp, name))
        return
    xs = [c[0] for c in cells]
    ys = [c[1] for c in cells]
    x0, x1, y0, y1 = min(xs), max(xs), min(ys), max(ys)
    grid = [[" "] * (x1 - x0 + 1) for _ in range(y1 - y0 + 1)]
    for i, j in cells:
        grid[y1 - j][i - x0] = "#"
    print("  U+%04X %s:" % (cp, name))
    for row in grid:
        print("    " + "".join(row))


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("font", nargs="?", default="ndot-firacode-nerd.otf",
                    help="merged font to restyle (default: ndot-firacode-nerd.otf)")
    ap.add_argument("-o", "--output", default=None,
                    help="output path (default: overwrite the input font)")
    ap.add_argument("-c", "--cells", type=int, default=16,
                    help="grid cells per em; higher = finer dots, more detail (default: 16)")
    ap.add_argument("-r", "--radius", type=float, default=0.499,
                    help="dot radius as a fraction of the grid pitch (default: 0.499, "
                         "~touching like NDOT)")
    ap.add_argument("-s", "--subsamples", type=int, default=3,
                    help="coverage sub-samples per axis, per cell (default: 3)")
    ap.add_argument("-t", "--threshold", type=float, default=0.34,
                    help="fraction of sub-samples that must be inside to place a dot "
                         "(default: 0.34)")
    ap.add_argument("--preview", type=lambda s: int(s, 0), action="append", default=[],
                    help="also print an ASCII preview for this codepoint "
                         "(e.g. --preview 0xE0B0); repeatable")
    ap.add_argument("--limit", type=int, default=0,
                    help="only process the first N icons (for quick tests)")
    args = ap.parse_args(argv)

    out = args.output or args.font
    dotmatrix_font(args.font, out, args.cells, args.radius, args.subsamples,
                   args.threshold, args.preview, args.limit)


if __name__ == "__main__":
    main()
