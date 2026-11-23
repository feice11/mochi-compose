/**
 * 《Mochi》42 秒全 3D 质感丝线一镜贯穿导演引擎
 *
 * 创作原则：
 * - 一根 3D 丝绸质感丝线从头至尾贯穿全片：绷紧 -> 被压弯 -> 拨弦回弹 -> 化作融球液桥 -> 穿过 3D 液态玻璃光学折断 -> 凝结信号红珠 -> 归静落版；
 * - 零廉价 sin(t) 永动呼吸、零原地匀速自转；每个动作皆遵循「蓄力 -> 释放 -> 丝滑长减速 -> 真正静止」的电影级曲线；
 * - 全片前 37.5 秒零任何文字，仅结尾极轻浮现唯一落版 Mochi。
 */

import {
  MOCHI_TOKENS,
  clamp,
  lerp,
  smoothstep,
  smootherstep,
  remap,
  expoOut,
  quintOut,
  springEval,
  jellyStretch,
  jellyCrossScale,
} from './physics.js';

export const TOTAL_DURATION = 42.0;

/** 极度丝滑的长拖尾缓出曲线（比普通 ease-out 尾段更长更安静，绝不生硬刹停） */
function silkyOut(t) {
  const x = clamp(t, 0, 1);
  return 1 - Math.pow(1 - x, 4.2);
}

/** 优雅 S 型电影运镜曲线（起停皆零加速度） */
function cinemaEase(t) {
  const x = clamp(t, 0, 1);
  return x < 0.5
    ? 8 * x * x * x * x
    : 1 - Math.pow(-2 * x + 2, 4) * 0.5;
}

/** 真实单次拨弦阻尼震荡（快速衰减归零，绝非永动循环） */
function pluckedStringWave(dt, amp = 0.28, freq = 26.0, decay = 6.2) {
  if (dt <= 0) return 0;
  return amp * Math.exp(-dt * decay) * Math.sin(dt * freq);
}

function makeBody(pos, size, rot, col, mat) {
  return { pos, size, rot, col, mat };
}

function hiddenBody() {
  return makeBody([0, -20, 0], [0.001, 0.001, 0.001, 0.001], [0, 0, 0], [0, 0, 0], [0, 0.5, 0, 0]);
}

