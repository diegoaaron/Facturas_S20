"""Construye documentos_teoricos/entregable2/solo_entregable2_presentacion.pptx (sustentación del informe «solo APF2»).

Parte de la presentación de construir_presentacion.py (mismas diapositivas, estilo y expositores) y la alinea con
solo_entregable2_documento.docx: agrega los objetivos y el alcance (1.2), el análisis con actores y casos de uso (3.4),
los resultados (Cap. 5) y las conclusiones, corrige la cobertura de la alternativa 2 según la Tabla 17 y remite solo a
los anexos que ese informe incluye. Uso:  python construir_presentacion_solo.py
"""
import os

import construir_presentacion as cp
from construir_presentacion import (AZUL, BLANCO, FONDO_CLARO, GRIS, ROJO, ROSA, VERDE, VERDE_CLARO, MSO_ANCHOR,
                                    PP_ALIGN, caja_texto, cifra, diapositiva, imagen, rect, tabla, tarjeta)

SALIDA = os.path.join(os.path.dirname(cp.IMG), "solo_entregable2_presentacion.pptx")


# ---------------------------------------------------------------- utilidades
def ultima():
    return cp.prs.slides[len(cp.prs.slides) - 1]


def reemplazar(slide, viejo, nuevo):
    """Cambia un texto que está dentro de un solo run de la diapositiva (debe aparecer una vez)."""
    runs = [r for sh in slide.shapes if sh.has_text_frame for p in sh.text_frame.paragraphs for r in p.runs
            if viejo in r.text]
    if len(runs) != 1:
        raise SystemExit(f"«{viejo}» aparece {len(runs)} veces")
    runs[0].text = runs[0].text.replace(viejo, nuevo)


def notas(slide, texto):
    slide.notes_slide.notes_text_frame.text = texto


# ---------------------------------------------------------------- diapositivas nuevas
def objetivos():
    s = diapositiva("Objetivos y alcance", "Objetivo general, siete objetivos específicos y límites", "diego",
                    "Diego: El objetivo general es desarrollar Facturas S20, una app Android en Java que registre las facturas "
                    "por foto con Gemma 4 ajustado y ejecutado en el teléfono, y que calcule el total del mes y la categoría del "
                    "NRUS. Lo desglosamos en siete objetivos específicos, cada uno con un resultado medible: del modelado de "
                    "procesos a la validación en un mes simulado. A la derecha, lo que queda fuera: la app calcula y prepara, "
                    "pero la declaración se presenta por los canales de la SUNAT.")
    rect(s, 0.55, 1.45, 8.3, 1.1, AZUL)
    caja_texto(s, 0.75, 1.5, 7.9, 1.0, [[("Objetivo general: ", True), (
        "app Android nativa en Java que registra las facturas de compra por fotografía con Gemma 4 E2B ajustado y "
        "ejecutado en el teléfono, sin conexión, y calcula el total mensual y la categoría del NRUS.", False)]],
               size=13, color=BLANCO, anchor=MSO_ANCHOR.MIDDLE)
    filas = [["Cód.", "Objetivo específico", "Resultado esperado"],
             ["OE1", "Modelar el proceso actual y el propuesto", "AS-IS y TO-BE con puntos de error"],
             ["OE2", "Conjunto de facturas peruanas etiquetadas", "Entrenamiento, validación y prueba"],
             ["OE3", "Ajuste fino de Gemma 4 E2B y conversión a .litertlm", "Mejor que el modelo base tras convertir"],
             ["OE4", "Motor de reglas del NRUS", "Verificado con casos de la norma"],
             ["OE5", "App Android en Java con LiteRT-LM", "Sin conexión, ≥ 50 % de Java"],
             ["OE6", "Exportación a CSV, imágenes y PDF", "Cero envíos automáticos de datos"],
             ["OE7", "Validar en un mes simulado", "≥ 90 % por campo · ≤ 20 s · < 2 % en el total"]]
    tabla(s, 0.55, 2.75, 8.3, filas, [0.7, 3.9, 3.7], size=11.5, alto_fila=0.5)
    tarjeta(s, 9.1, 1.45, 3.7, 3.0, "Fuera del alcance", ["Presentar y pagar ante la SUNAT", "Registrar cada venta diaria",
                                                          "Emitir comprobantes electrónicos", "Contabilidad, inventario o planillas",
                                                          "Otros regímenes e iOS"], color=ROJO, size=13, fondo=ROSA)
    tarjeta(s, 9.1, 4.6, 3.7, 2.15, "Delimitación", ["Bodegas del NRUS con un solo local", "Validación en San Juan de Lurigancho",
                                                     "Teléfono con < 6 GB: registro manual", "Agosto a diciembre de 2026"],
            color=VERDE, size=12.5)


