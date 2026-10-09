package com.example.trabajo_practico_spring.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Proyeccion DTO del reporte ejecutivo de ventas.
 *
 * El constructor de todos argumentos DEBE coincidir con el orden de los
 * argumentos de la expresion "SELECT new com.example.trabajo_practico_spring.dto.FacturaReporteDTO(...)"
 * en FacturaRepository, porque JPQL lo invoca por reflexion.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FacturaReporteDTO {

    private Long numeroFactura;

    private LocalDate fechaEmision;

    private String clienteDenominacion;

    private String condicionIva;

    private String puntoVentaDescripcion;

    private double importeTotal;

    private long cantidadItems;
}
