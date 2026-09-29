# CLAUDE.md

Guía para Claude Code al trabajar en este repositorio.

## Memoria del proyecto (`memorias/`)

El usuario trabaja este proyecto desde **dos computadoras**, así que la memoria persistente de Claude vive **dentro del repo**, en `memorias/`, y se sincroniza con git. **No uses la memoria local** (`~/.claude/projects/.../memory/`) para este proyecto.

- Índice (se carga automáticamente): @memorias/MEMORY.md
- Una memoria = un archivo `memorias/<NNN>_<dd>_<mm>_<yy>_<nombre_descriptivo>.md` (p. ej. `006_03_10_26_proyecto_estructura_android.md`), para ver en qué orden y cuándo se fue registrando cada cosa:
  - `NNN`: correlativo de 3 dígitos; la siguiente memoria toma el número más alto existente + 1 (no se reutilizan números de memorias borradas).
  - `dd_mm_yy`: fecha de **creación**; no cambia aunque la memoria se edite después.
  - `nombre_descriptivo`: en snake_case, sin tildes.
- Frontmatter (el `name` es el nombre del archivo sin `.md`):
  ```markdown
  ---
  name: <NNN>_<dd>_<mm>_<yy>_<nombre_descriptivo>
  description: <resumen de una línea para decidir si es relevante>
  metadata:
    type: user | feedback | project | reference
  ---

  <el hecho; para feedback/project añade líneas **Why:** y **How to apply:**. Enlaza otras memorias con [[name]].>
  ```
- Tras crear o editar un archivo, añade/actualiza su línea en `memorias/MEMORY.md` (`- [Título](archivo.md) — gancho`), en orden de número. Nunca pongas contenido en el índice.
- Antes de crear una memoria, revisa si ya existe una que lo cubra y actualízala; borra las que queden obsoletas. Convierte fechas relativas en absolutas.
- No guardes lo que ya está en el código, en git o en este `CLAUDE.md`. Nunca guardes secretos (URLs de `.env`, contraseñas, tokens).
- Al terminar de modificar memorias, recuerda al usuario que haga commit + push para que la otra computadora las reciba (y `git pull` al empezar en la otra).

## De qué va el proyecto

**Facturas S20** es una **app Android nativa en Java** (en español, orientada a Perú) para escanear facturas y boletas con la cámara del teléfono. Sus datos se extraen con un modelo **Gemma 4 E2B afinado (LoRA)** que corre **dentro del teléfono** (formato `.litertlm` con LiteRT-LM), y la app ayuda al titular a controlar su categoría y a hacer su declaración mensual del NRUS.

Claves de la arquitectura (el detalle está en `roadmap/001_27_09_26_documentacion_tecnica_y_roadmap.md`):
- **Java en al menos el 50 % del código** (exigencia del curso; se apunta a ~85 %). Lo único en Kotlin es el puente con LiteRT-LM, porque su API es Kotlin.
- **Un solo usuario y sin servidor.** Toda la data vive en el teléfono, en SQLite con Room + SQLCipher. El usuario exporta sus datos (ZIP con CSV, imágenes y PDF) para analizarlos en una PC. No hay cuentas, sincronización, nube ni consola de administración.
- **Funciona sin conexión.** La única conexión de red es la descarga única del modelo desde Hugging Face. En teléfonos sin RAM suficiente hay un modo de registro manual.
- Se desarrolla con **IntelliJ IDEA** (plugin de Android + Gradle). La configuración del equipo está en `roadmap/002_27_09_26_configuracion_equipo.md`.

### App Android (`android/`)

Proyecto Gradle propio; en IntelliJ se abre la carpeta `android/`, no la raíz del repo.

- Módulos: `:app` (Android; paquete `pe.facturass20`, `viewBinding`) y `:dominio` (`java-library`, Java puro sin Android, JUnit 5). Regla de dependencias: `ui → dominio ← inferencia, datos, exportacion`.
- Versiones: Gradle 9.8.0 (wrapper), AGP 9.1.1 (máximo que soporta IntelliJ 2026.2; no subir), Java 17, `compileSdk` 36.1, `minSdk` 26. **Todas las dependencias van en `android/gradle/libs.versions.toml`**; no escribas versiones sueltas en los `build.gradle.kts`.
- Los paquetes siguen la documentación técnica §3.3 y cada uno tiene su `package-info.java`. `MainActivity` está en `ui.comun`.

```bash
cd android
export JAVA_HOME=/c/Users/<usuario>/.jdks/temurin-17.0.20.1
./gradlew :app:assembleDebug :dominio:test   # verifica cambios con esto
./gradlew :app:installDebug                  # instala en el teléfono conectado
./gradlew :app:connectedDebugAndroidTest     # pruebas instrumentadas (base cifrada, Keystore) en el teléfono
```

### Prototipo previo (retirado del repo)

Antes de pasar a Android se hizo una PWA (React 19 + Vite + Tailwind, desplegada en Vercel) que llamaba a Gemma por un servidor FastAPI en Colab, expuesto con un túnel de Cloudflare. Se usó en la competencia Build with Gemma. El 2026-09-28 se borró del repo, junto con la copia `sergio/`, los diseños `design_factu/` y los notebooks viejos. **Su último estado está en el tag `prototipo-pwa`** (`git checkout prototipo-pwa` para verlo). No se debe retomar ni proponer: el producto es la app Android.

