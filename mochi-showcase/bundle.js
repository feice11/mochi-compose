(function () {
"use strict";


// ==================== physics.js ====================

/**
 * Mochi 真实物理与运动力学库
 * 精确还原 MochiMotionTokens、二阶弹簧、iOS 橡皮筋、速度果冻与融球融合系数。
 */

const MOCHI_TOKENS = {
  Page:         { response: 0.42, damping: 1.00 },
  Present:      { response: 0.42, damping: 0.86 },
  Pop:          { response: 0.36, damping: 0.78 },
  Control:      { response: 0.34, damping: 0.86 },
  Settle:       { response: 0.28, damping: 1.00 },
  Bounce:       { response: 0.34, damping: 0.55 },
  PressDown:    { response: 0.22, damping: 0.62 },
  PressRelease: { response: 0.38, damping: 0.62 },
};

function clamp(x, min = 0, max = 1) {
  return x < min ? min : x > max ? max : x;
}

function lerp(a, b, t) {
  return a + (b - a) * t;
}

function smoothstep(edge0, edge1, x) {
  const t = clamp((x - edge0) / (edge1 - edge0), 0, 1);
  return t * t * (3 - 2 * t);
}

function smootherstep(edge0, edge1, x) {
  const t = clamp((x - edge0) / (edge1 - edge0), 0, 1);
  return t * t * t * (t * (t * 6 - 15) + 10);
}

function remap(x, inMin, inMax, outMin = 0, outMax = 1) {
  return lerp(outMin, outMax, clamp((x - inMin) / (inMax - inMin), 0, 1));
}

/** 电影节级非对称指数急停曲线 */
function expoSnap(t) {
  const x = clamp(t, 0, 1);
  if (x === 0 || x === 1) return x;
  return x < 0.5
    ? Math.pow(2, 20 * x - 10) * 0.5
    : (2 - Math.pow(2, -20 * x + 10)) * 0.5;
}

function expoOut(t) {
  const x = clamp(t, 0, 1);
  return x === 1 ? 1 : 1 - Math.pow(2, -10 * x);
}

function expoIn(t) {
  const x = clamp(t, 0, 1);
  return x === 0 ? 0 : Math.pow(2, 10 * x - 10);
}

function quintOut(t) {
  const x = 1 - clamp(t, 0, 1);
  return 1 - x * x * x * x * x;
}

/**
 * 确定性二阶弹簧解析求解（给定触发后经过的秒数 dt，返回 { value, velocity }）
 * 支持任意拖动时间轴和精准逐帧渲染，绝不依赖历史状态漂移。
 */
function springEval(dt, token = MOCHI_TOKENS.Bounce) {
  if (dt <= 0) return { value: 0, velocity: 0 };
  const omega = (2 * Math.PI) / Math.max(token.response, 0.01);
  const zeta = token.damping;

  if (zeta < 0.999) {
    // 欠阻尼：带真实物理回弹
    const omegaD = omega * Math.sqrt(1 - zeta * zeta);
    const decay = Math.exp(-zeta * omega * dt);
    const cosT = Math.cos(omegaD * dt);
    const sinT = Math.sin(omegaD * dt);
    const value = 1 - decay * (cosT + (zeta * omega / omegaD) * sinT);
    const velocity = decay * ((omega * omega) / omegaD) * sinT;
    return { value, velocity };
  } else {
    // 临界阻尼：稳稳停住不回弹
    const decay = Math.exp(-omega * dt);
    const value = 1 - decay * (1 + omega * dt);
    const velocity = decay * omega * omega * dt;
    return { value, velocity };
  }
}

/**
 * 2 帧 Hit-Stop（撞击停顿）时间整形器
 * 在指定冲击时刻停留 hold 秒再骤然释放，赋予排印与形变强烈实体重量感。
 */
function applyHitStop(t, impactTime, holdSec = 0.036) {
  if (t < impactTime) return t;
  if (t < impactTime + holdSec) return impactTime;
  return t - holdSec;
}

/**
 * iOS UIScrollView 橡皮筋越界阻尼公式
 * 刚拉出去 1:1 跟手，越远越拉不动，软上限 maxPull。
 */
function rubberBand(overshoot, dimension, maxPull) {
  const sign = Math.sign(overshoot);
  const abs = Math.abs(overshoot);
  const c = 0.55;
  const d = Math.max(dimension, 1);
  const m = Math.max(maxPull, 1);
  const res = (1 - 1 / ((abs * c) / d + 1)) * m;
  return sign * res;
}

/**
 * Mochi 速度果冻形变（沿运动方向拉长，垂直方向面积守恒收窄）
 */
function jellyStretch(velocity, gel = 0.55) {
  const v = clamp(Math.abs(velocity), 0, 5.0);
  return 1 + Math.tanh(v * 0.34) * gel * 0.42;
}

function jellyCrossScale(stretch) {
  return 1 / Math.sqrt(Math.max(stretch, 0.4));
}

/**
 * Mochi 融球距离融合系数 proximityFuse
 */
function proximityFuse(dist, restGap) {
  if (dist >= restGap) return 0;
  return smoothstep(1, 0, clamp(dist / Math.max(restGap, 1e-4), 0, 1));
}

/**
 * MochiFollowSpring：用于实时鼠标/触控指针的二维跟手弹簧
 */
class MochiFollowSpring {
  constructor(response = 0.28, damping = 0.78) {
    this.response = response;
    this.damping = damping;
    this.x = 0.5;
    this.y = 0.5;
    this.vx = 0;
    this.vy = 0;
  }

  snap(x, y) {
    this.x = x;
    this.y = y;
    this.vx = 0;
    this.vy = 0;
  }

  step(targetX, targetY, dt) {
    const h = clamp(dt, 0.001, 0.04);
    const omega = (2 * Math.PI) / Math.max(this.response, 0.01);
    const k = omega * omega;
    const c = 2 * this.damping * omega;

    const ax = k * (targetX - this.x) - c * this.vx;
    const ay = k * (targetY - this.y) - c * this.vy;

    this.vx += ax * h;
    this.vy += ay * h;
    this.x += this.vx * h;
    this.y += this.vy * h;

    return { x: this.x, y: this.y, vx: this.vx, vy: this.vy };
  }
}

/**
 * Mochi 指尖按压柔光物理衰减曲线：0.85 * exp(-r^3 * 1.6)
 */
function mochiGlowCurve(rNorm) {
  const r = Math.max(0, rNorm);
  return 0.85 * Math.exp(-r * r * r * 1.6);
}


// ==================== director.js ====================

/**
 * 《Mochi》42 秒全 3D 质感丝线一镜贯穿导演引擎
 *
 * 创作原则：
 * - 一根 3D 丝绸质感丝线从头至尾贯穿全片：绷紧 -> 被压弯 -> 拨弦回弹 -> 化作融球液桥 -> 穿过 3D 液态玻璃光学折断 -> 凝结信号红珠 -> 归静落版；
 * - 零廉价 sin(t) 永动呼吸、零原地匀速自转；每个动作皆遵循「蓄力 -> 释放 -> 丝滑长减速 -> 真正静止」的电影级曲线；
 * - 全片前 37.5 秒零任何文字，仅结尾极轻浮现唯一落版 Mochi。
 */


const TOTAL_DURATION = 42.0;

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

function evaluateScene(rawTime, userCtrl, audioEnergy = 0) {
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


// ==================== studio_shader.js ====================

/**
 * 《Mochi》全屏实时 3D 摄影棚 PBR 光线步进着色器（极速零冗余内联版 · GLSL 300 es）
 * 全平台（ANGLE D3D11 / SwiftShader / Metal / Vulkan）毫秒级编译链接，稳定 60fps。
 */

const FULLSCREEN_VS = `#version 300 es
precision highp float;
const vec2 POS[3] = vec2[3](
  vec2(-1.0, -1.0),
  vec2( 3.0, -1.0),
  vec2(-1.0,  3.0)
);
out vec2 vUv;
void main() {
  vec2 p = POS[gl_VertexID];
  vUv = p * 0.5 + 0.5;
  gl_Position = vec4(p, 0.0, 1.0);
}
`;

const STUDIO_3D_FS = `#version 300 es
precision highp float;

in vec2 vUv;
out vec4 fragColor;

uniform vec2 u_res;
uniform float u_time;

uniform vec3 u_camPos;
uniform vec3 u_camTarget;
uniform float u_camFov;
uniform float u_camRoll;

uniform float u_threadEnabled;
uniform vec3 u_threadA;
uniform vec3 u_threadB;
uniform vec3 u_threadC;
uniform float u_threadRadius;
uniform vec3 u_threadCol;
uniform float u_threadGlow;
uniform float u_threadFuse;

uniform vec3 u_bgTop;
uniform vec3 u_bgBot;
uniform float u_floorY;
uniform float u_floorEnabled;
uniform float u_floorRough;
uniform float u_lightAngle;
uniform float u_lightIntensity;
uniform float u_backdropMode;
uniform float u_causticsStrength;
uniform float u_rimOnly;
uniform vec3 u_redCorePos;
uniform float u_redCoreStrength;

uniform float u_smoothK;

uniform vec3 u_pos0; uniform vec4 u_size0; uniform mat3 u_rot0; uniform vec3 u_col0; uniform vec4 u_mat0;
uniform vec3 u_pos1; uniform vec4 u_size1; uniform mat3 u_rot1; uniform vec3 u_col1; uniform vec4 u_mat1;
uniform vec3 u_pos2; uniform vec4 u_size2; uniform mat3 u_rot2; uniform vec3 u_col2; uniform vec4 u_mat2;

float sdRoundBox(vec3 p, vec3 b, float r) {
  float rad = min(r, min(b.x, min(b.y, b.z)) * 0.96);
  vec3 q = abs(p) - (b - vec3(rad));
  return length(max(q, 0.0)) + min(max(q.x, max(q.y, q.z)), 0.0) - rad;
}

float sminF(float a, float b, float k) {
  float h = clamp(0.5 + 0.5 * (b - a) / max(k, 0.0001), 0.0, 1.0);
  return mix(b, a, h) - k * h * (1.0 - h);
}

vec2 sminW(float a, float b, float k) {
  if (k <= 0.001) return a < b ? vec2(a, 0.0) : vec2(b, 1.0);
  float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
  return vec2(mix(b, a, h) - k * h * (1.0 - h), 1.0 - h);
}

// 极简 O(1) 二次贝塞尔丝线距离（单次投影，极快且丝滑）
float sdThreadFast(vec3 p) {
  vec3 ac = u_threadC - u_threadA;
  float s = clamp(dot(p - u_threadA, ac) / max(dot(ac, ac), 1e-4), 0.0, 1.0);
  float is = 1.0 - s;
  vec3 pt = is * is * u_threadA + 2.0 * is * s * u_threadB + s * s * u_threadC;
  return length(p - pt) - u_threadRadius;
}

vec3 threadTangentAt(vec3 p) {
  vec3 ac = u_threadC - u_threadA;
  float s = clamp(dot(p - u_threadA, ac) / max(dot(ac, ac), 1e-4), 0.0, 1.0);
  vec3 tv = 2.0 * (1.0 - s) * (u_threadB - u_threadA) + 2.0 * s * (u_threadC - u_threadB);
  return normalize(tv + vec3(1e-4, 0.0, 0.0));
}

float mapSculpt(vec3 p) {
  float d = 1e4;
  if (u_size0.x > 0.01) {
    d = sdRoundBox(u_rot0 * (p - u_pos0), u_size0.xyz, u_size0.w);
  }
  if (u_size1.x > 0.01) {
    float d1 = sdRoundBox(u_rot1 * (p - u_pos1), u_size1.xyz, u_size1.w);
    d = (u_mat1.z > 0.5 || u_smoothK <= 0.001) ? min(d, d1) : sminF(d, d1, u_smoothK);
  }
  if (u_size2.x > 0.01) {
    float d2 = sdRoundBox(u_rot2 * (p - u_pos2), u_size2.xyz, u_size2.w);
    d = (u_mat2.z > 0.5 || u_smoothK <= 0.001) ? min(d, d2) : sminF(d, d2, u_smoothK);
  }
  if (u_threadFuse > 0.01) {
    d = sminF(d, sdThreadFast(p), u_threadFuse * 0.42);
  }
  return d;
}

// 极简纯点积摄影棚柔光环境（零 atan / 零矩阵旋转，极速且高光如商业柔光灯箱般纯净）
vec3 evalStudioEnv(vec3 dir, float rough, vec3 L1) {
  vec3 d = normalize(dir);
  float up = clamp(d.y * 0.5 + 0.5, 0.0, 1.0);
  vec3 env = mix(u_bgBot, u_bgTop, smoothstep(0.12, 0.88, up));

  if (u_backdropMode > 0.01) {
    float xBand = sin(d.x * 16.0 + 0.4);
    float crispLines = smoothstep(0.84, 0.96, abs(xBand));
    float warmPillar = smoothstep(0.90, 0.985, sin(d.x * 5.2 + 0.9));
    float vMask = smoothstep(-0.45, -0.05, d.y) * smoothstep(0.58, 0.14, d.y);
    vec3 archWall = vec3(0.024, 0.032, 0.048)
                  + vec3(0.84, 0.91, 1.0) * crispLines * 0.34 * vMask
                  + vec3(0.94, 0.48, 0.30) * warmPillar * 0.86 * vMask;
    env = mix(env, archWall, u_backdropMode);
  }

  float sharp = mix(36.0, 3.0, rough);
  float s1 = pow(max(dot(d, L1), 0.0001), sharp);
  float strip = smoothstep(0.28 + rough * 0.35, 0.02, abs(dot(d, vec3(L1.z, 0.0, -L1.x)) - 0.12))
              * smoothstep(0.22, 0.85, d.y);
  float s2 = pow(max(dot(d, normalize(vec3(0.85, 0.30, 0.36))), 0.0001), sharp * 0.65);
  float s3 = pow(max(dot(d, normalize(vec3(-0.84, 0.22, -0.45))), 0.0001), sharp * 0.55);

  vec3 lights = vec3(1.0, 0.98, 0.95) * (s1 * 1.35 + strip * 0.88)
              + vec3(0.84, 0.92, 1.0) * s2 * 0.72
              + vec3(1.0, 0.78, 0.62) * s3 * 0.60;

  return env + lights * u_lightIntensity * (1.0 - u_rimOnly * 0.85);
}

void main() {
  vec2 ndc = (gl_FragCoord.xy - 0.5 * u_res) / max(u_res.y, 1.0);
  float cr = cos(u_camRoll), sr = sin(u_camRoll);
  vec2 rNdc = vec2(cr * ndc.x - sr * ndc.y, sr * ndc.x + cr * ndc.y);

  vec3 ww = normalize(u_camTarget - u_camPos);
  vec3 uu = normalize(cross(ww, vec3(0.0, 1.0, 0.0)));
  vec3 vv = cross(uu, ww);
  vec3 rd = normalize(rNdc.x * uu + rNdc.y * vv + u_camFov * ww);
  vec3 ro = u_camPos;

  vec3 L1 = normalize(vec3(sin(u_lightAngle) * 0.72, 0.82, cos(u_lightAngle) * 0.55));
  vec3 L2 = normalize(vec3(-0.75, 0.35, -0.55));

  vec3 bgCol = evalStudioEnv(rd, 0.45, L1);
  vec3 col = bgCol;

  // 主光线步进（36 步，同时捕捉丝线像素足迹防断线）
  float tHit = 0.0;
  float maxT = 14.0;
  bool hit = false;
  int hitKind = 0; // 1=sculpt, 2=floor, 3=thread
  float bestThreadErr = 1e4;
  float bestThreadT = 1e4;
  float pxCone = 1.45 / (max(u_res.y, 1.0) * u_camFov);

  for (int i = 0; i < 38; i++) {
    vec3 p = ro + rd * tHit;
    float dS = mapSculpt(p);
    float dT = (u_threadEnabled > 0.5 && u_threadFuse <= 0.01) ? sdThreadFast(p) : 1e4;
    float dF = (u_floorEnabled > 0.5) ? (p.y - u_floorY) : 1e4;

    float cone = tHit * pxCone;
    if (dT < cone && dT < bestThreadErr) {
      bestThreadErr = dT;
      bestThreadT = tHit;
    }

    float d = min(dS, min(dT, dF));
    if (d < 0.002) {
      hit = true;
      hitKind = (dF <= dS && dF <= dT) ? 2 : ((dT < dS) ? 3 : 1);
      break;
    }
    tHit += d * 0.96;
    if (tHit > maxT) break;
  }

  if (hit) {
    vec3 pos = ro + rd * tHit;
    vec3 V = -rd;

    if (hitKind == 2) {
      // 无影棚地面：法线恒为 (0, 1, 0)，零额外法线采样！
      vec3 N = vec3(0.0, 1.0, 0.0);
      float NdotV = clamp(V.y, 0.0, 1.0);

      // 6 步柔和接触阴影
      float shadow = 1.0;
      float st = 0.05;
      for (int i = 0; i < 6; i++) {
        float h = mapSculpt(pos + L1 * st);
        shadow = min(shadow, 6.5 * h / st);
        st += clamp(h, 0.08, 0.45);
      }
      shadow = clamp(shadow, 0.0, 1.0);

      // 2 步快速地面接触 AO
      float ao = clamp(1.0 - 2.0 * max(0.0, 0.14 - mapSculpt(pos + vec3(0.0, 0.14, 0.0))), 0.35, 1.0);

      vec3 floorCol = mix(u_bgBot, u_bgTop, 0.25);
      float diff = 0.62 + 0.38 * clamp(L1.y, 0.0, 1.0);
      vec3 baseFloor = floorCol * diff * mix(0.42, 1.0, shadow) * ao;

      vec3 R = reflect(rd, N);
      float fres = 0.04 + 0.96 * pow(max(1.0 - NdotV, 0.0001), 4.0);
      vec3 floorRefl = evalStudioEnv(R, u_floorRough, L1) * (1.0 - u_floorRough * 0.72);
      col = mix(baseFloor, floorRefl, fres * 0.45);

      // 实时玻璃地板焦散
      if (u_causticsStrength > 0.01) {
        float dc = mapSculpt(pos + L1 * 1.05);
        float cMask = exp(-max(dc, 0.0) * 3.8);
        float w1 = sin(pos.x * 9.5 + sin(pos.z * 7.2));
        float w2 = cos(pos.z * 10.5 + cos(pos.x * 8.0));
        float cv = clamp(0.5 + 0.5 * w1 * w2, 0.0, 1.0);
        float caust = (cv * cv * cv * cv * 0.85 + exp(-abs(dc + 0.06) * 9.0) * 0.75) * cMask * u_causticsStrength;
        col += vec3(0.86, 0.94, 1.0) * caust * 0.68;
      }

      if (u_redCoreStrength > 0.01) {
        vec3 toRed = u_redCorePos - pos;
        col += vec3(0.95, 0.08, 0.11) * (u_redCoreStrength / (dot(toRed, toRed) + 0.25)) * 0.34 * ao;
      }

      col = mix(col, bgCol, smoothstep(3.8, 11.5, tHit));
    } else if (hitKind == 3) {
      // 3D 质感丝线本体（Kajiya-Kay 各向异性丝绸高光）
      vec3 T = threadTangentAt(pos);
      vec3 H1 = normalize(L1 + V);
      float sinTH = sqrt(max(1.0 - dot(T, H1) * dot(T, H1), 0.0001));
      col = u_threadCol * (0.72 + u_threadGlow * 0.95) + vec3(1.0, 0.96, 0.90) * pow(sinTH, 22.0) * u_lightIntensity * 0.55;
    } else {
      // 3D 雕塑本体：四面体 4 点法线
      const vec2 k = vec2(1.0, -1.0);
      const float e = 0.0018;
      vec3 N = normalize(
        k.xyy * mapSculpt(pos + k.xyy * e) +
        k.yyx * mapSculpt(pos + k.yyx * e) +
        k.yxy * mapSculpt(pos + k.yxy * e) +
        k.xxx * mapSculpt(pos + k.xxx * e)
      );

      // 评估融合色与材质
      float dAcc = 1e4;
      vec3 hitCol = u_col0;
      vec4 hitMat = u_mat0;
      if (u_size0.x > 0.01) dAcc = sdRoundBox(u_rot0 * (pos - u_pos0), u_size0.xyz, u_size0.w);
      if (u_size1.x > 0.01) {
        float d1 = sdRoundBox(u_rot1 * (pos - u_pos1), u_size1.xyz, u_size1.w);
        if (u_mat1.z > 0.5 || u_smoothK <= 0.001) {
          if (d1 < dAcc) { dAcc = d1; hitCol = u_col1; hitMat = u_mat1; }
        } else {
          vec2 sm = sminW(dAcc, d1, u_smoothK);
          dAcc = sm.x;
          float w = smoothstep(0.08, 0.92, sm.y);
          hitCol = mix(hitCol, u_col1, w);
          hitMat = mix(hitMat, u_mat1, w);
        }
      }
      if (u_size2.x > 0.01) {
        float d2 = sdRoundBox(u_rot2 * (pos - u_pos2), u_size2.xyz, u_size2.w);
        if (u_mat2.z > 0.5 || u_smoothK <= 0.001) {
          if (d2 < dAcc) { dAcc = d2; hitCol = u_col2; hitMat = u_mat2; }
        } else {
          vec2 sm = sminW(dAcc, d2, u_smoothK);
          dAcc = sm.x;
          float w = smoothstep(0.08, 0.92, sm.y);
          hitCol = mix(hitCol, u_col2, w);
          hitMat = mix(hitMat, u_mat2, w);
        }
      }
      if (u_threadFuse > 0.01) {
        vec2 smT = sminW(dAcc, sdThreadFast(pos), u_threadFuse * 0.42);
        float wT = smoothstep(0.18, 0.85, smT.y);
        hitCol = mix(hitCol, u_threadCol, wT);
        hitMat = mix(hitMat, vec4(0.0, 0.16, u_threadGlow * 0.3, 0.0), wT);
      }

      // Nothing 喷砂微孔点阵表面法线扰动
      if (hitMat.w > 0.01) {
        vec3 aN = abs(N);
        vec3 tw = aN * aN * aN;
        tw /= (tw.x + tw.y + tw.z + 1e-4);
        vec2 gXY = fract(pos.xy * 22.0) - 0.5;
        vec2 gXZ = fract(pos.xz * 22.0) - 0.5;
        vec2 gYZ = fract(pos.yz * 22.0) - 0.5;
        vec2 dXY = gXY * smoothstep(0.28, 0.16, length(gXY));
        vec2 dXZ = gXZ * smoothstep(0.28, 0.16, length(gXZ));
        vec2 dYZ = gYZ * smoothstep(0.28, 0.16, length(gYZ));
        vec3 bump = vec3(dXY.x * tw.z + dXZ.x * tw.y, dXY.y * tw.z + dYZ.x * tw.x, dXZ.y * tw.y + dYZ.y * tw.x);
        N = normalize(N - bump * 1.45 * hitMat.w);
      }

      float NdotV = clamp(dot(N, V), 0.0, 1.0);

      if (u_rimOnly > 0.5) {
        float rim = pow(max(1.0 - NdotV, 0.0001), 4.0);
        col = vec3(0.01) + vec3(1.0, 0.94, 0.88) * rim * (0.45 + 0.95 * smoothstep(-0.1, 0.85, N.y));
      } else {
        float rough = clamp(hitMat.y, 0.02, 0.95);
        float glassMix = clamp(hitMat.x, 0.0, 1.0);
        float fresnel = 0.04 + 0.96 * pow(max(1.0 - NdotV, 0.0001), 4.0);
        vec3 R = reflect(rd, N);
        vec3 envRefl = evalStudioEnv(R, mix(rough, 0.025, glassMix), L1);
        vec3 H1 = normalize(L1 + V);

        // 实体次表面散射麻薯着色
        float ao = clamp(1.0 - 2.2 * max(0.0, 0.12 - mapSculpt(pos + N * 0.12)), 0.45, 1.0);
        float thick = clamp(mapSculpt(pos + L1 * 0.28) / 0.28, 0.0, 1.0);
        float bs = clamp(dot(V, -L1 + N * 0.45), 0.0, 1.0);
        float wrapDiff = clamp((dot(N, L1) + 0.45) / 1.45, 0.0, 1.0);
        vec3 sssTint = mix(hitCol, vec3(1.0, 0.52, 0.34), 0.42);
        vec3 diffuse = hitCol * (0.30 + 0.70 * wrapDiff) * ao
                     + sssTint * (bs * bs * bs * (1.0 - thick) * 0.65 + 0.14 * (1.0 - hitMat.w)) * (1.0 - hitMat.w);
        float spec = pow(max(dot(N, H1), 0.0001), mix(92.0, 14.0, rough)) * mix(1.1, 0.22, rough);

        vec3 solidCol = mix(diffuse, envRefl, fresnel * mix(0.55, 0.14, rough))
                      + vec3(1.0, 0.98, 0.95) * spec * u_lightIntensity * 0.65
                      + hitCol * clamp(dot(N, L2), 0.0, 1.0) * 0.18 * ao;

        if (u_redCoreStrength > 0.01 && hitMat.z < 0.5) {
          vec3 toRed = u_redCorePos - pos;
          vec3 rDir = normalize(toRed);
          float rDiff = clamp(dot(N, rDir), 0.0, 1.0);
          float rSpec = pow(max(dot(N, normalize(rDir + V)), 0.0001), 28.0);
          solidCol += vec3(0.98, 0.09, 0.12) * (rDiff * 0.85 + rSpec * 0.65) * (u_redCoreStrength / (dot(toRed, toRed) + 0.18)) * 0.50;
        }
        solidCol += hitCol * hitMat.z;

        // 3D 液态玻璃：单步解析穿平面追踪背后 3D 丝线与暖橘内芯 + 克制三通道色散！
        vec3 glassCol = solidCol;
        if (glassMix > 0.005) {
          vec3 refrG = refract(rd, N, 1.0 / 1.50);
          if (length(refrG) < 0.01) refrG = R;

          vec3 bgRefr = evalStudioEnv(refrG, 0.035, L1);

          // 解析投射到丝线所在的 Z 平面，零循环直接算出折射折断后的丝线位置与三通道色散！
          if (u_threadEnabled > 0.5) {
            float tz = (u_threadB.z - pos.z) / (abs(refrG.z) > 0.05 ? refrG.z : -0.5);
            vec3 hitPlane = pos + refrG * clamp(tz, 0.12, 2.6);
            vec3 dispOff = N * 0.022;
            float dR = sdThreadFast(hitPlane - dispOff);
            float dG = sdThreadFast(hitPlane);
            float dB = sdThreadFast(hitPlane + dispOff);
            vec3 tMask = vec3(
              smoothstep(0.042, 0.0, dR) + exp(-max(dR, 0.0) * 14.0) * 0.30,
              smoothstep(0.042, 0.0, dG) + exp(-max(dG, 0.0) * 14.0) * 0.30,
              smoothstep(0.042, 0.0, dB) + exp(-max(dB, 0.0) * 14.0) * 0.30
            );
            bgRefr = mix(bgRefr, u_threadCol * (1.35 + u_threadGlow * 1.25), clamp(tMask, 0.0, 1.0));
          }

          // 解析探测第六幕玻璃背后的暖橘内芯球
          if (u_smoothK <= 0.001 && u_size1.x > 0.05 && u_mat1.x < 0.5) {
            float tc = (u_pos1.z - pos.z) / (abs(refrG.z) > 0.05 ? refrG.z : -0.5);
            vec3 corePt = pos + refrG * clamp(tc, 0.12, 2.6);
            float dCore = sdRoundBox(u_rot1 * (corePt - u_pos1), u_size1.xyz, u_size1.w);
            float coreGlow = exp(-max(dCore, 0.0) * 5.2);
            bgRefr = mix(bgRefr, u_col1 * (1.05 + u_mat1.z * 0.78), clamp(coreGlow * 0.90, 0.0, 0.92));
          }

          vec3 absorption = mix(vec3(0.97, 0.99, 1.02), vec3(0.76, 0.86, 0.96), (1.0 - NdotV) * 0.52);
          bgRefr *= absorption;

          float gSpec1 = pow(max(dot(N, H1), 0.0001), 110.0) * 1.45;
          float gSpec2 = pow(max(dot(N, normalize(L2 + V)), 0.0001), 44.0) * 0.42;
          float rimLine = smoothstep(0.62, 0.98, 1.0 - NdotV);

          glassCol = mix(bgRefr, envRefl, clamp(fresnel * 0.66 + 0.06, 0.0, 0.86));
          glassCol += vec3(1.0, 0.99, 0.97) * gSpec1 + vec3(0.95, 0.82, 0.72) * gSpec2;
          glassCol += vec3(0.94, 0.97, 1.0) * rimLine * 0.34;
        }

        col = mix(solidCol, glassCol, glassMix);
      }
    }
  }

  // 锥形像素足迹抗锯齿细丝补全（零断线）
  if (u_threadEnabled > 0.5 && u_threadFuse <= 0.01 && bestThreadT < tHit + 0.015) {
    float cone = bestThreadT * pxCone;
    float cov = 1.0 - smoothstep(0.0, cone, bestThreadErr);
    if (cov > 0.005) {
      vec3 tp = ro + rd * bestThreadT;
      vec3 T = threadTangentAt(tp);
      vec3 H1 = normalize(L1 - rd);
      float sinTH = sqrt(max(1.0 - dot(T, H1) * dot(T, H1), 0.0001));
      vec3 threadLit = u_threadCol * (0.72 + u_threadGlow * 0.95) + vec3(1.0, 0.96, 0.90) * pow(sinTH, 22.0) * 0.55;
      col = mix(col, threadLit, cov * 0.88);
    }
  }

  fragColor = vec4(max(col, vec3(0.0)), 1.0);
}
`;

const POST_CINEMA_FS = `#version 300 es
precision highp float;

in vec2 vUv;
out vec4 fragColor;

uniform sampler2D u_sceneTex;
uniform vec2 u_res;
uniform float u_time;
uniform float u_bloom;
uniform float u_chromaPunch;
uniform float u_vignette;
uniform float u_grain;
uniform float u_blackout;
uniform float u_exposure;

vec3 acesFilm(vec3 x) {
  const float a = 2.51;
  const float b = 0.03;
  const float c = 2.43;
  const float d = 0.59;
  const float e = 0.14;
  return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}

float hash12(vec2 p) {
  vec3 p3 = fract(vec3(p.xyx) * 0.1031);
  p3 += dot(p3, p3.yzx + 33.33);
  return fract((p3.x + p3.y) * p3.z);
}

void main() {
  vec2 centered = vUv - 0.5;
  vec3 col;

  if (u_chromaPunch > 0.002) {
    vec2 shift = centered * u_chromaPunch * 0.015;
    col.r = texture(u_sceneTex, clamp(vUv + shift, 0.001, 0.999)).r;
    col.g = texture(u_sceneTex, vUv).g;
    col.b = texture(u_sceneTex, clamp(vUv - shift, 0.001, 0.999)).b;
  } else {
    col = texture(u_sceneTex, vUv).rgb;
  }

  vec2 px1 = 3.0 / u_res;
  vec2 px2 = 7.5 / u_res;
  vec3 hi = vec3(0.0);
  hi += max(texture(u_sceneTex, clamp(vUv + vec2( px1.x,  px1.y), 0.001, 0.999)).rgb - 0.85, 0.0);
  hi += max(texture(u_sceneTex, clamp(vUv + vec2(-px1.x,  px1.y), 0.001, 0.999)).rgb - 0.85, 0.0);
  hi += max(texture(u_sceneTex, clamp(vUv + vec2( px1.x, -px1.y), 0.001, 0.999)).rgb - 0.85, 0.0);
  hi += max(texture(u_sceneTex, clamp(vUv + vec2(-px1.x, -px1.y), 0.001, 0.999)).rgb - 0.85, 0.0);
  hi += max(texture(u_sceneTex, clamp(vUv + vec2( px2.x,  0.0),   0.001, 0.999)).rgb - 0.92, 0.0) * 0.7;
  hi += max(texture(u_sceneTex, clamp(vUv + vec2(-px2.x,  0.0),   0.001, 0.999)).rgb - 0.92, 0.0) * 0.7;
  hi += max(texture(u_sceneTex, clamp(vUv + vec2( 0.0,    px2.y), 0.001, 0.999)).rgb - 0.92, 0.0) * 0.7;
  hi += max(texture(u_sceneTex, clamp(vUv + vec2( 0.0,   -px2.y), 0.001, 0.999)).rgb - 0.92, 0.0) * 0.7;
  col += (hi / 6.8) * u_bloom;

  col *= u_exposure;
  col = acesFilm(col);
  col = pow(col, vec3(1.0 / 2.15));

  float vig = 1.0 - smoothstep(0.32, 1.02, length(centered) * 1.15) * u_vignette;
  col *= vig;
  col *= (1.0 - clamp(u_blackout, 0.0, 1.0));

  float n1 = hash12(gl_FragCoord.xy + fract(u_time * 13.7) * 100.0);
  float n2 = hash12(gl_FragCoord.xy + fract(u_time * 29.3) * 100.0 + 17.3);
  col += (n1 + n2 - 1.0) * (u_grain * 0.42 + 0.5 / 255.0);

  fragColor = vec4(clamp(col, 0.0, 1.0), 1.0);
}
`;


// ==================== renderer.js ====================

/**
 * WebGL2 双通道 3D 摄影棚渲染管线
 * CPU 预计算逆旋转矩阵，向 GPU 传入零嵌套循环的扁平 Uniform，全平台毫秒级编译与 60fps 渲染。
 */


/** 计算 R_x(-rx) * R_y(-ry) * R_z(-rz) 列主序 3x3 逆旋转矩阵 */
function buildInvRotMat3(rx, ry, rz, out) {
  const cx = Math.cos(-rx), sx = Math.sin(-rx);
  const cy = Math.cos(-ry), sy = Math.sin(-ry);
  const cz = Math.cos(-rz), sz = Math.sin(-rz);

  // 先绕 Z 轴，再绕 Y 轴，再绕 X 轴（与 GLSL p.xy->p.xz->p.yz 顺序一致）
  const a00 = cy * cz;
  const a01 = -cy * sz;
  const a02 = sy;

  const a10 = cx * sz + sx * sy * cz;
  const a11 = cx * cz - sx * sy * sz;
  const a12 = -sx * cy;

  const a20 = sx * sz - cx * sy * cz;
  const a21 = sx * cz + cx * sy * sz;
  const a22 = cx * cy;

  // WebGL uniformMatrix3fv 接受列主序 (column-major)
  out[0] = a00; out[1] = a10; out[2] = a20;
  out[3] = a01; out[4] = a11; out[5] = a21;
  out[6] = a02; out[7] = a12; out[8] = a22;
  return out;
}

function compileShader(gl, type, source, label) {
  const s = gl.createShader(type);
  gl.shaderSource(s, source);
  gl.compileShader(s);
  if (!gl.getShaderParameter(s, gl.COMPILE_STATUS)) {
    const info = gl.getShaderInfoLog(s);
    gl.deleteShader(s);
    throw new Error(`着色器编译失败 [${label}]: ${info}`);
  }
  return s;
}

function createProgram(gl, vsSrc, fsSrc, label) {
  const vs = compileShader(gl, gl.VERTEX_SHADER, vsSrc, `${label}-VS`);
  const fs = compileShader(gl, gl.FRAGMENT_SHADER, fsSrc, `${label}-FS`);
  const prog = gl.createProgram();
  gl.attachShader(prog, vs);
  gl.attachShader(prog, fs);
  gl.linkProgram(prog);
  const ok = gl.getProgramParameter(prog, gl.LINK_STATUS);
  if (!ok) {
    const pLog = gl.getProgramInfoLog(prog) || '';
    const vLog = gl.getShaderInfoLog(vs) || '';
    const fLog = gl.getShaderInfoLog(fs) || '';
    gl.deleteShader(vs);
    gl.deleteShader(fs);
    gl.deleteProgram(prog);
    throw new Error(`着色器程序链接失败 [${label}]: prog=${pLog} | vs=${vLog} | fs=${fLog}`);
  }
  gl.deleteShader(vs);
  gl.deleteShader(fs);
  return prog;
}

function createHDRTarget(gl, w, h) {
  const extFloat = gl.getExtension('EXT_color_buffer_float');
  const tex = gl.createTexture();
  gl.bindTexture(gl.TEXTURE_2D, tex);
  if (extFloat) {
    gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA16F, w, h, 0, gl.RGBA, gl.HALF_FLOAT, null);
  } else {
    gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA8, w, h, 0, gl.RGBA, gl.UNSIGNED_BYTE, null);
  }
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);

  const fbo = gl.createFramebuffer();
  gl.bindFramebuffer(gl.FRAMEBUFFER, fbo);
  gl.framebufferTexture2D(gl.FRAMEBUFFER, gl.COLOR_ATTACHMENT0, gl.TEXTURE_2D, tex, 0);

  if (gl.checkFramebufferStatus(gl.FRAMEBUFFER) !== gl.FRAMEBUFFER_COMPLETE) {
    gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA8, w, h, 0, gl.RGBA, gl.UNSIGNED_BYTE, null);
  }
  gl.bindFramebuffer(gl.FRAMEBUFFER, null);
  return { fbo, tex, width: w, height: h };
}

