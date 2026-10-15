package com.example.trabajo_practico_spring.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Renglon de una factura recibido en las peticiones POST y PUT.
 *
 * Solo trae identificadores y datos ingresados por el usuario. El precio unitario
 * y los importes (neto, IVA, subtotal) los calcula el servicio a partir de la
 * lista de precios, por lo que no se aceptan desde el cliente.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class FacturaDetalleRequestDTO {

    @NotNull(message = "El articuloId es obligatorio")
    private Long articuloId;

    @Size(max = 200, message = "La descripcion no puede superar los 200 caracteres")
    private String descripcion;

    @Positive(message = "La cantidad debe ser mayor a cero")
    private double cantidad;

    @DecimalMin(value = "0.0", message = "La bonificacion no puede ser negativa")
    @DecimalMax(value = "100.0", message = "La bonificacion no puede superar el 100%")
    private double porcentajeBonificacion;
}
