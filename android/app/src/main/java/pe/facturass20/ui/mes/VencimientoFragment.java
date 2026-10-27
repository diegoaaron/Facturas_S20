package pe.facturass20.ui.mes;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import java.time.LocalDate;
import java.time.YearMonth;

import pe.facturass20.R;
import pe.facturass20.databinding.FragmentVencimientoBinding;
import pe.facturass20.ui.comun.Filas;
import pe.facturass20.ui.comun.Formatos;
import pe.facturass20.ui.comun.Mensajes;
import pe.facturass20.ui.comun.PermisoNotificaciones;
import pe.facturass20.ui.inicio.ResumenInicio;

/**
 * P15 Vencimiento (prototipo pantalla_15_vencimiento_y_recordatorio.png, RF-13 y RF-14): la fecha límite
 * del cronograma de la SUNAT para el mes en curso, el calendario de ese mes, el último dígito del RUC y
 * los recordatorios 5 y 1 días antes.
 */
public class VencimientoFragment extends Fragment {

    private static final String[] DIAS = {"L", "M", "M", "J", "V", "S", "D"};

    private FragmentVencimientoBinding binding;
    private CategoriaViewModel modelo;
    private final PermisoNotificaciones permiso = new PermisoNotificaciones(this);

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentVencimientoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        modelo = new ViewModelProvider(this).get(CategoriaViewModel.class);
        binding.barra.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        binding.botonReporte.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.vencimiento_a_reporte));
        recordatorio(R.string.vencimiento_5_dias, PreferenciasAvisos.Aviso.VENCE_EN_5_DIAS);
        Filas.separador(binding.recordatorios);
        recordatorio(R.string.vencimiento_1_dia, PreferenciasAvisos.Aviso.VENCE_EN_1_DIA);

        modelo.vista().observe(getViewLifecycleOwner(), vista -> mostrar(vista.resumen(), vista.ultimoDigitoRuc()));
        Mensajes.observar(this, modelo, binding.getRoot());
        modelo.cargar();
    }

    private void recordatorio(int titulo, PreferenciasAvisos.Aviso aviso) {
        Filas.interruptor(binding.recordatorios, getString(titulo), null, modelo.avisoActivado(aviso),
                activado -> {
                    modelo.activarAviso(aviso, activado);
                    if (activado) {
                        permiso.pedirAlActivar();
                    }
                });
    }

    private void mostrar(ResumenInicio resumen, int digito) {
        binding.desplazamiento.setVisibility(View.VISIBLE);
        LocalDate vence = resumen.vencimiento();
        String fecha = Formatos.fechaLarga(vence);
        binding.textoFecha.setText(Character.toUpperCase(fecha.charAt(0)) + fecha.substring(1));
        long dias = resumen.diasParaVencer();
        if (dias > 0) {
            binding.textoDias.setText(getResources().getQuantityString(R.plurals.p04_faltan_dias, (int) dias, (int) dias));
        } else if (dias == 0) {
            binding.textoDias.setText(R.string.p04_vence_hoy);
        } else {
            binding.textoDias.setText(getResources().getQuantityString(R.plurals.p04_vencio_hace, (int) -dias, (int) -dias));
        }
        binding.textoDigito.setText(getString(R.string.vencimiento_digito, Formatos.periodo(resumen.periodo()), digito));
        calendario(vence);
    }

    /** Mes de la fecha límite, de lunes a domingo, con el día del vencimiento resaltado. */
    private void calendario(LocalDate vence) {
        YearMonth mes = YearMonth.from(vence);
        binding.textoMesCalendario.setText(Formatos.periodo(mes));
        GridLayout cuadricula = binding.calendario;
        cuadricula.removeAllViews();
        for (String dia : DIAS) {
            TextView texto = celda(cuadricula, dia);
            texto.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_texto2));
            texto.setTypeface(null, Typeface.BOLD);
            texto.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
        int vacios = mes.atDay(1).getDayOfWeek().getValue() - 1;
        for (int i = 0; i < vacios; i++) {
            celda(cuadricula, "");
        }
        for (int dia = 1; dia <= mes.lengthOfMonth(); dia++) {
            TextView texto = celda(cuadricula, String.valueOf(dia));
            if (dia == vence.getDayOfMonth()) {
                texto.setBackgroundResource(R.drawable.fondo_dia_vencimiento);
                texto.setTypeface(null, Typeface.BOLD);
                texto.setContentDescription(getString(R.string.vencimiento_dia_limite, dia));
            }
        }
    }

    /** Agrega una celda al calendario: el texto va en un círculo de tamaño fijo centrado en la columna. */
    private TextView celda(GridLayout cuadricula, String contenido) {
        int lado = Math.round(38 * getResources().getDisplayMetrics().density);
        TextView texto = new TextView(requireContext());
        texto.setText(contenido);
        texto.setGravity(Gravity.CENTER);
        texto.setTextAppearance(R.style.Texto_FacturasS20_Dato);
        FrameLayout marco = new FrameLayout(requireContext());
        marco.addView(texto, new FrameLayout.LayoutParams(lado, lado, Gravity.CENTER));
        cuadricula.addView(marco, medidas());
        return texto;
    }

    private GridLayout.LayoutParams medidas() {
        int lado = Math.round(40 * getResources().getDisplayMetrics().density);
        GridLayout.LayoutParams medidas = new GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED),
                GridLayout.spec(GridLayout.UNDEFINED, 1f));
        medidas.width = 0;
        medidas.height = lado;
        medidas.setMargins(4, 4, 4, 4);
        return medidas;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
