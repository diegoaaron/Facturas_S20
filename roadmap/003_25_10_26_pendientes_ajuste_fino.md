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
8. **Ampliar el dataset:** más emisores, más facturas (hoy casi todo son boletas) y alguna en USD si hay. Etiquetar con las reglas de `memorias/007_…`. El 2026-10-27 el usuario sumó 69 fotos y revisó las etiquetas dudosas (van 120 tras sacar una ilegible; 19 emisores, 28 facturas). `dividir_por_emisor` suma el grupo `prueba_conocidos` (un comprobante nuevo de cada proveedor de entrenamiento con 4 fotos o más) para medir aparte el caso de proveedor conocido. División (semilla 42): 58 fotos de entrenamiento, 21 de validación, 32 de prueba con proveedor nuevo (AC, CBC, Ecobesa, S&C) y 9 de proveedor conocido. Resultados en PyTorch (afinado; entre paréntesis el base):

   | campo | `lora_v2` 512 px, nuevo | `lora_v2_768`, nuevo | `lora_v2` 512 px, conocido | `lora_v2_768`, conocido |
   |---|---|---|---|---|
   | ruc | 47 % (34) | 50 % (34) | 78 % (44) | 78 % (67) |
   | razon_social | 25 % (3) | 28 % (3) | 78 % (33) | 89 % (56) |
   | serie | 53 % (34) | 69 % (38) | 100 % (44) | 100 % (33) |
   | numero | 47 % (28) | 41 % (28) | 67 % (33) | 78 % (22) |
   | fecha_emision | 19 % (12) | 28 % (16) | 11 % (0) | 22 % (11) |
   | moneda | 100 % | 97 % | 100 % | 100 % |
   | importe_total | 22 % (22) | 25 % (19) | 22 % (33) | 56 % (33) |
   | completos | 0 % | 3 % | 11 % | 22 % |

   No son comparables con `lora_v1` (otra prueba, más grande y difícil). Errores típicos: dígitos inventados, fecha corrida a octubre (casi todo lo nuevo es de octubre), en Ecobesa el total sin percepción y «ECOBESA» antepuesto a la razón social. **Gemma 4 lleva toda imagen a 280 tokens como máximo (`max_soft_tokens`, unos 700 × 930 px)**: a 512 px se desperdicia detalle, a 768 px se usa casi todo y por encima de ~900 px no se gana nada. Subir a 560/1120 tokens exigiría exportar nuestra propia visión (el codificador oficial llega a 2520 parches = 280 tokens, y la visión exportada perdía serie/número). Las fotos nuevas muestran la hoja A4 entera con mesa alrededor, lo que achica los números. **El usuario eligió `lora_v2_768` como mejor modelo (2026-10-27)** y sigue la cadena (pasos de abajo).

   **Cadena de `lora_v2_768` en curso** (retomar desde el primer paso sin hacer):
   1. ~~Entrenar~~ (5,7 min, pico 4,22 GiB, pérdida val 0,120/0,114/0,120, mejor época 2) y ~~evaluar en PyTorch~~ (tabla de arriba).
   2. ~~Fusionar~~: `fusionar.py --nombre lora_v2_768` (hecho el 2026-10-27; 245 pesos LoRA, deja `modelos/lora_v2_768/fusionado/`).
   3. ~~Convertir~~ (51,1 min, `litertlm/model.litertlm` de 4,88 GiB) y ~~empaquetar~~ (`empaquetado/model.litertlm`, 4,93 GiB), hechos el 2026-10-27. Comando de conversión (~1 h, el usuario desde su terminal): `source ~/venvs/litert/bin/activate && cd /tmp && python /mnt/c/reposPersonal/Facturas_S20/tunning_gemma4/convertir.py --nombre lora_v2_768`.
   4. Empaquetar: `empaquetar.py --nombre lora_v2_768` (venv litert, segundos).
   5. ~~Evaluar el `.litertlm` en la PC~~ (hecho el 2026-10-27, ~7 s por foto). Afinado frente al oficial, a 768 px:

      | campo | nuevo: oficial | nuevo: afinado | conocido: oficial | conocido: afinado |
      |---|---|---|---|---|
      | ruc | 31 % | 59 % | 33 % | 78 % |
      | razon_social | 9 % | 16 % | 33 % | 89 % |
      | serie | 34 % | 72 % | 33 % | 100 % |
      | numero | 28 % | 44 % | 33 % | 67 % |
      | fecha_emision | 12 % | 34 % | 22 % | 0 % |
      | moneda | 81 % | 97 % | 78 % | 100 % |
      | importe_total | 16 % | 25 % | 22 % | 33 % |
      | completos | 0 % | 3 % | 0 % | 0 % |

      Supera al oficial en todo salvo la fecha con proveedor conocido (9 fotos: una foto son 11 puntos). La conversión no le quita lo aprendido (parecido a PyTorch). No cumple el 90 %.
   6. ~~Teléfono~~ (hecho el 2026-10-27): `MedicionGemmaTest` con `-e lados 768,512` en el Moto G34, modo automático (CPU + visión en GPU). Carga en 13,7 s; lee la factura de control **con los 7 campos bien, fecha incluida** (`lora_v1` erraba la fecha); **~32 s por lectura a 768 y a 512 px** (la primera, 46 s, calienta), memoria ~3,5 GB. Como a 768 px no cuesta más (la imagen siempre son 280 tokens), `ReductorFoto.LADO_MODELO` pasó a 768. En el teléfono, `modelo.litertlm` es ahora `lora_v2_768`; el oficial quedó como `modelo_oficial.litertlm` y sigue `modelo_afinado_v1.litertlm`.
   Original del paso 5: evaluar el `.litertlm` en la PC a 768 px en `prueba` y `prueba_conocidos`, contra el oficial (venv litertlm). `evaluar_litertlm.py --nombre lora_v2_768 --grupo <g> --modelos oficial,afinado --resolucion 768` (logs en `~/evaluar_litertlm_v2_768_<g>.log`).
   7. ~~Registrar resultados aquí~~; falta commit + push (lo hace el usuario o le da permiso a Claude). **Cadena de `lora_v2_768` terminada**: lo siguiente son los pendientes 10 (QR) y 11 (recorte).
