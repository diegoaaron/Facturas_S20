package pe.facturass20.ui.facturas;

import android.graphics.Paint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.chip.Chip;

import java.time.YearMonth;
import java.util.List;

import pe.facturass20.R;
import pe.facturass20.databinding.FragmentListaFacturasBinding;
import pe.facturass20.databinding.ItemFacturaListaBinding;
import pe.facturass20.dominio.modelo.FacturaCompra;
import pe.facturass20.dominio.modelo.OrigenRegistro;
import pe.facturass20.dominio.modelo.PeriodoMensual;
import pe.facturass20.ui.comun.Formatos;
import pe.facturass20.ui.comun.Mensajes;

/**
 * P11 Facturas del mes (prototipo pantalla_11_facturas_del_mes.png): filtros Todas / Leídas por IA /
 * Manuales / Anuladas, búsqueda por proveedor, RUC o número, lista por semana y el total del mes. Tocar
 * una factura abre P12.
 */
public class ListaFacturasFragment extends Fragment {

    private FragmentListaFacturasBinding binding;
    private ListaFacturasViewModel modelo;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentListaFacturasBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        modelo = new ViewModelProvider(this).get(ListaFacturasViewModel.class);

        binding.barra.setOnMenuItemClickListener(item -> {
            boolean mostrar = binding.campoBusqueda.getVisibility() != View.VISIBLE;
            binding.campoBusqueda.setVisibility(mostrar ? View.VISIBLE : View.GONE);
            if (mostrar) {
                binding.campoBusqueda.requestFocus();
            } else if (binding.campoBusqueda.getEditText() != null) {
                binding.campoBusqueda.getEditText().setText("");
            }
            return true;
        });
        if (!modelo.busqueda().isEmpty()) {
            binding.campoBusqueda.setVisibility(View.VISIBLE);
        }
        binding.campoBusqueda.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int inicio, int cantidad, int despues) { }

            @Override
            public void onTextChanged(CharSequence s, int inicio, int antes, int cantidad) { }

            @Override
            public void afterTextChanged(Editable s) {
                modelo.buscar(s.toString());
                mostrar(modelo.periodo().getValue());
            }
        });

        binding.filtros.check(idDe(modelo.filtro()));
        binding.filtros.setOnCheckedStateChangeListener((grupo, elegidos) -> {
            if (!elegidos.isEmpty()) {
                modelo.filtrar(filtroDe(elegidos.get(0)));
                mostrar(modelo.periodo().getValue());
            }
        });

        modelo.periodo().observe(getViewLifecycleOwner(), this::mostrar);
        modelo.ocupado().observe(getViewLifecycleOwner(), ocupado ->
                binding.progreso.setVisibility(ocupado && modelo.periodo().getValue() == null ? View.VISIBLE : View.GONE));
        Mensajes.observar(this, modelo, binding.getRoot());
    }

    @Override
    public void onResume() {
        super.onResume();
        modelo.cargar(argumentoPeriodo());
    }

    private String argumentoPeriodo() {
        return getArguments() == null ? "" : getArguments().getString("periodo", "");
    }

    private void mostrar(@Nullable PeriodoMensual periodo) {
        if (periodo == null || binding == null) {
            return;
        }
        YearMonth mes = periodo.periodo();
        binding.barra.setTitle(getString(R.string.facturas_titulo, Formatos.mes(mes)));
        binding.barra.setSubtitle(argumentoPeriodo().isEmpty() ? null : String.valueOf(mes.getYear()));
        rotular(binding.filtroTodas, R.string.facturas_filtro_todas, periodo, ListadoFacturas.Filtro.TODAS);
        rotular(binding.filtroIa, R.string.facturas_filtro_ia, periodo, ListadoFacturas.Filtro.IA);
        rotular(binding.filtroManuales, R.string.facturas_filtro_manuales, periodo, ListadoFacturas.Filtro.MANUALES);
        rotular(binding.filtroAnuladas, R.string.facturas_filtro_anuladas, periodo, ListadoFacturas.Filtro.ANULADAS);

        int vigentes = ListadoFacturas.contar(periodo, ListadoFacturas.Filtro.TODAS);
        binding.textoTotalEtiqueta.setText(getResources().getQuantityString(R.plurals.facturas_total_mes, vigentes, vigentes));
        binding.textoTotal.setText(Formatos.moneda(ListadoFacturas.total(periodo)));

        List<ListadoFacturas.Semana> semanas = ListadoFacturas.agrupar(periodo, modelo.filtro(), modelo.busqueda());
        binding.lista.removeAllViews();
        binding.vacio.setVisibility(semanas.isEmpty() ? View.VISIBLE : View.GONE);
        if (semanas.isEmpty()) {
            boolean filtrando = modelo.filtro() != ListadoFacturas.Filtro.TODAS || !modelo.busqueda().trim().isEmpty();
            binding.textoVacio.setText(filtrando ? R.string.facturas_sin_resultados : R.string.p04_sin_facturas);
            binding.textoVacioAyuda.setVisibility(filtrando ? View.GONE : View.VISIBLE);
        }
        LayoutInflater inflater = getLayoutInflater();
        for (ListadoFacturas.Semana semana : semanas) {
            TextView encabezado = (TextView) inflater.inflate(R.layout.item_encabezado_semana, binding.lista, false);
            encabezado.setText(getString(R.string.facturas_semana, semana.desde(), semana.hasta()));
            binding.lista.addView(encabezado);
            for (FacturaCompra factura : semana.facturas()) {
                binding.lista.addView(fila(inflater, factura));
            }
        }
    }

    private View fila(LayoutInflater inflater, FacturaCompra factura) {
        ItemFacturaListaBinding fila = ItemFacturaListaBinding.inflate(inflater, binding.lista, false);
        String proveedor = factura.emisor().razonSocial();
        fila.textoInicial.setText(proveedor.isEmpty() ? "?" : String.valueOf(Character.toUpperCase(proveedor.charAt(0))));
        fila.textoProveedor.setText(proveedor);
        fila.textoDocumento.setText(getString(R.string.facturas_documento, factura.serie(), factura.numero(),
                Formatos.fecha(factura.fechaEmision())));
        fila.textoImporte.setText(Formatos.moneda(factura.importeTotal()));
        if (!factura.esVigente()) {
            fila.textoImporte.setPaintFlags(fila.textoImporte.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            fila.textoOrigen.setText(R.string.facturas_etiqueta_anulada);
            fila.textoOrigen.setBackgroundResource(R.drawable.fondo_etiqueta_error);
            fila.textoOrigen.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_error));
        } else if (factura.origen() == OrigenRegistro.MANUAL) {
            fila.textoOrigen.setText(R.string.facturas_etiqueta_manual);
            fila.textoOrigen.setBackgroundResource(R.drawable.fondo_etiqueta_neutra);
            fila.textoOrigen.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_texto2));
        } else {
            fila.textoOrigen.setText(R.string.facturas_etiqueta_ia);
        }
        Long id = factura.id();
        if (id != null) {
            fila.getRoot().setOnClickListener(v -> {
                Bundle argumentos = new Bundle();
                argumentos.putLong("idFactura", id);
                NavHostFragment.findNavController(this).navigate(R.id.facturas_a_detalleFactura, argumentos);
            });
        }
        return fila.getRoot();
    }

    private void rotular(Chip chip, int texto, PeriodoMensual periodo, ListadoFacturas.Filtro filtro) {
        String nombre = getString(texto);
        chip.setText(chip.isChecked() ? getString(R.string.facturas_filtro_con_numero, nombre,
                ListadoFacturas.contar(periodo, filtro)) : nombre);
    }

    private static int idDe(ListadoFacturas.Filtro filtro) {
        switch (filtro) {
            case IA:
                return R.id.filtro_ia;
            case MANUALES:
                return R.id.filtro_manuales;
            case ANULADAS:
                return R.id.filtro_anuladas;
            default:
                return R.id.filtro_todas;
        }
    }

    private static ListadoFacturas.Filtro filtroDe(int id) {
        if (id == R.id.filtro_ia) {
            return ListadoFacturas.Filtro.IA;
        } else if (id == R.id.filtro_manuales) {
            return ListadoFacturas.Filtro.MANUALES;
        } else if (id == R.id.filtro_anuladas) {
            return ListadoFacturas.Filtro.ANULADAS;
        }
        return ListadoFacturas.Filtro.TODAS;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
