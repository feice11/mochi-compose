/**
 * 《Mochi》全屏实时 3D 摄影棚 PBR 光线步进着色器（极速零冗余内联版 · GLSL 300 es）
 * 全平台（ANGLE D3D11 / SwiftShader / Metal / Vulkan）毫秒级编译链接，稳定 60fps。
 */

export const FULLSCREEN_VS = `#version 300 es
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

export const STUDIO_3D_FS = `#version 300 es
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

export const POST_CINEMA_FS = `#version 300 es
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
