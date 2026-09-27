"""Diagramas BPMN 2.0 del entregable 2 (notación, AS-IS, TO-BE y subprocesos)."""
from svg import (Svg, guardar, envolver, GRIS, GRIS_MEDIO, GRIS_CLARO, AZUL, AZUL_CLARO, ROJO, ROJO_CLARO,
                 VERDE, VERDE_CLARO, NARANJA, NARANJA_CLARO)

TAREA_W, TAREA_H = 150, 64


class Bpmn(Svg):
    # ---------- contenedores ----------
    def pool(self, x, y, w, h, nombre, carriles=None):
        """Pool horizontal con banda de nombre a la izquierda y carriles opcionales [(nombre, alto)]."""
        self.rect(x, y, w, h, fill="#fff", stroke=GRIS, sw=1.6)
        self.rect(x, y, 34, h, fill=AZUL_CLARO, stroke=GRIS, sw=1.6)
        self._vertical(x + 17, y + h / 2, nombre, 14, True)
        if carriles:
            cy = y
            for i, (nom, alto) in enumerate(carriles):
                self.rect(x + 34, cy, 30, alto, fill=GRIS_CLARO, stroke=GRIS, sw=1)
                self._vertical(x + 49, cy + alto / 2, nom, 12, False)
                if i > 0:
                    self.line(x + 34, cy, x + w, cy, stroke=GRIS, sw=1)
                cy += alto
        return self

    def _vertical(self, cx, cy, texto, size, bold):
        partes = texto.split("\n")
        n = len(partes)
        for i, p in enumerate(partes):
            off = (i - (n - 1) / 2) * size * 1.2
            self.add(f'<text x="{cx + off}" y="{cy}" font-size="{size}" fill="{GRIS}" '
                     f'font-weight="{"bold" if bold else "normal"}" text-anchor="middle" dominant-baseline="middle" '
                     f'transform="rotate(-90 {cx + off} {cy})">{p}</text>')

    # ---------- eventos ----------
    def inicio(self, cx, cy, etiqueta=None, tipo=None, r=17):
        self.circle(cx, cy, r, fill="#fff", stroke=VERDE, sw=2)
        self._icono_evento(cx, cy, tipo, VERDE)
        if etiqueta:
            self._etiqueta(cx, cy + r + 14, etiqueta, 14)
        return (cx, cy, r)

    def fin(self, cx, cy, etiqueta=None, r=17, tipo=None):
        self.circle(cx, cy, r, fill="#fff", stroke=ROJO, sw=4)
        self._icono_evento(cx, cy, tipo, ROJO, relleno=True)
        if etiqueta:
            self._etiqueta(cx, cy + r + 14, etiqueta)
        return (cx, cy, r)

    def intermedio(self, cx, cy, etiqueta=None, tipo="timer", r=17, enviar=False):
        self.circle(cx, cy, r, fill="#fff", stroke=NARANJA, sw=1.6)
        self.circle(cx, cy, r - 4, fill="#fff", stroke=NARANJA, sw=1.2)
        self._icono_evento(cx, cy, tipo, NARANJA, relleno=enviar)
        if etiqueta:
            self._etiqueta(cx, cy + r + 14, etiqueta)
        return (cx, cy, r)

    def _icono_evento(self, cx, cy, tipo, color, relleno=False):
        if tipo == "timer":
            self.circle(cx, cy, 8, fill="#fff", stroke=color, sw=1.3)
            self.line(cx, cy, cx, cy - 6, stroke=color, sw=1.3)
            self.line(cx, cy, cx + 4, cy + 2, stroke=color, sw=1.3)
        elif tipo == "mensaje":
            f = color if relleno else "#fff"
            self.rect(cx - 8, cy - 5.5, 16, 11, fill=f, stroke=color, sw=1.3)
            self.poly([(cx - 8, cy - 5.5), (cx, cy + 1), (cx + 8, cy - 5.5)], stroke="#fff" if relleno else color, sw=1.2)

    def _etiqueta(self, cx, y, texto, ancho=20, color=GRIS):
        for i, l in enumerate(envolver(texto, ancho)):
            self.text(cx, y + i * 13, l, size=11, color=color, anchor="middle")

    # ---------- actividades ----------
    def tarea(self, cx, cy, texto, tipo=None, w=TAREA_W, h=TAREA_H, fill="#fff", stroke=AZUL, sub=False):
        lineas = envolver(texto, int(w / 7.2) - (2 if tipo else 0))
        if len(lineas) * 15 + 22 > h:
            h = len(lineas) * 15 + 22
        x, y = cx - w / 2, cy - h / 2
        self.rect(x, y, w, h, fill=fill, stroke=stroke, sw=1.8, rx=10)
        if tipo:
            self._icono_tarea(x + 8, y + 8, tipo)
        y0 = cy - (len(lineas) - 1) * 7.5 + 4 + (4 if tipo else 0) - (6 if sub else 0)
        for i, l in enumerate(lineas):
            self.text(cx, y0 + i * 15, l, size=12.5, color="#1a1a1a", anchor="middle")
        if sub:
            self.rect(cx - 7, y + h - 15, 14, 12, fill="#fff", stroke=GRIS, sw=1)
            self.line(cx - 4, y + h - 9, cx + 4, y + h - 9, stroke=GRIS, sw=1.2)
            self.line(cx, y + h - 13, cx, y + h - 5, stroke=GRIS, sw=1.2)
        return (cx, cy, w, h)

    def _icono_tarea(self, x, y, tipo):
        if tipo == "usuario":
            self.circle(x + 6, y + 4, 3.6, fill="#fff", stroke=GRIS, sw=1.1)
            self.path(f"M{x},{y + 14} Q{x + 6},{y + 5} {x + 12},{y + 14} Z", fill="#fff", stroke=GRIS, sw=1.1)
        elif tipo == "servicio":
            self.circle(x + 6, y + 6, 5.5, fill="#fff", stroke=GRIS, sw=1.2, dash="2.2 1.6")
            self.circle(x + 6, y + 6, 2.2, fill="#fff", stroke=GRIS, sw=1.1)
        elif tipo == "regla":
            self.rect(x, y, 14, 11, fill="#fff", stroke=GRIS, sw=1)
            self.rect(x, y, 14, 3.5, fill=GRIS_CLARO, stroke=GRIS, sw=1)
            self.line(x, y + 7.2, x + 14, y + 7.2, stroke=GRIS, sw=0.8)
            self.line(x + 4.5, y + 3.5, x + 4.5, y + 11, stroke=GRIS, sw=0.8)
        elif tipo == "manual":
            self.path(f"M{x},{y + 5} h8 a2,2 0 0 1 0,4 h-2 M{x},{y + 5} v6 h9", stroke=GRIS, sw=1.1)
        elif tipo == "envio":
            self.rect(x, y + 1, 14, 10, fill=GRIS, stroke=GRIS, sw=1)
            self.poly([(x, y + 1), (x + 7, y + 6), (x + 14, y + 1)], stroke="#fff", sw=1)

    def compuerta(self, cx, cy, etiqueta=None, tipo="xor", s=24, pos_etq="arriba"):
        self.polygon([(cx, cy - s), (cx + s, cy), (cx, cy + s), (cx - s, cy)], fill="#fffbe6", stroke=NARANJA, sw=1.8)
        if tipo == "xor":
            self.line(cx - 8, cy - 8, cx + 8, cy + 8, stroke=GRIS, sw=2.6)
            self.line(cx + 8, cy - 8, cx - 8, cy + 8, stroke=GRIS, sw=2.6)
        elif tipo == "and":
            self.line(cx - 10, cy, cx + 10, cy, stroke=GRIS, sw=2.6)
            self.line(cx, cy - 10, cx, cy + 10, stroke=GRIS, sw=2.6)
        if etiqueta:
            if pos_etq == "arriba":
                ls = envolver(etiqueta, 22)
                for i, l in enumerate(ls):
                    self.text(cx, cy - s - 8 - (len(ls) - 1 - i) * 13, l, size=11, color=GRIS, anchor="middle", bold=True)
            elif pos_etq == "izq":
                ls = envolver(etiqueta, 16)
                for i, l in enumerate(ls):
                    self.text(cx - s - 6, cy - s + 4 - (len(ls) - 1 - i) * 13, l, size=11, color=GRIS, anchor="end", bold=True)
            else:
                self._etiqueta(cx, cy + s + 14, etiqueta, 22)
        return (cx, cy, s)

    def datos(self, x, y, texto, w=36, h=46):
        self.path(f"M{x},{y} h{w - 10} l10,10 v{h - 10} h-{w} z", fill="#fff", stroke=GRIS, sw=1.3)
        self.path(f"M{x + w - 10},{y} v10 h10", stroke=GRIS, sw=1.1)
        self._etiqueta(x + w / 2, y + h + 13, texto, 18)

    def almacen(self, cx, y, texto, w=56, h=44):
        x = cx - w / 2
        self.path(f"M{x},{y + 7} a{w / 2},7 0 0 0 {w},0 v{h - 14} a{w / 2},7 0 0 1 -{w},0 z", fill="#fff", stroke=GRIS, sw=1.3)
        self.add(f'<ellipse cx="{cx}" cy="{y + 7}" rx="{w / 2}" ry="7" fill="#fff" stroke="{GRIS}" stroke-width="1.3"/>')
        self._etiqueta(cx, y + h + 13, texto, 20)

    def anotacion(self, x, y, texto, color=ROJO, ancho=26, desde=None):
        ls = envolver(texto, ancho)
        h = len(ls) * 14 + 10
        self.path(f"M{x + 10},{y} h-10 v{h} h10", stroke=color, sw=1.5)
        for i, l in enumerate(ls):
            self.text(x + 6, y + 16 + i * 14, l, size=11.5, color=color, bold=True)
        if desde:
            hacia = (x + 5, y) if desde[1] < y else (x + 5, y + h)
            self.line(desde[0], desde[1], hacia[0], hacia[1], stroke=color, sw=1.2, dash="3 3")

    # ---------- flujos ----------
    def flujo(self, pts, etiqueta=None, pos=None, color=GRIS):
        self.poly(pts, stroke=color, sw=1.6, end="flecha")
        if etiqueta:
            px, py = pos if pos else ((pts[0][0] + pts[1][0]) / 2, (pts[0][1] + pts[1][1]) / 2 - 6)
            self.text(px, py, etiqueta, size=11, color=GRIS, anchor="middle", italic=True)

    def mensaje(self, pts, etiqueta=None, pos=None):
        self.poly(pts, stroke=GRIS, sw=1.3, dash="7 5", end="abierta", start="circulo")
        if etiqueta:
            px, py = pos if pos else (pts[0][0] + 6, (pts[0][1] + pts[-1][1]) / 2)
            self.text(px, py, etiqueta, size=11, color=GRIS_MEDIO, italic=True)

    def asociacion(self, a, b):
        self.line(a[0], a[1], b[0], b[1], stroke=GRIS_MEDIO, sw=1.2, dash="2 3")


