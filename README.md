# Facturas S20

Aplicación móvil con IA multimodal (Gemma 4) para mejorar la exactitud de la declaración mensual del NRUS en bodegas de S.J.L.

Facturas S20 es una **app Android nativa en Java**. El titular de una bodega fotografía sus facturas de compra y un modelo **Gemma 4 E2B ajustado**, que corre **dentro del teléfono**, lee los datos. La app suma las compras y las ventas del mes, determina la categoría del NRUS y recuerda la fecha de vencimiento.

- **Sin conexión:** la lectura se hace en el teléfono. Solo se necesita internet una vez, para descargar el modelo.
- **Sin servidor:** los datos viven en el teléfono (SQLite cifrada) y se pueden exportar a CSV y PDF.
- **Registro manual:** para teléfonos que no pueden ejecutar el modelo.

> Proyecto del curso Integrador I: Sistemas Software (UTP, 2026). En desarrollo.

## Estructura del repositorio

| Carpeta | Contenido |
|---|---|
| `android/` | Proyecto Gradle de la app: módulos `:app` (Android) y `:dominio` (Java puro) |
| `tunning_gemma4/` | Notebook de Google Colab para el ajuste fino del modelo |
| `roadmap/` | Documentación técnica, plan del proyecto y configuración del equipo |
| `documentos_teoricos/` | Informes y presentaciones de los avances del curso |

## Cómo empezar

1. Prepara el entorno (IntelliJ IDEA, Android SDK y JDK 17) siguiendo la Parte 2 de [`roadmap/002_27_09_26_configuracion_equipo.md`](roadmap/002_27_09_26_configuracion_equipo.md).
2. En IntelliJ, abre la carpeta **`android/`**.
3. Conecta un teléfono con la depuración USB activada y pulsa **Run ▶**.

Desde la terminal:

```bash
cd android
./gradlew :app:assembleDebug     # compila el APK
./gradlew :dominio:test          # pruebas del dominio
./gradlew :app:installDebug      # instala en el teléfono conectado
```

## Documentación

- [Documentación técnica y roadmap](roadmap/001_27_09_26_documentacion_tecnica_y_roadmap.md): arquitectura, reglas del NRUS, base de datos, pantallas y plan por iteraciones.

## Equipo

- Damián Valdivia, Diego Aarón
- Coronel Obregón, Paulo
- Mateo Velásquez, Morán
- Gonzales Maco, Fabrizzio Jhoel

## Licencia

[Apache 2.0](LICENSE)
