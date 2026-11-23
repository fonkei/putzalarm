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

Namenssätze ("Hallo Maja!"): Kindernamen in tools/stimme_namen.txt eintragen (eine Zeile je Name). Dann entsteht
zusätzlich tools/stimme_saetze_privat.json mit Zielen unter app/src/main/assets/stimme_privat/. Namen, diese Liste
und die Aufnahmen stehen in .gitignore: Sie bleiben auf dem PC und kommen nur über den Build dort in die App,
nie ins öffentliche Repo. Für andere Namen spricht weiter die Gerätestimme.

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


NAME_VARS = re.compile(r'\$\{(?:c|child)\.name\}|\$n\b')


def name_clauses(texts, names):
    """Sätze mit einem Kindernamen, für jeden Namen ausgeschrieben (z. B. "Hallo Maja!"). Andere Einsetzungen bleiben weg."""
    seen, out = set(), []
    for t in texts:
        if not NAME_VARS.search(t):
            continue
        for name in names:
            filled = DYN.sub("\0", NAME_VARS.sub(name, t))
            for sen in sentences(filled):
                for c in ([sen] if "\0" not in sen else [p for p in parts(sen) if "\0" not in p]):
                    k = norm(c)
                    if name.lower() not in k.split() or k in seen or re.search(r'[{}()<>=]', c):
                        continue
                    seen.add(k); out.append(c)
    return out


def screams():
    src = open(os.path.join(SRC, "ui/screens/BrushGame.kt"), encoding="utf-8").read()
    block = src[src.index("val Screams = linkedMapOf("):src.index("val ZoneLines")]
    return [m.group(2).replace('\\n', ' ') for m in re.finditer(r'"((?:[^"\\]|\\.)*)"\s+to\s+"((?:[^"\\]|\\.)*)"', block)]


def lines(names=None):
    home = literals(os.path.join(SRC, "ui/screens/HomeScreen.kt"))
    darth_only = literals(os.path.join(SRC, "ui/screens/HomeScreen.kt"), "private fun darth(", ")\n")
    other = []
    for f in ["ui/screens/ResultAlbum.kt", "ui/screens/Wardrobe.kt", "ui/screens/BrushScreen.kt"]:
        other += literals(os.path.join(SRC, f))
    other += literals(os.path.join(SRC, "ui/screens/BrushGame.kt"), "val ZoneLines", "\n")
    sc = set(screams())
    pick = (lambda ts: name_clauses(ts, names)) if names else fixed_clauses
    buersti = pick([t for t in home + other if t not in sc and t not in darth_only])
    # Darth Bürsti spricht nur auf dem Startbildschirm; dort alle Sätze in seiner Stimme
    darth = pick(home)
    roles = {"buersti": buersti, "darth": darth}
    if names:
        return roles
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


PRIV = os.path.join(ROOT, "app/src/main/assets/stimme_privat")
NAMES_FILE = os.path.join(ROOT, "tools/stimme_namen.txt")


def names():
    """Kindernamen nur aus tools/stimme_namen.txt (eine Zeile je Name). Die Datei bleibt auf dem PC, nicht im Repo."""
    if not os.path.exists(NAMES_FILE):
        return []
    return [l.strip() for l in open(NAMES_FILE, encoding="utf-8") if l.strip()]


def export():
    """Schreibt tools/stimme_saetze.json (feste Sätze, im Repo) und, wenn es tools/stimme_namen.txt gibt,
    tools/stimme_saetze_privat.json mit den Namenssätzen (bleibt auf dem PC, steht in .gitignore)."""
    roles = lines()
    out = [{"rolle": r, "stimme": ROLES[r], "text": t, "datei": "app/src/main/assets/stimme/" + file_for(r, t)}
           for r, texts in roles.items() for t in texts]
    json.dump(out, open(os.path.join(ROOT, "tools/stimme_saetze.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    print(f"{len(out)} Sätze, {sum(len(o['text']) for o in out)} Zeichen -> tools/stimme_saetze.json")
    nm = names()
    if nm:
        roles = lines(nm)
        out = [{"rolle": r, "stimme": ROLES[r], "text": t, "datei": "app/src/main/assets/stimme_privat/" + file_for(r, t)}
               for r, texts in roles.items() for t in texts]
        json.dump(out, open(os.path.join(ROOT, "tools/stimme_saetze_privat.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1)
        print(f"{len(out)} Namenssätze für {', '.join(nm)} -> tools/stimme_saetze_privat.json")


def build_index(roles, base):
    new, n = {}, 0
    for r, texts in roles.items():
        ent = {}
        for t in texts:
            f = file_for(r, t)
            if os.path.exists(os.path.join(base, f)):
                ent[norm(t)] = f; n += 1
        if ent:
            new[r] = ent
    os.makedirs(base, exist_ok=True)
    json.dump(new, open(os.path.join(base, "index.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1, sort_keys=True)
    return n


def index():
    """Baut assets/stimme/index.json (und assets/stimme_privat/index.json) aus den MP3s, die schon da sind.
    Nur was eine Datei hat, spielt die App ab."""
    print(f"{build_index(lines(), OUT)} Aufnahmen im Index")
    nm = names()
    if nm:
        print(f"{build_index(lines(nm), PRIV)} Namens-Aufnahmen im privaten Index")


if __name__ == "__main__":
    index() if "--index" in sys.argv else export()
