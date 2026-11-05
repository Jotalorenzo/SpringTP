package com.example.trabajo_practico_spring.service;

import com.example.trabajo_practico_spring.config.AfipProperties;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.springframework.stereotype.Service;

import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collections;

/**
 * Servicio de autenticación contra el WSAA de AFIP/ARCA (homologación).
 *
 * Genera el TRA (Ticket de Requerimiento de Acceso), lo firma con la clave
 * privada del contribuyente usando CMS/PKCS#7, y obtiene el Token + Sign
 * necesarios para invocar el WSFE.
 */
@Service
public class AfipWsaaService {

    private static final String URL_WSAA_HOMO =
            "https://wsaahomo.afip.gov.ar/ws/services/LoginCms";

    private final AfipProperties props;

    public AfipWsaaService(AfipProperties props) {
        this.props = props;
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        deshabilitarValidacionSslHomologacion();
    }

    /** Solo para homologación: deshabilita validación SSL para aceptar el cert de AFIP. */
    private void deshabilitarValidacionSslHomologacion() {
        try {
            TrustManager[] trustAll = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(java.security.cert.X509Certificate[] c, String a) {}
                    public void checkServerTrusted(java.security.cert.X509Certificate[] c, String a) {}
                }
            };
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAll, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception ignored) {}
    }

    /**
     * Obtiene un Ticket de Acceso válido del WSAA.
     *
     * @return arreglo de dos elementos: [0] = token, [1] = sign
     * @throws Exception si el certificado o la clave no se pueden leer,
     *                   o si el WSAA devuelve un error SOAP.
     */
    public String[] obtenerTokenYSign() throws Exception {
        PrivateKey privateKey = cargarClavePrivada(props.getKeyPath());
        X509Certificate cert    = cargarCertificado(props.getCertPath());

        String traXml = construirTra();
        String cmsBase64 = firmarTra(traXml, privateKey, cert);
        String soapResponse = enviarSoap(URL_WSAA_HOMO, construirSoapEnvelope(cmsBase64));

        return extraerTokenYSign(soapResponse);
    }

    // ------------------------------------------------------------------ TRA

    private String construirTra() {
        ZonedDateTime now = ZonedDateTime.now();
        String genTime = now.minusMinutes(5).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String expTime = now.plusMinutes(10).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        long uniqueId  = System.currentTimeMillis() / 1000;

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<loginTicketRequest version=\"1.0\">"
                + "<header>"
                + "<uniqueId>" + uniqueId + "</uniqueId>"
                + "<generationTime>" + genTime + "</generationTime>"
                + "<expirationTime>" + expTime + "</expirationTime>"
                + "</header>"
                + "<service>wsfe</service>"
                + "</loginTicketRequest>";
    }

    // ---------------------------------------------------------- firma CMS/P7

    private String firmarTra(String traXml, PrivateKey key, X509Certificate cert) throws Exception {
        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider("BC")
                .build(key);

        gen.addSignerInfoGenerator(
                new JcaSignerInfoGeneratorBuilder(
                        new JcaDigestCalculatorProviderBuilder().setProvider("BC").build())
                        .build(signer, cert));

        gen.addCertificates(new JcaCertStore(Collections.singletonList(cert)));

        CMSSignedData signedData = gen.generate(
                new CMSProcessableByteArray(traXml.getBytes(StandardCharsets.UTF_8)), true);

        return Base64.getEncoder().encodeToString(signedData.getEncoded());
    }

    // ------------------------------------------------------------ SOAP call

    private String construirSoapEnvelope(String cmsBase64) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<soapenv:Envelope "
                + "xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:nso=\"http://wsaa.afip.gov.ar/ws/services/LoginCms\">"
                + "<soapenv:Header/>"
                + "<soapenv:Body>"
                + "<nso:loginCms><nso:in0>" + cmsBase64 + "</nso:in0></nso:loginCms>"
                + "</soapenv:Body>"
                + "</soapenv:Envelope>";
    }

    private String enviarSoap(String urlStr, String soapXml) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        conn.setRequestProperty("SOAPAction", "");
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(15_000);
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
                throw new RuntimeException("WSAA respondió HTTP " + status + ": " + response);
            }
            return response;
        }
    }

    // ------------------------------------------------------- parse respuesta

    private String[] extraerTokenYSign(String xmlResponse) {
        String xml = xmlResponse
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&");

        String token = extraerTag(xml, "token");
        String sign  = extraerTag(xml, "sign");
        return new String[]{token, sign};
    }

    private String extraerTag(String xml, String tag) {
        int inicio = xml.indexOf("<" + tag + ">") + tag.length() + 2;
        int fin    = xml.indexOf("</" + tag + ">");
        if (inicio < tag.length() + 2 || fin < 0) {
            throw new RuntimeException("No se encontró la etiqueta <" + tag + "> en la respuesta WSAA");
        }
        return xml.substring(inicio, fin);
    }

    // -------------------------------------------------- carga de credenciales

    private PrivateKey cargarClavePrivada(String path) throws Exception {
        String content = new String(Files.readAllBytes(new File(path).toPath()));
        content = content
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(content);
        return KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    private X509Certificate cargarCertificado(String path) throws Exception {
        try (FileInputStream fis = new FileInputStream(path)) {
            return (X509Certificate) CertificateFactory
                    .getInstance("X.509")
                    .generateCertificate(fis);
        }
    }
}