def titulo(s, x, y, texto, color=AZUL, size=20):
    s.text(x, y, texto, size=size, color=color, bold=True)


# ======================================================================
def notacion():
    s = Bpmn(1500, 900)
    titulo(s, 30, 42, "Notación BPMN 2.0 utilizada en los diagramas de Facturas S20")
    celdas = []
    W, H = 360, 190
    for fila in range(4):
        for col in range(4):
            celdas.append((30 + col * (W + 5), 70 + fila * (H + 10)))

    def caja(i, nombre, desc):
        x, y = celdas[i]
        s.rect(x, y, W, H, fill="#fff", stroke="#d0d7de", sw=1.2, rx=8)
        s.text(x + 16, y + 128, nombre, size=14.5, color=AZUL, bold=True)
        for k, l in enumerate(envolver(desc, 50)):
            s.text(x + 16, y + 148 + k * 15, l, size=12, color=GRIS)
        return x + W / 2, y + 60

    cx, cy = caja(0, "Evento de inicio", "Dispara el proceso. Con sobre: lo inicia un mensaje (llega el pedido).")
    s.inicio(cx - 45, cy); s.inicio(cx + 45, cy, tipo="mensaje")
    cx, cy = caja(1, "Evento intermedio de temporizador", "Espera una fecha o un plazo: fin de mes, días antes del vencimiento.")
    s.intermedio(cx - 45, cy, tipo="timer"); s.intermedio(cx + 45, cy, tipo="mensaje", enviar=True)
    cx, cy = caja(2, "Evento de fin", "Termina el recorrido del proceso. Borde grueso; puede indicar un resultado.")
    s.fin(cx, cy)
    cx, cy = caja(3, "Tarea", "Trabajo atómico. El ícono indica el tipo: de usuario, de servicio, de regla de negocio o manual.")
    s.tarea(cx - 80, cy - 4, "Tarea de usuario", tipo="usuario", w=140, h=56)
    s.tarea(cx + 80, cy - 4, "Tarea de servicio", tipo="servicio", w=140, h=56)
    cx, cy = caja(4, "Tarea de regla de negocio", "La ejecuta el motor de reglas del NRUS (categoría, cuota, topes, vencimiento).")
    s.tarea(cx - 80, cy - 4, "Calcular categoría", tipo="regla", w=140, h=56)
    s.tarea(cx + 80, cy - 4, "Declarar en SUNAT", tipo="manual", w=140, h=56)
    cx, cy = caja(5, "Subproceso colapsado", "Agrupa actividades que se detallan en otro diagrama (marca [+]).")
    s.tarea(cx, cy - 4, "Registrar factura con IA", w=170, h=60, sub=True)
    cx, cy = caja(6, "Compuerta exclusiva (XOR)", "Solo una salida según la condición: ¿la foto es legible?")
    s.compuerta(cx, cy + 4, tipo="xor")
    cx, cy = caja(7, "Compuerta paralela (AND)", "Abre o sincroniza caminos que ocurren a la vez.")
    s.compuerta(cx, cy + 4, tipo="and")
    cx, cy = caja(8, "Flujo de secuencia", "Orden de ejecución dentro de un mismo pool. Puede llevar condición.")
    s.flujo([(cx - 110, cy), (cx + 110, cy)], "sí", (cx, cy - 8))
    cx, cy = caja(9, "Flujo de mensaje", "Comunicación entre participantes distintos (bodega ↔ SUNAT).")
    s.mensaje([(cx - 110, cy), (cx + 110, cy)])
    cx, cy = caja(10, "Objeto de datos", "Información que una tarea usa o produce (factura, reporte PDF).")
    s.datos(cx - 18, cy - 38, "")
    cx, cy = caja(11, "Almacén de datos", "Información persistente: base local cifrada, servidor de respaldo.")
    s.almacen(cx, cy - 34, "")
    x, y = celdas[12]
    s.rect(x, y, W * 2 + 5, H, fill="#fff", stroke="#d0d7de", sw=1.2, rx=8)
    s.pool(x + 20, y + 16, 330, 120, "Bodega", [("Titular", 60), ("App", 60)])
    s.text(x + 380, y + 40, "Pool y carriles", size=14.5, color=AZUL, bold=True)
    for k, l in enumerate(envolver("El pool representa a un participante (la bodega, el distribuidor, la SUNAT). "
                                    "Los carriles separan quién ejecuta cada tarea dentro del participante: "
                                    "el bodeguero o la app en el teléfono.", 44)):
        s.text(x + 380, y + 62 + k * 15, l, size=12, color=GRIS)
    x, y = celdas[14]
    s.rect(x, y, W * 2 + 5, H, fill="#fff", stroke="#d0d7de", sw=1.2, rx=8)
    s.anotacion(x + 24, y + 26, "Se pierden 5 de 14 facturas", color=ROJO, ancho=24)
    s.anotacion(x + 24, y + 96, "Ninguna factura sin registrar", color=VERDE, ancho=24)
    s.text(x + 380, y + 40, "Anotación y asociación", size=14.5, color=AZUL, bold=True)
    for k, l in enumerate(envolver("Comentario sobre un elemento. En rojo, los puntos de error del proceso actual; "
                                    "en verde, la mejora que introduce el proceso propuesto.", 44)):
        s.text(x + 380, y + 62 + k * 15, l, size=12, color=GRIS)
    guardar(s, "bpmn_00_notacion")


