---
name: proyecto-index-html-build
description: El index.html de la raíz es un build viejo que carga assets/index-D-068JsU.js en vez de src/main.tsx; pendiente de corregir
metadata:
  type: project
---

Detectado el 2026-09-26: el `index.html` de la raíz carga el bundle compilado `/assets/index-D-068JsU.js` (versión antigua con `getUserMedia`) en lugar de `/src/main.tsx`. Resultado: los cambios en `src/` (p. ej. el commit 8477eca de la cámara) no aparecen ni en `npm run dev` ni en el build de Vercel. La versión correcta está en `sergio/index.html`.

**Why:** el usuario aún no ha decidido corregirlo; puede afectar al despliegue actual en Vercel.
**How to apply:** si un cambio en `src/` "no se ve", la causa es esto. No corregir sin confirmar con el usuario; al resolverlo, actualizar o borrar esta memoria.
