package com.example.trabajo_practico_spring.service;

import com.example.trabajo_practico_spring.config.AfipProperties;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.springframework.stereotype.Service;

import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Collections;

@Service
public class AfipWsaaService {

    private static final String URL_WSAA_HOMO =
            "https://wsaahomo.afip.gov.ar/ws/services/LoginCms";

    private static final DateTimeFormatter AFIP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final AfipProperties props;

    public AfipWsaaService(AfipProperties props) {
        this.props = props;
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        deshabilitarValidacionSslHomologacion();
    }

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

    public String[] obtenerTokenYSign() throws Exception {
        PrivateKey privateKey = cargarClavePrivada(props.getKeyPath());
        X509Certificate cert  = cargarCertificado(props.getCertPath());
        String traXml    = construirTra();
        String cmsBase64 = firmarTra(traXml, privateKey, cert);
        String response  = enviarSoap(URL_WSAA_HOMO, construirSoapEnvelope(cmsBase64));
        return extraerTokenYSign(response);
    }

    private String construirTra() {
        ZonedDateTime now = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        String genTime = now.minusMinutes(10).format(AFIP_FMT);
        String expTime = now.plusMinutes(10).format(AFIP_FMT);
        long uniqueId  = System.currentTimeMillis() / 1000;
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<loginTicketRequest version=\"1.0\">"
                + "<header>"
                + "<destination>cn=wsaahomo,o=afip,c=ar,serialNumber=CUIT 33693450239</destination>"
                + "<uniqueId>" + uniqueId + "</uniqueId>"
                + "<generationTime>" + genTime + "</generationTime>"
                + "<expirationTime>" + expTime + "</expirationTime>"
                + "</header>"
                + "<service>wsfe</service>"
                + "</loginTicketRequest>";
    }

    private String firmarTra(String traXml, PrivateKey key, X509Certificate cert) throws Exception {
        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(key);
        gen.addSignerInfoGenerator(
                new JcaSignerInfoGeneratorBuilder(
                        new JcaDigestCalculatorProviderBuilder().setProvider("BC").build())
                        .build(signer, cert));
        gen.addCertificates(new JcaCertStore(Collections.singletonList(cert)));
        CMSSignedData signedData = gen.generate(
                new CMSProcessableByteArray(traXml.getBytes(StandardCharsets.UTF_8)), true);
        return Base64.getEncoder().encodeToString(signedData.getEncoded());
    }

    private String construirSoapEnvelope(String cmsBase64) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:nso=\"http://wsaa.afip.gov.ar/ws/services/LoginCms\">"
                + "<soapenv:Header/><soapenv:Body>"
                + "<nso:loginCms><nso:in0>" + cmsBase64 + "</nso:in0></nso:loginCms>"
                + "</soapenv:Body></soapenv:Envelope>";
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
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line.trim());
            String response = sb.toString();
            if (status >= 400) throw new RuntimeException("WSAA respondio HTTP " + status + ": " + response);
            return response;
        }
    }

    private String[] extraerTokenYSign(String xmlResponse) {
        String xml = xmlResponse.replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
        if (!xml.contains("<token>")) throw new RuntimeException("Error en respuesta WSAA: " + xmlResponse);
        return new String[]{extraerTag(xml, "token"), extraerTag(xml, "sign")};
    }

    private String extraerTag(String xml, String tag) {
        int inicio = xml.indexOf("<" + tag + ">") + tag.length() + 2;
        int fin    = xml.indexOf("</" + tag + ">");
        if (inicio < tag.length() + 2 || fin < 0)
            throw new RuntimeException("Tag <" + tag + "> no encontrado en respuesta WSAA");
        return xml.substring(inicio, fin);
    }

    private PrivateKey cargarClavePrivada(String path) throws Exception {
        try (FileReader fr = new FileReader(path); PEMParser pemParser = new PEMParser(fr)) {
            Object parsed = pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
            if (parsed instanceof PEMKeyPair) return converter.getKeyPair((PEMKeyPair) parsed).getPrivate();
            else if (parsed instanceof PrivateKeyInfo) return converter.getPrivateKey((PrivateKeyInfo) parsed);
            else if (parsed instanceof PrivateKey) return (PrivateKey) parsed;
            else throw new IllegalArgumentException("Formato de clave no reconocido: " + path);
        }
    }

    private X509Certificate cargarCertificado(String path) throws Exception {
        try (FileInputStream fis = new FileInputStream(path)) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(fis);
        }
    }
}
