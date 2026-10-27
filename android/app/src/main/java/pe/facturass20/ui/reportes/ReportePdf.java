package pe.facturass20.ui.reportes;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import pe.facturass20.R;
import pe.facturass20.dominio.modelo.Determinacion;
import pe.facturass20.dominio.modelo.FacturaCompra;
import pe.facturass20.ui.comun.Formatos;

/**
 * Dibuja el PDF del mes (RF-15, documentación técnica §8.5) con {@link PdfDocument}: A4 vertical; en la
 * primera página el encabezado, el negocio, el resumen y la tabla de facturas con el total; luego la tabla
 * sigue si no cabe y al final van las fotos, {@value ReporteViewModel#MINIATURAS_POR_PAGINA} por página.
 * Todas las páginas llevan el aviso de que no reemplaza la declaración.
 *
 * <p>Las páginas son las mismas que calcula {@link ReporteViewModel.Vista#paginas()} para la vista previa.</p>
 */
final class ReportePdf {

    // A4 en puntos (1/72 de pulgada).
    private static final int ANCHO = 595;
    private static final int ALTO = 842;
    private static final float MARGEN = 40;
    private static final float ALTO_FILA = 16;
    private static final float ALTO_PIE = 36;
    /**
     * Píxeles de la miniatura por punto de la página. {@link PdfDocument} guarda cada foto como PNG, sin
     * pérdida: a la resolución de la foto (896 px) pesaba ~600 KB y un PDF de 9 facturas, 5,5 MB. A 1,5 la
     * miniatura se reconoce y el PDF de un mes con muchas facturas sigue entrando en un correo; la foto
     * completa va en la exportación de datos.
     */
    private static final float PIXELES_POR_PUNTO = 1.5f;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    /** Columnas de la tabla: n.º, fecha, proveedor, RUC, serie-número, importe (x de inicio). */
    private static final float[] COLUMNAS = {MARGEN, 66, 120, 300, 382, 470};

    private final Context contexto;
    private final Paint titulo = texto(18, true);
    private final Paint subtitulo = texto(10, false);
    private final Paint normal = texto(10, false);
    private final Paint negrita = texto(10, true);
    private final Paint tabla = texto(8.5f, false);
    private final Paint tablaNegrita = texto(8.5f, true);
    private final Paint pie = texto(7.5f, false);
    private final Paint linea = new Paint();
    private final Paint fondoCabecera = new Paint();

    private PdfDocument documento;
    private PdfDocument.Page paginaActual;
    private int numeroPagina;
    private int totalPaginas;

    ReportePdf(Context contexto) {
        this.contexto = contexto;
        subtitulo.setColor(Color.DKGRAY);
        pie.setColor(Color.DKGRAY);
        linea.setColor(Color.LTGRAY);
        linea.setStrokeWidth(0.6f);
        fondoCabecera.setColor(Color.rgb(0xEE, 0xF1, 0xF4));
    }

    /**
     * Escribe el PDF en {@code destino}.
     *
     * @param imagen la foto de una factura (JPEG ya descifrado), o null si no tiene
     */
    void escribir(ReporteViewModel.Vista vista, LocalDateTime generado, Function<FacturaCompra, byte[]> imagen,
            File destino) throws IOException {
        List<FacturaCompra> facturas = vista.facturas();
        // Las fotos primero: el total de páginas cuenta solo las que se pudieron leer.
        List<Integer> conFoto = new ArrayList<>();
        List<byte[]> fotos = new ArrayList<>();
        for (int i = 0; i < facturas.size(); i++) {
            byte[] jpeg = imagen.apply(facturas.get(i));
            if (jpeg != null) {
                conFoto.add(i);
                fotos.add(jpeg);
            }
        }
        documento = new PdfDocument();
        numeroPagina = 0;
        totalPaginas = ReporteViewModel.paginas(facturas.size(), fotos.size());
        try {
            Canvas lienzo = nuevaPagina();
            float y = encabezado(lienzo, vista, generado);
            y = resumen(lienzo, vista, y);
            y = cabeceraTabla(lienzo, y + 14);
            int filasEnPagina = ReporteViewModel.FILAS_PRIMERA_PAGINA;
            int enEstaPagina = 0;
            for (int i = 0; i < facturas.size(); i++) {
                if (enEstaPagina == filasEnPagina) {
                    terminarPagina();
                    lienzo = nuevaPagina();
                    y = cabeceraTabla(lienzo, MARGEN);
                    filasEnPagina = ReporteViewModel.FILAS_POR_PAGINA;
                    enEstaPagina = 0;
                }
                y = fila(lienzo, i + 1, facturas.get(i), y);
                enEstaPagina++;
            }
            if (facturas.isEmpty()) {
                lienzo.drawText(contexto.getString(R.string.p04_sin_facturas), MARGEN, y + 12, normal);
                y += ALTO_FILA;
            }
            total(lienzo, vista, y);
            terminarPagina();
            miniaturas(facturas, conFoto, fotos);
            try (OutputStream salida = new FileOutputStream(destino)) {
                documento.writeTo(salida);
            }
        } finally {
            documento.close();
            documento = null;
        }
    }

