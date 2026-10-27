package pe.facturass20.dominio.casosuso;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import pe.facturass20.dominio.modelo.Aviso;
import pe.facturass20.dominio.modelo.Contribuyente;
import pe.facturass20.dominio.modelo.Determinacion;
import pe.facturass20.dominio.modelo.NivelAlerta;
import pe.facturass20.dominio.modelo.PeriodoMensual;
import pe.facturass20.dominio.modelo.TipoAviso;
import pe.facturass20.dominio.puertos.AvisoRepositorio;
import pe.facturass20.dominio.puertos.ContribuyenteRepositorio;
import pe.facturass20.dominio.puertos.PeriodoRepositorio;
import pe.facturass20.dominio.puertos.Reloj;

/**
 * Decide qué notificaciones tocan hoy (RF-14, P14 y P15) y las anota para no repetirlas:
 * <ul>
 *   <li>Vencimiento de la declaración del mes anterior: 5 días y 1 día antes. Si se pasó el de 5 días
 *       (teléfono apagado), se manda solo el más cercano.</li>
 *   <li>Límite de la categoría, una vez por mes: 80 % o 100 % (que incluye quedar fuera del régimen), en los
 *       meses abiertos actual y anterior.</li>
 *   <li>Acercarse al tope anual, una vez por año.</li>
 * </ul>
 * Lo llama {@code RecordatorioWorker} una vez al día y después de cada cambio en los montos.
 */
public final class RevisarAvisos {

    static final int DIAS_PRIMER_RECORDATORIO = 5;
    static final int DIAS_SEGUNDO_RECORDATORIO = 1;

    /** Los interruptores de P14 y P15. */
    public record Preferencias(boolean limite80, boolean limite100, boolean topeAnual, boolean vence5Dias,
            boolean vence1Dia) {

        public static Preferencias todas() {
            return new Preferencias(true, true, true, true, true);
        }
    }

    /** Una notificación para mandar, con la determinación del mes para armar el texto. */
    public record Pendiente(Aviso aviso, Determinacion determinacion) {

        /** Días que faltan para el vencimiento (0 = vence hoy); solo tiene sentido en {@code VENCIMIENTO}. */
        public long diasParaVencer(LocalDate hoy) {
            return ChronoUnit.DAYS.between(hoy, determinacion.fechaVencimiento());
        }
    }

    private final PeriodoRepositorio periodos;
    private final ContribuyenteRepositorio contribuyentes;
    private final DeterminarCategoria determinarCategoria;
    private final AvisoRepositorio avisos;
    private final Reloj reloj;

    public RevisarAvisos(PeriodoRepositorio periodos, ContribuyenteRepositorio contribuyentes,
            DeterminarCategoria determinarCategoria, AvisoRepositorio avisos, Reloj reloj) {
        this.periodos = Objects.requireNonNull(periodos);
        this.contribuyentes = Objects.requireNonNull(contribuyentes);
        this.determinarCategoria = Objects.requireNonNull(determinarCategoria);
        this.avisos = Objects.requireNonNull(avisos);
        this.reloj = Objects.requireNonNull(reloj);
    }

