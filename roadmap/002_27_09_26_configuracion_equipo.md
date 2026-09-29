# Configuración del equipo de desarrollo (IntelliJ + Android)

Este documento tiene dos partes:

- **Parte 1 — Lo que se hizo en la primera computadora (2026-09-27).** Es el historial de cómo se creó y configuró el proyecto Android, con los errores que salieron y cómo se resolvieron. Es solo referencia: **no hay que repetirlo**.
- **Parte 2 — Levantar el proyecto en otra computadora.** Son los pasos para una computadora con **IntelliJ IDEA 2026.2 recién instalado**, usando el proyecto que ya está en el repo, hasta tener la app corriendo en el teléfono. Es el punto de partida antes de empezar el desarrollo.

---

# Parte 1 — Lo que se hizo en la primera computadora

Entorno: Windows 11, IntelliJ IDEA **2026.2.3**, teléfono de pruebas con **Android 15**.

### 1.1 Plugin de Android
- En la pantalla de bienvenida **no existe el menú *File*** (solo aparece con un proyecto abierto). Se entró por **⚙ (abajo a la izquierda) → Settings → Plugins → Marketplace → "Android"** (autor *JetBrains s.r.o.*) → **Install** → **Restart IDE**.

### 1.2 Android SDK
- Se entró por **⚙ → Settings → Languages & Frameworks → Android SDK Updater**. En esta versión se llama así, no "SDK Manager".
- La ruta estaba vacía y salía el aviso rojo *"cannot be at the filesystem root"*. Se pulsó **Edit** y, en la ventana **SDK Setup**, se eligió la ruta `C:\Users\aaron\AppData\Local\Android\Sdk`. Luego se aceptaron las licencias → **Finish**.
- Quedó instalado **Android 16.0 "Baklava" (API 36.1)**. No se instalaron las previews (CANARY, DEV, CinnamonBun) ni las API 37.x.
- El teléfono tiene Android 15 (API 35) y **no fue necesario** instalar esa plataforma: la app compila con la API 36 y corre desde Android 8.0 (`minSdk 26`).

### 1.3 Creación del proyecto
- **Primer intento, fallido:** se eligió la plantilla **"Empty Activity"**, que salió en **Kotlin + Jetpack Compose** (`MainActivity.kt`, `ui/theme/Color.kt`, `Theme.kt`, `Type.kt`). Se borró la carpeta y se volvió a crear.
- **Segundo intento, correcto:** **New Project → Android → "Empty Views Activity"**, con estos datos:

  | Campo | Valor |
  |---|---|
  | Name | `Facturas S20` |
  | Package name | `pe.facturass20` |
  | Save location | `C:\reposPersonal\Facturas_S20\android` |
  | Language | Java |
  | Minimum SDK | API 26 |
  | Build configuration language | Kotlin DSL |

- A los avisos **"Add Files to Git"** y **"IDE project settings can be added to Git"** se les dio **Cancel / Don't Ask Again**.

### 1.4 Errores al sincronizar y ejecutar, y cómo se resolvieron

| Error | Causa | Solución |
|---|---|---|
| `Invalid Gradle JDK configuration found` y la franja *"Module JDK is not defined"* | Gradle no tenía un JDK | **☰ → File → Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JVM** (estaba en rojo `GRADLE_LOCAL_JAVA_HOME`) → **Download JDK → 17 → Eclipse Temurin** → Sync |
| Aviso *"Sync Android SDKs — The SDK path 'unset'…"* | `local.properties` no tenía la ruta del SDK | Se pulsó **OK**; IntelliJ escribió la ruta |
| `AAPT: error: resource mipmap/ic_launcher ... not found` al pulsar Run | La plantilla de IntelliJ no crea los íconos | Se agregaron íconos adaptativos en XML: `mipmap-anydpi-v26/ic_launcher*.xml`, `drawable/ic_launcher_foreground.xml` y `values/ic_launcher_background.xml` |
| `The project is using an incompatible version (AGP 9.4.1)… Latest supported version is AGP 9.1.0` | Se había subido AGP a la última de Maven, pero el plugin Android de IntelliJ 2026.2 solo soporta hasta 9.1 | Se volvió a **AGP 9.1.1** (la de la plantilla) |
| `NoClassDefFoundError: ProjectTypeBinding` (en la terminal) | AGP 9.4.1 con Gradle 9.3.1 | Desapareció al volver a AGP 9.1.1; el wrapper quedó en Gradle 9.8.0 |

Con eso la app se instaló en el teléfono y mostró **"Hello World!"**.

### 1.5 Configuración base que quedó en el repo (commit `e84444d`)

