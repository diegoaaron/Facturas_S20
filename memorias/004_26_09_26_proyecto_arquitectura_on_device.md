---
name: 004_26_09_26_proyecto_arquitectura_on_device
description: Decisión 2026-09-26/27 — app Android nativa en Java con Gemma 4 E2B embebido, un solo usuario, SIN servidor; toda la data en SQLite del teléfono y exportación a CSV/PDF
metadata:
  type: project
---

El 2026-09-26 el usuario decidió que el producto final sea **una app Android nativa (Java) con Gemma 4 E2B ajustado ejecutado 100 % en el teléfono** (formato `.litertlm` con LiteRT-LM), en lugar de la PWA React + FastAPI/túnel que había antes (borrada del repo el 2026-09-28; su último estado está en el tag `prototipo-pwa`). El nombre definitivo es **"Facturas S20"** (nunca "WASYK").

El 2026-09-27 el usuario decidió además que **no haya servidor**: la app es de **un solo usuario**, toda la data vive en el celular (**SQLite** con Room + SQLCipher, 13 tablas) y el usuario puede **exportar** sus datos (ZIP con CSV, imágenes y PDF, pantalla P20) para analizarlos en una PC. **No quiere ninguna aplicación de administración** que controle la app ni que reciba o pida datos de ella. Se retiraron el servidor de respaldo, su base Oracle/SQL Server, el actor Administrador y la consola web.

**Why:** el curso (UTP, Curso Integrador I) exige ≥ 50 % de Java en cada alternativa, y el usuario quiere que funcione sin conexión en móviles medios y que los datos no salgan del teléfono. Google recomienda ~6 GB de RAM para E2B en Android (modelo ≈ 2,6 GB); en gama básica no entra, por eso hay un modo de registro manual. La API de LiteRT-LM es Kotlin → solo un puente Kotlin, el resto Java (~85 %).

**How to apply:**
- No proponer servidores, cuentas, sincronización, respaldos en la nube ni consolas de administración. La única conexión de red es la descarga única del modelo desde un repositorio público (Hugging Face), según `assets/modelo.json`; los parámetros del NRUS van en `assets/parametros_nrus.json` y se actualizan con cada versión de la app (Google Play).
- Las alternativas 2 y 3 (descartadas) sí usan servidor por naturaleza; eso se deja así.
- Los entregables de `documentos_teoricos/` describen esta arquitectura (12 CU, 4 actores, 22 RF, 19 RNF, 20 pantallas móviles P01–P20). Para código, la referencia es `roadmap/001_27_09_26_documentacion_tecnica_y_roadmap.md` (movida allí el 2026-09-27 y fusionada con el roadmap el 2026-09-28). El entregable 1 ya entregado aún menciona el respaldo en servidor; en el APF2 se presenta como ajuste de alcance.
- La PWA fue solo un prototipo previo (competencia Build with Gemma); no retomarla. Los generadores del entregable 2 están en `documentos_teoricos/entregable2/entregable2_archivos/scripts/` (ver [[005_27_09_26_proyecto_entregable2_apf2]]).
