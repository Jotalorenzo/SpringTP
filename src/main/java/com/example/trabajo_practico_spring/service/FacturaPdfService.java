package com.example.trabajo_practico_spring.service;

import com.example.trabajo_practico_spring.exception.RecursoNoEncontradoException;
import com.example.trabajo_practico_spring.model.FacturaVenta;
import com.example.trabajo_practico_spring.model.FacturaVentaDetalle;
import com.example.trabajo_practico_spring.repository.FacturaVentaRepository;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class FacturaPdfService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final FacturaVentaRepository facturaRepository;

    @Transactional(readOnly = true)
    public byte[] generarFacturaPdf(Long id) throws DocumentException {
        FacturaVenta factura = facturaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Factura", id));

        Document document = new Document(PageSize.A4, 25, 25, 25, 25);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        Font fontTitulo = new Font(Font.HELVETICA, 13, Font.BOLD);
        Font fontSubtitulo = new Font(Font.HELVETICA, 9, Font.BOLD);
        Font fontTexto = new Font(Font.HELVETICA, 8, Font.NORMAL);
        Font fontLetra = new Font(Font.HELVETICA, 20, Font.BOLD);
        Font fontLetraSub = new Font(Font.HELVETICA, 7, Font.NORMAL);
        Font fontCae = new Font(Font.HELVETICA, 9, Font.BOLD);

        // Determinación de Letra de Factura (A para Responsables Inscriptos, B para Consumidor Final)
        String letra = "B";
        String codAfip = "COD. 06";
        if (factura.getCliente() != null && factura.getCondicionIva() != null 
                && factura.getCondicionIva().getDenominacion().contains("Responsable")) {
            letra = "A";
            codAfip = "COD. 01";
        }

        // ==========================================
        // 1. CABECERA PRINCIPAL (3 Columnas con Letra al centro)
        // ==========================================
        PdfPTable tablaCabecera = new PdfPTable(new float[]{45f, 10f, 45f});
        tablaCabecera.setWidthPercentage(100);

        // Columna Izquierda: Datos del Emisor
        PdfPCell celdaEmisor = new PdfPCell();
        celdaEmisor.setPadding(8f);
        celdaEmisor.addElement(new Phrase("DISTRIBUIDORA CENTRAL S.A.", fontTitulo));
        celdaEmisor.addElement(new Phrase("Razón Social: Distribuidora Central S.A.", fontTexto));
        celdaEmisor.addElement(new Phrase("Domicilio: " + factura.getPuntoVenta().getDomicilioComercial(), fontTexto));
        celdaEmisor.addElement(new Phrase("Condición IVA: IVA Responsable Inscripto", fontTexto));
        celdaEmisor.setBorder(PdfPCell.BOX);
        tablaCabecera.addCell(celdaEmisor);

        // Columna Centro: Recuadro con la Letra A / B
        PdfPCell celdaLetra = new PdfPCell();
        celdaLetra.setHorizontalAlignment(Element.ALIGN_CENTER);
        celdaLetra.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celdaLetra.setBackgroundColor(new Color(245, 245, 245));
        celdaLetra.addElement(new Phrase("\n " + letra, fontLetra));
        celdaLetra.addElement(new Phrase(codAfip, fontLetraSub));
        celdaLetra.setBorder(PdfPCell.BOX);
        tablaCabecera.addCell(celdaLetra);

        // Columna Derecha: Datos del Comprobante
        PdfPCell celdaFactura = new PdfPCell();
        celdaFactura.setPadding(8f);
        celdaFactura.addElement(new Phrase("FACTURA", fontTitulo));
        String numPuntoVenta = String.format("%04d", factura.getPuntoVenta().getNumero());
        String numFactura = String.format("%08d", factura.getNumero() != null ? factura.getNumero() : 0);
        celdaFactura.addElement(new Phrase("Punto de Venta: " + numPuntoVenta + "  Comp. Nro: " + numFactura, fontSubtitulo));
        celdaFactura.addElement(new Phrase("Fecha de Emisión: " + factura.getFechaEmision().format(FORMATO_FECHA), fontTexto));
        celdaFactura.addElement(new Phrase("CUIT Emisor: 30-71234567-9", fontTexto));
        celdaFactura.addElement(new Phrase("Ingresos Brutos: 901-283849-2", fontTexto));
        celdaFactura.addElement(new Phrase("Inicio de Actividades: 01/01/2010", fontTexto));
        celdaFactura.setBorder(PdfPCell.BOX);
        tablaCabecera.addCell(celdaFactura);

        document.add(tablaCabecera);

        // ==========================================
        // 2. DATOS DEL CLIENTE / RECEPTOR
        // ==========================================
        PdfPTable tablaCliente = new PdfPTable(2);
        tablaCliente.setWidthPercentage(100);
        tablaCliente.setSpacingBefore(8f);

        PdfPCell cCliente1 = new PdfPCell();
        cCliente1.setPadding(6f);
        String clienteNombre = factura.getCliente() != null ? factura.getCliente().getDenominacion() : "Consumidor Final";
        String clienteCuit = factura.getCliente() != null ? factura.getCliente().getCuitCuil() : "00-00000000-0";
        cCliente1.addElement(new Phrase("Receptor / Razón Social: " + clienteNombre, fontSubtitulo));
        cCliente1.addElement(new Phrase("CUIT / DNI: " + clienteCuit, fontTexto));
        cCliente1.setBorder(PdfPCell.BOX);
        tablaCliente.addCell(cCliente1);

        PdfPCell cCliente2 = new PdfPCell();
        cCliente2.setPadding(6f);
        String condIva = factura.getCondicionIva() != null ? factura.getCondicionIva().getDenominacion() : "Consumidor Final";
        cCliente2.addElement(new Phrase("Condición frente al IVA: " + condIva, fontSubtitulo));
        cCliente2.addElement(new Phrase("Condición de Venta: Contado / Saldo: $" + factura.getImporteSaldo(), fontTexto));
        cCliente2.setBorder(PdfPCell.BOX);
        tablaCliente.addCell(cCliente2);

        document.add(tablaCliente);

        // ==========================================
        // 3. TABLA DE ÍTEMS Y DETALLES
        // ==========================================
        PdfPTable tablaDetalles = new PdfPTable(new float[]{15f, 40f, 10f, 15f, 20f});
        tablaDetalles.setWidthPercentage(100);
        tablaDetalles.setSpacingBefore(10f);

        // Encabezados
        String[] cabeceras = {"Código", "Descripción", "Cant.", "Precio Unit.", "Subtotal"};
        for (String cab : cabeceras) {
            PdfPCell c = new PdfPCell(new Phrase(cab, fontSubtitulo));
            c.setBackgroundColor(new Color(230, 230, 230));
            c.setPadding(5f);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            tablaDetalles.addCell(c);
        }

        // Renglones
        for (FacturaVentaDetalle det : factura.getDetalles()) {
            tablaDetalles.addCell(crearCeldaItem(det.getListaPrecioArticulo().getArticulo().getCodigo(), fontTexto, Element.ALIGN_CENTER));
            tablaDetalles.addCell(crearCeldaItem(det.getDescripcion(), fontTexto, Element.ALIGN_LEFT));
            tablaDetalles.addCell(crearCeldaItem(String.valueOf(det.getCantidad()), fontTexto, Element.ALIGN_CENTER));
            tablaDetalles.addCell(crearCeldaItem(String.format(Locale.US, "$ %.2f", det.getPrecioUnitario()), fontTexto, Element.ALIGN_RIGHT));
            tablaDetalles.addCell(crearCeldaItem(String.format(Locale.US, "$ %.2f", det.getImporteSubtotal()), fontTexto, Element.ALIGN_RIGHT));
        }

        document.add(tablaDetalles);

        // ==========================================
        // 4. TOTALES
        // ==========================================
        PdfPTable tablaTotales = new PdfPTable(new float[]{70f, 30f});
        tablaTotales.setWidthPercentage(100);
        tablaTotales.setSpacingBefore(10f);

        PdfPCell cVacia = new PdfPCell();
        cVacia.setBorder(PdfPCell.NO_BORDER);
        tablaTotales.addCell(cVacia);

        PdfPCell cTotal = new PdfPCell();
        cTotal.setPadding(8f);
        cTotal.setBackgroundColor(new Color(245, 245, 245));
        cTotal.setBorder(PdfPCell.BOX);
        cTotal.addElement(new Phrase("TOTAL: " + String.format(Locale.US, "$ %,.2f", factura.getImporteTotal()), fontTitulo));
        tablaTotales.addCell(cTotal);

        document.add(tablaTotales);

        // ==========================================
        // 5. PIE FISCAL (ARCA / AFIP)
        // ==========================================
        PdfPTable tablaPie = new PdfPTable(2);
        tablaPie.setWidthPercentage(100);
        tablaPie.setSpacingBefore(20f);

        PdfPCell cPieIzq = new PdfPCell();
        cPieIzq.setPadding(8f);
        cPieIzq.setBorder(PdfPCell.BOX);

        if (factura.getCae() != null && !factura.getCae().isBlank()) {
            cPieIzq.addElement(new Phrase("CAE N°: " + factura.getCae(), fontCae));
            String vto = factura.getCaeFechaVencimiento() != null 
                    ? factura.getCaeFechaVencimiento().format(FORMATO_FECHA) 
                    : "N/A";
            cPieIzq.addElement(new Phrase("Fecha de Vto. de CAE: " + vto, fontCae));
            cPieIzq.addElement(new Phrase("Comprobante Autorizado por ARCA (ex AFIP)", fontTexto));
        } else {
            cPieIzq.addElement(new Phrase("COMPROBANTE NO FISCAL", fontCae));
            cPieIzq.addElement(new Phrase("Documento de uso interno / No informado a ARCA", fontTexto));
        }
        tablaPie.addCell(cPieIzq);

        PdfPCell cPieDer = new PdfPCell();
        cPieDer.setPadding(8f);
        cPieDer.setBorder(PdfPCell.BOX);
        cPieDer.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cPieDer.addElement(new Phrase("Estado: " + factura.getEstado(), fontSubtitulo));
        if (factura.getFechaAnulacion() != null) {
            cPieDer.addElement(new Phrase("Fecha Anulación: " + factura.getFechaAnulacion().format(FORMATO_FECHA), fontTexto));
        }
        tablaPie.addCell(cPieDer);

        document.add(tablaPie);

        document.close();
        return out.toByteArray();
    }

    private PdfPCell crearCeldaItem(String texto, Font font, int alineacion) {
        PdfPCell cell = new PdfPCell(new Phrase(texto == null ? "" : texto, font));
        cell.setPadding(5f);
        cell.setHorizontalAlignment(alineacion);
        cell.setBorder(PdfPCell.BOTTOM);
        return cell;
    }
}