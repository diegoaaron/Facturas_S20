package pe.facturass20.datos.repositorios;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import pe.facturass20.datos.dao.DeterminacionConCategoria;
import pe.facturass20.datos.dao.FacturaConEmisor;
import pe.facturass20.datos.entidades.CampoExtraidoEntity;
import pe.facturass20.datos.entidades.CategoriaNrusEntity;
import pe.facturass20.datos.entidades.ContribuyenteEntity;
import pe.facturass20.datos.entidades.CronogramaVencEntity;
import pe.facturass20.datos.entidades.DeterminacionEntity;
import pe.facturass20.datos.entidades.EmisorEntity;
import pe.facturass20.datos.entidades.FacturaCompraEntity;
import pe.facturass20.datos.entidades.ImagenFacturaEntity;
import pe.facturass20.datos.entidades.ParametroVersionEntity;
import pe.facturass20.dominio.modelo.CampoExtraido;
import pe.facturass20.dominio.modelo.CategoriaNRUS;
import pe.facturass20.dominio.modelo.Contribuyente;
import pe.facturass20.dominio.modelo.CronogramaVencimiento;
import pe.facturass20.dominio.modelo.Determinacion;
import pe.facturass20.dominio.modelo.Emisor;
import pe.facturass20.dominio.modelo.FacturaCompra;
import pe.facturass20.dominio.modelo.ImagenFactura;
import pe.facturass20.dominio.modelo.ParametrosNrus;

/** Conversión entre las entidades Room y los objetos del dominio. */
final class Mapeos {

    private Mapeos() { }

    static Contribuyente aDominio(ContribuyenteEntity e) {
        return new Contribuyente(e.ruc, e.nombre, e.titular, e.fechaAlta);
    }

    /** Copia los datos del contribuyente sobre la fila; no toca el PIN. */
    static void copiar(Contribuyente c, ContribuyenteEntity e) {
        e.ruc = c.ruc();
        e.nombre = c.nombre();
        e.titular = c.titular();
        e.ultimoDigito = c.ultimoDigitoRuc();
        e.fechaAlta = c.fechaAlta();
    }

    static Emisor aDominio(EmisorEntity e) {
        return new Emisor(e.ruc, e.razonSocial);
    }

    static FacturaCompra aDominio(FacturaConEmisor fila) {
        FacturaCompraEntity f = fila.factura;
        return new FacturaCompra(f.idFactura, aDominio(fila.emisor), f.serie, f.numero, f.fechaEmision, f.moneda,
                f.importeTotal, f.origen, f.estado, f.motivoAnulacion);
    }

    static List<FacturaCompra> facturas(List<FacturaConEmisor> filas) {
        List<FacturaCompra> facturas = new ArrayList<>(filas.size());
        for (FacturaConEmisor fila : filas) {
            facturas.add(aDominio(fila));
        }
        return facturas;
    }

    static FacturaCompraEntity aEntidad(FacturaCompra f, long idPeriodo, long idEmisor, LocalDateTime registro) {
        FacturaCompraEntity e = new FacturaCompraEntity();
        e.idPeriodo = idPeriodo;
        e.idEmisor = idEmisor;
        e.serie = f.serie();
        e.numero = f.numero();
        e.numeroNormalizado = FacturaCompra.numeroNormalizado(f.numero());
        e.fechaEmision = f.fechaEmision();
        e.moneda = f.moneda();
        e.importeTotal = f.importeTotal();
        e.origen = f.origen();
        e.estado = f.estado();
        e.motivoAnulacion = f.motivoAnulacion();
        e.fechaRegistro = registro;
        return e;
    }

    static ImagenFacturaEntity aEntidad(ImagenFactura imagen, long idFactura) {
        ImagenFacturaEntity e = new ImagenFacturaEntity();
        e.idFactura = idFactura;
        e.rutaCifrada = imagen.ruta();
        e.nitidez = imagen.nitidez();
        e.fechaCaptura = imagen.fechaCaptura();
        return e;
    }

    static CampoExtraidoEntity aEntidad(CampoExtraido campo, long idFactura, Long idModelo) {
        CampoExtraidoEntity e = new CampoExtraidoEntity();
        e.idFactura = idFactura;
        e.idModelo = idModelo;
        e.nombreCampo = campo.nombre();
        e.valorLeido = campo.valorLeido();
        e.valorFinal = campo.valorFinal();
        e.confianza = campo.confianza();
        e.corregido = campo.corregido();
        return e;
    }

    static CategoriaNRUS aDominio(CategoriaNrusEntity e) {
        return new CategoriaNRUS(e.codigo, e.limiteMensual, e.cuota);
    }

    static Determinacion aDominio(DeterminacionConCategoria fila, YearMonth periodo) {
        DeterminacionEntity d = fila.determinacion;
        return new Determinacion(periodo, d.totalAdquisiciones, d.montoDeterminante,
                fila.categoria == null ? null : aDominio(fila.categoria), d.cuota, d.fechaVencimiento,
                d.nivelAlerta, d.avisoTopeAnual);
    }

    static DeterminacionEntity aEntidad(Determinacion d, long idPeriodo, Long idCategoria, LocalDateTime calculo) {
        DeterminacionEntity e = new DeterminacionEntity();
        e.idPeriodo = idPeriodo;
        e.idCategoria = idCategoria;
        e.totalAdquisiciones = d.totalAdquisiciones();
        e.montoDeterminante = d.montoDeterminante();
        e.cuota = d.cuota();
        e.fechaVencimiento = d.fechaVencimiento();
        e.nivelAlerta = d.nivelAlerta();
        e.avisoTopeAnual = d.avisoTopeAnual();
        e.fechaCalculo = calculo;
        return e;
    }

    static ParametrosNrus aDominio(ParametroVersionEntity v, List<CategoriaNrusEntity> categorias,
            List<CronogramaVencEntity> cronograma) {
        List<CategoriaNRUS> cats = new ArrayList<>(categorias.size());
        for (CategoriaNrusEntity c : categorias) {
            cats.add(aDominio(c));
        }
        List<CronogramaVencimiento> filas = new ArrayList<>(cronograma.size());
        for (CronogramaVencEntity c : cronograma) {
            filas.add(new CronogramaVencimiento(c.anio, c.mes, c.ultimoDigito, c.fechaLimite));
        }
        return new ParametrosNrus(v.version, v.vigenteDesde, v.umbralAviso, v.topeAnual, cats, filas);
    }
}
