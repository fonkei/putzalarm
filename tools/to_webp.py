"""Wandelt die gerenderten Sprites (hero_*, cape_*, germ_*) in verlustfreie WebP-Bilder um (kleinere App)."""
import pathlib
from PIL import Image

out = pathlib.Path(__file__).resolve().parent.parent / 'app/src/main/res/drawable-nodpi'
for p in sorted(out.glob('*.png')):
    if p.name.split('_')[0] in ('hero', 'cape', 'germ'):
        Image.open(p).save(p.with_suffix('.webp'), 'WEBP', lossless=True, method=6)
        p.unlink()
print('fertig')
