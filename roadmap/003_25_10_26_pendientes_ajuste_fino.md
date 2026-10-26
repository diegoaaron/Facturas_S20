# Pendientes del ajuste fino de Gemma 4 E2B (iteración I4)

Estado al 2026-10-25 (noche) y lo que falta, en orden. Complementa la documentación técnica §6.2, §6.5 y §6.7 (`001_…`). El contexto que no está en el código vive en `memorias/007_…` (reglas de etiquetado) y `memorias/008_…` (entorno de WSL y truco de memoria).

## Hecho

- **Dataset:** 52 fotos en `tunning_gemma4/datos/imagenes/` + `datos/etiquetas.csv` (7 campos), revisadas por el usuario. Carpeta **fuera de git** (datos reales): copiarla a mano a la otra computadora. `tunning_gemma4/validar_etiquetas.py` comprueba el formato.
- **Entrenamiento en local viable:** `tunning_gemma4/prueba_memoria_gpu.py` entrenó 10 pasos en la RTX 3050 de 6 GB (pico 4,17 GiB a 512 px, ~1 s/paso) dejando `embed_tokens_per_layer` en la CPU.
- **Formato de salida decidido:** JSON plano, sin espacios y sin confianza (§6.2 y §6.7). Ya cambiados `ParserRespuesta` (instrucción + confianza calculada con `ValidadorCampos`), `ParserRespuestaTest` (13 pruebas, pasan) y la documentación técnica. `assembleDebug`, `testDebugUnitTest` y `:dominio:test` pasan.

## Pendiente

1. ~~Commit y push del cambio de formato~~ — hecho el 2026-10-25.
2. ~~Probar la instrucción nueva en el teléfono~~ — hecho el 2026-10-25 con `MedicionGemmaTest` a 512 px en el Moto G34. El **modelo base no obedece «sin espacios»**: responde el JSON plano pero con saltos de línea y dentro de ```` ```json ````. Tardó 120 s y 64 s (antes, con el esquema de `valor` + `confianza`: 53 s y 47 s); los 7 campos salen bien. Con el base no se gana tiempo: la ganancia tiene que venir del afinado.
3. **Avisar a Coronel Obregón** de la decisión del formato (§6.7 decía que lo eligían ambos).
4. ~~Script de entrenamiento~~ — hecho: `tunning_gemma4/entrenar.py` (lo común está en `comun.py`, que lee la instrucción directo de `ParserRespuesta.java`). Divide por emisor (semilla 42): 31 fotos / 7 emisores para entrenar, 9 / 1 (Gloria) para validar y 12 / 7 para la prueba. Entrena solo sobre la respuesta (`train_on_responses_only`), 3 épocas, se queda con la de menor pérdida de validación. Deja el LoRA, `division.json` y `entrenamiento.json` en `tunning_gemma4/modelos/<nombre>/` (ignorada por git). Se corre desde `/tmp`: `python /mnt/c/reposPersonal/Facturas_S20/tunning_gemma4/entrenar.py --nombre lora_v1`.
5. **Evaluar por campo** — script hecho (`evaluar.py --nombre lora_v1`, deja `evaluacion_prueba.json` con cada respuesta). Resultado de `lora_v1` (5,7 min de entrenamiento, pico 4,22 GiB, mejor época 2) en las 12 fotos de prueba, a 512 px:

   | Campo | Base | Afinado v1 |
   |---|---|---|
   | ruc | 50 % | 58 % |
   | razon_social | 25 % | 50 % |
   | serie | 8 % | 83 % |
   | numero | 0 % | 50 % |
   | fecha_emision | 25 % | 67 % |
   | moneda | 83 % | 92 % |
   | importe_total | 75 % | 75 % |
   | comprobante completo | 0 % | 8 % |

   **Mejora al base en todo, pero no llega al 90 %** (criterio §6.7). Lo que queda:
   - El formato ya está aprendido (JSON compacto, serie y número separados, fecha ISO). Los errores restantes son **dígitos mal leídos** (RUC, número) a 512 px y **razón social** recortada (`CURTIS & CO.` en vez del nombre completo) o confundida con el nombre del cliente.
   - **Bucle de repetición:** en `compr_001` (base y afinado) y `compr_052` (base) el modelo escribe ceros sin parar en `numero`. Sin la llave de cierre, en la app eso duraba hasta el tiempo límite. **Resuelto el 2026-10-26:** `LiteRtLmPuente` corta a los `ParserRespuesta.MAX_TOKENS_RESPUESTA` (200) tokens y la app pide otra foto. Con el modelo base y la factura de control no llega al tope (la medición sigue pasando); el corte en sí no se pudo ver en el teléfono porque esa factura no provoca el bucle.
   - Hay pocos datos: 31 fotos de 7 emisores para entrenar. Lo que más debería ayudar es ampliar el dataset (punto 8). También se puede probar más resolución para los dígitos, pero en el teléfono 896 px tarda 4–5 min.
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
