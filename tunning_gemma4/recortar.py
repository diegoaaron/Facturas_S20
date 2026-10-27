"""Recorta y endereza las fotos del dataset, para que el modelo vea el comprobante y no la mesa.

En la app el escáner de documentos de ML Kit entrega el papel recortado y con la perspectiva
corregida; aquí se hace lo mismo con las fotos del dataset para entrenar con imágenes parecidas.
Gemma 4 lleva toda imagen a 280 tokens, así que lo que se quita de mesa se gana en detalle.

Tres pasos (Python de Windows con `opencv-python-headless`, Pillow y numpy):

    python recortar.py detectar   # propone las esquinas y el giro de las fotos que no estén en recortes.json
    python recortar.py revisar    # abre el navegador para corregir esquinas y giro, foto por foto
    python recortar.py generar    # escribe datos/recortadas/<archivo> con lo que dice recortes.json
    python recortar.py generar --automatico   # datos/recortadas_auto/: esquinas detectadas sin corregir,
                                              # como recorta la app (RecortadorComprobante); para evaluar

`datos/recortes.json` guarda, por foto, las 4 esquinas (arriba-izquierda, arriba-derecha,
abajo-derecha, abajo-izquierda) como fracciones de la foto ya girada según su EXIF, el giro
horario que deja el texto derecho (0, 90, 180 o 270) y si el usuario ya la revisó. `detectar` no
toca las revisadas.

La detección busca la tinta (más oscura que su entorno y sobre fondo claro) y no el borde del papel,
porque la hoja blanca sobre la mesa gris casi no se distingue.
"""
import argparse
import io
import json
import sys
import threading
import webbrowser
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlparse

import cv2
import numpy as np
from PIL import Image, ImageOps

RAIZ = Path(__file__).resolve().parent
CARPETA = RAIZ / 'datos'
IMAGENES = CARPETA / 'imagenes'
RECORTADAS = CARPETA / 'recortadas'
RECORTADAS_AUTO = CARPETA / 'recortadas_auto'
RECORTES = CARPETA / 'recortes.json'
LADO_DETECCION = 1024
LADO_VISTA = 1400
# Más que los 768 px que usa el modelo, para poder probar otras resoluciones sin recortar de nuevo.
LADO_RECORTADA = 1600
MARGEN = 0.03
GIROS = {90: cv2.ROTATE_90_CLOCKWISE, 180: cv2.ROTATE_180, 270: cv2.ROTATE_90_COUNTERCLOCKWISE}


def abrir(ruta):
    """La foto derecha según su EXIF, como la verían el usuario y el entrenamiento, en RGB."""
    return np.array(ImageOps.exif_transpose(Image.open(ruta)).convert('RGB'))


def reducir(rgb, lado):
    escala = lado / max(rgb.shape[:2])
    if escala >= 1:
        return rgb
    return cv2.resize(rgb, (round(rgb.shape[1] * escala), round(rgb.shape[0] * escala)),
                      interpolation=cv2.INTER_AREA)


def ordenar(puntos):
    """Arriba-izquierda, arriba-derecha, abajo-derecha, abajo-izquierda."""
    p = np.asarray(puntos, dtype=float)
    suma = p.sum(axis=1)
    resta = p[:, 1] - p[:, 0]
    return np.array([p[suma.argmin()], p[resta.argmin()], p[suma.argmax()], p[resta.argmax()]])


def mascara_tinta(gris):
    """Píxeles de tinta: más oscuros que su entorno (las sombras suaves no pasan) y sobre papel claro."""
    tinta = cv2.adaptiveThreshold(gris, 255, cv2.ADAPTIVE_THRESH_MEAN_C, cv2.THRESH_BINARY_INV, 25, 15)
    tinta[cv2.blur(gris, (41, 41)) <= 120] = 0  # la mesa de madera
    tinta = cv2.morphologyEx(tinta, cv2.MORPH_CLOSE, cv2.getStructuringElement(cv2.MORPH_RECT, (15, 15)))
    cantidad, etiquetas, datos, _ = cv2.connectedComponentsWithStats(tinta)
    grandes = [i for i in range(1, cantidad) if datos[i, cv2.CC_STAT_AREA] >= 150]
    return np.isin(etiquetas, grandes).astype(np.uint8) * 255


