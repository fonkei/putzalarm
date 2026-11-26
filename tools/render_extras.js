// Rendert die Erweiterungen vom November 2026 aus design/super-buersti-3d-modell.html, ohne die vorhandenen Bilder anzufassen:
//  - neue Moves (alle Kostüme, Umhang, Mund) und neue Kostüme (alle Moves)
//  - Ausrüstung als eigene Ebene je Bild: acc_<teil>_<pose>_<bild>
//  - Schlüpflinge aus dem Überraschungsei: germ_<form>_egg_c<farbe>_f0
//  - Riesen-Bosse: giant_<nr>_f<0|1>
// Aufruf: node tools/render_extras.js <pfad/zu/three.min.js (three@0.149.0)> [Ausgabeordner]
// Danach: python3 tools/to_webp.py (wandelt die PNGs in WebP um). hero_layout.json wird ergänzt, alte Einträge bleiben.
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const out = process.argv[3] ? path.resolve(process.argv[3]) : path.join(root, 'app/src/main/res/drawable-nodpi');
const layoutFile = path.join(root, 'app/src/main/res/raw/hero_layout.json');
const PALETTE = [0xff5d8f, 0x7bdc5a, 0xffb627, 0x5ac8fa, 0xb57bff, 0xff7a5c, 0x3ee0b0, 0xffd23f];

// Was schon in der App ist (nicht neu rendern)
const OLD_MOVES = ['dance', 'brush', 'box', 'moonwalk', 'karate', 'jack', 'spin', 'robot', 'guitar', 'fly'];
const OLD_COSTUMES = ['gold', 'chrom', 'neon', 'darth'];
const NEW_COSTUMES = ['feuer', 'eis'];
// Schlüpflinge: [Form, Palettenfarbe] wie in data/Data.kt (EggMonsters)
const EGGS = [['blob', 0], ['star', 7], ['drop', 3], ['bean', 4], ['cube', 6], ['jelly', 1], ['spike', 2], ['cyclops', 5]];
// Riesen-Bosse wie in data/Data.kt (Giants): Form, Farbe, Extras
const GIANTS = [
  ['blob', 0xff4f8b, { horns: 2, fangs: true, ears: true }],
  ['spike', 0x7a4a2e, { wings: true, fangs: true, wingColor: 0x3a2014 }],
  ['cyclops', 0x5fcf3a, { horns: 1, fangs: true, spikes: true }],
  ['bean', 0xb57bff, { crown: true, fangs: true }],
  ['jelly', 0x3aa6ff, { tentacles: true, horns: 2, hornColor: 0xdff4ff }],
  ['cube', 0x3b2a55, { horns: 4, wings: true, wingColor: 0xff3d7f, fangs: true }],
  ['star', 0xffb627, { crown: true, spikes: true, fangs: true }],
  ['drop', 0xff7a5c, { horns: 2, wings: true, wingColor: 0xffd23f }],
];

