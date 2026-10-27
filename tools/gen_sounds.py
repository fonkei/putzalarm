"""Erzeugt die Comic-Sounds der App (Monster-Schreie, PLOPP, Gong) synthetisch als WAV.

Aufruf: python3 tools/gen_sounds.py  (braucht numpy und scipy)
Schreibt nach app/src/main/res/raw/.
"""
import os
import wave

import numpy as np
from scipy.signal import butter, lfilter

SR = 22050
OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "raw")
rng = np.random.default_rng(7)

# Winzige Kariesmonster: Tonhöhe und Formanten kräftig nach oben (Mickymaus-Effekt)
PITCH = 1.9
FORMANT = 1.5

# Formanten (F1, F2, F3) für Kinder-/Monsterstimmen, leicht nach oben verschoben
VOWELS = {
    "a": (1000, 1500, 3200),
    "i": (420, 3000, 3900),
    "e": (650, 2400, 3500),
    "u": (480, 1100, 3000),
}


def env(n, attack=0.02, release=0.12):
    e = np.ones(n)
    a = max(1, int(attack * SR))
    r = max(1, int(release * SR))
    e[:a] = np.linspace(0, 1, a)
    e[-r:] *= np.linspace(1, 0, r) ** 1.5
    return e


def glide(points, n):
    """Tonhöhenverlauf durch Stützpunkte (Zeitanteil, Hz)."""
    t = np.linspace(0, 1, n)
    xs, ys = zip(*points)
    return np.interp(t, xs, ys)


def voice(f0, vowel_from, vowel_to=None, vib_rate=7.0, vib_depth=0.04, rough=0.0):
    n = len(f0)
    t = np.arange(n) / SR
    f = f0 * (1 + vib_depth * np.sin(2 * np.pi * vib_rate * t + rng.random() * 6))
    f *= 1 + rough * rng.standard_normal(n).cumsum() / np.sqrt(np.arange(1, n + 1)) * 0.05
    phase = np.cumsum(f) / SR
    # Stimmlippen-artiger Puls: Sägezahn mit weicher Kante
    src = 2 * (phase % 1.0) - 1
    src = np.tanh(src * 3)
    src += 0.04 * rng.standard_normal(n)  # Hauch
    out = np.zeros(n)
    vt = vowel_to or vowel_from
    # Formanten blockweise überblenden
    blocks = 12
    for b in range(blocks):
        s, e = b * n // blocks, (b + 1) * n // blocks
        k = b / max(1, blocks - 1)
        fa, fb = np.array(VOWELS[vowel_from]) * FORMANT, np.array(VOWELS[vt]) * FORMANT
        forms = fa * (1 - k) + fb * k
        seg = np.zeros(e - s)
        pad = src[max(0, s - 400):e]
        for i, fc in enumerate(forms):
            hi = min(fc * 1.18, SR / 2 - 200)
            lo = min(fc * 0.82, hi * 0.8)
            bb, aa = butter(2, [lo / (SR / 2), hi / (SR / 2)], btype="band")
            seg += lfilter(bb, aa, pad)[-(e - s):] * (1.0, 0.7, 0.35)[i]
        out[s:e] = seg
    return out


def norm(x, peak=0.85):
    return x / (np.max(np.abs(x)) + 1e-9) * peak


def write(name, x):
    os.makedirs(OUT, exist_ok=True)
    pcm = (np.clip(x, -1, 1) * 32767).astype(np.int16)
    with wave.open(os.path.join(OUT, name + ".wav"), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())


def scream(dur, points, v1, v2=None, **kw):
    n = int(dur * SR)
    x = voice(glide(points, n) * PITCH, v1, v2, **kw) * env(n, 0.015, min(0.2, dur * 0.4))
    return norm(x)


# --- Monster-Schreie ---
write("scream_iiih", scream(0.55, [(0, 700), (0.25, 1250), (1, 1450)], "i", vib_rate=9, vib_depth=0.05))
write("scream_aaah", scream(0.75, [(0, 1000), (0.2, 1150), (1, 520)], "a", vib_rate=8, vib_depth=0.07))
write("scream_uaaah", scream(0.8, [(0, 420), (0.3, 1050), (1, 650)], "u", "a", vib_rate=6, vib_depth=0.06))
write("scream_wheee", scream(0.9, [(0, 1500), (1, 380)], "i", "e", vib_rate=11, vib_depth=0.03))
write("scream_waaah", scream(0.6, [(0, 600), (0.5, 900), (1, 700)], "e", "a", vib_rate=16, vib_depth=0.09, rough=1.0))


def eek():
    parts = []
    for f in (1300, 1650):
        n = int(0.13 * SR)
        parts.append(voice(glide([(0, f * PITCH), (1, f * 1.35 * PITCH)], n), "i") * env(n, 0.005, 0.05))
        parts.append(np.zeros(int(0.05 * SR)))
    return norm(np.concatenate(parts))


write("scream_eek", eek())


# --- PLOPP: Blubb mit schnellem Tonhöhen-Abfall und Klick ---
def plopp():
    n = int(0.16 * SR)
    t = np.arange(n) / SR
    f = 750 * np.exp(-t * 28) + 110
    x = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 22)
    x[:60] += np.linspace(0.8, 0, 60) * rng.standard_normal(60) * 0.5
    return norm(x, 0.9)


write("plopp", plopp())


# --- Gong beim Zonenwechsel: zwei helle Glockentöne ---
def bell(freq, dur):
    n = int(dur * SR)
    t = np.arange(n) / SR
    x = sum(a * np.sin(2 * np.pi * freq * m * t) * np.exp(-t * d) for m, a, d in [(1, 1, 5), (2.01, 0.5, 8), (3.02, 0.25, 12), (4.1, 0.12, 16)])
    return x * env(n, 0.003, 0.05)


ding = np.concatenate([bell(1047, 0.18), bell(1319, 0.5)])
write("ding", norm(ding, 0.7))

# --- Countdown-Piep und Los-Fanfare ---
write("beep", norm(bell(880, 0.25), 0.6))
fan = np.concatenate([bell(784, 0.1), bell(988, 0.1), bell(1175, 0.1), bell(1568, 0.6)])
write("go", norm(fan, 0.75))

# --- Combo: kurzes Aufwärts-Arpeggio ---
write("combo", norm(np.concatenate([bell(f, 0.07) for f in (1047, 1319, 1568, 2093)] + [bell(2093, 0.3)]), 0.6))

# --- Jubel am Ende: Fanfare ---
write("fanfare", norm(np.concatenate([bell(523, 0.15), bell(659, 0.15), bell(784, 0.15), bell(1047, 0.25), bell(784, 0.12), bell(1047, 0.8)]), 0.75))
print("ok", sorted(os.listdir(OUT)))
