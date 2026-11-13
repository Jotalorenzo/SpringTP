package com.example.trabajo_practico_spring.service;

import com.example.trabajo_practico_spring.dto.*;
import com.example.trabajo_practico_spring.exception.OperacionNoPermitidaException;
import com.example.trabajo_practico_spring.exception.RecursoNoEncontradoException;
import com.example.trabajo_practico_spring.model.*;
import com.example.trabajo_practico_spring.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

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
    private final ListaPrecioRepository listaPrecioRepository;
    private final ListaPrecioArticuloRepository listaPrecioArticuloRepository;
    private final AuditoriaService auditoria;
    private final AfipWsaaService afipWsaaService;
    private final AfipWsfeService afipWsfeService;

    // ------------------------------------------------------------- EXPLORER (Máximo 100 resultados)
    @Transactional(readOnly = true)
    public List<ClienteDTO> buscarClientesExplorer(String termino) {
        return clienteRepository.buscarPorTermino(termino, PageRequest.of(0, 50))
                .stream().map(c -> ClienteDTO.builder()
                .id(c.getId())
                .cuitCuil(c.getCuitCuil())
                .denominacion(c.getDenominacion())
                .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ArticuloPrecioDTO> buscarArticulosExplorer(Long listaPrecioId, String termino) {
        if (listaPrecioId == null) {
            throw new OperacionNoPermitidaException("Debe seleccionar una Lista de Precios antes de buscar artículos.");
        }
        return listaPrecioArticuloRepository.buscarPorListaYTermino(listaPrecioId, termino, PageRequest.of(0, 50))
                .stream().map(lpa -> ArticuloPrecioDTO.builder()
                .articuloId(lpa.getArticulo().getId())
                .listaPrecioArticuloId(lpa.getId())
                .codigo(lpa.getArticulo().getCodigo())
                .denominacion(lpa.getArticulo().getDenominacion())
                .precioVenta(lpa.getPrecioVenta())
                .build())
                .toList();
    }

    // ------------------------------------------------------------- CREAR (Transaccional)
    @Transactional
    public FacturaReporteDTO crear(FacturaRequestDTO request) {
        validarFechaEmision(request.getPuntoVentaId(), request.getFechaEmision(), null);

        FacturaVenta factura = new FacturaVenta();
        factura.setEstado(ESTADO_INICIAL);
        factura.setLetra(request.getLetra() != null ? request.getLetra().toUpperCase() : "B");
        aplicarDatos(factura, request);
        auditoria.completarAlta(factura);
        return aReporteDto(facturaVentaRepository.save(factura));
    }

    // ------------------------------------------------------------- ACTUALIZAR (Transaccional)
    @Transactional
    public FacturaReporteDTO actualizar(Long id, FacturaRequestDTO request) {
        FacturaVenta factura = buscarFactura(id);

        if (ESTADO_ANULADA.equalsIgnoreCase(factura.getEstado())) {
            throw new OperacionNoPermitidaException("No se puede modificar una factura que ya fue ANULADA.");
        }
        if (factura.getCae() != null && !factura.getCae().isBlank()) {
            throw new OperacionNoPermitidaException("La factura ya fue informada a ARCA (CAE: " + factura.getCae() + "). No permite modificaciones.");
        }

        validarFechaEmision(request.getPuntoVentaId(), request.getFechaEmision(), id);

        factura.setLetra(request.getLetra() != null ? request.getLetra().toUpperCase() : factura.getLetra());
        aplicarDatos(factura, request);
        auditoria.completarModificacion(factura);
        return aReporteDto(facturaVentaRepository.save(factura));
    }

    // ------------------------------------------------------------- VALIDACIÓN DE FECHA
    private void validarFechaEmision(Long puntoVentaId, LocalDate fechaNueva, Long idFacturaActual) {
        Optional<LocalDate> maxFecha = facturaVentaRepository.findMaxFechaEmisionByPuntoVentaId(puntoVentaId);
        if (maxFecha.isPresent() && idFacturaActual == null) { // Al crear
            if (fechaNueva.isBefore(maxFecha.get())) {
                throw new OperacionNoPermitidaException(
                        "La fecha de emisión (" + fechaNueva + ") no puede ser anterior a la última fecha facturada en este punto de venta (" + maxFecha.get() + ")."
                );
            }
        }
    }

    // ------------------------------------------------------------- ANULACIÓN LÓGICA
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

    // ------------------------------------------------------------- ELIMINACIÓN FÍSICA
    @Transactional
    public void eliminar(Long id) {
        FacturaVenta factura = buscarFactura(id);

        boolean tieneNumero = factura.getNumero() != null && factura.getNumero() > 0;
        boolean tieneCae = factura.getCae() != null && !factura.getCae().isBlank();

        if (tieneNumero || tieneCae) {
            throw new OperacionNoPermitidaException(
                    "No se puede eliminar físicamente una factura con número oficial o CAE de ARCA. Debe utilizar 'Anular'."
            );
        }

        facturaVentaRepository.delete(factura);
    }

    // ------------------------------------------------------------- INFORMAR A ARCA
    @Transactional
    public FacturaReporteDTO informarArca(Long id) throws Exception {
        FacturaVenta factura = buscarFactura(id);

        if ("X".equalsIgnoreCase(factura.getLetra())) {
            throw new OperacionNoPermitidaException("Las Factura 'X' son comprobantes internos no fiscales y no pueden ser informadas a ARCA.");
        }
        if (ESTADO_ANULADA.equalsIgnoreCase(factura.getEstado())) {
            throw new OperacionNoPermitidaException("No se puede informar a ARCA una factura ANULADA.");
        }
        if (factura.getCae() != null && !factura.getCae().isBlank()) {
            throw new OperacionNoPermitidaException("La factura ya fue informada a ARCA (CAE: " + factura.getCae() + ").");
        }

        String[] tokenSign = afipWsaaService.obtenerTokenYSign();
        String token = tokenSign[0];
        String sign = tokenSign[1];

        LocalDate fecha = factura.getFechaEmision() != null ? factura.getFechaEmision() : LocalDate.now();
        String fechaEmision = fecha.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        Long cuitCliente = null;
        if (factura.getCliente() != null && factura.getCliente().getCuitCuil() != null) {
            try {
                cuitCliente = Long.parseLong(factura.getCliente().getCuitCuil().replaceAll("[^0-9]", ""));
            } catch (Exception ignored) {
            }
        }

        AfipWsfeService.ResultadoCae resultado = afipWsfeService.solicitarCae(
                token, sign, factura.getImporteTotal(), fechaEmision, cuitCliente);

        factura.setCae(resultado.cae());
        factura.setCaeFechaVencimiento(resultado.fechaVencimiento());
        factura.setResultadoAfip("A");
        factura.setNumero(resultado.nroComprobante());
        factura.setEstado("AUTORIZADA");
        auditoria.completarModificacion(factura);

        return aReporteDto(facturaVentaRepository.save(factura));
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
        factura.setListaPrecio(listaPrecioRepository.findById(request.getListaPrecioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("ListaPrecio", request.getListaPrecioId())));
        factura.setCliente(request.getClienteId() == null ? null
                : clienteRepository.findById(request.getClienteId())
                        .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", request.getClienteId())));

        reemplazarDetalles(factura, request.getDetalles(), request.getListaPrecioId());
    }

    private void reemplazarDetalles(FacturaVenta factura, List<FacturaDetalleRequestDTO> items, Long listaPrecioId) {
        factura.getDetalles().clear();

        double total = 0.0;
        for (FacturaDetalleRequestDTO item : items) {
            ListaPrecioArticulo precio = listaPrecioArticuloRepository
                    .findByListaPrecioIdAndArticuloId(listaPrecioId, item.getArticuloId())
                    .orElseGet(() -> listaPrecioArticuloRepository.findFirstByArticuloIdOrderByIdAsc(item.getArticuloId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Precio del artículo", item.getArticuloId())));

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

    @Transactional(readOnly = true)
    public PaginaDTO<FacturaReporteDTO> buscarFacturasPaginadas(
            Long numeroFactura, LocalDate fechaDesde, LocalDate fechaHasta,
            String cliente, String estado, Boolean informadaArca, int pagina, int tamanio) {
        return facturaRepository.buscarFacturasPaginadas(numeroFactura, fechaDesde, fechaHasta, cliente, estado, informadaArca, pagina, tamanio);
    }

    @Transactional(readOnly = true)
    public FacturaVentaDTO obtener(Long id) {
        return aDetalleDto(buscarFactura(id));
    }

    @Transactional(readOnly = true)
    public List<FacturaReporteDTO> buscarFacturasFiltradas(LocalDate fechaDesde, LocalDate fechaHasta, String estado, Double montoMinimo) {
        return facturaRepository.buscarFacturasFiltradas(fechaDesde, fechaHasta, estado, montoMinimo);
    }

    private FacturaReporteDTO aReporteDto(FacturaVenta f) {
        return new FacturaReporteDTO(
                f.getId(),
                f.getNumero(),
                f.getFechaEmision(),
                f.getCliente() == null ? "Consumidor Final" : f.getCliente().getDenominacion(),
                f.getCondicionIva() == null ? "Consumidor Final" : f.getCondicionIva().getDenominacion(),
                f.getPuntoVenta() == null ? "Punto de Venta" : f.getPuntoVenta().getDescripcion(),
                f.getImporteTotal(),
                f.getDetalles() == null ? 0 : f.getDetalles().size(),
                f.getEstado(),
                f.getCae());
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
