"""Contenido del informe del entregable 2 (APF2).

`escribir(d)` recorre el informe en orden. `d.e1(i, j)` reutiliza los bloques i..j del cuerpo del
entregable 1 (índices de sus elementos de primer nivel: ver construir_documento.py); el resto es
contenido nuevo. `{ref:clave}` se sustituye por el número de la tabla o figura con esa clave.
"""
import docx_xml as D
from docx_xml import h1, h2, h3, p, subtitulo, vinetas, codigo, nota

IA = "IA"

# Título oficial del proyecto (portada del Word, Project Charter y portada del PPT).
TITULO = ("Aplicación móvil con IA multimodal (Gemma 4) para mejorar la exactitud de la declaración mensual del NRUS "
          "en bodegas de S.J.L.")
# Títulos del entregable 1 que se reemplazan por TITULO.
TITULO_E1_PORTADA = ("Facturas S20: aplicación móvil con IA multimodal (Gemma 4) embebida en el teléfono para mejorar la "
                     "exactitud de la declaración mensual del NRUS en bodegas de San Juan de Lurigancho")
TITULO_E1_CHARTER = "Facturas S20: aplicación móvil con Gemma 4 embebido para la declaración del NRUS"


def escribir(d):
    portada_e_introduccion(d)
    capitulo1(d)
    capitulo2(d)
    capitulo3(d)
    capitulo4(d)
    capitulo5_y_conclusiones(d)
    anexos(d)
    bibliografia(d)


# ======================================================================
def portada_e_introduccion(d):
    d.e1(0, 3, reemplazos=[(TITULO_E1_PORTADA, TITULO)])
    d.e1(4, reemplazos=[("Avance de Proyecto Final 1 (APF1)", "Avance de Proyecto Final 2 (APF2)")])
    d.e1(5, 21)
    d.add(p("Este informe corresponde al segundo avance del proyecto final del curso. El segundo avance no reemplaza al "
            "primero: lo amplía, lo profundiza y lo completa. Conserva el análisis del contexto, las alternativas y la "
            "especificación de requerimientos aprobados en el primer avance, y agrega el diseño de la solución informática: "
            "los procesos de negocio modelados con BPMN, el diseño lógico y físico de la base de datos, los diagramas de "
            "clases, el prototipo de la interfaz y de los reportes, la validación del diseño frente al alcance, el cronograma "
            "actualizado con su presupuesto y la documentación técnica para los desarrolladores. Respecto del primer avance se "
            "retiró el servidor de respaldo opcional: toda la información permanece en el teléfono y el usuario la exporta "
            "cuando la necesita."))
    d.e1(23, 24)
    d.add(p("El informe sigue la estructura del proyecto integrador. El Capítulo 1 presenta el contexto de la empresa cliente, "
            "define el problema y cómo afecta sus procesos, y establece los objetivos, el alcance, la justificación y el estado "
            "del arte. El Capítulo 2 desarrolla el fundamento teórico, incluida la notación BPMN, el diseño de bases de datos "
            "seguras y el diseño de experiencia de usuario. El Capítulo 3 desarrolla la solución: metodología, alternativas, "
            "gestión del proyecto, análisis, diseño detallado, diseño del prototipo y validación. El Capítulo 4 presenta el "
            "cronograma actualizado y el presupuesto. El Capítulo 5 reporta los resultados del avance. Los anexos A a J "
            "reúnen las evidencias del primer avance y los anexos K a T las del segundo."))


# ======================================================================
def capitulo1(d):
    d.e1(26, 73)
    d.add(subtitulo("d) Impacto del problema en los procesos de la empresa y alcance de la solución informática"))
    d.add(p("El problema no afecta a un proceso aislado, sino a la cadena que va desde que llega la mercadería hasta que se paga "
            "la cuota. El modelado del proceso actual con BPMN (apartado 3.5.3) permite ubicar con precisión dónde se origina "
            "cada consecuencia y, por tanto, qué debe cubrir la solución. La Tabla {ref:t_impacto} relaciona cada proceso "
            "de la bodega con el efecto que el problema produce en él y con el indicador con el que se medirá la mejora."))
    d.tabla("t_impacto", "Procesos de la empresa afectados por el problema",
            ["Proceso de la bodega", "Cómo lo afecta el problema", "Indicador", "Qué hace la solución"],
            [["Recepción de mercadería", "La factura se recibe con el cliente esperando y se guarda sin registrar.",
              "Facturas registradas al recibirlas", "Registro por foto en ≈ 10 s en el mostrador."],
             ["Custodia de comprobantes", "Las facturas se arrugan o se pierden (5 de 14 en el caso de referencia).",
              "Facturas conservadas al cierre", "Imagen cifrada de cada factura en el teléfono."],
             ["Cálculo mensual de compras", "Se suma a mano una vez al mes; toma unas 3 horas y produce errores.",
              "Tiempo de cálculo y diferencia con el control", "Acumulado automático actualizado con cada factura."],
             ["Determinación de la categoría", "Ante la duda se declara la categoría superior o se subdeclara.",
              "Categoría correcta en el mes", "Motor de reglas del NRUS con avisos al 80 % y 100 %."],
             ["Declaración y pago", "Se posterga al último día y sin sustento documental.",
              "Pago a tiempo y con reporte", "Recordatorio según el RUC y reporte PDF del mes."]],
            [2.2, 3.2, 2.2, 3.0])
    d.add(p("De ese análisis se deriva el alcance de la solución informática en seis módulos, que son los que se diseñan en este "
            "avance. La Tabla {ref:t_modulos} los resume junto con los requerimientos funcionales que agrupan; la cobertura "
            "detallada se verifica en el apartado 3.7."))
    d.tabla("t_modulos", "Alcance de la solución informática por módulo",
            ["Módulo", "Procesos que cubre", "Requerimientos", "Pantallas"],
            [["Acceso y configuración", "Alta del contribuyente, acceso seguro, preparación del modelo", "RF-19, RF-20", "P01–P03"],
             ["Registro de facturas", "Captura, lectura con IA en el teléfono, verificación, duplicados", "RF-01 a RF-07", "P05–P10, P19"],
             ["Control de facturas", "Consulta del mes, detalle y anulación", "RF-08, RF-18", "P11–P12"],
             ["Determinación del NRUS", "Ventas, categoría, cuota, avisos y vencimiento", "RF-09 a RF-14, RF-22", "P04, P13–P15"],
             ["Reportes e historial", "Reporte PDF, cierre del mes, histórico", "RF-15 a RF-17", "P16–P17"],
             ["Exportación y modelo", "Exportación de datos para PC, instalación y actualización del modelo, parámetros vigentes", "RF-20 a RF-22", "P03, P18, P20"]],
            [2.3, 4.0, 2.0, 1.6])
    d.e1(74, 125, reemplazos=[
        ("Desarrollar un servicio de respaldo en Java sobre Spring Boot, con persistencia en Oracle o SQL Server, que conserve "
         "el histórico de forma opcional y publique los parámetros del NRUS y las versiones del modelo.",
         "Implementar en Java la exportación de los datos registrados a archivos abiertos (CSV, imágenes y PDF) para "
         "analizarlos en una PC, y la actualización de los parámetros del NRUS con cada versión de la app, sin que ningún "
         "dato salga del teléfono sin la acción del titular."),
        ("Servicio REST desplegado y documentado, con base de datos relacional operativa.",
         "Exportación a CSV y ZIP verificada en una hoja de cálculo; cero envíos automáticos de datos."),
        ("Instalación y actualización del modelo en el teléfono, y respaldo opcional cifrado en un servidor propio.",
         "Instalación y actualización del modelo en el teléfono, y exportación de los datos a archivos para analizarlos en "
         "una PC. Respecto del primer avance se retiró el servidor de respaldo opcional: toda la información permanece en "
         "el teléfono."),
        ("; servicio de respaldo en Java con Spring Boot y Oracle o SQL Server.",
         "; sin servidor propio: la única conexión es la descarga del modelo.")])


# ======================================================================
def capitulo2(d):
    d.e1(126, 162, reemplazos=[
        ("El servidor en Java con Spring Boot es opcional y no participa en la lectura de facturas: guarda respaldos, "
         "publica los parámetros del NRUS y distribuye las versiones del",
         "No hay servidor: la lectura, el cálculo y la exportación de datos ocurren en el teléfono, y la red solo se usa "
         "para descargar una vez el archivo del")])
    d.add(h3("2.1.7. Modelado de procesos de negocio con BPMN"))
    d.add(p("La gestión de procesos de negocio (BPM, *Business Process Management*) es la disciplina que identifica, modela, "
            "analiza y mejora los procesos de una organización. Su herramienta de modelado estándar es BPMN (*Business Process "
            "Model and Notation*), cuya versión 2.0.2 publica el Object Management Group y fue adoptada como la norma "
            "ISO/IEC 19510. BPMN define una notación gráfica común para el negocio y para los desarrolladores, de modo que el "
            "mismo diagrama sirve para discutir el problema con el bodeguero y para derivar los requerimientos del software."))
    d.add(p("Los elementos de BPMN se agrupan en cuatro categorías. Los objetos de flujo son los eventos (inicio, intermedio y "
            "fin, que pueden ser de mensaje o de temporizador), las actividades (tareas y subprocesos) y las compuertas "
            "(exclusiva, paralela, inclusiva). Los objetos de conexión son el flujo de secuencia, el flujo de mensaje entre "
            "participantes y la asociación. Los contenedores son los *pools*, que representan a cada participante, y los "
            "carriles, que separan responsabilidades dentro de él. Los artefactos son los objetos de datos, los almacenes de "
            "datos y las anotaciones. El proyecto modela primero el proceso actual (AS-IS) para ubicar los puntos de error y "
            "luego el proceso propuesto (TO-BE) que implementa la aplicación."))
    d.add(h3("2.1.8. Diseño de bases de datos y seguridad de la información"))
    d.add(p("El diseño de una base de datos relacional avanza en tres niveles. El modelo conceptual identifica las entidades y "
            "sus relaciones, independientemente de la tecnología; el modelo lógico las traduce a tablas con claves primarias y "
            "foráneas normalizadas, al menos hasta la tercera forma normal, para evitar redundancias y anomalías de "
            "actualización; y el modelo físico fija los tipos de datos, las restricciones, los índices y el motor concreto. El "
            "diagrama entidad-relación con notación de pata de gallo expresa la cardinalidad mínima y máxima de cada relación."))
    d.add(p("La seguridad se diseña junto con el esquema y no después. Los principios aplicados son el de mínimo privilegio "
            "(el usuario de la aplicación solo tiene los permisos que necesita), la defensa en profundidad (cifrado en tránsito "
            "con TLS y en reposo con AES-256), la minimización de datos (no se guarda lo que no se usa), la integridad "
            "verificable (huellas SHA-256 del modelo y de los reportes), el control de acceso con PIN o huella y la prevención "
            "de inyección SQL mediante consultas parametrizadas, que Room genera por defecto. En el teléfono se siguen las "
            "recomendaciones del estándar OWASP MASVS para el almacenamiento seguro en dispositivos móviles."))
    d.add(h3("2.1.9. Diseño de experiencia de usuario, prototipado y reportes"))
    d.add(p("El diseño de experiencia de usuario (UX) se ocupa de que el producto sea útil y fácil de usar para su usuario real, "
            "y el diseño de interfaz (UI) de su expresión visual. Un prototipo es una representación del sistema que permite "
            "evaluar el diseño antes de programarlo: los *wireframes* fijan la estructura, los *mockups* la apariencia y los "
            "prototipos navegables la interacción. Herramientas como Balsamiq, Figma o Penpot se emplean para elaborarlos."))
    d.add(p("La calidad de uso se evalúa con criterios conocidos: las diez heurísticas de usabilidad de Nielsen, las pautas de "
            "accesibilidad WCAG 2.1 (contraste mínimo de 4,5:1 y objetivos táctiles suficientes) y las guías de Material Design "
            "para Android, que recomiendan objetivos táctiles de al menos 48 dp. Para un usuario sin formación contable que "
            "trabaja de pie en el mostrador, estos criterios se traducen en pocas pantallas por tarea, textos en lenguaje "
            "cotidiano, botones grandes y mensajes que indican qué hacer. Los reportes clave se diseñan con el mismo criterio: "
            "deben contener solo la información que el usuario o su contador necesitan para declarar."))
    d.add(h3("2.1.10. Documentación técnica como código"))
    d.add(p("La documentación técnica para desarrolladores se escribe en Markdown, un lenguaje de marcado ligero que se lee "
            "igual en texto plano que renderizado, y se versiona en el mismo repositorio que el código (*docs as code*). Así, "
            "cada cambio de diseño se revisa y se conserva junto con el cambio de código que lo implementa. El proyecto "
            "entrega un documento Markdown con la arquitectura, los contratos entre capas, el esquema de datos, las reglas "
            "del NRUS y las convenciones de construcción (Anexo T)."))


