# Imágenes del entregable 2 (APF2)

Figuras usadas en `../entregable2_documento.docx` y `../entregable2_presentacion.pptx`.

- `*.png`: imágenes que se insertan en el Word y la PPT (a 2× de resolución).
- `fuentes/*.svg`: la misma figura en vectorial, editable en Inkscape, Figma o Penpot (las pantallas se pueden importar a Figma para armar el prototipo navegable). `fuentes/utp/logo_utp.png` es el logo de la portada.
- `scripts/`: código que genera todo. Si cambias un texto o un dato, edítalo en el script y regenera; así la figura, el Word y la PPT quedan coherentes.
- `e1_*.png`: figuras del entregable 1 que se reutilizan (casos de uso, secuencia, pantallas de las alternativas 2 y 3, WBS). No tienen fuente editable.

## Contenido

| Archivo | Qué es | Dónde se usa |
|---|---|---|
| `bpmn_00_notacion` | Notación BPMN 2.0 con ejemplos | Word 3.5.2 |
| `bpmn_01_asis` / `bpmn_02_tobe` | Proceso actual y propuesto | Word 3.5.3 · PPT 4–5 |
| `bpmn_03_registro` | Subproceso «Registrar factura con IA» | Word 3.5.3 · PPT 6 |
| `bpmn_04_cierre` / `bpmn_05_modelo` | Cierre mensual; publicación del modelo | Word Anexo K |
| `arquitectura_capas` | Arquitectura por capas | Word 3.5.1 · PPT 7 |
| `arquitectura_despliegue` | Diagrama de despliegue UML | Word 3.5.9 |
| `clases_dominio` / `clases_diseno` | Diagramas de clases | Word 3.5.4 · PPT 8 |
| `componentes_java` | Componentes y % de Java | Word 3.5.10 |
| `der_local` / `der_servidor` | Diagramas entidad-relación | Word 3.5.5 · PPT 9 |
| `ui_sistema_diseno` / `ui_mapa_navegacion` | Guía de estilo y navegación | Word 3.5.7, 3.6.2 · PPT 10 |
| `pantalla_01` … `pantalla_19` | Pantallas móviles P01–P19 | Word 3.6 · PPT 11–12 |
| `pantalla_20_consola_admin` | Consola web del administrador | Word 3.6.8 · PPT 12 |
| `prototipo_hoja_1` … `_5` | Pantallas agrupadas por módulo | Word 3.6.5 |
| `reporte_mensual_pdf` | Página 1 del reporte mensual | Word 3.6.8 · PPT 12 |
| `gantt_actualizado` | Cronograma al corte de la semana 8 | Word 4.1 · PPT 14 |

## Regenerar

Requisitos (Windows): Python 3 con `lxml`, `Pillow` y `python-pptx`; Google Chrome (convierte SVG a PNG en modo headless); Microsoft Word (actualiza el índice).

```bash
cd scripts
python generar_todo.py            # figuras + Word + índice + PPT
python generar_todo.py figuras    # solo figuras
python construir_documento.py     # solo el Word (luego: powershell -File actualizar_indice.ps1)
python construir_presentacion.py  # solo la PPT
```

| Script | Genera |
|---|---|
| `svg.py` | Utilidades de dibujo y conversión SVG → PNG |
| `diagramas_bpmn.py` | `bpmn_*` |
| `diagramas_uml.py` | arquitectura, despliegue, clases, componentes, DER, Gantt |
| `pantallas.py` | pantallas, hojas del prototipo, consola, reporte, sistema de diseño, mapa de navegación |
| `docx_xml.py` + `contenido_documento.py` + `construir_documento.py` | `../entregable2_documento.docx`, construido sobre `../entregable1_documento.docx` |
| `construir_presentacion.py` | `../entregable2_presentacion.pptx` |
| `actualizar_indice.ps1` | Abre el Word, regenera el índice y lo guarda |

> Si editas un SVG a mano, el PNG no se actualiza solo: expórtalo desde tu editor con el mismo nombre, o vuelve a correr el script (que sobrescribe el SVG). Si modificas `entregable2_documento.docx` directamente en Word, esos cambios se pierden al regenerarlo desde el script.
