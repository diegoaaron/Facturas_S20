"""Compara por campo el modelo base y el afinado en las fotos de prueba (documentación técnica §6.7).

Toma la división que guardó `entrenar.py` en `modelos/<nombre>/division.json`,
le pide a cada modelo la lectura de cada foto con la misma instrucción que la
app y normaliza la respuesta como `ParserRespuesta`. Un campo acierta si queda
igual a la etiqueta. Criterio de aceptación: el afinado mejora al base y llega
al 90 % en cada campo.

Deja `modelos/<nombre>/evaluacion.json` con los porcentajes y cada respuesta.

    cd /tmp && python /mnt/c/reposPersonal/Facturas_S20/tunning_gemma4/evaluar.py --nombre lora_v1
"""
import argparse
import json
import time

import unsloth  # noqa: F401  (debe importarse antes que transformers)
import torch
from peft import PeftModel
from unsloth import FastVisionModel

from comun import CAMPOS, FOTOS, INSTRUCCION, MODELOS, cargar_imagen, interpretar, leer_etiquetas, normalizar
from modelo_gpu import cargar_modelo_base

UMBRAL = 0.9


def leer(model, tokenizer, imagen, max_tokens):
    mensajes = [{'role': 'user', 'content': [{'type': 'image'}, {'type': 'text', 'text': INSTRUCCION}]}]
    texto = tokenizer.apply_chat_template(mensajes, add_generation_prompt=True)
    entradas = tokenizer(imagen, texto, add_special_tokens=False, return_tensors='pt').to('cuda')
    with torch.inference_mode():
        salida = model.generate(**entradas, max_new_tokens=max_tokens, do_sample=False, use_cache=True)
    return tokenizer.decode(salida[0][entradas['input_ids'].shape[1]:], skip_special_tokens=True)


def evaluar(model, tokenizer, filas, resolucion, max_tokens, fotos):
    detalle = []
    for fila in filas:
        imagen = cargar_imagen(FOTOS[fotos] / fila['archivo'], resolucion)
        inicio = time.time()
        respuesta = leer(model, tokenizer, imagen, max_tokens)
        segundos = time.time() - inicio
        leido = interpretar(respuesta)
        esperado = {c: normalizar(c, fila[c]) for c in CAMPOS}
        aciertos = {c: leido[c] == esperado[c] for c in CAMPOS}
        detalle.append({'archivo': fila['archivo'], 'segundos': round(segundos, 1), 'respuesta': respuesta,
                        'errores': {c: {'esperado': esperado[c], 'leido': leido[c]}
                                    for c in CAMPOS if not aciertos[c]}})
        print(f'  {fila["archivo"]}: {sum(aciertos.values())}/7 en {segundos:.1f} s')
    por_campo = {c: sum(c not in d['errores'] for d in detalle) / len(detalle) for c in CAMPOS}
    completos = sum(not d['errores'] for d in detalle) / len(detalle)
    return {'por_campo': por_campo, 'comprobantes_completos': completos, 'detalle': detalle}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--nombre', default='lora_v1', help='carpeta dentro de modelos/ que dejó entrenar.py')
    ap.add_argument('--grupo', default='prueba', choices=['prueba', 'prueba_conocidos', 'validacion', 'entrenamiento'])
    ap.add_argument('--resolucion', type=int, default=512)
    ap.add_argument('--fotos', default='recortadas', choices=list(FOTOS),
                    help='recortadas por recortar.py (como el escáner de la app) u originales')
    ap.add_argument('--max-tokens', type=int, default=256)
    ap.add_argument('--sin-base', action='store_true', help='no evaluar el modelo base (ya se midió)')
    args = ap.parse_args()

    carpeta = MODELOS / args.nombre
    archivos = set(json.loads((carpeta / 'division.json').read_text(encoding='utf-8'))[args.grupo])
    filas = [f for f in leer_etiquetas() if f['archivo'] in archivos]
    print(f'{args.grupo}: {len(filas)} fotos de {len({f["ruc"] for f in filas})} emisores')

    model, tokenizer = cargar_modelo_base()
    model = PeftModel.from_pretrained(model, str(carpeta / 'lora'))
    FastVisionModel.for_inference(model)

    resultados = {}
    if not args.sin_base:
        print('Modelo base:')
        with model.disable_adapter():
            resultados['base'] = evaluar(model, tokenizer, filas, args.resolucion, args.max_tokens, args.fotos)
    print('Modelo afinado:')
    resultados['afinado'] = evaluar(model, tokenizer, filas, args.resolucion, args.max_tokens, args.fotos)

    salida = carpeta / f'evaluacion_{args.grupo}.json'
    salida.write_text(json.dumps(resultados, indent=2, ensure_ascii=False), encoding='utf-8')

    modelos = list(resultados)
    print(f'\n=== Aciertos por campo ({args.grupo}, {len(filas)} fotos) ===')
    print(f'{"campo":<15}' + ''.join(f'{m:>10}' for m in modelos))
    for c in CAMPOS + ['comprobantes_completos']:
        valores = [resultados[m]['por_campo'][c] if c in CAMPOS else resultados[m][c] for m in modelos]
        print(f'{c:<15}' + ''.join(f'{v:>10.0%}' for v in valores))
    afinado = resultados['afinado']['por_campo']
    cumple = all(v >= UMBRAL for v in afinado.values())
    if 'base' in resultados:
        cumple = cumple and all(afinado[c] >= resultados['base']['por_campo'][c] for c in CAMPOS)
    print(f'\nCriterio §6.7 (≥ {UMBRAL:.0%} por campo y no peor que el base): {"cumple" if cumple else "no cumple"}')
    print(f'Detalle en {salida}')


if __name__ == '__main__':
    main()