def detectar_esquinas(rgb):
    """Rectángulo girado que encierra la tinta, con un margen, como fracciones de la foto."""
    chica = reducir(rgb, LADO_DETECCION)
    alto, ancho = chica.shape[:2]
    puntos = cv2.findNonZero(mascara_tinta(cv2.cvtColor(chica, cv2.COLOR_RGB2GRAY)))
    if puntos is None:
        return [[0, 0], [1, 0], [1, 1], [0, 1]]
    caja = ordenar(cv2.boxPoints(cv2.minAreaRect(puntos)))
    centro = caja.mean(axis=0)
    caja = centro + (caja - centro) * (1 + 2 * MARGEN)
    caja[:, 0] = caja[:, 0].clip(0, ancho - 1) / (ancho - 1)
    caja[:, 1] = caja[:, 1].clip(0, alto - 1) / (alto - 1)
    return caja.round(4).tolist()


def enderezar(rgb, esquinas, giro, lado):
    """Corrige la perspectiva del cuadrilátero, gira y deja el lado mayor en `lado` px como máximo."""
    alto, ancho = rgb.shape[:2]
    origen = (np.asarray(esquinas, dtype=float) * [ancho - 1, alto - 1]).astype(np.float32)
    ai, ad, bd, bi = origen
    w = max(np.linalg.norm(ad - ai), np.linalg.norm(bd - bi))
    h = max(np.linalg.norm(bi - ai), np.linalg.norm(bd - ad))
    destino = np.float32([[0, 0], [w - 1, 0], [w - 1, h - 1], [0, h - 1]])
    plano = cv2.warpPerspective(rgb, cv2.getPerspectiveTransform(origen, destino),
                                (round(w), round(h)), flags=cv2.INTER_CUBIC)
    if giro:
        plano = cv2.rotate(plano, GIROS[giro])
    return reducir(plano, lado)


def adivinar_giro(rgb, esquinas):
    """0 si los renglones van de lado a lado, 90 si van de arriba abajo (la hoja está de costado).

    No distingue 90 de 270 ni 0 de 180: eso lo corrige el usuario en `revisar`.
    """
    plano = enderezar(rgb, esquinas, 0, LADO_DETECCION)
    tinta = mascara_tinta(cv2.cvtColor(plano, cv2.COLOR_RGB2GRAY)) > 0
    # Con renglones horizontales, la tinta por fila sube y baja mucho más que la tinta por columna.
    return 0 if tinta.mean(axis=1).std() >= tinta.mean(axis=0).std() else 90


def leer_recortes():
    if RECORTES.exists():
        return json.loads(RECORTES.read_text(encoding='utf-8'))
    return {}


def guardar_recortes(recortes):
    temporal = RECORTES.with_suffix('.tmp')
    temporal.write_text(json.dumps(dict(sorted(recortes.items())), ensure_ascii=False, indent=1),
                        encoding='utf-8')
    temporal.replace(RECORTES)


def fotos():
    return sorted(p.name for p in IMAGENES.glob('*.jpg'))


def detectar(_args):
    recortes = leer_recortes()
    nuevas = [a for a in fotos() if a not in recortes]
    for i, archivo in enumerate(nuevas, 1):
        rgb = abrir(IMAGENES / archivo)
        esquinas = detectar_esquinas(rgb)
        recortes[archivo] = {'esquinas': esquinas, 'giro': adivinar_giro(rgb, esquinas), 'revisado': False}
        print(f'{i}/{len(nuevas)} {archivo} giro {recortes[archivo]["giro"]}', flush=True)
    guardar_recortes(recortes)
    print(f'{len(nuevas)} detectadas; {sum(r["revisado"] for r in recortes.values())}'
          f' de {len(recortes)} revisadas. Siga con: python recortar.py revisar')


