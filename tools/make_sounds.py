"""Synthesizes Outbreak's gun sounds (mono .ogg)."""
import os, sys
import numpy as np
import soundfile as sf
OUT = sys.argv[1]; os.makedirs(OUT, exist_ok=True)
SR = 44100
rng = np.random.default_rng(11)

def lp(x, cutoff):
    a = np.exp(-2*np.pi*cutoff/SR); y = np.empty_like(x); acc = 0.0
    for i in range(len(x)):
        acc = (1-a)*x[i] + a*acc; y[i] = acc
    return y
def hp(x, c): return x - lp(x, c)
def t(sec): return np.arange(int(SR*sec))/SR
def save(name, x, peak=0.95):
    x = x/(np.max(np.abs(x))+1e-9)*peak
    sf.write(os.path.join(OUT, name+'.ogg'), np.clip(x, -1, 1).astype(np.float32), SR, format='OGG', subtype='VORBIS')

def shot(name, length, crack, body, boom_hz, boom, decay, tail, tail_len):
    tt = t(length); n = rng.standard_normal(len(tt))
    e_fast = np.exp(-tt/decay)
    x = hp(n, 2500)*crack*np.exp(-tt/0.006)            # supersonic crack
    x += lp(n, 1800)*body*e_fast                          # muzzle blast
    x += np.sin(2*np.pi*boom_hz*tt*(1-0.4*tt/length))*boom*np.exp(-tt/(decay*2.5))  # chest thump
    # echo/reverb tail off buildings
    tail_env = np.exp(-tt/tail_len)*(1-np.exp(-tt/0.02))
    x += lp(n, 700)*tail*tail_env
    for d, g in ((0.09, 0.35), (0.21, 0.2), (0.37, 0.1)):
        k = int(d*SR); y = np.zeros_like(x); y[k:] = x[:-k]*g; x = x + lp(y, 1200)
    save(name, x)

shot('pistol_fire', 0.9, 1.0, 1.2, 140, 0.6, 0.03, 0.35, 0.25)
shot('smg_fire', 0.55, 0.8, 1.0, 170, 0.4, 0.02, 0.25, 0.15)
shot('shotgun_fire', 1.4, 0.7, 1.8, 70, 1.1, 0.06, 0.6, 0.45)
shot('rifle_fire', 1.6, 1.6, 1.3, 95, 0.8, 0.04, 0.7, 0.55)

def click(tt0, length, freq, amp):
    tt = t(length); n = rng.standard_normal(len(tt))
    return (hp(n, 3000)*0.7 + np.sin(2*np.pi*freq*tt))*np.exp(-tt/0.004)*amp

def seq(total, parts):
    x = np.zeros(int(SR*total))
    for at, sig in parts:
        k = int(at*SR); x[k:k+len(sig)] += sig[:len(x)-k]
    return x

save('reload', seq(1.0, [(0.0, click(0, 0.05, 900, 0.8)), (0.12, lp(rng.standard_normal(int(SR*0.18)), 2500)*0.25*np.hanning(int(SR*0.18))),
                         (0.45, click(0, 0.05, 1300, 1.0)), (0.62, click(0, 0.05, 700, 0.9)), (0.7, click(0, 0.06, 1800, 1.0))]), 0.7)
save('empty_click', seq(0.15, [(0.0, click(0, 0.05, 2200, 1.0))]), 0.6)
print('sounds ok')

# wave 2
shot('silenced_fire', 0.35, 0.15, 0.5, 220, 0.15, 0.012, 0.05, 0.06)
shot('revolver_fire', 1.1, 1.1, 1.5, 120, 0.8, 0.04, 0.45, 0.32)
shot('sniper_fire', 2.2, 2.0, 1.5, 80, 1.0, 0.05, 0.9, 0.8)
print('more sounds ok')
