package com.example.trabajo_practico_spring.service;

import com.example.trabajo_practico_spring.dto.FacturaDetalleRequestDTO;
import com.example.trabajo_practico_spring.dto.FacturaReporteDTO;
import com.example.trabajo_practico_spring.dto.FacturaRequestDTO;
import com.example.trabajo_practico_spring.dto.FacturaVentaDTO;
import com.example.trabajo_practico_spring.dto.FacturaVentaDetalleDTO;
import com.example.trabajo_practico_spring.dto.PaginaDTO;
import com.example.trabajo_practico_spring.exception.OperacionNoPermitidaException;
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

    @Transactional(readOnly = true)
    public List<FacturaReporteDTO> buscarFacturasFiltradas(LocalDate fechaDesde, LocalDate fechaHasta, String estado, Double montoMinimo) {
        return facturaRepository.buscarFacturasFiltradas(fechaDesde, fechaHasta, estado, montoMinimo);
    }

    @Transactional(readOnly = true)
    public FacturaVentaDTO obtener(Long id) {
        return aDetalleDto(buscarFactura(id));
    }

    @Transactional
    public FacturaReporteDTO crear(FacturaRequestDTO request) {
        FacturaVenta factura = new FacturaVenta();
        factura.setEstado(ESTADO_INICIAL);
        aplicarDatos(factura, request);
        auditoria.completarAlta(factura);
        return aReporteDto(facturaVentaRepository.save(factura));
    }

    /**
     * REGLA: Si la factura ya fue informada a ARCA (tiene CAE) o ya está
     * anulada, se bloquea cualquier modificación de sus datos o ítems.
     */
    @Transactional
    public FacturaReporteDTO actualizar(Long id, FacturaRequestDTO request) {
        FacturaVenta factura = buscarFactura(id);

        if (ESTADO_ANULADA.equalsIgnoreCase(factura.getEstado())) {
            throw new OperacionNoPermitidaException("No se puede modificar una factura que ya fue ANULADA.");
        }

        boolean informadaArca = factura.getCae() != null && !factura.getCae().isBlank();
        if (informadaArca) {
            throw new OperacionNoPermitidaException("La factura ya fue informada a ARCA (CAE: " + factura.getCae() + "). Está bloqueada para modificaciones; solo puede ser Anulada.");
        }

        aplicarDatos(factura, request);
        auditoria.completarModificacion(factura);
        return aReporteDto(facturaVentaRepository.save(factura));
    }

    /**
     * REGLA: Anulación lógica. Registra fecha y estado ANULADA.
     */
    @Transactional
    public FacturaReporteDTO anular(Long id) {
        FacturaVenta factura = buscarFactura(id);

        if (ESTADO_ANULADA.equalsIgnoreCase(factura.getEstado())) {
            throw new OperacionNoPermitidaException("La factura ya se encuentra anulada.");
        }

        factura.setEstado(ESTADO_ANULADA);
        factura.setFechaAnulacion(LocalDate.now());
        factura.setImporteSaldo(0.0);
        auditoria.completarModificacion(factura);
        return aReporteDto(facturaVentaRepository.save(factura));
    }

    @Transactional(readOnly = true)
    public PaginaDTO<FacturaReporteDTO> buscarFacturasPaginadas(
            Long numeroFactura, LocalDate fechaDesde, LocalDate fechaHasta,
            String cliente, String estado, Boolean informadaArca, int pagina, int tamanio) {
        return facturaRepository.buscarFacturasPaginadas(numeroFactura, fechaDesde, fechaHasta, cliente, estado, informadaArca, pagina, tamanio);
    }

    /**
     * REGLA: Eliminación física. Si la factura posee NÚMERO o CAE, se prohíbe
     * el borrado físico (debe anularse).
     */
    @Transactional
    public void eliminar(Long id) {
        FacturaVenta factura = buscarFactura(id);

        boolean tieneNumero = factura.getNumero() != null && factura.getNumero() > 0;
        boolean tieneCae = factura.getCae() != null && !factura.getCae().isBlank();

        if (tieneNumero || tieneCae) {
            throw new OperacionNoPermitidaException(
                    "No se puede eliminar físicamente una factura con número oficial o informada a ARCA. Debe utilizar la opción 'Anular'."
            );
        }

        facturaVentaRepository.delete(factura);
    }

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

        reemplazarDetalles(factura, request.getDetalles());
    }

    private void reemplazarDetalles(FacturaVenta factura, List<FacturaDetalleRequestDTO> items) {
        factura.getDetalles().clear();

        double total = 0.0;
        for (FacturaDetalleRequestDTO item : items) {
            ListaPrecioArticulo precio = listaPrecioArticuloRepository
                    .findFirstByArticuloIdOrderByIdAsc(item.getArticuloId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Precio del articulo", item.getArticuloId()));

            double precioUnitario = precio.getPrecioVenta();
            double neto = redondear(item.getCantidad() * precioUnitario * (1 - (item.getPorcentajeBonificacion() / 100.0)));
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
