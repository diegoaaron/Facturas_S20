"""Fusiona el LoRA con los pesos de 16 bits del modelo base, para convertirlo a `.litertlm`.

Suma a cada peso afectado su delta `B @ A * alfa / r`, archivo por archivo de
`safetensors`, sin cargar el modelo entero: así cabe en la RAM de WSL. El LoRA
se entrenó sobre el modelo en 4 bits, pero se fusiona sobre el de 16 bits (lo
habitual en QLoRA). El resultado, con la configuración, el tokenizador y la
plantilla del base, queda en `modelos/<nombre>/fusionado/`.

    python fusionar.py --nombre lora_v1
"""
import argparse
import json
import shutil
from pathlib import Path

import torch
from huggingface_hub import snapshot_download
from safetensors import safe_open
from safetensors.torch import save_file

from comun import MODELO_BASE, MODELOS

PREFIJO = 'base_model.model.'


def leer_lora(carpeta):
    """Los pares (A, B) del adaptador, indexados por el nombre del peso base, y el factor alfa / r."""
    config = json.loads((carpeta / 'adapter_config.json').read_text(encoding='utf-8'))
    if config.get('use_rslora') or config.get('use_dora'):
        raise SystemExit('Solo se fusiona un LoRA estándar (sin rsLoRA ni DoRA).')
    factor = config['lora_alpha'] / config['r']
    pares = {}
    with safe_open(carpeta / 'adapter_model.safetensors', 'pt') as f:
        for clave in f.keys():
            for parte in ('lora_A', 'lora_B'):
                sufijo = f'.{parte}.weight'
                if clave.endswith(sufijo):
                    base = clave[len(PREFIJO):-len(sufijo)] + '.weight'
                    pares.setdefault(base, {})[parte] = f.get_tensor(clave)
    return pares, factor


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--nombre', default='lora_v1', help='carpeta dentro de modelos/ que dejó entrenar.py')
    args = ap.parse_args()

    carpeta = MODELOS / args.nombre
    salida = carpeta / 'fusionado'
    salida.mkdir(exist_ok=True)
    base = Path(snapshot_download(MODELO_BASE))
    pares, factor = leer_lora(carpeta / 'lora')
    print(f'LoRA: {len(pares)} pesos, factor {factor}')

    pendientes = set(pares)
    for archivo in sorted(base.glob('*.safetensors')):
        tensores = {}
        with safe_open(archivo, 'pt') as f:
            metadatos = f.metadata()
            for clave in f.keys():
                tensor = f.get_tensor(clave)
                par = pares.get(clave)
                if par is not None:
                    delta = (par['lora_B'].float() @ par['lora_A'].float()) * factor
                    tensor = (tensor.float() + delta).to(tensor.dtype)
                    pendientes.discard(clave)
                tensores[clave] = tensor
        save_file(tensores, salida / archivo.name, metadata=metadatos)
        print(f'  {archivo.name}: {len(tensores)} tensores')
        del tensores

    if pendientes:
        raise SystemExit(f'{len(pendientes)} pesos del LoRA no están en el modelo base, p. ej. {sorted(pendientes)[0]}')
    for archivo in base.iterdir():
        if archivo.suffix != '.safetensors':
            shutil.copy(archivo, salida / archivo.name)
    print(f'Modelo fusionado en {salida}')


if __name__ == '__main__':
    with torch.no_grad():
        main()
