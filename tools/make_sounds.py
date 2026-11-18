"""Synthesizes all Vehicle Forge sounds (mono .ogg) with numpy + ffmpeg."""
import os, subprocess, sys, wave
import numpy as np
OUT = sys.argv[1]; os.makedirs(OUT, exist_ok=True)
SR = 22050
rng = np.random.default_rng(7)
def t(sec): return np.arange(int(SR * sec)) / SR
def lowpass(x, cutoff):
    a = np.exp(-2 * np.pi * cutoff / SR); y = np.zeros_like(x); acc = 0.0
    for i in range(len(x)):
        acc = (1 - a) * x[i] + a * acc; y[i] = acc
    return y
def highpass(x, cutoff): return x - lowpass(x, cutoff)
def saw(f, tt, harm=12):
    return sum(np.sin(2*np.pi*f*k*tt) / k for k in range(1, harm+1))
def norm(x, peak=0.9): return x / (np.max(np.abs(x)) + 1e-9) * peak
def env(n, a, r):
    e = np.ones(n); ai = int(a*SR); ri = int(r*SR)
    if ai: e[:ai] = np.linspace(0, 1, ai)
    if ri: e[-ri:] = np.linspace(1, 0, ri)
    return e
def save(name, x):
    import soundfile as sf
    x = np.clip(x, -1, 1).astype(np.float32)
    sf.write(os.path.join(OUT, name + '.ogg'), x, SR, format='OGG', subtype='VORBIS')

def engine(name, f, rumble, rasp, pulse_hz, extra=None):
    tt = t(1.0)
    x = saw(f, tt, 10) * 0.6 + saw(f*2, tt, 6) * 0.25
    x *= 1 + 0.35 * np.sin(2*np.pi*pulse_hz*tt)
    n = rng.standard_normal(len(tt))
    x += lowpass(n, 300) * rumble + highpass(lowpass(n, 2500), 600) * rasp
    if extra is not None: x += extra(tt)
    save(name, norm(lowpass(x, 3000)) * 0.8)

engine('engine_car', 55, 0.8, 0.15, 27.5)
engine('engine_truck', 40, 1.4, 0.2, 20)
engine('engine_bike', 90, 0.5, 0.5, 45)
engine('engine_boat', 70, 1.0, 0.2, 35, lambda tt: lowpass(rng.standard_normal(len(tt)), 900) * 0.6)
engine('engine_tank', 30, 2.0, 0.1, 15, lambda tt: (np.sin(2*np.pi*8*tt) > 0.97) * rng.standard_normal(len(tt)) * 1.2)
engine('engine_prop', 110, 0.3, 0.3, 22)

tt = t(1.0)
blade = np.zeros(len(tt))
for k in range(6):
    c = int(k * SR / 6); n = int(0.07 * SR)
    blade[c:c+n] += lowpass(rng.standard_normal(n), 400) * np.hanning(n) * 3
heli = blade + 0.08 * np.sin(2*np.pi*1200*tt) + lowpass(rng.standard_normal(len(tt)), 200) * 0.4
save('engine_heli', norm(heli) * 0.85)

jet = lowpass(rng.standard_normal(len(tt)), 1200) * 1.0 + highpass(lowpass(rng.standard_normal(len(tt)), 5000), 2000) * 0.25 + 0.05 * np.sin(2*np.pi*2000*tt)
save('engine_jet', norm(jet) * 0.85)
rocket = lowpass(rng.standard_normal(len(tt)), 250) * 2 + lowpass(rng.standard_normal(len(tt)), 3000) * 0.5
rocket += (rng.random(len(tt)) > 0.995) * rng.standard_normal(len(tt)) * 3
save('engine_rocket', norm(rocket) * 0.9)
chuff = np.zeros(len(tt))
for k in range(4):
    c = int(k * SR / 4); n = int(0.16 * SR)
    chuff[c:c+n] += lowpass(rng.standard_normal(n), 1500) * np.exp(-np.linspace(0, 6, n))
train = chuff * 1.5 + saw(35, tt, 4) * 0.2
save('engine_train', norm(train) * 0.85)

