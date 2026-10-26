# Pendientes del ajuste fino de Gemma 4 E2B (iteración I4)

Estado al 2026-10-25 y lo que falta, en orden. Complementa la documentación técnica §6.2, §6.5 y §6.7 (`001_…`). El contexto que no está en el código vive en `memorias/007_…` (reglas de etiquetado) y `memorias/008_…` (entorno de WSL y truco de memoria).

## Hecho

- **Dataset:** 52 fotos en `tunning_gemma4/datos/imagenes/` + `datos/etiquetas.csv` (7 campos), revisadas por el usuario. Carpeta **fuera de git** (datos reales): copiarla a mano a la otra computadora. `tunning_gemma4/validar_etiquetas.py` comprueba el formato.
- **Entrenamiento en local viable:** `tunning_gemma4/prueba_memoria_gpu.py` entrenó 10 pasos en la RTX 3050 de 6 GB (pico 4,17 GiB a 512 px, ~1 s/paso) dejando `embed_tokens_per_layer` en la CPU.
- **Formato de salida decidido:** JSON plano, sin espacios y sin confianza (§6.2 y §6.7). Ya cambiados `ParserRespuesta` (instrucción + confianza calculada con `ValidadorCampos`), `ParserRespuestaTest` (13 pruebas, pasan) y la documentación técnica. `assembleDebug`, `testDebugUnitTest` y `:dominio:test` pasan.

## Pendiente

1. ~~Commit y push del cambio de formato~~ — hecho el 2026-10-25.
2. **Probar la instrucción nueva en el teléfono** con el modelo base (`MedicionGemmaTest` / `LecturaGemmaTest`, `./gradlew :app:connectedDebugAndroidTest`): comprobar que responde en JSON plano y cómo cambia el tiempo frente a la tabla de §6.5.
3. **Avisar a Coronel Obregón** de la decisión del formato (§6.7 decía que lo eligían ambos).
4. **Script de entrenamiento completo** (`tunning_gemma4/entrenar.py`), partiendo de `prueba_memoria_gpu.py`:
   - Respuesta = `json.dumps({7 campos}, ensure_ascii=False, separators=(',', ':'))`, con `null` para celdas vacías. La instrucción debe ser **idéntica** a `ParserRespuesta.INSTRUCCION` (copiarla tal cual).
   - División entrenamiento / validación / prueba **por emisor (RUC)**, para no medir de más; `compr_021` y `compr_022` son la misma boleta y van juntas. Son 52 fotos de 15 emisores: la prueba saldrá chica, conviene anotarlo en los resultados.
   - 512 px (lo que recibe el modelo en la app), LoRA r=16 con visión congelada, 3 épocas, reutilizando `EmbeddingEnCpu`.
   - Guardar el LoRA en una carpeta ignorada por git (p. ej. `tunning_gemma4/modelos/`). Ejecutar con cwd en `/tmp` (Unsloth crea `unsloth_compiled_cache/`).
5. **Evaluar por campo** el afinado contra el modelo base en el conjunto de prueba (coincidencia exacta tras normalizar como `ParserRespuesta`). Criterio §6.7: mejorar al base y ≥ 90 % por campo.
6. **Fusionar y convertir a `.litertlm`:**
   - Subir la RAM de WSL en `C:\Users\diego\.wslconfig` (hoy 11 GB; fusionar a 16 bits pide ~10 GB o más).
   - `litert-torch export_hf` (paquete `litert-torch-nightly`) con `--externalize_embedder`, `--jinja_chat_template_override=litert-community/gemma-4-E2B-it-litert-lm` (la plantilla original usa `map.get()` y falla en el teléfono), `cache_length` ≥ 1024 y cuantización `dynamic_wi4_afp32`.
   - Visión: probar `--task=multimodal_lm` (litert-torch ≥ 0.10.0). Si el codificador de visión sale mal, copiar sus secciones del `.litertlm` oficial de `litert-community/gemma-4-E2B-it-litert-lm` (el LoRA no las toca).
   - **Evaluar de nuevo después de convertir** (hay reportes de modelos afinados que empeoran al convertir) y medir en el Moto G34 con `MedicionGemmaTest`.
7. **Publicar** (§6.6/§6.7): subir a Hugging Face en una revisión fija, calcular SHA-256, actualizar `assets/modelo.json` y sacar versión de la app.
8. **Ampliar el dataset:** más emisores, más facturas (hoy casi todo son boletas) y alguna en USD si hay. Etiquetar con las reglas de `memorias/007_…`.
9. **Ordenar después:** actualizar §6.7 (todavía dice 896 px y el cuaderno) y decidir si se retira `tunning_model_gemma4_6.ipynb` en favor de los scripts.

## Notas de entorno

- WSL `Ubuntu-26.04`, venv `~/venvs/gemma` (`source ~/venvs/gemma/bin/activate`). Lanzar WSL desde la herramienta **PowerShell**, no desde Git Bash (que convierte rutas y expande `$VAR`). Para lo que pida `sudo`: `wsl -d Ubuntu-26.04 -u root -- …`.
- Gradle: `JAVA_HOME=/c/Users/diego/.jdks/temurin-17.0.20.1`.
