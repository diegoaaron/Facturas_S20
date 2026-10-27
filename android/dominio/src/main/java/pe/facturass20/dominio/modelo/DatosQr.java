package pe.facturass20.dominio.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lo que trae el código QR impreso en el comprobante electrónico. Cada emisor lo arma a su manera, así
 * que cualquier campo puede faltar (nulo). No trae razón social ni moneda.
 *
 * @param ruc           RUC del emisor, ya validado
 * @param serie         con la letra del tipo de comprobante ({@code B020}, aunque el QR diga {@code 020})
 * @param numero        sin los ceros de la izquierda que sí van impresos ({@code 432533})
 * @param importeTotal  sin la percepción: el QR la deja fuera aunque el comprobante la cobre
 */
public record DatosQr(String ruc, String serie, String numero, LocalDate fechaEmision, BigDecimal importeTotal) {
}
