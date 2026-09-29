package pe.facturass20.ui.inicio;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.Snackbar;

import pe.facturass20.R;
import pe.facturass20.databinding.FragmentInicioBinding;
import pe.facturass20.databinding.ItemFacturaResumenBinding;
import pe.facturass20.dominio.modelo.FacturaCompra;
import pe.facturass20.dominio.modelo.NivelAlerta;
import pe.facturass20.ui.comun.Formatos;

/**
 * P04 Inicio: compras del mes con la barra del límite, ventas, categoría y cuota, vencimiento y las
 * últimas facturas. Los datos vienen de {@link InicioViewModel}; aquí solo se les da formato.
 */
public class InicioFragment extends Fragment {

    private FragmentInicioBinding binding;
    private InicioViewModel modelo;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentInicioBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        modelo = new ViewModelProvider(this).get(InicioViewModel.class);

        binding.tarjetaVentas.setOnClickListener(v -> navegar(R.id.inicio_a_ventas));
        binding.tarjetaCategoria.setOnClickListener(v -> navegar(R.id.inicio_a_categoria));
        binding.botonAvisos.setOnClickListener(v -> navegar(R.id.inicio_a_categoria));
        binding.tarjetaVencimiento.setOnClickListener(v -> navegar(R.id.inicio_a_vencimiento));
        binding.botonVerTodas.setOnClickListener(v -> navegar(R.id.inicio_a_facturas));

        modelo.resumen().observe(getViewLifecycleOwner(), this::mostrar);
        modelo.ocupado().observe(getViewLifecycleOwner(), ocupado ->
                binding.progreso.setVisibility(ocupado && modelo.resumen().getValue() == null ? View.VISIBLE : View.GONE));
        modelo.mensaje().observe(getViewLifecycleOwner(), evento -> {
            String texto = evento.tomar();
            if (texto != null) {
                Snackbar.make(binding.getRoot(), texto, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Al volver de registrar una factura o las ventas, el resumen se actualiza.
        modelo.cargar();
    }

    private void mostrar(ResumenInicio resumen) {
        binding.desplazamiento.setVisibility(View.VISIBLE);
        binding.textoSaludo.setText(getString(R.string.p04_saludo, resumen.primerNombre()));
        binding.textoPeriodo.setText(Formatos.periodo(resumen.periodo()));
        binding.puntoAvisos.setVisibility(resumen.hayAvisos() ? View.VISIBLE : View.GONE);

        mostrarCompras(resumen);
        mostrarVentasYCategoria(resumen);
        mostrarVencimiento(resumen);
        mostrarUltimas(resumen);
    }

    private void mostrarCompras(ResumenInicio resumen) {
        binding.textoCompras.setText(Formatos.moneda(resumen.compras()));
        binding.textoNumeroFacturas.setText(getResources().getQuantityString(R.plurals.p04_facturas,
                resumen.numeroFacturas(), resumen.numeroFacturas()));

        NivelAlerta nivel = resumen.nivel();
        @ColorRes int colorBarra;
        @ColorRes int colorTexto;
        if (nivel == NivelAlerta.NINGUNA) {
            colorBarra = R.color.color_acento;
            colorTexto = R.color.color_acento_oscuro;
        } else if (nivel == NivelAlerta.AVISO_80) {
            colorBarra = R.color.color_alerta;
            colorTexto = R.color.color_alerta_oscuro;
        } else {
            colorBarra = R.color.color_error;
            colorTexto = R.color.color_error;
        }
        binding.barraLimite.setIndicatorColor(ContextCompat.getColor(requireContext(), colorBarra));
        binding.barraLimite.setProgressCompat(resumen.progreso(), false);
        binding.textoLimite.setTextColor(ContextCompat.getColor(requireContext(), colorTexto));

        ConstraintLayout.LayoutParams guia = (ConstraintLayout.LayoutParams) binding.guiaUmbral.getLayoutParams();
        guia.guidePercent = resumen.umbralAviso().floatValue();
        binding.guiaUmbral.setLayoutParams(guia);
        binding.marcaUmbral.setVisibility(resumen.fueraDeRegimen() ? View.GONE : View.VISIBLE);

        String limite = Formatos.monedaCorta(resumen.limite());
        String restante = Formatos.moneda(resumen.restante());
        if (resumen.fueraDeRegimen()) {
            binding.textoLimite.setText(getString(R.string.p04_fuera_limite, limite));
            binding.textoRestante.setText(R.string.p04_fuera_ayuda);
        } else {
            String porcentaje = resumen.porcentaje() + String.valueOf(Formatos.ESPACIO) + "%";
            binding.textoLimite.setText(getString(R.string.p04_porcentaje_limite, porcentaje,
                    resumen.categoria(), limite));
            if (resumen.siguienteCategoria() != null) {
                binding.textoRestante.setText(getString(R.string.p04_restante_categoria, restante,
                        resumen.siguienteCategoria()));
            } else if (resumen.restante().signum() > 0) {
                binding.textoRestante.setText(getString(R.string.p04_restante_nrus, restante));
            } else {
                binding.textoRestante.setText(R.string.p04_en_limite_nrus);
            }
        }
        if (resumen.ventasDeterminan()) {
            binding.textoRestante.append("\n" + getString(R.string.p04_determinan_ventas));
        }
    }

    private void mostrarVentasYCategoria(ResumenInicio resumen) {
        if (resumen.ventas() != null) {
            binding.textoVentas.setText(Formatos.moneda(resumen.ventas()));
            binding.textoVentas.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_texto));
            binding.textoVentasAccion.setText(R.string.p04_editar);
        } else {
            binding.textoVentas.setText(R.string.p04_ventas_sin_registrar);
            binding.textoVentas.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_texto2));
            binding.textoVentasAccion.setText(R.string.p04_registrar);
        }

