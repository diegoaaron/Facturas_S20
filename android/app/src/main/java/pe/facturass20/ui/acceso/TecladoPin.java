package pe.facturass20.ui.acceso;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import java.util.Arrays;
import java.util.function.Consumer;

import pe.facturass20.R;

/**
 * Cuatro puntos y un teclado numérico para escribir el PIN (prototipo P02). Con {@code app:sobreOscuro}
 * se dibuja en blanco sobre el color primario (P02); si no, sobre fondo claro (P01).
 *
 * <p>Al completar los 4 dígitos entrega una copia del PIN y se deshabilita hasta {@link #limpiar()}.</p>
 */
public final class TecladoPin extends LinearLayout {

    private static final int LARGO = 4;
    private static final String[] TECLAS = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫"};

    private final char[] digitos = new char[LARGO];
    private final View[] puntos = new View[LARGO];
    private final MaterialButton[] botones = new MaterialButton[TECLAS.length];
    private final LinearLayout filaPuntos;
    private final boolean sobreOscuro;
    private int escritos;
    private Consumer<char[]> alCompletar;

    public TecladoPin(Context contexto, @Nullable AttributeSet atributos) {
        super(contexto, atributos);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);
        // Sin try-with-resources: TypedArray es AutoCloseable solo desde Android 12.
        TypedArray valores = contexto.obtainStyledAttributes(atributos, R.styleable.TecladoPin);
        sobreOscuro = valores.getBoolean(R.styleable.TecladoPin_sobreOscuro, false);
        valores.recycle();
        filaPuntos = new LinearLayout(contexto);
        filaPuntos.setGravity(Gravity.CENTER);
        filaPuntos.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        addView(filaPuntos, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        crearPuntos();
        crearTeclas();
        actualizarPuntos();
    }

    public void setAlCompletar(Consumer<char[]> alCompletar) {
        this.alCompletar = alCompletar;
    }

    /** Borra lo escrito y vuelve a aceptar dígitos. */
    public void limpiar() {
        Arrays.fill(digitos, '0');
        escritos = 0;
        actualizarPuntos();
        setHabilitado(true);
    }

    public void setHabilitado(boolean habilitado) {
        for (MaterialButton boton : botones) {
            if (boton != null) {
                boton.setEnabled(habilitado);
                boton.setAlpha(habilitado ? 1f : 0.4f);
            }
        }
    }

    /** Sacude los puntos: el PIN no fue aceptado. */
    public void sacudir() {
        filaPuntos.animate().cancel();
        filaPuntos.setTranslationX(0);
        float paso = dp(10);
        filaPuntos.animate().translationX(paso).setDuration(50)
                .withEndAction(() -> filaPuntos.animate().translationX(-paso).setDuration(100)
                        .withEndAction(() -> filaPuntos.animate().translationX(0).setDuration(50)));
    }

    private void crearPuntos() {
        int tamano = (int) dp(22);
        int separacion = (int) dp(10);
        for (int i = 0; i < LARGO; i++) {
            View punto = new View(getContext());
            LayoutParams parametros = new LayoutParams(tamano, tamano);
            parametros.setMargins(separacion, 0, separacion, 0);
            filaPuntos.addView(punto, parametros);
            puntos[i] = punto;
        }
    }

    private void crearTeclas() {
        int tamano = (int) dp(64);
        int margenH = (int) dp(14);
        int margenV = (int) dp(6);
        LinearLayout fila = null;
        for (int i = 0; i < TECLAS.length; i++) {
            if (i % 3 == 0) {
                fila = new LinearLayout(getContext());
                LayoutParams parametrosFila = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                parametrosFila.topMargin = i == 0 ? (int) dp(28) : 0;
                addView(fila, parametrosFila);
            }
            LayoutParams parametros = new LayoutParams(tamano, tamano);
            parametros.setMargins(margenH, margenV, margenH, margenV);
            String tecla = TECLAS[i];
            if (tecla.isEmpty()) {
                fila.addView(new View(getContext()), parametros);
                continue;
            }
            MaterialButton boton = crearBoton(tecla);
            fila.addView(boton, parametros);
            botones[i] = boton;
        }
    }

    private MaterialButton crearBoton(String tecla) {
        MaterialButton boton = new MaterialButton(getContext());
        boton.setInsetTop(0);
        boton.setInsetBottom(0);
        boton.setPadding(0, 0, 0, 0);
        boton.setMinWidth(0);
        boton.setMinHeight(0);
        boton.setCornerRadius((int) dp(32));
        boton.setBackgroundTintList(ColorStateList.valueOf(color(sobreOscuro ? R.color.color_tecla_oscura
                : R.color.color_tecla_clara)));
        boton.setRippleColor(ColorStateList.valueOf(color(sobreOscuro ? R.color.color_tecla_oscura_pulsada
                : R.color.color_borde)));
        int colorTexto = color(sobreOscuro ? R.color.color_superficie : R.color.color_texto);
        if (tecla.equals("⌫")) {
            boton.setIconResource(R.drawable.ic_borrar);
            boton.setIconTint(ColorStateList.valueOf(colorTexto));
            boton.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
            boton.setIconPadding(0);
            boton.setIconSize((int) dp(28));
            boton.setContentDescription(getContext().getString(R.string.pin_borrar));
            boton.setOnClickListener(v -> borrar());
        } else {
            boton.setText(tecla);
            boton.setTextColor(colorTexto);
            boton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
            boton.setOnClickListener(v -> escribir(tecla.charAt(0)));
        }
        return boton;
    }

    private void escribir(char digito) {
        if (escritos >= LARGO) {
            return;
        }
        digitos[escritos++] = digito;
        actualizarPuntos();
        if (escritos == LARGO) {
            setHabilitado(false);
            if (alCompletar != null) {
                alCompletar.accept(digitos.clone());
            }
        }
    }

    private void borrar() {
        if (escritos > 0) {
            digitos[--escritos] = '0';
            actualizarPuntos();
        }
    }

    private void actualizarPuntos() {
        for (int i = 0; i < LARGO; i++) {
            boolean lleno = i < escritos;
            int fondo;
            if (sobreOscuro) {
                fondo = lleno ? R.drawable.punto_pin_lleno_claro : R.drawable.punto_pin_vacio_claro;
            } else {
                fondo = lleno ? R.drawable.punto_pin_lleno : R.drawable.punto_pin_vacio;
            }
            puntos[i].setBackgroundResource(fondo);
        }
        filaPuntos.setContentDescription(getContext().getString(R.string.pin_puntos, escritos));
    }

    @ColorInt
    private int color(int id) {
        return ContextCompat.getColor(getContext(), id);
    }

    private float dp(int valor) {
        return valor * getResources().getDisplayMetrics().density;
    }
}
