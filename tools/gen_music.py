"""Komponiert die lustige Putzmusik der App (eigene Musik, keine Lizenzfragen) als nahtlose Schleife.

Aufruf: python3 tools/gen_music.py  (braucht numpy und ffmpeg)
Schreibt app/src/main/res/raw/putzmusik.ogg.
Stil: hüpfende Marimba-Polka in C-Dur mit Zupfbass, Bumm-Tschak und kleinen Pfeif-Einwürfen.
"""
import os
import subprocess
import tempfile
import wave

import numpy as np

SR = 32000
BPM = 132
BEAT = 60 / BPM
OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "raw", "putzmusik.ogg")
rng = np.random.default_rng(3)

NOTE = {n: i for i, n in enumerate(["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"])}


def hz(name):
    """'C5' -> Frequenz."""
    n, o = name[:-1], int(name[-1])
    return 440 * 2 ** ((NOTE[n] + 12 * (o - 4) - 9) / 12)


def marimba(f, dur):
    n = int(dur * SR)
    t = np.arange(n) / SR
    tone = np.sin(2 * np.pi * f * t) + 0.35 * np.sin(2 * np.pi * f * 4 * t) * np.exp(-t * 30) + 0.15 * np.sin(2 * np.pi * f * 10 * t) * np.exp(-t * 60)
    return tone * np.exp(-t * 7) * np.minimum(1, t * 400)


def bass(f, dur):
    n = int(dur * SR)
    t = np.arange(n) / SR
    tone = np.sin(2 * np.pi * f * t) + 0.4 * np.sin(2 * np.pi * 2 * f * t) + 0.15 * np.sin(2 * np.pi * 3 * f * t)
    return tone * np.exp(-t * 5) * np.minimum(1, t * 300)