class StudioRenderer {
  constructor(canvas) {
    this.canvas = canvas;
    const gl = canvas.getContext('webgl2', {
      alpha: false,
      antialias: false,
      depth: false,
      stencil: false,
      powerPreference: 'high-performance',
      preserveDrawingBuffer: true,
    });
    if (!gl) {
      throw new Error('当前浏览器不支持 WebGL2');
    }
    this.gl = gl;
    this.vao = gl.createVertexArray();

    this.studioProg = createProgram(gl, FULLSCREEN_VS, STUDIO_3D_FS, 'Studio3D');
    this.postProg = createProgram(gl, FULLSCREEN_VS, POST_CINEMA_FS, 'PostCinema');

    this.uStudio = this._cacheUniforms(this.studioProg, [
      'u_res', 'u_time',
      'u_camPos', 'u_camTarget', 'u_camFov', 'u_camRoll',
      'u_threadEnabled', 'u_threadA', 'u_threadB', 'u_threadC',
      'u_threadRadius', 'u_threadCol', 'u_threadGlow', 'u_threadFuse',
      'u_bgTop', 'u_bgBot', 'u_floorY', 'u_floorEnabled', 'u_floorRough',
      'u_lightAngle', 'u_lightIntensity', 'u_backdropMode', 'u_causticsStrength',
      'u_rimOnly', 'u_redCorePos', 'u_redCoreStrength', 'u_smoothK',
      'u_pos0', 'u_size0', 'u_rot0', 'u_col0', 'u_mat0',
      'u_pos1', 'u_size1', 'u_rot1', 'u_col1', 'u_mat1',
      'u_pos2', 'u_size2', 'u_rot2', 'u_col2', 'u_mat2',
    ]);
    this.uPost = this._cacheUniforms(this.postProg, [
      'u_sceneTex', 'u_res', 'u_time', 'u_bloom', 'u_chromaPunch', 'u_vignette', 'u_grain', 'u_blackout', 'u_exposure',
    ]);

    this.rotMats = [new Float32Array(9), new Float32Array(9), new Float32Array(9)];

    this.width = 1280;
    this.height = 720;
    this.rtWidth = 1280;
    this.rtHeight = 720;
    this.sceneRT = null;

    this.resize(1280, 720);
  }

