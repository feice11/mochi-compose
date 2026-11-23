/**
 * WebGL2 双通道 3D 摄影棚渲染管线
 * CPU 预计算逆旋转矩阵，向 GPU 传入零嵌套循环的扁平 Uniform，全平台毫秒级编译与 60fps 渲染。
 */

import { FULLSCREEN_VS, STUDIO_3D_FS, POST_CINEMA_FS } from './studio_shader.js';

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

export class StudioRenderer {
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