(async () => {
  fs.mkdirSync(out, { recursive: true });
  const browser = await chromium.launch({ args: ['--use-gl=swiftshader', '--enable-unsafe-swiftshader'] });
  const page = await browser.newPage({ viewport: { width: 1000, height: 800 } });
  page.on('pageerror', e => console.error(e.message));
  page.on('console', m => { if (m.type() === 'error') console.error(m.text()); });
  await page.route('**/three.min.js', r => r.fulfill({ body: fs.readFileSync(process.argv[2]), contentType: 'application/javascript' }));
  await page.route('**fonts.googleapis.com**', r => r.abort());
  await page.goto('file://' + path.join(root, 'design/super-buersti-3d-modell.html') + '?render');
  await page.waitForFunction(() => window.PUTZ);

  // QUICK=move1,move2: nur Körper (Gold) und Umhang dieser Moves, zum Ausprobieren
  const QUICK = process.env.QUICK ? process.env.QUICK.split(',') : null;
  const shots = await page.evaluate(({ OLD_MOVES, OLD_COSTUMES, NEW_COSTUMES, EGGS, GIANTS, PALETTE, QUICK }) => {
    const P = window.PUTZ, T = THREE, res = {};
    const { renderer, scene, hero } = P;
    renderer.setPixelRatio(1);
    renderer.setClearColor(0x000000, 0);
    P.glowDisc.visible = false;
    P.ground.visible = false;
    P.mons.forEach(m => { m.g.visible = false; });
    Object.values(P.ACC).forEach(a => { a.visible = false; });
    const grab = (cam, w, h) => { renderer.setSize(w, h, false); renderer.render(scene, cam); return renderer.domElement.toDataURL('image/png'); };

    P.setBody('hero');
    const cam = new T.PerspectiveCamera(32, 3 / 4, 0.1, 100);
    cam.position.set(0, 2.05, 8.1); cam.lookAt(0, 1.98, 0);
    const W = 450, H = 600, MW = 84, MH = 64;
    const depthOnly = new T.MeshBasicMaterial({ colorWrite: false });
    const capeWhite = P.cape.material.clone(); capeWhite.color.setHex(0xffffff);
    const capeMat = P.cape.material;
    const accMeshes = new Set();
    Object.values(P.ACC).forEach(a => a.traverse(m => { if (m.isMesh) accMeshes.add(m); }));
    // which: 'body' (ohne Umhang), 'cape' oder ein Ausrüstungsteil; alles andere schreibt nur Tiefe
    const layer = (which, accGroup) => {
      const swap = [];
      hero.traverse(m => {
        if (!m.isMesh) return;
        let show;
        if (accMeshes.has(m)) show = which === 'acc' && accGroup.getObjectById(m.id) !== undefined;
        else if (which === 'body') show = m !== P.cape;
        else if (which === 'cape') show = m === P.cape;
        else show = false;
        swap.push([m, m.material, m.renderOrder]);
        if (!show) { m.material = depthOnly; m.renderOrder = -1; } else if (m === P.cape) m.material = capeWhite;
      });
      return () => swap.forEach(([m, mat, ro]) => { m.material = mat; m.renderOrder = ro; });
    };
    const crop = (x, y, w, h) => {
      const c = document.createElement('canvas'); c.width = w; c.height = h;
      c.getContext('2d').drawImage(renderer.domElement, x, y, w, h, 0, 0, w, h);
      return c.toDataURL('image/png');
    };
    const trimmed = () => {
      const c = document.createElement('canvas'); c.width = W; c.height = H;
      const g = c.getContext('2d'); g.drawImage(renderer.domElement, 0, 0);
      const d = g.getImageData(0, 0, W, H).data;
      let x0 = W, y0 = H, x1 = -1, y1 = -1;
      for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) if (d[(y * W + x) * 4 + 3] > 2) {
        if (x < x0) x0 = x; if (x > x1) x1 = x; if (y < y0) y0 = y; if (y > y1) y1 = y;
      }
      if (x1 < 0) return null;
      x0 = Math.max(0, x0 - 1); y0 = Math.max(0, y0 - 1); x1 = Math.min(W - 1, x1 + 1); y1 = Math.min(H - 1, y1 + 1);
      return { url: crop(x0, y0, x1 - x0 + 1, y1 - y0 + 1), at: [x0, y0], w: x1 - x0 + 1, h: y1 - y0 + 1 };
    };
    // Alle Bilder (wie in render_sprites.js), mit Merker, ob der Move neu ist
    const frames = [];
    const legacy = {
      flex: [Math.PI / 6, -Math.PI / 6],
      wave: [0, 1, 2, 3].map(k => k * Math.PI / 2 / 7),
      jubel: [0, 1, 2, 3].map(k => k * Math.PI / 24),
    };
    for (const [pose, ts] of Object.entries(legacy)) ts.forEach((t, i) => frames.push([`${pose}_${i}`, false, () => { P.setPose(pose, t); P.waveCape(t * 2); return {}; }]));
    for (const [name, mv] of Object.entries(P.MOVES)) mv.f.forEach((_, i) => frames.push([`${name}_${i}`, !OLD_MOVES.includes(name), () => {
      const extra = P.setMove(name, i * mv.ms / 1000); P.waveCape(i * 0.8); return extra;
    }]));
    if (QUICK) {
      for (let i = frames.length - 1; i >= 0; i--) if (!QUICK.includes(frames[i][0].replace(/_\d+$/, ''))) frames.splice(i, 1);
      OLD_COSTUMES = ['gold']; NEW_COSTUMES = [];
    }
    const place = extra => {
      hero.rotation.y = -0.3 + (extra.turn || 0); hero.rotation.z = extra.sway || 0; hero.rotation.x = extra.lean || 0;
      P.headG.rotation.z = -(extra.sway || 0) * 0.6;
    };
    const at = {}, mouths = {};
    renderer.setSize(W, H, false);
    const cut = t => t.at[0] <= 0 || t.at[1] <= 0 || (t.at[0] + t.w) >= W || (t.at[1] + t.h) >= H;
    // Körper und Mund: neue Moves in allen Kostümen, neue Kostüme in allen Moves
    for (const costume of [...OLD_COSTUMES, ...NEW_COSTUMES]) {
      const fresh = NEW_COSTUMES.includes(costume);
      P.costume(costume);
      const restore = layer('body');
      for (const [name, isNew, setup] of frames) {
        if (!fresh && !isNew) continue;
        place(setup());
        P.setMouth(0.1, 0); renderer.render(scene, cam);
        let t = trimmed();
        if (costume === 'darth') for (const rx of [1.45, 1.8, 2.2, 2.6, 3.0]) {
          if (!cut(t)) break;
          P.saber.rotation.x = rx; renderer.render(scene, cam); t = trimmed();
        }
        res[`hero_${costume}_${name}`] = t.url; at[`hero_${costume}_${name}`] = t.at;
        hero.updateMatrixWorld(true);
        const m = P.headG.localToWorld(new T.Vector3(0, -0.23, 0.16)).project(cam);
        const cx = Math.round((m.x + 1) / 2 * W), cy = Math.round((1 - m.y) / 2 * H);
        mouths[name] = [cx - MW / 2, cy - MH / 2];
        P.setMouth(0.45, 0); renderer.render(scene, cam); res[`hero_${costume}_${name}_grin`] = crop(cx - MW / 2, cy - MH / 2, MW, MH);
        P.setMouth(1, 0); renderer.render(scene, cam); res[`hero_${costume}_${name}_laugh`] = crop(cx - MW / 2, cy - MH / 2, MW, MH);
        P.setMouth(0.1, 0);
        P.saber.rotation.x = 1.05;
      }
      restore();
    }
    P.costume('gold');
    // Umhang für die neuen Moves
    {
      const restore = layer('cape');
      for (const [name, isNew, setup] of frames) {
        if (!isNew) continue;
        place(setup()); renderer.render(scene, cam);
        const t = trimmed(); if (t) { res[`cape_${name}`] = t.url; at[`cape_${name}`] = t.at; }
      }
      restore();
    }
    if (QUICK && !QUICK.includes('acc')) { P.cape.material = capeMat; res.__layout = JSON.stringify({ mouth: mouths, at }); return res; }
    // Ausrüstung: jedes Teil allein, Körper und Umhang verdecken es über den Tiefenpuffer
    for (const [acc, g] of Object.entries(P.ACC)) {
      g.visible = true;
      const restore = layer('acc', g);
      for (const [name, , setup] of frames) {
        place(setup()); renderer.render(scene, cam);
        const t = trimmed(); if (t) { res[`acc_${acc}_${name}`] = t.url; at[`acc_${acc}_${name}`] = t.at; }
      }
      restore();
      g.visible = false;
    }
    P.cape.material = capeMat;
    P.setPose('flex', 0);
    hero.position.y = 0; hero.rotation.z = 0; hero.rotation.x = 0;
    res.__layout = JSON.stringify({ mouth: mouths, at });

    // Schlüpflinge und Riesen-Bosse
    hero.visible = false;
    const ortho = new T.OrthographicCamera(-0.6, 0.6, 0.6, -0.6, 0.1, 20);
    ortho.position.set(0, 0, 5); ortho.lookAt(0, 0, 0);
    for (const [kind, ci] of EGGS) {
      const m = P.monster(kind, PALETTE[ci], 'egg');
      m.g.position.set(0, 0, 0);
      m.arms[0].rotation.z = 0.3; m.arms[1].rotation.z = -0.3;
      res[`germ_${kind}_egg_c${ci}_f0`] = grab(ortho, 256, 256);
      scene.remove(m.g);
    }
    const big = new T.OrthographicCamera(-0.82, 0.82, 0.86, -0.78, 0.1, 20);
    big.position.set(0, 0, 5); big.lookAt(0, 0, 0);
    GIANTS.forEach(([kind, hex, extra], i) => {
      const m = P.giant(kind, hex, extra);
      m.g.position.set(0, 0, 0); m.g.rotation.y = 0.18;
      [0.35, -0.35].forEach((w, f) => {
        m.arms[0].rotation.z = w; m.arms[1].rotation.z = -w;
        res[`giant_${i}_f${f}`] = grab(big, 384, 384);
      });
      scene.remove(m.g);
    });
    return res;
  }, { OLD_MOVES, OLD_COSTUMES, NEW_COSTUMES, EGGS, GIANTS, PALETTE, QUICK });

  // Lage-Datei ergänzen: vorhandene Einträge bleiben, wie sie sind
  const add = JSON.parse(shots.__layout);
  delete shots.__layout;
  const layout = JSON.parse(fs.readFileSync(layoutFile, 'utf8'));
  for (const [k, v] of Object.entries(add.mouth)) if (!(k in layout.mouth)) layout.mouth[k] = v;
  for (const [k, v] of Object.entries(add.at)) if (!(k in layout.at)) layout.at[k] = v;
  if (!process.argv[3]) fs.writeFileSync(layoutFile, JSON.stringify(layout));
  else fs.writeFileSync(path.join(out, 'hero_layout.json'), JSON.stringify(layout));
  for (const [name, url] of Object.entries(shots)) fs.writeFileSync(path.join(out, name + '.png'), Buffer.from(url.split(',')[1], 'base64'));
  console.log(Object.keys(shots).length, 'Bilder in', out);
  await browser.close();
})();
