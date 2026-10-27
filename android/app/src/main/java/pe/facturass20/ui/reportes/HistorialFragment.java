package pe.facturass20.ui.reportes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;

import pe.facturass20.R;
import pe.facturass20.databinding.FragmentHistorialBinding;
import pe.facturass20.databinding.ItemMesAbiertoBinding;
import pe.facturass20.databinding.ItemMesCerradoBinding;
import pe.facturass20.dominio.modelo.Determinacion;
import pe.facturass20.dominio.modelo.PeriodoMensual;
import pe.facturass20.ui.comun.Filas;
import pe.facturass20.ui.comun.Formatos;
import pe.facturass20.ui.comun.Mensajes;

/**
 * P17 Historial y cierre (prototipo pantalla_17_historial_y_cierre.png): cada mes abierto se puede
 * cerrar (pide confirmación y exige las ventas, RF-17: sin ventas, el botón lleva a P13 de ese mes); al
 * cerrarlo se sugiere exportar los datos (RF-21).
 * Los meses cerrados abren su reporte (P16).
 */
public class HistorialFragment extends Fragment {

    private FragmentHistorialBinding binding;
    private HistorialViewModel modelo;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHistorialBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        modelo = new ViewModelProvider(this).get(HistorialViewModel.class);
        binding.barra.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        Filas.enlace(binding.enlaces, getString(R.string.exportar_titulo), getString(R.string.historial_exportar_detalle),
                () -> NavHostFragment.findNavController(this).navigate(R.id.historial_a_exportarDatos));