  _cacheUniforms(prog, names) {
    const out = {};
    for (const n of names) {
      out[n] = this.gl.getUniformLocation(prog, n);
    }
    return out;
  }

  resize(width, height) {
    const gl = this.gl;
    const w = Math.max(320, Math.round(width));
    const h = Math.max(240, Math.round(height));
    if (w === this.width && h === this.height && this.sceneRT) return;

    this.width = w;
    this.height = h;
    this.canvas.width = w;
    this.canvas.height = h;

    const scale = w > 1280 ? 1280 / w : 1.0;
    this.rtWidth = Math.max(320, Math.round(w * scale));
    this.rtHeight = Math.max(240, Math.round(h * scale));

    if (this.sceneRT) {
      gl.deleteFramebuffer(this.sceneRT.fbo);
      gl.deleteTexture(this.sceneRT.tex);
    }
    this.sceneRT = createHDRTarget(gl, this.rtWidth, this.rtHeight);
  }

  render(scene) {
    const gl = this.gl;
    const u = this.uStudio;

    gl.bindVertexArray(this.vao);

    gl.bindFramebuffer(gl.FRAMEBUFFER, this.sceneRT.fbo);
    gl.viewport(0, 0, this.rtWidth, this.rtHeight);
    gl.useProgram(this.studioProg);

    gl.uniform2f(u.u_res, this.rtWidth, this.rtHeight);
    gl.uniform1f(u.u_time, scene.t);

    gl.uniform3fv(u.u_camPos, scene.camPos);
    gl.uniform3fv(u.u_camTarget, scene.camTarget);
    gl.uniform1f(u.u_camFov, scene.camFov);
    gl.uniform1f(u.u_camRoll, scene.camRoll);

    const th = scene.thread;
    gl.uniform1f(u.u_threadEnabled, th.enabled);
    gl.uniform3fv(u.u_threadA, th.a);
    gl.uniform3fv(u.u_threadB, th.b);
    gl.uniform3fv(u.u_threadC, th.c);
    gl.uniform1f(u.u_threadRadius, th.radius);
    gl.uniform3fv(u.u_threadCol, th.col);
    gl.uniform1f(u.u_threadGlow, th.glow);
    gl.uniform1f(u.u_threadFuse, th.fuseWithBodies);

    const st = scene.studio;
    gl.uniform3fv(u.u_bgTop, st.bgTop);
    gl.uniform3fv(u.u_bgBot, st.bgBot);
    gl.uniform1f(u.u_floorY, st.floorY);
    gl.uniform1f(u.u_floorEnabled, st.floorEnabled);
    gl.uniform1f(u.u_floorRough, st.floorRough);
    gl.uniform1f(u.u_lightAngle, st.lightAngle);
    gl.uniform1f(u.u_lightIntensity, st.lightIntensity);
    gl.uniform1f(u.u_backdropMode, st.backdropMode);
    gl.uniform1f(u.u_causticsStrength, st.causticsStrength);
    gl.uniform1f(u.u_rimOnly, st.rimOnly);
    gl.uniform3fv(u.u_redCorePos, st.redCorePos);
    gl.uniform1f(u.u_redCoreStrength, st.redCoreStrength);
    gl.uniform1f(u.u_smoothK, scene.smoothK);

    for (let i = 0; i < 3; i++) {
      const b = scene.bodies[i];
      buildInvRotMat3(b.rot[0], b.rot[1], b.rot[2], this.rotMats[i]);
      gl.uniform3fv(u[`u_pos${i}`], b.pos);
      gl.uniform4fv(u[`u_size${i}`], b.size);
      gl.uniformMatrix3fv(u[`u_rot${i}`], false, this.rotMats[i]);
      gl.uniform3fv(u[`u_col${i}`], b.col);
      gl.uniform4fv(u[`u_mat${i}`], b.mat);
    }

    gl.drawArrays(gl.TRIANGLES, 0, 3);

    gl.bindFramebuffer(gl.FRAMEBUFFER, null);
    gl.viewport(0, 0, this.width, this.height);
    gl.useProgram(this.postProg);

    gl.activeTexture(gl.TEXTURE0);
    gl.bindTexture(gl.TEXTURE_2D, this.sceneRT.tex);
    gl.uniform1i(this.uPost.u_sceneTex, 0);
    gl.uniform2f(this.uPost.u_res, this.width, this.height);
    gl.uniform1f(this.uPost.u_time, scene.t);
    gl.uniform1f(this.uPost.u_bloom, scene.post.bloom);
    gl.uniform1f(this.uPost.u_chromaPunch, scene.post.chromaPunch);
    gl.uniform1f(this.uPost.u_vignette, scene.post.vignette);
    gl.uniform1f(this.uPost.u_grain, scene.post.grain);
    gl.uniform1f(this.uPost.u_blackout, scene.post.blackout);
    gl.uniform1f(this.uPost.u_exposure, scene.post.exposure);

    gl.drawArrays(gl.TRIANGLES, 0, 3);
    gl.bindVertexArray(null);
  }
}


