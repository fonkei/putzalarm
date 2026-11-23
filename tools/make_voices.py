#!/usr/bin/env python3
"""
Erzeugt die gesprochenen Sätze der App einmal mit ElevenLabs als MP3 (läuft in GitHub Actions).

Rollen:
  buersti  Super Bürsti, heldenhaft
  darth    Darth Bürsti, tiefer und feierlich (nur die Sätze vom Startbildschirm)
  monster1..3  ängstliche Monster, drei verschiedene Stimmen für Abwechslung

Super Bürstis Texte werden in Sätze zerlegt (an . ! ?). Steckt in einem Satz ein Name, eine Zahl o. Ä.,
wird er weiter an , und : zerlegt. Teile mit Kindernamen, Zahlen
oder anderen Einsetzungen bleiben weg; die spricht die App wie bisher mit der Gerätestimme.
Die App zerlegt genauso (siehe Voice.kt: sentences(), parts() und norm()), daher müssen beide Regeln gleich bleiben.

Ergebnis: app/src/main/assets/stimme/index.json und die MP3s daneben. Schon erzeugte Dateien
bleiben liegen, neu erzeugt wird nur, was fehlt oder dessen Stimme in tools/voices.json geändert wurde.

  python3 tools/make_voices.py --dry-run   # nur auflisten und Zeichen zählen
"""
import hashlib, json, os, re, sys, time, urllib.request, urllib.error

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "app/src/main/java/de/putzalarm/app")
OUT = os.path.join(ROOT, "app/src/main/assets/stimme")
CONF = json.load(open(os.path.join(ROOT, "tools/voices.json"), encoding="utf-8"))

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


def tts(text, role):
    c = CONF[role]
    model = CONF.get("model", "eleven_v3")
    spoken = (c.get("tag", "") + " " + text).strip() if model == "eleven_v3" else text
    body = {"text": spoken, "model_id": model, "language_code": "de",
            "voice_settings": c.get("settings", {"stability": 0.5, "similarity_boost": 0.75})}
    url = f"https://api.elevenlabs.io/v1/text-to-speech/{c['voice']}?output_format=mp3_22050_32"
    for attempt in range(5):
        req = urllib.request.Request(url, json.dumps(body).encode(), {
            "xi-api-key": os.environ["ELEVENLABS_API_KEY"], "Content-Type": "application/json", "Accept": "audio/mpeg"})
        try:
            with urllib.request.urlopen(req, timeout=60) as r:
                return r.read()
        except urllib.error.HTTPError as e:
            msg = e.read().decode(errors="replace")[:300]
            if e.code in (429, 500, 502, 503) and attempt < 4:
                time.sleep(3 * (attempt + 1)); continue
            sys.exit(f"ElevenLabs {e.code} bei '{text}' ({role}): {msg}")


def main():
    dry = "--dry-run" in sys.argv
    roles = lines()
    os.makedirs(OUT, exist_ok=True)
    ip = os.path.join(OUT, "index.json")
    old = json.load(open(ip, encoding="utf-8")) if os.path.exists(ip) else {}
    new = {"_meta": {}}
    total = todo_chars = 0
    for role, texts in roles.items():
        c = CONF[role]
        meta = f"{c['voice']}|{CONF.get('model')}|{c.get('tag','')}|{json.dumps(c.get('settings',{}), sort_keys=True)}"
        same = old.get("_meta", {}).get(role) == meta
        new["_meta"][role] = meta
        entries = {}
        os.makedirs(os.path.join(OUT, role), exist_ok=True)
        for t in texts:
            k = norm(t)
            name = f"{role}/{hashlib.sha1(k.encode()).hexdigest()[:12]}.mp3"
            path = os.path.join(OUT, name)
            total += len(t)
            if not (same and old.get(role, {}).get(k) == name and os.path.exists(path)):
                todo_chars += len(t)
                if dry:
                    print(f"[{role}] {t}")
                else:
                    open(path, "wb").write(tts(t, role))
                    print(f"[{role}] {t}", flush=True)
            entries[k] = name
        new[role] = entries
    print(f"{sum(len(v) for v in roles.values())} Sätze, {total} Zeichen gesamt, davon neu zu erzeugen: {todo_chars} Zeichen")
    if dry:
        return
    # Nicht mehr gebrauchte Dateien entfernen
    keep = {n for r in roles for n in new[r].values()}
    for role in os.listdir(OUT):
        d = os.path.join(OUT, role)
        if os.path.isdir(d):
            for f in os.listdir(d):
                if f"{role}/{f}" not in keep:
                    os.remove(os.path.join(d, f))
    json.dump(new, open(ip, "w", encoding="utf-8"), ensure_ascii=False, indent=1, sort_keys=True)


if __name__ == "__main__":
    main()