def tone_chord(freqs, sec, a, r, harm=10, vib=0):
    tt = t(sec); x = sum(saw(f * (1 + vib*np.sin(2*np.pi*5*tt)), tt, harm) for f in freqs)
    return norm(lowpass(x, 3500)) * env(len(tt), a, r)
save('horn_car', tone_chord([400, 500], 0.6, 0.01, 0.08, 6) * 0.8)
save('horn_truck', tone_chord([220, 277, 330], 1.0, 0.03, 0.15) * 0.9)
save('horn_train', tone_chord([311, 370, 440, 554], 2.0, 0.12, 0.35) * 0.9)
save('horn_boat', tone_chord([98, 147], 2.2, 0.15, 0.4) * 0.95)
tt = t(1.2)
bell = np.zeros(len(tt))
for start in (0.0, 0.25):
    s = int(start * SR); seg = tt[: len(tt) - s]
    ring = (np.sin(2*np.pi*2400*seg) + 0.6*np.sin(2*np.pi*3700*seg) + 0.4*np.sin(2*np.pi*5300*seg)) * np.exp(-seg * 6)
    bell[s:] += ring
save('bike_bell', norm(bell) * 0.7)
tt = t(1.7)
freq = 950 + 350 * np.sin(2*np.pi*tt/1.7 - np.pi/2)
phase = 2*np.pi*np.cumsum(freq)/SR
siren = np.sign(np.sin(phase)) * 0.4 + np.sin(phase) * 0.6
save('siren', norm(lowpass(siren, 4000)) * 0.75)
tt = t(1.6)
boom = np.sin(2*np.pi*48*tt) * np.exp(-tt*4) * 1.5 + lowpass(rng.standard_normal(len(tt)), 800) * np.exp(-tt*5) * 2
boom[: int(0.02*SR)] += rng.standard_normal(int(0.02*SR)) * 2
save('cannon', norm(boom) * 0.95)
tt = t(1.5)
cut = 300 + 2500 * (tt / 1.5)
wh = np.zeros(len(tt)); n = rng.standard_normal(len(tt)); acc = 0.0
for i in range(len(tt)):
    a = np.exp(-2*np.pi*cut[i]/SR); acc = (1-a)*n[i] + a*acc; wh[i] = acc
whoosh = wh * env(len(tt), 0.05, 0.6) * 3 + lowpass(rng.standard_normal(len(tt)), 150) * np.exp(-tt*2) * 2
save('rocket_launch', norm(whoosh) * 0.9)
tt = t(0.3)
shot = rng.standard_normal(len(tt)) * np.exp(-tt*35) * 2 + np.sin(2*np.pi*90*tt) * np.exp(-tt*25)
save('gunshot', norm(highpass(shot, 80)) * 0.9)
tt = t(0.7)
spray = highpass(lowpass(rng.standard_normal(len(tt)), 6000), 1500) * env(len(tt), 0.05, 0.2)
save('water_spray', norm(spray) * 0.6)
print('sounds ok')

# ---- wave 2: tyre squeal, crash, refuel
tt = t(0.8)
n = rng.standard_normal(len(tt))
sq = highpass(lowpass(n, 2600), 1400) * 2.5 + np.sin(2*np.pi*(1900 + 120*np.sin(2*np.pi*7*tt))*tt) * 0.6
save('skid', norm(sq * env(len(tt), 0.04, 0.3)) * 0.7)
tt = t(1.1)
thud = np.sin(2*np.pi*55*tt) * np.exp(-tt*9) * 2
crunch = lowpass(rng.standard_normal(len(tt)), 2500) * np.exp(-tt*7) * 2
ring = (np.sin(2*np.pi*720*tt) + 0.6*np.sin(2*np.pi*1130*tt) + 0.4*np.sin(2*np.pi*1710*tt)) * np.exp(-tt*5) * 0.5
glass = highpass(rng.standard_normal(len(tt)), 3500) * (rng.random(len(tt)) > 0.97) * np.exp(-tt*4) * 2
save('crash', norm(thud + crunch + ring + glass) * 0.95)
tt = t(1.2)
glug = np.zeros(len(tt))
for k in range(7):
    c = int((0.05 + k * 0.16) * SR); m = int(0.1 * SR)
    seg = np.arange(m) / SR
    glug[c:c+m] += np.sin(2*np.pi*(180 + 90*seg/0.1)*seg) * np.hanning(m)