        if (resumen.fueraDeRegimen()) {
            binding.textoCategoria.setText(R.string.p04_fuera_nrus);
            binding.textoCuota.setText(R.string.p04_sin_cuota);
            pintarTarjetaCategoria(R.color.color_error_claro, R.color.color_error);
        } else {
            binding.textoCategoria.setText(getString(R.string.p04_categoria, resumen.categoria()));
            binding.textoCuota.setText(getString(R.string.p04_cuota, Formatos.monedaCorta(resumen.cuota())));
            pintarTarjetaCategoria(R.color.color_ok_claro, R.color.color_acento_borde);
        }

        binding.textoTopeAnual.setVisibility(resumen.avisoTopeAnual() ? View.VISIBLE : View.GONE);
        binding.textoTopeAnual.setText(getString(R.string.p04_tope_anual, Formatos.monedaCorta(resumen.topeAnual())));
    }

    private void pintarTarjetaCategoria(@ColorRes int fondo, @ColorRes int borde) {
        binding.tarjetaCategoria.setCardBackgroundColor(ContextCompat.getColor(requireContext(), fondo));
        binding.tarjetaCategoria.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), borde)));
    }

    private void mostrarVencimiento(ResumenInicio resumen) {
        binding.textoVencimiento.setText(getString(R.string.p04_declare_hasta, Formatos.fechaLarga(resumen.vencimiento())));
        long dias = resumen.diasParaVencer();
        if (dias > 0) {
            binding.textoDias.setText(getResources().getQuantityString(R.plurals.p04_faltan_dias, (int) dias, (int) dias));
        } else if (dias == 0) {
            binding.textoDias.setText(R.string.p04_vence_hoy);
        } else {
            int pasados = (int) -dias;
            binding.textoDias.setText(getResources().getQuantityString(R.plurals.p04_vencio_hace, pasados, pasados));
        }
    }

    private void mostrarUltimas(ResumenInicio resumen) {
        binding.listaUltimas.removeAllViews();
        boolean vacia = resumen.ultimas().isEmpty();
        binding.vacioUltimas.setVisibility(vacia ? View.VISIBLE : View.GONE);
        binding.botonVerTodas.setVisibility(vacia ? View.GONE : View.VISIBLE);
        LayoutInflater inflater = getLayoutInflater();
        for (FacturaCompra factura : resumen.ultimas()) {
            ItemFacturaResumenBinding fila = ItemFacturaResumenBinding.inflate(inflater, binding.listaUltimas, false);
            String proveedor = factura.emisor().razonSocial();
            fila.textoInicial.setText(proveedor.isEmpty() ? "?"
                    : String.valueOf(Character.toUpperCase(proveedor.charAt(0))));
            fila.textoProveedor.setText(proveedor);
            fila.textoDocumento.setText(getString(R.string.p04_documento, factura.serie(), factura.numero(),
                    Formatos.diaMes(factura.fechaEmision())));
            fila.textoImporte.setText(Formatos.moneda(factura.importeTotal()));
            Long id = factura.id();
            if (id != null) {
                fila.getRoot().setOnClickListener(v -> {
                    Bundle argumentos = new Bundle();
                    argumentos.putLong("idFactura", id);
                    NavHostFragment.findNavController(this).navigate(R.id.inicio_a_detalleFactura, argumentos);
                });
            }
            binding.listaUltimas.addView(fila.getRoot());
        }
    }

    private void navegar(int accion) {
        NavHostFragment.findNavController(this).navigate(accion);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
