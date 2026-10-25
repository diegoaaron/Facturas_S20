package pe.facturass20.ui.reportes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import java.util.List;

import pe.facturass20.R;
import pe.facturass20.databinding.FragmentReporteBinding;
import pe.facturass20.databinding.ItemDatoBinding;
import pe.facturass20.dominio.modelo.Determinacion;
import pe.facturass20.dominio.modelo.FacturaCompra;
import pe.facturass20.ui.comun.Formatos;
import pe.facturass20.ui.comun.Mensajes;

/**
 * P16 Reporte (prototipo pantalla_16_reporte_mensual.png): vista previa de la primera página del PDF con
 * los datos reales del mes, el resumen y las acciones. Avisa si faltan las ventas del mes.
 */
public class ReporteFragment extends Fragment {

    /** Filas de la tabla que caben en la vista previa. */
    private static final int FILAS_VISTA_PREVIA = 8;

    private FragmentReporteBinding binding;
    private ReporteViewModel modelo;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentReporteBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        modelo = new ViewModelProvider(this).get(ReporteViewModel.class);
        binding.barra.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        binding.botonCompartir.setOnClickListener(v -> modelo.generarPdf());
        binding.botonGuardar.setOnClickListener(v -> modelo.generarPdf());
        modelo.vista().observe(getViewLifecycleOwner(), this::mostrar);
        Mensajes.observar(this, modelo, binding.getRoot());
        if (modelo.vista().getValue() == null) {
            modelo.cargar(getArguments() == null ? "" : getArguments().getString("periodo", ""));
        }
    }

    private void mostrar(ReporteViewModel.Vista vista) {
        Determinacion determinacion = vista.determinacion();
        binding.desplazamiento.setVisibility(View.VISIBLE);
        binding.barra.setSubtitle(Formatos.periodo(vista.periodo().periodo()));
        binding.avisoVentas.setVisibility(vista.periodo().ventasRegistradas() ? View.GONE : View.VISIBLE);

        binding.textoNegocio.setText(getString(R.string.reporte_negocio, vista.contribuyente().nombre(),
                vista.contribuyente().ruc()));
        String categoria = determinacion.fueraDeRegimen() ? getString(R.string.p04_fuera_nrus)
                : getString(R.string.reporte_categoria_corta, determinacion.categoria().codigo());
        binding.textoResumen1.setText(getString(R.string.reporte_resumen_1,
                Formatos.moneda(determinacion.totalAdquisiciones()), categoria));
        binding.textoResumen2.setText(getString(R.string.reporte_resumen_2,
                determinacion.cuota() == null ? getString(R.string.p04_sin_cuota) : Formatos.monedaCorta(determinacion.cuota()),
                Formatos.fecha(determinacion.fechaVencimiento())));

        binding.tabla.removeAllViews();
        List<FacturaCompra> facturas = vista.facturas();
        for (int i = 0; i < Math.min(FILAS_VISTA_PREVIA, facturas.size()); i++) {
            FacturaCompra factura = facturas.get(i);
            ItemDatoBinding fila = ItemDatoBinding.inflate(getLayoutInflater(), binding.tabla, true);
            fila.getRoot().setMinimumHeight(0);
            fila.textoEtiqueta.setTextAppearance(R.style.Texto_FacturasS20_Ayuda);
            fila.textoValor.setTextAppearance(R.style.Texto_FacturasS20_Ayuda);
            fila.textoEtiqueta.setText(getString(R.string.reporte_fila, Formatos.diaMes(factura.fechaEmision()),
                    factura.emisor().razonSocial()));
            fila.textoValor.setText(Formatos.moneda(factura.importeTotal()));
        }
        if (facturas.isEmpty()) {
            ItemDatoBinding fila = ItemDatoBinding.inflate(getLayoutInflater(), binding.tabla, true);
            fila.textoEtiqueta.setTextAppearance(R.style.Texto_FacturasS20_Ayuda);
            fila.textoEtiqueta.setText(R.string.p04_sin_facturas);
        } else if (facturas.size() > FILAS_VISTA_PREVIA) {
            ItemDatoBinding fila = ItemDatoBinding.inflate(getLayoutInflater(), binding.tabla, true);
            fila.textoEtiqueta.setTextAppearance(R.style.Texto_FacturasS20_Ayuda);
            int resto = facturas.size() - FILAS_VISTA_PREVIA;
            fila.textoEtiqueta.setText(getResources().getQuantityString(R.plurals.reporte_mas, resto, resto));
        }

        binding.textoArchivo.setText(getResources().getQuantityString(R.plurals.reporte_archivo, vista.paginas(),
                vista.nombreArchivo(), vista.paginas()));

        binding.resumen.removeAllViews();
        dato(R.string.reporte_incluidas, getResources().getQuantityString(R.plurals.reporte_facturas_con_imagen,
                facturas.size(), facturas.size(), vista.conImagen()));
        dato(R.string.reporte_ventas, vista.periodo().ventasRegistradas()
                ? Formatos.moneda(vista.periodo().totalVentas()) : getString(R.string.p04_ventas_sin_registrar));
        dato(R.string.reporte_determinante, Formatos.moneda(determinacion.montoDeterminante()));
        dato(R.string.reporte_categoria_cuota, determinacion.fueraDeRegimen() ? getString(R.string.p04_fuera_nrus)
                : getString(R.string.reporte_categoria_y_cuota, determinacion.categoria().codigo(),
                Formatos.moneda(determinacion.cuota())));
    }

    private void dato(int etiqueta, String valor) {
        ItemDatoBinding fila = ItemDatoBinding.inflate(getLayoutInflater(), binding.resumen, true);
        fila.textoEtiqueta.setText(etiqueta);
        fila.textoValor.setText(valor);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