def whistle(f0, f1, dur):
    """Lustiger Pfeif-Glissando-Einwurf."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    f = np.linspace(f0, f1, n) * (1 + 0.012 * np.sin(2 * np.pi * 6 * t))
    ph = 2 * np.pi * np.cumsum(f) / SR
    e = np.sin(np.pi * np.linspace(0, 1, n)) ** 0.7
    return np.sin(ph) * e


def kick(dur=0.25):
    n = int(dur * SR)
    t = np.arange(n) / SR
    f = 110 * np.exp(-t * 25) + 45
    return np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 14)


def tschak(dur=0.12):
    n = int(dur * SR)
    t = np.arange(n) / SR
    noise = rng.standard_normal(n)
    noise = noise - np.concatenate([[0], noise[:-1]])  # hell machen
    return noise * np.exp(-t * 35) * 0.5


def hat(dur=0.04):
    n = int(dur * SR)
    noise = rng.standard_normal(n)
    noise = noise - np.concatenate([[0], noise[:-1]])
    return noise * np.exp(-np.arange(n) / SR * 120) * 0.25


# Akkorde je Takt (2/4-Polka, aber in 4/4 notiert: 4 Schläge pro Takt)
CHORDS = ["C", "C", "G", "G", "F", "F", "G", "C",
          "C", "C", "G", "G", "F", "G", "C", "C"]
ROOT = {"C": ("C2", "G2"), "G": ("G2", "D3"), "F": ("F2", "C3")}
TRIAD = {"C": ["C4", "E4", "G4"], "G": ["B3", "D4", "G4"], "F": ["A3", "C4", "F4"]}

# Melodie: (Note oder None, Länge in Schlägen); 16 Takte
MEL = [
    ("G4", .5), ("C5", .5), ("E5", .5), ("C5", .5), ("G4", 1), (None, .5), ("G4", .5),
    ("A4", .5), ("G4", .5), ("E4", .5), ("G4", .5), ("C5", 1.5), (None, .5),
    ("B4", .5), ("D5", .5), ("G5", .5), ("D5", .5), ("B4", 1), (None, .5), ("B4", .5),
    ("C5", .5), ("B4", .5), ("A4", .5), ("B4", .5), ("G4", 1.5), (None, .5),
    ("A4", .5), ("C5", .5), ("F5", .5), ("C5", .5), ("A4", .5), ("C5", .5), ("F5", 1),
    ("E5", .5), ("D5", .5), ("C5", .5), ("B4", .5), ("A4", .5), ("B4", .5), ("C5", .5), ("D5", .5),
    ("E5", .25), ("F5", .25), ("E5", .5), ("D5", .5), ("B4", .5), ("G4", 1), (None, 1),
    ("C5", .5), ("G4", .5), ("E4", .5), ("G4", .5), ("C5", 1), (None, 1),
    # zweite Hälfte, etwas frecher
    ("E5", .5), ("E5", .5), ("G5", .5), ("E5", .5), ("C5", 1), (None, .5), ("C5", .5),
    ("D5", .5), ("C5", .5), ("A4", .5), ("C5", .5), ("E5", 1.5), (None, .5),
    ("D5", .5), ("D5", .5), ("F5", .5), ("D5", .5), ("B4", 1), (None, .5), ("B4", .5),
    ("C5", .5), ("D5", .5), ("E5", .5), ("D5", .5), ("B4", 1.5), (None, .5),
    ("A4", .5), ("C5", .5), ("F5", .5), ("A5", .5), ("G5", 1), ("F5", .5), ("E5", .5),
    ("D5", .5), ("G5", .5), ("F5", .5), ("D5", .5), ("B4", 1), ("G4", 1),
    ("C5", .5), ("E5", .5), ("G5", .5), ("E5", .5), ("C5", .5), ("G4", .5), ("E4", .5), ("G4", .5),
    ("C5", 1), (None, .5), ("G4", .25), ("A4", .25), ("B4", 1), (None, 1),
]

bars = len(CHORDS)
total = int(bars * 4 * BEAT * SR)
mix = np.zeros(total + SR * 2)


def put(sig, at_beats, gain):
    i = int(at_beats * BEAT * SR)
    mix[i:i + len(sig)] += sig * gain


# Melodie (Marimba) plus leise Oktave darüber
pos = 0.0
for note, dur in MEL:
    if note:
        put(marimba(hz(note), dur * BEAT + 0.5), pos, 0.42)
        put(marimba(hz(note) * 2, dur * BEAT + 0.3), pos, 0.08)
    pos += dur
assert abs(pos - bars * 4) < 1e-6, pos

for b, ch in enumerate(CHORDS):
    base = b * 4
    r, fifth = ROOT[ch]
    # Polka-Bass: Grundton – Akkord – Quinte – Akkord
    for k, note in enumerate([r, None, fifth, None]):
        if note:
            put(bass(hz(note), BEAT * 0.9), base + k, 0.5)
        else:
            for n in TRIAD[ch]:
                put(marimba(hz(n), BEAT * 0.5), base + k + 0.02, 0.09)
    for k in range(4):
        put(kick() if k % 2 == 0 else tschak(), base + k, 0.55 if k % 2 == 0 else 0.35)
        put(hat(), base + k + 0.5, 1.0)
    # Pfeif-Einwürfe am Ende jeder Viertakt-Phrase
    if b % 4 == 3:
        up = b % 8 == 3
        put(whistle(900 if up else 1500, 1500 if up else 800, BEAT * 0.8), base + 3, 0.12)

# Nahtlose Schleife: Nachklang an den Anfang falten
tail = mix[total:]
mix = mix[:total]
mix[:len(tail)] += tail
mix /= np.max(np.abs(mix)) * 1.12

with tempfile.TemporaryDirectory() as d:
    wav = os.path.join(d, "m.wav")
    with wave.open(wav, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes((mix * 32767).astype(np.int16).tobytes())
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", "3", OUT], check=True)
print(f"{OUT}: {total / SR:.1f} s")