// ==================== interstitial.js ====================

/**
 * 极简电影字卡与落版层
 * 全片 90% 以上时间完全透明、零文字，不破坏 3D 摄影棚画面；
 * 仅在极短节奏硬切（0.3s 单字冲击）与最终落版「Mochi」时浮现。
 */

const SANS_STACK = '"PingFang SC", "Noto Sans SC", "Microsoft YaHei UI", "Microsoft YaHei", sans-serif';

class OverlayLayer {
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


// ==================== audio.js ====================

/**
 * 42 秒丝线材质电影程序化声景合成器（100% Web Audio API）
 * 紧扣丝线绷紧、拨弦回弹、融球液桥、玻璃泛音、31.0s 真空抽静与三境静切。
 */

class MochiAudioEngine {
  constructor() {
    this.ctx = null;
    this.masterGain = null;
    this.reverbSend = null;
    this.analyser = null;
    this.freqData = null;

    this.enabled = false;
    this.muted = false;
    this.lastTime = -1;
    this.triggeredEvents = new Set();
    this.energy = 0;
  }

  init() {
    if (this.ctx) {
      if (this.ctx.state === 'suspended') {
        this.ctx.resume().catch(() => {});
      }
      this.enabled = true;
      return;
    }

    const AudioCtx = window.AudioContext || window.webkitAudioContext;
    if (!AudioCtx) return;

    const ctx = new AudioCtx();
    this.ctx = ctx;

    const comp = ctx.createDynamicsCompressor();
    comp.threshold.value = -14;
    comp.knee.value = 18;
    comp.ratio.value = 6;
    comp.attack.value = 0.004;
    comp.release.value = 0.16;

    const master = ctx.createGain();
    master.gain.value = this.muted ? 0 : 0.78;

    const analyser = ctx.createAnalyser();
    analyser.fftSize = 128;
    analyser.smoothingTimeConstant = 0.78;
    this.freqData = new Uint8Array(analyser.frequencyBinCount);

    const convolver = ctx.createConvolver();
    convolver.buffer = this._createImpulseResponse(ctx, 2.8, 3.0);

    const reverbGain = ctx.createGain();
    reverbGain.gain.value = 0.38;

    master.connect(comp);
    comp.connect(analyser);
    analyser.connect(ctx.destination);

    reverbGain.connect(convolver);
    convolver.connect(master);

    this.masterGain = master;
    this.reverbSend = reverbGain;
    this.analyser = analyser;
    this.enabled = true;
  }

