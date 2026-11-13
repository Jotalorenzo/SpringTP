package com.example.trabajo_practico_spring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticuloPrecioDTO {
    private Long articuloId;
    private Long listaPrecioArticuloId;
    private String codigo;
    private String denominacion;
    private double precioVenta;
}