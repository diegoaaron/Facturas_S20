"""Lo que comparten los scripts del ajuste fino: datos, instrucción, carga del modelo y normalización.

La instrucción se lee de `ParserRespuesta.java` para que el modelo se entrene con
el mismo texto que le manda la app, y la normalización imita a `ParserRespuesta`
para que la evaluación mida lo mismo que verá el usuario en P08.
"""
import csv
import json
import random
import re
from collections import defaultdict
from datetime import date
from decimal import Decimal
from pathlib import Path

import torch
from PIL import Image, ImageOps

RAIZ = Path(__file__).resolve().parent
CARPETA = RAIZ / 'datos'
MODELOS = RAIZ / 'modelos'
MODELO_BASE = 'unsloth/gemma-4-E2B-it'
CAMPOS = ['ruc', 'razon_social', 'serie', 'numero', 'fecha_emision', 'moneda', 'importe_total']
PARSER_JAVA = (RAIZ.parent / 'android' / 'app' / 'src' / 'main' / 'java' / 'pe' / 'facturass20'
               / 'inferencia' / 'ParserRespuesta.java')
# Marcas de la plantilla de chat de Gemma 4, para entrenar solo con la respuesta.
MARCA_USUARIO = '<|turn>user\n'
MARCA_MODELO = '<|turn>model\n'


def leer_instruccion():
    """`ParserRespuesta.INSTRUCCION` tal cual, uniendo los literales concatenados con `+`."""
    fuente = PARSER_JAVA.read_text(encoding='utf-8')
    bloque = re.search(r'String INSTRUCCION\s*=(.*?);', fuente, re.S).group(1)
    return ''.join(json.loads(f'"{literal}"') for literal in re.findall(r'"((?:[^"\\]|\\.)*)"', bloque))


INSTRUCCION = leer_instruccion()


def leer_etiquetas():
    with open(CARPETA / 'etiquetas.csv', encoding='utf-8-sig', newline='') as f:
        return list(csv.DictReader(f))


def respuesta_esperada(fila):
    """El JSON plano que debe escribir el modelo: sin espacios y con `null` en las celdas vacías."""
    return json.dumps({c: (fila[c].strip() or None) for c in CAMPOS}, ensure_ascii=False, separators=(',', ':'))


def dividir_por_emisor(filas, semilla=42, prueba=0.2, validacion=0.15):
    """Separa entrenamiento, validación y prueba sin repetir emisores (RUC) entre grupos.

    Así la prueba mide comprobantes de emisores que el modelo no vio, y no solo
    si memorizó el diseño de cada emisor. Los emisores se barajan con la semilla
    y se van llenando prueba y validación hasta su proporción; el resto entrena.
    """
    por_ruc = defaultdict(list)
    for fila in filas:
        por_ruc[fila['ruc']].append(fila)
    rucs = sorted(por_ruc)
    random.Random(semilla).shuffle(rucs)
    total = len(filas)
    grupos = {'prueba': [], 'validacion': [], 'entrenamiento': []}
    for ruc in rucs:
        if len(grupos['prueba']) < prueba * total:
            destino = 'prueba'
        elif len(grupos['validacion']) < validacion * total:
            destino = 'validacion'
        else:
            destino = 'entrenamiento'
        grupos[destino].extend(por_ruc[ruc])
    return grupos


def cargar_imagen(ruta, resolucion):
    img = ImageOps.exif_transpose(Image.open(ruta)).convert('RGB')
    img.thumbnail((resolucion, resolucion))
    return img


def a_ejemplo(fila, resolucion):
    return {'messages': [
        {'role': 'user', 'content': [
            {'type': 'image', 'image': cargar_imagen(CARPETA / 'imagenes' / fila['archivo'], resolucion)},
            {'type': 'text', 'text': INSTRUCCION},
        ]},
        {'role': 'assistant', 'content': [{'type': 'text', 'text': respuesta_esperada(fila)}]},
    ]}


def gib(b):
    return f'{b / 1024**3:.2f} GiB'


class EmbeddingEnCpu(torch.nn.Module):
    """Deja la tabla de embeddings por capa (4,4 GiB en bf16) en la CPU.

    Solo se consulta por índice y no se entrena, así que la búsqueda se hace
    en la CPU y a la GPU pasa únicamente el resultado. La tabla se guarda en
    una lista para que no sea un submódulo y `.to('cuda')` no la mueva.
    """

    def __init__(self, embedding):
        super().__init__()
        self._tabla = [embedding.to('cpu')]

    def forward(self, input_ids):
        return self._tabla[0](input_ids.to('cpu')).to(input_ids.device, non_blocking=True)