  _createImpulseResponse(ctx, duration, decay) {
    const rate = ctx.sampleRate;
    const len = Math.floor(rate * duration);
    const buffer = ctx.createBuffer(2, len, rate);
    for (let ch = 0; ch < 2; ch++) {
      const data = buffer.getChannelData(ch);
      for (let i = 0; i < len; i++) {
        const env = Math.pow(1 - i / len, decay);
        data[i] = (Math.random() * 2 - 1) * env;
      }
    }
    return buffer;
  }

  setMuted(muted) {
    this.muted = muted;
    if (this.masterGain && this.ctx) {
      const now = this.ctx.currentTime;
      this.masterGain.gain.cancelScheduledValues(now);
      this.masterGain.gain.setTargetAtTime(muted ? 0 : 0.78, now, 0.04);
    }
  }

  resetSync() {
    this.triggeredEvents.clear();
    this.lastTime = -1;
  }

  update(t, playing) {
    if (!this.enabled || !this.ctx || this.ctx.state !== 'running') {
      this.energy *= 0.9;
      return this.energy;
    }

    if (this.analyser && !this.muted) {
      this.analyser.getByteFrequencyData(this.freqData);
      let sum = 0;
      for (let i = 0; i < 10; i++) sum += this.freqData[i];
      this.energy = this.energy * 0.78 + (sum / (10 * 255)) * 0.22;
    } else {
      this.energy *= 0.86;
    }

    if (!playing || this.muted) {
      this.lastTime = t;
      return this.energy;
    }

    if (this.lastTime < 0 || t < this.lastTime - 0.5 || t - this.lastTime > 1.0) {
      this.resetSync();
      this.lastTime = t;
      return this.energy;
    }

    // 31.0 - 31.36s 真空静音
    if (t >= 31.0 && t < 31.35) {
      this.lastTime = t;
      return 0;
    }

    const prev = this.lastTime;
    const crossed = (mark, key) => {
      if (prev < mark && t >= mark && !this.triggeredEvents.has(key)) {
        this.triggeredEvents.add(key);
        return true;
      }
      return false;
    };

    // 第一段：丝线抽直与缓缓受压绷紧
    if (crossed(0.15, 's1_line'))  this._playGlassTone(587.3, 2.2, 0.08);
    if (crossed(1.80, 's1_drop'))  this._playSubDrop(78, 44, 1.4, 0.22);
    if (crossed(3.60, 's1_bend'))  this._playTensionBow(196.0, 246.9, 2.6, 0.14);

    // 第二段：拨弦弹射 + 三颗麻薯依次落位
    if (crossed(7.02, 's2_pluck')) {
      this._playPluck(196.0, 0.36);
      this._playSubDrop(115, 48, 0.55, 0.30);
    }
    if (crossed(7.14, 's2_m0')) this._playMochiPluck(293.7, 0.24);
    if (crossed(7.44, 's2_m1')) this._playMochiPluck(392.0, 0.26);
    if (crossed(7.74, 's2_m2')) this._playMochiPluck(493.9, 0.24);
    if (crossed(13.2, 's2_gather')) this._playLiquidSweep(190, 540, 1.4, 0.18);

    // 第三段：丝线化作融球液桥
    if (crossed(14.9, 's3_fuse')) this._playGlassChord([220.0, 329.6, 440.0], 4.2, 0.15);
    if (crossed(18.2, 's3_pull')) this._playLiquidSweep(520, 210, 2.2, 0.16);

    // 第四段：丝线穿过 3D 液态玻璃折射
    if (crossed(22.45, 's4_glass')) this._playGlassChord([261.6, 392.0, 493.9, 659.2], 6.4, 0.18);
    if (crossed(25.80, 's4_shimmer')) this._playGlassTone(784.0, 3.8, 0.10);

    // 第五段：0.36s 真空后三境静切
    if (crossed(31.38, 's5_nothing')) {
      this._playSubDrop(140, 38, 0.78, 0.44);
      this._playClick(3200, 0.24);
    }
    if (crossed(33.32, 's5_warm')) {
      this._playMochiPluck(329.6, 0.30);
      this._playGlassChord([261.6, 329.6, 392.0], 2.0, 0.14);
    }
    if (crossed(35.26, 's5_glass')) {
      this._playGlassChord([293.7, 440.0, 587.3, 740.0], 2.4, 0.16);
    }

    // 第六段：归静落版单一泛音
    if (crossed(37.25, 's6_end')) {
      this._playGlassChord([261.6, 392.0, 523.2], 4.6, 0.15);
    }

    this.lastTime = t;
    return this.energy;
  }