def analisis():
    s = diapositiva("Análisis de la solución", "Actores, 12 casos de uso y requerimientos", "diego",
                    "Diego: El análisis del primer avance se afinó con los procesos BPMN: cada tarea que ejecuta la app "
                    "corresponde a un caso de uso y a uno o más requerimientos. Hay dos actores persona, el bodeguero y el "
                    "contador externo, que solo recibe el reporte, y dos actores sistema: el motor de extracción y el motor de "
                    "reglas. Se retiró el Administrador porque ya no hay servidor. Son 12 casos de uso, 22 requerimientos "
                    "funcionales y 19 no funcionales según ISO/IEC 25010. Paso la palabra a Paulo.")
    imagen(s, "casos_uso.png", 0.45, 1.45, 6.0, 5.45, alinear="medio")
    filas = [["Actor", "Tipo", "Rol"], ["Bodeguero", "Persona", "Registra facturas y consulta el mes"],
             ["Contador externo", "Persona", "Recibe el reporte o los datos exportados"],
             ["Motor de extracción", "Sistema", "Gemma 4 E2B con LiteRT-LM"],
             ["Motor de reglas NRUS", "Sistema", "Categoría, cuota, topes y vencimiento"]]
    tabla(s, 6.75, 1.45, 6.05, filas, [1.9, 0.9, 3.25], size=11.5, alto_fila=0.48)
    datos = [("12", "casos de uso", AZUL), ("22", "RF: 13 alta, 8 media, 1 baja", AZUL), ("19", "RNF con métrica ISO 25010", VERDE)]
    for i, (v, e, c) in enumerate(datos):
        x = 6.75 + i * 2.07
        rect(s, x, 4.05, 1.91, 1.45, FONDO_CLARO)
        cifra(s, x, 4.15, 1.91, v, e, color=c, size=30)
    rect(s, 6.75, 5.7, 6.05, 1.2, VERDE_CLARO)
    caja_texto(s, 6.95, 5.75, 5.65, 1.1, "Respecto del primer avance se retiró el actor Administrador: sin servidor ni consola "
               "no hay nada que administrar fuera del teléfono.", size=12.5, color=VERDE, anchor=MSO_ANCHOR.MIDDLE)


def resultados():
    s = diapositiva("Resultados", "Entregables del APF2 completos y metas por medir", "diego",
                    "Diego: Los resultados de este avance son los entregables de diseño que pide la consigna, y los nueve están "
                    "completos. No adelantamos cifras que aún no medimos: la exactitud del modelo convertido y el tiempo de lectura "
                    "se medirán en APF3, y el total mensual y la categoría en el mes simulado del informe final. El prototipo de la "
                    "competencia Build with Gemma ya confirmó que el modelo ajustado extrae los campos en JSON.")
    filas = [["Criterio de la consigna", "Entregable", "Ubicación"],
             ["Análisis del contexto", "Contexto, canvas e impacto en procesos", "Cap. 1, An. A"],
             ["Alternativas", "3 alternativas profundizadas", "3.2, An. F"],
             ["Análisis", "Actores, 12 CU, 22 RF y 19 RNF", "3.4, An. H e I"],
             ["Diseño", "Charter, Gantt, 5 BPMN, 2 clases, DER", "3.5, An. B, D, K–N"],
             ["Base de datos", "SQLite lógico y físico con seguridad", "3.5.5–3.5.6, An. M"],
             ["Prototipo", "20 pantallas y reporte PDF", "3.6, An. O–Q"],
             ["Validación", "Matrices de cobertura y verificación", "3.7, An. R y S"],
             ["Cronograma y presupuesto", "Gantt, presupuesto, recursos, costos", "Cap. 4"],
             ["Documentación técnica", "Markdown para desarrolladores", "An. T"]]
    tabla(s, 0.55, 1.45, 6.7, filas, [2.0, 3.0, 1.7], size=11, alto_fila=0.5)
    caja_texto(s, 0.55, 6.55, 6.7, 0.3, "Los nueve entregables están completos.", size=12, color=VERDE, bold=True)
    filas = [["Indicador", "Línea base", "Meta", "Cuándo"],
             ["Registro por factura", "1–2 min", "≤ 20 s", "APF3"],
             ["Cálculo mensual", "3 h", "Inmediato", "Final"],
             ["Facturas conservadas", "64 %", "100 %", "Final"],
             ["Exactitud por campo", "No medida", "≥ 90 %", "APF3"],
             ["Exactitud tras convertir", "No medida", "Sin pérdida", "APF3"],
             ["Diferencia del total", "Desconocida", "< 2 %", "Final"],
             ["Categoría correcta", "No (cat. 2)", "Sí", "Final"]]
    tabla(s, 7.5, 1.45, 5.3, filas, [2.1, 1.2, 1.1, 0.9], size=11, alto_fila=0.54, resaltar=None)
    rect(s, 7.5, 5.95, 5.3, 0.95, FONDO_CLARO)
    caja_texto(s, 7.65, 5.95, 5.0, 0.95, "El prototipo de Build with Gemma confirmó que el modelo ajustado extrae los "
               "campos en JSON; falta medirlo cuantizado en el teléfono.", size=11.5, color=GRIS, anchor=MSO_ANCHOR.MIDDLE)


