package pe.facturass20.dominio.casosuso;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import pe.facturass20.dominio.modelo.CampoExtraido;
import pe.facturass20.dominio.modelo.CampoFactura;
import pe.facturass20.dominio.modelo.Emisor;
import pe.facturass20.dominio.modelo.FacturaCompra;
import pe.facturass20.dominio.modelo.Moneda;
import pe.facturass20.dominio.modelo.OrigenRegistro;
import pe.facturass20.dominio.modelo.PeriodoMensual;
import pe.facturass20.dominio.modelo.ResultadoExtraccion;
import pe.facturass20.dominio.puertos.FacturaRepositorio;
import pe.facturass20.dominio.puertos.PeriodoRepositorio;
import pe.facturass20.dominio.puertos.Reloj;
import pe.facturass20.dominio.reglas.DetectorDuplicados;
import pe.facturass20.dominio.reglas.Montos;
import pe.facturass20.dominio.reglas.ValidadorCampos;
import pe.facturass20.dominio.reglas.ValidadorFecha;

/**
 * Valida los 7 campos tal como están en P08 (o P19) antes de guardar: formato de cada campo, RUC,
 * fecha respecto del mes (RF-06) y duplicados (RF-07). No guarda nada.
 */
public final class VerificarFactura {

    /** Los 7 campos como texto, tal como los muestra o los escribió el usuario. */
    public record Datos(String ruc, String razonSocial, String serie, String numero, String fechaEmision,
            String moneda, String importeTotal) {

        /** Los valores finales de una lectura de la IA; los campos que no vinieron quedan nulos. */
        public static Datos desde(ResultadoExtraccion extraccion) {
            Map<CampoFactura, String> v = new EnumMap<>(CampoFactura.class);
            for (CampoExtraido campo : extraccion.campos()) {
                v.put(campo.nombre(), campo.valorFinal());
            }
            return new Datos(v.get(CampoFactura.RUC), v.get(CampoFactura.RAZON_SOCIAL), v.get(CampoFactura.SERIE),
                    v.get(CampoFactura.NUMERO), v.get(CampoFactura.FECHA_EMISION), v.get(CampoFactura.MONEDA),
                    v.get(CampoFactura.IMPORTE_TOTAL));
        }

        public String valor(CampoFactura campo) {
            return switch (campo) {
                case RUC -> ruc;
                case RAZON_SOCIAL -> razonSocial;
                case SERIE -> serie;
                case NUMERO -> numero;
                case FECHA_EMISION -> fechaEmision;
                case MONEDA -> moneda;
                case IMPORTE_TOTAL -> importeTotal;
            };
        }
    }

    /** Qué pintar en rojo, si hay que confirmar el mes y si la factura ya existe. */
    public static final class Resultado {

        private final Map<CampoFactura, String> errores;
        private final FacturaCompra factura;
        private final ValidadorFecha.Resultado fecha;
        private final FacturaCompra duplicado;

        Resultado(Map<CampoFactura, String> errores, FacturaCompra factura, ValidadorFecha.Resultado fecha,
                FacturaCompra duplicado) {
            this.errores = Collections.unmodifiableMap(new EnumMap<>(errores));
            this.factura = factura;
            this.fecha = fecha;
            this.duplicado = duplicado;
        }

        /** Mensaje por cada campo inválido; vacío si todos están bien. */
        public Map<CampoFactura, String> errores() {
            return errores;
        }

        /** La factura lista para {@link RegistrarFactura}; vacía si hay errores. */
        public Optional<FacturaCompra> factura() {
            return Optional.ofNullable(factura);
        }

        /** Vacío si la fecha no se pudo leer. */
        public Optional<ValidadorFecha.Resultado> fecha() {
            return Optional.ofNullable(fecha);
        }

        /** La factura ya registrada con la misma clave (P09). */
        public Optional<FacturaCompra> duplicado() {
            return Optional.ofNullable(duplicado);
        }

        /** La fecha es de un mes antiguo: preguntar antes de guardar. */
        public boolean requiereConfirmarPeriodo() {
            return fecha == ValidadorFecha.Resultado.REQUIERE_CONFIRMACION;
        }

        /** Sin errores y sin duplicado: se habilita Guardar. */
        public boolean puedeGuardar() {
            return factura != null && duplicado == null;
        }
    }

    private final PeriodoRepositorio periodos;
    private final FacturaRepositorio facturas;
    private final Reloj reloj;

    public VerificarFactura(PeriodoRepositorio periodos, FacturaRepositorio facturas, Reloj reloj) {
        this.periodos = Objects.requireNonNull(periodos);
        this.facturas = Objects.requireNonNull(facturas);
        this.reloj = Objects.requireNonNull(reloj);
    }

    public Resultado ejecutar(Datos datos, OrigenRegistro origen) {
        Map<CampoFactura, String> errores = new EnumMap<>(CampoFactura.class);
        for (CampoFactura campo : CampoFactura.values()) {
            ValidadorCampos.error(campo, datos.valor(campo)).ifPresent(error -> errores.put(campo, error));
        }

        ValidadorFecha.Resultado evaluacionFecha = null;
        Optional<LocalDate> fecha = ValidadorFecha.interpretar(datos.fechaEmision());
        if (fecha.isPresent()) {
            evaluacionFecha = ValidadorFecha.evaluar(fecha.get(), reloj.hoy(),
                    periodos.buscar(YearMonth.from(fecha.get())).map(PeriodoMensual::estado).orElse(null));
            if (evaluacionFecha == ValidadorFecha.Resultado.FUTURA) {
                errores.put(CampoFactura.FECHA_EMISION, "La fecha no puede ser posterior a hoy.");
            } else if (evaluacionFecha == ValidadorFecha.Resultado.PERIODO_CERRADO) {
                errores.put(CampoFactura.FECHA_EMISION, "El mes de esta factura ya está cerrado.");
            }
        }
        if (!errores.isEmpty()) {
            return new Resultado(errores, null, evaluacionFecha, null);
        }

        FacturaCompra factura = FacturaCompra.nueva(
                new Emisor(normal(datos, CampoFactura.RUC), normal(datos, CampoFactura.RAZON_SOCIAL)),
                normal(datos, CampoFactura.SERIE),
                normal(datos, CampoFactura.NUMERO),
                fecha.orElseThrow(),
                Moneda.valueOf(normal(datos, CampoFactura.MONEDA)),
                Montos.interpretar(datos.importeTotal()).orElseThrow(),
                origen);
        FacturaCompra duplicado = DetectorDuplicados.buscarDuplicado(factura,
                facturas.buscarPorEmisorYSerie(factura.emisor().ruc(), factura.serie())).orElse(null);
        return new Resultado(errores, factura, evaluacionFecha, duplicado);
    }

    private static String normal(Datos datos, CampoFactura campo) {
        return ValidadorCampos.normalizar(campo, datos.valor(campo));
    }
}
