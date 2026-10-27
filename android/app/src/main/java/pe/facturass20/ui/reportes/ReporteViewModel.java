package pe.facturass20.ui.reportes;

import android.app.Application;
import android.content.ContentResolver;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.YearMonth;
import java.util.List;

import pe.facturass20.ContenedorDependencias;
import pe.facturass20.R;
import pe.facturass20.dominio.modelo.Contribuyente;
import pe.facturass20.dominio.modelo.Determinacion;
import pe.facturass20.dominio.modelo.FacturaCompra;
import pe.facturass20.dominio.modelo.PeriodoMensual;
import pe.facturass20.ui.comun.BaseViewModel;
import pe.facturass20.ui.comun.Evento;

/**
 * P16 Reporte (RF-15, RF-16): la vista previa con los datos del mes y el PDF (documentación técnica §8.5)
 * para compartirlo o guardarlo; los datos y el PDF los arma {@link GeneradorReporte}.
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
            return ReporteViewModel.paginas(facturas.size(), conImagen);
        }
    }

    public enum Accion { COMPARTIR, GUARDAR }

    /** El PDF ya escrito en {@code filesDir/reportes/}, para compartirlo o guardarlo donde elija el usuario. */
    public record PdfListo(Accion accion, File archivo, String nombre) { }

    private final MutableLiveData<Vista> vista = new MutableLiveData<>();
    private final MutableLiveData<Evento<PdfListo>> pdfListo = new MutableLiveData<>();

    /** Resumen y tabla (con la continuación si no cabe en la primera página), más las miniaturas. */
    static int paginas(int facturas, int conImagen) {
        int tabla = 1 + Math.max(0, (facturas - FILAS_PRIMERA_PAGINA + FILAS_POR_PAGINA - 1) / FILAS_POR_PAGINA);
        return tabla + (conImagen + MINIATURAS_POR_PAGINA - 1) / MINIATURAS_POR_PAGINA;
    }

    public ReporteViewModel(@NonNull Application aplicacion) {
        super(aplicacion);
    }

    public LiveData<Vista> vista() {
        return vista;
    }

    public void cargar(String argumentoPeriodo) {
        YearMonth mes = periodoDe(argumentoPeriodo);
        ContenedorDependencias c = contenedor();
        String sinConfigurar = texto(R.string.error_sin_configurar);
        enFondo(() -> GeneradorReporte.datos(c, mes, sinConfigurar), vista::setValue);
    }

    /** Se dispara una vez cuando el PDF está listo para la acción pedida. */
    public LiveData<Evento<PdfListo>> pdfListo() {
        return pdfListo;
    }

    /**
     * Prepara el PDF del mes (§8.5) y avisa con {@link #pdfListo()}. De un mes cerrado se usa el generado al
     * cerrarlo; de un mes abierto, o si aquel falta, se genera con los datos del momento.
     */
    public void generarPdf(Accion accion) {
        Vista datos = vista.getValue();
        if (datos == null) {
            return;
        }
        ContenedorDependencias c = contenedor();
        Application aplicacion = getApplication();
        enFondo(() -> {
            File pdf = GeneradorReporte.delCierre(c, datos.periodo());
            if (pdf == null) {
                pdf = GeneradorReporte.generar(aplicacion, c, datos);
            }
            return new PdfListo(accion, pdf, datos.nombreArchivo());
        }, listo -> pdfListo.setValue(new Evento<>(listo)));
    }

    /** Copia el PDF a donde lo eligió el usuario (selector de archivos del sistema). */
    public void guardarEn(File pdf, Uri destino) {
        ContentResolver archivos = getApplication().getContentResolver();
        enFondo(() -> {
            try (InputStream entrada = new FileInputStream(pdf);
                 OutputStream salida = archivos.openOutputStream(destino)) {
                if (salida == null) {
                    throw new IOException("No se pudo abrir el destino");
                }
                byte[] bloque = new byte[64 * 1024];
                for (int n; (n = entrada.read(bloque)) > 0; ) {
                    salida.write(bloque, 0, n);
                }
            }
            return true;
        }, guardado -> mostrarMensaje(texto(R.string.reporte_guardado)));
    }
}
