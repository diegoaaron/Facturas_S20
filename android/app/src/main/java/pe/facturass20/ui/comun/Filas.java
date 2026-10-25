package pe.facturass20.ui.comun;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.function.Consumer;

import pe.facturass20.R;
import pe.facturass20.databinding.ItemEnlaceBinding;
import pe.facturass20.databinding.ItemInterruptorBinding;

/** Filas de las tarjetas de ajustes y avisos: un enlace con flecha o un texto con interruptor. */
public final class Filas {

    private Filas() { }

    /** @param alTocar nulo para una fila solo informativa, sin flecha */
    public static ItemEnlaceBinding enlace(ViewGroup contenedor, CharSequence titulo, @Nullable CharSequence detalle,
            @Nullable Runnable alTocar) {
        ItemEnlaceBinding fila = ItemEnlaceBinding.inflate(LayoutInflater.from(contenedor.getContext()), contenedor, true);
        fila.textoTitulo.setText(titulo);
        fila.textoDetalle.setText(detalle);
        fila.textoDetalle.setVisibility(detalle == null ? View.GONE : View.VISIBLE);
        if (alTocar == null) {
            fila.icono.setVisibility(View.GONE);
            fila.getRoot().setBackground(null);
        } else {
            fila.getRoot().setOnClickListener(v -> alTocar.run());
        }
        return fila;
    }

    public static ItemInterruptorBinding interruptor(ViewGroup contenedor, CharSequence titulo,
            @Nullable CharSequence detalle, boolean activado, Consumer<Boolean> alCambiar) {
        ItemInterruptorBinding fila = ItemInterruptorBinding.inflate(LayoutInflater.from(contenedor.getContext()),
                contenedor, true);
        fila.textoTitulo.setText(titulo);
        fila.textoDetalle.setText(detalle);
        fila.textoDetalle.setVisibility(detalle == null ? View.GONE : View.VISIBLE);
        fila.interruptor.setChecked(activado);
        fila.interruptor.setContentDescription(titulo);
        fila.interruptor.setOnCheckedChangeListener((boton, marcado) -> alCambiar.accept(marcado));
        fila.getRoot().setOnClickListener(v -> fila.interruptor.toggle());
        return fila;
    }

    /** Línea fina entre filas de una misma tarjeta. */
    public static void separador(ViewGroup contenedor) {
        View linea = new View(contenedor.getContext());
        linea.setBackgroundColor(ContextCompat.getColor(contenedor.getContext(), R.color.color_borde));
        ViewGroup.MarginLayoutParams medidas = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                Math.max(1, Math.round(contenedor.getResources().getDisplayMetrics().density)));
        int margen = contenedor.getResources().getDimensionPixelSize(R.dimen.relleno_tarjeta);
        medidas.setMarginStart(margen);
        medidas.setMarginEnd(margen);
        contenedor.addView(linea, medidas);
    }
}
