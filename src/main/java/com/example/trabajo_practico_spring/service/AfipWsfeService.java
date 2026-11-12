package com.example.trabajo_practico_spring.service;

import com.example.trabajo_practico_spring.config.AfipProperties;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class AfipWsfeService {

    private static final String URL_WSFE_HOMO = "https://wswhomo.afip.gov.ar/wsfev1/service.asmx";

    private final AfipProperties props;

    public AfipWsfeService(AfipProperties props) {
        this.props = props;
    }

    public record ResultadoCae(String cae, LocalDate fechaVencimiento, long nroComprobante) {

    }

    public ResultadoCae solicitarCae(String token, String sign, double importeTotal, String fechaEmision, Long cuitCliente) throws Exception {
        int cbteTipo = props.getCbteTipo();
        System.out.println(">>> [WSFE] Consultando último comprobante para PtoVta=" + props.getPuntoVenta() + ", CbteTipo=" + cbteTipo);
        long ultimoNro = consultarUltimoNro(token, sign, cbteTipo);
        long nuevoNro = ultimoNro + 1;
        System.out.println(">>> [WSFE] Último número fue: " + ultimoNro + ". El nuevo comprobante será el N°: " + nuevoNro);

        String soapReq = construirSolicitudCae(token, sign, nuevoNro, importeTotal, fechaEmision, cbteTipo);
        String response = enviarSoap(URL_WSFE_HOMO, "FECAESolicitar", soapReq);

        return parsearRespuesta(response, nuevoNro);
    }

    private long consultarUltimoNro(String token, String sign, int cbteTipo) throws Exception {
        String soap = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:ar=\"http://ar.gov.afip.dif.FEV1/\">"
                + "<soap:Body><ar:FECompUltimoAutorizado>"
                + "<ar:Auth>"
                + "<ar:Token>" + token + "</ar:Token>"
                + "<ar:Sign>" + sign + "</ar:Sign>"
                + "<ar:Cuit>" + props.getCuit() + "</ar:Cuit>"
                + "</ar:Auth>"
                + "<ar:PtoVta>" + props.getPuntoVenta() + "</ar:PtoVta>"
                + "<ar:CbteTipo>" + cbteTipo + "</ar:CbteTipo>"
                + "</ar:FECompUltimoAutorizado></soap:Body></soap:Envelope>";

        String resp = enviarSoap(URL_WSFE_HOMO, "FECompUltimoAutorizado", soap);

        if (resp.contains("<Errors>") && resp.contains("<Err>")) {
            String msg = extraerTagSeguro(resp, "Msg");
            String code = extraerTagSeguro(resp, "Code");
            throw new RuntimeException("AFIP WSFE Error (" + code + "): " + msg);
        }

        return extraerLong(resp, "CbteNro");
    }

    private String construirSolicitudCae(String token, String sign, long nro, double importe, String fecha, int cbteTipo) {
        String importeStr = String.format(Locale.US, "%.2f", importe);

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
                + "<ar:CbteTipo>" + cbteTipo + "</ar:CbteTipo>"
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
                + "<ar:CondicionIVAReceptorId>5</ar:CondicionIVAReceptorId>" // <--- RESOLUCIÓN GENERAL 5616
                + "</ar:FECAEDetRequest>"
                + "</ar:FeDetReq>"
                + "</ar:FeCAEReq>"
                + "</ar:FECAESolicitar></soap:Body></soap:Envelope>";
    }

    private ResultadoCae parsearRespuesta(String xml, long nro) {
        if (xml.contains("<Errors>") && xml.contains("<Err>")) {
            String msg = extraerTagSeguro(xml, "Msg");
            String code = extraerTagSeguro(xml, "Code");
            throw new RuntimeException("AFIP rechazó la solicitud (" + code + "): " + msg);
        }

        String resultado = extraerTagSeguro(xml, "Resultado");
        if (!"A".equals(resultado)) {
            String obs = xml.contains("<Obs>") ? extraerTagSeguro(xml, "Msg") : "Comprobante rechazado por AFIP";
            throw new RuntimeException("AFIP rechazó la factura: " + obs);
        }

        String cae = extraerTagSeguro(xml, "CAE");
        String vtoStr = extraerTagSeguro(xml, "CAEFchVto");
        LocalDate vto = LocalDate.parse(vtoStr, DateTimeFormatter.ofPattern("yyyyMMdd"));

        System.out.println(">>> [WSFE] ¡Factura APROBADA! CAE=" + cae + ", Vto=" + vto);
        return new ResultadoCae(cae, vto, nro);
    }

    private String enviarSoap(String urlStr, String soapAction, String soapXml) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        conn.setRequestProperty("SOAPAction", "\"http://ar.gov.afip.dif.FEV1/" + soapAction + "\"");
        conn.setConnectTimeout(20_000);
        conn.setReadTimeout(20_000);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(soapXml.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        InputStream is = (status < 400) ? conn.getInputStream() : conn.getErrorStream();

        if (is == null) {
            throw new RuntimeException("AFIP WSFE respondió con código HTTP " + status + " pero sin cuerpo de respuesta.");
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            String response = sb.toString();

            if (status >= 400) {
                System.err.println(">>> [WSFE ERROR HTTP " + status + "]: " + response);
                throw new RuntimeException("WSFE HTTP " + status + ": " + response);
            }
            return response;
        }
    }

    private String extraerTagSeguro(String xml, String tag) {
        int inicio = xml.indexOf("<" + tag + ">");
        int fin = xml.indexOf("</" + tag + ">");
        if (inicio == -1 || fin == -1) {
            return "Desconocido";
        }
        return xml.substring(inicio + tag.length() + 2, fin).trim();
    }

    private long extraerLong(String xml, String tag) {
        return Long.parseLong(extraerTagSeguro(xml, tag));
    }
}
