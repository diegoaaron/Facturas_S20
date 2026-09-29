package pe.facturass20;

import android.content.Context;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Inyección de dependencias manual (documentación técnica §3.1): crea una sola vez los adaptadores
 * que implementan los puertos del dominio y los entrega a los ViewModels.
 *
 * <p>Por ahora solo tiene el ejecutor de fondo. Aquí se irán agregando, en este orden: la base de datos
 * y los repositorios (Room + SQLCipher), el extractor de facturas (falso primero, Gemma después),
 * el cargador de parámetros del NRUS y el exportador de datos.</p>
 */
public final class ContenedorDependencias {

    private final Context contexto;
    private final ExecutorService ejecutor = Executors.newFixedThreadPool(2);

    ContenedorDependencias(Context contexto) {
        this.contexto = contexto.getApplicationContext();
    }

    /** Hilos de fondo para la base de datos, la inferencia, el PDF y la exportación; nunca el hilo de UI. */
    public ExecutorService ejecutor() {
        return ejecutor;
    }

    Context contexto() {
        return contexto;
    }
}
