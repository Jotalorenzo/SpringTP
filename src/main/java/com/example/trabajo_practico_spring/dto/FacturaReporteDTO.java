package com.example.trabajo_practico_spring.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FacturaReporteDTO {

    private Long id;
    private Long numeroFactura;
    private LocalDate fechaEmision;
    private String clienteDenominacion;
    private String condicionIva;
    private String puntoVentaDescripcion;
    private double importeTotal;
    private long cantidadItems;
    private String estado;
    private String cae;

    // Constructor compatible con reportes anteriores que no pedían estado ni cae
    public FacturaReporteDTO(Long numeroFactura, LocalDate fechaEmision, String clienteDenominacion,
                             String condicionIva, String puntoVentaDescripcion, double importeTotal,
                             long cantidadItems) {
        this.numeroFactura = numeroFactura;
        this.fechaEmision = fechaEmision;
        this.clienteDenominacion = clienteDenominacion;
        this.condicionIva = condicionIva;
        this.puntoVentaDescripcion = puntoVentaDescripcion;
        this.importeTotal = importeTotal;
        this.cantidadItems = cantidadItems;
    }
}