#!/usr/bin/env python3
"""
Listet alle festen Sätze der App, die als echte Aufnahme gesprochen werden sollen (z. B. mit VoiceStudio auf dem PC).

Rollen:
  buersti  Super Bürsti, heldenhaft
  darth    Darth Bürsti, tiefer und feierlich (nur die Sätze vom Startbildschirm)
  monster1..3  ängstliche Monster, drei verschiedene Stimmen für Abwechslung

Super Bürstis Texte werden in Sätze zerlegt (an . ! ?). Steckt in einem Satz ein Name, eine Zahl o. Ä.,
wird er weiter an , und : zerlegt. Teile mit Kindernamen, Zahlen
oder anderen Einsetzungen bleiben weg; die spricht die App wie bisher mit der Gerätestimme.
Die App zerlegt genauso (siehe Voice.kt: sentences(), parts() und norm()), daher müssen beide Regeln gleich bleiben.

  python3 tools/stimme_saetze.py           # schreibt tools/stimme_saetze.json (Rolle, Text, Ziel-Datei)
  python3 tools/stimme_saetze.py --index   # nachdem die MP3s abgelegt sind: assets/stimme/index.json bauen

Die App spielt nur Sätze ab, die im Index stehen; alles andere spricht weiter die Gerätestimme.
"""
import hashlib, json, os, re, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "app/src/main/java/de/putzalarm/app")
OUT = os.path.join(ROOT, "app/src/main/assets/stimme")

LIT = re.compile(r'"((?:[^"\\\n]|\\.)*)"')
DYN = re.compile(r'\$\{[^}]*\}|\$[A-Za-z_][A-Za-z0-9_.]*')
SENT = re.compile(r'[^.!?]+[.!?]*')
PART = re.compile(r'[^,:]+[,:]*')


def norm(s):
    s = s.lower()
    s = re.sub(r'[^\w ]|_', ' ', s)
    return re.sub(r' +', ' ', s).strip()


def sentences(text):
    return [c.strip() for c in SENT.findall(text) if norm(c)]


def parts(sentence):
    return [c.strip() for c in PART.findall(sentence) if norm(c)]


def literals(path, start=None, end=None):
    src = open(path, encoding="utf-8").read()
    if start:
        a = src.index(start)
        b = src.index(end, a) if end else len(src)
        src = src[a:b]
    out = []
    for m in LIT.finditer(src):
        s = m.group(1)
        # nur echte Sätze: Großbuchstabe oder Einsetzung am Anfang, mindestens ein Wort mit Buchstaben
        if not re.search(r'[A-Za-zÄÖÜäöüß]{2}', s):
            continue
        if not (s[:1].isupper() or s.startswith("$") or s.startswith(" ")):
            continue
        if re.fullmatch(r'[A-Za-z0-9_./-]+', s):  # Schlüssel, Dateinamen
            continue
        out.append(s.replace('\\n', ' ').replace("\\'", "'").replace('\\"', '"'))
    return out


def fixed_clauses(texts):
    seen, out = set(), []
    for t in texts:
        for sen in sentences(DYN.sub("\0", t)):
            # Ganzer Satz, wenn nichts eingesetzt wird; sonst nur die festen Satzteile drumherum
            for c in ([sen] if "\0" not in sen else [p for p in parts(sen) if "\0" not in p]):
                k = norm(c)
                if not k or k in seen or re.search(r'[{}()<>=]', c) or k in ("else", "neu", "plopp"):
                    continue
                seen.add(k); out.append(c)
    return out


def screams():
    src = open(os.path.join(SRC, "ui/screens/BrushGame.kt"), encoding="utf-8").read()
    block = src[src.index("val Screams = linkedMapOf("):src.index("val ZoneLines")]
    return [m.group(2).replace('\\n', ' ') for m in re.finditer(r'"((?:[^"\\]|\\.)*)"\s+to\s+"((?:[^"\\]|\\.)*)"', block)]


def lines():
    home = literals(os.path.join(SRC, "ui/screens/HomeScreen.kt"))
    darth_only = literals(os.path.join(SRC, "ui/screens/HomeScreen.kt"), "private fun darth(", ")\n")
    other = []
    for f in ["ui/screens/ResultAlbum.kt", "ui/screens/Wardrobe.kt", "ui/screens/BrushScreen.kt"]:
        other += literals(os.path.join(SRC, f))
    other += literals(os.path.join(SRC, "ui/screens/BrushGame.kt"), "val ZoneLines", "\n")
    sc = set(screams())
    buersti = fixed_clauses([t for t in home + other if t not in sc and t not in darth_only])
    # Darth Bürsti spricht nur auf dem Startbildschirm; dort alle Sätze in seiner Stimme
    darth = fixed_clauses(home)
    roles = {"buersti": buersti, "darth": darth}
    for i in range(1, 4):
        roles[f"monster{i}"] = list(dict.fromkeys(screams()))
    return roles


ROLES = {
    "buersti": "Super Bürsti, heldenhaft und fröhlich",
    "darth": "Darth Bürsti, tief und feierlich",
    "monster1": "Monster, ängstlich (Stimme 1)",
    "monster2": "Monster, ängstlich (Stimme 2)",
    "monster3": "Monster, ängstlich (Stimme 3)",
}


def file_for(role, text):
    return f"{role}/{hashlib.sha1(norm(text).encode()).hexdigest()[:12]}.mp3"


def export():
    """Schreibt tools/stimme_saetze.json: welche Sätze in welcher Stimme gebraucht werden und wohin die MP3 gehört."""
    roles = lines()
    out = [{"rolle": r, "stimme": ROLES[r], "text": t, "datei": "app/src/main/assets/stimme/" + file_for(r, t)}
           for r, texts in roles.items() for t in texts]
    json.dump(out, open(os.path.join(ROOT, "tools/stimme_saetze.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    print(f"{len(out)} Sätze, {sum(len(o['text']) for o in out)} Zeichen -> tools/stimme_saetze.json")


def index():
    """Baut assets/stimme/index.json aus den MP3s, die schon da sind. Nur was eine Datei hat, spielt die App ab."""
    roles = lines()
    new, n = {}, 0
    for r, texts in roles.items():
        ent = {}
        for t in texts:
            f = file_for(r, t)
            if os.path.exists(os.path.join(OUT, f)):
                ent[norm(t)] = f; n += 1
        if ent:
            new[r] = ent
    os.makedirs(OUT, exist_ok=True)
    json.dump(new, open(os.path.join(OUT, "index.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1, sort_keys=True)
    print(f"{n} Aufnahmen im Index")


if __name__ == "__main__":
    index() if "--index" in sys.argv else export()
