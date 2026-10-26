# Pendientes del ajuste fino de Gemma 4 E2B (iteración I4)

Estado al 2026-10-25 (noche) y lo que falta, en orden. Complementa la documentación técnica §6.2, §6.5 y §6.7 (`001_…`). El contexto que no está en el código vive en `memorias/007_…` (reglas de etiquetado) y `memorias/008_…` (entorno de WSL y truco de memoria).

## Hecho

- **Dataset:** 52 fotos en `tunning_gemma4/datos/imagenes/` + `datos/etiquetas.csv` (7 campos), revisadas por el usuario. Carpeta **fuera de git** (datos reales): copiarla a mano a la otra computadora. `tunning_gemma4/validar_etiquetas.py` comprueba el formato.
- **Entrenamiento en local viable:** `tunning_gemma4/prueba_memoria_gpu.py` entrenó 10 pasos en la RTX 3050 de 6 GB (pico 4,17 GiB a 512 px, ~1 s/paso) dejando `embed_tokens_per_layer` en la CPU.
- **Formato de salida decidido:** JSON plano, sin espacios y sin confianza (§6.2 y §6.7). Ya cambiados `ParserRespuesta` (instrucción + confianza calculada con `ValidadorCampos`), `ParserRespuestaTest` (13 pruebas, pasan) y la documentación técnica. `assembleDebug`, `testDebugUnitTest` y `:dominio:test` pasan.

## Pendiente

