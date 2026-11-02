package com.example.trabajo_practico_spring.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propiedades de conexión con ARCA/AFIP leídas desde application.properties.
 * Prefijo: afip
 */
@Component
@ConfigurationProperties(prefix = "afip")
@Getter
@Setter
public class AfipProperties {

    /** CUIT del contribuyente habilitado en homologación. */
    private long cuit;

    /** Ruta absoluta al archivo .crt (certificado digital). */
    private String certPath;

    /** Ruta absoluta al archivo .key (clave privada RSA). */
    private String keyPath;

    /** Punto de venta configurado en AFIP (homologación). */
    private int puntoVenta = 2;
}
