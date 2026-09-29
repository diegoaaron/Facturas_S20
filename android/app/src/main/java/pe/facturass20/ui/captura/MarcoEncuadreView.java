package pe.facturass20.ui.captura;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import pe.facturass20.R;

/**
 * Marco de encuadre de P06 sobre la vista previa (prototipo pantalla_06_camara_con_encuadre.png): oscurece
 * lo que queda fuera, dibuja el borde discontinuo y las esquinas, y se pone verde cuando la nitidez y la luz
 * son buenas. Ocupa lo mismo que la vista previa, así {@link #marco()} es la parte de la foto que se lee.
 */
public class MarcoEncuadreView extends View {

    private final float margenLateral;
    private final float margenArriba;
    private final float margenAbajo;
    private final float largoEsquina;
    private final Paint pinturaFondo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pinturaBorde = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pinturaEsquina = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rectangulo = new RectF();
    private final Path esquinas = new Path();
    private final Path fuera = new Path();
    private boolean bueno;

    public MarcoEncuadreView(Context contexto, @Nullable AttributeSet atributos) {
        super(contexto, atributos);
        float dp = getResources().getDisplayMetrics().density;
        margenLateral = 24 * dp;
        margenArriba = 24 * dp;
        // Espacio para la etiqueta «Nitidez: buena · Luz: buena» debajo del marco.
        margenAbajo = 56 * dp;
        largoEsquina = 28 * dp;

        pinturaFondo.setColor(ContextCompat.getColor(contexto, R.color.color_camara_velo));
        pinturaBorde.setStyle(Paint.Style.STROKE);
        pinturaBorde.setStrokeWidth(3 * dp);
        pinturaBorde.setPathEffect(new DashPathEffect(new float[]{14 * dp, 10 * dp}, 0));
        pinturaEsquina.setStyle(Paint.Style.STROKE);
        pinturaEsquina.setStrokeWidth(6 * dp);
        pinturaEsquina.setStrokeCap(Paint.Cap.SQUARE);
        actualizarColor();
    }

    /** Verde con nitidez y luz buenas; blanco mientras no. */
    public void setBueno(boolean bueno) {
        if (this.bueno != bueno) {
            this.bueno = bueno;
            actualizarColor();
            invalidate();
        }
    }

    /** El marco como fracciones de la vista (y de la vista previa); todo si aún no se midió. */
    public MarcoEncuadre marco() {
        if (getWidth() == 0 || getHeight() == 0) {
            return MarcoEncuadre.completo();
        }
        calcularRectangulo();
        return new MarcoEncuadre(rectangulo.left / getWidth(), rectangulo.top / getHeight(),
                rectangulo.right / getWidth(), rectangulo.bottom / getHeight());
    }

    @Override
    protected void onDraw(Canvas lienzo) {
        super.onDraw(lienzo);
        calcularRectangulo();

        fuera.reset();
        fuera.setFillType(Path.FillType.EVEN_ODD);
        fuera.addRect(0, 0, getWidth(), getHeight(), Path.Direction.CW);
        fuera.addRect(rectangulo, Path.Direction.CW);
        lienzo.drawPath(fuera, pinturaFondo);

        lienzo.drawRect(rectangulo, pinturaBorde);

        RectF r = rectangulo;
        float l = largoEsquina;
        esquinas.reset();
        esquinas.moveTo(r.left, r.top + l);
        esquinas.lineTo(r.left, r.top);
        esquinas.lineTo(r.left + l, r.top);
        esquinas.moveTo(r.right - l, r.top);
        esquinas.lineTo(r.right, r.top);
        esquinas.lineTo(r.right, r.top + l);
        esquinas.moveTo(r.right, r.bottom - l);
        esquinas.lineTo(r.right, r.bottom);
        esquinas.lineTo(r.right - l, r.bottom);
        esquinas.moveTo(r.left + l, r.bottom);
        esquinas.lineTo(r.left, r.bottom);
        esquinas.lineTo(r.left, r.bottom - l);
        lienzo.drawPath(esquinas, pinturaEsquina);
    }

    private void calcularRectangulo() {
        rectangulo.set(margenLateral, margenArriba, getWidth() - margenLateral, getHeight() - margenAbajo);
    }

    private void actualizarColor() {
        int color = ContextCompat.getColor(getContext(), bueno ? R.color.color_acento : R.color.color_superficie);
        pinturaBorde.setColor(color);
        pinturaEsquina.setColor(color);
    }
}
