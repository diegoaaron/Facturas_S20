package pe.facturass20.datos.parametros;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import pe.facturass20.datos.BaseDatosFacturas;
import pe.facturass20.datos.dao.ParametrosDao;
import pe.facturass20.datos.entidades.CategoriaNrusEntity;
import pe.facturass20.datos.entidades.CronogramaVencEntity;
import pe.facturass20.datos.entidades.ParametroVersionEntity;
import pe.facturass20.dominio.modelo.CategoriaNRUS;
import pe.facturass20.dominio.modelo.CronogramaVencimiento;
import pe.facturass20.dominio.modelo.ParametrosNrus;
import pe.facturass20.dominio.reglas.Montos;

/**
 * Carga {@code assets/parametros_nrus.json} en la base (documentación técnica §5.2). Si su versión no está
 * en {@code parametro_version}, la inserta con sus categorías y su cronograma y la deja como la única
 * activa; si ya estaba, no hace nada. Los meses cerrados conservan su determinación (RF-22).
 *
 * <p>Se llama al iniciar la app, en un hilo de fondo.</p>
 */
public final class CargadorParametrosNrus {

    public static final String ARCHIVO = "parametros_nrus.json";

    private final Context contexto;
    private final BaseDatosFacturas db;

    public CargadorParametrosNrus(Context contexto, BaseDatosFacturas db) {
        this.contexto = contexto.getApplicationContext();
        this.db = db;
    }

    /** @return {@code true} si se cargó una versión nueva */
    public boolean cargarSiHaceFalta() {
        return cargar(interpretar(leerAsset()));
    }

    boolean cargar(ParametrosNrus parametros) {
        ParametrosDao dao = db.parametrosDao();
        return db.runInTransaction(() -> {
            if (dao.buscar(parametros.version()) != null) {
                return false;
            }
            dao.desactivarTodas();
            long id = dao.insertarVersion(aEntidad(parametros));
            List<CategoriaNrusEntity> categorias = new ArrayList<>();
            for (CategoriaNRUS c : parametros.categorias()) {
                CategoriaNrusEntity e = new CategoriaNrusEntity();
                e.idParametro = id;
                e.codigo = c.codigo();
                e.limiteMensual = c.limiteMensual();
                e.cuota = c.cuota();
                categorias.add(e);
            }
            dao.insertarCategorias(categorias);
            List<CronogramaVencEntity> cronograma = new ArrayList<>();
            for (CronogramaVencimiento c : parametros.cronograma()) {
                CronogramaVencEntity e = new CronogramaVencEntity();
                e.idParametro = id;
                e.anio = c.anio();
                e.mes = c.mes();
                e.ultimoDigito = c.ultimoDigito();
                e.fechaLimite = c.fechaLimite();
                cronograma.add(e);
            }
            dao.insertarCronograma(cronograma);
            return true;
        });
    }

    /** Convierte el JSON en parámetros del dominio, que validan rangos y fechas. */
    public static ParametrosNrus interpretar(String json) {
        try {
            JSONObject raiz = new JSONObject(json);
            List<CategoriaNRUS> categorias = new ArrayList<>();
            JSONArray cats = raiz.getJSONArray("categorias");
            for (int i = 0; i < cats.length(); i++) {
                JSONObject c = cats.getJSONObject(i);
                categorias.add(new CategoriaNRUS(c.getInt("codigo"), monto(c, "limiteMensual"), monto(c, "cuota")));
            }
            List<CronogramaVencimiento> cronograma = new ArrayList<>();
            JSONArray filas = raiz.getJSONArray("cronograma");
            for (int i = 0; i < filas.length(); i++) {
                JSONObject f = filas.getJSONObject(i);
                cronograma.add(new CronogramaVencimiento(f.getInt("anio"), f.getInt("mes"), f.getInt("ultimoDigito"),
                        LocalDate.parse(f.getString("fechaLimite"))));
            }
            if (categorias.isEmpty()) {
                throw new IllegalArgumentException("Los parámetros no traen categorías");
            }
            return new ParametrosNrus(raiz.getString("version"), LocalDate.parse(raiz.getString("vigenteDesde")),
                    monto(raiz, "umbralAviso"), monto(raiz, "topeAnual"), categorias, cronograma);
        } catch (JSONException e) {
            throw new IllegalArgumentException(ARCHIVO + " no es válido: " + e.getMessage(), e);
        }
    }

    private static BigDecimal monto(JSONObject objeto, String nombre) throws JSONException {
        return Montos.normalizar(new BigDecimal(objeto.getString(nombre)));
    }

    private static ParametroVersionEntity aEntidad(ParametrosNrus p) {
        ParametroVersionEntity e = new ParametroVersionEntity();
        e.version = p.version();
        e.umbralAviso = p.umbralAviso();
        e.topeAnual = p.topeAnual();
        e.vigenteDesde = p.vigenteDesde();
        e.activo = true;
        return e;
    }

    private String leerAsset() {
        // Sin InputStream.readAllBytes(): solo existe desde Android 13 y la app corre desde Android 8.
        try (InputStream entrada = contexto.getAssets().open(ARCHIVO)) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] bloque = new byte[8192];
            for (int leidos; (leidos = entrada.read(bloque)) != -1; ) {
                bytes.write(bloque, 0, leidos);
            }
            return bytes.toString(StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
