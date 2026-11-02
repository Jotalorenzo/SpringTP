package com.example.trabajo_practico_spring.service;

import com.example.trabajo_practico_spring.config.AfipProperties;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Servicio para interactuar con el WSFE v1 de AFIP/ARCA (homologación).
 *
 * Flujo:
 *  1. Consulta el último comprobante autorizado para calcular el próximo número.
 *  2. Envía la solicitud FECAESolicitar.
 *  3. Devuelve el CAE, fecha de vencimiento y número autorizado.
 */
@Service
public class AfipWsfeService {

    private static final String URL_WSFE_HOMO =
            "https://wswhomo.afip.gov.ar/wsfev1/service.asmx";

    /** Tipo de comprobante: 11 = Factura C (sin discriminar IVA). */
    private static final int CBTE_TIPO = 11;

    private final AfipProperties props;

    public AfipWsfeService(AfipProperties props) {
        this.props = props;
    }

    // ============================================================= público

    /**
     * Resultado de la solicitud de CAE.
     */
    public record ResultadoCae(String cae, LocalDate fechaVencimiento, long nroComprobante) {}

    /**
     * Solicita el CAE para una factura.
     *
     * @param token       token obtenido del WSAA
     * @param sign        sign obtenida del WSAA
     * @param importeTotal importe total de la factura
     * @param fechaEmision fecha de emisión en formato YYYYMMDD
     * @return resultado con CAE, fecha de vencimiento y número asignado
     */
    public ResultadoCae solicitarCae(String token, String sign,
                                     double importeTotal, String fechaEmision) throws Exception {

        long ultimoNro = consultarUltimoNro(token, sign);
        long nuevoNro  = ultimoNro + 1;

        String soapReq = construirSolicitudCae(token, sign, nuevoNro, importeTotal, fechaEmision);
        String response = enviarSoap(URL_WSFE_HOMO, "FECAESolicitar", soapReq);

        return parsearRespuesta(response, nuevoNro);
    }

    // ============================================================= privado

    private long consultarUltimoNro(String token, String sign) throws Exception {
        String soap = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:ar=\"http://ar.gov.afip.dif.FEV1/\">"
                + "<soap:Body><ar:FECompUltimoAutorizado>"
                + "<ar:Auth>"
                + "<ar:Token>" + token + "</ar:Token>"
                + "<ar:Sign>" + sign + "</ar:Sign>"
                + "<ar:Cuit>" + props.getCuit() + "</ar:Cuit>"
                + "</ar:Auth>"
                + "<ar:PtoVta>" + props.getPuntoVenta() + "</ar:PtoVta>"
                + "<ar:CbteTipo>" + CBTE_TIPO + "</ar:CbteTipo>"
                + "</ar:FECompUltimoAutorizado></soap:Body></soap:Envelope>";

        String resp = enviarSoap(URL_WSFE_HOMO, "FECompUltimoAutorizado", soap);
        return extraerLong(resp, "CbteNro");
    }

    private String construirSolicitudCae(String token, String sign,
                                          long nro, double importe, String fecha) {
        String importeStr = String.format("%.2f", importe).replace(",", ".");

        return "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:ar=\"http://ar.gov.afip.dif.FEV1/\">"
                + "<soap:Body><ar:FECAESolicitar>"
                + "<ar:Auth>"
                + "<ar:Token>" + token + "</ar:Token>"
                + "<ar:Sign>" + sign + "</ar:Sign>"
                + "<ar:Cuit>" + props.getCuit() + "</ar:Cuit>"
                + "</ar:Auth>"
                + "<ar:FeCAEReq>"
                + "<ar:FeCabReq>"
                + "<ar:CantReg>1</ar:CantReg>"
                + "<ar:PtoVta>" + props.getPuntoVenta() + "</ar:PtoVta>"
                + "<ar:CbteTipo>" + CBTE_TIPO + "</ar:CbteTipo>"
                + "</ar:FeCabReq>"
                + "<ar:FeDetReq>"
                + "<ar:FECAEDetRequest>"
                + "<ar:Concepto>1</ar:Concepto>"
                + "<ar:DocTipo>99</ar:DocTipo>"
                + "<ar:DocNro>0</ar:DocNro>"
                + "<ar:CbteDesde>" + nro + "</ar:CbteDesde>"
                + "<ar:CbteHasta>" + nro + "</ar:CbteHasta>"
                + "<ar:CbteFch>" + fecha + "</ar:CbteFch>"
                + "<ar:ImpTotal>" + importeStr + "</ar:ImpTotal>"
                + "<ar:ImpTotConc>0.00</ar:ImpTotConc>"
                + "<ar:ImpNeto>" + importeStr + "</ar:ImpNeto>"
                + "<ar:ImpOpEx>0.00</ar:ImpOpEx>"
                + "<ar:ImpTrib>0.00</ar:ImpTrib>"
                + "<ar:ImpIVA>0.00</ar:ImpIVA>"
                + "<ar:MonId>PES</ar:MonId>"
                + "<ar:MonCotiz>1</ar:MonCotiz>"
                + "</ar:FECAEDetRequest>"
                + "</ar:FeDetReq>"
                + "</ar:FeCAEReq>"
                + "</ar:FECAESolicitar></soap:Body></soap:Envelope>";
    }

    private ResultadoCae parsearRespuesta(String xml, long nro) {
        String resultado = extraerString(xml, "Resultado");
        if (!"A".equals(resultado)) {
            String obs = contieneTag(xml, "Msg") ? extraerString(xml, "Msg") : "Sin detalle";
            throw new RuntimeException("AFIP rechazó la factura: " + obs);
        }

        String cae    = extraerString(xml, "CAE");
        String vtoStr = extraerString(xml, "CAEFchVto"); // YYYYMMDD
        LocalDate vto = LocalDate.parse(vtoStr, DateTimeFormatter.ofPattern("yyyyMMdd"));

        return new ResultadoCae(cae, vto, nro);
    }

    // ============================================================= SOAP HTTP

    private String enviarSoap(String urlStr, String soapAction, String soapXml) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        conn.setRequestProperty("SOAPAction",
                "\"http://ar.gov.afip.dif.FEV1/" + soapAction + "\"");
        conn.setConnectTimeout(20_000);
        conn.setReadTimeout(20_000);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(soapXml.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        InputStream is = (status < 400) ? conn.getInputStream() : conn.getErrorStream();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            String response = sb.toString();
            if (status >= 400) {
                throw new RuntimeException("WSFE HTTP " + status + ": " + response);
            }
            return response;
        }
    }

    // ============================================================= XML utils

    private String extraerString(String xml, String tag) {
        int inicio = xml.indexOf("<" + tag + ">") + tag.length() + 2;
        int fin    = xml.indexOf("</" + tag + ">");
        if (inicio < tag.length() + 2 || fin < 0)
            throw new RuntimeException("Tag <" + tag + "> no encontrado en respuesta WSFE");
        return xml.substring(inicio, fin).trim();
    }

    private long extraerLong(String xml, String tag) {
        return Long.parseLong(extraerString(xml, tag));
    }

    private boolean contieneTag(String xml, String tag) {
        return xml.contains("<" + tag + ">");
    }
}
