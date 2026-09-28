# Facturas S20 — Roadmap: cronograma, responsables y pendientes

Resumen del plan del proyecto (ciclo 2026-03, 18 semanas), reunido desde los generadores del informe y la sustentación del APF2 (`documentos_teoricos/entregable2/entregable2_archivos/scripts/`: `contenido_documento.py` §3.1.2, §3.7.6, cap. 4 y conclusiones; `construir_presentacion.py`; `diagramas_uml.py` → `gantt()`).

- Las **tareas técnicas detalladas** de cada iteración están en `entregable2_documentacion_tecnica.md` §13 (misma carpeta).
- Este archivo es la referencia de trabajo. Si cambias el plan y quieres que el informe lo refleje, actualiza también los scripts de arriba (el Word y el PPT se regeneran desde allí).

Estado al **corte de la semana 8 (APF2, 27–29/09/2026)**.

---

## 1. Entregas del curso

| Semana | Entrega |
|---|---|
| 4 | APF1 — entregado |
| 8 | APF2 — entrega hasta el 29/09/2026 18:00 |
| 12 | APF3 |
| 18 | Informe final y sustentación |

Pedido del docente para el siguiente avance: mostrar **~20 % del back-end y 60–80 % del front-end** (interfaces hechas a partir del prototipo y de la base de datos). Sin servidor, el "back-end" son las capas Java del teléfono: dominio, motor NRUS, Room/SQLite y exportación.

## 2. Iteraciones

Son iteraciones de dos semanas, alineadas con las entregas del curso.

| Iteración | Semanas | Objetivo | Entregable | Estado |
|---|---|---|---|---|
| I1 | 1–4 | Análisis del contexto, alternativas y SRS | APF1 | ✅ completo |
| I2 | 5–8 | Diseño de procesos, datos, clases, prototipo y documentación técnica | APF2 | ✅ completo |
| I3 | 9–10 | Front-end navegable (P01–P20) y dominio con motor de reglas probado | Incremento 1 | ⏳ siguiente |
| I4 | 11–12 | Persistencia cifrada (Room + SQLCipher) e inferencia local con el modelo convertido | APF3 | pendiente |
| I5 | 13–14 | Reporte PDF, exportación de datos e historial | Incremento 3 | pendiente |
| I6 | 15–18 | Pruebas, validación en mes simulado y sustentación | Informe final | pendiente |

## 3. Actividades de las semanas 9 a 18 y responsables

| Semanas | Actividad | Responsable | Entregable |
|---|---|---|---|
| 9–10 | Front-end navegable (P01–P20) y dominio con pruebas unitarias del motor NRUS | Coronel Obregón | Incremento 1 |
| 9–12 | Ajuste fino, evaluación, conversión a `.litertlm` y pruebas en el teléfono | Damián Valdivia | Modelo v1 convertido |
| 11–12 | Room con SQLCipher, repositorios e integración del puente LiteRT-LM | Coronel Obregón | APF3 |
| 11–12 | Prueba de usabilidad del prototipo con tres titulares | Gonzales Maco | Informe de usabilidad |
| 13–14 | Reporte PDF, exportación de datos e historial | Coronel Obregón, Mateo Velásquez | Incremento 3 |
| 12–16 | Pruebas de integración y validación en mes simulado | Gonzales Maco | Resultados de validación |
| 17–18 | Informe final y sustentación | Mateo Velásquez | Informe final |

## 4. Estado del Gantt al corte de la semana 8

| Paquete | Semanas | Estado |
|---|---|---|
| 1. Gestión del proyecto (acta, WBS, cronograma y riesgos: S1–3; seguimiento: S4–18) | 1–18 | en curso |
| 2. Análisis (contexto, canvas, entrevistas, SRS) | 1–4 | completo |
| 3. Diseño (BPMN, clases, DER, prototipo UX/UI, documentación técnica) | 3–8 | completo |
| 4. Modelo Gemma 4 ajustado — recopilación y etiquetado | 5–9 | en curso |
| 4. Modelo Gemma 4 ajustado — ajuste fino, conversión y pruebas | 9–12 | pendiente |
| 5. Construcción de la app — front-end (pantallas y navegación) | 7–12 | en curso |
| 5. Construcción de la app — back-end (dominio, Room y exportación) | 8–14 | en curso |
| 6. Pruebas y validación | 12–16 | pendiente |
| 7. Documentación y sustentación | 4–18 | en curso |

No hay desviaciones respecto de la línea base (Anexo D). La construcción de la app se adelantó una semana para empezar el front-end mientras se cerraba el diseño.

## 5. Mediciones comprometidas (qué hay que demostrar y cuándo)

| RNF | Medición prevista | Cuándo |
|---|---|---|
| RNF-01, RNF-02 | Exactitud ≥ 90 % por campo (conjunto de prueba separado, comparado con el modelo base) | APF3 |
| RNF-03 | Diferencia < 2 % en el mes simulado | Final |
| RNF-04, RNF-05 | ≤ 20 s y ≤ 2 GB en el equipo de referencia (imagen a 896 px, GPU si existe) | APF3 |
| RNF-06, RNF-07 | APK ≤ 60 MB (modelo como descarga separada); pruebas en Android 8 y 13 (minSdk 26) | APF3 |
| RNF-09 a RNF-11 | Prueba de usabilidad con 3 titulares (Anexo S) | Semanas 11–12, I4 (resultados en APF3) |
| RNF-12, RNF-14 | Prueba en modo avión e inspección de tráfico | APF3 |
| RNF-13 | Prueba de cierre forzado durante el guardado | APF3 |
| RNF-15, RNF-16 | Revisión con OWASP MASVS e inspección de tráfico | Final |
| RNF-17 a RNF-19 | Cobertura de pruebas del motor ≥ 80 % | APF3 |

> **Decidido (2026-09-27):** la prueba de usabilidad es en las **semanas 11–12 (I4)**. El informe del APF2 también la ubica en I3 (§3.7.6 y conclusión cuarta); esa mención se corrige en el informe del APF3.

## 6. Próximos pasos hacia el APF3 (presentados en la sustentación)

1. Construir el front-end y el dominio a partir de la documentación técnica.
2. Hacer el ajuste fino y la conversión a `.litertlm`.
3. Medir la exactitud y el tiempo de lectura en el teléfono de referencia.
4. Hacer la prueba de usabilidad con tres titulares. Los resultados se reportan en el APF3 y **no se inventan cifras**.
