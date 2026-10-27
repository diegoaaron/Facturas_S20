package pe.facturass20.ui.exportacion;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pe.facturass20.ContenedorDependencias;
import pe.facturass20.R;
import pe.facturass20.datos.repositorios.DetallesExportacionRoom;
import pe.facturass20.dominio.modelo.DatosExportacion;
import pe.facturass20.dominio.modelo.FacturaCompra;
import pe.facturass20.dominio.modelo.ImagenFactura;
import pe.facturass20.dominio.modelo.PeriodoMensual;
import pe.facturass20.dominio.modelo.ResumenExportacion;
import pe.facturass20.dominio.modelo.SolicitudExportacion;
import pe.facturass20.exportacion.ExportadorDatos;
import pe.facturass20.ui.comun.BaseViewModel;
import pe.facturass20.ui.comun.Evento;
import pe.facturass20.ui.comun.Formatos;
import pe.facturass20.ui.reportes.GeneradorReporte;

/**
 * P20 Exportar datos (RF-21, documentación técnica §9): elige un mes o un año y qué incluir, estima el
 * tamaño del ZIP y lo escribe donde eligió el usuario (SAF) o en {@code cacheDir/exportaciones/} para
 * compartirlo.
 */
public final class ExportarDatosViewModel extends BaseViewModel {

    /** Tamaño medio de una foto de 896 px en JPEG 90 y de una fila de los CSV; solo para la estimación. */
    static final long BYTES_POR_IMAGEN = 180L * 1024;
    static final long BYTES_POR_FILA = 200;
    static final long BYTES_PDF_BASE = 60L * 1024;

    /** Carpeta temporal de los ZIP para compartir; el {@code FileProvider} da acceso a ella (§10). */
    static final String CARPETA = "exportaciones";

    /** Un mes con datos: cuántas facturas (vigentes y anuladas) y cuántas fotos tiene. */
    public record Mes(YearMonth periodo, int facturas, int imagenes) { }

    /** El ZIP escrito en la carpeta temporal, listo para la hoja de compartir. */
    public record ZipListo(File archivo, String nombre) { }

    private final MutableLiveData<List<Mes>> meses = new MutableLiveData<>();
    private final MutableLiveData<Evento<ZipListo>> zipListo = new MutableLiveData<>();

    public ExportarDatosViewModel(@NonNull Application aplicacion) {
        super(aplicacion);
    }

    /** Del más reciente al más antiguo; vacía si no hay nada registrado. */
    public LiveData<List<Mes>> meses() {
        return meses;
    }

    /** Se dispara una vez cuando el ZIP para compartir está escrito. */
    public LiveData<Evento<ZipListo>> zipListo() {
        return zipListo;
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

    /** Escribe el ZIP en el archivo que eligió el usuario con el selector del sistema. */
    public void guardarEn(SolicitudExportacion solicitud, Uri destino) {
        ContenedorDependencias c = contenedor();
        ContentResolver archivos = getApplication().getContentResolver();
        Complementos complementos = new Complementos(getApplication(), c, texto(R.string.error_sin_configurar));
        enFondo(() -> {
            try (OutputStream salida = archivos.openOutputStream(destino)) {
                if (salida == null) {
                    throw new IOException("No se pudo abrir el destino");
                }
                return c.exportarDatos(new ExportadorDatos(complementos)).ejecutar(solicitud, salida);
            }
        }, resumen -> avisarListo(solicitud, resumen));
    }

    /** Escribe el ZIP en la carpeta temporal y avisa con {@link #zipListo()} para compartirlo. */
    public void compartir(SolicitudExportacion solicitud) {
        ContenedorDependencias c = contenedor();
        Context contexto = getApplication();
        Complementos complementos = new Complementos(contexto, c, texto(R.string.error_sin_configurar));
        String nombre = DatosExportacion.nombreArchivo(solicitud);
        enFondo(() -> {
            borrarTemporales(contexto);
            File carpeta = new File(contexto.getCacheDir(), CARPETA);
            if (!carpeta.isDirectory() && !carpeta.mkdirs()) {
                throw new IOException("No se pudo crear la carpeta de exportaciones");
            }
            File zip = new File(carpeta, nombre);
            try (OutputStream salida = new FileOutputStream(zip)) {
                c.exportarDatos(new ExportadorDatos(complementos)).ejecutar(solicitud, salida);
            } catch (IOException | RuntimeException e) {
                zip.delete();
                throw e;
            }
            return new ZipListo(zip, nombre);
        }, listo -> zipListo.setValue(new Evento<>(listo)));
    }

    /**
     * Borra los ZIP que quedaron de compartir: el archivo no va cifrado y no debe quedarse en el teléfono
     * (§9.1). Se llama antes de cada exportación y al iniciar la app.
     */
    public static void borrarTemporales(Context contexto) {
        File[] archivos = new File(contexto.getCacheDir(), CARPETA).listFiles();
        if (archivos == null) {
            return;
        }
        for (File archivo : archivos) {
            if (!archivo.delete()) {
                Log.w("FacturasS20", "No se borró un ZIP temporal");
            }
        }
    }

    private void avisarListo(SolicitudExportacion solicitud, ResumenExportacion resumen) {
        String periodo = solicitud.esAnual() ? String.valueOf(solicitud.anio())
                : Formatos.periodo(YearMonth.of(solicitud.anio(), solicitud.mes()));
        mostrarMensaje(getApplication().getResources().getQuantityString(R.plurals.exportar_listo,
                resumen.facturas(), resumen.facturas(), periodo));
    }

    /** Lo que el ZIP toma de la base, de las fotos cifradas y de los PDF del mes. Corre en el hilo de fondo. */
    private static final class Complementos implements ExportadorDatos.Complementos {

        private final Context contexto;
        private final ContenedorDependencias c;
        private final String sinConfigurar;
        private final DetallesExportacionRoom detalles;

        Complementos(Context contexto, ContenedorDependencias c, String sinConfigurar) {
            this.contexto = contexto;
            this.c = c;
            this.sinConfigurar = sinConfigurar;
            this.detalles = c.detallesExportacion();
        }

        @Override
        public LocalDateTime fechaRegistro(long idFactura) {
            return detalles.fechaRegistro(idFactura);
        }

        @Override
        public String versionParametros(YearMonth periodo) {
            return detalles.calculo(periodo).map(DetallesExportacionRoom.Calculo::versionParametros).orElse(null);
        }

        @Override
        public LocalDateTime fechaCalculo(YearMonth periodo) {
            return detalles.calculo(periodo).map(DetallesExportacionRoom.Calculo::fecha).orElse(null);
        }

        @Override
        public byte[] imagen(long idFactura) {
            Optional<ImagenFactura> imagen = c.facturas().buscarImagen(idFactura);
            if (!imagen.isPresent()) {
                return null;
            }
            try {
                return c.almacenImagenes().leer(imagen.get().ruta());
            } catch (RuntimeException e) {
                Log.w("FacturasS20", "Foto no incluida en la exportación: " + e.getClass().getSimpleName());
                return null;
            }
        }

        @Override
        public File reporte(YearMonth periodo) {
            try {
                return GeneradorReporte.delMes(contexto, c, periodo, sinConfigurar);
            } catch (IOException | RuntimeException e) {
                Log.w("FacturasS20", "PDF no incluido en la exportación: " + e.getClass().getSimpleName());
                return null;
            }
        }
    }
}
