/**
 * 《Mochi》主驱动与交互控制器
 * 极简影院播放 + 按住画面平滑拖拽旋转 3D 机位与轻拉丝线 + 一键 WebM 录屏导出。
 */

import { TOTAL_DURATION, evaluateScene } from './director.js';
import { MochiFollowSpring, clamp } from './physics.js';
import { StudioRenderer } from './renderer.js';
import { OverlayLayer } from './interstitial.js';
import { MochiAudioEngine } from './audio.js';

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
