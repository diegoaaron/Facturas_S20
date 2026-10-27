# Índice de memorias del proyecto

Una línea por memoria: `- [Título](archivo.md) — gancho`. El contenido va en cada archivo, no aquí.

- [Trabajo en 2 computadoras](001_26_09_26_usuario_dos_computadoras.md) — la memoria vive en el repo, hacer pull/push para sincronizar
- [Arquitectura final: Android Java + Gemma embebido](004_26_09_26_proyecto_arquitectura_on_device.md) — decisión 2026-09-26/27; un solo usuario, SIN servidor ni administración; SQLite en el teléfono y exportación a CSV/PDF
- [Entregable 2 (APF2) generado](005_27_09_26_proyecto_entregable2_apf2.md) — entrega hasta 29/09/2026 18:00; versión completa y "solo APF2"; cómo regenerar; prueba de usabilidad retirada (2026-10-27)
- [Observaciones docentes al APF2](006_14_10_26_feedback_observaciones_docente_apf2.md) — citas APA, tablas/figuras APA, sin formulación, objetivo = título, OE con indicador; corregido en scripts el 2026-10-24; copia corregida en entregable3 sin prueba de usabilidad (2026-10-27)
- [Etiquetado del dataset de ajuste fino](007_25_10_26_proyecto_etiquetado_dataset.md) — total con percepción, razón social impresa; `tunning_gemma4/datos/` fuera de git
- [Entrenamiento local en WSL con 6 GB](008_25_10_26_proyecto_entrenamiento_local_wsl.md) — cabe dejando embed_tokens_per_layer en la CPU; venv ~/venvs/gemma
- [El usuario decide todo](009_26_10_26_feedback_usuario_decide_todo.md) — no proponer consultar ni avisar decisiones al resto del equipo
- [Captura manual, sin escáner de Google](010_27_10_26_feedback_captura_manual_sin_escaner_google.md) — P06 con disparo manual y linterna propia; recorte automático en la app; ML Kit document scanner descartado
- [Pruebas en el teléfono sin desinstalar](011_27_10_26_feedback_pruebas_telefono_sin_desinstalar.md) — connectedDebugAndroidTest borra datos y modelo; usar adb install -r + am instrument
