"""Prueba corta de ajuste fino para saber si Gemma 4 E2B cabe en la GPU local.

Carga el modelo en 4 bits, le pone un LoRA (visión congelada, como en el
cuaderno) y entrena unos pocos pasos con las fotos de datos/. No guarda nada:
solo informa la memoria de video usada y el tiempo por paso.

Se corre dentro de WSL con el entorno ~/venvs/gemma:
    python prueba_memoria_gpu.py --pasos 10 --resolucion 512
"""
import argparse
import time

import unsloth  # noqa: F401  (debe importarse antes que transformers)
import torch
from trl import SFTConfig, SFTTrainer
from unsloth import FastVisionModel
from unsloth.trainer import UnslothVisionDataCollator

from comun import a_ejemplo, gib, leer_etiquetas
from modelo_gpu import cargar_modelo_base


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--pasos', type=int, default=10)
    ap.add_argument('--resolucion', type=int, default=512)
    ap.add_argument('--rango', type=int, default=16, help='rango r del LoRA')
    args = ap.parse_args()

    gpu = torch.cuda.get_device_properties(0)
    print(f'GPU: {gpu.name} ({gib(gpu.total_memory)})')

    ejemplos = [a_ejemplo(f, args.resolucion) for f in leer_etiquetas()]
    print(f'Ejemplos: {len(ejemplos)} a {args.resolucion} px')

    model, tokenizer = cargar_modelo_base()
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