    private Canvas nuevaPagina() {
        numeroPagina++;
        PdfDocument.Page pagina = documento.startPage(
                new PdfDocument.PageInfo.Builder(ANCHO, ALTO, numeroPagina).create());
        paginaActual = pagina;
        return pagina.getCanvas();
    }

    /** Pie de página y cierre. */
    private void terminarPagina() {
        Canvas lienzo = paginaActual.getCanvas();
        float y = ALTO - ALTO_PIE + 8;
        lienzo.drawLine(MARGEN, y, ANCHO - MARGEN, y, linea);
        lienzo.drawText(contexto.getString(R.string.reporte_pdf_aviso), MARGEN, y + 14, pie);
        String pagina = contexto.getString(R.string.reporte_pdf_pagina, numeroPagina, totalPaginas);
        lienzo.drawText(pagina, ANCHO - MARGEN - pie.measureText(pagina), y + 14, pie);
        documento.finishPage(paginaActual);
        paginaActual = null;
    }

    private float encabezado(Canvas lienzo, ReporteViewModel.Vista vista, LocalDateTime generado) {
        float y = MARGEN + 18;
        lienzo.drawText(contexto.getString(R.string.reporte_encabezado), MARGEN, y, titulo);
        y += 16;
        lienzo.drawText(contexto.getString(R.string.reporte_pdf_periodo, Formatos.periodo(vista.periodo().periodo()),
                Formatos.fecha(generado.toLocalDate()), HORA.format(generado)), MARGEN, y, subtitulo);
        y += 22;
        lienzo.drawText(vista.contribuyente().nombre(), MARGEN, y, negrita);
        y += 14;
        lienzo.drawText(contexto.getString(R.string.reporte_pdf_contribuyente, vista.contribuyente().ruc()),
                MARGEN, y, normal);
        return y + 10;
    }

    private float resumen(Canvas lienzo, ReporteViewModel.Vista vista, float y) {
        Determinacion d = vista.determinacion();
        lienzo.drawLine(MARGEN, y, ANCHO - MARGEN, y, linea);
        y += 16;
        y = dato(lienzo, R.string.reporte_pdf_compras, contexto.getResources().getQuantityString(
                R.plurals.reporte_pdf_compras_valor, vista.facturas().size(),
                Formatos.moneda(d.totalAdquisiciones()), vista.facturas().size()), y);
        y = dato(lienzo, R.string.reporte_ventas, vista.periodo().ventasRegistradas()
                ? Formatos.moneda(vista.periodo().totalVentas())
                : contexto.getString(R.string.p04_ventas_sin_registrar), y);
        y = dato(lienzo, R.string.reporte_determinante, Formatos.moneda(d.montoDeterminante()), y);
        y = dato(lienzo, R.string.reporte_categoria_cuota, d.fueraDeRegimen()
                ? contexto.getString(R.string.p04_fuera_nrus)
                : contexto.getString(R.string.reporte_pdf_categoria, d.categoria().codigo(), Formatos.moneda(d.cuota())), y);
        y = dato(lienzo, R.string.reporte_pdf_vencimiento, contexto.getString(R.string.reporte_pdf_vencimiento_valor,
                Formatos.fecha(d.fechaVencimiento()), vista.contribuyente().ultimoDigitoRuc()), y);
        lienzo.drawLine(MARGEN, y - 4, ANCHO - MARGEN, y - 4, linea);
        return y;
    }

    private float dato(Canvas lienzo, int etiqueta, String valor, float y) {
        lienzo.drawText(contexto.getString(etiqueta), MARGEN, y, normal);
        lienzo.drawText(valor, 220, y, negrita);
        return y + 16;
    }

    private float cabeceraTabla(Canvas lienzo, float y) {
        lienzo.drawRect(MARGEN, y, ANCHO - MARGEN, y + ALTO_FILA, fondoCabecera);
        int[] titulos = {R.string.reporte_pdf_col_numero, R.string.reporte_pdf_col_fecha,
                R.string.reporte_pdf_col_proveedor, R.string.reporte_pdf_col_ruc, R.string.reporte_pdf_col_comprobante};
        for (int i = 0; i < titulos.length; i++) {
            lienzo.drawText(contexto.getString(titulos[i]), COLUMNAS[i] + 3, y + 11.5f, tablaNegrita);
        }
        derecha(lienzo, contexto.getString(R.string.reporte_pdf_col_importe), y + 11.5f, tablaNegrita);
        return y + ALTO_FILA;
    }

