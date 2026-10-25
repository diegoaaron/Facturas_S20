package pe.facturass20.ui.reportes;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import pe.facturass20.ContenedorDependencias;
import pe.facturass20.R;
import pe.facturass20.dominio.modelo.Contribuyente;
import pe.facturass20.dominio.modelo.Determinacion;
import pe.facturass20.dominio.modelo.FacturaCompra;
import pe.facturass20.dominio.modelo.PeriodoMensual;
import pe.facturass20.dominio.modelo.ReglaNegocioException;
import pe.facturass20.ui.comun.BaseViewModel;

/**
 * P16 Reporte (RF-15, RF-16): los datos que llevará el PDF del mes (documentación técnica §8.5). La
 * generación del PDF y el compartir llegan en la iteración I5; por ahora la pantalla muestra la vista
 * previa con los datos reales.
 */
public final class ReporteViewModel extends BaseViewModel {

    /** Facturas por página de miniaturas del PDF y filas de la tabla en la primera página. */
    static final int MINIATURAS_POR_PAGINA = 4;
    static final int FILAS_PRIMERA_PAGINA = 20;
    static final int FILAS_POR_PAGINA = 35;

    /**
     * @param facturas  las vigentes, por fecha
     * @param conImagen cuántas de ellas tienen foto
     */
    public record Vista(Contribuyente contribuyente, PeriodoMensual periodo, Determinacion determinacion,
            List<FacturaCompra> facturas, int conImagen) {

        /** {@code facturas-s20_2026-09.pdf} (§8.5). */
        public String nombreArchivo() {
            return "facturas-s20_" + periodo.periodo() + ".pdf";
        }

        /** Páginas que tendrá el PDF: resumen y tabla, más las miniaturas. */
        public int paginas() {
            int tabla = 1 + Math.max(0, (facturas.size() - FILAS_PRIMERA_PAGINA + FILAS_POR_PAGINA - 1) / FILAS_POR_PAGINA);
            return tabla + (conImagen + MINIATURAS_POR_PAGINA - 1) / MINIATURAS_POR_PAGINA;
        }
    }

    private final MutableLiveData<Vista> vista = new MutableLiveData<>();

    public ReporteViewModel(@NonNull Application aplicacion) {
        super(aplicacion);
    }

    public LiveData<Vista> vista() {
        return vista;
    }

    public void cargar(String argumentoPeriodo) {
        YearMonth mes = periodoDe(argumentoPeriodo);
        ContenedorDependencias c = contenedor();
        enFondo(() -> {
            Contribuyente contribuyente = c.contribuyentes().obtener()
                    .orElseThrow(() -> new ReglaNegocioException(texto(R.string.error_sin_configurar)));
            Determinacion determinacion = c.determinarCategoria().ejecutar(mes);
            PeriodoMensual periodo = c.periodos().buscar(mes).orElseGet(() -> PeriodoMensual.nuevo(mes));
            List<FacturaCompra> facturas = new ArrayList<>(periodo.facturasVigentes());
            facturas.sort(Comparator.comparing(FacturaCompra::fechaEmision));
            int conImagen = 0;
            for (FacturaCompra factura : facturas) {
                if (factura.id() != null && c.facturas().buscarImagen(factura.id()).isPresent()) {
                    conImagen++;
                }
            }
            return new Vista(contribuyente, periodo, determinacion, facturas, conImagen);
        }, vista::setValue);
    }

    public void generarPdf() {
        mostrarMensaje(texto(R.string.reporte_pronto));
    }
}
