package pe.facturass20.dominio.reglas;

import java.math.BigDecimal;
import java.util.Optional;

import pe.facturass20.dominio.modelo.CampoFactura;
import pe.facturass20.dominio.modelo.Moneda;

/**
 * Validación de cada uno de los 7 campos de la factura, igual para lo que lee la IA (P08) y lo que se
 * escribe a mano (P19). Los mensajes se muestran tal cual al usuario, junto al campo en rojo.
 */
public final class ValidadorCampos {

    private ValidadorCampos() { }

    /** Quita espacios y deja el valor como se guarda: RUC solo dígitos, serie y moneda en mayúsculas. */
    public static String normalizar(CampoFactura campo, String valor) {
        if (valor == null) {
            return null;
        }
        String v = valor.trim();
        return switch (campo) {
            case RUC -> v.replaceAll("[\\s\\u00A0-]", "");
            case SERIE, MONEDA -> v.toUpperCase();
            case RAZON_SOCIAL -> v.replaceAll("\\s+", " ");
            default -> v;
        };
    }

    /** Mensaje de error para mostrar junto al campo, o vacío si el valor es válido. */
    public static Optional<String> error(CampoFactura campo, String valor) {
        String v = normalizar(campo, valor);
        if (v == null || v.isEmpty()) {
            return Optional.of(mensajeFalta(campo));
        }
        return Optional.ofNullable(switch (campo) {
            case RUC -> ValidadorRuc.esValido(v) ? null : "El RUC no es válido. Revise los 11 dígitos.";
            case RAZON_SOCIAL -> null;
            case SERIE -> v.matches("[A-Z0-9]{4}") ? null : "La serie tiene 4 letras o números, por ejemplo F001.";
            case NUMERO -> v.matches("\\d{1,8}") ? null : "El número tiene de 1 a 8 dígitos.";
            case FECHA_EMISION -> ValidadorFecha.interpretar(v).isPresent()
                    ? null : "Escriba la fecha como día/mes/año, por ejemplo 22/09/2026.";
            case MONEDA -> errorMoneda(v);
            case IMPORTE_TOTAL -> errorImporte(v);
        });
    }

    private static String mensajeFalta(CampoFactura campo) {
        return switch (campo) {
            case RUC -> "Falta el RUC del proveedor.";
            case RAZON_SOCIAL -> "Falta el nombre del proveedor.";
            case SERIE -> "Falta la serie, por ejemplo F001.";
            case NUMERO -> "Falta el número de la factura.";
            case FECHA_EMISION -> "Falta la fecha de emisión.";
            case MONEDA -> "Falta la moneda.";
            case IMPORTE_TOTAL -> "Falta el importe total.";
        };
    }

    private static String errorMoneda(String v) {
        if (v.equals(Moneda.PEN.name())) {
            return null;
        }
        if (v.equals(Moneda.USD.name())) {
            return "Por ahora solo se registran compras en soles (S/).";
        }
        return "La moneda debe ser soles (PEN).";
    }

    private static String errorImporte(String v) {
        Optional<BigDecimal> monto = Montos.interpretar(v);
        if (monto.isEmpty()) {
            return "Escriba el importe total, por ejemplo 1450,00.";
        }
        return Montos.esPositivo(monto.get()) ? null : "El importe debe ser mayor que cero.";
    }
}
