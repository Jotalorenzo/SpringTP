package com.example.trabajo_practico_spring.service;

import com.example.trabajo_practico_spring.dto.FacturaReporteDTO;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReporteExcelService {

    /** Filas que SXSSF mantiene en RAM; las anteriores se vuelcan a un archivo temporal en disco. */
    private static final int FILAS_EN_MEMORIA = 100;

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] HEADERS = {
            "Numero Factura", "Fecha Emision", "Cliente", "Condicion IVA",
            "Punto de Venta", "Importe Total", "Cantidad Items"
    };

    public byte[] generarReporteExcel(List<FacturaReporteDTO> facturas) throws IOException {
        SXSSFWorkbook workbook = new SXSSFWorkbook(FILAS_EN_MEMORIA);
        workbook.setCompressTempFiles(true);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Reporte de Ventas");

            CellStyle estiloTitulo = workbook.createCellStyle();
            Font fuenteTitulo = workbook.createFont();
            fuenteTitulo.setBold(true);
            fuenteTitulo.setFontHeightInPoints((short) 14);
            estiloTitulo.setFont(fuenteTitulo);
            estiloTitulo.setAlignment(HorizontalAlignment.CENTER);

            Row filaTitulo = sheet.createRow(0);
            Cell celdaTitulo = filaTitulo.createCell(0);
            celdaTitulo.setCellValue("Reporte Ejecutivo de Ventas");
            celdaTitulo.setCellStyle(estiloTitulo);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, HEADERS.length - 1));
            filaTitulo.setHeightInPoints(22f);

            CellStyle estiloEncabezado = workbook.createCellStyle();
            Font fuenteEncabezado = workbook.createFont();
            fuenteEncabezado.setBold(true);
            fuenteEncabezado.setColor(IndexedColors.WHITE.getIndex());
            estiloEncabezado.setFont(fuenteEncabezado);
            estiloEncabezado.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
            estiloEncabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            estiloEncabezado.setAlignment(HorizontalAlignment.CENTER);
            estiloEncabezado.setBorderBottom(BorderStyle.THIN);

            Row filaEncabezado = sheet.createRow(2);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = filaEncabezado.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(estiloEncabezado);
            }

            CellStyle estiloTexto = workbook.createCellStyle();
            CellStyle estiloNumero = workbook.createCellStyle();
            estiloNumero.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("#,##0.00"));
            CellStyle estiloCantidad = workbook.createCellStyle();
            estiloCantidad.setAlignment(HorizontalAlignment.CENTER);

            int filaActual = 3;
            for (FacturaReporteDTO f : facturas) {
                Row row = sheet.createRow(filaActual++);

                Cell cNumero = row.createCell(0);
                cNumero.setCellValue(f.getNumeroFactura());
                cNumero.setCellStyle(estiloTexto);

                Cell cFecha = row.createCell(1);
                cFecha.setCellValue(f.getFechaEmision().format(FORMATO_FECHA));
                cFecha.setCellStyle(estiloTexto);

                Cell cCliente = row.createCell(2);
                cCliente.setCellValue(valorONulo(f.getClienteDenominacion()));
                cCliente.setCellStyle(estiloTexto);

                Cell cCondicion = row.createCell(3);
                cCondicion.setCellValue(valorONulo(f.getCondicionIva()));
                cCondicion.setCellStyle(estiloTexto);

                Cell cPuntoVenta = row.createCell(4);
                cPuntoVenta.setCellValue(valorONulo(f.getPuntoVentaDescripcion()));
                cPuntoVenta.setCellStyle(estiloTexto);

                Cell cImporte = row.createCell(5);
                cImporte.setCellValue(f.getImporteTotal());
                cImporte.setCellStyle(estiloNumero);

                Cell cItems = row.createCell(6);
                cItems.setCellValue(f.getCantidadItems());
                cItems.setCellStyle(estiloCantidad);
            }

            sheet.setColumnWidth(0, 16 * 256);
            sheet.setColumnWidth(1, 15 * 256);
            sheet.setColumnWidth(2, 32 * 256);
            sheet.setColumnWidth(3, 26 * 256);
            sheet.setColumnWidth(4, 24 * 256);
            sheet.setColumnWidth(5, 18 * 256);
            sheet.setColumnWidth(6, 16 * 256);
            sheet.createFreezePane(0, 3);

            workbook.write(out);
            return out.toByteArray();
        } finally {
            workbook.close();
            workbook.dispose(); // elimina los archivos temporales
        }
    }

    private String valorONulo(String valor) {
        return valor == null ? "" : valor;
    }
}