# ======================================================================
def capitulo3(d):
    d.e1(163)
    # ---------------- 3.1 ----------------
    d.add(h2("3.1. Metodología"))
    d.add(h3("3.1.1. Tipo, enfoque, diseño y alcance de la investigación"))
    d.tabla("t_metodologia", "Caracterización metodológica del proyecto",
            ["Aspecto", "Definición en el proyecto"],
            [["Tipo de investigación", "Aplicada: usa conocimiento existente (IA multimodal, ajuste fino, ingeniería de software) para resolver un problema concreto de la bodega del NRUS."],
             ["Enfoque", "Cuantitativo: la mejora se mide con indicadores numéricos (exactitud por campo, tiempo de registro, diferencia del total mensual)."],
             ["Diseño", "Preexperimental con medición antes y después: línea base del caso simulado (Tabla 7) frente al resultado con la aplicación en un mes simulado."],
             ["Alcance", "Explicativo: busca establecer si el registro por fotografía con Gemma 4 en el teléfono mejora la exactitud de la declaración."],
             ["Variables", "Independiente: aplicación Facturas S20. Dependiente: exactitud en la determinación de la categoría y la cuota del NRUS."]],
            [2.2, 7.8])
    d.add(h3("3.1.2. Metodología de desarrollo"))
    d.add(p("El desarrollo sigue un proceso iterativo e incremental con iteraciones de dos semanas, alineadas con las entregas del "
            "curso. Cada iteración parte de los requerimientos priorizados del SRS, termina con un incremento verificable y "
            "actualiza el cronograma. El segundo avance cierra la fase de diseño; las iteraciones siguientes construyen primero "
            "el núcleo sin conexión (dominio, datos y pantallas de registro) y después la inferencia con el modelo convertido."))
    d.tabla("t_iteraciones", "Iteraciones del proyecto",
            ["Iteración", "Semanas", "Objetivo", "Entregable"],
            [["I1", "1–4", "Análisis del contexto, alternativas y SRS", "APF1"],
             ["I2", "5–8", "Diseño de procesos, datos, clases, prototipo y documentación técnica", "APF2 (este informe)"],
             ["I3", "9–10", "Front-end navegable y dominio con motor de reglas probado", "Incremento 1"],
             ["I4", "11–12", "Persistencia cifrada, inferencia local con el modelo convertido", "APF3"],
             ["I5", "13–14", "Reportes, exportación de datos e historial", "Incremento 3"],
             ["I6", "15–18", "Pruebas, validación en mes simulado y sustentación", "Informe final"]],
            [1.2, 1.2, 5.0, 2.4], centrar=(0, 1))
    d.add(h3("3.1.3. Retos y decisiones técnicas"))
    d.add(p("Antes del diseño detallado se resumen los retos técnicos que dieron forma a la solución seleccionada en el "
            "apartado 3.2 y la decisión tomada para cada uno."))
    d.e1(166, 168, reemplazos=[("Inferencia 100 % local; respaldo opcional y cifrado", "Inferencia 100 % local; sin servidor ni respaldo remoto"),
                               ("Parámetros externos versionados publicados por el servidor", "Parámetros en un archivo versionado dentro de la app"),
                               ("Actualizar sin publicar una versión nueva de la app", "Cambiar la norma sin modificar el código Java")])

    # ---------------- 3.2 ----------------
    d.add(h2("3.2. Planteamiento de alternativas de solución"))
    d.add(h3("3.2.1. Identificación de alternativas"))
    d.e1(431)
    d.add(p("En este avance cada alternativa se profundiza con seis aspectos: la descripción de la propuesta, las tecnologías, "
            "la forma en que aplica las TIC, la participación de Java, sus pantallas y la cobertura del alcance planteado. Las "
            "fichas técnicas del primer avance se conservan en el Anexo E y las pantallas de las alternativas 2 y 3 en el "
            "Anexo F."))
    alternativas = [
        ("3.2.2. Alternativa de solución 1 (seleccionada): app Android nativa en Java con Gemma 4 embebido", "t_alt1", "Alternativa 1: app Android con Gemma 4 embebido en el teléfono",
         [["Descripción de la propuesta", "Aplicación Android nativa que fotografía la factura y la lee dentro del teléfono con Gemma 4 E2B ajustado y cuantizado. Calcula el acumulado del mes, la categoría, la cuota y el vencimiento, y exporta los datos a archivos para analizarlos en una PC. No tiene servidor propio: toda la información permanece en el teléfono."],
          ["Tecnologías utilizadas", "Java con Android SDK, CameraX, Room con SQLCipher, WorkManager; LiteRT-LM con un puente en Kotlin; Gemma 4 E2B (.litertlm) descargado de un repositorio público; exportación a CSV y PDF con el selector de archivos de Android."],
          ["Aplicación de TIC", "IA multimodal en el borde (edge AI), visión por computadora para medir nitidez, base de datos cifrada, notificaciones locales, intercambio de archivos abiertos (CSV y PDF) y computación en la nube solo para entrenar el modelo."],
          ["Participación de Java", "≈ 85 % del código del producto, todo en la app Android; Kotlin ≈ 5 % limitado al puente con LiteRT-LM."],
          ["Pantallas", "20 pantallas móviles (P01–P20) y el reporte mensual en PDF (apartado 3.6)."],
          ["Cobertura del alcance", "22 de 22 requerimientos funcionales y 19 de 19 no funcionales; funciona sin conexión, incluida la lectura de la factura."]]),
        ("3.2.3. Alternativa de solución 2: app Android nativa en Java con la inferencia en un servidor", "t_alt2", "Alternativa 2: app Android con la inferencia en un servidor",
         [["Descripción de la propuesta", "La app captura la foto y la envía a un servidor propio que ejecuta Gemma 4 ajustado en una GPU y devuelve los campos. Sin conexión, la factura queda en cola hasta recuperar la señal."],
          ["Tecnologías utilizadas", "Java con Android SDK, Room sobre SQLite, Retrofit; servidor Java con Spring Boot que invoca el modelo en una GPU; Oracle Database o SQL Server."],
          ["Aplicación de TIC", "IA multimodal en la nube, servicios web REST, colas de mensajes para el modo sin conexión y almacenamiento centralizado."],
          ["Participación de Java", "≈ 85 % (app y servidor en Java; el servicio del modelo en Python se aísla detrás de la API)."],
          ["Pantallas", "7 pantallas (Anexo F): acceso, cámara, envío y espera, verificación, resumen, cola sin conexión y ajustes."],
          ["Cobertura del alcance", "Cubre los 22 RF, pero no cumple RNF-12 (lectura sin conexión) ni RNF-14 (la imagen no sale del teléfono) y agrega un costo mensual de GPU."]]),
        ("3.2.4. Alternativa de solución 3: aplicación web en Java con un servicio de extracción de terceros", "t_alt3", "Alternativa 3: aplicación web con un servicio comercial de extracción",
         [["Descripción de la propuesta", "Aplicación web adaptable al teléfono donde el usuario sube la foto; el servidor la envía a un servicio comercial de comprensión documental y guarda el resultado."],
          ["Tecnologías utilizadas", "Java con Spring Boot y Thymeleaf, HTML y CSS, Oracle Database o SQL Server y la API de un proveedor de extracción documental."],
          ["Aplicación de TIC", "Servicios en la nube de terceros, aplicación web adaptable y base de datos centralizada."],
          ["Participación de Java", "≈ 80 % (servidor y vistas en Java; HTML y CSS en las plantillas)."],
          ["Pantallas", "6 pantallas web (Anexo F): acceso, carga de foto, verificación, resumen, reporte y configuración."],
          ["Cobertura del alcance", "Cubre la mayoría de RF, pero no permite ajuste fino (RNF-02), requiere conexión permanente (RNF-12), envía las imágenes a terceros (RNF-14) y cobra por documento."]]),
    ]
    for titulo, clave, leyenda, filas in alternativas:
        d.add(h3(titulo))
        d.tabla(clave, leyenda, ["Aspecto", "Contenido"], filas, [2.2, 7.8])
    d.add(h3("3.2.5. Comparación de alternativas"))
    d.e1(455, 458)
    d.add(p("La Tabla {ref:t_cobertura_alt} complementa la matriz con la cobertura del alcance por grupo de requerimientos, que "
            "es el criterio que exige la consigna: las tres alternativas tienen al menos cinco pantallas y al menos 50 % de "
            "Java, pero solo la primera cubre todo el alcance funcional y no funcional."))
    d.tabla("t_cobertura_alt", "Cobertura del alcance por alternativa",
            ["Grupo de requerimientos", "A1 en el teléfono", "A2 en servidor", "A3 web con terceros"],
            [["Captura y lectura (RF-01 a RF-04)", "Completa, sin conexión", "Completa, solo con conexión", "Completa, solo con conexión"],
             ["Verificación y validación (RF-05 a RF-07)", "Completa", "Completa", "Completa"],
             ["Cálculo NRUS y avisos (RF-08 a RF-14)", "Completa", "Completa", "Completa"],
             ["Reportes e historial (RF-15 a RF-18)", "Completa", "Completa", "Completa"],
             ["Seguridad, modelo y exportación (RF-19 a RF-22)", "Completa", "Parcial: no instala modelo", "Parcial: modelo cerrado"],
             ["Requerimientos no funcionales (19)", "19 de 19", "16 de 19", "13 de 19"],
             ["Pantallas / participación de Java", "20 / ≈ 85 %", "7 / ≈ 85 %", "6 / ≈ 80 %"]],
            [3.2, 2.3, 2.3, 2.3], centrar=(1, 2, 3))
    d.add(h3("3.2.6. Selección de la alternativa"))
    d.e1(459)
    d.add(p("Por lo anterior se selecciona la alternativa 1. Todo el diseño que sigue —procesos, datos, clases, prototipo y "
            "arquitectura— corresponde a ella."))

    # ---------------- 3.3 ----------------
    d.add(h2("3.3. Gestión del proyecto"))
    d.add(h3("3.3.1. Project Charter"))
    d.add(p("El acta de constitución formaliza el proyecto con su propósito, objetivo, alcance de alto nivel, interesados, "
            "supuestos, restricciones y criterio de éxito. Se aprobó en el primer avance y se mantiene sin cambios de alcance; "
            "su versión completa está en el Anexo B."))
    d.add(h3("3.3.2. Estructura de Descomposición del Trabajo (WBS)"))
    d.e1(466)
    d.add(p("El diagrama y el diccionario con los responsables de cada paquete están en el Anexo C. En este avance se completaron "
            "los paquetes del entregable 3 (Diseño): prototipos de interfaz, arquitectura, diagramas de secuencia y de clases y "
            "modelo de datos."))
    d.add(h3("3.3.3. Diagrama de Gantt"))
    d.add(p("La línea base del cronograma aprobada en el primer avance se conserva en el Anexo D. El cronograma actualizado al "
            "corte de la semana 8, con el estado de cada actividad, se presenta en el apartado 4.1."))
    d.add(h3("3.3.4. Planificación del proyecto"))
    d.e1(478, 480)
    d.add(p("El registro de riesgos se revisó al cierre del diseño. Los riesgos R4 (pérdida de exactitud al convertir el modelo) "
            "y R5 (rendimiento en el teléfono) siguen siendo los de mayor impacto; el diseño los mitiga con el registro manual "
            "alternativo (P19), la verificación de memoria antes de descargar el modelo (P03) y la conservación de la versión "
            "anterior del modelo (RNF-19). El registro completo está en el Anexo D."))

    # ---------------- 3.4 ----------------
    d.add(h2("3.4. Análisis de la solución"))
    d.add(p("El análisis del primer avance se afinó con los procesos modelados en BPMN: cada tarea del proceso propuesto que "
            "ejecuta la aplicación corresponde a un caso de uso y a uno o más requerimientos, lo que permitió confirmar que el "
            "catálogo está completo. No se agregaron requerimientos; se precisaron sus criterios de aceptación en el diseño."))
    d.add(h3("3.4.1. Identificación de actores"))
    d.tabla("t_actores", "Actores del sistema", ["Actor", "Tipo", "Descripción"],
            [["Bodeguero", "Persona", "Titular del negocio; registra facturas y consulta su situación del mes."],
             ["Contador externo", "Persona", "Recibe el reporte mensual o los datos exportados cuando el titular los comparte; no usa la app."],
             ["Motor de extracción", "Sistema", "Gemma 4 E2B ajustado ejecutado en el teléfono mediante LiteRT-LM."],
             ["Motor de reglas NRUS", "Sistema", "Componente Java que aplica la norma: categoría, cuota, topes y vencimiento."]],
            [2.4, 1.4, 6.2])
    d.add(p("Respecto del primer avance se retiró el actor Administrador: sin servidor ni consola no hay nada que administrar "
            "fuera del teléfono. Las versiones del modelo y de los parámetros del NRUS llegan con cada actualización de la app."))
    d.add(h3("3.4.2. Casos de uso del sistema"))
    d.e1(493, 495, reemplazos=[("Bodeguero, Administrador", "Bodeguero")])
    d.add(h3("3.4.3. Diagrama general de casos de uso"))
    d.e1(181, reemplazos=[("La especificación completa está en el Anexo 4.", "Las especificaciones están en el apartado 3.4.4 y en el Anexo G."),
                          ("El sistema tiene doce casos de uso y cinco actores. Tres son personas —el bodeguero, el contador externo que "
                           "recibe el reporte y el administrador que publica el modelo y los parámetros— y dos son actores de sistema",
                           "El sistema tiene doce casos de uso y cuatro actores. Dos son personas —el bodeguero y el contador externo que "
                           "recibe el reporte o los datos exportados— y dos son actores de sistema")])
    d.figura("f_casos_uso", "casos_uso.png", "Diagrama de casos de uso del sistema Facturas S20")
    d.add(h3("3.4.4. Especificación de casos de uso"))
    d.add(p("Se presentan las especificaciones de los dos casos de uso de mayor riesgo técnico: el registro por fotografía y la "
            "instalación del modelo. Las de CU-02, CU-03, CU-05, CU-08 y CU-12 están en el Anexo G."))
    d.e1(185, 187)
    d.tabla("t_cu09", "Especificación del caso de uso CU-09 Instalar y actualizar el modelo de IA", None,
            [["Actores", "Bodeguero (principal)"],
             ["Precondición", "La app está instalada con el manifiesto del modelo (assets/modelo.json) y el teléfono tiene conexión Wi-Fi."],
             ["Flujo básico", "1. El sistema comprueba la memoria y el espacio libre del equipo. 2. Muestra el tamaño del modelo (≈ 2,6 GB) "
              "y pide confirmación. 3. Descarga el archivo .litertlm por Wi-Fi desde el repositorio público indicado en el manifiesto. "
              "4. Verifica su huella SHA-256. 5. Carga el modelo con una factura de prueba. 6. Registra la versión instalada."],
             ["Flujos alternos", "1a. El equipo no alcanza la memoria recomendada: se informa y se activa el modo de registro manual. "
              "3a. Se corta la conexión: la descarga se reanuda. 4a. La huella no coincide: se descarta el archivo y se reintenta. "
              "6a. Una actualización de la app trae un manifiesto con otra versión: se repite el flujo y se conserva la versión "
              "anterior hasta verificar la nueva."],
             ["Postcondición", "El modelo queda instalado y la lectura de facturas funciona sin conexión. Durante la descarga no se envía ningún dato del usuario."],
             ["Requerimientos", "RF-20; RNF-05, RNF-06, RNF-07, RNF-16, RNF-19"]],
            [2.0, 8.0])
    d.add(h3("3.4.5. Requerimientos funcionales"))
    d.add(p("La especificación define 22 requerimientos funcionales con su prioridad y el caso de uso del que provienen. La tabla "
            "completa está en el Anexo H. Por prioridad, 13 son de prioridad alta, 8 de prioridad media y 1 de prioridad baja; "
            "los de prioridad alta forman el núcleo que se construye primero (captura, lectura, validación, acumulado, "
            "determinación, vencimiento, acceso e instalación del modelo)."))
    d.add(h3("3.4.6. Requerimientos no funcionales"))
    d.add(p("Los 19 requerimientos no funcionales se clasifican con las características de calidad de la norma ISO/IEC 25010 y "
            "cada uno tiene una métrica de aceptación verificable (Anexo I). En el diseño, cada uno se tradujo en una decisión "
            "concreta de arquitectura, de datos o de interfaz, como muestra la verificación del apartado 3.7.6."))
    d.add(h3("3.4.7. Especificación de Requerimientos de Software (SRS)"))
    d.add(p("La especificación sigue la estructura de la norma ISO/IEC/IEEE 29148. La Tabla {ref:t_srs} indica dónde se "
            "encuentra cada sección dentro de este informe."))
    d.tabla("t_srs", "Estructura del SRS según ISO/IEC/IEEE 29148 y su ubicación",
            ["Sección del SRS", "Contenido", "Ubicación"],
            [["1. Introducción", "Propósito, alcance del producto, definiciones y referencias", "Cap. 1 y Bibliografía"],
             ["2. Descripción general", "Perspectiva del producto, funciones, usuarios, restricciones y supuestos", "1.2.3, 3.4.1 y Anexo B"],
             ["3.1 Interfaces externas", "Interfaces de usuario, de hardware (cámara) y de software (LiteRT-LM, selector de archivos de Android)", "3.5.7, 3.6 y Anexo N"],
             ["3.2 Requerimientos funcionales", "22 RF con prioridad y caso de uso", "Anexo H"],
             ["3.3 Requerimientos no funcionales", "19 RNF según ISO/IEC 25010 con métrica", "Anexo I"],
             ["3.4 Restricciones de diseño", "Java ≥ 50 %, Android 8.0+, ejecución local del modelo", "3.5.8 y 3.5.10"],
             ["3.5 Requerimientos de datos", "Modelo lógico y físico, retención y seguridad", "3.5.5, 3.5.6 y Anexo M"],
             ["4. Verificación", "Criterios de aceptación y matriz de cobertura", "3.7 y Anexo R"],
             ["Apéndice: trazabilidad", "Objetivo → caso de uso → requerimiento", "Anexo J"]],
            [2.6, 4.8, 2.4])

    # ---------------- 3.5 ----------------
    d.add(h2("3.5. Diseño detallado de la solución"))
    d.add(h3("3.5.1. Arquitectura de la solución"))
    d.add(p("La arquitectura se organiza en cuatro capas dentro del teléfono; fuera de él solo están Google Play, que distribuye "
            "la app, y el repositorio público del que se descarga una vez el modelo. No hay servidor propio. La regla de "
            "dependencias es que las capas externas dependen de las internas y nunca al revés: la presentación usa el dominio, "
            "y la inferencia y los datos implementan interfaces que el dominio define. Así, el motor de reglas del NRUS no "
            "conoce Android ni LiteRT-LM y puede probarse de forma aislada, y el modelo de IA puede reemplazarse sin tocar el "
            "dominio (RNF-18 y RNF-19)."))
    d.figura("f_arquitectura", "arquitectura_capas.png", "Arquitectura por capas de Facturas S20")
    d.tabla("t_capas", "Responsabilidades de cada capa",
            ["Capa", "Responsabilidad", "No debe"],
            [["Presentación", "Mostrar el estado y capturar las acciones del usuario; cámara y navegación.", "Calcular montos ni acceder a la base de datos."],
             ["Dominio", "Casos de uso, reglas del NRUS, validaciones del RUC, de la fecha y de duplicados.", "Depender de clases de Android o de bibliotecas externas."],
             ["Inferencia local", "Preparar la imagen, invocar a Gemma 4 en el teléfono y convertir la respuesta en campos con confianza.", "Enviar la imagen fuera del teléfono."],
             ["Datos", "Persistir de forma transaccional y cifrada; exponer repositorios al dominio.", "Contener reglas de negocio."],
             ["Fuera del teléfono", "Google Play distribuye la app; el repositorio público aloja el archivo del modelo.", "Recibir o solicitar datos del usuario."]],
            [1.8, 4.6, 3.2])
    d.add(p("La vista dinámica de la arquitectura para el caso de uso principal se muestra en el diagrama de secuencia de la "
            "Figura {ref:f_secuencia}: todos los mensajes ocurren dentro del teléfono y ningún paso requiere conexión."))
    d.add(D.leyenda("Figura", "f_secuencia", "Diagrama de secuencia de CU-01 y CU-02: registrar y verificar una factura"))
    d.e1(191, 192)

    d.add(h3("3.5.2. Diseño de procesos"))
    d.add(p("Los procesos se modelaron con BPMN 2.0. La Figura {ref:f_notacion} resume los elementos de la notación que se usan en "
            "todos los diagramas, con un ejemplo tomado del propio proyecto para cada uno. Se modelaron cinco procesos: el "
            "proceso actual (AS-IS), el proceso propuesto (TO-BE) y tres subprocesos del proceso propuesto que la aplicación "
            "automatiza: el registro de una factura, el cierre mensual y la publicación y actualización del modelo."))
    d.figura("f_notacion", "bpmn_00_notacion.png", "Elementos de la notación BPMN 2.0 utilizados, con ejemplos del proyecto")
    d.tabla("t_procesos", "Procesos de negocio modelados",
            ["Proceso", "Tipo", "Participantes (pools)", "Figura", "Casos de uso"],
            [["Recepción de facturas y declaración mensual", "AS-IS", "Distribuidor, bodega, SUNAT", "{ref:f_asis}", "—"],
             ["Registro al recibir y cierre asistido", "TO-BE", "Distribuidor, bodega (bodeguero y app), SUNAT", "{ref:f_tobe}", "CU-01 a CU-08"],
             ["Registrar factura con IA", "Subproceso", "App (bodeguero, captura, motor de extracción, reglas)", "{ref:f_registro}", "CU-01, CU-02"],
             ["Cierre mensual y declaración", "Subproceso", "Bodega, contador externo", "{ref:f_cierre}", "CU-03, CU-05 a CU-08, CU-11"],
             ["Publicar y actualizar el modelo", "Subproceso", "Equipo, Hugging Face y Google Play, app", "{ref:f_modelo}", "CU-09"]],
            [3.0, 1.3, 3.2, 0.9, 2.0], centrar=(1, 3))

    d.add(h3("3.5.3. Diagramas de procesos"))
    d.add(subtitulo("a) Proceso actual (AS-IS)"))
    d.add(p("El proceso actual tiene tres participantes. El distribuidor entrega la mercadería con la factura; el bodeguero la "
            "recibe y la guarda en una caja; y recién al fin de mes —un evento de temporizador— reúne las facturas, estima las "
            "que faltan, suma a mano y decide la categoría. La compuerta «¿Seguro de la categoría?» es donde se origina el pago "
            "en exceso: cuando la respuesta es no, el titular declara la categoría superior. Las anotaciones en rojo marcan los "
            "tres puntos de error que el diagnóstico cuantificó."))
    d.figura("f_asis", "bpmn_01_asis.png", "Proceso actual (AS-IS) de recepción de facturas y declaración del NRUS")
    d.add(subtitulo("b) Proceso propuesto (TO-BE)"))
    d.add(p("En el proceso propuesto la bodega se divide en dos carriles: el bodeguero y la aplicación en su teléfono. La factura "
            "se fotografía en el momento en que llega, la lectura la hace Gemma 4 dentro del teléfono y el bodeguero solo "
            "verifica. El acumulado y la categoría se recalculan con cada factura, y la compuerta «¿≥ 80 % del límite?» dispara "
            "un aviso antes de que el problema ocurra. El cierre mensual deja de ser una tarea manual: un temporizador recuerda "
            "el vencimiento y pide solo el total de ventas. Desaparecen la caja, la suma a mano y la estimación ante la duda."))
    d.figura("f_tobe", "bpmn_02_tobe.png", "Proceso propuesto (TO-BE) con Facturas S20")
    d.tabla("t_asis_tobe", "Comparación del proceso actual y el propuesto",
            ["Aspecto", "Proceso actual (AS-IS)", "Proceso propuesto (TO-BE)"],
            [["Momento del registro", "Fin de mes, si se registra", "Al recibir la factura"],
             ["Tareas manuales del titular", "Guardar, reunir, estimar, sumar, decidir, declarar", "Fotografiar, verificar, ingresar ventas, declarar"],
             ["Punto de decisión de la categoría", "Juicio del titular ante la duda", "Motor de reglas con el monto real"],
             ["Tiempo mensual dedicado", "≈ 3 horas al cierre", "≈ 10 s por factura y un campo al cierre"],
             ["Sustento documental", "Facturas en papel, 64 % conservadas", "Imagen de cada factura y reporte PDF"],
             ["Avisos preventivos", "Ninguno", "80 % y 100 % del límite, topes anuales y vencimiento"]],
            [2.4, 3.6, 3.6])
    d.add(subtitulo("c) Subproceso «Registrar factura con IA»"))
    d.add(p("El subproceso detalla el caso de uso principal con sus excepciones. Tiene cuatro carriles dentro de la aplicación. "
            "Tres compuertas exclusivas cubren los flujos alternos: si la foto no es legible se pide otra toma; si el modelo no "
            "está instalado o la memoria no alcanza se ofrece el registro manual; y si la factura ya existe se muestra el aviso "
            "de duplicado y el proceso termina sin registrar. Solo después de la revisión del bodeguero se guarda la factura, en "
            "una transacción que incluye la imagen y los campos leídos, y se recalcula la determinación del mes."))
    d.figura("f_registro", "bpmn_03_registro.png", "Subproceso «Registrar factura con IA» (CU-01 y CU-02)")
    d.add(p("Los subprocesos de cierre mensual y de publicación del modelo se presentan en el Anexo K (Figuras "
            "{ref:f_cierre} y {ref:f_modelo})."))

    d.add(h3("3.5.4. Diagrama de clases"))
    d.add(p("El diseño de clases tiene dos vistas. El modelo del dominio (Figura {ref:f_clases_dominio}) contiene 14 clases y las "
            "enumeraciones que fijan los estados válidos. Respecto del primer avance se agregaron ParametrosNrus, que agrupa en "
            "una versión las categorías y el cronograma para que puedan actualizarse sin modificar el código, y Aviso, que representa "
            "los recordatorios programados; FacturaCompra incorpora su origen (IA o manual) y el motivo de anulación, y "
            "CampoExtraido distingue el valor leído del valor final para medir la exactitud en uso."))
    d.figura("f_clases_dominio", "clases_dominio.png", "Diagrama de clases del dominio")
    d.add(p("La segunda vista (Figura {ref:f_clases_diseno}) muestra las clases de diseño que implementan el flujo CU-01 en las "
            "cuatro capas. Se aplican tres buenas prácticas: inversión de dependencias (RegistrarFacturaUseCase depende de las "
            "interfaces ExtractorFacturas y FacturaRepositorio, no de sus implementaciones), responsabilidad única (el "
            "ViewModel solo gestiona estado; el caso de uso solo orquesta) y encapsulamiento (atributos privados, montos con "
            "BigDecimal y enumeraciones en lugar de cadenas)."))
    d.figura("f_clases_diseno", "clases_diseno.png", "Diagrama de clases de diseño del flujo de registro")
    d.add(p("El diccionario de clases, con la responsabilidad de cada una, está en el Anexo L."))

    d.add(h3("3.5.5. Diagrama entidad-relación"))
    d.add(p("La solución tiene una sola base de datos: SQLite dentro del teléfono, administrada con Room y cifrada con SQLCipher. "
            "Es la única fuente de verdad: contiene las facturas, sus imágenes, los campos leídos, los períodos y su determinación. Tiene 13 tablas "
            "normalizadas hasta la tercera forma normal (Figura {ref:f_der_local}). Las categorías y el cronograma dependen de "
            "una versión de parámetros, de modo que el histórico conserva la norma con la que se calculó cada mes."))
    d.figura("f_der_local", "der_local.png", "Diagrama entidad-relación de la base local")
    d.add(p("Respecto del primer avance se retiró la base del servidor de respaldo: no existen cuentas en línea, respaldos "
            "remotos ni tablas de administración. Cuando el titular necesita revisar sus datos en una PC, la aplicación los "
            "exporta desde esta misma base a archivos CSV (apartado 3.6.8)."))

    d.add(h3("3.5.6. Diseño de base de datos"))
    d.add(subtitulo("a) Diseño lógico"))
    d.add(p("El diseño lógico parte de las entidades del dominio y aplica la normalización. En primera forma normal todos los "
            "atributos son atómicos: la serie y el número se separan, y los campos leídos por el modelo se guardan como filas "
            "de campo_extraido en lugar de columnas repetidas. En segunda forma normal los datos del emisor salen de la factura "
            "a su propia tabla, porque dependen solo del RUC del emisor. En tercera forma normal la cuota no se repite en la "
            "determinación como dato derivado de la categoría sin más: se conserva porque es el valor histórico efectivamente "
            "calculado, y la categoría vigente se referencia por clave. La Tabla {ref:t_reglas_integridad} resume las reglas de "
            "integridad."))
    d.tabla("t_reglas_integridad", "Reglas de integridad del modelo lógico",
            ["Regla", "Implementación", "Requerimiento"],
            [["Un período por contribuyente y mes", "UNIQUE (id_contribuyente, anio, mes)", "RF-08"],
             ["Una factura no se registra dos veces", "UNIQUE (id_emisor, serie, numero)", "RF-07"],
             ["Importe positivo y moneda válida", "CHECK (importe_total > 0), moneda IN ('PEN','USD')", "RF-06"],
             ["RUC de 11 dígitos", "CHECK (length(ruc) = 11) + validación módulo 11 en el dominio", "RF-06, RF-19"],
             ["Una sola determinación por período", "id_periodo UNIQUE en determinacion", "RF-10"],
             ["La factura anulada no se borra", "estado VIGENTE | ANULADA con motivo_anulacion", "RF-18"],
             ["Cada campo leído conserva la versión del modelo", "FK id_modelo en campo_extraido", "RNF-01, RNF-19"]],
            [3.0, 4.6, 1.8])
    d.add(subtitulo("b) Diseño físico"))
    d.add(p("El esquema se implementa en SQLite mediante Room y se cifra completo con SQLCipher (AES-256); la clave se genera en "
            "el teléfono y se protege con Android Keystore. Las claves primarias son INTEGER con autoincremento, las fechas se "
            "guardan como TEXT en formato ISO-8601 y los montos como NUMERIC, que Room convierte a BigDecimal con un "
            "TypeConverter. Las claves foráneas se activan al abrir la base (PRAGMA foreign_keys = ON) y los cambios de esquema "
            "se aplican con migraciones de Room versionadas. A continuación se muestran la entidad Room principal y un extracto "
            "del DDL de SQLite; el script completo de las 13 tablas está en el Anexo M."))
    d.add(codigo('''@Entity(tableName = "factura_compra",
        foreignKeys = {
            @ForeignKey(entity = PeriodoEntity.class, parentColumns = "id_periodo", childColumns = "id_periodo"),
            @ForeignKey(entity = EmisorEntity.class, parentColumns = "id_emisor", childColumns = "id_emisor")},
        indices = {
            @Index(value = {"id_emisor", "serie", "numero"}, unique = true),
            @Index(value = {"id_periodo", "estado"})})
public class FacturaCompraEntity {
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id_factura") public long id;
    @ColumnInfo(name = "id_periodo") public long idPeriodo;
    @ColumnInfo(name = "id_emisor") public long idEmisor;
    @NonNull public String serie;
    @NonNull public String numero;
    @ColumnInfo(name = "fecha_emision") @NonNull public String fechaEmision;   // ISO-8601
    @NonNull public String moneda = "PEN";
    @ColumnInfo(name = "importe_total") @NonNull public BigDecimal importeTotal; // TypeConverter a NUMERIC
    @NonNull public String origen;          // IA | MANUAL
    @NonNull public String estado = "VIGENTE";
    @ColumnInfo(name = "motivo_anulacion") public String motivoAnulacion;
}''', "FacturaCompraEntity.java — entidad Room de la base local"))
    d.add(codigo('''CREATE TABLE modelo_local (
  id_modelo         INTEGER PRIMARY KEY AUTOINCREMENT,
  version           TEXT NOT NULL UNIQUE,
  tamano_mb         INTEGER NOT NULL,
  sha256            TEXT NOT NULL CHECK (length(sha256) = 64),
  estado            TEXT NOT NULL CHECK (estado IN ('NO_INSTALADO','DESCARGANDO','VERIFICADO','ACTIVO')),
  ruta              TEXT,
  fecha_instalacion TEXT
);
CREATE TABLE parametro_version (
  id_parametro      INTEGER PRIMARY KEY AUTOINCREMENT,
  version           TEXT NOT NULL UNIQUE,            -- p. ej. 2026.1, leída de assets/parametros_nrus.json
  umbral_aviso      NUMERIC NOT NULL DEFAULT 0.80,
  tope_anual        NUMERIC NOT NULL,
  vigente_desde     TEXT NOT NULL,
  activo            INTEGER NOT NULL DEFAULT 0 CHECK (activo IN (0,1))
);
CREATE TABLE categoria_nrus (
  id_categoria      INTEGER PRIMARY KEY AUTOINCREMENT,
  id_parametro      INTEGER NOT NULL REFERENCES parametro_version(id_parametro),
  codigo            INTEGER NOT NULL,
  limite_mensual    NUMERIC NOT NULL,
  cuota             NUMERIC NOT NULL,
  UNIQUE (id_parametro, codigo)
);''', "Extracto del DDL de la base local (SQLite)"))
    d.add(subtitulo("c) Consideraciones de seguridad"))
    d.tabla("t_seguridad_bd", "Medidas de seguridad del diseño de datos",
            ["Amenaza", "Medida", "Dónde", "RNF"],
            [["Robo o pérdida del teléfono", "Base cifrada con SQLCipher (AES-256); clave protegida por Android Keystore; acceso con PIN o huella", "Teléfono", "RNF-15"],
             ["Lectura de imágenes desde otras apps", "Imágenes cifradas en el almacenamiento interno de la app, no en la galería", "Teléfono", "RNF-14"],
             ["Envío de datos a terceros", "No hay servidor ni cuentas en línea; la única conexión de la app descarga el modelo", "Teléfono", "RNF-16"],
             ["Acceso no autorizado a la app", "PIN guardado con hash y sal, espera tras 3 intentos fallidos, huella con BiometricPrompt", "Teléfono", "RNF-15"],
             ["Inyección SQL", "Consultas parametrizadas con Room (@Query con parámetros)", "Teléfono", "RNF-15"],
             ["Exposición del archivo exportado", "Solo se genera a pedido del titular, con aviso de que no va cifrado; se entrega por el selector de archivos", "Teléfono", "RNF-14"],
             ["Manipulación del modelo", "Verificación de SHA-256 antes de activar el archivo descargado", "Teléfono", "RNF-19"],
             ["Pérdida de datos por cierre inesperado", "Escritura transaccional (@Transaction) de factura, imagen y campos", "Teléfono", "RNF-13"],
             ["Pérdida del teléfono sin respaldo remoto", "La app sugiere exportar al cerrar cada mes; los reportes PDF compartidos sirven de sustento", "Teléfono", "RNF-14"]],
            [2.4, 4.6, 1.2, 1.1], centrar=(2, 3))
    d.add(subtitulo("d) Índices y volumen esperado"))
    d.add(p("Con 14 a 22 facturas al mes por bodega, la base local crece en menos de 300 filas de facturas al año y alrededor de "
            "2 000 filas de campos leídos; el mayor volumen son las imágenes, de 200 a 400 kB cada una. Los índices se definen "
            "para las consultas frecuentes: facturas del período por estado (lista del mes y acumulado), búsqueda de duplicado "
            "por emisor, serie y número, y cronograma por año, mes y dígito. Un archivo exportado de un mes con imágenes pesa "
            "entre 3 y 9 MB."))

    d.add(h3("3.5.7. Diseño de interfaces"))
    d.add(p("Las interfaces se diseñaron a partir del proceso mejorado (TO-BE): cada tarea del carril del bodeguero tiene una "
            "pantalla y cada tarea automática de la aplicación tiene una respuesta visible en la interfaz. La Tabla "
            "{ref:t_proceso_pantalla} muestra esa correspondencia. El sistema de diseño (colores, tipografía, componentes y "
            "reglas de UX) se presenta en la Figura {ref:f_sistema_diseno} y se detalla en el Anexo O."))
    d.tabla("t_proceso_pantalla", "Correspondencia entre las tareas del proceso TO-BE y las pantallas",
            ["Tarea del proceso propuesto", "Carril", "Pantalla"],
            [["Fotografiar la factura", "Bodeguero", "P05 Guía de captura, P06 Cámara con encuadre"],
             ["Leer la factura con Gemma 4 en el teléfono", "App", "P07 Lectura en el teléfono"],
             ["Validar RUC, fecha y duplicado", "App", "P08 Verificación (campos validados), P09 Aviso de duplicado"],
             ["Verificar y confirmar los campos", "Bodeguero", "P08 Verificación de datos"],
             ["Guardar y actualizar el acumulado", "App", "P10 Factura guardada, P04 Inicio"],
             ["Mostrar aviso de proximidad al tope", "App", "P04 Inicio, P10, P14 Categoría y cuota"],
             ["Recordar ventas y vencimiento", "App", "Notificación local, P15 Vencimiento"],
             ["Ingresar el total de ventas del mes", "Bodeguero", "P13 Ventas del mes"],
             ["Determinar categoría y generar reporte", "App", "P14 Categoría y cuota, P16 Reporte mensual"],
             ["Cerrar el mes", "Bodeguero", "P17 Historial y cierre de mes"],
             ["Exportar los datos para revisarlos en una PC", "Bodeguero", "P20 Exportar datos (desde P17 o P18)"]],
            [3.8, 1.4, 4.6])
    d.figura("f_sistema_diseno", "ui_sistema_diseno.png", "Sistema de diseño de Facturas S20")

    d.add(h3("3.5.8. Tecnologías utilizadas"))
    d.tabla("t_tecnologias", "Tecnologías de la solución",
            ["Capa o función", "Tecnología", "Uso en el proyecto"],
            [["Lenguaje de la app", "Java 17 (Android); Kotlin solo en un archivo", "Todo el código de la app; puente con LiteRT-LM"],
             ["Plataforma móvil", "Android SDK, minSdk 26 (Android 8.0)", "Aplicación nativa para teléfonos Android"],
             ["Interfaz", "Material Components 3, Navigation, ViewBinding", "Pantallas P01–P20 y navegación"],
             ["Cámara", "CameraX", "Vista previa, captura y medición de nitidez"],
             ["IA en el teléfono", "LiteRT-LM (litertlm-android) y Gemma 4 E2B ajustado (.litertlm)", "Lectura de la factura sin conexión"],
             ["Persistencia local", "Room con SQLCipher", "Base local cifrada de 13 tablas"],
             ["Tareas en segundo plano", "WorkManager y notificaciones locales", "Recordatorios y descarga del modelo"],
             ["Seguridad local", "BiometricPrompt, Android Keystore", "Acceso con huella o PIN, claves de cifrado"],
             ["Reportes", "android.graphics.pdf.PdfDocument", "Reporte mensual en PDF"],
             ["Exportación", "Storage Access Framework, FileProvider, java.util.zip", "Archivo ZIP con CSV, imágenes y PDF para la PC"],
             ["Distribución", "Google Play y repositorio público de Hugging Face", "Instalación de la app y descarga única del modelo"],
             ["Entrenamiento", "Python, Unsloth, TRL, Google Colab con GPU", "Ajuste fino y conversión del modelo (fuera del producto)"],
             ["Diseño y documentación", "Figma o Penpot (mockups), SVG, Markdown, Git", "Prototipo, diagramas y documentación técnica"],
             ["Pruebas", "JUnit 5, Espresso, pruebas instrumentadas de Room", "Pruebas unitarias, de interfaz y de datos"]],
            [2.3, 4.2, 3.6])

    d.add(h3("3.5.9. Arquitectura tecnológica"))
    d.add(p("El diagrama de despliegue (Figura {ref:f_despliegue}) muestra dónde se ejecuta cada artefacto. El teléfono aloja la "
            "APK, el motor LiteRT-LM, el archivo del modelo, la base SQLite cifrada y las imágenes: es el único nodo donde se "
            "ejecuta el producto. Google Play distribuye la aplicación con el manifiesto del modelo y los parámetros del NRUS; "
            "el archivo del modelo se descarga una sola vez por HTTPS desde el repositorio público de Hugging Face y se "
            "verifica con su SHA-256; y Google Colab solo interviene para entrenar y publicar nuevas versiones. La PC del "
            "usuario recibe el archivo exportado solo cuando él lo decide. No hay servidor de aplicaciones ni base de datos remota."))
    d.figura("f_despliegue", "arquitectura_despliegue.png", "Diagrama de despliegue de la arquitectura tecnológica")

    d.add(h3("3.5.10. Componentes desarrollados en Java"))
    d.add(p("La Figura {ref:f_componentes} muestra los componentes del producto y su lenguaje. Solo el puente con LiteRT-LM está "
            "en Kotlin, porque la API del motor es Kotlin; el resto de la app está en Java. La participación "
            "estimada de Java es de alrededor de 85 %, por encima del 50 % exigido (Tabla {ref:t_composicion})."))
    d.figura("f_componentes", "componentes_java.png", "Componentes desarrollados en Java y participación por lenguaje")
    d.tabla("t_composicion", "Composición estimada del código del producto", ["Componente", "Lenguaje", "Participación estimada"],
            [["App Android: presentación, dominio, datos y exportación", "Java", "≈ 85 %"],
             ["Puente de inferencia con LiteRT-LM", "Kotlin", "≈ 5 %"],
             ["Layouts, recursos y configuración", "XML / Gradle", "≈ 8 %"],
             ["Migraciones de Room", "SQL", "≈ 2 %"],
             ["Total Java", "", "≈ 85 %"]],
            [5.4, 2.2, 2.4], centrar=(1, 2), resaltar_filas=(4,))
    d.add(codigo('''pe.facturass20
├── ui/                 Activities, Fragments y ViewModels (Java)
│   ├── acceso/         P01 ConfiguracionFragment, P02 AccesoFragment
│   ├── inicio/         P04 InicioFragment, InicioViewModel
│   ├── captura/        P05 GuiaFragment, P06 CapturaActivity, EvaluadorNitidez
│   ├── verificacion/   P07 LecturaFragment, P08 VerificacionFragment, P09, P10
│   ├── facturas/       P11 ListaFacturasFragment, P12 DetalleFacturaFragment
│   ├── mes/            P13 VentasFragment, P14 CategoriaFragment, P15 VencimientoFragment
│   ├── reportes/       P16 ReporteFragment, P17 HistorialFragment
│   ├── ajustes/        P18 AjustesFragment, P03 PreparacionModeloFragment, P19 RegistroManualFragment
│   └── exportacion/    P20 ExportarDatosFragment, ExportarDatosViewModel
├── dominio/            Java puro, sin dependencias de Android
│   ├── modelo/         Contribuyente, PeriodoMensual, FacturaCompra, Determinacion, ...
│   ├── casosuso/       RegistrarFactura, DeterminarCategoria, CerrarPeriodo, GenerarReporte, ExportarDatos, ...
│   ├── reglas/         MotorReglasNRUS, ValidadorRuc, DetectorDuplicados
│   └── puertos/        ExtractorFacturas, FacturaRepositorio, PeriodoRepositorio (interfaces)
├── inferencia/         ExtractorGemmaLocal, ParserRespuesta, GestorModeloLocal
│   └── litert/         LiteRtLmPuente.kt  ← único archivo Kotlin
├── datos/              Room: entidades, DAO, BaseDatosFacturas (SQLCipher), repositorios
├── trabajos/           Workers de recordatorio y de descarga del modelo
└── exportacion/        ExportadorDatos, EscritorCsv (ZIP con CSV, imágenes y PDF)''', "Estructura de paquetes de la aplicación"))
    d.e1(305, 378)
    d.add(codigo('''public class ExportadorDatos implements ExportadorArchivos {

    private final FacturaRepositorio facturas;
    private final AlmacenImagenes imagenes;

    public ExportadorDatos(FacturaRepositorio facturas, AlmacenImagenes imagenes) {
        this.facturas = facturas;
        this.imagenes = imagenes;
    }

    /** Escribe el ZIP en el destino que eligió el usuario con el selector de archivos. No usa la red. */
    @Override
    public void exportar(SolicitudExportacion solicitud, OutputStream destino) throws IOException {
        List<FacturaCompra> delPeriodo = facturas.delPeriodo(solicitud.idPeriodo());
        try (ZipOutputStream zip = new ZipOutputStream(destino)) {
            zip.putNextEntry(new ZipEntry("facturas.csv"));
            EscritorCsv.escribirFacturas(delPeriodo, zip);          // UTF-8 con BOM, separador ;
            zip.closeEntry();
            if (solicitud.incluirImagenes()) {
                for (FacturaCompra f : delPeriodo) {
                    zip.putNextEntry(new ZipEntry("imagenes/" + f.serieNumero() + ".jpg"));
                    imagenes.descifrarEn(f.idImagen(), zip);         // se descifra solo para exportar
                    zip.closeEntry();
                }
            }
        }
    }
}''', "ExportadorDatos.java — exportación de los datos del período a un archivo ZIP"))

    # ---------------- 3.6 ----------------
    d.add(h2("3.6. Diseño del prototipo"))
    d.add(h3("3.6.1. Herramienta utilizada para el prototipo"))
    d.add(p("El prototipo se elaboró como *mockups* de alta fidelidad en formato vectorial SVG, con un sistema de componentes "
            "propio (botones, campos con estados, tarjetas, insignias, barra de navegación) que reproduce la línea gráfica de "
            "la iteración final de diseño del equipo. Los archivos SVG se abren y editan en Figma o Penpot, herramientas de "
            "prototipado equivalentes a Balsamiq, donde se enlazan las pantallas para la navegación interactiva. Cada pantalla "
            "se exporta también como imagen PNG para este informe (Anexo Q)."))
    d.add(h3("3.6.2. Diseño general del prototipo"))
    d.add(p("El prototipo tiene 20 pantallas móviles agrupadas en seis módulos y el reporte mensual en PDF. La navegación principal usa una barra inferior con cuatro destinos (Inicio, Facturas, Mes y "
            "Ajustes) y un botón central de Escanear, de modo que el registro de una factura está siempre a un toque. La Figura "
            "{ref:f_mapa} muestra el mapa de navegación completo."))
    d.figura("f_mapa", "ui_mapa_navegacion.png", "Mapa de navegación del prototipo")
    d.add(h3("3.6.3. Prototipo de la solución"))
    d.tabla("t_pantallas", "Pantallas del prototipo y requerimientos que cubren",
            ["N.º", "Pantalla", "Módulo", "Función", "Requerimientos"],
            [["P01", "Configuración inicial", "Acceso", "RUC validado, nombre del negocio y del titular", "RF-06, RF-19"],
             ["P02", "Acceso con PIN o huella", "Acceso", "Ingreso protegido", "RF-19"],
             ["P03", "Preparar lectura con IA", "Acceso", "Comprobación del equipo y descarga verificada del modelo", "RF-20"],
             ["P04", "Inicio: resumen del mes", "Principal", "Compras, ventas, categoría, cuota, barra al límite, vencimiento", "RF-08, RF-10 a RF-13"],
             ["P05", "Guía de captura", "Registro", "Consejos de foto; acceso a cámara o galería", "RF-01, RF-03"],
             ["P06", "Cámara con encuadre", "Registro", "Marco de encuadre, nitidez y luz en vivo", "RF-01 a RF-03"],
             ["P07", "Lectura en el teléfono", "Registro", "Progreso por campo de Gemma 4 sin conexión", "RF-04"],
             ["P08", "Verificación de datos", "Registro", "Imagen y campos; baja confianza resaltada", "RF-05, RF-06"],
             ["P09", "Aviso de duplicado", "Registro", "Detecta la factura ya registrada", "RF-07"],
             ["P10", "Factura guardada", "Registro", "Confirmación y acumulado actualizado", "RF-08, RF-12"],
             ["P11", "Facturas del mes", "Control", "Lista por semana, filtros y total", "RF-08"],
             ["P12", "Detalle y anulación", "Control", "Datos leídos y anulación con motivo", "RF-18"],
             ["P13", "Ventas del mes", "NRUS", "Un solo campo para el total de ventas", "RF-09"],
             ["P14", "Categoría y cuota", "NRUS", "Monto determinante, tabla vigente y avisos", "RF-10 a RF-12, RF-22"],
             ["P15", "Vencimiento y recordatorio", "NRUS", "Fecha según el RUC y recordatorios", "RF-13, RF-14"],
             ["P16", "Reporte mensual", "Reportes", "Vista previa del PDF y compartir", "RF-15, RF-16"],
             ["P17", "Historial y cierre de mes", "Reportes", "Cierre del mes, meses cerrados, tope anual", "RF-12, RF-17"],
             ["P18", "Ajustes, modelo y datos", "Ajustes", "Negocio, seguridad, modelo, parámetros vigentes y acceso a exportar", "RF-19 a RF-22"],
             ["P19", "Registro manual (sin IA)", "Registro", "Registro validado cuando el equipo no alcanza", "RF-01, RF-06"],
             ["P20", "Exportar datos", "Exportación", "ZIP con CSV, imágenes y PDF para analizar en una PC", "RF-21"]],
            [0.8, 2.4, 1.5, 3.8, 1.9], centrar=(0,))
    d.add(h3("3.6.4. Diseño de la pantalla principal"))
    d.add(p("La pantalla de inicio (P04) responde en un vistazo las tres preguntas del bodeguero: cuánto compró en el mes, qué "
            "categoría y cuota le corresponden, y hasta cuándo tiene para declarar. La tarjeta superior muestra el total de "
            "compras en tamaño grande y una barra de progreso hacia el límite de la categoría con una marca en el 80 %; cuando "
            "se supera esa marca la barra y el texto cambian a ámbar. Debajo están las ventas del mes (editables), la categoría "
            "y la cuota, el vencimiento con los días que faltan y las últimas facturas. El botón central Escanear inicia el "
            "registro."))
    d.figura("f_p04", "pantalla_04_inicio_resumen_del.png", "Pantalla principal (P04): resumen del mes", ancho_cm=6.2)
    d.add(h3("3.6.5. Diseño de módulos"))
    d.add(p("Las Figuras {ref:f_hoja1} a {ref:f_hoja5} presentan las pantallas agrupadas por módulo."))
    hojas = [("f_hoja1", "prototipo_hoja_1_acceso.png", "Módulo de acceso y configuración (P01–P04)"),
             ("f_hoja2", "prototipo_hoja_2_registro.png", "Módulo de registro de facturas (P05–P08)"),
             ("f_hoja3", "prototipo_hoja_3_control.png", "Módulo de control de facturas (P09–P12)"),
             ("f_hoja4", "prototipo_hoja_4_nrus.png", "Módulo de determinación del NRUS (P13–P16)"),
             ("f_hoja5", "prototipo_hoja_5_historial.png", "Módulos de historial, ajustes, registro manual y exportación (P17–P20)")]
    for clave, archivo, titulo in hojas:
        d.figura(clave, archivo, titulo)
    d.add(h3("3.6.6. Diseño de los procesos principales"))
    d.tabla("t_flujos", "Procesos principales recorridos en el prototipo",
            ["Proceso", "Recorrido de pantallas", "Toques", "Excepciones diseñadas"],
            [["Registrar una factura", "P04 → P06 → P07 → P08 → P10", "3 (Escanear, disparar, Guardar)", "Foto ilegible (P06), duplicado (P09), sin IA (P19)"],
             ["Corregir un campo leído", "P08 → editar campo → Guardar", "2", "RUC inválido: el campo se marca en rojo"],
             ["Anular una factura", "P11 → P12 → motivo → Anular", "3", "Confirmación con el monto que se descuenta"],
             ["Cerrar el mes y declarar", "Notificación → P13 → P14 → P16 → P17", "5", "Supera topes: alerta de cambio de régimen"],
             ["Preparar la lectura con IA", "P01 → P03 → P04", "2", "Memoria insuficiente: registro manual"],
             ["Exportar los datos del mes", "P18 → P20 → Guardar en el teléfono o la PC", "3", "Aviso de archivo sin cifrar; sin imágenes si falta espacio"]],
            [2.3, 3.6, 1.9, 3.0])
    d.add(h3("3.6.7. Navegación e interacción"))
    d.tabla("t_interaccion", "Especificación de interacción",
            ["Elemento", "Comportamiento"],
            [["Barra inferior", "Visible en P04, P11, P14, P17 y P18; el destino activo en negrita y color primario."],
             ["Botón Escanear", "Abre P05 la primera vez y directamente P06 cuando el usuario marcó «no volver a mostrar»."],
             ["Cámara", "Evalúa nitidez y luz en cada fotograma; el disparador se habilita cuando ambas son «buenas»."],
             ["Lectura con IA", "Muestra el progreso por campo; se puede cancelar sin perder la foto."],
             ["Campos de verificación", "Borde verde con visto si se validó, ámbar con triángulo si la confianza es menor al 80 %, rojo si no pasa la validación."],
             ["Diálogos", "Se usan solo para acciones que no se pueden deshacer (duplicado, anulación)."],
             ["Mensajes", "En lenguaje cotidiano y con la acción a seguir; nunca códigos de error."],
             ["Retroalimentación", "Vibración corta al capturar; confirmación visual al guardar (P10)."],
             ["Atrás del sistema", "Vuelve a la pantalla anterior; en P08 pide confirmación para descartar la foto."]],
            [2.4, 7.6])
    d.add(h3("3.6.8. Diseño de interfaces y reportes"))
    d.add(p("Además de las pantallas móviles se diseñaron las dos salidas que el titular entrega a otras personas. El reporte "
            "mensual en PDF (Figura {ref:f_reporte}) es el documento que el bodeguero usa para declarar o comparte con su "
            "contador: resume el monto determinante, la categoría, la cuota y el vencimiento, lista todas las facturas y adjunta "
            "sus imágenes. La exportación de datos (P20, Figura {ref:f_p20}) genera un archivo ZIP con el contenido de la Tabla "
            "{ref:t_exportacion}: los CSV van en UTF-8 y separados por punto y coma para abrirlos directamente en Excel o "
            "LibreOffice, de modo que el titular o su contador pueden analizar los datos en una PC sin que la aplicación los "
            "envíe a ningún servidor."))
    d.figura("f_reporte", "reporte_mensual_pdf.png", "Reporte mensual de compras en PDF (página 1)", ancho_cm=12.5)
    d.figura("f_p20", "pantalla_20_exportar_datos.png", "Pantalla de exportación de datos (P20)", ancho_cm=6.2)
    d.tabla("t_exportacion", "Contenido del archivo de exportación", ["Archivo", "Contenido", "Uso en la PC"],
            [["facturas.csv", "Una fila por factura: fecha, RUC y razón social del emisor, serie, número, moneda, importe, origen (IA o manual) y estado", "Filtrar, sumar y revisar las compras"],
             ["periodos.csv", "Una fila por mes: total de compras, total de ventas y estado (abierto o cerrado)", "Comparar meses"],
             ["determinaciones.csv", "Monto determinante, categoría, cuota, nivel de alerta y fecha de vencimiento por mes", "Revisar la declaración"],
             ["imagenes/ (opcional)", "Una imagen JPEG por factura, nombrada con su serie y número", "Sustento de cada compra"],
             ["Reporte PDF (opcional)", "Reporte mensual del período exportado", "Archivo o impresión"]],
            [2.6, 5.0, 2.4])
    d.add(h3("3.6.9. Validación del prototipo"))
    d.add(p("El prototipo se validó en dos niveles. Primero, el equipo realizó una evaluación heurística con las diez "
            "heurísticas de Nielsen y un recorrido cognitivo de las seis tareas de la Tabla {ref:t_flujos}, verificando que "
            "cada paso tenga una pantalla, una acción visible y una respuesta del sistema. El resultado y las mejoras que "
            "introdujo en el diseño están en el Anexo S. Segundo, se definió el protocolo de prueba de usabilidad con tres "
            "titulares de bodega sin formación contable, que se aplicará sobre el prototipo navegable en la iteración I3 con "
            "las metas de la Tabla {ref:t_metas_usabilidad}."))
    d.tabla("t_metas_usabilidad", "Metas de la prueba de usabilidad del prototipo",
            ["Métrica", "Meta", "Requerimiento"],
            [["Tareas completadas sin ayuda", "≥ 5 de 6 tareas por participante", "RNF-11"],
             ["Toques para registrar una factura", "≤ 3 desde Inicio", "RNF-09"],
             ["Tiempo para registrar una factura (sin lectura)", "≤ 30 s", "RNF-09"],
             ["Errores de interpretación de la categoría", "0", "RF-10, RNF-11"],
             ["Satisfacción (escala SUS)", "≥ 70 puntos", "RNF-11"]],
            [4.2, 3.4, 2.0])
    d.add(h3("3.6.10. Cobertura del 100 % del alcance funcional"))
    d.add(p("La Tabla {ref:t_cobertura_rf} verifica que cada requerimiento funcional tiene al menos una pantalla que lo "
            "implementa. Los 22 requerimientos están cubiertos, lo que representa el 100 % del alcance funcional comprometido."))
    cobertura = [
        ("RF-01", "Capturar con guía de encuadre", "P05, P06, P19"), ("RF-02", "Evaluar nitidez e iluminación", "P06"),
        ("RF-03", "Importar desde la galería", "P05, P06"), ("RF-04", "Extraer campos en el teléfono", "P07 (P03 prepara el modelo)"),
        ("RF-05", "Mostrar campos y resaltar baja confianza", "P08"), ("RF-06", "Validar RUC y fecha", "P01, P08, P19"),
        ("RF-07", "Detectar duplicados", "P09"), ("RF-08", "Acumular el total mensual", "P04, P10, P11"),
        ("RF-09", "Registrar ventas del mes", "P13"), ("RF-10", "Determinar la categoría", "P04, P14"),
        ("RF-11", "Calcular la cuota", "P04, P14"), ("RF-12", "Avisos al 80 % y 100 % y topes", "P04, P10, P14, P17"),
        ("RF-13", "Calcular el vencimiento", "P04, P15"), ("RF-14", "Notificar el vencimiento", "P15 y notificación local"),
        ("RF-15", "Generar el reporte PDF", "P16 y reporte PDF"), ("RF-16", "Compartir el reporte", "P16"),
        ("RF-17", "Cerrar el mes y consultar el histórico", "P17"), ("RF-18", "Anular una factura", "P12"),
        ("RF-19", "Configurar contribuyente y acceso", "P01, P02, P18"), ("RF-20", "Instalar y actualizar el modelo", "P03, P18"),
        ("RF-21", "Exportar los datos para PC", "P17, P18, P20"), ("RF-22", "Aplicar los parámetros del NRUS vigentes", "P14, P18")]
    d.tabla("t_cobertura_rf", "Cobertura del alcance funcional en el prototipo",
            ["RF", "Requerimiento", "Pantallas", "Cubierto"],
            [[a, b, c, "Sí"] for a, b, c in cobertura] + [["Total", "22 requerimientos funcionales", "", "22 / 22 (100 %)"]],
            [1.0, 4.2, 3.4, 1.4], centrar=(3,), resaltar_filas=(22,))
    d.add(h3("3.6.11. Cobertura del alcance no funcional"))
    rnf = [
        ("RNF-01 a RNF-03", "Exactitud de extracción y del total", "P08 obliga a revisar; campo_extraido guarda leído y final para medir la exactitud en uso"),
        ("RNF-04", "Lectura ≤ 20 s", "P07 muestra progreso y tiempo restante; imagen a 896 px"),
        ("RNF-05 a RNF-07", "Memoria, tamaño de la app y compatibilidad", "P03 comprueba RAM y espacio; modelo descargado aparte; P19 si no alcanza"),
        ("RNF-08", "Reporte legible en cualquier equipo", "Reporte en PDF/A (P16)"),
        ("RNF-09", "≤ 3 toques para registrar", "Botón central Escanear → disparar → Guardar"),
        ("RNF-10", "Uso con una mano", "Objetivos ≥ 48 dp, texto de datos ≥ 16 sp, acciones en la mitad inferior"),
        ("RNF-11", "Lenguaje sencillo", "«Compras», «ventas», «le corresponde»; sin términos contables"),
        ("RNF-12", "Funciona sin conexión", "Todo el flujo principal es local; la red solo para descargar el modelo"),
        ("RNF-13", "Ningún registro se pierde", "Guardado transaccional al pulsar Guardar (P08)"),
        ("RNF-14 y RNF-15", "Privacidad y base cifrada", "Mensajes en P01, P07, P16 y P20 (archivo sin cifrar); SQLCipher y PIN o huella (P02)"),
        ("RNF-16", "Ningún dato sale por la red", "Sin servidor ni cuentas; la única conexión es la descarga del modelo (P03)"),
        ("RNF-17 a RNF-19", "Mantenibilidad y portabilidad", "Versión de parámetros visible (P14, P18); versión del modelo (P18)")]
    d.tabla("t_cobertura_rnf", "Cobertura del alcance no funcional en el diseño",
            ["RNF", "Cualidad", "Cómo la cubre el diseño"], [list(r) for r in rnf], [1.8, 2.8, 5.4])

    # ---------------- 3.7 ----------------
    d.add(h2("3.7. Validación de la solución"))
    d.add(p("La validación comprueba, antes de construir, que el diseño resuelve el problema planteado y cubre el alcance "
            "comprometido. Se realiza con matrices de relación que encadenan el problema, los objetivos, los requerimientos y "
            "los elementos del diseño."))
    d.add(h3("3.7.1. Relación problema - objetivos - solución"))
    d.tabla("t_prob_obj", "Relación entre causas del problema, objetivos y solución",
            ["Causa o efecto del problema", "Objetivo", "Elemento de la solución"],
            [["Las facturas no se registran al recibirlas", "OE1, OE5", "Registro por foto (P05–P10) y proceso TO-BE"],
             ["Cada emisor imprime un formato distinto", "OE2, OE3", "Gemma 4 E2B con ajuste fino sobre facturas peruanas"],
             ["No hay señal estable en el mostrador", "OE3, OE5", "Inferencia local con LiteRT-LM"],
             ["El total se suma a mano y con errores", "OE4", "Acumulado automático y motor de reglas"],
             ["Ante la duda se declara la categoría superior", "OE4", "Determinación con el monto real y avisos"],
             ["Las facturas se pierden", "OE5, OE6", "Imagen cifrada, reporte PDF y exportación de datos"],
             ["No se sabe si la solución funciona", "OE7", "Métricas de exactitud, tiempo y total mensual"]],
            [3.8, 1.4, 4.6])
    d.add(h3("3.7.2. Relación objetivos - funcionalidades"))
    d.tabla("t_obj_func", "Relación entre objetivos específicos y funcionalidades diseñadas",
            ["Objetivo", "Funcionalidades", "Evidencia de diseño"],
            [["OE1", "Proceso actual y propuesto", "Figuras {ref:f_asis} a {ref:f_registro}"],
             ["OE2", "Conjunto etiquetado con los 7 campos", "Esquema de campo_extraido y enumeración CampoFactura"],
             ["OE3", "Modelo ajustado, convertido y publicado", "Subproceso de publicación (Figura {ref:f_modelo}), P03"],
             ["OE4", "Acumulado, categoría, cuota, avisos y vencimiento", "MotorReglasNRUS, P04, P13–P15"],
             ["OE5", "App Android en Java sin conexión", "Arquitectura (Figura {ref:f_arquitectura}), prototipo P01–P20"],
             ["OE6", "Exportación de datos y parámetros versionados en la app", "P20, contenido del archivo (Tabla {ref:t_exportacion}), Anexo N"],
             ["OE7", "Medición de exactitud, tiempo y total", "Metas de la Tabla {ref:t_verif_rnf}"]],
            [1.2, 4.0, 4.6], centrar=(0,))
    d.add(h3("3.7.3. Relación requerimientos - funcionalidades"))
    d.tabla("t_req_func", "Relación entre grupos de requerimientos y componentes del diseño",
            ["Requerimientos", "Caso de uso", "Pantallas", "Clases principales", "Tablas"],
            [["RF-01 a RF-04", "CU-01", "P05–P07", "CapturaActivity, ExtractorGemmaLocal", "imagen_factura, campo_extraido"],
             ["RF-05 a RF-07", "CU-02", "P08, P09", "VerificacionViewModel, ValidadorRuc, DetectorDuplicados", "factura_compra, emisor"],
             ["RF-08 a RF-11", "CU-03 a CU-05", "P04, P13, P14", "MotorReglasNRUS, DeterminarCategoriaUseCase", "periodo, determinacion"],
             ["RF-12 a RF-14", "CU-06, CU-07", "P14, P15", "MotorReglasNRUS, RecordatorioWorker", "aviso, cronograma_venc"],
             ["RF-15 a RF-17", "CU-08, CU-11", "P16, P17", "GenerarReporteUseCase, CerrarPeriodoUseCase", "reporte_mensual, periodo"],
             ["RF-18", "CU-12", "P12", "FacturaCompra.anular()", "factura_compra"],
             ["RF-19", "CU-10", "P01, P02, P18", "Contribuyente, GestorAcceso", "contribuyente"],
             ["RF-20 a RF-22", "CU-05, CU-09, CU-11", "P03, P18, P20", "GestorModeloLocal, ExportadorDatos", "modelo_local, parametro_version, periodo"]],
            [1.6, 1.4, 1.4, 3.3, 2.5])
    d.add(h3("3.7.4. Matriz de cobertura del alcance"))
    d.tabla("t_matriz_alcance", "Matriz de cobertura del alcance funcional comprometido",
            ["Ítem del alcance (1.2.3)", "RF", "Proceso", "Pantallas", "Datos", "Estado"],
            [["Captura con guía y nitidez", "RF-01 a RF-03", "Registro", "P05, P06", "imagen_factura", "Diseñado"],
             ["Extracción en el teléfono sin conexión", "RF-04", "Registro", "P07", "campo_extraido", "Diseñado"],
             ["Revisión y corrección de campos", "RF-05 a RF-07", "Registro", "P08, P09", "factura_compra", "Diseñado"],
             ["Acumulado mensual", "RF-08", "Registro", "P04, P10, P11", "periodo", "Diseñado"],
             ["Registro del total de ventas", "RF-09", "Cierre", "P13", "periodo", "Diseñado"],
             ["Categoría, cuota, avisos y vencimiento", "RF-10 a RF-14", "Cierre", "P14, P15", "determinacion, aviso", "Diseñado"],
             ["Reporte, cierre e histórico", "RF-15 a RF-18", "Cierre", "P12, P16, P17", "reporte_mensual", "Diseñado"],
             ["Modelo en el teléfono y exportación de datos", "RF-19 a RF-22", "Modelo", "P01–P03, P18, P20", "modelo_local, parametro_version", "Diseñado"]],
            [3.0, 1.5, 1.2, 1.7, 2.0, 1.2], centrar=(5,))
    d.add(p("El detalle por requerimiento, con su caso de uso, proceso BPMN, pantalla, clase y tabla, está en el Anexo R."))
    d.add(h3("3.7.5. Verificación de funcionalidades del prototipo"))
    d.add(p("Cada funcionalidad se verificó recorriendo el prototipo con un escenario del caso simulado. La verificación "
            "confirma que el diseño contiene todas las pantallas, datos y respuestas necesarias; la prueba con usuarios y la "
            "prueba de la aplicación construida se realizarán en las iteraciones siguientes."))
    d.tabla("t_verif_func", "Verificación de funcionalidades sobre el prototipo",
            ["Escenario", "Datos de prueba", "Resultado esperado", "Pantallas", "Verificado"],
            [["Registrar factura legible", "F001-004821 · S/ 1 450,00", "Campos leídos; total del mes S/ 4 120,50", "P06–P10", "Sí"],
             ["Importe con baja confianza", "Confianza 62 % en importe_total", "Campo en ámbar con indicación de revisar", "P08", "Sí"],
             ["Factura duplicada", "Misma serie y número", "Aviso y no se registra", "P09", "Sí"],
             ["Equipo sin memoria suficiente", "Teléfono de 4 GB", "Se ofrece registro manual validado", "P03, P19", "Sí"],
             ["Superar el 80 % del límite", "Compras S/ 4 120,50 > S/ 4 000", "Barra y aviso en ámbar", "P04, P10, P14", "Sí"],
             ["Categoría por el mayor monto", "Compras 4 120,50; ventas 3 900,00", "Categoría 1, cuota S/ 20", "P14", "Sí"],
             ["Vencimiento según el RUC", "Último dígito 4", "Fecha límite y recordatorios", "P15", "Sí"],
             ["Anular factura", "F002-000915 registrada por error", "Se descuenta S/ 386,40 y se recalcula", "P12", "Sí"],
             ["Cerrar el mes", "Septiembre 2026", "Mes cerrado en el historial", "P17", "Sí"],
             ["Exportar el mes", "Septiembre 2026 con imágenes", "ZIP que se abre en Excel; facturas.csv suma S/ 4 120,50", "P20", "Sí"]],
            [2.3, 2.4, 2.9, 1.3, 1.1], centrar=(4,))
    d.add(h3("3.7.6. Verificación de requerimientos no funcionales"))
    d.tabla("t_verif_rnf", "Verificación de los requerimientos no funcionales en el diseño y medición prevista",
            ["RNF", "Verificación en el diseño", "Medición prevista", "Cuándo"],
            [["RNF-01, RNF-02", "Conjunto de prueba separado; evaluación contra el modelo base", "Exactitud ≥ 90 % por campo", "APF3"],
             ["RNF-03", "Motor de reglas con BigDecimal", "Diferencia < 2 % en el mes simulado", "Final"],
             ["RNF-04, RNF-05", "Imagen a 896 px; motor LiteRT-LM en GPU si existe", "≤ 20 s y ≤ 2 GB en el equipo de referencia", "APF3"],
             ["RNF-06, RNF-07", "Modelo como descarga separada; minSdk 26", "APK ≤ 60 MB; prueba en Android 8 y 13", "APF3"],
             ["RNF-09 a RNF-11", "Recorrido cognitivo y heurísticas (Anexo S)", "Prueba con 3 titulares (Tabla {ref:t_metas_usabilidad})", "I3"],
             ["RNF-12, RNF-14", "Arquitectura sin llamadas de red en el flujo principal", "Prueba en modo avión; inspección de tráfico", "APF3"],
             ["RNF-13", "Guardado en una transacción", "Prueba de cierre forzado durante el guardado", "APF3"],
             ["RNF-15, RNF-16", "SQLCipher, Keystore y PIN con hash; sin servidor; solo descarga del modelo por TLS", "Revisión con OWASP MASVS e inspección de tráfico", "Final"],
             ["RNF-17 a RNF-19", "Parámetros y modelo versionados; dominio aislado", "Cobertura de pruebas del motor ≥ 80 %", "APF3"]],
            [1.8, 3.6, 3.2, 1.0], centrar=(3,))