| Qué | Valor |
|---|---|
| Gradle (wrapper) | 9.8.0 (`gradlew`, `gradlew.bat`, `gradle/wrapper/`) |
| Android Gradle Plugin | **9.1.1**. No subir de 9.1.x mientras IntelliJ no lo soporte |
| Java | 17 en `:app` y `:dominio` |
| SDK | `compileSdk` 36.1 · `targetSdk` 36 · `minSdk` 26 |
| Versiones | todas en `android/gradle/libs.versions.toml` (se usan con `libs.<alias>`) |
| Módulos | `:app` (Android, `viewBinding`) y `:dominio` (`java-library`, Java puro, JUnit 5) |
| Paquetes | estructura de `001_27_09_26_documentacion_tecnica_y_roadmap.md` §3.3, con un `package-info.java` por paquete; `MainActivity` en `pe.facturass20.ui.comun` |
| `android/.gitignore` | excluye `.gradle/`, `build/`, `local.properties`, `.idea/`, `*.iml`, llaves de firma y `*.litertlm` |
| `android/.gitattributes` | `gradlew` con finales LF |

---

# Parte 2 — Levantar el proyecto en otra computadora

Requisito: IntelliJ IDEA **2026.2** instalado y el repo clonado en `C:\reposPersonal\Facturas_S20`. Sigue los pasos en orden.

### Paso 1 · Traer el proyecto
En una terminal (Git Bash o PowerShell):
```bash
cd /c/reposPersonal/Facturas_S20
git pull
```
Comprueba que exista la carpeta `android/`, con `gradlew`, `settings.gradle.kts`, `app/` y `dominio/`.

### Paso 2 · Instalar el plugin de Android
1. Abre IntelliJ. En la bienvenida, pulsa **⚙ (abajo a la izquierda) → Settings → Plugins**.
2. En **Marketplace**, busca **"Android"** (autor *JetBrains s.r.o.*) → **Install**.
3. Pulsa **OK** → **Restart IDE**.
4. Comprueba en **⚙ → Settings → Plugins → Installed** que "Android" está marcado.

### Paso 3 · Instalar el Android SDK
1. **⚙ → Settings → Languages & Frameworks → Android SDK Updater**. Ignora *Android (Experimental)*.
2. En *Android SDK Location* pulsa **Edit**. Luego, en **SDK Setup**:
   - Deja marcados *Android SDK* y *Android SDK Platform*.
   - Ruta: `C:\Users\<usuario>\AppData\Local\Android\Sdk`.
   - Pulsa **Next**, acepta **cada** licencia, pulsa **Finish** y espera la descarga.
3. En **SDK Platforms**, confirma que está instalado **Android 16.0 "Baklava" (API 36.1)**. Si no, márcalo. **No marques** las previews (CANARY, DEV, nombres en clave).
4. En **SDK Tools**, confirma que están *Android SDK Build-Tools*, *Android SDK Platform-Tools* y *Android Emulator*.
5. Pulsa **Apply → OK**.

### Paso 4 · Abrir el proyecto
1. En la bienvenida, pulsa **Open** y elige **`C:\reposPersonal\Facturas_S20\android`**. ⚠️ Esa carpeta, no la raíz del repo.
2. Si pregunta *Trust project*, pulsa **Trust Project**.
3. IntelliJ empieza a sincronizar. Lo más probable es que falle con *"Invalid Gradle JDK configuration found"*; es normal y se arregla en el paso 5.

### Paso 5 · Configurar el JDK 17 de Gradle
1. Menú **☰ (arriba a la izquierda) → File → Settings → Build, Execution, Deployment → Build Tools → Gradle**. Otra forma: el enlace **Open Gradle Settings** del error.
2. En **Gradle JVM** (no se llama "Gradle JDK"):
   - Si ya hay un **17**, elígelo.
   - Si no, elige **Download JDK… → Version 17 → Vendor Eclipse Temurin → Download**.
3. Deja *Distribution: Wrapper* y *Build and run using: Gradle*. Pulsa **Apply → OK**.

### Paso 6 · Sincronizar
1. En la pestaña **Build** (abajo), pulsa **🔄 Sync**.
2. Si sale **"Sync Android SDKs — The SDK path 'unset'…"**, pulsa **OK**. IntelliJ crea `local.properties` con la ruta de esta computadora; ese archivo **no se sube a git**.
3. La primera vez tarda varios minutos: descarga Gradle 9.8.0 y las librerías.
4. ✅ Termina bien con **"android: finished"** / **BUILD SUCCESSFUL**. Los módulos `app` y `dominio` aparecen en el panel *Gradle*.
5. Se pueden ignorar:
   - *"New Minor Gradle Version Available"*;
   - *"Deprecated Gradle features… setVisible"* (viene del plugin de Android).