1. ~~Commit y push del cambio de formato~~ — hecho el 2026-10-25.
2. ~~Probar la instrucción nueva en el teléfono~~ — hecho el 2026-10-25 con `MedicionGemmaTest` a 512 px en el Moto G34. El **modelo base no obedece «sin espacios»**: responde el JSON plano pero con saltos de línea y dentro de ```` ```json ````. Tardó 120 s y 64 s (antes, con el esquema de `valor` + `confianza`: 53 s y 47 s); los 7 campos salen bien. Con el base no se gana tiempo: la ganancia tiene que venir del afinado.
3. ~~Avisar a Coronel Obregón de la decisión del formato~~ — hecho el 2026-10-26.
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
6. **Fusionar y convertir a `.litertlm`** — la cadena funciona de punta a punta, pero **el modelo convertido lee peor que el oficial**. Hecho el 2026-10-26 con `lora_v1`:
   - `fusionar.py --nombre lora_v1` (entorno `~/venvs/gemma`): suma el delta del LoRA a los pesos de 16 bits de `unsloth/gemma-4-E2B-it`, archivo por archivo, sin cargar el modelo entero. Usa unos 5 GB de RAM y deja `modelos/lora_v1/fusionado/` (9,6 GB).
   - `convertir.py --nombre lora_v1` (entorno aparte `~/venvs/litert`, con `litert-torch-nightly` 0.11.0.dev20261025, porque pide transformers 5.20): `task=image_text_to_text`, visión incluida, embebedor externo, `cache_length` 1024, `dynamic_wi4_afp32`. Esta versión ya no tiene `--jinja_chat_template_override`: el script copia el `chat_template.jinja` del repo oficial en la carpeta fusionada antes de exportar. **No acepta `model_dtype='bfloat16'`** (falla el embebedor), así que va en float32: 86,6 min y pico de 19,8 GB de RAM. Hubo que subir WSL a 20 GB + 24 GB de swap en `C:\Users\diego\.wslconfig` (antes no existía; por defecto eran ~11,8 GB). Mientras convierte, a Windows le queda menos de 1 GB libre.
   - Resultado: `modelos/lora_v1/litertlm/model.litertlm`, 2,54 GiB (el oficial: 2,41 GiB). En el Moto G34 a 512 px carga y lee en **23–30 s** (el base oficial: 64–120 s) con 1,9 GB de memoria (el base: 2,7 GB), pero **lee mal la factura de control**: RUC `2001045678912`, serie `01`, número `001`; solo acierta moneda e importe. Además responde con ```` ```json ```` y saltos de línea, no en el formato compacto que aprendió.
   - Diagnóstico: el fusionado, probado en PyTorch (4 bits), lee bien los datos (incluso el RUC de `compr_039`, que el LoRA fallaba), pero en 3 de 4 fotos vuelve al formato del base. O sea: (a) **la fusión pierde parte del ajuste**, porque el LoRA se entrenó contra los pesos en 4 bits (QLoRA) y se fusiona sobre los de 16 bits; y (b) **la conversión estropea la lectura**, probablemente por la cuantización `dynamic_wi4_afp32`, más tosca que la del `.litertlm` oficial.
   - En el teléfono el oficial sigue como `modelo.litertlm`; el convertido quedó al lado como `modelo_afinado_v1.litertlm` (para probarlo, intercambiar los nombres con `adb shell mv`). Por Wi-Fi la subida iba a 0,75 MB/s; por cable, a 4,1 MB/s (10,5 min).
   - **Base sin afinar convertido con la misma receta** (`convertir.py --nombre base`, 52 min, lanzado desde una terminal propia porque Claude Code corta los procesos en segundo plano cuando falta memoria): **lee igual de mal** (RUC `200104567814`, razón social `ADINA S.A.C.`, serie y número `1`). El problema es la conversión, no el LoRA.
   - **Híbrido con la visión oficial** (texto nuestro + codificador, adaptador y fin de visión del `.litertlm` oficial, `max_num_patches` 2520): armado en segundos con `litert-lm-peek --dump_files_dir` + `litert-lm-builder toml`; quedó en `modelos/lora_v1/hibrido/`. **Empeora:** el RUC entra en bucle (lo cortó el tope de 200 tokens a los 52 s). La visión queda descartada; el culpable es **el modelo de texto convertido**, casi seguro la cuantización `dynamic_wi4_afp32` (nuestro `prefill_decode` pesa 1.163 MB; el oficial, 818 MB con activaciones fp16, otra receta).
   - Comparación de secciones (con `litert-lm-peek`): el oficial trae además audio y `mtp_drafter` (decodificación especulativa), y marca `prefer_activation_type=fp16` en visión y texto.
   - Por cable la subida al teléfono tardó 64 s en la última prueba (antes 10 min; la conexión varía).
   - **Receta que funciona (2026-10-26), medida con el base y la factura de control:** se recuantizaron las partes en float32 que quedaron del intento cortado (`modelos/base/litertlm/tmpy6m7opqh/`), unos 4 min cada una:

     | Variante del base | Lectura | Memoria | Tamaño |
     |---|---|---|---|
     | Todo a 4 bits | mal: RUC, razón social, serie, número, fecha | 1,9 GB | 2,54 GiB |
     | Texto a 8 bits | RUC ✓, razón social parcial, serie y número vacíos | 3,2 GB | 3,6 GiB |
     | Texto + embebedores a 8 bits | además razón social ✓ | 3,2 GB | 4,9 GiB |
     | **Texto + embebedores a 8 bits + visión oficial** | **igual que el oficial salvo la fecha** (`2025-10-25`; la impresa es 25/10/2026) | 3,5 GB | 4,93 GiB |
     | Texto a 8 bits + embebedores y visión oficiales | no sirve: los embebedores oficiales no son compatibles con nuestro texto | — | — |

     Lee en 32–57 s (el oficial: 64–120 s).
   - **La receta por defecto de `litert-torch` ya es 8 bits** (`dynamic_wi8_afp32`; `quantization_recipe=None` significa «la de siempre»). El 4 bits salió del plan de este documento. Cadena final:
     1. `fusionar.py --nombre lora_v1` (entorno `~/venvs/gemma`).
     2. `convertir.py --nombre lora_v1` (entorno `~/venvs/litert`, terminal propia; ~1 h, pico ~20 GB): exporta a 8 bits en `modelos/<nombre>/litertlm/`.
     3. `empaquetar.py --nombre lora_v1` (segundos): cambia la visión por la oficial y deja `modelos/<nombre>/empaquetado/model.litertlm`.
   - **`lora_v1` convertido así (2026-10-26):** pasa `MedicionGemmaTest`. Lee bien RUC, razón social, serie (`F001`) y número (`004521`) ya separados, moneda e importe, en el JSON compacto que aprendió. Falla la fecha igual que el base (`2025-10-25`; la impresa es 25/10/2026). 31–44 s por lectura, 3,5 GB de memoria, 4,93 GiB. En el teléfono quedó como `modelo_afinado_v1.litertlm` (el oficial sigue en `modelo.litertlm`).
   - **Evaluación del `.litertlm` en las 12 fotos de prueba (2026-10-26)** con `evaluar_litertlm.py` (entorno `~/venvs/litertlm` con `litert-lm-api` 0.18.1; corre el archivo que va al teléfono en la CPU de la PC, ~7 s por foto, y lee la factura de control igual que el teléfono):

     | Campo | Oficial | Base convertido | **Afinado v1 convertido** | (afinado v1 en PyTorch) |
     |---|---|---|---|---|
     | ruc | 50 % | 42 % | **67 %** | 58 % |
     | razon_social | 17 % | 33 % | **58 %** | 50 % |
     | serie | 0 % | 0 % | **75 %** | 83 % |
     | numero | 8 % | 8 % | **42 %** | 50 % |
     | fecha_emision | 17 % | 0 % | **25 %** | 67 % |
     | moneda | 58 % | 100 % | **92 %** | 92 % |
     | importe_total | 50 % | 83 % | **83 %** | 75 % |

     El afinado convertido **supera al oficial en todos los campos** y la conversión no le quita lo aprendido (sube en unos campos y baja en otros frente a PyTorch; con 12 fotos, una foto son 8 puntos). No llega al 90 %. El oficial responde todo `null` en 5 de las 12 fotos reales. Los errores del afinado son casi todos un dígito mal leído o la fecha corrida unos días (límite de 512 px), y `compr_001` entra en bucle de ceros.
   - **Pendiente:** ver si alguna parte puede volver a 4 bits para achicar los 4,9 GiB (la descarga del oficial es de 2,4 GiB); ver si los 3,5 GB de memoria caben en los teléfonos objetivo (RNF-05); medir 768 px en el teléfono con el afinado (los errores son de dígitos). `litert-lm-api` trae `LoraConfig`: LiteRT-LM podría aplicar el LoRA sobre el modelo oficial en tiempo de ejecución, sin convertir todo; vale la pena averiguarlo.
7. **Publicar** (§6.6/§6.7): subir a Hugging Face en una revisión fija, calcular SHA-256, actualizar `assets/modelo.json` y sacar versión de la app.
8. **Ampliar el dataset:** más emisores, más facturas (hoy casi todo son boletas) y alguna en USD si hay. Etiquetar con las reglas de `memorias/007_…`.
9. **Ordenar después:** actualizar §6.7 (todavía dice 896 px y el cuaderno) y decidir si se retira `tunning_model_gemma4_6.ipynb` en favor de los scripts.

## Notas de entorno

- WSL `Ubuntu-26.04`, venv `~/venvs/gemma` (`source ~/venvs/gemma/bin/activate`). Lanzar WSL desde la herramienta **PowerShell**, no desde Git Bash (que convierte rutas y expande `$VAR`). Para lo que pida `sudo`: `wsl -d Ubuntu-26.04 -u root -- …`.
- Gradle: `JAVA_HOME=/c/Users/diego/.jdks/temurin-17.0.20.1`.
