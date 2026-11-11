package com.example.trabajo_practico_spring.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "afip")
@Getter
@Setter
public class AfipProperties {

    /** CUIT del contribuyente habilitado en homologación. */
    private long cuit;

    /** Ruta al archivo .crt (certificado digital). */
    private String certPath;

    /** Ruta al archivo .key (clave privada RSA). */
    private String keyPath;

    /** Punto de venta configurado en AFIP (homologación). */
    private int puntoVenta = 2;

    /** Tipo de comprobante: 11 = Factura C, 1 = Factura A, 6 = Factura B */
    private int cbteTipo = 11;
}