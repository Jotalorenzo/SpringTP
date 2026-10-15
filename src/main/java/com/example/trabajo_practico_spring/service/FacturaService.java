package com.example.trabajo_practico_spring.service;

import com.example.trabajo_practico_spring.dto.FacturaDetalleRequestDTO;
import com.example.trabajo_practico_spring.dto.FacturaReporteDTO;
import com.example.trabajo_practico_spring.dto.FacturaRequestDTO;
import com.example.trabajo_practico_spring.dto.FacturaVentaDTO;
import com.example.trabajo_practico_spring.dto.FacturaVentaDetalleDTO;
import com.example.trabajo_practico_spring.exception.RecursoNoEncontradoException;
import com.example.trabajo_practico_spring.model.FacturaVenta;
import com.example.trabajo_practico_spring.model.FacturaVentaDetalle;
import com.example.trabajo_practico_spring.model.ListaPrecioArticulo;
import com.example.trabajo_practico_spring.repository.ClienteRepository;
import com.example.trabajo_practico_spring.repository.CondicionIvaRepository;
import com.example.trabajo_practico_spring.repository.FacturaRepository;
import com.example.trabajo_practico_spring.repository.FacturaVentaRepository;
import com.example.trabajo_practico_spring.repository.ListaPrecioArticuloRepository;
import com.example.trabajo_practico_spring.repository.PuntoVentaRepository;
import com.example.trabajo_practico_spring.repository.TipoMonedaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FacturaService {

    private static final double ALICUOTA_IVA = 0.21;
    private static final String ESTADO_INICIAL = "EMITIDA";
    private static final String ESTADO_ANULADA = "ANULADA";

    private final FacturaRepository facturaRepository;
    private final FacturaVentaRepository facturaVentaRepository;
    private final PuntoVentaRepository puntoVentaRepository;
    private final CondicionIvaRepository condicionIvaRepository;
    private final TipoMonedaRepository tipoMonedaRepository;
    private final ClienteRepository clienteRepository;
    private final ListaPrecioArticuloRepository listaPrecioArticuloRepository;
    private final AuditoriaService auditoria;

    // ------------------------------------------------------------------ lectura

    @Transactional(readOnly = true)
    public List<FacturaReporteDTO> buscarFacturasFiltradas(LocalDate fechaDesde,
                                                           LocalDate fechaHasta,
                                                           String estado,
                                                           Double montoMinimo) {
        return facturaRepository.buscarFacturasFiltradas(fechaDesde, fechaHasta, estado, montoMinimo);
    }

    @Transactional(readOnly = true)
    public FacturaVentaDTO obtener(Long id) {
        return aDetalleDto(buscarFactura(id));
    }

    // ---------------------------------------------------------------- escritura

    /**
     * Crea la factura con sus detalles (persistidos en cascada) y calcula neto, IVA y total.
     */
    @Transactional
    public FacturaReporteDTO crear(FacturaRequestDTO request) {
        FacturaVenta factura = new FacturaVenta();
        factura.setEstado(ESTADO_INICIAL);
        aplicarDatos(factura, request);
        auditoria.completarAlta(factura);
        return aReporteDto(facturaVentaRepository.save(factura));
    }

    /**
     * Actualiza cabecera e items. Los detalles anteriores se reemplazan (orphanRemoval).
     * Si el estado pasa a ANULADA se registra la fecha de anulacion.
     */
    @Transactional
    public FacturaReporteDTO actualizar(Long id, FacturaRequestDTO request) {
        FacturaVenta factura = buscarFactura(id);
        aplicarDatos(factura, request);
        auditoria.completarModificacion(factura);
        return aReporteDto(facturaVentaRepository.save(factura));
    }

    /**
     * Eliminacion fisica: borra la factura y, en cascada, sus detalles.
     * Para una anulacion logica usar actualizar() con estado = "ANULADA".
     */
    @Transactional
    public void eliminar(Long id) {
        facturaVentaRepository.delete(buscarFactura(id));
    }

    // ------------------------------------------------------------------ helpers

    private FacturaVenta buscarFactura(Long id) {
        return facturaVentaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Factura", id));
    }

    private void aplicarDatos(FacturaVenta factura, FacturaRequestDTO request) {
        factura.setNumero(request.getNumero());
        factura.setFechaEmision(request.getFechaEmision());
        factura.setObservaciones(request.getObservaciones());

        factura.setPuntoVenta(puntoVentaRepository.findById(request.getPuntoVentaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("PuntoVenta", request.getPuntoVentaId())));
        factura.setCondicionIva(condicionIvaRepository.findById(request.getCondicionIvaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("CondicionIva", request.getCondicionIvaId())));
        factura.setTipoMoneda(tipoMonedaRepository.findById(request.getTipoMonedaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("TipoMoneda", request.getTipoMonedaId())));
        factura.setCliente(request.getClienteId() == null ? null
                : clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", request.getClienteId())));

        if (request.getEstado() != null && !request.getEstado().isBlank()) {
            factura.setEstado(request.getEstado().trim().toUpperCase());
        }
        factura.setFechaAnulacion(ESTADO_ANULADA.equals(factura.getEstado())
                ? (factura.getFechaAnulacion() != null ? factura.getFechaAnulacion() : LocalDate.now())
                : null);

        reemplazarDetalles(factura, request.getDetalles());
    }

    private void reemplazarDetalles(FacturaVenta factura, List<FacturaDetalleRequestDTO> items) {
        factura.getDetalles().clear();

        double total = 0.0;
        for (FacturaDetalleRequestDTO item : items) {
            ListaPrecioArticulo precio = listaPrecioArticuloRepository
                    .findFirstByArticuloIdOrderByIdAsc(item.getArticuloId())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Precio del articulo", item.getArticuloId()));

            double precioUnitario = precio.getPrecioVenta();
            double neto = redondear(item.getCantidad() * precioUnitario
                    * (1 - item.getPorcentajeBonificacion() / 100.0));
            double iva = redondear(neto * ALICUOTA_IVA);
            double subtotal = redondear(neto + iva);

            FacturaVentaDetalle detalle = new FacturaVentaDetalle();
            detalle.setListaPrecioArticulo(precio);
            detalle.setDescripcion(item.getDescripcion() != null && !item.getDescripcion().isBlank()
                    ? item.getDescripcion()
                    : precio.getArticulo().getDenominacion());
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(precioUnitario);
            detalle.setPorcentajeBonificacion(item.getPorcentajeBonificacion());
            detalle.setImporteNeto(neto);
            detalle.setImporteIva(iva);
            detalle.setImporteSubtotal(subtotal);
            factura.agregarDetalle(detalle);

            total += subtotal;
        }

        factura.setImporteTotal(redondear(total));
        factura.setImporteSaldo(redondear(factura.getImporteTotal() - factura.getImporteCobrado()));
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    private FacturaReporteDTO aReporteDto(FacturaVenta f) {
        return new FacturaReporteDTO(
                f.getNumero(),
                f.getFechaEmision(),
                f.getCliente() == null ? "Consumidor Final" : f.getCliente().getDenominacion(),
                f.getCondicionIva().getDenominacion(),
                f.getPuntoVenta().getDescripcion(),
                f.getImporteTotal(),
                f.getDetalles().size());
    }

    private FacturaVentaDTO aDetalleDto(FacturaVenta f) {
        List<FacturaVentaDetalleDTO> detalles = f.getDetalles().stream()
                .map(d -> FacturaVentaDetalleDTO.builder()
                        .id(d.getId())
                        .listaPrecioArticuloId(d.getListaPrecioArticulo().getId())
                        .articuloId(d.getListaPrecioArticulo().getArticulo().getId())
                        .articuloDenominacion(d.getListaPrecioArticulo().getArticulo().getDenominacion())
                        .descripcion(d.getDescripcion())
                        .cantidad(d.getCantidad())
                        .precioUnitario(d.getPrecioUnitario())
                        .porcentajeBonificacion(d.getPorcentajeBonificacion())
                        .importeNeto(d.getImporteNeto())
                        .importeIva(d.getImporteIva())
                        .importeSubtotal(d.getImporteSubtotal())
                        .build())
                .toList();

        return FacturaVentaDTO.builder()
                .id(f.getId())
                .numero(f.getNumero())
                .fechaEmision(f.getFechaEmision())
                .puntoVentaId(f.getPuntoVenta().getId())
                .puntoVentaDescripcion(f.getPuntoVenta().getDescripcion())
                .clienteId(f.getCliente() == null ? null : f.getCliente().getId())
                .clienteDenominacion(f.getCliente() == null ? null : f.getCliente().getDenominacion())
                .tipoMonedaId(f.getTipoMoneda() == null ? null : f.getTipoMoneda().getId())
                .condicionIvaId(f.getCondicionIva() == null ? null : f.getCondicionIva().getId())
                .importeCobrado(f.getImporteCobrado())
                .importeSaldo(f.getImporteSaldo())
                .importeTotal(f.getImporteTotal())
                .cae(f.getCae())
                .caeFechaVencimiento(f.getCaeFechaVencimiento())
                .resultadoAfip(f.getResultadoAfip())
                .motivoRechazo(f.getMotivoRechazo())
                .estado(f.getEstado())
                .fechaAnulacion(f.getFechaAnulacion())
                .observaciones(f.getObservaciones())
                .detalles(detalles)
                .build();
    }
}