# ======================================================================
def asis():
    s = Bpmn(1760, 700)
    titulo(s, 30, 36, "Proceso actual (AS-IS): recepción de facturas y declaración mensual del NRUS", ROJO)
    X0 = 30
    s.pool(X0, 60, 1700, 110, "Distribuidor")
    s.pool(X0, 190, 1700, 330, "Bodega (NRUS)", [("Titular / bodeguero", 330)])
    s.pool(X0, 540, 1700, 110, "SUNAT")
    # distribuidor
    s.inicio(130, 115)
    s.tarea(260, 115, "Entregar mercadería y factura", tipo="manual")
    s.fin(400, 115)
    s.flujo([(147, 115), (185, 115)]); s.flujo([(335, 115), (383, 115)])
    # bodega
    Y = 300
    s.inicio(140, Y, "Llega el pedido", tipo="mensaje")
    s.mensaje([(260, 147), (260, 230), (140, 230), (140, Y - 17)], "factura", (150, 222))
    s.tarea(260, Y, "Recibir mercadería y factura", tipo="manual")
    s.tarea(440, Y, "Guardar la factura en la caja", tipo="manual")
    s.almacen(440, 400, "Caja bajo el mostrador")
    s.asociacion((440, Y + 32), (440, 400))
    s.intermedio(590, Y, "Fin de mes", tipo="timer")
    s.tarea(730, Y, "Reunir las facturas del mes", tipo="manual")
    s.compuerta(880, Y, "¿Están todas?")
    s.tarea(880, Y + 130, "Estimar lo que falta de memoria", tipo="manual")
    s.compuerta(1010, Y, tipo="xor")
    s.tarea(1140, Y, "Sumar a mano las compras", tipo="manual")
    s.compuerta(1290, Y, "¿Seguro de la categoría?", pos_etq="izq")
    s.tarea(1420, Y - 72, "Elegir la categoría según la suma", tipo="usuario")
    s.tarea(1420, Y + 72, "Declarar la categoría superior", tipo="usuario")
    s.compuerta(1540, Y, tipo="xor")
    s.tarea(1540, Y + 165, "Declarar y pagar la cuota", tipo="manual", w=130)
    s.fin(1680, Y + 165)
    s.flujo([(157, Y), (185, Y)]); s.flujo([(335, Y), (365, Y)]); s.flujo([(515, Y), (573, Y)])
    s.flujo([(607, Y), (655, Y)]); s.flujo([(805, Y), (856, Y)])
    s.flujo([(904, Y), (986, Y)], "sí", (945, Y - 8))
    s.flujo([(880, Y + 24), (880, Y + 98)], "no", (892, Y + 60))
    s.flujo([(955, Y + 130), (1010, Y + 130), (1010, Y + 24)])
    s.flujo([(1034, Y), (1065, Y)]); s.flujo([(1215, Y), (1266, Y)])
    s.flujo([(1290, Y - 24), (1290, Y - 72), (1345, Y - 72)], "sí", (1302, Y - 50))
    s.flujo([(1290, Y + 24), (1290, Y + 72), (1345, Y + 72)], "no", (1302, Y + 54))
    s.flujo([(1495, Y - 72), (1540, Y - 72), (1540, Y - 24)])
    s.flujo([(1495, Y + 72), (1540, Y + 72), (1540, Y + 24)])
    s.flujo([(1540, Y + 24 + 1), (1540, Y + 133)])
    s.flujo([(1605, Y + 165), (1663, Y + 165)])
    s.mensaje([(1540, Y + 197), (1540, 580)], "declaración y pago", (1548, 560))
    s.tarea(1540, 598, "Recibir declaración y cuota", tipo="servicio", w=150, h=52)
    # anotaciones de error
    s.anotacion(470, 214, "Se pierden 5 de 14 facturas", ancho=28, desde=(470, Y - 32))
    s.anotacion(1060, 380, "3 h al mes y errores de suma", ancho=26, desde=(1140, Y + 32))
    s.anotacion(1180, 440, "Ante la duda paga S/ 50 en vez de S/ 20", ancho=22, desde=(1345, Y + 90))
    guardar(s, "bpmn_01_asis")


