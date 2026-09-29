package pe.facturass20.dominio.reglas;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import pe.facturass20.dominio.modelo.CategoriaNRUS;
import pe.facturass20.dominio.modelo.ConfiguracionNrusException;
import pe.facturass20.dominio.modelo.Determinacion;
import pe.facturass20.dominio.modelo.NivelAlerta;
import pe.facturass20.dominio.modelo.ParametrosNrus;
import pe.facturass20.dominio.modelo.PeriodoMensual;

/**
 * Determina la categoría, la cuota, el nivel de alerta y el vencimiento de un mes con los parámetros
 * del NRUS (documentación técnica §5.4). No guarda nada: el caso de uso decide qué persistir.
 */
public final class MotorReglasNRUS {

    private final ParametrosNrus parametros;

    public MotorReglasNRUS(ParametrosNrus parametros) {
        this.parametros = Objects.requireNonNull(parametros, "parametros");
    }

    /** Determina el mes sin tomar en cuenta los demás meses del año para el tope anual. */
    public Determinacion determinar(PeriodoMensual periodo, int ultimoDigitoRuc) {
        return determinar(periodo, ultimoDigitoRuc, List.of());
    }

    /**
     * @param periodosDelAnio meses registrados, para el acumulado anual; los de otros años y el propio
     *                        {@code periodo} (por si viene una copia anterior) se ignoran
     * @throws ConfiguracionNrusException si no hay categorías vigentes o el cronograma no trae la fecha
     */
    public Determinacion determinar(PeriodoMensual periodo, int ultimoDigitoRuc,
            Collection<PeriodoMensual> periodosDelAnio) {
        if (ultimoDigitoRuc < 0 || ultimoDigitoRuc > 9) {
            throw new IllegalArgumentException("Dígito fuera de rango: " + ultimoDigitoRuc);
        }
        YearMonth mes = periodo.periodo();
        BigDecimal adquisiciones = periodo.totalAdquisiciones();
        BigDecimal determinante = Montos.mayor(adquisiciones, periodo.totalVentas());

        List<CategoriaNRUS> categorias = parametros.categoriasVigentes(mes.atDay(1));
        if (categorias.isEmpty()) {
            throw new ConfiguracionNrusException("Los parámetros del NRUS " + parametros.version()
                    + " no tienen categorías vigentes para " + mes);
        }
        Optional<CategoriaNRUS> categoria = categorias.stream().filter(c -> c.admite(determinante)).findFirst();
        NivelAlerta nivel = categoria.map(c -> nivelAlerta(c, categorias, determinante))
                .orElse(NivelAlerta.FUERA_DE_REGIMEN);

        LocalDate vencimiento = parametros.fechaVencimiento(mes, ultimoDigitoRuc)
                .orElseThrow(() -> new ConfiguracionNrusException("El cronograma " + parametros.version()
                        + " no trae el vencimiento de " + mes + " para el dígito " + ultimoDigitoRuc));

        return new Determinacion(mes, adquisiciones, determinante, categoria.orElse(null),
                categoria.map(CategoriaNRUS::cuota).orElse(null), vencimiento, nivel,
                llegaAlTopeAnual(periodo, periodosDelAnio));
    }

    /**
     * LIMITE_100 si ya pasó de la categoría 1 o está justo en el límite de la última; AVISO_80 si llegó
     * al umbral de aviso de su categoría; si no, NINGUNA.
     */
    private NivelAlerta nivelAlerta(CategoriaNRUS categoria, List<CategoriaNRUS> categorias, BigDecimal monto) {
        CategoriaNRUS ultima = categorias.get(categorias.size() - 1);
        if (categoria != categorias.get(0)
                || (categoria == ultima && monto.compareTo(ultima.limiteMensual()) == 0)) {
            return NivelAlerta.LIMITE_100;
        }
        BigDecimal umbral = categoria.limiteMensual().multiply(parametros.umbralAviso());
        return monto.compareTo(umbral) >= 0 ? NivelAlerta.AVISO_80 : NivelAlerta.NINGUNA;
    }

    /** El acumulado del año, de compras o de ventas, llega al tope anual por el umbral de aviso. */
    private boolean llegaAlTopeAnual(PeriodoMensual periodo, Collection<PeriodoMensual> periodosDelAnio) {
        BigDecimal compras = periodo.totalAdquisiciones();
        BigDecimal ventas = periodo.totalVentas();
        for (PeriodoMensual otro : periodosDelAnio) {
            if (otro.anio() == periodo.anio() && !otro.periodo().equals(periodo.periodo())) {
                compras = compras.add(otro.totalAdquisiciones());
                ventas = ventas.add(otro.totalVentas());
            }
        }
        BigDecimal umbral = parametros.topeAnual().multiply(parametros.umbralAviso());
        return Montos.mayor(compras, ventas).compareTo(umbral) >= 0;
    }
}
