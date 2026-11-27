# Baut design/super-buersti-pro.html aus template.html und Helm/Ausrüstung der alten Vorschau
import os
d=os.path.dirname(os.path.abspath(__file__));root=os.path.dirname(d)
src=open(os.path.join(root,'super-buersti-3d-modell.html'),encoding='utf8').read().split('\n')
helmet='\n'.join(src[383:415]);acc='\n'.join(src[419:506])
t=open(os.path.join(d,'template.html'),encoding='utf8').read()
t=t.replace("B[bn].add(ring);neon.add(ring.clone());ring.userData.neon=true;","B[bn].add(ring);ring.userData.neon=true;").replace("const neon=new THREE.Group();\n","")
t=t.replace('/*@@HELMET@@*/',helmet).replace('/*@@ACC@@*/',acc)
open(os.path.join(root,'super-buersti-pro.html'),'w',encoding='utf8').write(t)