## Convenciones

- Todo el texto de la UI, mensajes de error y comentarios van en **español**. Los identificadores del dominio también, sin tildes (`FacturaCompra`, `importeTotal`); los sufijos técnicos van en inglés (`ViewModel`, `Dao`, `Fragment`). El resto de convenciones está en la documentación técnica §14.
- Dominio peruano: RUC de 11 dígitos (prefijo 10/15/17/20 y dígito verificador módulo 11), moneda PEN, dinero con `BigDecimal` de escala 2.
- Commits en español y en imperativo («Agrega validación de RUC»).

## Roadmap (`roadmap/`)

Plan de trabajo del desarrollo. Todo lo que sea roadmap, cronograma, pendientes o requerimientos por avanzar va aquí.

Los archivos siguen la misma convención que las memorias: `<NNN>_<dd>_<mm>_<yy>_<nombre_descriptivo>.md`. El número es correlativo (el siguiente toma el más alto + 1) y la fecha es la de creación.

- `001_27_09_26_documentacion_tecnica_y_roadmap.md` — **la base de todo lo que hay que hacer para completar el proyecto.** Referencia técnica de la app Android (arquitectura, contratos, esquema SQLite, pantallas, RF/RNF en §11) y roadmap en §13: entregas, iteraciones I1–I6, tareas, responsables, Gantt y mediciones comprometidas. El paso 0 (configuración del equipo) ya está hecho y remite a `002`. Si el código lo contradice, gana el documento, salvo que se decida cambiarlo. Si cambia el plan, conviene reflejarlo también en los scripts del entregable correspondiente.
- `002_27_09_26_configuracion_equipo.md` — paso 0 del roadmap. La Parte 1 es lo que se hizo en la primera computadora (errores incluidos); la Parte 2, los pasos para levantar el proyecto en otra computadora con IntelliJ recién instalado. Actualízalo si cambia algo del entorno.

## Documentos teóricos (`documentos_teoricos/`)

Entregables del curso (UTP, Curso Integrador I). Cada entregable tiene su subcarpeta `documentos_teoricos/entregable<N>/`, y dentro los archivos siguen la convención `entregable<N>_<tipo>.<ext>`:

- `entregable<N>_baseX.*` — material base que entrega el curso/usuario (enunciado, rúbrica, descripción del proyecto). En el entregable 2 los de apoyo tienen nombre descriptivo con el prefijo `base_`, para distinguirlos de los generados: `base_entregable2_puntos_evaluacion.pdf` (consigna y rúbrica, el principal), `base_entregable2_diferencias_con_entregable1.pdf` (estructura oficial del informe y anexos A–S) y `base_entregable2_consideraciones.docx` (notas del docente).
- `entregable<N>_documento.docx` — informe generado para ese entregable.
- `entregable<N>_presentacion.pptx` — sustentación generada para ese entregable.
- `entregable<N>_documentacion_tecnica.md` — (desde el 2) referencia para desarrolladores; el usuario la mueve a `roadmap/` para la conversación de código.
- `entregable<N>_archivos/` — (desde el 2) figuras PNG, fuentes SVG editables y `scripts/` que regeneran figuras, Word y PPT (ver su `README.md`).

El entregable 1 corresponde al APF1 y el 2 al APF2. Al añadir nuevos entregables, crea su subcarpeta, sigue la misma convención y referencia los archivos por esa ruta.

**Entregable 2:** el Word **no se edita a mano**: `entregable2/entregable2_archivos/scripts/construir_documento.py` lo arma sobre `entregable1/entregable1_documento.docx` (reutiliza sus bloques por índice de elemento del cuerpo y añade el contenido de `contenido_documento.py`), y `actualizar_indice.ps1` regenera el índice con Word (`-Docx` elige el archivo). `construir_documento_solo.py` genera `solo_entregable2_documento.docx` (solo los apartados del APF2) a partir del informe completo; `generar_todo.py` corre todo. Requiere Python de Windows (`C:\Users\diego\AppData\Local\Python\bin\python.exe`, con `lxml`, `Pillow`, `python-pptx`, `pymupdf`), Chrome (SVG → PNG) y Word/PowerPoint (COM) para revisar en PDF; LibreOffice y pandoc no están instalados.

## Notebooks de fine-tuning (`tunning_gemma4/`)

Solo queda **`tunning_model_gemma4_6.ipynb`** (las iteraciones `_1` a `_5` se borraron el 2026-09-28; están en el historial de git). Pasos: instalar Unsloth/TRL/transformers 5.5 → montar Google Drive (`/MyDrive/dataset_facturas/` con `data.json` + `imagenes/`) → cargar el LoRA existente o crear uno nuevo sobre el modelo base en 4 bits → entrenar con `SFTTrainer` (resolución 896, 3 épocas, 80/20 train/val) → guardar el LoRA en Drive → evaluar por campo con validación de RUC/fecha → levantar FastAPI en el puerto 8000 → exponerlo con `cloudflared tunnel`. Requiere GPU de Colab; no se puede ejecutar localmente.

La parte final (FastAPI + túnel) era para la PWA y **ya no se usa**. Para la app Android, el notebook debe terminar fusionando el LoRA y convirtiendo el modelo a `.litertlm` (documentación técnica §6.7, iteración I4).
