---
name: proyecto-entregable2-apf2
description: Estado del APF2 (entregable 2, diseño) al 2026-09-27 — entrega hasta el 29/09/2026 18:00, pendientes y cómo regenerar
metadata:
  type: project
---

El 2026-09-27 se generaron `entregable2_documento.docx` (103 págs., estructura oficial con cap. 3.1–3.7, cap. 4 cronograma y presupuesto, anexos A–T), `entregable2_presentacion.pptx` (15 diapositivas, ≈ 10 min, expositores repartidos) y `entregable2_documentacion_tecnica.md`. Según `entregable2_consideraciones.docx`, la entrega es del 27 al 29/09/2026 hasta las 18:00 (el docx dice 2025, es error de año) y el orden de exposición se sortea.

**Why:** el docente pidió corregir observaciones del APF1, prototipos + GUI, base de datos y mostrar ~20 % de back-end y 60–80 % de front-end en el siguiente avance.

**How to apply:**
- El usuario confirmó (2026-09-27) que no hay observaciones que corregir del APF1.
- El prototipo son mockups SVG propios; se entregará al docente la carpeta `entregable2_imagenes` como evidencia, tras la revisión del usuario.
- El usuario editó el .docx a mano (quitó la fila «Generadores» del Anexo Q; el script ya está alineado). Antes de regenerar el Word, confirmar con el usuario si hizo otros cambios manuales, porque se perderían.
- La prueba de usabilidad con 3 titulares está **planificada** (I3), no realizada; no presentar resultados inventados.
- Para regenerar: `python entregable2_imagenes/scripts/generar_todo.py` (ver [[usuario-dos-computadoras]] para sincronizar). No editar el .docx a mano: se sobrescribe.
