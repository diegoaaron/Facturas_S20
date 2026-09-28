---
name: proyecto-entregable2-apf2
description: Estado del APF2 (entregable 2, diseño) al 2026-09-27 — entrega hasta el 29/09/2026 18:00, versión completa y "solo APF2", pendientes y cómo regenerar
metadata:
  type: project
---

El 2026-09-27 se generaron, en `documentos_teoricos/entregable2/`:
- `entregable2_documento.docx`: informe completo y acumulativo (APF1 + APF2), 106 págs., estructura oficial con cap. 3.1–3.7, cap. 4 y anexos A–T.
- `solo_entregable2_documento.docx`: 85 págs., solo los apartados del APF2 más lo mínimo del APF1. Conserva la numeración y las letras oficiales (con saltos: 3.2 → 3.4, anexos A, B, D, F, H, I, K–T).
- `entregable2_presentacion.pptx`: 15 diapositivas, ≈ 10 min.
- `entregable2_documentacion_tecnica.md`.

Ese mismo día se quitó el servidor de todo el diseño (ver [[proyecto-arquitectura-on-device]]). Según `entregable2_consideraciones.docx`, la entrega es del 27 al 29/09/2026 hasta las 18:00 (el docx dice 2025, es error de año) y el orden de exposición se sortea.

**Why:** el docente pidió corregir observaciones del APF1, prototipos + GUI, base de datos y mostrar ~20 % de back-end y 60–80 % de front-end en el siguiente avance. Sin servidor, el back-end son las capas Java del teléfono (dominio, motor NRUS, Room/SQLite, exportación).

**How to apply:**
- El usuario confirmó (2026-09-27) que no hay observaciones que corregir del APF1 y que no hizo otros cambios manuales al Word.
- El prototipo son mockups SVG propios; se entregará al docente la carpeta `entregable2/entregable2_archivos` como evidencia, tras la revisión del usuario.
- La prueba de usabilidad con 3 titulares está **planificada** (I3), no realizada; no presentar resultados inventados.
- Para regenerar todo: `python documentos_teoricos/entregable2/entregable2_archivos/scripts/generar_todo.py` (Python con lxml, Pillow y python-pptx; Chrome; Word). No editar los .docx a mano: se sobrescriben. Ver [[usuario-dos-computadoras]] para sincronizar.
