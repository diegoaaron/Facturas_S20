package pe.facturass20.ui.comun;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import pe.facturass20.R;

/**
 * Barra de avance hacia un límite con la marca del umbral de aviso (la línea negra del 80 % del
 * prototipo): compras de P10, compras y ventas de P14 y acumulado anual de P17. P04 usa la misma idea con
 * un {@code LinearProgressIndicator} y una guía.
 */
public final class BarraLimiteView extends View {

    private final Paint pintura = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rectangulo = new RectF();
    private final float grosor;
    private final float anchoMarca;
    private final float altoMinimo;
    @ColorInt
    private final int colorPista;
    @ColorInt
    private final int colorMarca;
    @ColorInt
    private int colorIndicador;
    private int progreso;
    private float marca = -1;

    public BarraLimiteView(Context contexto, @Nullable AttributeSet atributos) {
        super(contexto, atributos);
        grosor = dp(14);
        anchoMarca = dp(2);
        altoMinimo = dp(26);
        colorPista = ContextCompat.getColor(contexto, R.color.color_pista_barra);
        colorMarca = ContextCompat.getColor(contexto, R.color.color_texto);
        colorIndicador = ContextCompat.getColor(contexto, R.color.color_acento);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    /** De 0 a 100; los valores fuera de rango se recortan. */
    public void setProgreso(int progreso) {
        this.progreso = Math.max(0, Math.min(progreso, 100));
        invalidate();
    }

    public void setColorIndicador(@ColorInt int color) {
        colorIndicador = color;
        invalidate();
    }

    /** Posición de la marca como fracción del ancho (p. ej. 0,80); negativa para no dibujarla. */
    public void setMarca(float fraccion) {
        marca = fraccion;
        invalidate();
    }

    @Override
    protected void onMeasure(int anchoMedida, int altoMedida) {
        int alto = resolveSize((int) altoMinimo + getPaddingTop() + getPaddingBottom(), altoMedida);
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), anchoMedida), alto);
    }

    @Override
    protected void onDraw(Canvas lienzo) {
        float izquierda = getPaddingLeft();
        float derecha = getWidth() - getPaddingRight();
        float centro = getPaddingTop() + (getHeight() - getPaddingTop() - getPaddingBottom()) / 2f;
        float radio = grosor / 2;

        pintura.setColor(colorPista);
        rectangulo.set(izquierda, centro - radio, derecha, centro + radio);
        lienzo.drawRoundRect(rectangulo, radio, radio, pintura);

        if (progreso > 0) {
            pintura.setColor(colorIndicador);
            float fin = izquierda + (derecha - izquierda) * progreso / 100f;
            rectangulo.set(izquierda, centro - radio, Math.max(fin, izquierda + grosor), centro + radio);
            lienzo.drawRoundRect(rectangulo, radio, radio, pintura);
        }

        if (marca >= 0) {
            pintura.setColor(colorMarca);
            float x = izquierda + (derecha - izquierda) * Math.min(marca, 1f);
            rectangulo.set(x - anchoMarca / 2, centro - altoMinimo / 2, x + anchoMarca / 2, centro + altoMinimo / 2);
            lienzo.drawRect(rectangulo, pintura);
        }
    }

    private float dp(float valor) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valor, getResources().getDisplayMetrics());
    }
}
