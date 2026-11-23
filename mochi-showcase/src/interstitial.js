/**
 * 极简电影字卡与落版层
 * 全片 90% 以上时间完全透明、零文字，不破坏 3D 摄影棚画面；
 * 仅在极短节奏硬切（0.3s 单字冲击）与最终落版「Mochi」时浮现。
 */

const SANS_STACK = '"PingFang SC", "Noto Sans SC", "Microsoft YaHei UI", "Microsoft YaHei", sans-serif';

export class OverlayLayer {
  constructor(canvas) {
    this.canvas = canvas;
    this.ctx = canvas.getContext('2d');
    this.width = 1280;
    this.height = 720;
  }

  resize(w, h) {
    if (w === this.width && h === this.height) return;
    this.width = w;
    this.height = h;
    this.canvas.width = w;
    this.canvas.height = h;
  }

  render(overlay) {
    const { ctx, width: W, height: H } = this;
    ctx.clearRect(0, 0, W, H);
    if (!overlay || overlay.mode === 'none' || overlay.alpha <= 0.005) return;

    const u = Math.min(W, H);

    if (overlay.mode === 'flash') {
      // 极干脆的满屏建筑节奏单字硬切（零副标题、零边框、零花哨特效）
      ctx.save();
      ctx.globalAlpha = overlay.alpha;
      ctx.fillStyle = overlay.invert ? '#1A120E' : '#F4ECE2';
      ctx.fillRect(0, 0, W, H);

      ctx.translate(W * 0.5, H * 0.5);
      ctx.scale(overlay.scale, overlay.scale);
      ctx.fillStyle = overlay.invert ? '#FAF4EC' : '#141110';
      ctx.font = `700 ${Math.round(u * 0.44)}px ${SANS_STACK}`;
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText(overlay.text, 0, u * 0.02);
      ctx.restore();
    } else if (overlay.mode === 'lockup') {
      // 结尾底部干净克制落版：只有「Mochi」本身
      ctx.save();
      ctx.globalAlpha = overlay.alpha;
      if (overlay.blur > 0.3 && 'filter' in ctx) {
        ctx.filter = `blur(${overlay.blur.toFixed(1)}px)`;
      }
      ctx.translate(W * 0.5, H * 0.785);
      ctx.scale(overlay.scale, overlay.scale);
      ctx.fillStyle = '#FAF6EF';
      ctx.font = `600 ${Math.round(u * 0.072)}px ${SANS_STACK}`;
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      // 考究字距手排
      const letters = overlay.text.split('');
      const tracking = u * 0.016;
      const widths = letters.map((ch) => ctx.measureText(ch).width);
      const totalW = widths.reduce((a, b) => a + b, 0) + tracking * (letters.length - 1);
      let curX = -totalW * 0.5;
      for (let i = 0; i < letters.length; i++) {
        ctx.fillText(letters[i], curX + widths[i] * 0.5, 0);
        curX += widths[i] + tracking;
      }
      ctx.restore();
    }
  }
}