def generar(args):
    recortes = leer_recortes()
    faltan = [a for a in fotos() if a not in recortes]
    if faltan:
        sys.exit(f'Faltan en recortes.json: {", ".join(faltan)}. Corra antes: python recortar.py detectar')
    sin_revisar = [a for a, r in recortes.items() if not r['revisado']]
    if sin_revisar and not args.automatico:
        print(f'Aviso: {len(sin_revisar)} sin revisar (se recortan con lo detectado).')
    # Con --automatico se usan las esquinas detectadas y no las corregidas, pero sí el giro revisado:
    # en la app el usuario sostiene el teléfono derecho, así que la hoja ya llega derecha.
    destino = RECORTADAS_AUTO if args.automatico else RECORTADAS
    destino.mkdir(exist_ok=True)
    for i, archivo in enumerate(fotos(), 1):
        r = recortes[archivo]
        rgb = abrir(IMAGENES / archivo)
        esquinas = detectar_esquinas(rgb) if args.automatico else r['esquinas']
        plano = enderezar(rgb, esquinas, r['giro'], LADO_RECORTADA)
        Image.fromarray(plano).save(destino / archivo, 'JPEG', quality=95)
        print(f'{i}/{len(recortes)} {archivo} {plano.shape[1]}x{plano.shape[0]}', flush=True)
    print(f'Listo: {destino}')


# ---- revisión en el navegador ----

