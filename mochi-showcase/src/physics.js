/**
 * Mochi 真实物理与运动力学库
 * 精确还原 MochiMotionTokens、二阶弹簧、iOS 橡皮筋、速度果冻与融球融合系数。
 */

export const MOCHI_TOKENS = {
  Page:         { response: 0.42, damping: 1.00 },
  Present:      { response: 0.42, damping: 0.86 },
  Pop:          { response: 0.36, damping: 0.78 },
  Control:      { response: 0.34, damping: 0.86 },
  Settle:       { response: 0.28, damping: 1.00 },
  Bounce:       { response: 0.34, damping: 0.55 },
  PressDown:    { response: 0.22, damping: 0.62 },
  PressRelease: { response: 0.38, damping: 0.62 },
};

export function clamp(x, min = 0, max = 1) {
  return x < min ? min : x > max ? max : x;
}

export function lerp(a, b, t) {
  return a + (b - a) * t;
}

export function smoothstep(edge0, edge1, x) {
  const t = clamp((x - edge0) / (edge1 - edge0), 0, 1);
  return t * t * (3 - 2 * t);
}

export function smootherstep(edge0, edge1, x) {
  const t = clamp((x - edge0) / (edge1 - edge0), 0, 1);
  return t * t * t * (t * (t * 6 - 15) + 10);
}

export function remap(x, inMin, inMax, outMin = 0, outMax = 1) {
  return lerp(outMin, outMax, clamp((x - inMin) / (inMax - inMin), 0, 1));
}

/** 电影节级非对称指数急停曲线 */
export function expoSnap(t) {
  const x = clamp(t, 0, 1);
  if (x === 0 || x === 1) return x;
  return x < 0.5
    ? Math.pow(2, 20 * x - 10) * 0.5
    : (2 - Math.pow(2, -20 * x + 10)) * 0.5;
}

export function expoOut(t) {
  const x = clamp(t, 0, 1);
  return x === 1 ? 1 : 1 - Math.pow(2, -10 * x);
}

export function expoIn(t) {
  const x = clamp(t, 0, 1);
  return x === 0 ? 0 : Math.pow(2, 10 * x - 10);
}

export function quintOut(t) {
  const x = 1 - clamp(t, 0, 1);
  return 1 - x * x * x * x * x;
}

/**
 * 确定性二阶弹簧解析求解（给定触发后经过的秒数 dt，返回 { value, velocity }）
 * 支持任意拖动时间轴和精准逐帧渲染，绝不依赖历史状态漂移。
 */
export function springEval(dt, token = MOCHI_TOKENS.Bounce) {
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
export function applyHitStop(t, impactTime, holdSec = 0.036) {
  if (t < impactTime) return t;
  if (t < impactTime + holdSec) return impactTime;
  return t - holdSec;
}

/**
 * iOS UIScrollView 橡皮筋越界阻尼公式
 * 刚拉出去 1:1 跟手，越远越拉不动，软上限 maxPull。
 */
export function rubberBand(overshoot, dimension, maxPull) {
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
export function jellyStretch(velocity, gel = 0.55) {
  const v = clamp(Math.abs(velocity), 0, 5.0);
  return 1 + Math.tanh(v * 0.34) * gel * 0.42;
}

export function jellyCrossScale(stretch) {
  return 1 / Math.sqrt(Math.max(stretch, 0.4));
}

/**
 * Mochi 融球距离融合系数 proximityFuse
 */
export function proximityFuse(dist, restGap) {
  if (dist >= restGap) return 0;
  return smoothstep(1, 0, clamp(dist / Math.max(restGap, 1e-4), 0, 1));
}

/**
 * MochiFollowSpring：用于实时鼠标/触控指针的二维跟手弹簧
 */
export class MochiFollowSpring {
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
export function mochiGlowCurve(rNorm) {
  const r = Math.max(0, rNorm);
  return 0.85 * Math.exp(-r * r * r * 1.6);
}
