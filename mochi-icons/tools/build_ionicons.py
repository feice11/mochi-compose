#!/usr/bin/env python3
# Copyright 2026, mochi-compose contributors
# SPDX-License-Identifier: Apache-2.0
"""
把 Ionicons 的 npm 包（`npm pack ionicons@<版本>` 解出来的 package 目录）转成 mochi-icons 的两份资源：

- `ionicons.txt`：一行一个图标，`名字\t路径\t路径…`；每段路径是 `flags,线宽[,旋转角,支点x,支点y]|d`
  flags 的位：1 填充、2 描边、4 evenOdd、8/16 圆 / 方线头、32/64 圆 / 斜角拐角
- `ionicons-index.txt`：一行一个基础名，`名字\t样式位（1 描边、2 实心、4 直角）\t标签…`

矩形、圆、椭圆换成路径；圆弧的两个 0/1 标志重新写成带分隔的形式（压缩过的 SVG 常把它们连着写，
Compose 的 PathParser 会读错）。用法：python build_ionicons.py <package 目录> <输出目录>
"""

import json
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

NS = "{http://www.w3.org/2000/svg}"


def num(v, default=0.0):
    if v is None:
        return default
    return float(v.replace("px", ""))


def fmt(v):
    s = f"{v:.3f}".rstrip("0").rstrip(".")
    return "0" if s in ("-0", "") else s


TOKEN = re.compile(r"[A-Za-z]|[-+]?(?:\d+\.?\d*|\.\d+)(?:[eE][-+]?\d+)?")


def normalize_path(d, scale=1.0):
    """逐个命令重写：圆弧的第 4、5 个参数只取一个字符；画布不是 512 时按 [scale] 放大坐标（圆弧的旋转角和标志不缩放）"""
    out = []
    i = 0
    cmd = ""
    arg = 0
    n = len(d)
    while i < n:
        c = d[i]
        if c in " ,\n\t\r":
            i += 1
            continue
        if c.isalpha() and c not in "eE":
            cmd = c
            arg = 0
            out.append(c)
            i += 1
            continue
        if cmd in "aA" and arg % 7 in (3, 4):
            out.append(c)
            i += 1
            arg += 1
            continue
        m = TOKEN.match(d, i)
        if not m:
            raise ValueError(f"无法解析：{d[i:i + 20]}")
        v = m.group(0)
        if scale != 1.0 and not (cmd in "aA" and arg % 7 == 2):
            v = fmt(float(v) * scale)
        out.append(v)
        i = m.end()
        arg += 1
    # 命令字母后紧跟数字、数字之间用空格（负号前也加空格，保持可读）
    s = []
    for t in out:
        if s and not t.isalpha() and not s[-1].isalpha():
            s.append(" ")
        s.append(t)
    return "".join(s)


def rect_path(e):
    x, y = num(e.get("x")), num(e.get("y"))
    w, h = num(e.get("width")), num(e.get("height"))
    rx, ry = e.get("rx"), e.get("ry")
    rx = num(rx if rx is not None else ry)
    ry = num(ry if ry is not None else e.get("rx"))
    rx, ry = min(rx, w / 2), min(ry, h / 2)
    if rx <= 0 or ry <= 0:
        return f"M{fmt(x)} {fmt(y)}h{fmt(w)}v{fmt(h)}h{fmt(-w)}z"
    a = f"a{fmt(rx)} {fmt(ry)} 0 0 1"
    return (
        f"M{fmt(x + rx)} {fmt(y)}h{fmt(w - 2 * rx)}{a} {fmt(rx)} {fmt(ry)}"
        f"v{fmt(h - 2 * ry)}{a} {fmt(-rx)} {fmt(ry)}"
        f"h{fmt(-(w - 2 * rx))}{a} {fmt(-rx)} {fmt(-ry)}"
        f"v{fmt(-(h - 2 * ry))}{a} {fmt(rx)} {fmt(-ry)}z"
    )


def ellipse_path(cx, cy, rx, ry):
    return (
        f"M{fmt(cx - rx)} {fmt(cy)}a{fmt(rx)} {fmt(ry)} 0 1 0 {fmt(2 * rx)} 0"
        f"a{fmt(rx)} {fmt(ry)} 0 1 0 {fmt(-2 * rx)} 0z"
    )