# ======================================================================
def tobe():
    s = Bpmn(1760, 860)
    titulo(s, 30, 36, "Proceso propuesto (TO-BE) con Facturas S20: registro al recibir y cierre mensual asistido", VERDE)
    s.pool(30, 60, 1700, 100, "Distribuidor")
    s.pool(30, 180, 1700, 520, "Bodega (NRUS)", [("Bodeguero", 250), ("App Facturas S20\n(en el teléfono)", 270)])
    s.pool(30, 720, 1700, 110, "SUNAT")
    s.inicio(130, 110); s.tarea(260, 110, "Entregar mercadería y factura", tipo="manual", h=56); s.fin(400, 110)
    s.flujo([(147, 110), (185, 110)]); s.flujo([(335, 110), (383, 110)])
    # --- fila 1: registro ---
    YB, YA = 280, 510
    s.inicio(140, YB, "Llega el pedido", tipo="mensaje")
    s.mensaje([(260, 138), (260, 205), (140, 205), (140, YB - 17)], "factura", (150, 199))
    s.tarea(260, YB, "Recibir mercadería y factura", tipo="manual")
    s.tarea(440, YB, "Fotografiar la factura (≈10 s)", tipo="usuario")
    s.tarea(440, YA, "Leer la factura con Gemma 4 en el teléfono", tipo="servicio", fill=VERDE_CLARO, stroke=VERDE)
    s.tarea(630, YA, "Validar RUC, fecha y duplicado", tipo="regla")
    s.tarea(630, YB, "Verificar y confirmar los campos", tipo="usuario")
    s.tarea(820, YA, "Guardar y actualizar el acumulado del mes", tipo="servicio")
    s.tarea(1000, YA, "Recalcular categoría y cuota", tipo="regla")
    s.compuerta(1150, YA, "¿≥ 80 % del límite?", pos_etq="abajo")
    s.tarea(1150, YA - 118, "Mostrar aviso de proximidad al tope", tipo="envio", w=140, h=56)
    s.compuerta(1290, YA, tipo="xor")
    s.fin(1380, YA, "Factura registrada")
    s.almacen(820, 604, "BD local cifrada")
    s.asociacion((820, YA + 32), (820, 604))
    s.flujo([(157, YB), (185, YB)]); s.flujo([(335, YB), (365, YB)])
    s.flujo([(440, YB + 32), (440, YA - 32)])
    s.flujo([(515, YA), (555, YA)])
    s.flujo([(630, YA - 32), (630, YB + 32)])
    s.flujo([(705, YB), (820, YB), (820, YA - 32)])
    s.flujo([(895, YA), (925, YA)]); s.flujo([(1075, YA), (1126, YA)])
    s.flujo([(1150, YA - 24), (1150, YA - 90)], "sí", (1162, YA - 50))
    s.flujo([(1174, YA), (1266, YA)], "no", (1215, YA - 8))
    s.flujo([(1220, YA - 118), (1290, YA - 118), (1290, YA - 24)])
    s.flujo([(1314, YA), (1363, YA)])
    # --- fila 2: cierre ---
    s.intermedio(1470, YA + 90, "5 días antes del vencimiento", tipo="timer")
    s.tarea(1470, YA - 20, "Recordar ventas y vencimiento", tipo="envio", w=140, h=56)
    s.tarea(1470, YB, "Ingresar el total de ventas del mes", tipo="usuario", w=140)
    s.tarea(1650, YA - 20, "Determinar categoría y generar reporte", tipo="regla", w=140, h=60)
    s.tarea(1650, YB, "Declarar y pagar con el monto exacto", tipo="manual", w=140)
    s.fin(1650, YB - 72, r=15)
    s.flujo([(1470, YA + 73), (1470, YA + 8)])
    s.flujo([(1470, YA - 48), (1470, YB + 32)])
    s.flujo([(1540, YB + 16), (1560, YB + 16), (1560, YA - 20), (1580, YA - 20)])
    s.flujo([(1650, YA - 50), (1650, YB + 32)])
    s.flujo([(1650, YB - 32), (1650, YB - 57)])
    s.mensaje([(1720, YB), (1724, YB), (1724, 735), (1650, 735), (1650, 750)], "declaración y pago", (1528, 756))
    s.tarea(1650, 775, "Recibir declaración y cuota", tipo="servicio", w=150, h=50)
    # anotaciones de mejora
    s.anotacion(470, 200, "Ninguna factura sin registrar", color=VERDE, ancho=30, desde=(470, YB - 32))
    s.anotacion(300, 590, "Sin internet: la foto no sale del teléfono", color=VERDE, ancho=22, desde=(420, YA + 32))
    s.anotacion(960, 590, "Total y categoría siempre al día", color=VERDE, ancho=20, desde=(1000, YA + 32))
    guardar(s, "bpmn_02_tobe")