PAGINA = r"""<!doctype html>
<html lang="es"><head><meta charset="utf-8"><title>Recortes del dataset</title>
<style>
 body{margin:0;font:14px system-ui,sans-serif;background:#1e1e1e;color:#eee;display:flex;flex-direction:column;height:100vh}
 header{display:flex;gap:8px;align-items:center;padding:8px;background:#2b2b2b;flex-wrap:wrap}
 button{font:inherit;padding:4px 10px;border-radius:4px;border:1px solid #666;background:#3a3a3a;color:#eee;cursor:pointer}
 button.principal{background:#2e7d32;border-color:#2e7d32}
 main{flex:1;display:flex;gap:8px;padding:8px;min-height:0}
 #lienzo{flex:3;min-width:0;cursor:crosshair}
 #vista{flex:2;min-width:0;object-fit:contain;max-height:100%;background:#111}
 .rev{color:#81c784}.pend{color:#ffb74d}
 small{color:#aaa}
</style></head><body>
<header>
 <button id="anterior">← Anterior</button><button id="siguiente">Siguiente →</button>
 <b id="nombre"></b><span id="estado"></span><span id="avance"></span>
 <label><input type="checkbox" id="soloPendientes"> saltar revisadas</label>
 <button id="izq">↺ Girar</button><button id="der">↻ Girar</button>
 <button id="todo">Toda la foto</button><button id="detectado">Detectado</button>
 <button class="principal" id="guardar">Guardar ✓ (Enter)</button>
 <small>Arrastre las esquinas al borde del papel · R gira · ←/→ cambia de foto</small>
</header>
<main><canvas id="lienzo"></canvas><img id="vista" alt="recorte"></main>
<script>
let lista = [], indice = 0, actual = null, imagen = new Image(), arrastrando = -1, temporizador;
const lienzo = document.getElementById('lienzo'), ctx = lienzo.getContext('2d');
const $ = id => document.getElementById(id);

async function cargarLista() {
  lista = await (await fetch('/lista')).json();
  const primera = lista.findIndex(f => !f.revisado);
  mostrar(primera >= 0 ? primera : 0);
}
function mostrar(i) {
  indice = (i + lista.length) % lista.length;
  actual = JSON.parse(JSON.stringify(lista[indice]));
  $('nombre').textContent = actual.archivo;
  $('estado').innerHTML = actual.revisado ? '<span class="rev">revisada</span>' : '<span class="pend">sin revisar</span>';
  $('avance').textContent = `· ${lista.filter(f => f.revisado).length}/${lista.length} revisadas`;
  imagen = new Image();
  imagen.onload = () => { dibujar(); pedirVista(); };
  imagen.src = '/foto?archivo=' + encodeURIComponent(actual.archivo);
}
function escala() {
  const r = lienzo.getBoundingClientRect();
  lienzo.width = r.width; lienzo.height = r.height;
  const s = Math.min(r.width / imagen.width, r.height / imagen.height);
  return {s, x: (r.width - imagen.width * s) / 2, y: (r.height - imagen.height * s) / 2};
}
function aPantalla(p, e) { return [e.x + p[0] * imagen.width * e.s, e.y + p[1] * imagen.height * e.s]; }
function dibujar() {
  if (!imagen.width) return;
  const e = escala();
  ctx.drawImage(imagen, e.x, e.y, imagen.width * e.s, imagen.height * e.s);
  const pts = actual.esquinas.map(p => aPantalla(p, e));
  ctx.strokeStyle = '#ff5252'; ctx.lineWidth = 2; ctx.beginPath();
  pts.forEach((p, i) => i ? ctx.lineTo(...p) : ctx.moveTo(...p)); ctx.closePath(); ctx.stroke();
  // borde de arriba (después del giro) en verde, para ver hacia dónde queda el texto
  const k = {0: 0, 90: 3, 180: 2, 270: 1}[actual.giro];
  ctx.strokeStyle = '#69f0ae'; ctx.lineWidth = 5; ctx.beginPath();
  ctx.moveTo(...pts[k]); ctx.lineTo(...pts[(k + 1) % 4]); ctx.stroke();
  pts.forEach(p => { ctx.fillStyle = '#ffeb3b'; ctx.beginPath(); ctx.arc(p[0], p[1], 7, 0, 7); ctx.fill(); });
}
function pedirVista() {
  clearTimeout(temporizador);
  temporizador = setTimeout(() => {
    $('vista').src = '/vista?archivo=' + encodeURIComponent(actual.archivo) +
      '&esquinas=' + encodeURIComponent(JSON.stringify(actual.esquinas)) + '&giro=' + actual.giro;
  }, 150);
}
function posicion(ev) {
  const e = escala(), r = lienzo.getBoundingClientRect();
  return [(ev.clientX - r.left - e.x) / (imagen.width * e.s), (ev.clientY - r.top - e.y) / (imagen.height * e.s)];
}
lienzo.onmousedown = ev => {
  const e = escala(), r = lienzo.getBoundingClientRect();
  const d = actual.esquinas.map(p => { const q = aPantalla(p, e); return Math.hypot(q[0] - ev.clientX + r.left, q[1] - ev.clientY + r.top); });
  const m = Math.min(...d); arrastrando = m < 40 ? d.indexOf(m) : -1;
};
window.onmousemove = ev => {
  if (arrastrando < 0) return;
  const p = posicion(ev).map(v => Math.min(1, Math.max(0, v)));
  actual.esquinas[arrastrando] = p; dibujar(); pedirVista();
};
window.onmouseup = () => { arrastrando = -1; };
function girar(g) { actual.giro = (actual.giro + g + 360) % 360; dibujar(); pedirVista(); }
async function guardar() {
  await fetch('/guardar', {method: 'POST', headers: {'Content-Type': 'application/json'},
    body: JSON.stringify({archivo: actual.archivo, esquinas: actual.esquinas, giro: actual.giro})});
  actual.revisado = true; lista[indice] = actual;
  avanzar(1);
}
function avanzar(paso) {
  let i = indice;
  for (let n = 0; n < lista.length; n++) {
    i = (i + paso + lista.length) % lista.length;
    if (!$('soloPendientes').checked || !lista[i].revisado) return mostrar(i);
  }
  mostrar(indice + paso);
}
$('anterior').onclick = () => avanzar(-1); $('siguiente').onclick = () => avanzar(1);
$('izq').onclick = () => girar(-90); $('der').onclick = () => girar(90);
$('todo').onclick = () => { actual.esquinas = [[0, 0], [1, 0], [1, 1], [0, 1]]; dibujar(); pedirVista(); };
$('detectado').onclick = async () => {
  actual.esquinas = await (await fetch('/detectar?archivo=' + encodeURIComponent(actual.archivo))).json();
  dibujar(); pedirVista();
};
$('guardar').onclick = guardar;
window.onkeydown = ev => {
  if (ev.key === 'Enter') guardar();
  else if (ev.key === 'ArrowRight') avanzar(1);
  else if (ev.key === 'ArrowLeft') avanzar(-1);
  else if (ev.key === 'r' || ev.key === 'R') girar(ev.shiftKey ? -90 : 90);
};
window.onresize = dibujar;
cargarLista();
</script></body></html>
"""


