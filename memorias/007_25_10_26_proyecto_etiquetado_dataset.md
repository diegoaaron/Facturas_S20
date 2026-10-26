---
name: 007_25_10_26_proyecto_etiquetado_dataset
description: Reglas para etiquetar las fotos del ajuste fino (total con percepción, razón social impresa); el dataset no está en git
metadata:
  type: project
---

Dataset del ajuste fino en `tunning_gemma4/datos/` (`imagenes/` + `etiquetas.csv` con los 7 campos), **ignorado por git**: tiene RUC, nombres y montos reales, así que hay que copiarlo a mano a la otra computadora. El 2026-10-25 Claude pre-etiquetó las 52 fotos (`compr_001` a `compr_052`, todas compras de un mismo cliente, 15 emisores) y el usuario las revisó y validó; `validar_etiquetas.py` comprueba el formato.

Reglas de etiquetado (confirmadas por el usuario):
- **`importe_total` = monto con percepción** cuando el comprobante la trae («Monto total», «Total a pagar con», «Total pagar»), no el «Importe total» sin ella. El usuario lo fijó para Backus («siempre, ya que le agregan prorrateo») y lo validó para los demás emisores con percepción (Salem, Ecobesa, Gloria, CBC).
- **Lo impreso manda sobre el lapicero:** montos, fechas o números escritos a mano nunca se toman como dato; se anotan en `nota` como distractores.
- `razon_social` tal como está impresa en el comprobante, no el nombre oficial de SUNAT.

**Why:** el modelo aprende lo que dicen las etiquetas; una regla inconsistente entre emisores le enseña respuestas contradictorias.

**How to apply:** al etiquetar fotos nuevas, seguir estas reglas y correr `validar_etiquetas.py`. Si el usuario cambia la regla de la percepción, actualizar todas las filas afectadas (su `nota` guarda el monto sin percepción). Relacionado: [[004_26_09_26_proyecto_arquitectura_on_device]].