# ======================================================================
def registro():
    s = Bpmn(1800, 900)
    titulo(s, 30, 36, "Subproceso «Registrar factura con IA» (CU-01 y CU-02) con sus excepciones", AZUL)
    carr = [("Bodeguero", 190), ("Captura\n(cámara)", 170), ("Motor de extracción\nGemma 4 E2B", 170), ("Reglas y datos", 250)]
    s.pool(30, 60, 1740, 780, "App Facturas S20", carr)
    Y1, Y2, Y3, Y4 = 155, 335, 505, 700
    s.inicio(130, Y1, "Toca «Escanear»")
    s.tarea(260, Y2, "Mostrar guía de encuadre", tipo="servicio")
    s.tarea(400, Y1, "Tomar la foto o elegir de galería", tipo="usuario")
    s.tarea(540, Y2, "Evaluar nitidez e iluminación", tipo="servicio")
    s.compuerta(690, Y2, "¿Legible?")
    s.compuerta(820, Y2, "¿Modelo instalado y memoria suficiente?", pos_etq="abajo")
    s.tarea(820, Y1, "Registrar los datos a mano", tipo="usuario")
    s.tarea(960, Y3, "Extraer campos y confianza (JSON)", tipo="servicio", fill=VERDE_CLARO, stroke=VERDE)
    s.tarea(1100, Y4 - 60, "Validar RUC módulo 11 y fecha del período", tipo="regla", w=170)
    s.compuerta(1260, Y4 - 60, "¿Duplicada?", pos_etq="abajo")
    s.tarea(1260, Y1, "Ver aviso de factura ya registrada", tipo="usuario", w=140)
    s.fin(1260, Y1 - 70, None, r=15)
    s.tarea(1410, Y1, "Revisar campos (baja confianza resaltada)", tipo="usuario", w=150)
    s.compuerta(1560, Y1, "¿Corrige?")
    s.tarea(1560, Y1 + 100, "Corregir campos", tipo="usuario", w=120, h=50)
    s.compuerta(1680, Y1, tipo="xor")
    s.tarea(1520, Y4 - 60, "Guardar factura, imagen y campos (transacción)", tipo="servicio", w=160)
    s.tarea(1520, Y4 + 60, "Recalcular acumulado y determinación", tipo="regla", w=160, h=56)
    s.fin(1690, Y4 + 60, "Factura registrada")
    s.almacen(1300, Y4 + 34, "BD local cifrada (SQLCipher)")
    s.datos(800, Y3 - 30, "Imagen 896 px")
    # flujos
    s.flujo([(147, Y1), (260, Y1), (260, Y2 - 32)])
    s.flujo([(335, Y2), (400, Y2), (400, Y1 + 32)])
    s.flujo([(475, Y1), (540, Y1), (540, Y2 - 32)])
    s.flujo([(615, Y2), (666, Y2)])
    s.flujo([(690, Y2 - 24), (690, Y1 + 60), (420, Y1 + 60), (420, Y1 + 34)], "no: otra toma", (560, Y1 + 54))
    s.flujo([(714, Y2), (796, Y2)], "sí", (755, Y2 - 8))
    s.flujo([(820, Y2 - 24), (820, Y1 + 32)], "no", (832, Y2 - 50))
    s.flujo([(844, Y2), (960, Y2), (960, Y3 - 32)], "sí", (900, Y2 - 8))
    s.flujo([(1035, Y3), (1060, Y3), (1060, Y4 - 92)])
    s.flujo([(895, Y1), (1160, Y1), (1160, Y4 - 92)])
    s.flujo([(1185, Y4 - 60), (1236, Y4 - 60)])
    s.flujo([(1260, Y4 - 84), (1260, Y1 + 32)], "sí", (1272, Y4 - 110))
    s.flujo([(1260, Y1 - 32), (1260, Y1 - 55)])
    s.flujo([(1284, Y4 - 60), (1340, Y4 - 60), (1340, Y1 + 50), (1410, Y1 + 50), (1410, Y1 + 32)], "no", (1310, Y4 - 68))
    s.flujo([(1485, Y1), (1536, Y1)])
    s.flujo([(1560, Y1 + 24), (1560, Y1 + 75)], "sí", (1572, Y1 + 52))
    s.flujo([(1620, Y1 + 100), (1680, Y1 + 100), (1680, Y1 + 24)])
    s.flujo([(1584, Y1), (1656, Y1)], "no", (1620, Y1 - 8))
    s.flujo([(1704, Y1), (1740, Y1), (1740, Y4 - 60), (1600, Y4 - 60)])
    s.flujo([(1520, Y4 - 28), (1520, Y4 + 32)])
    s.flujo([(1600, Y4 + 60), (1673, Y4 + 60)])
    s.asociacion((1440, Y4 - 40), (1328, Y4 + 50))
    s.asociacion((885, Y3), (836, Y3 - 8))
    s.anotacion(130, 770, "Reglas: RF-01 a RF-08 · RNF-04 lectura ≤ 20 s · RNF-12 funciona sin red · RNF-14 la imagen no sale del teléfono",
                color=AZUL, ancho=48)
    guardar(s, "bpmn_03_registro")


