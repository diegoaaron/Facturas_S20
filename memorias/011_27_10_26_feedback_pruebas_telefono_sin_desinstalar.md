---
name: 011_27_10_26_feedback_pruebas_telefono_sin_desinstalar
description: no usar connectedDebugAndroidTest en el teléfono del usuario; instalar con adb install -r y correr pruebas sueltas con am instrument
metadata:
  type: feedback
---

En el Moto G34 del usuario la app tiene sus datos reales y el modelo afinado (5,3 GB). `./gradlew :app:connectedDebugAndroidTest` **desinstala la app al terminar** y borra ambos, y `FlujoPrincipalTest.escanearDispararYGuardarEnTresToques` guarda una factura de prueba en la base real. `GestorModeloLocalTest` **borra `files/modelos`** (el modelo instalado) al empezar y al terminar: no correrla en ese teléfono.

**Why:** el 2026-10-27, con una presentación ese día, se probó así para no perder los datos ni volver a descargar el modelo.

**How to apply:** compilar `:app:assembleDebug :app:assembleDebugAndroidTest`, instalar ambos APK con `adb install -r` y correr solo la prueba necesaria con `adb shell am instrument -w -e class 'pe.facturass20.ui.Clase#metodo' pe.facturass20.test/androidx.test.runner.AndroidJUnitRunner`. Evitar las que guardan datos salvo que el usuario lo apruebe. La pantalla debe estar encendida y desbloqueada (`adb shell svc power stayon usb` mientras dura, y `false` al terminar).
