package com.example.trabajo_practico_spring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Response DTO: representacion plana de la entidad, sin referencias circulares ni datos sensibles.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class FacturaVentaDetalleDTO {

    private Long id;

    private Long listaPrecioArticuloId;

    private Long articuloId;

    private String articuloDenominacion;

    private String descripcion;

    private double cantidad;

    private double precioUnitario;

    private double porcentajeBonificacion;

    private double importeNeto;

    private double importeIva;

    private double importeSubtotal;
}