  _playClick(freq, gainAmt) {
    const ctx = this.ctx;
    const now = ctx.currentTime;
    const osc = ctx.createOscillator();
    const g = ctx.createGain();
    osc.type = 'triangle';
    osc.frequency.setValueAtTime(freq, now);
    osc.frequency.exponentialRampToValueAtTime(freq * 0.25, now + 0.015);
    g.gain.setValueAtTime(0.0001, now);
    g.gain.linearRampToValueAtTime(gainAmt, now + 0.0015);
    g.gain.exponentialRampToValueAtTime(0.0001, now + 0.024);
    osc.connect(g);
    g.connect(this.masterGain);
    osc.start(now);
    osc.stop(now + 0.028);
  }

  _playPluck(freq, gainAmt) {
    const ctx = this.ctx;
    const now = ctx.currentTime;
    const osc = ctx.createOscillator();
    const filter = ctx.createBiquadFilter();
    const g = ctx.createGain();
    osc.type = 'sawtooth';
    osc.frequency.setValueAtTime(freq * 1.03, now);
    osc.frequency.exponentialRampToValueAtTime(freq, now + 0.08);
    filter.type = 'lowpass';
    filter.frequency.setValueAtTime(2800, now);
    filter.frequency.exponentialRampToValueAtTime(240, now + 0.9);
    g.gain.setValueAtTime(0.0001, now);
    g.gain.linearRampToValueAtTime(gainAmt, now + 0.006);
    g.gain.exponentialRampToValueAtTime(0.0001, now + 1.15);
    osc.connect(filter);
    filter.connect(g);
    g.connect(this.masterGain);
    g.connect(this.reverbSend);
    osc.start(now);
    osc.stop(now + 1.2);
  }

