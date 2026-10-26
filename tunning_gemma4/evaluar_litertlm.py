"""Compara por campo los `.litertlm` (oficial y convertidos) en las fotos de prueba, con LiteRT-LM en la CPU de la PC.

Es la evaluación «después de convertir» de la documentación técnica §6.7: mide el
archivo que va al teléfono, no el modelo de PyTorch. La foto se reduce como en la
app (`ReductorFoto`: lado mayor 512 px, JPEG al 90 %), la instrucción es la de
`ParserRespuesta` y la respuesta se normaliza igual que en `evaluar.py`.

Va en su propio entorno (`~/venvs/litertlm`, con `litert-lm-api` y Pillow) y deja
`modelos/<nombre>/evaluacion_litertlm_<grupo>.json`:
    python /mnt/c/reposPersonal/Facturas_S20/tunning_gemma4/evaluar_litertlm.py --nombre lora_v1
"""
import argparse
import io
import json
import time

import litert_lm
from huggingface_hub import hf_hub_download

from comun import CAMPOS, CARPETA, INSTRUCCION, MODELOS, cargar_imagen, interpretar, leer_etiquetas, normalizar

REPO_OFICIAL = 'litert-community/gemma-4-E2B-it-litert-lm'
LADO_MODELO = 512
CALIDAD_JPEG = 90
# Mismo tope que ParserRespuesta.MAX_TOKENS_RESPUESTA.
MAX_TOKENS = 200
UMBRAL = 0.9


def jpeg_como_la_app(ruta):
    salida = io.BytesIO()
    cargar_imagen(ruta, LADO_MODELO).save(salida, 'JPEG', quality=CALIDAD_JPEG)
    return salida.getvalue()


def texto(mensaje):
    """El texto de la respuesta, sea cual sea la forma en que lo entregue la API."""
    if isinstance(mensaje, str):
        return mensaje
    contenidos = getattr(mensaje, 'contents', None)
    if contenidos is not None:
        contenidos = getattr(contenidos, 'contents', contenidos)
        return ''.join(getattr(c, 'text', '') or '' for c in contenidos)
    return str(mensaje)


def evaluar(ruta_modelo, filas):
    motor = litert_lm.Engine(ruta_modelo, backend=litert_lm.Backend.CPU(), vision_backend=litert_lm.Backend.CPU())
    detalle = []
    try:
        for fila in filas:
            jpeg = jpeg_como_la_app(CARPETA / 'imagenes' / fila['archivo'])
            # Siempre el token más probable, como la app.
            conversacion = motor.create_conversation(
                sampler_config=litert_lm.SamplerConfig(top_k=1, top_p=1.0, temperature=1.0))
            inicio = time.time()
            respuesta = texto(conversacion.send_message(
                litert_lm.Contents.of(litert_lm.Content.ImageBytes(jpeg), litert_lm.Content.Text(INSTRUCCION)),
                max_output_tokens=MAX_TOKENS))
            segundos = time.time() - inicio
            conversacion.close()
            leido = interpretar(respuesta)
            esperado = {c: normalizar(c, fila[c]) for c in CAMPOS}
            errores = {c: {'esperado': esperado[c], 'leido': leido[c]} for c in CAMPOS if leido[c] != esperado[c]}
            detalle.append({'archivo': fila['archivo'], 'segundos': round(segundos, 1), 'respuesta': respuesta,
                            'errores': errores})
            print(f'  {fila["archivo"]}: {7 - len(errores)}/7 en {segundos:.0f} s', flush=True)
    finally:
        motor.close()
    por_campo = {c: sum(c not in d['errores'] for d in detalle) / len(detalle) for c in CAMPOS}
    completos = sum(not d['errores'] for d in detalle) / len(detalle)
    return {'por_campo': por_campo, 'comprobantes_completos': completos, 'detalle': detalle}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--nombre', default='lora_v1', help='carpeta dentro de modelos/ con division.json y empaquetado/')
    ap.add_argument('--grupo', default='prueba', choices=['prueba', 'validacion', 'entrenamiento'])
    ap.add_argument('--modelos', default='oficial,base,afinado',
                    help='cuáles evaluar: oficial, base (modelos/base/empaquetado) y afinado (modelos/<nombre>/empaquetado)')
    ap.add_argument('--limite', type=int, help='solo las primeras N fotos (para probar)')
    args = ap.parse_args()

    carpeta = MODELOS / args.nombre
    archivos = set(json.loads((carpeta / 'division.json').read_text(encoding='utf-8'))[args.grupo])
    filas = [f for f in leer_etiquetas() if f['archivo'] in archivos][:args.limite]
    print(f'{args.grupo}: {len(filas)} fotos de {len({f["ruc"] for f in filas})} emisores')

    rutas = {
        'oficial': lambda: hf_hub_download(REPO_OFICIAL, 'gemma-4-E2B-it.litertlm'),
        'base': lambda: str(MODELOS / 'base' / 'empaquetado' / 'model.litertlm'),
        'afinado': lambda: str(carpeta / 'empaquetado' / 'model.litertlm'),
    }
    resultados = {}
    for nombre in args.modelos.split(','):
        print(f'Modelo {nombre}:', flush=True)
        resultados[nombre] = evaluar(rutas[nombre](), filas)

    salida = carpeta / f'evaluacion_litertlm_{args.grupo}.json'
    salida.write_text(json.dumps(resultados, indent=2, ensure_ascii=False), encoding='utf-8')

    modelos = list(resultados)
    print(f'\n=== Aciertos por campo ({args.grupo}, {len(filas)} fotos) ===')
    print(f'{"campo":<24}' + ''.join(f'{m:>10}' for m in modelos))
    for c in CAMPOS + ['comprobantes_completos']:
        valores = [resultados[m]['por_campo'][c] if c in CAMPOS else resultados[m][c] for m in modelos]
        print(f'{c:<24}' + ''.join(f'{v:>10.0%}' for v in valores))
    tiempos = [sum(d['segundos'] for d in resultados[m]['detalle']) / len(filas) for m in modelos]
    print(f'{"segundos por foto (PC)":<24}' + ''.join(f'{t:>10.0f}' for t in tiempos))
    if 'afinado' in resultados:
        afinado = resultados['afinado']['por_campo']
        cumple = all(v >= UMBRAL for v in afinado.values())
        if 'oficial' in resultados:
            cumple = cumple and all(afinado[c] >= resultados['oficial']['por_campo'][c] for c in CAMPOS)
        print(f'\nCriterio §6.7 (≥ {UMBRAL:.0%} por campo y no peor que el oficial): '
              f'{"cumple" if cumple else "no cumple"}')
    print(f'Detalle en {salida}')


if __name__ == '__main__':
    main()
