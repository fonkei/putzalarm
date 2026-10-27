// Rendert Super Bürsti und die Monster aus design/super-buersti-3d-modell.html als PNGs für die App.
// Aufruf: node tools/render_sprites.js <pfad/zu/three.min.js (three@0.149.0)>
// Ergebnis: app/src/main/res/drawable-nodpi/{hero_*,germ_*}.png
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

    // Super Bürsti (Goldglanz), leicht schräg von vorn
    P.costume('gold');
    hero.rotation.y = -0.3;
    const cam = new T.PerspectiveCamera(32, 3 / 4, 0.1, 100);
    cam.position.set(0, 2.05, 8.1); cam.lookAt(0, 1.98, 0);
    // Jedes Bild einmal mit Lächeln (ganzes Bild) und dazu nur der Mundausschnitt mit Grinsen und Lachen.
    // Die App legt den Ausschnitt an der gespeicherten Stelle über das Bild.
    const MW = 84, MH = 64, mouths = {};
    const crop = (cx, cy) => {
      const c = document.createElement('canvas'); c.width = MW; c.height = MH;
      c.getContext('2d').drawImage(renderer.domElement, cx - MW / 2, cy - MH / 2, MW, MH, 0, 0, MW, MH);
      return c.toDataURL('image/png');
    };
    const shoot = (name, extra) => {
      hero.rotation.y = -0.3 + (extra.turn || 0); hero.rotation.z = extra.sway || 0; hero.rotation.x = extra.lean || 0; P.headG.rotation.z = -(extra.sway || 0) * 0.6;
      P.setMouth(0.1, 0); res[name] = grab(cam, 450, 600);
      hero.updateMatrixWorld(true);
      const m = P.headG.localToWorld(new T.Vector3(0, -0.23, 0.16)).project(cam);
      const cx = Math.round((m.x + 1) / 2 * 450), cy = Math.round((1 - m.y) / 2 * 600);
      mouths[name] = [cx - MW / 2, cy - MH / 2];
      P.setMouth(0.45, 0); renderer.render(scene, cam); res[name + '_grin'] = crop(cx, cy);
      P.setMouth(1, 0); renderer.render(scene, cam); res[name + '_laugh'] = crop(cx, cy);
      P.setMouth(0.1, 0);
    };
    const frames = {
      flex: [Math.PI / 6, -Math.PI / 6],
      wave: [0, 1, 2, 3].map(k => k * Math.PI / 2 / 7),
      jubel: [0, 1, 2, 3].map(k => k * Math.PI / 24),
    };
    for (const [pose, ts] of Object.entries(frames)) ts.forEach((t, i) => {
      P.setPose(pose, t); P.waveCape(t * 2);
      shoot(`hero_${pose}_${i}`, {});
    });
    // Tanzen, Putzen, Boxen: Schlüsselbilder aus dem Modell (MOVES)
    for (const [name, mv] of Object.entries(P.MOVES)) mv.f.forEach((_, i) => {
      const extra = P.setMove(name, i * mv.ms / 1000);
      P.waveCape(i * 0.8);
      shoot(`hero_${name}_${i}`, extra);
    });
    P.setPose('flex', 0);
    hero.position.y = 0; hero.rotation.z = 0; hero.rotation.x = 0;
    res.__mouths = JSON.stringify({ w: MW, h: MH, at: mouths });

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

  fs.writeFileSync(path.join(root, 'app/src/main/res/raw/hero_mouths.json'), shots.__mouths);
  delete shots.__mouths;
  for (const [name, url] of Object.entries(shots)) fs.writeFileSync(path.join(out, name + '.png'), Buffer.from(url.split(',')[1], 'base64'));
  console.log(Object.keys(shots).length, 'Bilder in', out);
  await browser.close();
})();
