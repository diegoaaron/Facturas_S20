---
name: 002_26_09_26_proyecto_tunel_gemma
description: La URL del túnel Cloudflare del backend Gemma cambia en cada reinicio del Colab y está hardcodeada en vercel.json
metadata:
  type: project
---

El backend de inferencia (FastAPI en Colab, notebook `tunning_gemma4/tunning_model_gemma4_6.ipynb`) se expone con `cloudflared tunnel --url`, que genera una URL `*.trycloudflare.com` nueva en cada arranque. Esa URL está escrita a mano en `vercel.json` (producción) y en `GEMMA_TUNNEL_ORIGIN` del `.env` (local, no versionado — hay que configurarlo en cada computadora).

**Why:** no existe backend persistente; si la app en Vercel falla con error de red, lo más probable es que el túnel haya muerto o cambiado.
**How to apply:** ante errores `network`/`server` del escaneo, revisar primero la URL del túnel. Relacionado: [[001_26_09_26_usuario_dos_computadoras]].