    /** Los avisos que tocan hoy, ya anotados como enviados. Vacía si el negocio no está configurado. */
    public List<Pendiente> ejecutar(Preferencias preferencias) {
        Optional<Contribuyente> contribuyente = contribuyentes.obtener();
        if (!contribuyente.isPresent()) {
            return List.of();
        }
        LocalDate hoy = reloj.hoy();
        YearMonth actual = YearMonth.from(hoy);
        YearMonth anterior = actual.minusMonths(1);
        YearMonth alta = YearMonth.from(contribuyente.get().fechaAlta());
        List<Pendiente> pendientes = new ArrayList<>();

        // La declaración de un mes vence el mes siguiente: la que puede vencer ahora es la del mes anterior.
        if (!anterior.isBefore(alta)) {
            vencimiento(determinarCategoria.ejecutar(anterior), preferencias, hoy).ifPresent(pendientes::add);
        }
        for (YearMonth mes : List.of(anterior, actual)) {
            Optional<PeriodoMensual> periodo = periodos.buscar(mes);
            if (periodo.isPresent() && periodo.get().estaAbierto()) {
                periodos.buscarDeterminacion(mes)
                        .flatMap(determinacion -> limite(determinacion, preferencias, hoy))
                        .ifPresent(pendientes::add);
            }
        }
        if (preferencias.topeAnual()) {
            periodos.buscarDeterminacion(actual)
                    .filter(Determinacion::avisoTopeAnual)
                    .filter(determinacion -> !topeAnualAvisado(actual.getYear()))
                    .ifPresent(determinacion -> pendientes.add(anotar(determinacion, TipoAviso.TOPE_ANUAL, hoy)));
        }
        return pendientes;
    }

    private Optional<Pendiente> vencimiento(Determinacion determinacion, Preferencias preferencias, LocalDate hoy) {
        LocalDate vence = determinacion.fechaVencimiento();
        long dias = ChronoUnit.DAYS.between(hoy, vence);
        if (dias < 0) {
            return Optional.empty();
        }
        LocalDate segundo = vence.minusDays(DIAS_SEGUNDO_RECORDATORIO);
        LocalDate primero = vence.minusDays(DIAS_PRIMER_RECORDATORIO);
        List<Aviso> mandados = avisos.listar(determinacion.periodo());
        if (dias <= DIAS_SEGUNDO_RECORDATORIO && preferencias.vence1Dia()) {
            return mandado(mandados, TipoAviso.VENCIMIENTO, segundo) ? Optional.empty()
                    : Optional.of(anotar(determinacion, TipoAviso.VENCIMIENTO, segundo));
        }
        if (dias <= DIAS_PRIMER_RECORDATORIO && preferencias.vence5Dias() && !mandado(mandados, TipoAviso.VENCIMIENTO, primero)) {
            return Optional.of(anotar(determinacion, TipoAviso.VENCIMIENTO, primero));
        }
        return Optional.empty();
    }

    private Optional<Pendiente> limite(Determinacion determinacion, Preferencias preferencias, LocalDate hoy) {
        List<Aviso> mandados = avisos.listar(determinacion.periodo());
        boolean avisado100 = mandado(mandados, TipoAviso.TOPE_100, null);
        NivelAlerta nivel = determinacion.nivelAlerta();
        if (nivel == NivelAlerta.LIMITE_100 || nivel == NivelAlerta.FUERA_DE_REGIMEN) {
            return preferencias.limite100() && !avisado100
                    ? Optional.of(anotar(determinacion, TipoAviso.TOPE_100, hoy)) : Optional.empty();
        }
        if (nivel == NivelAlerta.AVISO_80 && preferencias.limite80() && !avisado100
                && !mandado(mandados, TipoAviso.TOPE_80, null)) {
            return Optional.of(anotar(determinacion, TipoAviso.TOPE_80, hoy));
        }
        return Optional.empty();
    }

    private boolean topeAnualAvisado(int anio) {
        for (int mes = 1; mes <= 12; mes++) {
            if (mandado(avisos.listar(YearMonth.of(anio, mes)), TipoAviso.TOPE_ANUAL, null)) {
                return true;
            }
        }
        return false;
    }

    /** @param fecha nula para cualquier fecha */
    private static boolean mandado(List<Aviso> mandados, TipoAviso tipo, LocalDate fecha) {
        return mandados.stream().anyMatch(a -> a.tipo() == tipo && (fecha == null || a.fechaProgramada().equals(fecha)));
    }

    private Pendiente anotar(Determinacion determinacion, TipoAviso tipo, LocalDate fecha) {
        Aviso aviso = new Aviso(determinacion.periodo(), tipo, fecha, true);
        avisos.registrar(aviso);
        return new Pendiente(aviso, determinacion);
    }
}
