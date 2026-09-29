package pe.facturass20.dominio.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import pe.facturass20.dominio.reglas.Montos;

/**
 * Parámetros del NRUS de una versión de {@code assets/parametros_nrus.json} (documentación técnica §5.2).
 * Viven fuera del código para que cambiarlos no exija modificar Java (RNF-17).
 *
 * @param umbralAviso fracción del límite a partir de la cual se avisa, p. ej. {@code 0.80}
 */
public record ParametrosNrus(String version, LocalDate vigenteDesde, BigDecimal umbralAviso,
        BigDecimal topeAnual, List<CategoriaNRUS> categorias, List<CronogramaVencimiento> cronograma) {

    public ParametrosNrus {
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(vigenteDesde, "vigenteDesde");
        Objects.requireNonNull(umbralAviso, "umbralAviso");
        topeAnual = Montos.normalizar(Objects.requireNonNull(topeAnual, "topeAnual"));
        categorias = categorias.stream()
                .sorted(Comparator.comparing(CategoriaNRUS::limiteMensual))
                .toList();
        cronograma = List.copyOf(cronograma);
    }

    /** Categorías que rigen en esa fecha, ordenadas de menor a mayor límite; vacía si la versión aún no rige. */
    public List<CategoriaNRUS> categoriasVigentes(LocalDate fecha) {
        return fecha.isBefore(vigenteDesde) ? List.of() : categorias;
    }

    /** Fecha límite para declarar ese mes según el último dígito del RUC, si el cronograma la trae. */
    public Optional<LocalDate> fechaVencimiento(YearMonth periodo, int ultimoDigitoRuc) {
        return cronograma.stream()
                .filter(fila -> fila.corresponde(periodo, ultimoDigitoRuc))
                .map(CronogramaVencimiento::fechaLimite)
                .findFirst();
    }
}
