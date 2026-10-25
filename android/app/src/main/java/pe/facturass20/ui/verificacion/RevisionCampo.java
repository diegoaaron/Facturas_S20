package pe.facturass20.ui.verificacion;

import java.util.Objects;
import java.util.Optional;

import pe.facturass20.dominio.modelo.CampoExtraido;
import pe.facturass20.dominio.modelo.CampoFactura;
import pe.facturass20.dominio.modelo.ResultadoExtraccion;
import pe.facturass20.dominio.reglas.Montos;
import pe.facturass20.dominio.reglas.ValidadorCampos;
import pe.facturass20.dominio.reglas.ValidadorFecha;
import pe.facturass20.ui.comun.Formatos;

/**
 * Color de cada campo de P08 y P19 (RF-05): verde ✓ si es válido, ámbar ⚠ si la IA lo leyó con confianza
 * menor a 0,80 y el usuario todavía no lo tocó, y rojo ✗ si no pasa la validación del dominio. Es Java
 * puro para probarlo en la JVM.
 *
 * @param mensaje             el error para el usuario si es {@link Estado#INVALIDO}; nulo en otro caso
 * @param confianzaPorcentaje la confianza de la IA en %, para el aviso ámbar
 */
public record RevisionCampo(Estado estado, String mensaje, int confianzaPorcentaje) {

    public enum Estado { VALIDO, REVISAR, INVALIDO }

    /**
     * @param texto      lo que hay ahora en el campo
     * @param textoLeido lo que se mostró al abrir la pantalla; nulo en el registro manual
     * @param leido      el campo que devolvió la IA; nulo en el registro manual
     */
    public static RevisionCampo revisar(CampoFactura campo, String texto, String textoLeido, CampoExtraido leido) {
        Optional<String> error = ValidadorCampos.error(campo, texto);
        if (error.isPresent()) {
            return new RevisionCampo(Estado.INVALIDO, error.get(), 0);
        }
        if (leido != null && leido.confianza() < ResultadoExtraccion.UMBRAL_CONFIANZA
                && Objects.equals(texto == null ? "" : texto.trim(), textoLeido)) {
            return new RevisionCampo(Estado.REVISAR, null, (int) Math.round(leido.confianza() * 100));
        }
        return new RevisionCampo(Estado.VALIDO, null, 0);
    }

    /**
     * Cómo se muestra en el campo un valor leído por la IA: la fecha {@code 2026-09-22} como
     * {@code 22/09/2026} y el importe {@code 1450.00} como {@code 1450,00}. Si no se entiende, va tal cual.
     */
    public static String paraMostrar(CampoFactura campo, String valor) {
        if (valor == null) {
            return "";
        }
        switch (campo) {
            case FECHA_EMISION:
                return ValidadorFecha.interpretar(valor).map(Formatos::fecha).orElse(valor);
            case IMPORTE_TOTAL:
                return Montos.interpretar(valor).map(Formatos::decimal).orElse(valor);
            default:
                return valor.trim();
        }
    }
}