# ======================================================================
def capitulo4(d):
    d.add(h1("CAPÍTULO 4: CRONOGRAMA Y PRESUPUESTO"))
    d.add(h2("4.1. Cronograma actualizado"))
    d.add(p("La Figura {ref:f_gantt} presenta el cronograma actualizado al corte de la semana 8. Las actividades de análisis y "
            "diseño están completas; la recopilación de facturas y la construcción del front-end están en curso, y el ajuste "
            "fino con conversión del modelo inicia en la semana 9. No hay desviaciones respecto de la línea base del Anexo D: "
            "la construcción de la app se adelantó una semana para iniciar el front-end en paralelo al cierre del diseño."))
    d.figura("f_gantt", "gantt_actualizado.png", "Cronograma actualizado del proyecto al corte de la semana 8")
    d.tabla("t_proximas", "Actividades de las semanas 9 a 18",
            ["Semanas", "Actividad", "Responsable", "Entregable"],
            [["9–10", "Front-end navegable (P01–P20) y dominio con pruebas unitarias del motor NRUS", "Coronel Obregón", "Incremento 1"],
             ["9–12", "Ajuste fino, evaluación, conversión a .litertlm y pruebas en el teléfono", "Damián Valdivia", "Modelo v1 convertido"],
             ["11–12", "Room con SQLCipher, repositorios e integración del puente LiteRT-LM", "Coronel Obregón", "APF3"],
             ["11–12", "Prueba de usabilidad del prototipo con tres titulares", "Gonzales Maco", "Informe de usabilidad"],
             ["13–14", "Reporte PDF, exportación de datos e historial", "Coronel Obregón, Mateo Velásquez", "Incremento 3"],
             ["12–16", "Pruebas de integración y validación en mes simulado", "Gonzales Maco", "Resultados de validación"],
             ["17–18", "Informe final y sustentación", "Mateo Velásquez", "Informe final"]],
            [1.1, 5.0, 2.3, 1.8], centrar=(0,))
    d.add(h2("4.2. Presupuesto del proyecto"))
    d.add(p("El presupuesto distingue el desembolso real del proyecto del valor de los recursos que aporta el equipo. Los montos "
            "en dólares se convierten con un tipo de cambio referencial de S/ 3,75 por dólar; los precios de servicios en la "
            "nube son estimaciones para la escala del proyecto y se ajustarán a la cotización vigente al contratarlos."))
    d.tabla("t_presupuesto", "Presupuesto estimado del proyecto (18 semanas)",
            ["Rubro", "Detalle", "Cantidad", "Costo unitario (S/)", "Total (S/)", "Tipo"],
            [["Recursos humanos", "4 integrantes × 10 h por semana × 18 semanas", "720 h", "25,00", "18 000,00", "Aporte del equipo"],
             ["Hardware", "Teléfono Android de gama media (6 GB) para pruebas", "1", "1 100,00", "1 100,00", "Aporte (equipo existente)"],
             ["Hardware", "Computadoras de desarrollo", "4", "0,00", "0,00", "Aporte (existentes)"],
             ["Software", "Android Studio, JDK, Git, DB Browser for SQLite, Figma o Penpot (plan gratuito)", "—", "0,00", "0,00", "Libre o gratuito"],
             ["Cómputo", "Google Colab Pro para el ajuste fino (opcional)", "2 meses", "38,00", "76,00", "Desembolso"],
             ["Alojamiento", "Repositorio público del modelo en Hugging Face", "—", "0,00", "0,00", "Gratuito"],
             ["Distribución", "Cuenta de desarrollador de Google Play (USD 25, pago único)", "1", "94,00", "94,00", "Desembolso"],
             ["Materiales", "Impresión de formatos de prueba y movilidad a bodegas", "—", "50,00", "50,00", "Desembolso"],
             ["Contingencia", "10 % del desembolso", "—", "—", "22,00", "Desembolso"],
             ["Total desembolso", "", "", "", "242,00", ""],
             ["Total con aportes", "", "", "", "19 342,00", ""]],
            [1.6, 3.8, 1.1, 1.3, 1.3, 1.6], centrar=(2, 3, 4), resaltar_filas=(9, 10))
    d.add(h2("4.3. Recursos requeridos"))
    d.tabla("t_recursos", "Recursos requeridos por tipo",
            ["Tipo", "Recurso", "Uso"],
            [["Humanos", "Líder y responsable del modelo (Damián Valdivia)", "Gestión, datos de entrenamiento, ajuste fino y conversión"],
             ["Humanos", "Desarrollador de la app (Coronel Obregón)", "Construcción en Java de la app, la base local y la exportación"],
             ["Humanos", "Diseño y documentación (Mateo Velásquez)", "Prototipo, diagramas e informes"],
             ["Humanos", "Análisis y pruebas (Gonzales Maco)", "SRS, trazabilidad, pruebas y validación"],
             ["Hardware", "Teléfono de gama media con 6 GB de RAM; computadoras con 16 GB", "Pruebas de rendimiento; desarrollo"],
             ["Software", "Android Studio, JDK 17, SQLite y DB Browser for SQLite", "Desarrollo y revisión de la base local"],
             ["Servicios", "Google Colab con GPU, Hugging Face, Google Play", "Entrenamiento, alojamiento del modelo y distribución de la app"],
             ["Datos", "Facturas de compra reales anonimizadas de las bodegas de referencia", "Ajuste fino y conjunto de prueba"]],
            [1.4, 4.4, 4.2])
    d.add(h2("4.4. Costos de implementación"))
    d.add(p("La decisión de ejecutar el modelo en el teléfono define la estructura de costos de implementación. Para el "
            "bodeguero, la aplicación no tiene costo: usa el teléfono que ya tiene y no paga por factura procesada. Para el "
            "proyecto tampoco hay costo recurrente: no existe servidor y el modelo se aloja sin costo en un repositorio "
            "público; el único pago es la cuenta de desarrollador de Google Play. En la alternativa 2, en cambio, el costo crecería con el uso "
            "porque cada lectura consume tiempo de una GPU en la nube, y en la alternativa 3 se pagaría por cada documento."))
    d.tabla("t_costos_impl", "Costos de implementación y operación",
            ["Concepto", "Costo único (S/)", "Costo mensual (S/)", "Observación"],
            [["Publicación en Google Play", "94,00", "0,00", "Pago único"],
             ["Alojamiento y descarga del modelo", "0,00", "0,00", "Repositorio público; descarga única de ≈ 2,6 GB por teléfono"],
             ["Inferencia por factura", "0,00", "0,00", "Se ejecuta en el teléfono"],
             ["Servidor y base de datos remota", "0,00", "0,00", "No existen: los datos quedan en el teléfono"],
             ["Costo para la bodega", "0,00", "0,00", "App gratuita; requiere teléfono de gama media"],
             ["Total", "94,00", "0,00", "No depende del número de facturas ni de usuarios"]],
            [3.2, 1.6, 1.6, 3.6], centrar=(1, 2), resaltar_filas=(5,))


