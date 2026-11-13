package com.example.trabajo_practico_spring.controller;

import com.example.trabajo_practico_spring.dto.*;
import com.example.trabajo_practico_spring.model.*;
import com.example.trabajo_practico_spring.repository.*;
import com.example.trabajo_practico_spring.service.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/facturas")
@CrossOrigin(origins = "*")
public class FacturaRestController {

    private final FacturaService facturaService;
    private final ReportePdfService reportePdfService;
    private final ReporteExcelService reporteExcelService;
    private final PuntoVentaRepository puntoVentaRepository;
    private final TipoMonedaRepository tipoMonedaRepository;
    private final ListaPrecioRepository listaPrecioRepository;
    private final CondicionIvaRepository condicionIvaRepository;
    private final FacturaPdfService facturaPdfService;

    @Autowired
    public FacturaRestController(FacturaService facturaService,
                                 ReportePdfService reportePdfService,
                                 ReporteExcelService reporteExcelService,
                                 PuntoVentaRepository puntoVentaRepository,
                                 TipoMonedaRepository tipoMonedaRepository,
                                 ListaPrecioRepository listaPrecioRepository,
                                 CondicionIvaRepository condicionIvaRepository,
                                 FacturaPdfService facturaPdfService) {
        this.facturaService = facturaService;
        this.reportePdfService = reportePdfService;
        this.reporteExcelService = reporteExcelService;
        this.puntoVentaRepository = puntoVentaRepository;
        this.tipoMonedaRepository = tipoMonedaRepository;
        this.listaPrecioRepository = listaPrecioRepository;
        this.condicionIvaRepository = condicionIvaRepository;
        this.facturaPdfService = facturaPdfService;
    }

    @GetMapping
    public ResponseEntity<PaginaDTO<FacturaReporteDTO>> buscarFacturas(
            @RequestParam(required = false) Long numeroFactura,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) String cliente,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Boolean informadaArca,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {

        return ResponseEntity.ok(facturaService.buscarFacturasPaginadas(
                numeroFactura, fechaDesde, fechaHasta, cliente, estado, informadaArca, page, size));
    }

    // ------------------------------------------------------------- EXPLORER (Máximo 100 registros)
    @GetMapping("/clientes/buscar")
    public ResponseEntity<List<ClienteDTO>> buscarClientesExplorer(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(facturaService.buscarClientesExplorer(q));
    }

    @GetMapping("/articulos/buscar")
    public ResponseEntity<List<ArticuloPrecioDTO>> buscarArticulosExplorer(
            @RequestParam Long listaPrecioId,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(facturaService.buscarArticulosExplorer(listaPrecioId, q));
    }

    @GetMapping("/listas-precio")
    public ResponseEntity<List<ListaPrecio>> getListasPrecio() {
        return ResponseEntity.ok(listaPrecioRepository.findAll());
    }

    @GetMapping("/condiciones-iva")
    public ResponseEntity<List<CondicionIva>> getCondicionesIva() {
        return ResponseEntity.ok(condicionIvaRepository.findAll());
    }

    @GetMapping("/puntos-venta")
    public ResponseEntity<List<PuntoVenta>> getPuntosVenta() {
        return ResponseEntity.ok(puntoVentaRepository.findAll());
    }

    @GetMapping("/tipos-moneda")
    public ResponseEntity<List<TipoMoneda>> getTiposMoneda() {
        return ResponseEntity.ok(tipoMonedaRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FacturaVentaDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(facturaService.obtener(id));
    }

    // ------------------------------------------------------------- ESCRITURA
    @PostMapping
    public ResponseEntity<FacturaReporteDTO> crear(@RequestBody @Valid FacturaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facturaService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FacturaReporteDTO> actualizar(@PathVariable Long id, @RequestBody @Valid FacturaRequestDTO request) {
        return ResponseEntity.ok(facturaService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        facturaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/anular")
    public ResponseEntity<FacturaReporteDTO> anular(@PathVariable Long id) {
        return ResponseEntity.ok(facturaService.anular(id));
    }

    @PutMapping("/{id}/informar-arca")
    public ResponseEntity<?> informarArca(@PathVariable Long id) {
        try {
            FacturaReporteDTO resultado = facturaService.informarArca(id);
            return ResponseEntity.ok(resultado);
        } catch (com.example.trabajo_practico_spring.exception.OperacionNoPermitidaException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(java.util.Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            String detalle = (e.getMessage() != null && !e.getMessage().isBlank()) ? e.getMessage() : e.toString();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Error al comunicarse con ARCA: " + detalle));
        }
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> descargarFacturaIndividualPdf(@PathVariable Long id) throws Exception {
        byte[] pdf = facturaPdfService.generarFacturaPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=factura-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }
}