# Configuración del equipo de desarrollo (IntelliJ + Android)

Pasos que **funcionaron** el 2026-09-27 para dejar IntelliJ IDEA 2026.2 listo y correr la app Android de Facturas S20 en un teléfono. Sirve para configurar la otra computadora o una nueva.

> El proyecto Android ya existe en el repo, en `android/`. En otra computadora **no se crea de nuevo**: se hace `git pull` y se abre esa carpeta (sección B). La sección C cuenta cómo se creó, solo como referencia.

---

## A. Preparar IntelliJ (una vez por computadora)

### A1. Actualizar IntelliJ
- Usa la versión más reciente de IntelliJ IDEA (se probó con **2026.2**).

### A2. Instalar el plugin de Android
En la **pantalla de bienvenida** no existe el menú *File*; solo aparece con un proyecto abierto.

1. En la bienvenida, pulsa **⚙ (abajo a la izquierda) → Settings → Plugins**.
2. En **Marketplace**, busca **"Android"** (autor *JetBrains s.r.o.*) y pulsa **Install**.
3. Pulsa **OK** y luego **Restart IDE**.
4. Vuelve a **⚙ → Settings → Plugins → Installed** y confirma que "Android" está marcado.

### A3. Instalar el Android SDK
1. **⚙ → Settings → Languages & Frameworks → Android SDK Updater**. En esta versión se llama así, no "SDK Manager". Ignora *Android (Experimental)*.
2. *Android SDK Location* está vacío (sale el aviso rojo *"cannot be at the filesystem root"*). Pulsa **Edit**.
3. En la ventana **SDK Setup**:
   - Deja marcados *Android SDK* y *Android SDK Platform*.
   - Ruta: `C:\Users\<usuario>\AppData\Local\Android\Sdk`.
   - Pulsa **Next**, acepta **cada** licencia y pulsa **Finish**. Espera a que termine la descarga.
4. De vuelta en *Android SDK Updater*, en la pestaña **SDK Platforms** queda instalado **Android 16.0 "Baklava" (API 36.1)**. Con eso basta.
   - **No** marques *CANARY Preview*, *DEV Preview* ni las de nombre en clave (*CinnamonBun*).
   - La API 37.x no hace falta por ahora.
5. En la pestaña **SDK Tools**, verifica que estén instalados:
   - *Android SDK Build-Tools*
   - *Android SDK Platform-Tools*
   - *Android Emulator*

   Pulsa **Apply → OK**.

> **Teléfono con Android 15 (API 35):** no hace falta instalar la plataforma 35. La app se compila con la API 36, pero se instala en cualquier Android desde el 8.0 (`minSdk 26`).

---

## B. Abrir el proyecto (en la otra computadora)

1. Actualiza el repo con `git pull` en `C:\reposPersonal\Facturas_S20`.
2. En la bienvenida de IntelliJ, pulsa **Open** y elige la carpeta **`C:\reposPersonal\Facturas_S20\android`**. No elijas la raíz del repo, que es la PWA con npm.
3. Si pregunta *Trust project*, pulsa **Trust**.
4. Configura el JDK de Gradle; sin esto, el sync falla con *"Invalid Gradle JDK configuration found"*:
   1. Menú **☰ (arriba a la izquierda) → File → Settings → Build, Execution, Deployment → Build Tools → Gradle**.
   2. El campo se llama **Gradle JVM** (no "Gradle JDK"). Si aparece en rojo `GRADLE_LOCAL_JAVA_HOME`, abre la lista y elige **Download JDK…**, con **Version 17** y **Vendor Eclipse Temurin**. Se instala en `C:\Users\<usuario>\.jdks\temurin-17...`.
   3. Deja *Distribution: Wrapper* y *Build and run using: Gradle*. Pulsa **Apply → OK**.
5. En la pestaña **Build** (abajo), pulsa **🔄 Sync**.
   - Si sale el aviso **"Sync Android SDKs — The SDK path 'unset'…"**, pulsa **OK**. IntelliJ escribe la ruta del SDK en `local.properties`. Ese archivo es de cada computadora y **no se sube a git**.
   - La primera vez tarda unos 5–6 minutos: descarga Gradle 9.3.1 y las librerías.
   - Termina bien con ✅ **"android: finished"** y **BUILD SUCCESSFUL**.
   - El aviso *"New Minor Gradle Version Available"* es solo informativo.
6. Si IntelliJ ofrece **"Add files to Git"** o **"IDE project settings can be added to Git"**, pulsa **Cancel** o **Don't Ask Again**. Lo que se versiona ya lo decide `android/.gitignore`.

---

## C. Cómo se creó el proyecto (referencia, no repetir)

1. En la bienvenida: **New Project → Android** → plantilla **"Empty Views Activity"**.
   - ⚠️ **No uses "Empty Activity"**, que sale primero: esa es de **Kotlin + Compose**. Genera `MainActivity.kt` y `ui/theme/*.kt`, y hubo que borrarla.
   - Con la plantilla correcta sale `MainActivity.java` (extiende `AppCompatActivity`) y `res/layout/activity_main.xml`.
