package pe.facturass20.inferencia;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Supplier;

import pe.facturass20.dominio.modelo.CampoExtraido;
import pe.facturass20.dominio.modelo.CampoFactura;
import pe.facturass20.dominio.modelo.ResultadoExtraccion;
import pe.facturass20.dominio.puertos.ExtraccionException;
import pe.facturass20.dominio.puertos.ExtractorFacturas;

/**
 * Lector de prueba mientras el modelo Gemma no está convertido (iteración I3, documentación técnica §13.4):
 * no mira la foto, espera unos segundos y devuelve una factura de ejemplo con RUC válido, la fecha de hoy y
 * un número al azar (para que no salga siempre como duplicada). El importe viene con confianza baja, así se
 * ve el campo en ámbar en P08. En I4 lo reemplaza {@code ExtractorGemmaLocal}.
 */
public final class ExtractorFalso implements ExtractorFacturas {

    /** Va en {@code campo_extraido}; no hay fila en {@code modelo_local} con esta versión. */
    public static final String VERSION = "prueba";
    public static final long DEMORA_MS = 3_000;

    private static final String[][] PROVEEDORES = {
            {"20601234565", "DISTRIBUIDORA ANDINA S.A.C."},
            {"20512345671", "COMERCIAL LIMA NORTE S.A.C."},
    };

    private final Random azar;
    private final Supplier<LocalDate> hoy;
    private final long demoraMs;

    public ExtractorFalso(Supplier<LocalDate> hoy) {
        this(new Random(), hoy, DEMORA_MS);
    }

    ExtractorFalso(Random azar, Supplier<LocalDate> hoy, long demoraMs) {
        this.azar = azar;
        this.hoy = hoy;
        this.demoraMs = demoraMs;
    }

    @Override
    public ResultadoExtraccion extraer(byte[] imagenJpeg) throws ExtraccionException {
        long inicio = System.currentTimeMillis();
        try {
            Thread.sleep(demoraMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExtraccionException("Se canceló la lectura.", e);
        }
        String[] proveedor = PROVEEDORES[azar.nextInt(PROVEEDORES.length)];
        String numero = String.format(Locale.ROOT, "%06d", 1 + azar.nextInt(999_999));
        BigDecimal importe = BigDecimal.valueOf(5_000 + azar.nextInt(95_000), 2).setScale(2, RoundingMode.UNNECESSARY);

        List<CampoExtraido> campos = new ArrayList<>();
        campos.add(CampoExtraido.leido(CampoFactura.RUC, proveedor[0], 0.98));
        campos.add(CampoExtraido.leido(CampoFactura.RAZON_SOCIAL, proveedor[1], 0.95));
        campos.add(CampoExtraido.leido(CampoFactura.SERIE, "F001", 0.99));
        campos.add(CampoExtraido.leido(CampoFactura.NUMERO, numero, 0.97));
        campos.add(CampoExtraido.leido(CampoFactura.FECHA_EMISION, hoy.get().toString(), 0.96));
        campos.add(CampoExtraido.leido(CampoFactura.MONEDA, "PEN", 0.99));
        campos.add(CampoExtraido.leido(CampoFactura.IMPORTE_TOTAL, importe.toPlainString(), 0.62));
        return new ResultadoExtraccion(VERSION, System.currentTimeMillis() - inicio, campos);
    }
}