# ======================================================================
def cierre():
    s = Bpmn(1760, 700)
    titulo(s, 30, 36, "Subproceso «Cierre mensual y declaración» (CU-03, CU-05 a CU-08, CU-11)", AZUL)
    s.pool(30, 60, 1700, 470, "Bodega (NRUS)", [("Bodeguero", 170), ("App Facturas S20\n(motor de reglas NRUS)", 300)])
    s.pool(30, 550, 1700, 120, "Contador\nexterno")
    YB, YA = 145, 330
    s.intermedio(152, YA, "5 días antes del vencimiento", tipo="timer")
    s.tarea(270, YA, "Calcular fecha límite según último dígito del RUC", tipo="regla")
    s.tarea(440, YA, "Enviar recordatorio local", tipo="envio")
    s.tarea(440, YB, "Ingresar el total de ventas del mes", tipo="usuario")
    s.tarea(620, YA, "Determinar categoría con el mayor monto", tipo="regla")
    s.compuerta(780, YA, "¿Supera los topes del régimen?", pos_etq="abajo")
    s.tarea(780, YA - 120, "Alertar: evaluar cambio de régimen", tipo="envio", w=140, h=56)
    s.compuerta(900, YA, tipo="xor")
    s.tarea(1030, YA, "Generar reporte PDF del mes", tipo="servicio")
    s.almacen(1030, YA + 90, "Facturas e imágenes")
    s.compuerta(1170, YB, "¿Compartir con el contador?", pos_etq="izq")
    s.tarea(1300, YB, "Compartir el reporte", tipo="envio", w=130)
    s.tarea(1300, 610, "Revisar el reporte", tipo="usuario", h=52)
    s.tarea(1460, YB, "Declarar y pagar en SUNAT", tipo="manual", w=130)
    s.tarea(1640, YB, "Cerrar el mes", tipo="usuario", w=120)
    s.tarea(1640, YA, "Archivar en histórico y respaldar (opcional)", tipo="servicio", w=150)
    s.fin(1640, YA + 120, "Mes cerrado")
    s.flujo([(169, YA), (195, YA)]); s.flujo([(345, YA), (365, YA)])
    s.flujo([(440, YA - 32), (440, YB + 32)])
    s.flujo([(515, YB), (620, YB), (620, YA - 32)])
    s.flujo([(695, YA), (756, YA)])
    s.flujo([(780, YA - 24), (780, YA - 92)], "sí", (792, YA - 50))
    s.flujo([(804, YA), (876, YA)], "no", (840, YA - 8))
    s.flujo([(850, YA - 120), (900, YA - 120), (900, YA - 24)])
    s.flujo([(924, YA), (955, YA)])
    s.flujo([(1105, YA), (1170, YA), (1170, YB + 24)])
    s.flujo([(1194, YB), (1235, YB)], "sí", (1214, YB - 8))
    s.flujo([(1170, YB - 24), (1170, 84), (1460, 84), (1460, YB - 32)], "no", (1320, 78))
    s.flujo([(1365, YB), (1395, YB)])
    s.mensaje([(1300, YB + 32), (1300, 584)], "reporte PDF", (1310, 560))
    s.flujo([(1525, YB), (1580, YB)])
    s.flujo([(1640, YB + 32), (1640, YA - 38)])
    s.flujo([(1640, YA + 38), (1640, YA + 103)])
    s.asociacion((1030, YA + 32), (1030, YA + 90))
    s.anotacion(160, 440, "Categoría = mayor entre ingresos y adquisiciones; aviso al 80 % y 100 % del límite",
                color=AZUL, ancho=44)
    guardar(s, "bpmn_04_cierre")


