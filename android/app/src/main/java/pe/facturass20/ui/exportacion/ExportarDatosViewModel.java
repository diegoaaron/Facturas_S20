package pe.facturass20.ui.exportacion;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import pe.facturass20.ContenedorDependencias;
import pe.facturass20.R;
import pe.facturass20.dominio.modelo.FacturaCompra;
import pe.facturass20.dominio.modelo.PeriodoMensual;
import pe.facturass20.ui.comun.BaseViewModel;

/**
 * P20 Exportar datos (RF-21, documentación técnica §9): elige un mes o un año y qué incluir, y estima el
 * tamaño del ZIP. Escribir el ZIP (ExportadorDatos, EscritorCsv, SAF y FileProvider) llega en la
 * iteración I5; por ahora la pantalla calcula lo que se exportaría con los datos reales.
 */
public final class ExportarDatosViewModel extends BaseViewModel {

    /** Tamaño medio de una foto de 896 px en JPEG 90 y de una fila de los CSV; solo para la estimación. */
    static final long BYTES_POR_IMAGEN = 180L * 1024;
    static final long BYTES_POR_FILA = 200;
    static final long BYTES_PDF_BASE = 60L * 1024;

    /** Un mes con datos: cuántas facturas (vigentes y anuladas) y cuántas fotos tiene. */
    public record Mes(YearMonth periodo, int facturas, int imagenes) { }

    private final MutableLiveData<List<Mes>> meses = new MutableLiveData<>();

    public ExportarDatosViewModel(@NonNull Application aplicacion) {
        super(aplicacion);
    }

    /** Del más reciente al más antiguo; vacía si no hay nada registrado. */
    public LiveData<List<Mes>> meses() {
        return meses;
    }

    public void cargar() {
        if (meses.getValue() != null) {
            return;
        }
        ContenedorDependencias c = contenedor();
        enFondo(() -> {
            List<Mes> resultado = new ArrayList<>();
            for (PeriodoMensual periodo : c.periodos().listar()) {
                int imagenes = 0;
                for (FacturaCompra factura : periodo.facturas()) {
                    if (factura.id() != null && c.facturas().buscarImagen(factura.id()).isPresent()) {
                        imagenes++;
                    }
                }
                if (!periodo.facturas().isEmpty() || periodo.ventasRegistradas()) {
                    resultado.add(new Mes(periodo.periodo(), periodo.facturas().size(), imagenes));
                }
            }
            return resultado;
        }, meses::setValue);
    }

    /** Tamaño aproximado del ZIP en bytes. */
    public static long estimarBytes(int facturas, int imagenes, int meses, boolean conImagenes, boolean conPdf) {
        long bytes = 4L * 1024 + facturas * BYTES_POR_FILA;
        if (conImagenes) {
            bytes += imagenes * BYTES_POR_IMAGEN;
        }
        if (conPdf) {
            bytes += meses * BYTES_PDF_BASE + imagenes * BYTES_POR_IMAGEN / 4;
        }
        return bytes;
    }

    public void exportar() {
        mostrarMensaje(texto(R.string.exportar_pronto));
    }
}