2. Datos del proyecto:

   | Campo | Valor |
   |---|---|
   | Name | `Facturas S20` |
   | Package name | `pe.facturass20` |
   | Save location | `C:\reposPersonal\Facturas_S20\android` |
   | Language | **Java** |
   | Minimum SDK | **API 26** (Android 8.0) |
   | Build configuration language | Kotlin DSL (`build.gradle.kts`) |

3. Durante la creación, a los avisos de "Add Files to Git" se les dio **Cancel**.
4. **Error al primer Run:** `AAPT: error: resource mipmap/ic_launcher ... not found`. La plantilla de IntelliJ no crea los íconos, así que se agregaron a mano (ya están en el repo):
   - `res/mipmap-anydpi-v26/ic_launcher.xml` e `ic_launcher_round.xml` (íconos adaptativos; con `minSdk 26` no hacen falta PNG)
   - `res/drawable/ic_launcher_foreground.xml`
   - `res/values/ic_launcher_background.xml` (`#1B4D45`)
5. Se creó `android/.gitignore`. Excluye `.gradle/`, `build/`, `local.properties`, `.idea/`, `*.iml`, las llaves de firma (`*.jks`, `*.keystore`) y el modelo (`*.litertlm`).

---

## D. Correr la app en el teléfono

1. En el teléfono, activa las opciones de desarrollador: **Ajustes → Acerca del teléfono →** toca **Número de compilación** 7 veces.
2. Activa **Ajustes → Sistema → Opciones de desarrollador → Depuración por USB**.
3. Conéctalo por USB (modo *Transferencia de archivos*) y acepta **"¿Permitir depuración USB?"** marcando *Permitir siempre*.
4. En IntelliJ, arriba, donde dice *No Devices*, elige el teléfono. Con la configuración **app** seleccionada, pulsa **Run ▶** (`Shift+F10`).
5. ✅ Resultado esperado: se instala **Facturas S20** y abre con **"Hello World!"**.

**Emulador (opcional):** **Tools → Android → Device Manager → + → Create Virtual Device**. Elige un Pixel con una imagen *Google APIs x86_64*. Sirve para la UI, pero no para medir Gemma: el modelo necesita un teléfono real con unos 6 GB de RAM.

---

## E. Compilar desde la terminal (sin IntelliJ)

El proyecto trae el **Gradle Wrapper** (`gradlew`, `gradlew.bat`), que la primera vez descarga solo Gradle 9.8.0. Gradle necesita `JAVA_HOME` apuntando al JDK 17 (desde Git Bash):

```bash
cd /c/reposPersonal/Facturas_S20/android
export JAVA_HOME=/c/Users/<usuario>/.jdks/temurin-17.0.20.1   # ajusta a tu versión
./gradlew :app:assembleDebug      # APK en app/build/outputs/apk/debug/app-debug.apk
./gradlew :dominio:test           # pruebas unitarias del dominio (JUnit 5)
./gradlew :app:installDebug       # instala en el teléfono conectado
```

En PowerShell o cmd se usa `gradlew.bat` en lugar de `./gradlew`.

---

## F. Configuración base del proyecto (hecha el 2026-09-27)

Todo compila con `./gradlew :app:assembleDebug :dominio:build` → **BUILD SUCCESSFUL**.

| Qué | Valor |
|---|---|
| Gradle (wrapper) | 9.8.0 |
| Android Gradle Plugin | **9.1.1**. No subir de 9.1.x: el plugin Android de IntelliJ 2026.2 solo soporta hasta AGP 9.1 (con 9.4.1 sale *"incompatible version (AGP 9.4.1)… Latest supported version is AGP 9.1.0"*). Revisar al actualizar IntelliJ. |
| Java | 17 en `:app` y `:dominio` |
| SDK | `compileSdk` 36.1 · `targetSdk` 36 · `minSdk` 26 |
| Versiones | todas en `android/gradle/libs.versions.toml` (se usan con `libs.<alias>`) |
| Módulos | `:app` (Android, `viewBinding` activo) y `:dominio` (`java-library`, Java puro, JUnit 5) |
| Paquetes | estructura de `entregable2_documentacion_tecnica.md` §3.3; cada paquete tiene un `package-info.java` que describe qué va en él |
| `MainActivity` | movida a `pe.facturass20.ui.comun` |
| `.gitattributes` | mantiene `gradlew` con finales LF para que funcione en Git Bash |

> **En IntelliJ, después de este cambio:** pulsa **🔄 Sync**. IntelliJ usa ahora el wrapper con Gradle 9.8.0, así que la primera vez vuelve a descargar. El aviso *"Deprecated Gradle features… setVisible"* viene de dentro del plugin de Android y se ignora.

Las dependencias de cada funcionalidad (Navigation, CameraX, Room + SQLCipher, WorkManager, LiteRT-LM) se agregan al catálogo cuando la iteración correspondiente las necesite. Ver `roadmap_cronograma_y_pendientes.md`.
