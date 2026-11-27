// Rendert die Ersatzbilder von Super Bürsti (hero_*, cape_*, acc_*) aus dem neuen 3D-Modell design/super-buersti-pro.html.
// Die App zeigt sie, solange die 3D-Szene lädt, in ruhigen Vorschauen (Schrank-Kacheln) und auf Geräten ohne 3D.
// Aufruf: node tools/render_pro_sprites.js <pfad/zu/three.min.js (three@0.149.0)>   danach: python3 tools/to_webp.py
// Monster (germ_*) und Riesen (giant_*) bleiben unverändert.
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const out = path.join(root, 'app/src/main/res/drawable-nodpi');
// Gleiche Bilder je Bewegung wie HeroPose in ui/art/Sprites.kt
const POSES = { flex: 2, wave: 4, jubel: 4, dance: 4, brush: 4, box: 4, moonwalk: 4, karate: 4, jack: 4, spin: 4, robot: 4, guitar: 4, fly: 4, floss: 4, dab: 4, chicken: 4, hula: 4, stomp: 4, run: 4 };
const COSTUMES = ['gold', 'chrom', 'neon', 'darth', 'feuer', 'eis'];

(async () => {
  const browser = await chromium.launch({ args: ['--use-gl=swiftshader', '--enable-webgl', '--ignore-gpu-blocklist', '--enable-unsafe-swiftshader'] });
  const page = await browser.newPage({ viewport: { width: 450, height: 600 } });
  page.on('pageerror', e => console.error(e.message));
  await page.route('**/three.min.js', r => r.fulfill({ body: fs.readFileSync(process.argv[2]), contentType: 'application/javascript' }));
  await page.route('**fonts.googleapis.com**', r => r.abort());
  await page.route('**fonts.gstatic.com**', r => r.abort());
  await page.goto('file://' + path.join(root, 'design/super-buersti-pro.html') + '?test');
  await page.waitForFunction(() => window.PUTZ && window.PUTZ.R);

  const shots = await page.evaluate(({ POSES, COSTUMES }) => {
    const P = window.PUTZ, R = P.R, T = THREE, res = {};
    const { renderer, scene } = P;
    const W = 450, H = 600, MW = 84, MH = 64;
    renderer.setPixelRatio(1);
    renderer.setSize(W, H, false);
    renderer.setClearColor(0x000000, 0);
    renderer.shadowMap.enabled = false;
    scene.background = null;
    R.ground.visible = false; R.glowDisc.visible = false; R.blob.visible = false; // den Bodenschatten zeichnet die App selbst
    // gleiche Kamera wie die 3D-Szene in der App
    const cam = new T.PerspectiveCamera(32, W / H, 0.1, 100);
    cam.position.set(0, 1.95, 8.9); cam.lookAt(0, 1.95, 0);
    R.noBlink();
    R.setFace('freundlich');
    // Moonwalk auf der Stelle, sonst läuft er aus dem Bild
    delete R.CLIPS.moonwalk.circle;

    const depthOnly = new T.MeshBasicMaterial({ colorWrite: false });
    const capeWhite = R.capeMesh.material.clone(); capeWhite.color.setHex(0xffffff);
    if (capeWhite.sheenColor) capeWhite.sheenColor.setHex(0xffffff);
    const accMeshes = new Set();
    Object.values(R.ACC).forEach(g => g.traverse(m => { if (m.isMesh) accMeshes.add(m); }));
    // Ebene rendern: 'body' (ohne Umhang und Ausrüstung), 'cape' (weiß) oder ein Ausrüstungsteil.
    // Alles andere schreibt nur in den Tiefenpuffer, damit sich die Ebenen richtig verdecken.
    const layer = (which, acc) => {
      const swap = [];
      scene.traverse(m => {
        if (!m.isMesh || !m.visible) return;
        if (m === R.ground || m === R.glowDisc || m === R.blob) return;
        let show;
        if (which === 'body') show = m !== R.capeMesh && !accMeshes.has(m);
        else if (which === 'cape') show = m === R.capeMesh;
        else show = acc.has(m);
        swap.push([m, m.material, m.renderOrder]);
        if (!show) { m.material = depthOnly; m.renderOrder = -1; }
        else if (m === R.capeMesh) m.material = capeWhite;
      });
      renderer.render(scene, cam);
      swap.forEach(([m, mat, ro]) => { m.material = mat; m.renderOrder = ro; });
    };
    const cv = document.createElement('canvas'); cv.width = W; cv.height = H;
    const g2 = cv.getContext('2d', { willReadFrequently: true });
    const crop = (x, y, w, h) => {
      const c = document.createElement('canvas'); c.width = w; c.height = h;
      c.getContext('2d').drawImage(renderer.domElement, x, y, w, h, 0, 0, w, h);
      return c.toDataURL('image/png');
    };
    const trimmed = () => {
      g2.clearRect(0, 0, W, H); g2.drawImage(renderer.domElement, 0, 0);
      const d = g2.getImageData(0, 0, W, H).data;
      let x0 = W, y0 = H, x1 = -1, y1 = -1;
      for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) if (d[(y * W + x) * 4 + 3] > 2) {
        if (x < x0) x0 = x; if (x > x1) x1 = x; if (y < y0) y0 = y; if (y > y1) y1 = y;
      }
      if (x1 < 0) return null;
      x0 = Math.max(0, x0 - 1); y0 = Math.max(0, y0 - 1); x1 = Math.min(W - 1, x1 + 1); y1 = Math.min(H - 1, y1 + 1);
      return { url: crop(x0, y0, x1 - x0 + 1, y1 - y0 + 1), at: [x0, y0] };
    };
    const at = {}, mouths = {};
    const MOUTH = { rest: [0.08, 1, 1, 0], grin: [0.32, 1.12, 1, 1], laugh: [0.78, 1.18, 1, 1] };
    const run = (dt, n) => { for (let i = 0; i < n; i++) P.step(dt); };

    for (const [pose, nf] of Object.entries(POSES)) {
      const clip = R.CLIPS[pose];
      P.costume('gold');
      P.setClip(pose);
      run(1 / 60, 150); // einschwingen (Federn, Umhang)
      const stepT = clip.T / nf;
      for (let i = 0; i < nf; i++) {
        if (i > 0) run(1 / 120, Math.max(1, Math.round(stepT * 120)));
        R.noBlink();
        const name = `${pose}_${i}`;
        // Mund-Ausschnitt: Stelle des Mundes im Bild
        const mp = R.mouthG.getWorldPosition(new T.Vector3()).project(cam);
        const cx = Math.round((mp.x + 1) / 2 * W), cy = Math.round((1 - mp.y) / 2 * H);
        mouths[name] = [cx - MW / 2, cy - MH / 2];
        for (const c of COSTUMES) {
          P.costume(c);
          R.mini.visible = !!clip.brush && c !== 'darth';
          R.setMouth(...MOUTH.rest); layer('body');
          const t = trimmed(); res[`hero_${c}_${name}`] = t.url; at[`hero_${c}_${name}`] = t.at;
          R.setMouth(...MOUTH.grin); layer('body'); res[`hero_${c}_${name}_grin`] = crop(cx - MW / 2, cy - MH / 2, MW, MH);
          R.setMouth(...MOUTH.laugh); layer('body'); res[`hero_${c}_${name}_laugh`] = crop(cx - MW / 2, cy - MH / 2, MW, MH);
          R.setMouth(...MOUTH.rest);
        }
        P.costume('gold');
        R.mini.visible = !!clip.brush;
        layer('cape');
        let t = trimmed(); if (t) { res[`cape_${name}`] = t.url; at[`cape_${name}`] = t.at; }
        for (const [k, grp] of Object.entries(R.ACC)) {
          const ms = new Set(); grp.traverse(m => { if (m.isMesh) ms.add(m); });
          const was = grp.visible; grp.visible = true;
          layer('acc', ms);
          grp.visible = was;
          t = trimmed(); if (t) { res[`acc_${k}_${name}`] = t.url; at[`acc_${k}_${name}`] = t.at; }
        }
      }
    }
    res.__layout = JSON.stringify({ w: W, h: H, mw: MW, mh: MH, mouth: mouths, at });
    return res;
  }, { POSES, COSTUMES });

  fs.writeFileSync(path.join(root, 'app/src/main/res/raw/hero_layout.json'), shots.__layout);
  delete shots.__layout;
  for (const f of fs.readdirSync(out)) if (/^(hero|cape|acc)_/.test(f)) fs.unlinkSync(path.join(out, f));
  for (const [name, url] of Object.entries(shots)) fs.writeFileSync(path.join(out, name + '.png'), Buffer.from(url.split(',')[1], 'base64'));
  console.log(Object.keys(shots).length, 'Bilder in', out);
  await browser.close();
})();