# ======================================================================
def modelo():
    s = Bpmn(1760, 760)
    titulo(s, 30, 36, "Subproceso «Publicar y actualizar el modelo de IA» (CU-09, RF-20, RF-22)", AZUL)
    s.pool(30, 60, 1700, 240, "Equipo del proyecto", [("Entrenamiento\n(Google Colab)", 130), ("Administrador", 110)])
    s.pool(30, 320, 1700, 110, "Servidor de\nrespaldo")
    s.pool(30, 450, 1700, 280, "App Facturas S20\n(teléfono)")
    Y1, Y2 = 125, 245
    s.inicio(130, Y1, "Nuevas facturas etiquetadas")
    s.tarea(270, Y1, "Ajuste fino LoRA de Gemma 4 E2B", tipo="servicio")
    s.tarea(440, Y1, "Evaluar contra el modelo base", tipo="servicio")
    s.compuerta(580, Y1, "¿Mejora y ≥ 90 %?")
    s.tarea(730, Y1, "Fusionar, cuantizar y convertir a .litertlm", tipo="servicio")
    s.tarea(900, Y1, "Evaluar después de convertir", tipo="servicio")
    s.tarea(900, Y2, "Publicar versión con SHA-256 y parámetros NRUS", tipo="usuario", w=160, h=60)
    s.fin(1060, Y2)
    s.flujo([(147, Y1), (195, Y1)]); s.flujo([(345, Y1), (365, Y1)]); s.flujo([(515, Y1), (556, Y1)])
    s.flujo([(604, Y1), (655, Y1)], "sí", (628, Y1 - 8))
    s.flujo([(580, Y1 + 24), (580, 176), (270, 176), (270, Y1 + 32)], "no: ampliar el conjunto", (425, 170))
    s.flujo([(805, Y1), (825, Y1)]); s.flujo([(900, Y1 + 32), (900, Y2 - 30)])
    s.flujo([(980, Y2), (1043, Y2)])
    s.mensaje([(900, Y2 + 30), (900, 350)], "versión publicada", (910, 342))
    s.tarea(900, 380, "Registrar versión del modelo y de parámetros", tipo="servicio", w=180, h=52)
    YA = 560
    s.inicio(130, YA, "Revisión semanal por Wi-Fi", tipo="timer")
    s.tarea(270, YA, "Consultar la versión publicada", tipo="servicio")
    s.mensaje([(810, 380), (270, 380), (270, YA - 32)], "manifiesto", (300, 372))
    s.compuerta(420, YA, "¿Versión nueva?")
    s.fin(420, YA + 100, "Sin cambios")
    s.tarea(570, YA, "Descargar el archivo por Wi-Fi (reanudable)", tipo="servicio")
    s.tarea(740, YA, "Verificar huella SHA-256", tipo="servicio")
    s.compuerta(890, YA, "¿Coincide?")
    s.tarea(890, YA + 110, "Descartar y reintentar", tipo="servicio", w=130, h=50)
    s.tarea(1040, YA, "Probar con una factura de control", tipo="servicio")
    s.tarea(1210, YA, "Activar versión y conservar la anterior", tipo="servicio")
    s.fin(1370, YA, "Modelo actualizado")
    s.almacen(1210, YA + 80, "Almacenamiento interno")
    s.flujo([(147, YA), (195, YA)]); s.flujo([(345, YA), (396, YA)])
    s.flujo([(420, YA + 24), (420, YA + 83)], "no", (432, YA + 55))
    s.flujo([(444, YA), (495, YA)], "sí", (470, YA - 8))
    s.flujo([(645, YA), (665, YA)]); s.flujo([(815, YA), (866, YA)])
    s.flujo([(914, YA), (965, YA)], "sí", (940, YA - 8))
    s.flujo([(890, YA + 24), (890, YA + 85)], "no", (902, YA + 55))
    s.flujo([(825, YA + 110), (570, YA + 110), (570, YA + 32)])
    s.flujo([(1115, YA), (1135, YA)]); s.flujo([(1285, YA), (1353, YA)])
    s.asociacion((1210, YA + 32), (1210, YA + 80))
    s.anotacion(1450, 470, "Si la prueba falla se mantiene la versión anterior (RNF-19)", color=AZUL, ancho=24)
    guardar(s, "bpmn_05_modelo")


def generar():
    notacion(); asis(); tobe(); registro(); cierre(); modelo()


if __name__ == "__main__":
    generar()
