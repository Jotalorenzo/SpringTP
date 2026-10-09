package com.example.trabajo_practico_spring.controller;

import com.example.trabajo_practico_spring.dto.FacturaReporteDTO;
import com.example.trabajo_practico_spring.service.FacturaService;
import com.example.trabajo_practico_spring.service.ReporteExcelService;
import com.example.trabajo_practico_spring.service.ReportePdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/facturas")
public class FacturaRestController {

    private final FacturaService facturaService;
    private final ReportePdfService reportePdfService;
    private final ReporteExcelService reporteExcelService;

    @Autowired
    public FacturaRestController(FacturaService facturaService,
                                 ReportePdfService reportePdfService,
                                 ReporteExcelService reporteExcelService) {
        this.facturaService = facturaService;
        this.reportePdfService = reportePdfService;
        this.reporteExcelService = reporteExcelService;
    }

    @GetMapping
    public List<FacturaReporteDTO> buscarFacturas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Double montoMinimo) {

        return facturaService.buscarFacturasFiltradas(fechaDesde, fechaHasta, estado, montoMinimo);
    }

    @GetMapping(value = "/reporte.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> descargarReportePdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Double montoMinimo) throws Exception {

        List<FacturaReporteDTO> facturas =
                facturaService.buscarFacturasFiltradas(fechaDesde, fechaHasta, estado, montoMinimo);
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

        List<FacturaReporteDTO> facturas =
                facturaService.buscarFacturasFiltradas(fechaDesde, fechaHasta, estado, montoMinimo);
        byte[] excel = reporteExcelService.generarReporteExcel(facturas);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte-ventas.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(excel.length)
                .body(excel);
    }
}
