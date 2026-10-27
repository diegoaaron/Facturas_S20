---
name: 006_14_10_26_feedback_observaciones_docente_apf2
description: Observaciones de la revisión docente del informe del APF2 (devuelto 2026-10-08) y su corrección en los scripts el 2026-10-24, base para el APF3
metadata:
  type: feedback
---

La revisión docente está en `documentos_teoricos/entregable2/observaciones_profesor_entregable/revision_entregable2_.docx` (versión «solo APF2» pasada a Word desde PDF; marcas en amarillo/rojo, tachados en rojo, sin comentarios ni control de cambios). El detalle por página y las consultas están en `documentos_teoricos/entregable2/entregable2_observaciones_docente.pdf` (2026-10-14). En el mismo .docx hay anotaciones en rojo entre paréntesis que funcionan como respuestas del docente: «citar autores dentro de párrafo para fundamentar teóricamente», «estilo del pdf semana 4, 13» (normas APA del curso; ese PDF no está en el repo), «esto no va para este trabajo» (formulación del problema), «DEBE SER DIRECTO: Desarrollar una aplicación móvil con IA multimodal (Gemma 4) para mejorar la exactitud de la declaración mensual del NRUS en bodegas de S.J.L.» y «objetivos específicos directos y con una medición para cada objetivo». `apuntes_correciones_integrador.txt` no son correcciones: es la lista de lo que pide el avance 3 (informe, BD, anexos, APK, GitHub, diccionario de datos, manual, fuentes de variables, prototipos en Figma).

Observaciones: (1) citar autores en 1.1.1 a); (2) nota APA en tablas (Tabla 1 y catálogo de casos de uso «AUTOR»); (3) formulación del problema tachada; (4) objetivo general no acorde al título; (5) columna «Resultado esperado» de los OE tachada; (6–7) faltan autores en 2.1.8 y 2.1.9; (8) Figura 1 «revisar normas APA»; (9) «AUTORÍA» en el TO-BE. Sin marcas en Charter, Gantt, BPMN, clases, DER, prototipo ni validación.

**Corregido el 2026-10-24** en los scripts (`contenido_documento.py`, `construir_documento.py`, `construir_documento_solo.py`, `construir_presentacion*.py`) y regenerado con `generar_todo.py`:
- Citas APA 7 verificadas en 1.1.1 a) (INEI 2018, Loayza 2008, Silva Pérez y Soria Asencios 2024, DL 937, SUNAT), 2.1.7 (Dumas et al. 2018, OMG, ISO), 2.1.8 (Codd 1970, Chen 1976, Elmasri y Navathe 2016, Saltzer y Schroeder 1975, OWASP MASVS, SQLCipher), 2.1.9 (ISO 9241-210, Norman 2013, Nielsen 1994, W3C, Material Design, Brooke 1996), 3.4.5 (Sommerville 2016, ISO 29148) y TO-BE; todas en la bibliografía. Varias obras sin fecha del mismo autor llevan s. f.-a, s. f.-b…
- Tablas y figuras: «Tabla/Figura N» en negrita en su línea, título en cursiva debajo y nota debajo; las tablas sin nota reciben «Nota. Elaboración propia.» o la de `NOTAS_TABLAS`. Se hace al construir (`leyendas_apa`, `notas_tablas`), también sobre los bloques del entregable 1.
- Formulación del problema retirada (el docente escribió que no va); «Impacto del problema» pasa a ser 1.1.1 c).
- Objetivo general = texto que dictó el docente (coincide con el título de la portada). Tabla 10 «Objetivos específicos e indicadores de medición»: OE más cortos y un indicador sin metas; los valores se miden al final del proyecto. El Charter usa el mismo objetivo.
- Prueba de usabilidad con 3 titulares: semanas 11–12, iteración I4 (3.6.9, Tabla de verificación de RNF, conclusión cuarta y PPT); sin resultados. Retirada el 2026-10-27: en el APF3 hay que quitarla de esos apartados.

Pendiente o por decidir: los diagramas (Figura 1 y BPMN) aún llevan su título dentro de la imagen; no se quitó porque implica rehacer los SVG. Las metas cuantitativas siguen en los RNF, el Charter (criterio de éxito) y la tabla de indicadores del capítulo 5. Confirmar con el docente si el formato APA del «pdf semana 4, 13» pide algo más.

**Why:** son los criterios con los que se calificará el siguiente avance.
**How to apply:** el APF3 debe partir de estos scripts; no reintroducir la formulación del problema ni metas como resultados obtenidos. Ver [[005_27_09_26_proyecto_entregable2_apf2]].