class Revision(BaseHTTPRequestHandler):
    recortes = {}
    candado = threading.Lock()
    cache = {}

    def log_message(self, *_):
        pass

    def _enviar(self, cuerpo, tipo, codigo=200):
        self.send_response(codigo)
        self.send_header('Content-Type', tipo)
        self.send_header('Content-Length', str(len(cuerpo)))
        self.end_headers()
        self.wfile.write(cuerpo)

    def _json(self, datos):
        self._enviar(json.dumps(datos).encode(), 'application/json')

    def _jpeg(self, rgb):
        salida = io.BytesIO()
        Image.fromarray(rgb).save(salida, 'JPEG', quality=85)
        self._enviar(salida.getvalue(), 'image/jpeg')

    def _foto(self, archivo):
        """La foto completa en memoria (las últimas pocas), para no decodificar 4080 × 3072 a cada cambio."""
        if archivo not in self.cache:
            if len(self.cache) > 4:
                self.cache.pop(next(iter(self.cache)))
            self.cache[archivo] = abrir(IMAGENES / archivo)
        return self.cache[archivo]

    def do_GET(self):
        url = urlparse(self.path)
        q = {k: v[0] for k, v in parse_qs(url.query).items()}
        archivo = Path(q.get('archivo', '')).name
        if archivo and not (IMAGENES / archivo).is_file():
            return self._enviar(b'no existe', 'text/plain', 404)
        if url.path == '/':
            self._enviar(PAGINA.encode(), 'text/html; charset=utf-8')
        elif url.path == '/lista':
            with self.candado:
                self._json([{'archivo': a, **self.recortes[a]} for a in sorted(self.recortes)])
        elif url.path == '/foto':
            self._jpeg(reducir(self._foto(archivo), LADO_VISTA))
        elif url.path == '/vista':
            self._jpeg(enderezar(self._foto(archivo), json.loads(q['esquinas']), int(q['giro']), 900))
        elif url.path == '/detectar':
            self._json(detectar_esquinas(self._foto(archivo)))
        else:
            self._enviar(b'', 'text/plain', 404)

    def do_POST(self):
        if urlparse(self.path).path != '/guardar':
            return self._enviar(b'', 'text/plain', 404)
        datos = json.loads(self.rfile.read(int(self.headers['Content-Length'])))
        archivo = Path(datos['archivo']).name
        with self.candado:
            if archivo not in self.recortes or datos['giro'] not in (0, 90, 180, 270):
                return self._enviar(b'datos invalidos', 'text/plain', 400)
            self.recortes[archivo] = {'esquinas': [[round(x, 4), round(y, 4)] for x, y in datos['esquinas']],
                                      'giro': datos['giro'], 'revisado': True}
            guardar_recortes(self.recortes)
        self._json({'ok': True})


def revisar(args):
    recortes = leer_recortes()
    faltan = [a for a in fotos() if a not in recortes]
    if faltan:
        sys.exit(f'Faltan en recortes.json: {", ".join(faltan)}. Corra antes: python recortar.py detectar')
    Revision.recortes = recortes
    servidor = ThreadingHTTPServer(('127.0.0.1', args.puerto), Revision)
    direccion = f'http://127.0.0.1:{args.puerto}/'
    print(f'Revisión en {direccion} (Ctrl+C para terminar). Cada «Guardar» escribe recortes.json.')
    webbrowser.open(direccion)
    try:
        servidor.serve_forever()
    except KeyboardInterrupt:
        pass


if __name__ == '__main__':
    ap = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    pasos = ap.add_subparsers(dest='paso', required=True)
    pasos.add_parser('detectar', help='propone esquinas y giro de las fotos nuevas').set_defaults(funcion=detectar)
    p = pasos.add_parser('revisar', help='corrige esquinas y giro en el navegador')
    p.add_argument('--puerto', type=int, default=8765)
    p.set_defaults(funcion=revisar)
    p = pasos.add_parser('generar', help='escribe datos/recortadas/')
    p.add_argument('--automatico', action='store_true',
                   help='esquinas detectadas sin corregir, en datos/recortadas_auto/ (como la app)')
    p.set_defaults(funcion=generar)
    args = ap.parse_args()
    args.funcion(args)
