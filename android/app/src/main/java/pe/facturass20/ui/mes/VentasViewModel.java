package pe.facturass20.ui.mes;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.math.BigDecimal;
import java.time.YearMonth;

import pe.facturass20.dominio.modelo.PeriodoMensual;
import pe.facturass20.ui.comun.BaseViewModel;
import pe.facturass20.ui.comun.Evento;

/** P13 Ventas del mes (RF-09): un solo total por mes, que se puede cambiar hasta cerrar el mes. */
public final class VentasViewModel extends BaseViewModel {

    private final MutableLiveData<PeriodoMensual> periodo = new MutableLiveData<>();
    private final MutableLiveData<Evento<Boolean>> guardadas = new MutableLiveData<>();
    private final EntradaMonto entrada = new EntradaMonto();
    private final MutableLiveData<String> texto = new MutableLiveData<>("");

    public VentasViewModel(@NonNull Application aplicacion) {
        super(aplicacion);
    }

    /** El mes de las ventas; nulo hasta la primera carga. */
    public LiveData<PeriodoMensual> periodo() {
        return periodo;
    }

    /** Lo escrito en el teclado, con separador de miles; vacío si no hay nada. */
    public LiveData<String> texto() {
        return texto;
    }

    public LiveData<Evento<Boolean>> guardadas() {
        return guardadas;
    }

    /** @param argumento "AAAA-MM" del argumento de navegación {@code periodo}; vacío = mes en curso */
    public void cargar(String argumento) {
        if (periodo.getValue() != null) {
            return;
        }
        YearMonth mes = periodoDe(argumento);
        enFondo(() -> contenedor().periodos().buscar(mes).orElseGet(() -> PeriodoMensual.nuevo(mes)), leido -> {
            if (leido.ventasRegistradas()) {
                entrada.cargar(leido.totalVentas());
                texto.setValue(entrada.paraMostrar());
            }
            periodo.setValue(leido);
        });
    }

    public void digito(char digito) {
        entrada.digito(digito);
        texto.setValue(entrada.paraMostrar());
    }

    public void punto() {
        entrada.punto();
        texto.setValue(entrada.paraMostrar());
    }

    public void borrar() {
        entrada.borrar();
        texto.setValue(entrada.paraMostrar());
    }

    public void guardar() {
        PeriodoMensual actual = periodo.getValue();
        if (actual == null || Boolean.TRUE.equals(ocupado().getValue())) {
            return;
        }
        BigDecimal monto = entrada.monto();
        enFondo(() -> contenedor().registrarVentas().ejecutar(actual.periodo(), monto), determinacion -> {
            guardadas.setValue(new Evento<>(true));
        });
    }
}