def conclusiones():
    s = diapositiva("Conclusiones", "Lo que demuestra el diseño", "diego",
                    "Diego: Cinco conclusiones. El problema es operativo y el proceso propuesto elimina sus tres puntos de error. "
                    "El diseño cubre el 100 % del alcance. Todo vive en una sola base cifrada en el teléfono y el titular exporta "
                    "sus datos cuando quiere. El registro toma tres toques y se validará con usuarios en la iteración 3. Y la "
                    "solución mantiene 85 % de Java sin costo mensual de operación.")
    items = [("1 · Problema operativo", "Los tres puntos de error del AS-IS —guardar sin registrar, sumar a mano y estimar "
              "ante la duda— se eliminan o automatizan en el TO-BE.", AZUL),
             ("2 · Alcance cubierto", "Los 22 RF tienen pantalla, caso de uso, clase y tabla; los 19 RNF, una decisión "
              "verificable.", VERDE),
             ("3 · Datos en el teléfono", "Una sola base SQLite cifrada; sin servidor de respaldo. El titular exporta a CSV y "
              "PDF cuando quiere.", AZUL),
             ("4 · Tres toques", "El registro de una factura toma tres toques desde el inicio; se validará con usuarios "
              "en la iteración I3.", VERDE),
             ("5 · Java y costo", "Alrededor de 85 % de código Java y sin costo de operación mensual: no hay servidor "
              "que mantener.", AZUL)]
    for i, (t, c, col) in enumerate(items):
        x, y = 0.55 + (i % 3) * 4.15, 1.6 + (i // 3) * 2.65
        tarjeta(s, x, y, 3.95, 2.35, t, c, color=col, size=14.5)
    rect(s, 8.85, 4.25, 3.95, 2.35, FONDO_CLARO)
    cifra(s, 8.85, 4.45, 1.97, "100 %", "del alcance funcional", color=VERDE, size=28)
    cifra(s, 10.83, 4.45, 1.97, "S/ 0", "de operación al mes", color=AZUL, size=28)
    caja_texto(s, 9.0, 5.7, 3.65, 0.8, "La prueba con tres titulares y la exactitud del modelo se miden en APF3.",
               size=11.5, color=GRIS, align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)


# ---------------------------------------------------------------- armado
def main():
    cp.portada()
    notas(ultima(), "Diego: Buenos días. Somos el equipo de Facturas S20. En el primer avance analizamos el problema y "
          "elegimos la solución; hoy presentamos su diseño: contexto y objetivos, alternativas y análisis, procesos BPMN, "
          "arquitectura, clases, base de datos, prototipo, validación, cronograma, presupuesto y resultados. Cada integrante "
          "expone una parte.")
    cp.contexto()
    objetivos()
    cp.alternativas()
    reemplazar(ultima(), "RF 22/22 · RNF 16/19", "RF parcial · RNF 16/19")
    notas(ultima(), "Diego: Profundizamos las tres alternativas con los seis aspectos que pide la consigna. Las tres tienen al "
          "menos cinco pantallas y más de 50 % de Java. La diferencia es dónde se lee la factura. Solo la alternativa 1 cubre "
          "los 22 requerimientos funcionales y los 19 no funcionales, porque funciona sin conexión y la imagen no sale del "
          "teléfono; la 2 no instala el modelo y la 3 usa un modelo cerrado. Obtuvo 4,40 sobre 5 en la matriz ponderada.")
    analisis()
    cp.asis(); cp.tobe(); cp.registro()
    cp.arquitectura(); cp.clases(); cp.base_datos()
    cp.ux(); cp.prototipo_flujo(); cp.prototipo_modulos(); cp.validacion()
    cp.cronograma()
    resultados()
    conclusiones()
    cp.cierre()
    s = ultima()
    reemplazar(s, "Anexos del informe: A–J evidencias del primer avance", "Anexos del informe: A, B, D, F, H e I evidencias del primer avance")
    notas(s, "Diego: Por último, entregamos la documentación técnica en Markdown, pensada para versionarse con el código: "
          "arquitectura, contratos, esquema de datos, reglas del NRUS, pantallas, exportación de datos y plan de pruebas. Con "
          "ella empieza la construcción hacia APF3: el front-end y el dominio, el ajuste fino con conversión a .litertlm y la "
          "medición en el teléfono de referencia. Gracias, quedamos atentos a sus preguntas.")
    cp.prs.core_properties.title = "APF2 (solo) - Sustentación — Facturas S20"
    cp.prs.save(SALIDA)
    print("  guardado", SALIDA, f"({cp.numero[0]} diapositivas)")


if __name__ == "__main__":
    main()