  _playTensionBow(startHz, endHz, dur, gainAmt) {
    const ctx = this.ctx;
    const now = ctx.currentTime;
    const osc = ctx.createOscillator();
    const g = ctx.createGain();
    osc.type = 'sine';
    osc.frequency.setValueAtTime(startHz, now);
    osc.frequency.exponentialRampToValueAtTime(endHz, now + dur);
    g.gain.setValueAtTime(0.0001, now);
    g.gain.linearRampToValueAtTime(gainAmt, now + dur * 0.65);
    g.gain.exponentialRampToValueAtTime(0.0001, now + dur);
    osc.connect(g);
    g.connect(this.masterGain);
    g.connect(this.reverbSend);
    osc.start(now);
    osc.stop(now + dur + 0.05);
  }

  _playSubDrop(startHz, endHz, dur, gainAmt) {
    const ctx = this.ctx;
    const now = ctx.currentTime;
    const osc = ctx.createOscillator();
    const g = ctx.createGain();
    osc.type = 'sine';
    osc.frequency.setValueAtTime(startHz, now);
    osc.frequency.exponentialRampToValueAtTime(Math.max(endHz, 26), now + dur);
    g.gain.setValueAtTime(0.0001, now);
    g.gain.linearRampToValueAtTime(gainAmt, now + 0.018);
    g.gain.exponentialRampToValueAtTime(0.0001, now + dur);
    osc.connect(g);
    g.connect(this.masterGain);
    osc.start(now);
    osc.stop(now + dur + 0.03);
  }

  _playMochiPluck(baseHz, gainAmt) {
    const ctx = this.ctx;
    const now = ctx.currentTime;
    const osc = ctx.createOscillator();
    const g = ctx.createGain();
    osc.type = 'sine';
    osc.frequency.setValueAtTime(baseHz * 0.88, now);
    osc.frequency.exponentialRampToValueAtTime(baseHz * 1.04, now + 0.06);
    osc.frequency.exponentialRampToValueAtTime(baseHz, now + 0.20);
    g.gain.setValueAtTime(0.0001, now);
    g.gain.linearRampToValueAtTime(gainAmt, now + 0.012);
    g.gain.exponentialRampToValueAtTime(0.0001, now + 0.45);
    osc.connect(g);
    g.connect(this.masterGain);
    g.connect(this.reverbSend);
    osc.start(now);
    osc.stop(now + 0.5);
  }

  _playLiquidSweep(startHz, endHz, dur, gainAmt) {
    const ctx = this.ctx;
    const now = ctx.currentTime;
    const osc = ctx.createOscillator();
    const filter = ctx.createBiquadFilter();
    const g = ctx.createGain();
    osc.type = 'sawtooth';
    osc.frequency.setValueAtTime(startHz * 0.5, now);
    osc.frequency.exponentialRampToValueAtTime(endHz * 0.5, now + dur);
    filter.type = 'bandpass';
    filter.Q.value = 6.5;
    filter.frequency.setValueAtTime(startHz, now);
    filter.frequency.exponentialRampToValueAtTime(endHz, now + dur);
    g.gain.setValueAtTime(0.0001, now);
    g.gain.linearRampToValueAtTime(gainAmt, now + dur * 0.25);
    g.gain.exponentialRampToValueAtTime(0.0001, now + dur);
    osc.connect(filter);
    filter.connect(g);
    g.connect(this.masterGain);
    g.connect(this.reverbSend);
    osc.start(now);
    osc.stop(now + dur + 0.05);
  }

  _playGlassTone(freq, dur, gainAmt) {
    const ctx = this.ctx;
    const now = ctx.currentTime;
    const carrier = ctx.createOscillator();
    const mod = ctx.createOscillator();
    const modGain = ctx.createGain();
    const g = ctx.createGain();
    carrier.type = 'sine';
    carrier.frequency.setValueAtTime(freq, now);
    mod.type = 'sine';
    mod.frequency.setValueAtTime(freq * 2.002, now);
    modGain.gain.setValueAtTime(freq * 0.35, now);
    modGain.gain.exponentialRampToValueAtTime(1.0, now + dur * 0.8);
    g.gain.setValueAtTime(0.0001, now);
    g.gain.linearRampToValueAtTime(gainAmt, now + dur * 0.16);
    g.gain.exponentialRampToValueAtTime(0.0001, now + dur);
    mod.connect(modGain);
    modGain.connect(carrier.frequency);
    carrier.connect(g);
    g.connect(this.masterGain);
    g.connect(this.reverbSend);
    mod.start(now);
    carrier.start(now);
    mod.stop(now + dur + 0.05);
    carrier.stop(now + dur + 0.05);
  }

