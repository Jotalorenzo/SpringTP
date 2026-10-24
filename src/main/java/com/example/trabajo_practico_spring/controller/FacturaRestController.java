package com.example.trabajo_practico_spring.controller;

import com.example.trabajo_practico_spring.dto.ClienteDTO;
import com.example.trabajo_practico_spring.model.Articulo;
import com.example.trabajo_practico_spring.model.PuntoVenta;
import com.example.trabajo_practico_spring.model.TipoMoneda;
import com.example.trabajo_practico_spring.repository.ArticuloRepository;
import com.example.trabajo_practico_spring.repository.PuntoVentaRepository;
import com.example.trabajo_practico_spring.repository.TipoMonedaRepository;
import com.example.trabajo_practico_spring.service.ClienteService;
import com.example.trabajo_practico_spring.dto.FacturaReporteDTO;
import com.example.trabajo_practico_spring.dto.FacturaRequestDTO;
import com.example.trabajo_practico_spring.dto.PaginaDTO;
import com.example.trabajo_practico_spring.service.FacturaPdfService;
import com.example.trabajo_practico_spring.service.FacturaService;
import com.example.trabajo_practico_spring.service.ReporteExcelService;
import com.example.trabajo_practico_spring.service.ReportePdfService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/facturas")
@CrossOrigin(origins = "*")
public class FacturaRestController {

    private final FacturaService facturaService;
    private final ReportePdfService reportePdfService;
    private final ReporteExcelService reporteExcelService;
    private final ClienteService clienteService;
    private final ArticuloRepository articuloRepository;
    private final PuntoVentaRepository puntoVentaRepository;
    private final TipoMonedaRepository tipoMonedaRepository;
    private final FacturaPdfService facturaPdfService;

    @Autowired
    public FacturaRestController(FacturaService facturaService,
            ReportePdfService reportePdfService,
            ReporteExcelService reporteExcelService,
            ClienteService clienteService,
            ArticuloRepository articuloRepository,
            PuntoVentaRepository puntoVentaRepository,
            TipoMonedaRepository tipoMonedaRepository,
            FacturaPdfService facturaPdfService) {
        this.facturaService = facturaService;
        this.reportePdfService = reportePdfService;
        this.reporteExcelService = reporteExcelService;
        this.clienteService = clienteService;
        this.articuloRepository = articuloRepository;
        this.puntoVentaRepository = puntoVentaRepository;
        this.tipoMonedaRepository = tipoMonedaRepository;
        this.facturaPdfService = facturaPdfService;
    }

    // ------------------------------------------------------------------ lectura
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

        PaginaDTO<FacturaReporteDTO> resultado = facturaService.buscarFacturasPaginadas(
                numeroFactura, fechaDesde, fechaHasta, cliente, estado, informadaArca, page, size);

        return ResponseEntity.ok(resultado);
    }

    @GetMapping(value = "/reporte.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> descargarReportePdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Double montoMinimo) throws Exception {

        List<FacturaReporteDTO> facturas
                = facturaService.buscarFacturasFiltradas(fechaDesde, fechaHasta, estado, montoMinimo);
        byte[] pdf = reportePdfService.generarReportePdf(facturas);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte-ventas.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }

    @GetMapping(value = "/reporte.xlsx",
            produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> descargarReporteExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Double montoMinimo) throws Exception {

        List<FacturaReporteDTO> facturas
                = facturaService.buscarFacturasFiltradas(fechaDesde, fechaHasta, estado, montoMinimo);
        byte[] excel = reporteExcelService.generarReporteExcel(facturas);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte-ventas.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(excel.length)
                .body(excel);
    }

    // ----------------------------------------------------------------- escritura
    /**
     * POST /api/facturas Crea una nueva factura con sus detalles. Calcula neto,
     * IVA y total automáticamente. Retorna 201 Created con el resumen de la
     * factura generada.
     */
    @PostMapping
    public ResponseEntity<FacturaReporteDTO> crear(@RequestBody @Valid FacturaRequestDTO request) {
        FacturaReporteDTO creada = facturaService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    /**
     * PUT /api/facturas/{id} Actualiza cabecera e ítems de una factura
     * existente. Si el estado pasa a "ANULADA" se registra la fecha de
     * anulación automáticamente. Retorna 200 OK con el resumen actualizado.
     */
    @PutMapping("/{id}")
    public ResponseEntity<FacturaReporteDTO> actualizar(@PathVariable Long id,
            @RequestBody @Valid FacturaRequestDTO request) {
        FacturaReporteDTO actualizada = facturaService.actualizar(id, request);
        return ResponseEntity.ok(actualizada);
    }

    /**
     * DELETE /api/facturas/{id} Eliminación física de la factura y sus detalles
     * (en cascada). Para anulación lógica usar PUT con estado = "ANULADA".
     * Retorna 204 No Content.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        facturaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/clientes")
    public ResponseEntity<List<ClienteDTO>> getClientes() {
        return ResponseEntity.ok(clienteService.listar());
    }

    @GetMapping("/articulos")
    public ResponseEntity<List<Articulo>> getArticulos() {
        return ResponseEntity.ok(articuloRepository.findAll());
    }

    @GetMapping("/puntos-venta")
    public ResponseEntity<List<PuntoVenta>> getPuntosVenta() {
        return ResponseEntity.ok(puntoVentaRepository.findAll());
    }

    @GetMapping("/tipos-moneda")
    public ResponseEntity<List<TipoMoneda>> getTiposMoneda() {
        return ResponseEntity.ok(tipoMonedaRepository.findAll());
    }

    /**
     * GET /api/facturas/{id} Obtiene el detalle completo de una factura para
     * edición o visualización.
     */
    @GetMapping("/{id}")
    public ResponseEntity<com.example.trabajo_practico_spring.dto.FacturaVentaDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(facturaService.obtener(id));
    }

    /**
     * PUT /api/facturas/{id}/anular Anula lógicamente la factura.
     */
    @PutMapping("/{id}/anular")
    public ResponseEntity<FacturaReporteDTO> anular(@PathVariable Long id) {
        FacturaReporteDTO anulada = facturaService.anular(id);
        return ResponseEntity.ok(anulada);
    }

    /**
     * GET /api/facturas/{id}/pdf Descarga la Factura Individual Legal con CAE y
     * Vencimiento en formato PDF.
     */
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