# ======================================================================
def capitulo5_y_conclusiones(d):
    d.e1_titulo(402, "CAPÍTULO 5: RESULTADOS")
    d.e1_titulo(403, "5.1. Resultados")
    d.add(p("Por tratarse del segundo avance, los resultados de esta etapa son los entregables de diseño que la consigna "
            "solicita, verificados contra el alcance comprometido. Los resultados cuantitativos del modelo y de la aplicación se "
            "reportarán en APF3 y en el informe final; no se adelantan cifras que aún no se han medido."))
    d.tabla("t_entregables", "Entregables del APF2 y su ubicación en el informe",
            ["Criterio de la consigna", "Entregable", "Ubicación", "Estado"],
            [["Análisis del contexto", "Contexto, visión, misión, entorno, estrategias, planes, canvas e impacto en los procesos", "Cap. 1 y Anexo A", "Completo"],
             ["Alternativas de solución", "3 alternativas profundizadas, cada una con ≥ 5 pantallas y ≥ 50 % Java", "3.2, Anexos E y F", "Completo"],
             ["Análisis de la solución", "Actores, 12 casos de uso, 22 RF y 19 RNF según SRS", "3.4, Anexos G a J", "Completo"],
             ["Diseño de la solución", "Charter, WBS, Gantt, 5 diagramas BPMN, 2 diagramas de clases, 1 DER de la base SQLite", "3.3, 3.5, Anexos B a D y K a N", "Completo"],
             ["Base de datos", "Diseño lógico y físico en SQLite con consideraciones de seguridad", "3.5.5, 3.5.6 y Anexo M", "Completo"],
             ["Diseño del prototipo", "20 pantallas móviles y reporte PDF con cobertura del 100 % de los RF", "3.6, Anexos O a Q", "Completo"],
             ["Validación", "Matrices de cobertura y verificación del prototipo", "3.7, Anexos R y S", "Completo"],
             ["Cronograma y presupuesto", "Gantt actualizado, presupuesto, recursos y costos", "Cap. 4", "Completo"],
             ["Documentación técnica", "Documento Markdown para desarrolladores", "Anexo T", "Completo"]],
            [2.4, 4.2, 2.2, 1.2], centrar=(3,))
    d.e1(408, 411)
    d.e1(412)
    d.add(p("Primera. El modelado BPMN confirmó que el problema de la bodega del NRUS es operativo: los tres puntos de error del "
            "proceso actual —guardar la factura sin registrarla, sumar a mano al cierre y estimar la categoría ante la duda— "
            "ocurren en tareas que el proceso propuesto elimina o automatiza."))
    d.add(p("Segunda. El diseño cubre el 100 % del alcance funcional: los 22 requerimientos funcionales tienen al menos una "
            "pantalla del prototipo, un caso de uso, una clase y una tabla que los implementan, y los 19 no funcionales se "
            "traducen en decisiones verificables de arquitectura, datos o interfaz."))
    d.add(p("Tercera. Toda la información vive en una sola base SQLite cifrada dentro del teléfono. Respecto del primer avance "
            "se retiró el servidor de respaldo: ningún sistema externo recibe ni solicita datos de la app, y el titular exporta "
            "sus datos a archivos CSV y PDF cuando quiere analizarlos en una PC, lo que refuerza la privacidad."))
    d.add(p("Cuarta. El prototipo reduce el registro de una factura a tres toques desde la pantalla de inicio y presenta la "
            "categoría y la cuota en lenguaje cotidiano; su validación con usuarios reales se realizará en la iteración I3 con "
            "metas definidas."))
    d.add(p("Quinta. La solución mantiene alrededor de 85 % de código Java y no tiene costo de operación mensual: la lectura se "
            "hace en el teléfono y no hay servidor que mantener."))
    d.add(p("Sexta. Los siguientes pasos, hacia APF3, son construir el front-end y el dominio a partir de la documentación "
            "técnica del Anexo T, ejecutar el ajuste fino con conversión a .litertlm y medir su exactitud y tiempo de lectura "
            "en el equipo de referencia."))


