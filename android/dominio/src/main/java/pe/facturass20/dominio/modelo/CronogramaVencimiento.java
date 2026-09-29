package pe.facturass20.dominio.modelo;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

/** Una fila del cronograma de la SUNAT: fecha límite para declarar un mes según el último dígito del RUC. */
public record CronogramaVencimiento(int anio, int mes, int ultimoDigito, LocalDate fechaLimite) {

    public CronogramaVencimiento {
        if (mes < 1 || mes > 12) {
            throw new IllegalArgumentException("Mes fuera de rango: " + mes);
        }
        if (ultimoDigito < 0 || ultimoDigito > 9) {
            throw new IllegalArgumentException("Dígito fuera de rango: " + ultimoDigito);
        }
        Objects.requireNonNull(fechaLimite, "fechaLimite");
    }

    public boolean corresponde(YearMonth periodo, int digito) {
        return anio == periodo.getYear() && mes == periodo.getMonthValue() && ultimoDigito == digito;
    }
}
