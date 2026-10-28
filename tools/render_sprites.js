// Rendert Super Bürsti und die Monster aus design/super-buersti-3d-modell.html als PNGs für die App.
// Aufruf: node tools/render_sprites.js <pfad/zu/three.min.js (three@0.149.0)>
// Ergebnis: app/src/main/res/drawable-nodpi/{hero_*,cape_*,germ_*}.png (danach tools/to_webp.py)
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const out = path.join(root, 'app/src/main/res/drawable-nodpi');
// Gleiche Reihenfolge wie Palette in data/Data.kt
const PALETTE = [0xff5d8f, 0x7bdc5a, 0xffb627, 0x5ac8fa, 0xb57bff, 0xff7a5c, 0x3ee0b0, 0xffd23f];
const KINDS = ['blob', 'spike', 'bean', 'cyclops'];

(async () => {
  fs.mkdirSync(out, { recursive: true });
  const browser = await chromium.launch({ args: ['--use-gl=swiftshader', '--enable-unsafe-swiftshader'] });
  const page = await browser.newPage({ viewport: { width: 1000, height: 800 } });
  page.on('pageerror', e => console.error(e.message));
  await page.route('**/three.min.js', r => r.fulfill({ body: fs.readFileSync(process.argv[2]), contentType: 'application/javascript' }));
  await page.route('**fonts.googleapis.com**', r => r.abort());
  await page.goto('file://' + path.join(root, 'design/super-buersti-3d-modell.html') + '?render');
  await page.waitForFunction(() => window.PUTZ);

  const shots = await page.evaluate(({ PALETTE, KINDS }) => {
    const P = window.PUTZ, T = THREE, res = {};
    const { renderer, scene, hero } = P;
    renderer.setPixelRatio(1);
    renderer.setClearColor(0x000000, 0);
    P.glowDisc.visible = false;
    P.ground.visible = false; // den Bodenschatten zeichnet die App selbst
    P.mons.forEach(m => { m.g.visible = false; });
    const grab = (cam, w, h) => { renderer.setSize(w, h, false); renderer.render(scene, cam); return renderer.domElement.toDataURL('image/png'); };

    // Super Bürsti, leicht schräg von vorn, in zwei Ebenen: Körper (je Kostüm) und Umhang (weiß, die App färbt ihn ein).
    // Jede Ebene verdeckt die andere über den Tiefenpuffer, so ergeben beide übereinander wieder das ganze Bild.
    P.setBody('hero'); // Heldenkörper (von Konni freigegeben)
    const cam = new T.PerspectiveCamera(32, 3 / 4, 0.1, 100);
    cam.position.set(0, 2.05, 8.1); cam.lookAt(0, 1.98, 0);
    const W = 450, H = 600, MW = 84, MH = 64;
    const depthOnly = new T.MeshBasicMaterial({ colorWrite: false });
    const capeWhite = P.cape.material.clone(); capeWhite.color.setHex(0xffffff);
    const capeMat = P.cape.material;
    const layer = which => {
      const swap = [];
      hero.traverse(m => {
        if (!m.isMesh) return;
        const hide = which === 'body' ? m === P.cape : m !== P.cape;
        swap.push([m, m.material, m.renderOrder]);
        if (hide) { m.material = depthOnly; m.renderOrder = -1; } else if (m === P.cape) m.material = capeWhite;
      });
      return () => swap.forEach(([m, mat, ro]) => { m.material = mat; m.renderOrder = ro; });
    };
    // Bild auf den sichtbaren Bereich zuschneiden, damit die App weniger Speicher braucht
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
      return { url: crop(x0, y0, x1 - x0 + 1, y1 - y0 + 1), at: [x0, y0] };
    };
    const frames = [];
    const legacy = {
      flex: [Math.PI / 6, -Math.PI / 6],
      wave: [0, 1, 2, 3].map(k => k * Math.PI / 2 / 7),
      jubel: [0, 1, 2, 3].map(k => k * Math.PI / 24),
    };
    for (const [pose, ts] of Object.entries(legacy)) ts.forEach((t, i) => frames.push([`${pose}_${i}`, () => { P.setPose(pose, t); P.waveCape(t * 2); return {}; }]));
    for (const [name, mv] of Object.entries(P.MOVES)) mv.f.forEach((_, i) => frames.push([`${name}_${i}`, () => {
      const extra = P.setMove(name, i * mv.ms / 1000); P.waveCape(i * 0.8); return extra;
    }]));
    const place = extra => {
      hero.rotation.y = -0.3 + (extra.turn || 0); hero.rotation.z = extra.sway || 0; hero.rotation.x = extra.lean || 0;
      P.headG.rotation.z = -(extra.sway || 0) * 0.6;
    };
    const at = {}, mouths = {};
    renderer.setSize(W, H, false);
    for (const costume of ['gold', 'chrom', 'neon']) {
      P.costume(costume);
      const restore = layer('body');
      for (const [name, setup] of frames) {
        place(setup());
        P.setMouth(0.1, 0); renderer.render(scene, cam);
        const t = trimmed(); res[`hero_${costume}_${name}`] = t.url; at[`hero_${costume}_${name}`] = t.at;
        hero.updateMatrixWorld(true);
        const m = P.headG.localToWorld(new T.Vector3(0, -0.23, 0.16)).project(cam);
        const cx = Math.round((m.x + 1) / 2 * W), cy = Math.round((1 - m.y) / 2 * H);
        mouths[name] = [cx - MW / 2, cy - MH / 2];
        P.setMouth(0.45, 0); renderer.render(scene, cam); res[`hero_${costume}_${name}_grin`] = crop(cx - MW / 2, cy - MH / 2, MW, MH);
        P.setMouth(1, 0); renderer.render(scene, cam); res[`hero_${costume}_${name}_laugh`] = crop(cx - MW / 2, cy - MH / 2, MW, MH);
        P.setMouth(0.1, 0);
      }
      restore();
    }
    P.costume('gold');
    const restore = layer('cape');
    for (const [name, setup] of frames) {
      place(setup()); renderer.render(scene, cam);
      const t = trimmed(); if (t) { res[`cape_${name}`] = t.url; at[`cape_${name}`] = t.at; }
    }
    restore();
    P.cape.material = capeMat;
    P.setPose('flex', 0);
    hero.position.y = 0; hero.rotation.z = 0; hero.rotation.x = 0;
    res.__layout = JSON.stringify({ w: W, h: H, mw: MW, mh: MH, mouth: mouths, at });

    // Monster: genau das 120er-Raster der alten Zeichnung (−0,6 … 0,6), zwei Bilder fürs Zappeln
    hero.visible = false;
    const ortho = new T.OrthographicCamera(-0.6, 0.6, 0.6, -0.6, 0.1, 20);
    ortho.position.set(0, 0, 5); ortho.lookAt(0, 0, 0);
    KINDS.forEach(kind => PALETTE.forEach((hex, ci) => {
      const m = P.monster(kind, hex);
      m.g.position.set(0, 0, 0);
      [0.3, -0.3].forEach((w, f) => {
        m.arms[0].rotation.z = w; m.arms[1].rotation.z = -w;
        res[`germ_${kind}_c${ci}_f${f}`] = grab(ortho, 256, 256);
      });
      scene.remove(m.g);
    }));
    return res;
  }, { PALETTE, KINDS });

  fs.writeFileSync(path.join(root, 'app/src/main/res/raw/hero_layout.json'), shots.__layout);
  delete shots.__layout;
  for (const f of fs.readdirSync(out)) if (/^(hero|cape|germ)_/.test(f)) fs.unlinkSync(path.join(out, f));
  for (const [name, url] of Object.entries(shots)) fs.writeFileSync(path.join(out, name + '.png'), Buffer.from(url.split(',')[1], 'base64'));
  console.log(Object.keys(shots).length, 'Bilder in', out);
  await browser.close();
})();