6. A **"Add files to Git"** o **"IDE project settings can be added to Git"**, responde **Cancel / Don't Ask Again**. El proyecto ya está versionado.
7. Si IntelliJ sugiere **actualizar AGP** ("AGP Upgrade Assistant"), **no lo aceptes**. Debe quedarse en 9.1.1.

### Paso 7 · Preparar el teléfono
1. **Ajustes → Acerca del teléfono →** toca **Número de compilación** 7 veces. En Samsung o Xiaomi puede estar en *Información de software*.
2. **Ajustes → Sistema → Opciones de desarrollador →** activa **Depuración por USB**.
3. Conéctalo por USB en modo **Transferencia de archivos** y acepta **"¿Permitir depuración USB?"** marcando *Permitir siempre desde esta computadora*.

### Paso 8 · Ejecutar
1. En la barra superior, donde dice *No Devices*, elige tu teléfono. Al lado debe decir **app**.
2. Pulsa **Run ▶** (`Shift+F10`).
3. ✅ Se instala **Facturas S20** (ícono verde con una factura) y abre en la configuración inicial: **"Configure su negocio · Paso 1 de 3"**. Antes del commit `7584731` mostraba "Hello World!".
4. Si el teléfono aparece **dos veces** (pasa con *Depuración inalámbrica*: son dos conexiones del mismo aparato), elige solo una.

### Paso 9 · (Opcional) Comprobar desde la terminal
Desde Git Bash, ajustando el nombre de la carpeta del JDK que se descargó:
```bash
cd /c/reposPersonal/Facturas_S20/android
export JAVA_HOME=/c/Users/<usuario>/.jdks/temurin-17.<version>
./gradlew :app:assembleDebug :dominio:build    # debe terminar en BUILD SUCCESSFUL
./gradlew :app:connectedDebugAndroidTest       # pruebas instrumentadas en el teléfono conectado
```
En PowerShell o cmd se usa `gradlew.bat` en lugar de `./gradlew`.

- Si todavía no abriste el proyecto en IntelliJ, no existe `local.properties` y Gradle no encuentra el SDK. Créalo en `android/` con la ruta usando **barras normales**: `sdk.dir=C:/Users/<usuario>/AppData/Local/Android/Sdk`. Si usas `\`, en un `.properties` tienen que ir dobles (`C\:\\Users\\...`), y al escribirlas desde Git Bash se pierden fácilmente. Con una ruta mal escrita falla con *"El nombre de archivo, el nombre de directorio o la sintaxis de la etiqueta del volumen no son correctos"*.
- La primera compilación descarga sola **Build-Tools 36.0.0** si falta.
- Si `adb devices` muestra el teléfono dos veces (depuración inalámbrica), `installDebug` y `connectedDebugAndroidTest` lo usarían dos veces. Limítalo a uno con `export ANDROID_SERIAL=<serial>` (el serial es la primera columna de `adb devices`), o instala directamente con `adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk`. No uses `adb -t <id>`: el `transport_id` cambia cada vez que se reconecta. `adb` está en `<SDK>/platform-tools/adb.exe`.

### Si algo falla

| Síntoma | Qué hacer |
|---|---|
| Franja azul *"Plugins supporting Android files found"* al abrir un archivo | Falta el plugin de Android o está desactivado: pulsa **Install Android plugin** → **Restart IDE** → Sync (equivale al paso 2) |
| En *Open* no se reconoce como proyecto Gradle | Abriste la raíz del repo; abre la carpeta `android/` |
| `Invalid Gradle JDK configuration found` | Paso 5 |
| `incompatible version (AGP …)` | Alguien subió AGP; en `android/gradle/libs.versions.toml` debe decir `agp = "9.1.1"` |
| `mipmap/ic_launcher ... not found` | Falta hacer `git pull`; los íconos están en el repo |
| El teléfono no aparece en *Devices* | Revisa el cable (algunos solo cargan), el aviso de depuración en el teléfono o el driver USB del fabricante |
| `IOException: El nombre de archivo… sintaxis de la etiqueta del volumen no son correctos` al compilar | Ruta del SDK mal escrita en `android/local.properties`; ver el paso 9 |
| El teléfono aparece dos veces | Depuración inalámbrica; elige uno (en terminal, `ANDROID_SERIAL`, ver el paso 9) |
| El menú *File* no aparece | En la bienvenida no existe; con el proyecto abierto está dentro de **☰** |

Con la app abierta en el teléfono, el equipo está listo para empezar el desarrollo: la iteración **I3** de `001_27_09_26_documentacion_tecnica_y_roadmap.md` §13 (este documento es su paso 0).
