/**
 * 42 秒丝线材质电影程序化声景合成器（100% Web Audio API）
 * 紧扣丝线绷紧、拨弦回弹、融球液桥、玻璃泛音、31.0s 真空抽静与三境静切。
 */

export class MochiAudioEngine {
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