    private float fila(Canvas lienzo, int numero, FacturaCompra factura, float y) {
        float base = y + 11.5f;
        lienzo.drawText(String.valueOf(numero), COLUMNAS[0] + 3, base, tabla);
        lienzo.drawText(Formatos.fecha(factura.fechaEmision()), COLUMNAS[1] + 3, base, tabla);
        lienzo.drawText(recortar(factura.emisor().razonSocial(), COLUMNAS[3] - COLUMNAS[2] - 8, tabla),
                COLUMNAS[2] + 3, base, tabla);
        lienzo.drawText(factura.emisor().ruc(), COLUMNAS[3] + 3, base, tabla);
        lienzo.drawText(comprobante(factura), COLUMNAS[4] + 3, base, tabla);
        derecha(lienzo, Formatos.moneda(factura.importeTotal()), base, tabla);
        lienzo.drawLine(MARGEN, y + ALTO_FILA, ANCHO - MARGEN, y + ALTO_FILA, linea);
        return y + ALTO_FILA;
    }

    private void total(Canvas lienzo, ReporteViewModel.Vista vista, float y) {
        float base = y + 13;
        lienzo.drawText(contexto.getString(R.string.reporte_pdf_total), COLUMNAS[4] + 3, base, negrita);
        derecha(lienzo, Formatos.moneda(vista.determinacion().totalAdquisiciones()), base, negrita);
    }

    /**
     * Las fotos, en una cuadrícula de 2 × 2 con el n.º de la tabla y la serie-número encima.
     *
     * @param conFoto la posición en {@code facturas} de cada foto
     */
    private void miniaturas(List<FacturaCompra> facturas, List<Integer> conFoto, List<byte[]> fotos) {
        float anchoCelda = (ANCHO - 2 * MARGEN - 20) / 2;
        float altoCelda = (ALTO - 2 * MARGEN - ALTO_PIE - 20) / 2;
        Canvas lienzo = null;
        for (int k = 0; k < fotos.size(); k++) {
            int posicion = k % ReporteViewModel.MINIATURAS_POR_PAGINA;
            if (posicion == 0) {
                if (lienzo != null) {
                    terminarPagina();
                }
                lienzo = nuevaPagina();
            }
            float x = MARGEN + (posicion % 2) * (anchoCelda + 20);
            float y = MARGEN + (posicion / 2) * (altoCelda + 20);
            FacturaCompra factura = facturas.get(conFoto.get(k));
            String leyenda = contexto.getString(R.string.reporte_pdf_foto, conFoto.get(k) + 1, comprobante(factura),
                    factura.emisor().razonSocial());
            lienzo.drawText(recortar(leyenda, anchoCelda, tablaNegrita), x, y + 10, tablaNegrita);
            dibujarFoto(lienzo, fotos.get(k), new RectF(x, y + 16, x + anchoCelda, y + altoCelda));
            fotos.set(k, null);
        }
        if (lienzo != null) {
            terminarPagina();
        }
    }

    /** La foto entera dentro de {@code caja}, sin deformarla, achicada a {@link #PIXELES_POR_PUNTO}. */
    private static void dibujarFoto(Canvas lienzo, byte[] jpeg, RectF caja) {
        Bitmap foto = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
        if (foto == null) {
            return;
        }
        float escala = Math.min(caja.width() / foto.getWidth(), caja.height() / foto.getHeight());
        float ancho = foto.getWidth() * escala;
        float alto = foto.getHeight() * escala;
        int anchoPixeles = Math.round(ancho * PIXELES_POR_PUNTO);
        int altoPixeles = Math.round(alto * PIXELES_POR_PUNTO);
        Bitmap miniatura = anchoPixeles < foto.getWidth()
                ? Bitmap.createScaledBitmap(foto, anchoPixeles, altoPixeles, true)
                : foto;
        RectF destino = new RectF(caja.left, caja.top, caja.left + ancho, caja.top + alto);
        lienzo.drawBitmap(miniatura, null, destino, new Paint(Paint.FILTER_BITMAP_FLAG));
        if (miniatura != foto) {
            miniatura.recycle();
        }
        foto.recycle();
    }

    private static String comprobante(FacturaCompra factura) {
        return factura.serie() + "-" + factura.numero();
    }

    private void derecha(Canvas lienzo, String texto, float base, Paint pincel) {
        lienzo.drawText(texto, ANCHO - MARGEN - 3 - pincel.measureText(texto), base, pincel);
    }

    /** Corta el texto con «…» si no cabe en {@code ancho}. */
    private static String recortar(String texto, float ancho, Paint pincel) {
        if (pincel.measureText(texto) <= ancho) {
            return texto;
        }
        int cabe = pincel.breakText(texto, true, ancho - pincel.measureText("…"), null);
        return texto.substring(0, Math.max(0, cabe)) + "…";
    }

    private static Paint texto(float tamano, boolean negrita) {
        Paint pincel = new Paint(Paint.ANTI_ALIAS_FLAG);
        pincel.setColor(Color.BLACK);
        pincel.setTextSize(tamano);
        pincel.setTypeface(negrita ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        return pincel;
    }
}