# ======================================================================
def anexos(d):
    d.e1(419)
    d.add(p("Los anexos A a J reúnen las evidencias del primer avance, actualizadas donde corresponde. Los anexos K a T contienen "
            "las evidencias del segundo avance. Las imágenes originales y sus fuentes editables están en la carpeta "
            "entregable2_archivos que acompaña al informe."))
    # A
    d.add(h2("Anexo A: Business Model Canvas"))
    d.e1(421)
    d.e1_estilo(422, "Ttulo3", "A.1. Lean Canvas")
    d.e1(423, 425, reemplazos=[("Desarrollo y mantenimiento de la app Android y del servicio de respaldo.", "Desarrollo y mantenimiento de la app Android."),
                               ("Plan voluntario de respaldo en la nube e histórico ampliado.", "Talleres de capacitación para gremios de bodegueros.")])
    d.e1_estilo(426, "Ttulo3", "A.2. Business Model Canvas")
    d.e1(427, 429, reemplazos=[("plan voluntario de respaldo en la nube;", "talleres de capacitación para gremios de bodegueros;"),
                               ("Desarrollo y mantenimiento de la app Android y del servicio de respaldo;", "Desarrollo y mantenimiento de la app Android;")])
    # B
    d.add(h2("Anexo B: Project Charter", salto=True))
    d.e1(462, 464, reemplazos=[(TITULO_E1_CHARTER, TITULO),
                               ("(detalle en A3.4)", "(detalle en el Anexo D)"),
                               ("instalación del modelo y respaldo opcional.", "instalación del modelo y exportación de datos.")])
    # C
    d.add(h2("Anexo C: Estructura de Descomposición del Trabajo (WBS)", salto=True))
    d.e1(466)
    d.figura("f_wbs", "wbs.png", "Estructura de desglose del trabajo (WBS)")
    d.e1(470, 472, reemplazos=[("Servicio de respaldo Spring Boot", "Exportación de datos para PC")])
    # D
    d.add(h2("Anexo D: Diagrama de Gantt (línea base) y registro de riesgos", salto=True))
    d.add(h3("D.1. Diagrama de Gantt de línea base"))
    d.e1(474, 477)
    d.add(h3("D.2. Registro de riesgos"))
    d.e1(482, 484, reemplazos=[("modo manual de respaldo.", "modo de registro manual.")])
    # E
    d.add(h2("Anexo E: Alternativas de solución (fichas técnicas)", salto=True))
    d.add(p("Fichas técnicas de las tres alternativas elaboradas en el primer avance. Su descripción profundizada está en el "
            "apartado 3.2."))
    d.add(h3("E.1. Alternativa 1 (seleccionada)"))
    d.e1(433, 435, reemplazos=[("· respaldo opcional en Java con Spring Boot y Oracle o SQL Server", "· exportación de datos a CSV y PDF; sin servidor propio")])
    d.add(h3("E.2. Alternativa 2"))
    d.e1(438, 440)
    d.add(h3("E.3. Alternativa 3"))
    d.e1(448, 450)
    # F
    d.add(h2("Anexo F: Pantallas de las alternativas", salto=True))
    d.add(p("Las 20 pantallas de la alternativa 1 se presentan en el apartado 3.6. A continuación se incluyen las pantallas de "
            "las alternativas 2 (siete pantallas) y 3 (seis pantallas)."))
    d.e1(441, 446)
    d.e1(451, 453)
    # G
    d.add(h2("Anexo G: Casos de uso", salto=True))
    d.add(p("Especificación de los casos de uso que complementan a CU-01 y CU-09 (apartado 3.4.4)."))
    especificaciones = [
        ("CU-02 Verificar y corregir los datos extraídos", [
            ["Actores", "Bodeguero (principal)"],
            ["Precondición", "CU-01 devolvió los campos con su confianza y la imagen de la factura."],
            ["Flujo básico", "1. El sistema muestra la imagen y los siete campos. 2. Marca en ámbar los de confianza menor al 80 % y en verde los validados. 3. El bodeguero compara y corrige los que no coinciden. 4. El sistema revalida RUC y fecha. 5. El bodeguero pulsa Guardar."],
            ["Flujos alternos", "3a. El RUC corregido no pasa el módulo 11: el campo se marca en rojo y no se permite guardar. 5a. El bodeguero descarta: se vuelve a la cámara."],
            ["Postcondición", "Los valores finales quedan listos para guardar; se conserva el valor leído y el corregido."],
            ["Requerimientos", "RF-05, RF-06; RNF-01, RNF-10, RNF-11"]]),
        ("CU-03 Ingresar el total de ventas del mes", [
            ["Actores", "Bodeguero (principal)"],
            ["Precondición", "Existe un período abierto."],
            ["Flujo básico", "1. El bodeguero abre Ventas del mes. 2. El sistema explica que la categoría usa el mayor monto. 3. El bodeguero ingresa el total. 4. El sistema guarda y ejecuta CU-05."],
            ["Flujos alternos", "3a. Monto negativo o vacío: se pide corregir. 4a. El mes está cerrado: el campo es de solo lectura."],
            ["Postcondición", "El período tiene total de ventas y la determinación está actualizada."],
            ["Requerimientos", "RF-09, RF-10; RNF-11"]]),
        ("CU-05 Calcular categoría y cuota del NRUS", [
            ["Actores", "Motor de reglas NRUS (sistema)"],
            ["Precondición", "Hay parámetros NRUS vigentes para la fecha del período."],
            ["Flujo básico", "1. Obtiene el total de adquisiciones y de ventas. 2. Toma el mayor como monto determinante. 3. Busca la categoría vigente que lo admite. 4. Asigna la cuota. 5. Calcula el nivel de alerta (80 % y 100 %). 6. Calcula el vencimiento con el último dígito del RUC. 7. Guarda la determinación."],
            ["Flujos alternos", "3a. El monto supera la última categoría o los topes anuales: nivel FUERA_DE_REGIMEN y alerta de cambio de régimen."],
            ["Postcondición", "El período tiene una determinación con categoría, cuota, alerta y vencimiento."],
            ["Requerimientos", "RF-10 a RF-13, RF-22; RNF-03, RNF-17, RNF-18"]]),
        ("CU-08 Generar y exportar el reporte mensual", [
            ["Actores", "Bodeguero (principal); Contador externo (receptor)"],
            ["Precondición", "El período tiene al menos una factura y una determinación."],
            ["Flujo básico", "1. El bodeguero abre Reporte del mes. 2. El sistema genera el PDF con el resumen, el detalle y las imágenes. 3. Calcula su SHA-256. 4. El bodeguero elige compartir o guardar."],
            ["Flujos alternos", "2a. Faltan las ventas del mes: se advierte y se permite generar un reporte preliminar."],
            ["Postcondición", "Queda un reporte registrado y, si se eligió, compartido por los medios del teléfono."],
            ["Requerimientos", "RF-15, RF-16; RNF-08, RNF-14"]]),
        ("CU-12 Anular una factura registrada por error", [
            ["Actores", "Bodeguero (principal)"],
            ["Precondición", "La factura está vigente y su período está abierto."],
            ["Flujo básico", "1. El bodeguero abre el detalle y elige Anular. 2. El sistema muestra el monto que se descontará. 3. El bodeguero elige el motivo. 4. El sistema marca la factura como ANULADA y ejecuta CU-05."],
            ["Flujos alternos", "1a. El período está cerrado: la anulación no está disponible."],
            ["Postcondición", "La factura sigue registrada como anulada y el acumulado se recalculó."],
            ["Requerimientos", "RF-18, RF-08; RNF-13"]]),
        ("CU-11 Consultar el histórico de meses cerrados y exportar los datos", [
            ["Actores", "Bodeguero (principal); Contador externo (receptor del archivo)"],
            ["Precondición", "Existe al menos un período registrado."],
            ["Flujo básico", "1. El bodeguero abre el historial. 2. El sistema lista los meses con sus compras, categoría y estado, y el acumulado anual frente al tope. 3. El bodeguero elige Exportar datos. 4. Elige un mes o el año y si incluye las imágenes y el reporte PDF. 5. El sistema genera el ZIP con los CSV y los archivos elegidos. 6. El bodeguero lo guarda con el selector de archivos o lo comparte."],
            ["Flujos alternos", "5a. No hay espacio suficiente: se ofrece exportar sin imágenes. 6a. El bodeguero cancela: no se guarda ningún archivo."],
            ["Postcondición", "El archivo queda donde el titular eligió; la app no lo envía a ningún servidor."],
            ["Requerimientos", "RF-17, RF-21; RNF-14, RNF-16"]]),
    ]
    for i, (titulo, filas) in enumerate(especificaciones):
        d.tabla(f"t_cu_{i}", f"Especificación del caso de uso {titulo}", None, filas, [2.0, 8.0])
    # H
    d.add(h2("Anexo H: Requerimientos funcionales", salto=True))
    d.e1(500, 502, reemplazos=[
        ("Descargar, verificar (SHA-256) e instalar el modelo Gemma 4 ajustado en el teléfono, y actualizarlo por Wi-Fi cuando exista una versión nueva.",
         "Descargar una sola vez, verificar (SHA-256) e instalar el modelo Gemma 4 ajustado desde el repositorio público indicado por la app, y actualizarlo cuando una nueva versión de la app traiga un modelo nuevo."),
        ("Respaldar de forma opcional y cifrada los registros en el servidor y restaurarlos en otro teléfono.",
         "Exportar los registros de un mes o del año a archivos abiertos (CSV, imágenes y reporte PDF en un ZIP) que el usuario guarda o comparte para analizarlos en una PC."),
        ("Baja", "Media", "RF-21"),
        ("Actualizar los parámetros del NRUS (límites, cuotas, cronograma) desde la configuración publicada.",
         "Aplicar los parámetros del NRUS (límites, cuotas, cronograma) incluidos en la versión instalada de la app; una versión nueva aplica a períodos nuevos, no a los cerrados.")])
    # I
    d.add(h2("Anexo I: Requerimientos no funcionales", salto=True))
    d.e1(504, 506, reemplazos=[
        ("Las imágenes y los datos tributarios no salen del teléfono salvo respaldo explícito.",
         "Las imágenes y los datos tributarios no salen del teléfono salvo que el usuario los exporte o comparta."),
        ("Respaldo cifrado en tránsito y en reposo.", "Ningún dato del usuario se envía por red; la única conexión es la descarga del modelo."),
        ("TLS 1.2+; AES-256 en el servidor", "Cero envíos de datos; descarga por TLS 1.2+ verificada con SHA-256"),
        ("Configuración versionada, sin recompilar", "Archivo versionado; cambiarlo no modifica código Java")])
    # J
    d.add(h2("Anexo J: Especificación de Requerimientos de Software (SRS) e instrumentos", salto=True))
    d.e1_estilo(486, "Ttulo3", "J.1. Levantamiento de información")
    d.e1(487, reemplazos=[("Los instrumentos están en el Anexo 6.", "Los instrumentos están en los apartados J.3 a J.5.")])
    d.e1_estilo(507, "Ttulo3", "J.2. Matriz de trazabilidad")
    d.e1(508, 510, reemplazos=[("Diagrama de proceso (Anexo 5.1)", "Diagramas BPMN (3.5.3)")])
    d.e1(528)
    d.e1_estilo(529, "Ttulo3", "J.3. Guía de entrevista estructurada al titular")
    d.e1(530, 536)
    d.e1_estilo(537, "Ttulo3", "J.4. Ficha de observación del proceso")
    d.e1(538, 541)
    d.e1_estilo(542, "Ttulo3", "J.5. Registro de evidencias adjuntas")
    d.e1(543, 545)
    # K
    d.add(h2("Anexo K: Diagramas de procesos", salto=True))
    d.add(p("Además de los diagramas del apartado 3.5.3, se modelaron los subprocesos de cierre mensual y de publicación y "
            "actualización del modelo. En el cierre mensual, un evento de temporizador cinco días antes del vencimiento inicia "
            "el recordatorio; la compuerta «¿Supera los topes del régimen?» dispara la alerta de cambio de régimen y la "
            "compuerta «¿Compartir con el contador?» envía el reporte como flujo de mensaje al contador externo. En la "
            "publicación del modelo, la compuerta «¿Mejora y ≥ 90 %?» impide publicar un modelo que no supere al base; publicar "
            "consiste en subir el archivo al repositorio público y lanzar una versión de la app con el nuevo manifiesto, sin "
            "servidor propio; y en "
            "el teléfono la verificación de SHA-256 y la prueba con una factura de control protegen la versión instalada."))
    d.figura("f_cierre", "bpmn_04_cierre.png", "Subproceso «Cierre mensual y declaración»")
    d.figura("f_modelo", "bpmn_05_modelo.png", "Subproceso «Publicar y actualizar el modelo de IA»")
    d.tabla("t_elementos_bpmn", "Elementos BPMN utilizados en cada diagrama",
            ["Diagrama", "Eventos", "Compuertas", "Otros elementos"],
            [["AS-IS", "Inicio, mensaje, temporizador (fin de mes), fin", "2 exclusivas y sus uniones", "3 pools, almacén de datos, anotaciones de error"],
             ["TO-BE", "Mensaje, temporizador (5 días antes), fin", "1 exclusiva y su unión", "2 carriles en la bodega, almacén, anotaciones de mejora"],
             ["Registrar factura", "Inicio, 2 fines", "4 exclusivas", "4 carriles, objeto de datos, almacén"],
             ["Cierre mensual", "Temporizador, fin", "2 exclusivas", "Flujo de mensaje al contador, almacén"],
             ["Publicar modelo", "2 inicios, 3 fines", "3 exclusivas", "3 pools, flujos de mensaje, almacén"]],
            [2.0, 3.0, 2.2, 3.4])
    # L
    d.add(h2("Anexo L: Diagrama de clases (diccionario de clases)", salto=True))
    clases = [
        ("Contribuyente", "dominio.modelo", "Titular del RUC; conoce su último dígito para el vencimiento."),
        ("PeriodoMensual", "dominio.modelo", "Mes de declaración; calcula el total de adquisiciones y se cierra."),
        ("FacturaCompra", "dominio.modelo", "Comprobante de compra; se anula con motivo y tiene clave única."),
        ("Emisor", "dominio.modelo", "Proveedor que emite la factura; valida su RUC."),
        ("ImagenFactura", "dominio.modelo", "Foto cifrada de la factura con su nitidez."),
        ("ResultadoExtraccion / CampoExtraido", "dominio.modelo", "Lo que leyó el modelo y lo que confirmó el usuario, por campo."),
        ("Determinacion", "dominio.modelo", "Resultado de aplicar la norma: categoría, cuota, alerta y vencimiento."),
        ("CategoriaNRUS / CronogramaVencimiento", "dominio.modelo", "Tabla de categorías y fechas límite de una versión de parámetros."),
        ("ParametrosNrus", "dominio.modelo", "Versión de la norma vigente con umbral de aviso y tope anual."),
        ("ModeloLocal", "dominio.modelo", "Versión del modelo instalada con su SHA-256 y estado."),
        ("Aviso / ReporteMensual", "dominio.modelo", "Recordatorios programados y reportes generados."),
        ("RegistrarFacturaUseCase", "dominio.casosuso", "Orquesta extracción, validación, duplicado, guardado y recálculo."),
        ("DeterminarCategoriaUseCase", "dominio.casosuso", "Obtiene el período y aplica el motor de reglas."),
        ("MotorReglasNRUS", "dominio.reglas", "Aplica la norma: mayor monto, categoría, cuota, avisos y vencimiento."),
        ("ValidadorRuc / DetectorDuplicados", "dominio.reglas", "Validación módulo 11 y búsqueda por emisor, serie y número."),
        ("ExtractorFacturas / FacturaRepositorio", "dominio.puertos", "Interfaces que el dominio exige a inferencia y datos."),
        ("ExtractorGemmaLocal", "inferencia", "Prepara la imagen, arma la instrucción y parsea el JSON del modelo."),
        ("LiteRtLmPuente (Kotlin)", "inferencia.litert", "Envuelve Engine y Conversation de LiteRT-LM tras la interfaz MotorInferencia."),
        ("GestorModeloLocal", "inferencia", "Comprueba memoria, descarga, verifica SHA-256 y activa versiones."),
        ("FacturaRepositorioRoom / FacturaDao", "datos", "Implementación Room del repositorio y consultas parametrizadas."),
        ("BaseDatosFacturas", "datos", "RoomDatabase abierta con la clave de SQLCipher del Keystore."),
        ("CapturaActivity / VerificacionViewModel", "ui", "Cámara con guía; estado de la verificación y acciones del usuario."),
        ("ExportarDatos / ExportadorDatos / EscritorCsv", "dominio.casosuso / exportacion", "Genera el ZIP con CSV, imágenes y PDF en el destino que elige el usuario.")]
    d.tabla("t_clases", "Diccionario de clases", ["Clase", "Paquete", "Responsabilidad"], [list(c) for c in clases], [3.2, 2.2, 4.8])
    # M
    d.add(h2("Anexo M: Diagrama entidad-relación, diccionario de datos y scripts", salto=True))
    d.add(h3("M.1. Diccionario de datos de las tablas principales"))
    dicc = [
        ("factura_compra", [["id_factura", "INTEGER", "PK", "Identificador"], ["id_periodo", "INTEGER", "FK", "Período del mes"],
                            ["id_emisor", "INTEGER", "FK", "Proveedor"], ["serie", "TEXT(4)", "UQ*", "Serie del comprobante"],
                            ["numero", "TEXT(8)", "UQ*", "Número correlativo"], ["fecha_emision", "TEXT", "", "Fecha ISO-8601 dentro del período"],
                            ["moneda", "TEXT(3)", "", "PEN o USD"], ["importe_total", "NUMERIC", "CHECK > 0", "Importe total"],
                            ["origen", "TEXT", "", "IA o MANUAL"], ["estado", "TEXT", "", "VIGENTE o ANULADA"],
                            ["motivo_anulacion", "TEXT", "", "Obligatorio si está anulada"]]),
        ("campo_extraido", [["id_campo", "INTEGER", "PK", "Identificador"], ["id_factura", "INTEGER", "FK", "Factura"],
                            ["id_modelo", "INTEGER", "FK", "Versión del modelo que leyó"], ["nombre_campo", "TEXT", "", "Uno de los 7 campos"],
                            ["valor_leido", "TEXT", "", "Salida del modelo"], ["valor_final", "TEXT", "", "Valor confirmado"],
                            ["confianza", "REAL", "0..1", "Confianza del modelo"], ["corregido", "INTEGER", "0|1", "El usuario lo cambió"]]),
        ("determinacion", [["id_determinacion", "INTEGER", "PK", "Identificador"], ["id_periodo", "INTEGER", "FK, UQ", "Período"],
                           ["id_categoria", "INTEGER", "FK", "Categoría aplicada"], ["total_adquisiciones", "NUMERIC", "", "Suma de facturas vigentes"],
                           ["monto_determinante", "NUMERIC", "", "Mayor entre compras y ventas"], ["cuota", "NUMERIC", "", "Cuota calculada"],
                           ["fecha_vencimiento", "TEXT", "", "Según cronograma y RUC"], ["nivel_alerta", "TEXT", "", "NINGUNA, AVISO_80, LIMITE_100, FUERA_DE_REGIMEN"]]),
        ("modelo_local", [["id_modelo", "INTEGER", "PK", "Identificador"], ["version", "TEXT", "UQ", "Versión del archivo del modelo"],
                          ["tamano_mb", "INTEGER", "", "Tamaño del archivo"], ["sha256", "TEXT(64)", "", "Huella publicada en el manifiesto"],
                          ["estado", "TEXT", "", "NO_INSTALADO, DESCARGANDO, VERIFICADO o ACTIVO"],
                          ["ruta", "TEXT", "", "Ubicación en el almacenamiento interno"], ["fecha_instalacion", "TEXT", "", "Fecha ISO-8601"]]),
    ]
    for i, (tabla_n, filas) in enumerate(dicc):
        d.tabla(f"t_dicc_{i}", f"Diccionario de datos de la tabla {tabla_n}", ["Columna", "Tipo", "Restricción", "Descripción"],
                filas, [2.4, 1.6, 1.6, 4.4])
    d.add(p("* UQ compuesta: la combinación de columnas marcadas es única."))
    d.add(h3("M.2. Script DDL de la base local (SQLite)"))
    d.add(p("Script de las 13 tablas de la base local en SQL de SQLite. Room crea las tablas, claves, índices y restricciones "
            "UNIQUE a partir de las entidades; las restricciones CHECK, que Room no declara, se aplican además en el dominio y se "
            "comprueban en las pruebas de migración. No existe otra base de datos en la solución."))
    d.add(codigo('''PRAGMA foreign_keys = ON;

CREATE TABLE contribuyente (
  id_contribuyente INTEGER PRIMARY KEY AUTOINCREMENT,
  ruc              TEXT NOT NULL UNIQUE CHECK (length(ruc) = 11),
  nombre           TEXT NOT NULL,
  titular          TEXT NOT NULL,
  ultimo_digito    INTEGER NOT NULL CHECK (ultimo_digito BETWEEN 0 AND 9),
  pin_hash         TEXT NOT NULL,
  pin_sal          TEXT NOT NULL,
  fecha_alta       TEXT NOT NULL);
CREATE TABLE periodo (
  id_periodo       INTEGER PRIMARY KEY AUTOINCREMENT,
  id_contribuyente INTEGER NOT NULL REFERENCES contribuyente(id_contribuyente),
  anio             INTEGER NOT NULL,
  mes              INTEGER NOT NULL CHECK (mes BETWEEN 1 AND 12),
  total_ventas     NUMERIC NOT NULL DEFAULT 0 CHECK (total_ventas >= 0),
  estado           TEXT NOT NULL DEFAULT 'ABIERTO' CHECK (estado IN ('ABIERTO','CERRADO')),
  UNIQUE (id_contribuyente, anio, mes));
CREATE TABLE emisor (
  id_emisor        INTEGER PRIMARY KEY AUTOINCREMENT,
  ruc              TEXT NOT NULL UNIQUE CHECK (length(ruc) = 11),
  razon_social     TEXT NOT NULL);
CREATE TABLE factura_compra (
  id_factura       INTEGER PRIMARY KEY AUTOINCREMENT,
  id_periodo       INTEGER NOT NULL REFERENCES periodo(id_periodo),
  id_emisor        INTEGER NOT NULL REFERENCES emisor(id_emisor),
  serie            TEXT NOT NULL,
  numero           TEXT NOT NULL,
  fecha_emision    TEXT NOT NULL,
  moneda           TEXT NOT NULL DEFAULT 'PEN' CHECK (moneda IN ('PEN','USD')),
  importe_total    NUMERIC NOT NULL CHECK (importe_total > 0),
  origen           TEXT NOT NULL CHECK (origen IN ('IA','MANUAL')),
  estado           TEXT NOT NULL DEFAULT 'VIGENTE' CHECK (estado IN ('VIGENTE','ANULADA')),
  motivo_anulacion TEXT,
  fecha_registro   TEXT NOT NULL,
  UNIQUE (id_emisor, serie, numero),
  CHECK (estado = 'VIGENTE' OR motivo_anulacion IS NOT NULL));
CREATE INDEX ix_factura_periodo ON factura_compra (id_periodo, estado);
CREATE TABLE imagen_factura (
  id_imagen        INTEGER PRIMARY KEY AUTOINCREMENT,
  id_factura       INTEGER NOT NULL UNIQUE REFERENCES factura_compra(id_factura),
  ruta_cifrada     TEXT NOT NULL,
  nitidez          REAL,
  fecha_captura    TEXT NOT NULL);
CREATE TABLE modelo_local (
  id_modelo        INTEGER PRIMARY KEY AUTOINCREMENT,
  version          TEXT NOT NULL UNIQUE,
  tamano_mb        INTEGER NOT NULL,
  sha256           TEXT NOT NULL CHECK (length(sha256) = 64),
  estado           TEXT NOT NULL CHECK (estado IN ('NO_INSTALADO','DESCARGANDO','VERIFICADO','ACTIVO')),
  ruta             TEXT,
  fecha_instalacion TEXT);
CREATE TABLE campo_extraido (
  id_campo         INTEGER PRIMARY KEY AUTOINCREMENT,
  id_factura       INTEGER NOT NULL REFERENCES factura_compra(id_factura),
  id_modelo        INTEGER REFERENCES modelo_local(id_modelo),      -- NULL si el registro fue manual
  nombre_campo     TEXT NOT NULL,
  valor_leido      TEXT,
  valor_final      TEXT,
  confianza        REAL CHECK (confianza BETWEEN 0 AND 1),
  corregido        INTEGER NOT NULL DEFAULT 0 CHECK (corregido IN (0,1)));
CREATE INDEX ix_campo_factura ON campo_extraido (id_factura);
CREATE TABLE parametro_version (
  id_parametro     INTEGER PRIMARY KEY AUTOINCREMENT,
  version          TEXT NOT NULL UNIQUE,
  umbral_aviso     NUMERIC NOT NULL DEFAULT 0.80,
  tope_anual       NUMERIC NOT NULL,
  vigente_desde    TEXT NOT NULL,
  activo           INTEGER NOT NULL DEFAULT 0 CHECK (activo IN (0,1)));
CREATE TABLE categoria_nrus (
  id_categoria     INTEGER PRIMARY KEY AUTOINCREMENT,
  id_parametro     INTEGER NOT NULL REFERENCES parametro_version(id_parametro),
  codigo           INTEGER NOT NULL,
  limite_mensual   NUMERIC NOT NULL,
  cuota            NUMERIC NOT NULL,
  UNIQUE (id_parametro, codigo));
CREATE TABLE cronograma_venc (
  id_cronograma    INTEGER PRIMARY KEY AUTOINCREMENT,
  id_parametro     INTEGER NOT NULL REFERENCES parametro_version(id_parametro),
  anio             INTEGER NOT NULL,
  mes              INTEGER NOT NULL CHECK (mes BETWEEN 1 AND 12),
  ultimo_digito    INTEGER NOT NULL CHECK (ultimo_digito BETWEEN 0 AND 9),
  fecha_limite     TEXT NOT NULL,
  UNIQUE (id_parametro, anio, mes, ultimo_digito));
CREATE TABLE determinacion (
  id_determinacion INTEGER PRIMARY KEY AUTOINCREMENT,
  id_periodo       INTEGER NOT NULL UNIQUE REFERENCES periodo(id_periodo),
  id_categoria     INTEGER REFERENCES categoria_nrus(id_categoria),
  total_adquisiciones NUMERIC NOT NULL,
  monto_determinante  NUMERIC NOT NULL,
  cuota            NUMERIC,
  fecha_vencimiento TEXT,
  nivel_alerta     TEXT NOT NULL CHECK (nivel_alerta IN ('NINGUNA','AVISO_80','LIMITE_100','FUERA_DE_REGIMEN')),
  fecha_calculo    TEXT NOT NULL);
CREATE TABLE aviso (
  id_aviso         INTEGER PRIMARY KEY AUTOINCREMENT,
  id_periodo       INTEGER NOT NULL REFERENCES periodo(id_periodo),
  tipo             TEXT NOT NULL CHECK (tipo IN ('TOPE_80','TOPE_100','VENCIMIENTO')),
  fecha_programada TEXT NOT NULL,
  enviado          INTEGER NOT NULL DEFAULT 0 CHECK (enviado IN (0,1)));
CREATE TABLE reporte_mensual (
  id_reporte       INTEGER PRIMARY KEY AUTOINCREMENT,
  id_periodo       INTEGER NOT NULL REFERENCES periodo(id_periodo),
  ruta_pdf         TEXT NOT NULL,
  sha256           TEXT NOT NULL,
  fecha_generacion TEXT NOT NULL);'''))
    d.add(h3("M.3. Apertura cifrada y consultas principales"))
    d.add(codigo('''byte[] clave = GestorClaves.claveBaseDatos(context);      // 32 bytes, protegida con una clave del Android Keystore
SupportOpenHelperFactory fabrica = new SupportOpenHelperFactory(clave);
BaseDatosFacturas db = Room.databaseBuilder(context, BaseDatosFacturas.class, "facturas.db")
        .openHelperFactory(fabrica)                            // SQLCipher: la base entera queda cifrada (AES-256)
        .build();''', "Apertura de la base SQLite cifrada con SQLCipher"))
    d.add(codigo('''-- Acumulado de compras del período (RF-08)
SELECT COALESCE(SUM(importe_total), 0)
  FROM factura_compra
 WHERE id_periodo = :idPeriodo AND estado = 'VIGENTE';

-- Detección de duplicados (RF-07)
SELECT f.id_factura
  FROM factura_compra f JOIN emisor e ON e.id_emisor = f.id_emisor
 WHERE e.ruc = :ruc AND f.serie = :serie AND f.numero = :numero;

-- Filas de facturas.csv para la exportación (RF-21)
SELECT f.fecha_emision, e.ruc, e.razon_social, f.serie, f.numero, f.moneda, f.importe_total, f.origen, f.estado
  FROM factura_compra f JOIN emisor e ON e.id_emisor = f.id_emisor
 WHERE f.id_periodo = :idPeriodo
 ORDER BY f.fecha_emision;''', "Consultas principales sobre la base local (DAO de Room)"))
    # N
    d.add(h2("Anexo N: Arquitectura tecnológica (configuración de la app)", salto=True))
    d.add(p("La solución no tiene API ni servidor. Su configuración técnica se reduce a dos archivos versionados dentro de la "
            "app —el manifiesto del modelo y los parámetros del NRUS— y a los permisos mínimos de Android. El manifiesto indica "
            "de dónde se descarga el modelo y con qué huella se verifica; una versión nueva llega con cada actualización de la app."))
    d.add(codigo('''{
  "version":   "1.3",
  "url":       "https://huggingface.co/<repositorio-del-proyecto>/resolve/v1.3/gemma-4-e2b-facturas.litertlm",
  "sha256":    "<64 caracteres hexadecimales>",
  "tamano_mb": 2650
}''', "assets/modelo.json — manifiesto del modelo incluido en la app"))
    d.tabla("t_permisos", "Permisos de Android que usa la app",
            ["Permiso", "Uso", "Cuándo se pide"],
            [["CAMERA", "Fotografiar la factura", "Al abrir la cámara (P06)"],
             ["INTERNET, ACCESS_NETWORK_STATE", "Solo la descarga del modelo por Wi-Fi", "Preparación de la lectura (P03)"],
             ["POST_NOTIFICATIONS", "Avisos de tope y de vencimiento", "Android 13 o superior, al activar los avisos"],
             ["USE_BIOMETRIC", "Acceso con huella", "Al activar la huella (P02, P18)"],
             ["Sin permisos de almacenamiento", "La exportación usa el selector de archivos de Android", "—"]],
            [3.0, 4.2, 2.8])
    d.tabla("t_entornos", "Entornos y configuración",
            ["Entorno", "Teléfono", "Modelo", "Datos"],
            [["Desarrollo", "Emulador y teléfono de referencia, compilación debug", "Archivo copiado al teléfono con ADB", "Base de prueba con el caso simulado"],
             ["Pruebas", "Teléfono de referencia, compilación release firmada", "Descarga desde el repositorio público", "Base limpia en cada prueba"],
             ["Producción", "Google Play", "Descarga única verificada con SHA-256", "Base cifrada del usuario, solo en su teléfono"]],
            [1.6, 3.2, 2.8, 2.6])
    # O
    d.add(h2("Anexo O: Diseño de interfaces (guía de estilo)", salto=True))
    d.add(p("Tokens del sistema de diseño que la app implementa como recursos de Android (colors.xml, dimens.xml y themes.xml)."))
    d.tabla("t_tokens", "Tokens de diseño",
            ["Token", "Valor", "Uso"],
            [["color/primario", "#1B4D45", "Barras, botones principales, títulos"],
             ["color/acento", "#8BC34A", "Botón Escanear, progreso y éxito"],
             ["color/fondo", "#F5F7F6", "Fondo de pantallas"],
             ["color/texto y texto2", "#1D2B28 / #5E6E6A", "Texto principal y secundario"],
             ["color/alerta", "#F0A202", "Confianza baja; 80 % del límite"],
             ["color/error y ok", "#C62828 / #2E7D32", "Validación fallida y correcta"],
             ["dimen/margen", "16 dp", "Margen lateral"],
             ["dimen/boton_alto", "48 dp", "Altura mínima de botones y objetivos táctiles"],
             ["dimen/radio_tarjeta", "14 dp", "Tarjetas"],
             ["texto/dato", "16 sp negrita", "Valores de campos"],
             ["texto/monto", "28–36 sp negrita", "Montos destacados"]],
            [2.8, 2.6, 4.6])
    # P
    d.add(h2("Anexo P: Prototipo completo (especificación por pantalla)", salto=True))
    especificacion = [
        ["P01", "RUC, nombre del negocio, titular", "Continuar", "RUC módulo 11; campos obligatorios"],
        ["P02", "PIN de 4 dígitos", "Ingresar, huella, restablecer", "3 intentos fallidos: espera de 30 s"],
        ["P03", "RAM, espacio, red, progreso de descarga", "Pausar, registrar a mano", "SHA-256 al terminar"],
        ["P04", "Compras, ventas, categoría, cuota, vencimiento, últimas facturas", "Escanear, editar ventas, ver todas", "Barra ámbar al 80 %"],
        ["P05", "Consejos de captura", "Abrir cámara, galería", "—"],
        ["P06", "Vista previa, nitidez y luz", "Disparar, galería, flash", "Disparador activo con nitidez y luz buenas"],
        ["P07", "Progreso por campo", "Cancelar", "Tiempo máximo de 20 s antes de ofrecer reintento"],
        ["P08", "Imagen y 7 campos con estado", "Editar campo, guardar, otra foto", "RUC, fecha del período, importe > 0"],
        ["P09", "Factura existente", "Ver registrada, descartar", "Clave emisor + serie + número"],
        ["P10", "Importe agregado y acumulado", "Escanear otra, inicio", "—"],
        ["P11", "Facturas por semana y total", "Filtrar, buscar, abrir", "—"],
        ["P12", "Datos de la factura", "Anular con motivo, compartir", "Solo en período abierto"],
        ["P13", "Total de ventas", "Guardar ventas", "Monto ≥ 0"],
        ["P14", "Determinación y tabla vigente", "Activar o desactivar avisos", "—"],
        ["P15", "Fecha límite y calendario", "Recordatorios, ver reporte", "—"],
        ["P16", "Vista previa del PDF", "Compartir, guardar", "Advierte si faltan las ventas"],
        ["P17", "Meses y acumulado anual", "Cerrar el mes, abrir mes, exportar", "No se cierra sin ventas registradas"],
        ["P18", "Negocio, seguridad, modelo, parámetros vigentes, mis datos", "Cambiar PIN, huella, buscar modelo nuevo, exportar datos", "—"],
        ["P19", "RUC, proveedor, serie, fecha, importe, foto", "Guardar, cancelar", "Mismas validaciones que P08"],
        ["P20", "Período (mes o año), contenido del archivo, tamaño estimado", "Guardar en el teléfono o la PC, compartir", "Aviso de archivo sin cifrar"]]
    d.tabla("t_espec_pantallas", "Especificación de datos, acciones y validaciones por pantalla",
            ["N.º", "Datos que muestra o captura", "Acciones", "Validaciones y estados"], especificacion,
            [0.8, 3.4, 3.0, 3.0], centrar=(0,))
    # Q
    d.add(h2("Anexo Q: Evidencias de las pantallas", salto=True))
    d.add(p("Cada pantalla se entrega como imagen PNG y como archivo SVG editable en la carpeta entregable2_archivos (las "
            "fuentes SVG en su subcarpeta fuentes). Los datos que aparecen son ilustrativos del caso simulado."))
    archivos = [["P01–P20", "pantalla_01_… a pantalla_20_….png", "Una imagen por pantalla móvil"],
                ["Hojas por módulo", "prototipo_hoja_1_acceso.png … prototipo_hoja_5_historial.png", "Figuras del apartado 3.6.5"],
                ["Reporte", "reporte_mensual_pdf.png", "Página 1 del reporte mensual"],
                ["Navegación", "ui_mapa_navegacion.png", "Mapa de navegación"],
                ["Guía de estilo", "ui_sistema_diseno.png", "Sistema de diseño"],
                ["Fuentes editables", "fuentes/*.svg", "Vectores editables en Figma, Penpot o Inkscape"],
                ["Figuras del primer avance", "casos_uso.png, wbs.png", "Ajustadas al diseño sin servidor (script ajustar_figuras_e1.py)"]]
    d.tabla("t_archivos", "Archivos de evidencia del prototipo", ["Elemento", "Archivo", "Contenido"], archivos, [2.2, 4.6, 3.4])
    # R
    d.add(h2("Anexo R: Matriz de cobertura del alcance", salto=True))
    matriz = [
        ["RF-01", "CU-01", "Registro", "P05, P06, P19", "CapturaActivity", "imagen_factura"],
        ["RF-02", "CU-01", "Registro", "P06", "EvaluadorNitidez", "imagen_factura"],
        ["RF-03", "CU-01", "Registro", "P05, P06", "CapturaActivity", "imagen_factura"],
        ["RF-04", "CU-01", "Registro", "P07", "ExtractorGemmaLocal", "campo_extraido"],
        ["RF-05", "CU-02", "Registro", "P08", "VerificacionViewModel", "campo_extraido"],
        ["RF-06", "CU-02", "Registro", "P01, P08, P19", "ValidadorRuc", "emisor, factura_compra"],
        ["RF-07", "CU-02", "Registro", "P09", "DetectorDuplicados", "factura_compra"],
        ["RF-08", "CU-04", "TO-BE", "P04, P10, P11", "PeriodoMensual", "periodo"],
        ["RF-09", "CU-03", "Cierre", "P13", "PeriodoMensual", "periodo"],
        ["RF-10", "CU-05", "Cierre", "P04, P14", "MotorReglasNRUS", "determinacion"],
        ["RF-11", "CU-05", "Cierre", "P04, P14", "MotorReglasNRUS", "determinacion"],
        ["RF-12", "CU-06", "TO-BE", "P04, P10, P14, P17", "MotorReglasNRUS", "aviso"],
        ["RF-13", "CU-07", "Cierre", "P04, P15", "CronogramaVencimiento", "cronograma_venc"],
        ["RF-14", "CU-07", "Cierre", "P15", "RecordatorioWorker", "aviso"],
        ["RF-15", "CU-08", "Cierre", "P16", "GenerarReporteUseCase", "reporte_mensual"],
        ["RF-16", "CU-08", "Cierre", "P16", "GenerarReporteUseCase", "reporte_mensual"],
        ["RF-17", "CU-11", "Cierre", "P17", "CerrarPeriodoUseCase", "periodo"],
        ["RF-18", "CU-12", "—", "P12", "FacturaCompra", "factura_compra"],
        ["RF-19", "CU-10", "—", "P01, P02, P18", "Contribuyente", "contribuyente"],
        ["RF-20", "CU-09", "Modelo", "P03, P18", "GestorModeloLocal", "modelo_local"],
        ["RF-21", "CU-11", "Cierre", "P17, P18, P20", "ExportadorDatos", "factura_compra, periodo, determinacion"],
        ["RF-22", "CU-05", "Modelo", "P14, P18", "ParametrosNrus", "parametro_version"]]
    d.tabla("t_matriz_r", "Matriz de cobertura por requerimiento funcional",
            ["RF", "Caso de uso", "Proceso BPMN", "Pantallas", "Clase", "Tablas"], matriz, [1.0, 1.2, 1.3, 2.0, 2.6, 2.3], centrar=(0, 1))
    # S
    d.add(h2("Anexo S: Validación del prototipo", salto=True))
    d.add(h3("S.1. Evaluación heurística del prototipo"))
    heur = [
        ["1. Visibilidad del estado del sistema", "Progreso por campo (P07), barra al límite (P04), descarga con porcentaje (P03)", "Se agregó el tiempo restante en P07"],
        ["2. Relación con el mundo real", "«Compras», «ventas», «le corresponde»; la factura se ve junto a los campos", "Se reemplazó «adquisiciones» por «compras» en la interfaz"],
        ["3. Control y libertad del usuario", "Cancelar la lectura, otra foto, corregir campos, anular con motivo", "—"],
        ["4. Consistencia y estándares", "Barra inferior fija, botón primario siempre abajo, colores con significado fijo", "—"],
        ["5. Prevención de errores", "Disparador solo con buena nitidez, duplicados, RUC validado antes de guardar", "Se bloqueó el cierre del mes sin ventas"],
        ["6. Reconocer antes que recordar", "Últimas facturas en Inicio; tabla del NRUS visible en P14", "—"],
        ["7. Flexibilidad y eficiencia", "Escanear a un toque; galería para facturas recibidas por PDF o foto", "Opción de omitir la guía de captura"],
        ["8. Diseño estético y minimalista", "Un dato principal por tarjeta; máximo 7 campos en verificación", "—"],
        ["9. Ayuda a reconocer y recuperarse de errores", "Mensajes que dicen qué hacer («Acérquese hasta que se lea el total»)", "Se reescribieron los mensajes de error"],
        ["10. Ayuda y documentación", "Guía de captura (P05) y explicación de la regla del mayor monto (P13)", "—"]]
    d.tabla("t_heuristicas", "Evaluación heurística del prototipo (equipo del proyecto)",
            ["Heurística de Nielsen", "Cómo la cumple el prototipo", "Mejora incorporada"], heur, [3.0, 4.4, 2.6])
    d.add(h3("S.2. Protocolo de la prueba de usabilidad"))
    d.tabla("t_protocolo", "Tareas del protocolo de prueba de usabilidad",
            ["Tarea", "Instrucción al participante", "Éxito"],
            [["T1", "Registre la factura que le entregamos", "Guarda la factura en ≤ 3 toques"],
             ["T2", "Corrija el importe si no coincide con el papel", "Detecta y corrige el campo ámbar"],
             ["T3", "Diga cuánto compró este mes y qué cuota le toca", "Lee el total y la cuota en P04"],
             ["T4", "Ingrese las ventas del mes", "Guarda el monto en P13"],
             ["T5", "Diga hasta qué fecha tiene para declarar", "Encuentra la fecha en P04 o P15"],
             ["T6", "Envíe el reporte del mes a su contador", "Comparte desde P16"]],
            [0.9, 5.4, 3.7], centrar=(0,))
    d.add(p("Participantes: tres titulares de bodega sin formación contable de las bodegas de referencia. Procedimiento: "
            "consentimiento informado, explicación de cinco minutos, ejecución de las tareas en voz alta sobre el prototipo "
            "navegable, cuestionario SUS y entrevista breve. Se registran el tiempo por tarea, los toques, los errores y las "
            "dudas. Los resultados se reportarán en APF3 junto con los cambios que produzcan en el diseño."))
    # T
    d.add(h2("Anexo T: Documentación técnica para desarrolladores (Markdown)", salto=True))
    d.add(p("La documentación técnica se entrega en el archivo entregable2_documentacion_tecnica.md, escrito en Markdown para "
            "versionarse en el repositorio del código. Es la referencia para implementar la solución y reúne en un solo lugar "
            "las decisiones de este informe en forma de contratos verificables. Su contenido es el siguiente:"))
    d.add(vinetas(["Visión del producto, alcance y restricciones (Java ≥ 50 %, Android 8.0+, lectura sin conexión).",
                   "Arquitectura por capas, regla de dependencias y estructura de módulos y paquetes.",
                   "Modelo de dominio, reglas del NRUS y algoritmo del motor de reglas con casos de prueba.",
                   "Contrato del motor de inferencia: instrucción, esquema JSON de salida, umbrales de confianza e integración con LiteRT-LM.",
                   "Esquema completo de la base local SQLite (13 tablas), con migraciones y seguridad.",
                   "Especificación de las pantallas P01–P20, navegación, tokens de diseño y textos de la interfaz.",
                   "Exportación de datos (formato del ZIP y de los CSV), seguridad sin servidor y configuración por entornos.",
                   "Estrategia de pruebas, criterios de aceptación por requerimiento y plan de construcción por iteraciones.",
                   "Convenciones de código, de ramas y de commits, y lista de verificación antes de cada entrega."]))
    d.add(codigo('''{
  "ruc":           { "valor": "20601234565",     "confianza": 0.98 },
  "razon_social":  { "valor": "DISTRIBUIDORA ANDINA S.A.C.", "confianza": 0.95 },
  "serie":         { "valor": "F001",            "confianza": 0.99 },
  "numero":        { "valor": "004821",          "confianza": 0.97 },
  "fecha_emision": { "valor": "2026-09-22",      "confianza": 0.96 },
  "moneda":        { "valor": "PEN",             "confianza": 0.99 },
  "importe_total": { "valor": "1450.00",         "confianza": 0.62 }
}''', "Ejemplo del contrato de salida del modelo (extracto de la documentación técnica)"))


