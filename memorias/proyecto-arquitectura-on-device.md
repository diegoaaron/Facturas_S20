---
name: proyecto-arquitectura-on-device
description: Decisión 2026-09-26 — el proyecto pasa a app Android nativa en Java con Gemma 4 E2B embebido (LiteRT-LM); documentos teóricos ya actualizados
metadata:
  type: project
---

El 2026-09-26 el usuario decidió que el producto final sea **una app Android nativa (Java) con Gemma 4 E2B ajustado ejecutado 100 % en el teléfono** (formato `.litertlm` con LiteRT-LM), en lugar de la PWA React + FastAPI/túnel que hay hoy en el repo. El nombre definitivo es **"Facturas S20"** (nunca "WASYK").

**Why:** el curso (UTP, Curso Integrador I) exige ≥ 50 % de Java en cada alternativa, y el usuario quiere que funcione sin conexión en móviles medios. Google recomienda ~6 GB de RAM para E2B en Android (modelo ≈ 2,6 GB, < 1,5 GB de RAM en ejecución); en gama básica no entra, por eso se definió un modo de registro manual de respaldo. La API de LiteRT-LM es Kotlin → solo un puente Kotlin, el resto Java (~85 %).

**How to apply:** los entregables 1 y 2 de `documentos_teoricos/` ya describen esta arquitectura (12 CU, 22 RF, 19 RNF, 19 pantallas P01–P19 + consola P20). Cualquier documento o código nuevo debe ser coherente con ellos; para código, la referencia es `entregable2_documentacion_tecnica.md`. Decisión del APF2 (2026-09-27): el servidor **no replica facturas**, solo guarda respaldos cifrados de extremo a extremo (clave de una frase de respaldo, no del PIN); base local de 13 tablas y del servidor de 9. La PWA actual y [[proyecto-tunel-gemma]] quedan como prototipo previo (competencia Build with Gemma). Los generadores del entregable 1 no se guardaron; los del entregable 2 sí están en `entregable2_imagenes/scripts/` (ver [[proyecto-entregable2-apf2]]).
