# Archivos del entregable 2 (APF2)

Figuras usadas en `../entregable2_documento.docx` (informe completo), `../solo_entregable2_documento.docx` (solo el APF2) `../entregable2_presentacion.pptx` y `../solo_entregable2_presentacion.pptx` (sustentación alineada con el Word solo APF2).

Diseño vigente: app Android de un solo usuario, **sin servidor**. Toda la data vive en una base SQLite cifrada en el teléfono y el usuario la exporta a CSV/PDF para analizarla en una PC (pantalla P20). Respecto del primer avance se retiraron el servidor de respaldo, su base Oracle/SQL Server, el actor Administrador y la consola web.

- `*.png`: imágenes que se insertan en el Word y la PPT (a 2× de resolución).
- `fuentes/*.svg`: la misma figura en vectorial, editable en Inkscape, Figma o Penpot (las pantallas se pueden importar a Figma para armar el prototipo navegable). `fuentes/utp/logo_utp.png` es el logo de la portada.
- `scripts/`: código que genera todo. Si cambias un texto o un dato, edítalo en el script y regenera; así la figura, el Word y la PPT quedan coherentes.
- `e1_*.png`: figuras del entregable 1 que se reutilizan (casos de uso, secuencia, pantallas de las alternativas 2 y 3, WBS). No tienen fuente editable. `casos_uso.png` y `wbs.png` son `e1_casos_uso.png` y `e1_wbs.png` ajustadas al diseño sin servidor por `scripts/ajustar_figuras_e1.py`.

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
| `der_local` | Diagrama entidad-relación de la base SQLite | Word 3.5.5 · PPT 9 |
| `ui_sistema_diseno` / `ui_mapa_navegacion` | Guía de estilo y navegación | Word 3.5.7, 3.6.2 · PPT 10 |
| `pantalla_01` … `pantalla_20` | Pantallas móviles P01–P20 (P20: exportar datos) | Word 3.6 · PPT 11–12 |
| `prototipo_hoja_1` … `_5` | Pantallas agrupadas por módulo | Word 3.6.5 |
| `reporte_mensual_pdf` | Página 1 del reporte mensual | Word 3.6.8 · PPT 12 |
| `gantt_actualizado` | Cronograma al corte de la semana 8 | Word 4.1 · PPT 14 |
| `casos_uso` / `wbs` | Figuras del primer avance ajustadas (sin Administrador; 5.7 Exportación de datos) | Word 3.4.3 y Anexo C |

## Regenerar

Requisitos (Windows): Python 3 con `lxml`, `Pillow` y `python-pptx` (`pip install lxml Pillow python-pptx`); Google Chrome (convierte SVG a PNG en modo headless); Microsoft Word (actualiza el índice).

```bash
cd scripts
python generar_todo.py            # figuras + Word completo + Word solo APF2 (con sus índices) + las dos PPT
python generar_todo.py figuras    # solo figuras
python construir_documento.py     # solo el Word completo (luego: powershell -File actualizar_indice.ps1)
python construir_documento_solo.py  # Word solo APF2 (luego: powershell -File actualizar_indice.ps1 -Docx solo_entregable2_documento.docx)
python construir_presentacion.py  # solo la PPT
python construir_presentacion_solo.py  # PPT solo APF2
```

| Script | Genera |
|---|---|
| `svg.py` | Utilidades de dibujo y conversión SVG → PNG |
| `diagramas_bpmn.py` | `bpmn_*` |
| `diagramas_uml.py` | arquitectura, despliegue, clases, componentes, DER, Gantt |
| `pantallas.py` | pantallas, hojas del prototipo, reporte, sistema de diseño, mapa de navegación |
| `ajustar_figuras_e1.py` | `casos_uso.png` y `wbs.png` a partir de las figuras del entregable 1 |
| `docx_xml.py` + `contenido_documento.py` + `construir_documento.py` | `../entregable2_documento.docx`, construido sobre `../../entregable1/entregable1_documento.docx` |
| `construir_documento_solo.py` | `../solo_entregable2_documento.docx`: solo los apartados del APF2, elegidos por título a partir del informe completo |
| `construir_presentacion.py` | `../entregable2_presentacion.pptx` |
| `construir_presentacion_solo.py` | `../solo_entregable2_presentacion.pptx`: la PPT anterior más objetivos y alcance, actores y casos de uso, resultados y conclusiones del Word solo APF2 |
| `actualizar_indice.ps1` | Abre el Word (`-Docx` para elegir cuál), regenera el índice y lo guarda |

> Si editas un SVG a mano, el PNG no se actualiza solo: expórtalo desde tu editor con el mismo nombre, o vuelve a correr el script (que sobrescribe el SVG). Si modificas `entregable2_documento.docx` directamente en Word, esos cambios se pierden al regenerarlo desde el script.
