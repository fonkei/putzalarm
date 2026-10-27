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
    const frames = {
      flex: [Math.PI / 6, -Math.PI / 6],
      wave: [0, 1, 2, 3].map(k => k * Math.PI / 2 / 7),
      jubel: [0, 1, 2, 3].map(k => k * Math.PI / 24),
    };
    for (const [pose, ts] of Object.entries(frames)) ts.forEach((t, i) => {
      P.setPose(pose, t); P.waveCape(t * 2); P.headG.rotation.z = 0;
      res[`hero_${pose}_${i}`] = grab(cam, 450, 600);
    });

    // Weitere Bewegungen, Bild für Bild direkt gestellt. Pro Arm: sy = nach vorn schwenken,
    // uz = Oberarm heben, ez = Unterarm nach innen beugen (alles gespiegelt für links/rechts).
    const setArm = (a, { sy = 0, uz = 0, ez = 0 }) => {
      a.shoulder.rotation.y = -a.side * sy; a.upper.rotation.z = a.side * uz; a.elbow.rotation.z = a.side * ez;
    };
    // Kleine Zahnbürste für die Putz-Bewegung, steckt in der rechten Faust
    const mini = new T.Group();
    const mh = new T.Mesh(new T.CapsuleGeometry(0.03, 0.42, 6, 12), P.mat.boot); mh.position.y = 0.2; mini.add(mh);
    const mhead = new T.Mesh(new T.CapsuleGeometry(0.045, 0.08, 6, 12), P.mat.head); mhead.position.set(0, 0.46, 0.02); mini.add(mhead);
    for (let k = 0; k < 3; k++) { const br = new T.Mesh(new T.CapsuleGeometry(0.018, 0.05, 4, 8), P.mat.bristleA); br.rotation.x = Math.PI / 2; br.position.set(0, 0.43 + k * 0.035, 0.08); mini.add(br); }
    mini.traverse(o => { if (o.isMesh) o.castShadow = true; });
        const R = P.arms.find(a => a.side > 0), L = P.arms.find(a => a.side < 0);
    const moves = {
      // Tanzen: ein Arm hoch, einer runter, Hüftschwung und kleine Hüpfer
      dance: [
        { L: { uz: -0.9, ez: -1.2 }, R: { uz: 1.1, ez: -0.4 }, sway: 0.1, hop: 0.06, turn: 0.15 },
        { L: { uz: -0.4, ez: -0.6 }, R: { uz: 0.4, ez: 0.6 }, sway: 0, hop: 0.14, turn: 0 },
        { L: { uz: 1.1, ez: -0.4 }, R: { uz: -0.9, ez: -1.2 }, sway: -0.1, hop: 0.06, turn: -0.15 },
        { L: { uz: 0.4, ez: 0.6 }, R: { uz: -0.4, ez: -0.6 }, sway: 0, hop: 0.14, turn: 0 },
      ],
      // Putzen: rechte Faust mit Mini-Bürste vor dem Mund, hin und her
      brush: [-0.14, 0, 0.14, 0].map(d => ({ L: { uz: -1.3, ez: -1.45 }, R: { sy: 1.35 + d, uz: 0.55, ez: -0.7 }, brush: true, turn: 0.1 })),
      // Boxen: Deckung, rechts schlagen, Deckung, links schlagen
      box: [
        { L: { sy: 1.1, uz: -0.35, ez: 0.35 }, R: { sy: 1.1, uz: -0.35, ez: 0.35 }, turn: 0 },
        { L: { sy: 1.1, uz: -0.35, ez: 0.35 }, R: { sy: 1.5, uz: 0.15, ez: -1.45 }, turn: 0.25 },
        { L: { sy: 1.1, uz: -0.35, ez: 0.35 }, R: { sy: 1.1, uz: -0.35, ez: 0.35 }, turn: 0, hop: 0.05 },
        { L: { sy: 1.5, uz: 0.15, ez: -1.45 }, R: { sy: 1.1, uz: -0.35, ez: 0.35 }, turn: -0.25 },
      ],
    };
    for (const [name, list] of Object.entries(moves)) list.forEach((f, i) => {
      setArm(L, f.L); setArm(R, f.R);
      hero.position.y = f.hop || 0; hero.rotation.z = f.sway || 0; hero.rotation.y = -0.3 + (f.turn || 0);
      hero.remove(mini);
      if (f.brush) {
        // Bürstenkopf am Mund, Griff durch die Faust
        hero.updateMatrixWorld(true);
        const fist = hero.worldToLocal(R.elbow.localToWorld(new T.Vector3(0, 0.47, 0)));
        const dir = new T.Vector3(0.02, 3.0, 0.24).sub(fist).normalize();
        mini.position.copy(fist).addScaledVector(dir, -0.12);
        mini.quaternion.setFromUnitVectors(new T.Vector3(0, 1, 0), dir);
        // Um den Griff drehen, bis die Borsten (lokal +z) nach unten zeigen
        const want = new T.Vector3(0, -1, 0).addScaledVector(dir, dir.y).normalize();
        const cur = new T.Vector3(0, 0, 1).applyQuaternion(mini.quaternion);
        mini.rotateY(Math.atan2(new T.Vector3().crossVectors(cur, want).dot(dir), cur.dot(want)));
        hero.add(mini);
      }
      P.waveCape(i * 0.8); P.headG.rotation.z = -(f.sway || 0) * 0.6;
      res[`hero_${name}_${i}`] = grab(cam, 450, 600);
    });
    hero.remove(mini);
    hero.position.y = 0; hero.rotation.z = 0;

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

  for (const [name, url] of Object.entries(shots)) fs.writeFileSync(path.join(out, name + '.png'), Buffer.from(url.split(',')[1], 'base64'));
  console.log(Object.keys(shots).length, 'Bilder in', out);
  await browser.close();
})();