def mover_a_gpu_sin_embeddings_por_capa(model):
    """Pasa el modelo a la GPU salvo `embed_tokens_per_layer`, que no cabe en 6 GB."""
    lm = model.model.language_model
    tabla = lm.embed_tokens_per_layer
    lm.embed_tokens_per_layer = torch.nn.Identity()
    model.to('cuda')
    lm.embed_tokens_per_layer = EmbeddingEnCpu(tabla)
    return model


def cargar_modelo_base():
    """Gemma 4 E2B en 4 bits, en la GPU; si no cabe entero, con los embeddings por capa en la CPU."""
    import unsloth  # noqa: F401  (debe importarse antes que transformers)
    from unsloth import FastVisionModel
    model, tokenizer = FastVisionModel.from_pretrained(
        MODELO_BASE,
        load_in_4bit=True,
        use_gradient_checkpointing='unsloth',
    )
    if next(model.parameters()).device.type != 'cuda':
        print('El modelo completo no cabe en la GPU: se deja embed_tokens_per_layer en la CPU.')
        model = mover_a_gpu_sin_embeddings_por_capa(model)
    return model, tokenizer


# --- Normalización, igual que ParserRespuesta.normalizar ---

def _monto(texto):
    t = re.sub(r'[\s ]', '', texto.replace('S/', ''))
    if not re.fullmatch(r'[0-9.,]*[0-9][0-9.,]*', t):
        return None
    coma, punto = t.rfind(','), t.rfind('.')
    separador = -1
    if coma >= 0 and punto >= 0:
        separador = max(coma, punto)
    elif coma >= 0 or punto >= 0:
        posicion = max(coma, punto)
        if t.index(t[posicion]) == posicion and len(t) - posicion - 1 != 3:
            separador = posicion
    entera = re.sub(r'[.,]', '', t[:separador] if separador >= 0 else t) or '0'
    decimales = t[separador + 1:] if separador >= 0 else ''
    if len(decimales) > 2 or not re.fullmatch(r'\d*', decimales):
        return None
    valor = Decimal(f'{entera}.{decimales}' if decimales else entera)
    return str(valor.quantize(Decimal('0.01')))


def _fecha(texto):
    t = texto.strip()
    if re.fullmatch(r'\d{1,2}[-./]\d{1,2}[-./]\d{4}', t):
        d, m, a = re.split(r'[-./]', t)
        t = f'{a}-{int(m):02d}-{int(d):02d}'
    try:
        return date.fromisoformat(t).isoformat()
    except ValueError:
        return None


def _moneda(texto):
    t = re.sub(r'[\s.]', '', texto.upper())
    if t in ('PEN', 'S/') or t.startswith('SOL'):
        return 'PEN'
    if t in ('USD', '$', 'US$') or t.startswith(('DOLAR', 'DÓLAR')):
        return 'USD'
    return None


def normalizar(campo, valor):
    """Nulo si falta o no tiene sentido para el campo."""
    if valor is None or isinstance(valor, (dict, list)):
        return None
    texto = str(valor).strip()
    if not texto or texto.lower() == 'null':
        return None
    if campo == 'ruc':
        return re.sub(r'\D', '', texto) or None
    if campo == 'razon_social':
        return re.sub(r'\s+', ' ', texto)
    if campo in ('serie', 'numero'):
        return re.sub(r'\s', '', texto).upper()
    if campo == 'fecha_emision':
        return _fecha(texto)
    if campo == 'moneda':
        return _moneda(texto)
    if campo == 'importe_total':
        return _monto(texto)
    raise ValueError(campo)


def primer_objeto(texto):
    """El primer objeto JSON del texto, aunque venga dentro de ```json o con texto alrededor."""
    decodificador = json.JSONDecoder()
    for inicio in (i for i, c in enumerate(texto) if c == '{'):
        try:
            objeto, _ = decodificador.raw_decode(texto, inicio)
        except json.JSONDecodeError:
            continue
        if isinstance(objeto, dict):
            return objeto
    return None


def interpretar(texto):
    """Los 7 campos normalizados; todos nulos si no hay JSON. Separa `F001-004521` como la app."""
    objeto = primer_objeto(texto) or {}
    valores = {}
    for campo in CAMPOS:
        valor = objeto.get(campo)
        if isinstance(valor, dict):  # esquema anterior, con valor y confianza
            valor = valor.get('valor')
        valores[campo] = normalizar(campo, valor)
    numero = valores['numero']
    if numero and re.fullmatch(r'[A-Z0-9]{4}-\d+', numero):
        serie, resto = numero.split('-')
        if valores['serie'] in (None, serie):
            valores['serie'], valores['numero'] = serie, resto
    return valores
