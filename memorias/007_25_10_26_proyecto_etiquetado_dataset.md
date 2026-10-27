---
name: 007_25_10_26_proyecto_etiquetado_dataset
description: Reglas para etiquetar las fotos del ajuste fino (total con percepción, razón social impresa); el dataset no está en git
metadata:
  type: project
---

Dataset del ajuste fino en `tunning_gemma4/datos/` (`imagenes/` + `etiquetas.csv` con los 7 campos), **ignorado por git**: tiene RUC, nombres y montos reales, así que hay que copiarlo a mano a la otra computadora. El 2026-10-25 Claude pre-etiquetó las 52 fotos (`compr_001` a `compr_052`, todas compras de un mismo cliente, 15 emisores) y el usuario las revisó y validó; `validar_etiquetas.py` comprueba el formato. El 2026-10-27 el usuario sumó 69 fotos más (`compr_053` a `compr_121`, renombradas desde `datos/nuevas/`, donde queda `renombres.txt` con el nombre original de cada una), con 7 emisores nuevos (Goluan, AC Distribuidores, Jirusa, Cavani…) y comprobantes de octubre de 2026. `compr_053` a `compr_064` son otras fotos de comprobantes que ya estaban (lo dice su `nota`). Claude las pre-etiquetó y el usuario revisó las dudosas; `compr_110` (foto movida, ilegible) se sacó a `datos/descartadas/`, así que quedan 120 fotos (no se renumeró). Las 37 fotos con QR de SUNAT legible coinciden con las etiquetas.

Reglas de etiquetado (confirmadas por el usuario):
- **`importe_total` = monto con percepción** cuando el comprobante la trae («Monto total», «Total a pagar con», «Total pagar»), no el «Importe total» sin ella. El usuario lo fijó para Backus («siempre, ya que le agregan prorrateo») y lo validó para los demás emisores con percepción (Salem, Ecobesa, Gloria, CBC).
- **Lo impreso manda sobre el lapicero:** montos, fechas o números escritos a mano nunca se toman como dato; se anotan en `nota` como distractores.
- `razon_social` tal como está impresa en el comprobante, no el nombre oficial de SUNAT. Si solo hay logo, el logo (`JIRUSA`); en persona natural con nombre comercial («CAVANI 2.0 / DE: LIVIA PAUCCA CARLO VALENTINO»), el nombre de la persona (confirmado por el usuario el 2026-10-27).

**Why:** el modelo aprende lo que dicen las etiquetas; una regla inconsistente entre emisores le enseña respuestas contradictorias.

**How to apply:** al etiquetar fotos nuevas, seguir estas reglas y correr `validar_etiquetas.py`. Si el usuario cambia la regla de la percepción, actualizar todas las filas afectadas (su `nota` guarda el monto sin percepción). Relacionado: [[004_26_09_26_proyecto_arquitectura_on_device]].
