# Trae la base de datos de la app desde el teléfono conectado por USB para revisarla en DBeaver.
#
# La base del teléfono (databases/facturas.db) está cifrada con SQLCipher y su clave está protegida por el
# Android Keystore, así que no se puede abrir fuera de la app. Este script:
#   1. compila e instala la app y su APK de pruebas sin desinstalar (adb install -r; no se pierden datos ni modelo);
#   2. corre CopiaLegibleBaseTest, que dentro de la app exporta una copia SIN cifrar (files/facturas_legible.db);
#   3. trae a ..\base_datos\ la copia legible y el archivo cifrado original, y borra la copia legible del teléfono.
#
# Uso (PowerShell, con el teléfono desbloqueado y la depuración USB activa):
#   .\copiar_base.ps1              # compila, instala y copia
#   .\copiar_base.ps1 -SinCompilar # usa los APK ya compilados
param([switch]$SinCompilar)
$ErrorActionPreference = "Stop"

$raiz = Resolve-Path (Join-Path $PSScriptRoot "..\..\..\..")
$android = Join-Path $raiz "android"
$destino = Join-Path $PSScriptRoot "..\base_datos"
$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
$paquete = "pe.facturass20"

if (-not $SinCompilar) {
    $env:JAVA_HOME = (Get-ChildItem "$env:USERPROFILE\.jdks" -Directory -Filter "temurin-17*" | Select-Object -First 1).FullName
    & "$android\gradlew.bat" -p $android -q :app:assembleDebug :app:assembleDebugAndroidTest
    if ($LASTEXITCODE -ne 0) { throw "Falló la compilación" }
}
& $adb install -r "$android\app\build\outputs\apk\debug\app-debug.apk"
& $adb install -r "$android\app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk"
if ($LASTEXITCODE -ne 0) { throw "No se pudo instalar el APK de pruebas" }

$salida = & $adb shell am instrument -w -e class "$paquete.datos.CopiaLegibleBaseTest" "$paquete.test/androidx.test.runner.AndroidJUnitRunner"
$salida
if (-not ($salida -match "^OK \(1 test\)")) { throw "No se pudo generar la copia legible" }

New-Item -ItemType Directory -Force $destino | Out-Null
$destino = Resolve-Path $destino
# exec-out conserva los bytes tal cual; la redirección se hace en cmd porque la de PowerShell altera binarios.
cmd /c "`"$adb`" exec-out run-as $paquete cat files/facturas_legible.db > `"$destino\facturas_legible.db`""
cmd /c "`"$adb`" exec-out run-as $paquete cat databases/facturas.db > `"$destino\facturas_cifrada.db`""
& $adb shell run-as $paquete rm files/facturas_legible.db

Get-ChildItem $destino -Filter "*.db" | Format-Table Name, Length, LastWriteTime