        modelo.vista().observe(getViewLifecycleOwner(), this::mostrar);
        modelo.cerrado().observe(getViewLifecycleOwner(), evento -> {
            HistorialViewModel.Cierre cierre = evento.tomar();
            if (cierre == null) {
                return;
            }
            YearMonth mes = cierre.mes();
            MaterialAlertDialogBuilder dialogo = new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.historial_cerrado_titulo, Formatos.periodo(mes)))
                    .setMessage(cierre.conPdf()
                            ? getString(R.string.cierre_pdf_listo) + " " + getString(R.string.cierre_sugerir_exportar)
                            : getString(R.string.cierre_sugerir_exportar))
                    .setPositiveButton(R.string.historial_exportar_ahora, (d, w) ->
                            NavHostFragment.findNavController(this).navigate(R.id.historial_a_exportarDatos))
                    .setNegativeButton(R.string.historial_despues, null);
            if (cierre.conPdf()) {
                dialogo.setNeutralButton(R.string.cierre_ver_reporte, (d, w) -> verReporte(mes));
            }
            dialogo.show();
        });
        modelo.ocupado().observe(getViewLifecycleOwner(), ocupado ->
                binding.progreso.setVisibility(ocupado && modelo.vista().getValue() == null ? View.VISIBLE : View.GONE));
        Mensajes.observar(this, modelo, binding.getRoot());
    }

    @Override
    public void onResume() {
        super.onResume();
        modelo.cargar();
    }

    private void mostrar(HistorialViewModel.Vista vista) {
        binding.desplazamiento.setVisibility(View.VISIBLE);
        binding.lista.removeAllViews();
        for (HistorialViewModel.Mes mes : vista.meses()) {
            if (mes.periodo().estaAbierto()) {
                abierto(mes);
            } else {
                cerrado(mes);
            }
        }

        binding.textoAnio.setText(getString(R.string.historial_acumulado, vista.anio()));
        binding.textoComprasAnio.setText(getString(R.string.historial_compras_anio,
                Formatos.moneda(vista.comprasAnio()), Formatos.monedaCorta(vista.topeAnual())));
        binding.textoVentasAnio.setText(getString(R.string.historial_ventas_anio, Formatos.moneda(vista.ventasAnio())));
        BigDecimal mayor = vista.comprasAnio().max(vista.ventasAnio());
        int progreso = mayor.multiply(BigDecimal.valueOf(100)).divide(vista.topeAnual(), 0, RoundingMode.DOWN).intValue();
        binding.barraAnio.setProgreso(progreso);
        binding.barraAnio.setColorIndicador(ContextCompat.getColor(requireContext(),
                progreso >= 80 ? R.color.color_alerta : R.color.color_acento));
    }

    private void abierto(HistorialViewModel.Mes mes) {
        PeriodoMensual periodo = mes.periodo();
        ItemMesAbiertoBinding tarjeta = ItemMesAbiertoBinding.inflate(getLayoutInflater(), binding.lista, true);
        tarjeta.textoMes.setText(Formatos.periodo(periodo.periodo()));
        String ventas = periodo.ventasRegistradas() ? Formatos.moneda(periodo.totalVentas())
                : getString(R.string.p04_ventas_sin_registrar);
        tarjeta.textoMontos.setText(getString(R.string.historial_montos, Formatos.moneda(periodo.totalAdquisiciones()), ventas));
        tarjeta.textoCategoria.setText(categoria(mes.determinacion(), true));
        if (periodo.ventasRegistradas()) {
            tarjeta.botonCerrar.setOnClickListener(v -> confirmarCierre(periodo));
        } else {
            tarjeta.botonCerrar.setText(R.string.historial_registrar_ventas);
            tarjeta.botonCerrar.setOnClickListener(v -> {
                Bundle argumentos = new Bundle();
                argumentos.putString("periodo", periodo.periodo().toString());
                NavHostFragment.findNavController(this).navigate(R.id.historial_a_ventas, argumentos);
            });
        }
        tarjeta.botonFacturas.setOnClickListener(v -> {
            Bundle argumentos = new Bundle();
            argumentos.putString("periodo", periodo.periodo().toString());
            NavHostFragment.findNavController(this).navigate(R.id.historial_a_facturas, argumentos);
        });
    }

    private void cerrado(HistorialViewModel.Mes mes) {
        PeriodoMensual periodo = mes.periodo();
        ItemMesCerradoBinding fila = ItemMesCerradoBinding.inflate(getLayoutInflater(), binding.lista, true);
        fila.textoMes.setText(Formatos.periodo(periodo.periodo()));
        fila.textoCompras.setText(getString(R.string.historial_compras, Formatos.moneda(periodo.totalAdquisiciones())));
        fila.textoCategoria.setText(categoria(mes.determinacion(), false));
        fila.getRoot().setOnClickListener(v -> verReporte(periodo.periodo()));
    }

    /** P16 del mes; si está cerrado, comparte el PDF generado al cerrarlo. */
    private void verReporte(YearMonth mes) {
        Bundle argumentos = new Bundle();
        argumentos.putString("periodo", mes.toString());
        NavHostFragment.findNavController(this).navigate(R.id.historial_a_reporte, argumentos);
    }

    private String categoria(@Nullable Determinacion determinacion, boolean larga) {
        if (determinacion == null) {
            return "";
        }
        if (determinacion.fueraDeRegimen()) {
            return getString(R.string.p04_fuera_nrus);
        }
        return getString(larga ? R.string.historial_categoria_larga : R.string.historial_categoria_corta,
                determinacion.categoria().codigo(), Formatos.monedaCorta(determinacion.cuota()));
    }

    private void confirmarCierre(PeriodoMensual periodo) {
        String mes = Formatos.periodo(periodo.periodo());
        boolean enCurso = periodo.periodo().equals(modelo.mesEnCurso());
        String mensaje = getString(R.string.historial_cerrar_mensaje, mes);
        if (enCurso) {
            mensaje = getString(R.string.historial_cerrar_en_curso) + " " + mensaje;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.historial_cerrar_titulo, mes))
                .setMessage(mensaje)
                .setPositiveButton(R.string.historial_cerrar, (d, w) -> modelo.cerrar(periodo.periodo()))
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
