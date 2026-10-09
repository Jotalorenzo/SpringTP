package com.example.trabajo_practico_spring.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.example.trabajo_practico_spring.dto.FacturaReporteDTO;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class ReportePdfService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] generarReportePdf(List<FacturaReporteDTO> facturas) throws DocumentException {
        Document document = new Document(PageSize.A4.rotate(), 36, 36, 48, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        Font fuenteTitulo = new Font(Font.HELVETICA, 16, Font.BOLD);
        Font fuenteSubtitulo = new Font(Font.HELVETICA, 10, Font.NORMAL);
        Font fuenteEncabezado = new Font(Font.HELVETICA, 9, Font.BOLD);
        Font fuenteCelda = new Font(Font.HELVETICA, 9, Font.NORMAL);

        Paragraph titulo = new Paragraph("Reporte Ejecutivo de Ventas", fuenteTitulo);
        titulo.setAlignment(Element.ALIGN_CENTER);
        titulo.setSpacingAfter(6f);
        document.add(titulo);

        Paragraph generado = new Paragraph(
                "Generado el " + LocalDate.now().format(FORMATO_FECHA)
                        + " - " + facturas.size() + " factura(s)", fuenteSubtitulo);
        generado.setAlignment(Element.ALIGN_CENTER);
        generado.setSpacingAfter(16f);
        document.add(generado);

        if (facturas.isEmpty()) {
            document.add(new Paragraph("No hay facturas para los filtros indicados.", fuenteSubtitulo));
            document.close();
            return out.toByteArray();
        }

        PdfPTable tabla = new PdfPTable(7);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{12f, 12f, 26f, 22f, 18f, 12f, 8f});

        agregarEncabezado(tabla, fuenteEncabezado, "Nro", "Fecha", "Cliente",
                "Condicion IVA", "Punto de Venta", "Importe Total", "Items");

        for (FacturaReporteDTO f : facturas) {
            tabla.addCell(celda(f.getNumeroFactura().toString(), fuenteCelda));
            tabla.addCell(celda(f.getFechaEmision().format(FORMATO_FECHA), fuenteCelda));
            tabla.addCell(celda(f.getClienteDenominacion(), fuenteCelda));
            tabla.addCell(celda(f.getCondicionIva(), fuenteCelda));
            tabla.addCell(celda(f.getPuntoVentaDescripcion(), fuenteCelda));
            tabla.addCell(celda(String.format(Locale.US, "%,.2f", f.getImporteTotal()), fuenteCelda, Element.ALIGN_RIGHT));
            tabla.addCell(celda(String.valueOf(f.getCantidadItems()), fuenteCelda, Element.ALIGN_CENTER));
        }

        tabla.setSpacingBefore(4f);
        document.add(tabla);

        document.add(new Paragraph(" "));
        Paragraph total = new Paragraph("Total: " + facturas.size() + " factura(s)", fuenteEncabezado);
        total.setAlignment(Element.ALIGN_RIGHT);
        document.add(total);

        document.close();
        return out.toByteArray();
    }

    private void agregarEncabezado(PdfPTable tabla, Font fuente, String... titulos) {
        for (String titulo : titulos) {
            PdfPCell cell = new PdfPCell(new Phrase(titulo, fuente));
            cell.setBackgroundColor(new java.awt.Color(230, 230, 230));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(6f);
            tabla.addCell(cell);
        }
    }

    private PdfPCell celda(String texto, Font fuente) {
        return celda(texto, fuente, Element.ALIGN_LEFT);
    }

    private PdfPCell celda(String texto, Font fuente, int alineacion) {
        PdfPCell cell = new PdfPCell(new Phrase(texto == null ? "" : texto, fuente));
        cell.setHorizontalAlignment(alineacion);
        cell.setPadding(5f);
        return cell;
    }
}