export function evaluateScene(rawTime, userCtrl, audioEnergy = 0) {
  const t = ((rawTime % TOTAL_DURATION) + TOTAL_DURATION) % TOTAL_DURATION;

  const orbitYaw = userCtrl ? userCtrl.yaw : 0;
  const orbitPitch = userCtrl ? userCtrl.pitch : 0;
  const pullX = userCtrl && userCtrl.down ? (userCtrl.px - 0.5) * 1.15 : 0;
  const pullY = userCtrl && userCtrl.down ? (0.5 - userCtrl.py) * 0.95 : 0;

  const scene = {
    t,
    shotIndex: 0,
    camPos: [0, 0.3, 4.0],
    camTarget: [0, 0.0, 0],
    camFov: 1.65,
    camRoll: 0.0,

    // 贯穿全片的 3D 质感丝线（二次贝塞尔曲线 A -> B -> C）
    thread: {
      enabled: 1.0,
      a: [-4.5, 0.0, 0.0],
      b: [0.0, 0.0, 0.0],
      c: [4.5, 0.0, 0.0],
      radius: 0.013,
      col: [0.96, 0.89, 0.80],
      glow: 0.35,
      fuseWithBodies: 0.0, // 第三段让丝线自然融入融球液桥！
    },

    studio: {
      bgTop: [0.020, 0.022, 0.028],
      bgBot: [0.050, 0.044, 0.046],
      floorY: -1.15,
      floorEnabled: 1.0,
      floorRough: 0.32,
      lightAngle: 0.35,
      lightIntensity: 1.15,
      backdropMode: 0.0,
      causticsStrength: 0.0,
      rimOnly: 0.0,
      redCorePos: [0, -20, 0],
      redCoreStrength: 0.0,
    },

    smoothK: 0.0,
    bodies: [hiddenBody(), hiddenBody(), hiddenBody(), hiddenBody(), hiddenBody()],

    // 全片前 37.5 秒完全无字；仅末尾克制浮现 Mochi
    overlay: {
      mode: 'none',
      text: '',
      alpha: 0.0,
      blur: 0.0,
      scale: 1.0,
    },

    post: {
      bloom: 0.24,
      chromaPunch: 0.0,
      vignette: 0.30,
      grain: 0.022,
      blackout: 0.0,
      exposure: 1.05,
    },
  };

  // =========================================================================
  // 第一段 (0.0s – 7.0s)：悬丝与蓄力
  // 暗棚中一根质感丝线横贯画面；温润 3D 麻薯轻轻压落，拉出真实悬链张力弧度，停顿蓄满
  // =========================================================================
  if (t < 7.0) {
    scene.shotIndex = 0;
    const lt = t;
    const fadeIn = silkyOut(remap(lt, 0.0, 1.3));

    // 极缓、极稳的单一滑移运镜（绝不摇晃）
    const camSlide = silkyOut(remap(lt, 0.0, 6.6));
    const yaw = lerp(-0.26, 0.14, camSlide) + orbitYaw;
    const dist = lerp(3.15, 3.65, camSlide);
    scene.camPos = [Math.sin(yaw) * dist, lerp(0.18, 0.36, camSlide) + orbitPitch * 1.1, Math.cos(yaw) * dist];
    scene.camTarget = [0, -0.05, 0];
    scene.camFov = 1.72;

    scene.studio.bgTop = [0.018, 0.020, 0.026];
    scene.studio.bgBot = [0.052, 0.043, 0.042];
    scene.studio.floorY = -1.25;
    scene.studio.floorRough = 0.28;
    // 棚拍长条光带柔和滑过曲率一次即停稳
    scene.studio.lightAngle = lerp(-1.15, 0.68, silkyOut(remap(lt, 0.4, 5.4)));
    scene.studio.lightIntensity = lerp(0.7, 1.25, fadeIn);

    // 0.0 - 1.4s 丝线从左向右轻盈抽直；1.8s 温润麻薯落至丝线中央将其下压成悬链张力弧！
    const lineDraw = silkyOut(remap(lt, 0.1, 1.55));
    const dropApproach = silkyOut(remap(lt, 1.7, 3.5));
    const heavyPress = cinemaEase(remap(lt, 3.3, 5.9));
    // 5.9 - 6.75s 屏息蓄力静止，6.75 - 7.0s 临界下压准备弹射！
    const finalLoad = smootherstep(6.35, 6.95, lt);

    const bendY = -(heavyPress * 0.78 + finalLoad * 0.18) + pullY * 0.45;
    scene.thread.a = [-4.6, 0.12, 0.0];
    scene.thread.b = [pullX * 0.4, 0.12 + bendY * 1.65, 0.0];
    scene.thread.c = [lerp(-4.4, 4.6, lineDraw), 0.12, 0.0];
    scene.thread.radius = lerp(0.014, 0.010, heavyPress); // 绷紧时丝线略微拉细，极具真实张力感！
    scene.thread.col = [0.97, 0.88, 0.78];
    scene.thread.glow = 0.32 + finalLoad * 0.45;

    // 温润奶油麻薯沿曲线缓缓压住丝线并产生底部柔软形变（走完即稳稳停住，绝不乱晃！）
    const mY = lerp(2.35, 0.56, dropApproach) + bendY * 0.72;
    const pressSquash = heavyPress * 0.18 + finalLoad * 0.14;
    const sy = 1.0 - pressSquash;
    const sxz = 1.0 / Math.sqrt(sy);

    scene.bodies[0] = makeBody(
      [pullX * 0.35, mY, 0.0],
      [0.64 * sxz, 0.56 * sy, 0.64 * sxz, 0.34],
      [0.08 * heavyPress, lerp(-0.35, 0.12, silkyOut(remap(lt, 1.7, 5.4))), 0.0],
      [0.97, 0.92, 0.87],
      [0.0, 0.24, 0.0, 0.0],
    );

    scene.post.blackout = 1.0 - fadeIn;
    scene.post.vignette = 0.34;
  }

  // =========================================================================
  // 第二段 (7.0s – 14.8s)：拨弦与串行
  // 丝线弹射瞬间照亮暖白无影棚！丝线如真实拨弦快速阻尼归静；
  // 三颗 3D 麻薯沿斜穿丝线依次滑入、轻碰压扁、经一次欠阻尼弹簧优雅停稳（致敬三团串！）
  // =========================================================================
  else if (t < 14.8) {
    scene.shotIndex = 1;
    const lt = t - 7.0; // 0 .. 7.8s

    // 高明度美术馆暖米纸无影棚
    scene.studio.bgTop = [0.95, 0.89, 0.82];
    scene.studio.bgBot = [0.89, 0.80, 0.71];
    scene.studio.floorY = -1.08;
    scene.studio.floorRough = 0.58;
    scene.studio.lightAngle = lerp(0.22, 0.64, silkyOut(remap(lt, 0.0, 6.2)));
    scene.studio.lightIntensity = 1.12;

    // 沉稳优雅的单次推拉运镜
    const camEase = silkyOut(remap(lt, 0.0, 6.4));
    const yaw = lerp(-0.22, 0.18, camEase) + orbitYaw;
    scene.camPos = [Math.sin(yaw) * 4.45, lerp(0.28, 0.52, camEase) + orbitPitch * 1.1, Math.cos(yaw) * 4.45];
    scene.camTarget = [0.0, -0.05, 0.0];
    scene.camFov = 1.68;

    // 丝线斜穿画面 (-16°)，在 t=0 释放一次真实阻尼拨弦波，0.9s 内优雅归静成笔直张力线！
    const pluckY = pluckedStringWave(lt, 0.52, 24.0, 4.6);
    const slope = 0.24; // 从左下斜穿至右上，像一把悬浮在光中的串签丝线
    scene.thread.a = [-4.8, -4.8 * slope, 0.0];
    scene.thread.b = [pullX * 0.5, pluckY + pullY * 0.45, 0.0];
    scene.thread.c = [4.8, 4.8 * slope, 0.0];
    scene.thread.radius = 0.015;
    scene.thread.col = [0.46, 0.28, 0.20]; // 暖棚中丝线呈深焦糖丝线质感，高雅锐利！
    scene.thread.glow = 0.0;

    // 三颗 3D 麻薯沿斜丝线方向依次以真实 Bounce 弹簧滑入落位，随后静静停稳 1.5s，最后 6.3s 轻缓滑向中心准备融球
    const sp0 = springEval(lt - 0.12, MOCHI_TOKENS.Bounce);
    const sp1 = springEval(lt - 0.42, MOCHI_TOKENS.Bounce);
    const sp2 = springEval(lt - 0.72, MOCHI_TOKENS.Bounce);
    const gather = cinemaEase(remap(lt, 6.15, 7.75));

    const targets = [-1.18, 0.0, 1.18];
    const starts  = [-4.2, -3.4, 4.0];
    const springs = [sp0, sp1, sp2];
    const colors  = [
      [0.86, 0.43, 0.29], // 焦糖陶橘
      [0.98, 0.95, 0.91], // 温润乳白
      [0.91, 0.71, 0.37], // 蜂蜜暖金
    ];
    const baseSizes = [0.50, 0.58, 0.50];

    for (let i = 0; i < 3; i++) {
      const sp = springs[i];
      const baseX = lerp(starts[i], targets[i], sp.value);
      const x = lerp(baseX, targets[i] * 0.36, gather) + (i === 1 ? pullX * 0.55 : pullX * 0.15);
      const y = x * slope + (i === 1 ? pluckY * 0.35 + pullY * 0.25 : pluckY * 0.15);

      // 沿速度方向果冻拉伸，速度归零后彻底恢复标准 G2 形态，绝不无故抖动！
      const vel = Math.abs(sp.velocity * (targets[i] - starts[i])) * 0.28;
      const sx = jellyStretch(vel, 0.58);
      const syz = jellyCrossScale(sx);
      const sz = baseSizes[i];

      scene.bodies[i] = makeBody(
        [x, y, 0.0],
        [sz * 1.06 * sx, sz * 0.94 * syz, sz * 0.96 * syz, sz * 0.56],
        [0.0, 0.0, Math.atan(slope)], // 精确顺着斜丝线轴向对齐！
        colors[i],
        [0.0, i === 1 ? 0.24 : 0.30, 0.0, 0.0],
      );
    }

    scene.smoothK = gather * 0.35;
    scene.post.vignette = 0.12;
    scene.post.bloom = 0.14;
    scene.post.exposure = 1.02;
  }

  // =========================================================================
  // 第三段 (14.8s – 22.4s)：丝线化液 —— 3D 融球液桥生长与优雅拉细回归
  // 暗曜石无影棚：两颗雕塑沿丝线滑近，丝线本身与两球平滑融合为粗壮晶莹的 3D 液桥，
  // 随后沿丝线方向缓缓拉开、拉丝变细、干净利落地回归纤细丝线！
  // =========================================================================
  else if (t < 22.4) {
    scene.shotIndex = 2;
    const lt = t - 14.8; // 0 .. 7.6s

    scene.studio.bgTop = [0.020, 0.022, 0.029];
    scene.studio.bgBot = [0.046, 0.042, 0.052];
    scene.studio.floorEnabled = 0.0;
    scene.studio.lightAngle = lerp(-0.55, 0.85, cinemaEase(remap(lt, 0.0, 7.2)));
    scene.studio.lightIntensity = 1.32;

    // 单次优雅绕弧运镜（无来回摇摆）
    const camP = cinemaEase(remap(lt, 0.0, 7.4));
    const yaw = lerp(-0.48, 0.42, camP) + orbitYaw;
    const dist = lerp(3.75, 3.25, silkyOut(remap(lt, 0.0, 4.5)));
    scene.camPos = [Math.sin(yaw) * dist, lerp(0.32, 0.08, camP) + orbitPitch * 1.2, Math.cos(yaw) * dist];
    scene.camTarget = [0, 0, 0];
    scene.camFov = 1.62;

    // 一次完整极具呼吸张力的物理叙事：合拢融桥 (0..2.2s) -> 静谧停顿凝视 (2.2..3.6s) -> 缓缓拉开拉丝 (3.6..6.2s) -> 透明相变准备入第四段 (6.2..7.6s)
    const pullOut = cinemaEase(remap(lt, 3.4, 6.2));
    const approach = silkyOut(remap(lt, 0.0, 2.0));
    const sep = lerp(0.92, 0.62, approach) + pullOut * 0.78;

    // 丝线半径在合拢时自然增粗融入液桥，拉开时再度收细为纤毫丝线！
    const fuseBell = smoothstep(0.0, 1.6, lt) * (1.0 - smoothstep(4.6, 6.3, lt));
    const glassTrans = smootherstep(6.2, 7.55, lt);

    scene.thread.a = [-4.8, 0.0, 0.0];
    scene.thread.b = [pullX * 0.35, pullY * 0.35, 0.0];
    scene.thread.c = [4.8, 0.0, 0.0];
    scene.thread.radius = lerp(0.013, 0.052, fuseBell);
    scene.thread.col = [0.96, 0.88, 0.80];
    scene.thread.glow = 0.25;
    scene.thread.fuseWithBodies = fuseBell * 0.92;

    // 左球：焦糖陶橘雕塑胶囊
    scene.bodies[0] = makeBody(
      [-sep + pullX * 0.35, pullY * 0.15, 0.0],
      [0.62, 0.48, 0.48, 0.40],
      [0.0, lerp(-0.18, 0.12, camP), 0.0],
      [0.88, 0.44, 0.29],
      [glassTrans, lerp(0.18, 0.02, glassTrans), 0.0, 0.0],
    );

    // 右球：羊脂乳白陶瓷雕塑胶囊
    scene.bodies[1] = makeBody(
      [sep - pullX * 0.2, -pullY * 0.15, 0.0],
      [0.68, 0.52, 0.52, 0.42],
      [0.0, lerp(0.22, -0.14, camP), 0.0],
      [0.97, 0.95, 0.91],
      [glassTrans, lerp(0.14, 0.02, glassTrans), 0.0, 0.0],
    );

    scene.smoothK = 0.56 * (1.0 - glassTrans * 0.35);
    scene.studio.backdropMode = glassTrans * 0.7;
    scene.post.vignette = 0.32;
    scene.post.bloom = 0.24;
  }

  // =========================================================================
  // 第四段 (22.4s – 31.0s)：穿镜折光 —— 笔直质感丝线穿过厚实 3D 液态玻璃！
  // 最令人屏息的光学时刻：一根发光丝绸暖白丝线斜穿深邃暗棚；
  // 一枚晶莹厚实的 3D 液态玻璃超椭圆沿优雅单次曲线缓缓滑过丝线上方并稳稳停驻，
  // 折射光线真实追踪背后丝线，令丝线在玻璃内部清晰折断、错位放大并析出克制冷暖色散！
  // =========================================================================
  else if (t < 31.0) {
    scene.shotIndex = 3;
    const lt = t - 22.4; // 0 .. 8.6s

    scene.studio.bgTop = [0.022, 0.028, 0.042];
    scene.studio.bgBot = [0.044, 0.052, 0.072];
    scene.studio.floorY = -1.12;
    scene.studio.floorEnabled = 1.0;
    scene.studio.floorRough = 0.18;
    scene.studio.backdropMode = 0.55; // 克制深邃的建筑光柱背墙，不喧宾夺主，衬托丝线折射！
    scene.studio.causticsStrength = silkyOut(remap(lt, 0.2, 2.0)) * 0.92;
    scene.studio.lightAngle = lerp(-0.35, 0.65, cinemaEase(remap(lt, 0.0, 7.8)));
    scene.studio.lightIntensity = 1.38;

    // 极缓慢稳重的横移凝视镜头
    const pan = cinemaEase(remap(lt, 0.0, 8.2));
    const yaw = lerp(-0.32, 0.30, pan) + orbitYaw;
    scene.camPos = [Math.sin(yaw) * 3.35, lerp(0.22, 0.34, pan) + orbitPitch * 1.1, Math.cos(yaw) * 3.35];
    scene.camTarget = [0, 0.02, 0];
    scene.camFov = 1.68;

    // 丝线居于玻璃正后方 (z = -0.62)，斜穿整个画面，泛着温润高辨识度的丝绸金白光泽
    scene.thread.a = [-4.6, -0.95, -0.62];
    scene.thread.b = [pullX * 0.3, pullY * 0.3, -0.62];
    scene.thread.c = [4.6, 0.95, -0.62];
    scene.thread.radius = 0.022;
    scene.thread.col = [1.0, 0.84, 0.68];
    scene.thread.glow = 0.92; // 令丝线在厚玻璃背后清晰发光、折射折断效果震撼无比！
    scene.thread.fuseWithBodies = 0.0;

    // 3D 厚实液态玻璃胶囊由左至中优雅滑入，略微转向让光学折射角度缓缓掠过丝线，随后稳稳停驻凝视！
    const slideIn = silkyOut(remap(lt, 0.0, 3.8));
    const tiltSweep = cinemaEase(remap(lt, 3.2, 7.6));
    const gx = lerp(-0.85, 0.08, slideIn) + pullX * 0.55;
    const gy = lerp(-0.12, 0.04, slideIn) + pullY * 0.42;

    scene.bodies[0] = makeBody(
      [gx, gy, 0.15],
      [1.06, 0.48, 0.44, 0.40],
      [lerp(0.18, -0.14, tiltSweep), lerp(-0.36, 0.34, tiltSweep), lerp(-0.08, 0.05, tiltSweep)],
      [0.95, 0.98, 1.0],
      [1.0, 0.02, 0.0, 0.0],
    );

    // 右上方一枚小玻璃珠沿单次弹簧优雅贴近并轻融于主透镜边缘，随后停稳
    const satSlide = silkyOut(remap(lt, 1.4, 5.2));
    scene.bodies[1] = makeBody(
      [lerp(2.15, 0.94, satSlide), lerp(0.55, 0.22, satSlide), 0.15],
      [0.32, 0.32, 0.32, 0.28],
      [0.0, 0.0, 0.0],
      [0.94, 0.97, 1.0],
      [1.0, 0.02, 0.0, 0.0],
    );

    scene.smoothK = 0.34;
    scene.post.bloom = 0.28;
    scene.post.vignette = 0.24;
    scene.post.exposure = 1.12;
  }

  // =========================================================================
  // 第五段 (31.0s – 37.2s)：0.36s 真空静默 → 沿丝线三记干净利落的材质静切
  //   不再急促鬼畜闪切！每一境给足 1.85 秒呼吸与单次精准位移停稳：
  //   境 A (Nothing 喷砂黑微孔 + 丝线末端凝结信号红珠) -> 境 B (暖白麻薯软陶) -> 境 C (深蓝液态玻璃)
  // =========================================================================
  else if (t < 37.2) {
    scene.shotIndex = 4;
    const lt = t - 31.0; // 0 .. 6.2s

    if (lt < 0.36) {
      // 0.36 秒绝对真空静默：漆黑空间里只有一根极细发光丝线与雕塑边缘一道锋利逆光
      scene.camPos = [0, 0.12, 3.45];
      scene.camTarget = [0, 0, 0];
      scene.camFov = 1.68;
      scene.studio.bgTop = [0.004, 0.004, 0.006];
      scene.studio.bgBot = [0.007, 0.007, 0.010];
      scene.studio.floorEnabled = 0.0;
      scene.studio.rimOnly = 1.0;
      scene.studio.lightIntensity = 0.8;

      scene.thread.a = [-4.5, 0.0, 0.0];
      scene.thread.b = [0.0, 0.0, 0.0];
      scene.thread.c = [4.5, 0.0, 0.0];
      scene.thread.radius = 0.009;
      scene.thread.col = [1.0, 0.95, 0.90];
      scene.thread.glow = 0.65;

      scene.bodies[0] = makeBody([0, 0, 0], [0.84, 0.54, 0.54, 0.38], [0.1, 0.22, 0.0], [0.95, 0.94, 0.92], [0.0, 0.22, 0.0, 0.0]);
      scene.post.vignette = 0.52;
    } else {
      const mT = lt - 0.36; // 0 .. 5.84s，三段各 ~1.95s，舒展沉稳、一镜到底质感！
      const segDur = 1.94;
      const segIdx = Math.min(2, Math.floor(mT / segDur));
      const segP = clamp((mT - segIdx * segDur) / segDur, 0, 1);
      const move = silkyOut(segP); // 单次滑移到位即优雅停稳！
      const punch = Math.exp(-segP * 14.0);
      scene.post.chromaPunch = punch * 0.32;

      // 丝线始终稳定横贯中央，作为视觉锚点连接三套截然不同的物理世界
      scene.thread.a = [-4.8, 0.0, 0.0];
      scene.thread.b = [pullX * 0.25, pullY * 0.25, 0.0];
      scene.thread.c = [4.8, 0.0, 0.0];
      scene.thread.radius = 0.012;

      if (segIdx === 0) {
        // 境 A：Nothing 工业喷砂炭黑微孔雕塑 + 丝线右端凝结一颗刺眼信号红珠 (#D71921)
        const yaw = lerp(-0.28, 0.14, move) + orbitYaw;
        scene.camPos = [Math.sin(yaw) * 3.15, lerp(0.32, 0.18, move) + orbitPitch, Math.cos(yaw) * 3.15];
        scene.camTarget = [0.08, 0.0, 0.0];
        scene.studio.bgTop = [0.012, 0.012, 0.016];
        scene.studio.bgBot = [0.030, 0.030, 0.036];
        scene.studio.floorY = -1.05;
        scene.studio.floorRough = 0.30;
        scene.studio.lightIntensity = 1.25;

        const redX = lerp(1.55, 0.78, move);
        scene.studio.redCorePos = [redX, 0.0, 0.0];
        scene.studio.redCoreStrength = 1.85;

        scene.thread.col = [0.95, 0.16, 0.18]; // 丝线被信号红点亮！
        scene.thread.glow = 0.55;

        scene.bodies[0] = makeBody([-0.26, 0.0, 0.0], [0.78, 0.54, 0.54, 0.34], [0.10, lerp(-0.18, 0.12, move), 0.0], [0.085, 0.09, 0.105], [0.0, 0.46, 0.0, 1.0]);
        scene.bodies[1] = makeBody([redX, 0.0, 0.0], [0.22, 0.22, 0.22, 0.22], [0, 0, 0], [0.96, 0.09, 0.12], [0.0, 0.12, 2.5, 0.0]);
        scene.smoothK = 0.0;
        scene.post.bloom = 0.36;
        scene.post.vignette = 0.38;
      } else if (segIdx === 1) {
        // 境 B：高明度暖白无影棚 + 陶橘/乳白温润软陶
        const yaw = lerp(0.34, -0.10, move) + orbitYaw;
        scene.camPos = [Math.sin(yaw) * 3.45, lerp(0.56, 0.24, move) + orbitPitch, Math.cos(yaw) * 3.45];
        scene.camTarget = [0.0, 0.0, 0.0];
        scene.studio.bgTop = [0.95, 0.89, 0.82];
        scene.studio.bgBot = [0.88, 0.78, 0.69];
        scene.studio.floorY = -1.05;
        scene.studio.floorRough = 0.56;
        scene.studio.lightIntensity = 1.15;

        scene.thread.col = [0.48, 0.28, 0.20];
        scene.thread.glow = 0.0;

        const sep = lerp(1.15, 0.68, move);
        scene.bodies[0] = makeBody([-sep * 0.55, 0.0, 0.0], [0.68, 0.52, 0.52, 0.36], [0.06, lerp(0.2, -0.05, move), 0.0], [0.98, 0.95, 0.91], [0.0, 0.26, 0.0, 0.0]);
        scene.bodies[1] = makeBody([sep * 0.72, 0.0, 0.0], [0.46, 0.46, 0.46, 0.34], [0, 0, 0], [0.86, 0.43, 0.29], [0.0, 0.30, 0.0, 0.0]);
        scene.smoothK = 0.42;
        scene.post.vignette = 0.14;
      } else {
        // 境 C：深海冰川液态玻璃 + 丝线穿透折光
        const yaw = lerp(-0.22, 0.16, move) + orbitYaw;
        scene.camPos = [Math.sin(yaw) * 3.25, lerp(0.18, 0.30, move) + orbitPitch, Math.cos(yaw) * 3.25];
        scene.camTarget = [0.0, 0.0, 0.0];
        scene.studio.bgTop = [0.025, 0.040, 0.068];
        scene.studio.bgBot = [0.052, 0.078, 0.125];
        scene.studio.floorY = -1.05;
        scene.studio.floorRough = 0.16;
        scene.studio.backdropMode = 0.65;
        scene.studio.causticsStrength = 0.95;
        scene.studio.lightIntensity = 1.38;

        scene.thread.a = [-4.6, -0.45, -0.58];
        scene.thread.b = [0.0, 0.0, -0.58];
        scene.thread.c = [4.6, 0.45, -0.58];
        scene.thread.radius = 0.020;
        scene.thread.col = [0.98, 0.92, 0.84];
        scene.thread.glow = 0.85;

        scene.bodies[0] = makeBody([0.0, 0.0, 0.12], [0.88, 0.50, 0.46, 0.38], [0.12, lerp(-0.25, 0.18, move), 0.0], [0.93, 0.97, 1.0], [1.0, 0.02, 0.0, 0.0]);
        scene.smoothK = 0.0;
        scene.post.vignette = 0.24;
      }

      scene.post.blackout = smootherstep(5.45, 5.84, mT) * 0.96;
    }
  }

  // =========================================================================
  // 第六段 (37.2s – 42.0s)：归静落版
  // 丝线温柔绷直穿过清透玻璃超椭圆与温润暖橘内芯，单次缓慢落位后完全安定，底部极轻浮现 Mochi
  // =========================================================================
  else {
    scene.shotIndex = 5;
    const lt = t - 37.2; // 0 .. 4.8s
    const fadeIn = silkyOut(remap(lt, 0.0, 1.25));
    const fadeOut = smootherstep(4.15, 4.8, lt);
    const settle = silkyOut(remap(lt, 0.0, 2.8));

    const yaw = lerp(-0.18, 0.06, settle) + orbitYaw;
    scene.camPos = [Math.sin(yaw) * 3.85, 0.24 + orbitPitch * 0.8, Math.cos(yaw) * 3.85];
    scene.camTarget = [0, -0.14, 0];
    scene.camFov = 1.65;

    scene.studio.bgTop = [0.018, 0.020, 0.026];
    scene.studio.bgBot = [0.048, 0.040, 0.042];
    scene.studio.floorY = -1.18;
    scene.studio.floorRough = 0.26;
    scene.studio.lightAngle = lerp(-0.15, 0.32, settle);
    scene.studio.lightIntensity = 1.25;
    scene.studio.causticsStrength = 0.55;

    // 贯穿全片的丝线静静归位为水平细光，从液态玻璃正后方温柔穿过
    scene.thread.a = [-4.6, 0.18, -0.58];
    scene.thread.b = [0.0, 0.18, -0.58];
    scene.thread.c = [4.6, 0.18, -0.58];
    scene.thread.radius = 0.014;
    scene.thread.col = [0.98, 0.86, 0.74];
    scene.thread.glow = 0.65;

    // 标志性圆润 G2 液态玻璃超椭圆（走完一次优雅旋转即静静停稳）
    scene.bodies[0] = makeBody(
      [0.0, 0.18, 0.12],
      [0.64, 0.64, 0.40, 0.34],
      [lerp(0.14, 0.04, settle), lerp(-0.32, 0.14, settle), 0.0],
      [0.97, 0.98, 1.0],
      [1.0, 0.02, 0.0, 0.0],
    );

    // 玻璃后方一颗温润陶橘内芯（静止柔和，绝不晃动）
    scene.bodies[1] = makeBody(
      [0.0, 0.18, -0.82],
      [0.28, 0.28, 0.28, 0.26],
      [0, 0, 0],
      [0.92, 0.46, 0.30],
      [0.0, 0.24, 0.65, 0.0],
    );

    scene.smoothK = 0.0;
    scene.post.blackout = Math.max(1.0 - fadeIn, fadeOut);
    scene.post.vignette = 0.36;
    scene.post.bloom = 0.28;

    // 全片唯一落版字标：极轻、极稳、极克制
    const textIn = silkyOut(remap(lt, 0.55, 2.0));
    scene.overlay = {
      mode: 'lockup',
      text: 'Mochi',
      alpha: textIn * (1.0 - fadeOut),
      blur: (1.0 - textIn) * 8.0,
      scale: lerp(0.97, 1.0, textIn),
    };
  }

  return scene;
}
