package com.example.trabajo_practico_spring.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Request DTO (POST/PUT): recibe IDs para las relaciones, sin entidades anidadas.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ListaPrecioArticuloRequestDTO {

    @NotNull(message = "El listaPrecioId es obligatorio")
    private Long listaPrecioId;

    @NotNull(message = "El articuloId es obligatorio")
    private Long articuloId;

    @PositiveOrZero(message = "El precio no puede ser negativo")
    private double precioVenta;
}
