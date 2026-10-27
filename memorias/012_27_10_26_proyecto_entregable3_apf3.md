---
name: 012_27_10_26_proyecto_entregable3_apf3
description: Qué pidió el docente para el APF3 (exposición del 2026-10-27) y dónde están el Word corregido, la PPT, las figuras y la copia de la BD para DBeaver
metadata:
  type: project
---

Para la exposición del APF3 (2026-10-27) el docente pidió una PPT con: Project Charter básico, Gantt, diagramas de procesos que cubran el alcance funcional y no funcional, diagrama físico y lógico de la BD y capturas de la app; además, **revisar y manipular la BD de la app en vivo** (se usa DBeaver). Todo lo del APF3 va en `documentos_teoricos/entregable3/` y no toca el entregable 2.

- `entregable3_apf2_corregido.docx`: informe del APF2 corregido (ver [[006_14_10_26_feedback_observaciones_docente_apf2]]).
- `entregable3_presentacion.pptx` (14 diapositivas): `scripts/construir_presentacion.py`, que reutiliza las utilidades de la PPT del entregable 2. Las figuras nuevas (`gantt_semana12`, `bd_logico`, `bd_fisico_registro`, `bd_fisico_nrus`) las genera `scripts/diagramas_e3.py`; los diagramas de la BD salen del esquema de Room (`android/app/schemas/.../1.json`), que difiere del DER del APF2 (p. ej. `titular`, `pin_sal`, `numero_normalizado`, montos en céntimos INTEGER). Los BPMN se toman del entregable 2 recortando el título interno.
- BD para DBeaver: `scripts/copiar_base.ps1` + `CopiaLegibleBaseTest` dejan `entregable3_archivos/base_datos/facturas_legible.db` (sin cifrar) y `facturas_cifrada.db`; la carpeta está fuera de git por tener datos reales.
- `req_profesor/`: lo que pide el avance 3 según `entregable2/observaciones_profesor_entregable/apuntes_correciones_integrador.txt` (informe, BD, anexos, proyecto, GitHub, diccionario, manual, fuentes, prototipos), armado por `scripts/generar_req_profesor.py` (HTML/SVG → PNG/PDF con Chrome; Word → PDF por COM; descarga los PDF de investigación de acceso abierto). El FODA no existía antes: se escribió para esta carpeta. Los prototipos son las pantallas SVG del APF2, no de Figma. `proyecto/` (APK, ZIP) y `fuentes/` (PDF) están fuera de git: en la otra computadora, regenerarlos con el script (antes `./gradlew :app:assembleDebug`).

**Why:** son los puntos con los que se evalúa este avance; la base real está cifrada con SQLCipher y su clave en el Keystore, así que copiar el archivo no basta.
**How to apply:** para cambiar la PPT, editar los scripts y regenerar; para tener la BD en la otra computadora, correr `copiar_base.ps1` con el teléfono conectado. Ver [[011_27_10_26_feedback_pruebas_telefono_sin_desinstalar]].
