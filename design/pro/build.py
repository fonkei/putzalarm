# Baut design/super-buersti-pro.html aus template.html und Helm/Ausrüstung der alten Vorschau
import os
d=os.path.dirname(os.path.abspath(__file__));root=os.path.dirname(d)
src=open(os.path.join(root,'super-buersti-3d-modell.html'),encoding='utf8').read().split('\n')
helmet='\n'.join(src[383:415]);acc='\n'.join(src[419:506])
t=open(os.path.join(d,'template.html'),encoding='utf8').read()
t=t.replace("B[bn].add(ring);neon.add(ring.clone());ring.userData.neon=true;","B[bn].add(ring);ring.userData.neon=true;").replace("const neon=new THREE.Group();\n","")
t=t.replace('/*@@HELMET@@*/',helmet).replace('/*@@ACC@@*/',acc)
open(os.path.join(root,'super-buersti-pro.html'),'w',encoding='utf8').write(t)

# App-Version: gleiche Szene ohne Knöpfe, three.js liegt daneben (kein Netz nötig)
s=t.index('<script>\n(function(){');e=t.rindex('</script>')
core=t[s+len('<script>'):e]
app='''<!doctype html>
<html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<style>html,body{margin:0;height:100%;background:transparent;overflow:hidden}#stage{position:fixed!important;left:0;top:0;width:100vw!important;height:100vh!important}canvas{display:block;width:100%!important;height:100%!important}#loading{display:none}</style>
</head><body><div id="stage"><div id="loading"></div></div>
<script>window.PUTZ_APP=true;window.onerror=function(m,u,l,c){try{Android.log("JS-Fehler: "+m+" @"+l+":"+c);}catch(e){}};</script>
<script src="three.min.js"></script>
<script>'''+core+'''</script>
</body></html>
'''
out=os.path.join(os.path.dirname(root),'app','src','main','assets','buersti','index.html')
os.makedirs(os.path.dirname(out),exist_ok=True)
open(out,'w',encoding='utf8').write(app)
