"""Carga de Gemma 4 E2B en la GPU de 6 GB para entrenar y evaluar con Unsloth."""
import torch

from comun import MODELO_BASE


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
