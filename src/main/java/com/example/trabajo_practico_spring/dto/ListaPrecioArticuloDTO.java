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
public class ListaPrecioArticuloDTO {

    private Long id;

    private Long listaPrecioId;

    private Long articuloId;

    private String articuloCodigo;

    private String articuloDenominacion;

    private double precioVenta;
}