glug += lowpass(rng.standard_normal(len(tt)), 800) * 0.15
save('fuel_fill', norm(glug) * 0.7)
print('wave2 sounds ok')

# ---- wave 3
tt = t(0.9)   # ostrich: booming hoot + squawk
hoot = np.sin(2*np.pi*(140 - 40*tt)*tt) * env(len(tt), 0.05, 0.3) * 1.2
sq = highpass(lowpass(rng.standard_normal(len(tt)), 3000), 800) * np.exp(-((tt-0.6)/0.08)**2) * 1.5
save('ostrich_call', norm(hoot + sq + np.sin(2*np.pi*900*tt) * np.exp(-((tt-0.6)/0.06)**2) * 0.6) * 0.85)
tt = t(2.2)   # time warp: rising shimmer, whoosh, thump
f = 200 + 1800 * (tt / 2.2) ** 2
ph = 2*np.pi*np.cumsum(f)/SR
warp = np.sin(ph) * 0.5 + np.sin(ph * 1.5) * 0.3 + lowpass(rng.standard_normal(len(tt)), 2000) * (tt/2.2) * 0.8
warp += np.sin(2*np.pi*50*tt) * np.exp(-np.maximum(0, tt-1.9)*20) * (tt > 1.9) * 2
save('time_warp', norm(warp * env(len(tt), 0.1, 0.15)) * 0.9)
tt = t(1.0)   # shrink: descending sci-fi zap
f = 1800 * np.exp(-tt * 2.5) + 150
ph = 2*np.pi*np.cumsum(f)/SR
save('shrink', norm((np.sin(ph) + 0.4*np.sign(np.sin(ph*0.5))) * env(len(tt), 0.01, 0.3)) * 0.7)
# ice cream jingle: an original little tune on a music-box tone
notes = [72, 76, 79, 76, 74, 77, 81, 77, 76, 79, 84, 79, 77, 76, 74, 72]
seg = int(0.18 * SR); jingle = np.zeros(seg * len(notes) + SR)
for k, n in enumerate(notes):
    fr = 440 * 2 ** ((n - 69) / 12); s_ = np.arange(int(0.6*SR)) / SR
    tone = (np.sin(2*np.pi*fr*s_) + 0.4*np.sin(2*np.pi*fr*2*s_) + 0.2*np.sin(2*np.pi*fr*3.01*s_)) * np.exp(-s_*5)
    jingle[k*seg:k*seg+len(tone)] += tone
save('ice_cream_jingle', norm(jingle) * 0.75)
tt = t(1.0)   # electric motor whine
el = np.sin(2*np.pi*620*tt) * 0.5 + np.sin(2*np.pi*1240*tt) * 0.2 + lowpass(rng.standard_normal(len(tt)), 400) * 0.2
save('engine_electric', norm(el) * 0.5)
tt = t(1.0)   # propane burner roar (loop)
save('burner', norm(lowpass(rng.standard_normal(len(tt)), 900) * 1.2 + highpass(lowpass(rng.standard_normal(len(tt)), 4000), 1500) * 0.3) * 0.8)
tt = t(1.0)   # ufo hum (loop)
hum = np.sin(2*np.pi*110*tt) * (1 + 0.4*np.sin(2*np.pi*6*tt)) + np.sin(2*np.pi*330*tt) * 0.4 * (1 + np.sin(2*np.pi*2*tt)) + np.sin(2*np.pi*1760*tt) * 0.06
save('ufo_hum', norm(hum) * 0.6)
tt = t(1.6)   # compactor: hydraulic whine + crunch
hyd = np.sin(2*np.pi*(180 + 60*tt)*tt) * 0.5 + lowpass(rng.standard_normal(len(tt)), 600) * 0.6
crunch = lowpass(rng.standard_normal(len(tt)), 3000) * ((tt > 0.9) & (tt < 1.3)) * 1.5
save('compactor', norm((hyd + crunch) * env(len(tt), 0.05, 0.2)) * 0.85)
print('wave3 sounds ok')