  _playGlassChord(freqs, dur, gainAmt) {
    const perNote = gainAmt / Math.sqrt(freqs.length);
    for (let i = 0; i < freqs.length; i++) {
      this._playGlassTone(freqs[i], dur, perNote);
    }
  }
}


// ==================== main.js ====================

/**
 * 《Mochi》主驱动与交互控制器
 * 极简影院播放 + 按住画面平滑拖拽旋转 3D 机位与轻拉丝线 + 一键 WebM 录屏导出。
 */






const stageEl = document.getElementById('stage');
const glCanvas = document.getElementById('gl-canvas');
const overlayCanvas = document.getElementById('overlay-canvas');
const curtainEl = document.getElementById('curtain');
const hudEl = document.getElementById('hud');

const btnEnter = document.getElementById('btn-enter');
const btnEnterMuted = document.getElementById('btn-enter-muted');
const btnPlay = document.getElementById('btn-play');
const iconPlay = document.getElementById('icon-play');
const btnMute = document.getElementById('btn-mute');
const btnScope = document.getElementById('btn-scope');
const btnRecord = document.getElementById('btn-record');

const hudTimecode = document.getElementById('hud-timecode');
const scrubberEl = document.getElementById('scrubber');
const scrubberFill = document.getElementById('scrubber-fill');
const scrubberThumb = document.getElementById('scrubber-thumb');

const renderer = new StudioRenderer(glCanvas);
const overlayLayer = new OverlayLayer(overlayCanvas);
const audioEngine = new MochiAudioEngine();
const orbitSpring = new MochiFollowSpring(0.42, 0.92); // 临界偏稳弹簧：松手平滑归位，绝不乱晃

const params = new URLSearchParams(window.location.search);
const urlAutoplay = params.get('autoplay') === '1';
const urlTime = parseFloat(params.get('t'));
const urlPaused = params.get('paused') === '1';
const urlHud = params.get('hud');
const urlScope = params.get('scope') === '1';

let started = urlAutoplay;
let playing = !urlPaused;
// 启幕前静静停在第一段最美悬丝受压构图（静止美学，绝不做廉价循环预览）
let currentTime = Number.isFinite(urlTime)
  ? ((urlTime % TOTAL_DURATION) + TOTAL_DURATION) % TOTAL_DURATION
  : (started ? 0.0 : 4.2);
let hudPinnedHidden = urlHud === '0';
let hudHideTimer = null;
let scrubbing = false;

const userCtrl = {
  down: false,
  targetYaw: 0,
  targetPitch: 0,
  yaw: 0,
  pitch: 0,
  px: 0.5,
  py: 0.5,
};

let mediaRecorder = null;
let recordedChunks = [];

function handleResize() {
  const dpr = Math.min(window.devicePixelRatio || 1, 2);
  const w = Math.floor(window.innerWidth * dpr);
  const h = Math.floor(window.innerHeight * dpr);
  renderer.resize(w, h);
  overlayLayer.resize(w, h);
}

window.addEventListener('resize', handleResize);
handleResize();

window.addEventListener('pagehide', () => {
  try {
    const ext = renderer.gl && renderer.gl.getExtension('WEBGL_lose_context');
    if (ext) ext.loseContext();
  } catch (_) {}
});

if (urlScope) {
  stageEl.classList.add('scope-active');
  btnScope.classList.add('active');
}

if (started) {
  curtainEl.classList.add('dismissed');
  if (urlHud === '1') hudEl.classList.remove('hud-hidden');
}

function formatTime(sec) {
  const s = ((sec % TOTAL_DURATION) + TOTAL_DURATION) % TOTAL_DURATION;
  const mins = Math.floor(s / 60);
  const secs = Math.floor(s % 60);
  return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
}

function showHudTemporarily() {
  if (!started || hudPinnedHidden) return;
  hudEl.classList.remove('hud-hidden');
  clearTimeout(hudHideTimer);
  hudHideTimer = setTimeout(() => {
    if (!scrubbing && !hudEl.matches(':hover')) {
      hudEl.classList.add('hud-hidden');
    }
  }, 2400);
}

function updatePlayUi() {
  iconPlay.textContent = playing ? '❚❚' : '▶';
}

function updateMuteUi() {
  btnMute.textContent = audioEngine.muted ? '静音' : '有声';
  btnMute.classList.toggle('active', !audioEngine.muted);
}

function seekTo(sec) {
  currentTime = ((sec % TOTAL_DURATION) + TOTAL_DURATION) % TOTAL_DURATION;
  audioEngine.resetSync();
}

function enterScreening(withAudio) {
  started = true;
  playing = true;
  currentTime = 0.0;
  curtainEl.classList.add('dismissed');
  try {
    audioEngine.init();
    audioEngine.setMuted(!withAudio);
    audioEngine.resetSync();
  } catch (err) {
    console.warn('音频初始化跳过:', err);
  }
  updateMuteUi();
  updatePlayUi();
  showHudTemporarily();
}

btnEnter.addEventListener('click', (e) => {
  e.stopPropagation();
  enterScreening(true);
});

btnEnterMuted.addEventListener('click', (e) => {
  e.stopPropagation();
  enterScreening(false);
});

btnPlay.addEventListener('click', (e) => {
  e.stopPropagation();
  playing = !playing;
  updatePlayUi();
  showHudTemporarily();
});

btnMute.addEventListener('click', (e) => {
  e.stopPropagation();
  try {
    audioEngine.init();
    audioEngine.setMuted(!audioEngine.muted);
  } catch (_) {}
  updateMuteUi();
  showHudTemporarily();
});

btnScope.addEventListener('click', (e) => {
  e.stopPropagation();
  const on = stageEl.classList.toggle('scope-active');
  btnScope.classList.toggle('active', on);
  showHudTemporarily();
});

function scrubFromEvent(e) {
  const rect = scrubberEl.getBoundingClientRect();
  const ratio = clamp((e.clientX - rect.left) / Math.max(rect.width, 1), 0, 1);
  seekTo(ratio * TOTAL_DURATION);
}

scrubberEl.addEventListener('pointerdown', (e) => {
  e.stopPropagation();
  scrubbing = true;
  scrubberEl.setPointerCapture(e.pointerId);
  scrubFromEvent(e);
});

scrubberEl.addEventListener('pointermove', (e) => {
  if (!scrubbing) return;
  e.stopPropagation();
  scrubFromEvent(e);
});

scrubberEl.addEventListener('pointerup', (e) => {
  if (!scrubbing) return;
  e.stopPropagation();
  scrubbing = false;
  scrubberEl.releasePointerCapture(e.pointerId);
  showHudTemporarily();
});

// 按住画布拖拽：丝滑转动 3D 摄影机并轻扯丝线，松手以临界阻尼平稳归位
stageEl.addEventListener('pointerdown', (e) => {
  if (!started || e.target.closest('.hud-bottom')) return;
  userCtrl.down = true;
  const rect = stageEl.getBoundingClientRect();
  userCtrl.px = clamp((e.clientX - rect.left) / Math.max(rect.width, 1), 0, 1);
  userCtrl.py = clamp((e.clientY - rect.top) / Math.max(rect.height, 1), 0, 1);
});

stageEl.addEventListener('pointermove', (e) => {
  const rect = stageEl.getBoundingClientRect();
  const nx = clamp((e.clientX - rect.left) / Math.max(rect.width, 1), 0, 1);
  const ny = clamp((e.clientY - rect.top) / Math.max(rect.height, 1), 0, 1);
  userCtrl.px = nx;
  userCtrl.py = ny;

  if (userCtrl.down) {
    userCtrl.targetYaw = (nx - 0.5) * 1.1;
    userCtrl.targetPitch = (0.5 - ny) * 0.55;
  } else {
    userCtrl.targetYaw = 0;
    userCtrl.targetPitch = 0;
  }

  if (started && ny > 0.78) {
    showHudTemporarily();
  }
});

window.addEventListener('pointerup', () => {
  userCtrl.down = false;
  userCtrl.targetYaw = 0;
  userCtrl.targetPitch = 0;
});

function toggleRecording() {
  if (mediaRecorder && mediaRecorder.state === 'recording') {
    mediaRecorder.stop();
    return;
  }
  if (typeof MediaRecorder === 'undefined' || !glCanvas.captureStream) return;
  const stream = glCanvas.captureStream(60);
  const mime = MediaRecorder.isTypeSupported('video/webm;codecs=vp9') ? 'video/webm;codecs=vp9' : 'video/webm';
  recordedChunks = [];
  mediaRecorder = new MediaRecorder(stream, { mimeType: mime, videoBitsPerSecond: 14_000_000 });
  mediaRecorder.ondataavailable = (ev) => {
    if (ev.data && ev.data.size > 0) recordedChunks.push(ev.data);
  };
  mediaRecorder.onstop = () => {
    const blob = new Blob(recordedChunks, { type: mime });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `mochi-${Date.now()}.webm`;
    a.click();
    setTimeout(() => URL.revokeObjectURL(url), 4000);
    btnRecord.textContent = '录制';
    btnRecord.classList.remove('recording');
  };
  mediaRecorder.start();
  btnRecord.textContent = '保存';
  btnRecord.classList.add('recording');
}

btnRecord.addEventListener('click', (e) => {
  e.stopPropagation();
  toggleRecording();
});

window.addEventListener('keydown', (e) => {
  if (e.code === 'Space') {
    e.preventDefault();
    if (!started) enterScreening(true);
    else {
      playing = !playing;
      updatePlayUi();
      showHudTemporarily();
    }
  } else if (e.code === 'ArrowRight') {
    e.preventDefault();
    seekTo(currentTime + (e.shiftKey ? 1.0 : 1 / 30));
    showHudTemporarily();
  } else if (e.code === 'ArrowLeft') {
    e.preventDefault();
    seekTo(currentTime - (e.shiftKey ? 1.0 : 1 / 30));
    showHudTemporarily();
  } else if (e.key === 'h' || e.key === 'H') {
    hudPinnedHidden = !hudPinnedHidden;
    if (hudPinnedHidden) hudEl.classList.add('hud-hidden');
    else showHudTemporarily();
  } else if (e.key === 'c' || e.key === 'C') {
    const on = stageEl.classList.toggle('scope-active');
    btnScope.classList.toggle('active', on);
  } else if (e.key === 'm' || e.key === 'M') {
    try {
      audioEngine.init();
      audioEngine.setMuted(!audioEngine.muted);
    } catch (_) {}
    updateMuteUi();
  } else if (e.key === 'r' || e.key === 'R') {
    toggleRecording();
  }
});

let lastPerf = performance.now();

function frame(now) {
  const dt = Math.min((now - lastPerf) * 0.001, 0.05);
  lastPerf = now;

  if (started && playing && !scrubbing) {
    currentTime = (currentTime + dt) % TOTAL_DURATION;
  }

  const sp = orbitSpring.step(userCtrl.targetYaw, userCtrl.targetPitch, dt);
  userCtrl.yaw = sp.x;
  userCtrl.pitch = sp.y;

  const energy = audioEngine.update(currentTime, started && playing && !scrubbing);
  const scene = evaluateScene(currentTime, userCtrl, energy);
  renderer.render(scene);
  overlayLayer.render(scene.overlay);

  if (started && !hudEl.classList.contains('hud-hidden')) {
    const pct = (scene.t / TOTAL_DURATION) * 100;
    scrubberFill.style.width = `${pct.toFixed(2)}%`;
    scrubberThumb.style.left = `${pct.toFixed(2)}%`;
    hudTimecode.textContent = `${formatTime(scene.t)} / 00:42`;
  }

  requestAnimationFrame(frame);
}

orbitSpring.snap(0, 0);
updatePlayUi();
updateMuteUi();
requestAnimationFrame(frame);


})();
