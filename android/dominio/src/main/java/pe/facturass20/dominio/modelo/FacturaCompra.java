package pe.facturass20.dominio.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

import pe.facturass20.dominio.reglas.Montos;

/**
 * Factura (o boleta) de compra registrada en un mes. Solo las VIGENTE suman al acumulado.
 *
 * <p>La anulación pasa por {@link PeriodoMensual#anularFactura}, que comprueba que el mes esté abierto.</p>
 */
public final class FacturaCompra {

    private Long id;
    private final Emisor emisor;
    private final String serie;
    private final String numero;
    private final LocalDate fechaEmision;
    private final Moneda moneda;
    private final BigDecimal importeTotal;
    private final OrigenRegistro origen;
    private EstadoFactura estado;
    private String motivoAnulacion;

    /** Constructor completo, para reconstruir una factura guardada. {@code id} es nulo si aún no se guarda. */
    public FacturaCompra(Long id, Emisor emisor, String serie, String numero, LocalDate fechaEmision,
            Moneda moneda, BigDecimal importeTotal, OrigenRegistro origen, EstadoFactura estado,
            String motivoAnulacion) {
        Objects.requireNonNull(emisor, "emisor");
        Objects.requireNonNull(fechaEmision, "fechaEmision");
        Objects.requireNonNull(moneda, "moneda");
        Objects.requireNonNull(importeTotal, "importeTotal");
        Objects.requireNonNull(origen, "origen");
        Objects.requireNonNull(estado, "estado");
        if (serie == null || !serie.matches("[A-Z0-9]{4}")) {
            throw new ReglaNegocioException("La serie tiene 4 letras o números, por ejemplo F001.");
        }
        if (numero == null || !numero.matches("\\d{1,8}")) {
            throw new ReglaNegocioException("El número tiene de 1 a 8 dígitos.");
        }
        if (!Montos.esPositivo(importeTotal)) {
            throw new ReglaNegocioException("El importe debe ser mayor que cero.");
        }
        if (estado == EstadoFactura.ANULADA && (motivoAnulacion == null || motivoAnulacion.isBlank())) {
            throw new ReglaNegocioException("Una factura anulada necesita un motivo.");
        }
        this.id = id;
        this.emisor = emisor;
        this.serie = serie;
        this.numero = numero;
        this.fechaEmision = fechaEmision;
        this.moneda = moneda;
        this.importeTotal = Montos.normalizar(importeTotal);
        this.origen = origen;
        this.estado = estado;
        this.motivoAnulacion = motivoAnulacion;
    }

    /** Factura nueva, vigente y todavía sin guardar. */
    public static FacturaCompra nueva(Emisor emisor, String serie, String numero, LocalDate fechaEmision,
            Moneda moneda, BigDecimal importeTotal, OrigenRegistro origen) {
        return new FacturaCompra(null, emisor, serie, numero, fechaEmision, moneda, importeTotal, origen,
                EstadoFactura.VIGENTE, null);
    }

    /**
     * Clave que identifica una factura: RUC del emisor, serie y número sin ceros de relleno
     * ({@code 004821} y {@code 4821} son la misma factura).
     */
    public static String claveUnica(String rucEmisor, String serie, String numero) {
        String sinCeros = numero.replaceFirst("^0+(?=\\d)", "");
        return rucEmisor + "-" + serie.toUpperCase() + "-" + sinCeros;
    }

    public String claveUnica() {
        return claveUnica(emisor.ruc(), serie, numero);
    }

    /** Lo llama el repositorio al guardar la factura por primera vez. */
    public void asignarId(long id) {
        if (this.id != null) {
            throw new IllegalStateException("La factura ya tiene id " + this.id);
        }
        this.id = id;
    }

    /** Cambia el estado; solo desde {@link PeriodoMensual#anularFactura}, que valida el mes. */
    void anular(String motivo) {
        if (estado != EstadoFactura.VIGENTE) {
            throw new ReglaNegocioException("Esta factura ya está anulada.");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaNegocioException("Escriba el motivo de la anulación.");
        }
        estado = EstadoFactura.ANULADA;
        motivoAnulacion = motivo.trim();
    }

    /** Mes al que pertenece la factura: el de su fecha de emisión. */
    public YearMonth periodo() {
        return YearMonth.from(fechaEmision);
    }

    public boolean esVigente() {
        return estado == EstadoFactura.VIGENTE;
    }

    /** Nulo mientras la factura no se guarda. */
    public Long id() {
        return id;
    }

    public Emisor emisor() {
        return emisor;
    }

    public String serie() {
        return serie;
    }

    public String numero() {
        return numero;
    }

    public LocalDate fechaEmision() {
        return fechaEmision;
    }

    public Moneda moneda() {
        return moneda;
    }

    public BigDecimal importeTotal() {
        return importeTotal;
    }

    public OrigenRegistro origen() {
        return origen;
    }

    public EstadoFactura estado() {
        return estado;
    }

    /** Nulo si la factura está vigente. */
    public String motivoAnulacion() {
        return motivoAnulacion;
    }

    @Override
    public String toString() {
        return "FacturaCompra[" + claveUnica() + ", " + fechaEmision + ", " + importeTotal + ", " + estado + "]";
    }
}