def rotation(t):
    """Ionicons 里的变换都是绕某点旋转：rotate(a cx cy) 或 matrix(-1 0 0 -1 e f)（绕 (e/2, f/2) 转 180°）"""
    if not t:
        return None
    m = re.fullmatch(r"rotate\(([-\d.]+)(?:[ ,]+([-\d.]+)[ ,]+([-\d.]+))?\)", t.strip())
    if m:
        return float(m.group(1)), float(m.group(2) or 0), float(m.group(3) or 0)
    m = re.fullmatch(r"matrix\(-1 0 0 -1 ([-\d.]+) ([-\d.]+)\)", t.strip())
    if m:
        return 180.0, float(m.group(1)) / 2, float(m.group(2)) / 2
    raise ValueError(f"不支持的变换：{t}")


INHERITED = ("fill", "stroke", "stroke-width", "stroke-linecap", "stroke-linejoin", "fill-rule")


def element_paths(svg):
    """根元素上的 fill / stroke 之类会被子元素继承；既不填充也不描边的元素（Ionicons 里留着的辅助形状）跳过"""
    vb = [float(v) for v in svg.get("viewBox", "0 0 512 512").split()]
    scale = 512.0 / vb[2]
    root = {k: svg.get(k) for k in INHERITED if svg.get(k) is not None}
    paths = []
    for e in svg.iter():
        tag = e.tag.replace(NS, "")
        if tag == "path":
            d = normalize_path(e.get("d"), scale)
        elif tag == "rect":
            d = normalize_path(rect_path(e), scale)
        elif tag == "circle":
            r = num(e.get("r"))
            d = normalize_path(ellipse_path(num(e.get("cx")), num(e.get("cy")), r, r), scale)
        elif tag == "ellipse":
            d = normalize_path(ellipse_path(num(e.get("cx")), num(e.get("cy")), num(e.get("rx")), num(e.get("ry"))), scale)
        elif tag in ("svg", "title", "g"):
            continue
        else:
            raise ValueError(f"不支持的元素：{tag}")

        def attr(k, default=None):
            return e.get(k, root.get(k, default))

        flags = 0
        if attr("fill", "currentColor") != "none":
            flags |= 1
        stroke = attr("stroke")
        width = 0.0
        if stroke and stroke != "none":
            flags |= 2
            width = num(attr("stroke-width"), 1.0) * scale
        if flags == 0:
            continue
        if attr("fill-rule") == "evenodd":
            flags |= 4
        flags |= {"round": 8, "square": 16}.get(attr("stroke-linecap"), 0)
        flags |= {"round": 32, "bevel": 64}.get(attr("stroke-linejoin"), 0)
        head = f"{flags},{fmt(width)}"
        rot = rotation(e.get("transform"))
        if rot:
            head += "," + ",".join(fmt(v) for v in (rot[0], rot[1] * scale, rot[2] * scale))
        paths.append(f"{head}|{d}")
    return paths


def main():
    pkg, out = Path(sys.argv[1]), Path(sys.argv[2])
    out.mkdir(parents=True, exist_ok=True)
    version = json.loads((pkg / "package.json").read_text(encoding="utf-8"))["version"]
    lines = []
    for f in sorted((pkg / "dist" / "svg").glob("*.svg")):
        svg = ET.parse(f).getroot()
        lines.append(f.stem + "\t" + "\t".join(element_paths(svg)))
    (out / "ionicons.txt").write_text(f"# ionicons {version}\n" + "\n".join(lines) + "\n", encoding="utf-8", newline="\n")

    tags = {}
    styles = {}
    for icon in json.loads((pkg / "dist" / "ionicons.json").read_text(encoding="utf-8"))["icons"]:
        name = icon["name"]
        base, bit = name, 2
        if name.endswith("-outline"):
            base, bit = name[: -len("-outline")], 1
        elif name.endswith("-sharp"):
            base, bit = name[: -len("-sharp")], 4
        styles[base] = styles.get(base, 0) | bit
        tags.setdefault(base, set()).update(t for t in icon["tags"] if t not in ("outline", "sharp"))
    index = [f"{b}\t{styles[b]}\t{' '.join(sorted(tags[b] - {b}))}" for b in sorted(styles)]
    (out / "ionicons-index.txt").write_text(f"# ionicons {version}\n" + "\n".join(index) + "\n", encoding="utf-8", newline="\n")
    print(f"ionicons {version}: {len(lines)} 个图标，{len(index)} 个基础名")


if __name__ == "__main__":
    main()
