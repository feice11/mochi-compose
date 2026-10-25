"""把卡片的模糊材质预先烧进背景视频：运行时只播视频，不再逐帧实时模糊。

材质 = 同一帧高斯模糊、乘 scale 压暗，按竖向渐变盖在原画面上
（从 top 处开始出现，往下经过 ramp 的高度盖满，和示例里 materialBelow 的写法一致）。
视频按卡片比例先裁好；loop 选 pingpong（正放接倒放）或 none。

用法（示例「效果 › 混合字」活动卡片的素材就是这样生成的）：
  python example/tools/bake_material.py ink.mp4 example/src/main/res/raw/vibrancy_ink.mp4 \
      --trim 0 13 --crop 624 720 330 0 --top 0.4 --ramp 0.2 --sigma 19 --scale 0.9 --loop pingpong
sigma 是视频像素：卡片上的模糊半径换算到视频分辨率（卡片高 / 视频高）再取 Android 模糊半径对应的 sigma（约 0.57 × 半径）。
需要 ffmpeg 在 PATH 里。
"""

import argparse
import subprocess


def main() -> None:
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("input")
    p.add_argument("output")
    p.add_argument("--trim", nargs=2, type=float, metavar=("START", "END"), help="只取这一段（秒）")
    p.add_argument("--crop", nargs=4, type=int, metavar=("W", "H", "X", "Y"), help="按卡片比例裁切")
    p.add_argument("--top", type=float, default=0.4, help="材质开始出现的位置（0..1，从上往下）")
    p.add_argument("--ramp", type=float, default=0.2, help="从开始出现到盖满经过的高度（0..1）")
    p.add_argument("--sigma", type=float, default=19, help="高斯模糊 sigma（视频像素）")
    p.add_argument("--scale", type=float, default=0.9, help="材质压暗：颜色乘这个数")
    p.add_argument("--loop", choices=["pingpong", "none"], default="pingpong")
    p.add_argument("--crf", type=int, default=29)
    a = p.parse_args()

    head = []
    if a.trim:
        head.append(f"trim={a.trim[0]}:{a.trim[1]},setpts=PTS-STARTPTS")
    if a.crop:
        w, h, x, y = a.crop
        head.append(f"crop={w}:{h}:{x}:{y}")
    # 在 RGB 里混：YUV 的色度平面按比例压暗会偏色
    head.append("format=gbrp")
    mask = f"clip((Y/H-{a.top})/{a.ramp}\\,0\\,1)"
    graph = (
        f"[0:v]{','.join(head)},split[s][b];"
        f"[b]gblur=sigma={a.sigma},colorchannelmixer=rr={a.scale}:gg={a.scale}:bb={a.scale}[m];"
        f"[s][m]blend=all_expr='A+(B-A)*{mask}'"
    )
    if a.loop == "pingpong":
        graph += "[baked];[baked]split[f][r0];[r0]reverse[r];[f][r]concat=n=2:v=1:a=0"
    graph += ",format=yuv420p[v]"
    subprocess.run(
        [
            "ffmpeg", "-y", "-v", "error", "-i", a.input,
            "-filter_complex", graph, "-map", "[v]",
            "-c:v", "libx264", "-profile:v", "high", "-crf", str(a.crf), "-preset", "veryslow",
            "-movflags", "+faststart", "-an", a.output,
        ],
        check=True,
    )


if __name__ == "__main__":
    main()
