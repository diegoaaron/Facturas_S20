package pe.facturass20.dominio.casosuso;

import java.time.YearMonth;
import java.util.Objects;
import java.util.Optional;

import pe.facturass20.dominio.modelo.Contribuyente;
import pe.facturass20.dominio.modelo.Determinacion;
import pe.facturass20.dominio.modelo.PeriodoMensual;
import pe.facturass20.dominio.modelo.ReglaNegocioException;
import pe.facturass20.dominio.puertos.ContribuyenteRepositorio;
import pe.facturass20.dominio.puertos.ParametrosRepositorio;
import pe.facturass20.dominio.puertos.PeriodoRepositorio;
import pe.facturass20.dominio.reglas.MotorReglasNRUS;

/**
 * Categoría, cuota, alertas y vencimiento de un mes (RF-10 a RF-13). Los demás casos de uso usan
 * {@link #calcular} para recalcular después de cada cambio.
 */
public final class DeterminarCategoria {

    private final PeriodoRepositorio periodos;
    private final ContribuyenteRepositorio contribuyentes;
    private final ParametrosRepositorio parametros;

    public DeterminarCategoria(PeriodoRepositorio periodos, ContribuyenteRepositorio contribuyentes,
            ParametrosRepositorio parametros) {
        this.periodos = Objects.requireNonNull(periodos);
        this.contribuyentes = Objects.requireNonNull(contribuyentes);
        this.parametros = Objects.requireNonNull(parametros);
    }

    /** Aplica el motor de reglas al mes tal como está en memoria, sin guardar nada. */
    public Determinacion calcular(PeriodoMensual periodo) {
        Contribuyente contribuyente = contribuyentes.obtener()
                .orElseThrow(() -> new ReglaNegocioException("Primero configure los datos de su negocio."));
        MotorReglasNRUS motor = new MotorReglasNRUS(parametros.vigentes());
        return motor.determinar(periodo, contribuyente.ultimoDigitoRuc(), periodos.listarDelAnio(periodo.anio()));
    }

    /**
     * Recalcula y guarda la determinación del mes, creándolo si todavía no existe. Si el mes está cerrado
     * devuelve la determinación congelada: los parámetros nuevos no cambian los meses cerrados (RF-22).
     */
    public Determinacion ejecutar(YearMonth mes) {
        Optional<PeriodoMensual> guardado = periodos.buscar(mes);
        if (guardado.isPresent() && !guardado.get().estaAbierto()) {
            return periodos.buscarDeterminacion(mes).orElseGet(() -> calcular(guardado.get()));
        }
        PeriodoMensual periodo = guardado.orElseGet(() -> PeriodoMensual.nuevo(mes));
        Determinacion determinacion = calcular(periodo);
        periodos.guardar(periodo, determinacion);
        return determinacion;
    }
}