# ======================================================================
def bibliografia(d):
    d.e1(546, 546)
    nuevas = {
        547: ["Elmasri, R., y Navathe, S. B. (2016). *Fundamentals of database systems* (7.ª ed.). Pearson."],
        551: ["Google. (s. f.). *Material Design 3*. https://m3.material.io/"],
        556: ["International Organization for Standardization. (2013). *ISO/IEC 19510:2013 — Information technology: Object Management Group Business Process Model and Notation*. ISO."],
        559: ["Nielsen, J. (1994). *Usability engineering*. Morgan Kaufmann.",
              "Object Management Group. (2014). *Business Process Model and Notation (BPMN), Version 2.0.2*. OMG."],
        561: ["OWASP Foundation. (s. f.). *OWASP Mobile Application Security Verification Standard (MASVS)*. https://mas.owasp.org/"],
        569: ["World Wide Web Consortium. (2018). *Web Content Accessibility Guidelines (WCAG) 2.1*. W3C. https://www.w3.org/TR/WCAG21/",
              "Zetetic. (s. f.). *SQLCipher for Android*. https://www.zetetic.net/sqlcipher/"],
    }
    for i in range(547, 570):
        d.e1(i)
        for ref in nuevas.get(i, []):
            d.e1_texto(547, ref)