9. **Ordenar después:** actualizar §6.7 (todavía dice 896 px y el cuaderno) y decidir si se retira `tunning_model_gemma4_6.ipynb` en favor de los scripts.
10. **Leer el QR de SUNAT (después de `lora_v2`):** probado el 2026-10-27 con `zxing-cpp` sobre las fotos a resolución completa: se lee en 37 de 121 y en las 37 coincide con las etiquetas. Trae RUC, tipo, serie, número, IGV, total **sin percepción** y fecha; no trae razón social ni moneda, y cada emisor lo arma distinto (Curtis sin RUC/serie/número; Gloria, AC y AyC con serie y número juntos, AC/AyC sin la «B»; Gloria con fecha `dd/mm/aaaa`). Usos: pre-etiquetar fotos nuevas y, en la app, leer el QR con ML Kit (sin conexión) sobre la foto original antes de reducirla, y dejar a Gemma solo lo que falte. **Recomendado por Claude y apuntado por el usuario el 2026-10-27, para después de la cadena de `lora_v2_768`.**

    **En la app (2026-10-27):** `LectorQr` (ML Kit `barcode-scanning` 17.3.0, +8 MB de APK, que queda en 48,7 MB) busca el QR en `PreparadorFoto` sobre el marco sin reducir, y la cámara pasó de 1920 × 1440 a 4080 × 3072. `QrSunat` (dominio, con pruebas) entiende los cinco formatos vistos (SUNAT; serie y número juntos; serie sin letra; solo IGV, total y fecha como Curtis; `clave:valor` como VIACE) y descarta los que no son del comprobante (Yape de LCN, enlace a Drive de Salem, que trae además el de SUNAT). `LecturaViewModel` corrige con él lo que leyó Gemma (§6.4). `LecturaQrDatasetTest` en el Moto G34: **ML Kit lee el QR en 39 de 120 fotos, todos coinciden con las etiquetas, ~160 ms por foto**; decodificando a la mitad, 34. Gemma sigue corriendo siempre.
    - **Falta:** probarlo con la cámara en el teléfono (que la foto a 12 MP no haga lento el disparo ni falle por memoria y que el QR se lea dentro del marco); pre-etiquetar con el QR las fotos nuevas del dataset.
    - **Ideas para después:** con QR completo y el emisor ya registrado (razón social en la base), no correr Gemma: la lectura pasaría de ~32 s a menos de 1 s (la moneda quedaría en PEN). En el modo manual (teléfonos sin RAM para Gemma) el QR podría prellenar los campos.
11. **Recortar el comprobante antes del modelo (después de la cadena de `lora_v2_768`):** con el escáner de documentos de ML Kit (sin conexión) en la app, para que los 280 tokens de imagen se gasten en el papel y no en la mesa; recortar igual las fotos del dataset y reentrenar. Junto con el QR (pendiente 10), es lo que más debería subir la lectura de números y fecha. Apuntado por el usuario el 2026-10-27.

## Notas de entorno

- WSL `Ubuntu-26.04`, venv `~/venvs/gemma` (`source ~/venvs/gemma/bin/activate`). Lanzar WSL desde la herramienta **PowerShell**, no desde Git Bash (que convierte rutas y expande `$VAR`). Para lo que pida `sudo`: `wsl -d Ubuntu-26.04 -u root -- …`.
- Gradle: `JAVA_HOME=/c/Users/diego/.jdks/temurin-17.0.20.1`.
