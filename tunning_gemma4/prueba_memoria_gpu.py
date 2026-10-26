"""Prueba corta de ajuste fino para saber si Gemma 4 E2B cabe en la GPU local.

Carga el modelo en 4 bits, le pone un LoRA (visión congelada, como en el
cuaderno) y entrena unos pocos pasos con las fotos de datos/. No guarda nada:
solo informa la memoria de video usada y el tiempo por paso.

Se corre dentro de WSL con el entorno ~/venvs/gemma:
    python prueba_memoria_gpu.py --pasos 10 --resolucion 512
"""
import argparse
import csv
import json
import time
from pathlib import Path

import unsloth  # noqa: F401  (debe importarse antes que transformers)
import torch
from PIL import Image, ImageOps
from trl import SFTConfig, SFTTrainer
from unsloth import FastVisionModel
from unsloth.trainer import UnslothVisionDataCollator

CARPETA = Path(__file__).parent / 'datos'
MODELO_BASE = 'unsloth/gemma-4-E2B-it'
CAMPOS = ['ruc', 'razon_social', 'serie', 'numero', 'fecha_emision', 'moneda', 'importe_total']
# Provisional: el formato compacto de salida aún no está decidido (documentación técnica §6.7).
# Para medir memoria basta con una respuesta de longitud parecida.
INSTRUCCION = (
    'Extrae de esta factura peruana los campos ruc, razon_social, serie, numero, '
    'fecha_emision (YYYY-MM-DD), moneda (PEN o USD) e importe_total (numero con punto decimal). '
    'Responde solo un objeto JSON.'
)


def gib(b):
    return f'{b / 1024**3:.2f} GiB'


def cargar_imagen(ruta, resolucion):
    img = ImageOps.exif_transpose(Image.open(ruta)).convert('RGB')
    img.thumbnail((resolucion, resolucion))
    return img


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


def cargar_ejemplos(resolucion):
    with open(CARPETA / 'etiquetas.csv', encoding='utf-8-sig', newline='') as f:
        filas = list(csv.DictReader(f))
    ejemplos = []
    for fila in filas:
        respuesta = json.dumps({c: (fila[c] or None) for c in CAMPOS}, ensure_ascii=False)
        ejemplos.append({'messages': [
            {'role': 'user', 'content': [
                {'type': 'image', 'image': cargar_imagen(CARPETA / 'imagenes' / fila['archivo'], resolucion)},
                {'type': 'text', 'text': INSTRUCCION},
            ]},
            {'role': 'assistant', 'content': [{'type': 'text', 'text': respuesta}]},
        ]})
    return ejemplos


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--pasos', type=int, default=10)
    ap.add_argument('--resolucion', type=int, default=512)
    ap.add_argument('--rango', type=int, default=16, help='rango r del LoRA')
    args = ap.parse_args()

    gpu = torch.cuda.get_device_properties(0)
    print(f'GPU: {gpu.name} ({gib(gpu.total_memory)})')

    ejemplos = cargar_ejemplos(args.resolucion)
    print(f'Ejemplos: {len(ejemplos)} a {args.resolucion} px')

    model, tokenizer = FastVisionModel.from_pretrained(
        MODELO_BASE,
        load_in_4bit=True,
        use_gradient_checkpointing='unsloth',
    )
    if next(model.parameters()).device.type != 'cuda':
        print('El modelo completo no cabe en la GPU: se deja embed_tokens_per_layer en la CPU.')
        model = mover_a_gpu_sin_embeddings_por_capa(model)
    model = FastVisionModel.get_peft_model(
        model,
        finetune_vision_layers=False,
        finetune_language_layers=True,
        finetune_attention_modules=True,
        finetune_mlp_modules=True,
        r=args.rango,
        lora_alpha=args.rango * 2,
        lora_dropout=0.05,
        bias='none',
        random_state=42,
    )
    torch.cuda.synchronize()
    print(f'Memoria tras cargar el modelo: {gib(torch.cuda.memory_allocated())} asignada, '
          f'{gib(torch.cuda.memory_reserved())} reservada; '
          f'dispositivo del modelo: {next(model.parameters()).device}')
    torch.cuda.reset_peak_memory_stats()

    FastVisionModel.for_training(model)
    trainer = SFTTrainer(
        model=model,
        processing_class=tokenizer,
        data_collator=UnslothVisionDataCollator(model, tokenizer, resize=args.resolucion),
        train_dataset=ejemplos,
        args=SFTConfig(
            per_device_train_batch_size=1,
            gradient_accumulation_steps=1,
            max_steps=args.pasos,
            learning_rate=2e-4,
            warmup_steps=2,
            logging_steps=1,
            optim='adamw_8bit',
            seed=42,
            report_to='none',
            output_dir='/tmp/prueba_memoria_gpu',
            save_strategy='no',
            remove_unused_columns=False,
            dataset_text_field='',
            dataset_kwargs={'skip_prepare_dataset': True},
            max_length=2048,
        ),
    )
    inicio = time.time()
    trainer.train()
    duracion = time.time() - inicio

    pico = torch.cuda.max_memory_reserved()
    print('\n=== Resultado ===')
    print(f'Pico de memoria de video: {gib(pico)} de {gib(gpu.total_memory)} ({pico / gpu.total_memory:.0%})')
    print(f'Tiempo: {duracion:.0f} s para {args.pasos} pasos ({duracion / args.pasos:.1f} s/paso)')


if __name__ == '__main__':
    main()
