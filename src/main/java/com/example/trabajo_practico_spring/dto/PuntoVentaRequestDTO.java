package com.example.trabajo_practico_spring.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class PuntoVentaRequestDTO {

    @NotNull(message = "El numero es obligatorio")
    private Integer numero;

    @Size(max = 120, message = "No puede superar los 120 caracteres")
    private String descripcion;

    @Size(max = 40, message = "No puede superar los 40 caracteres")
    private String tipoEmision;

    @Size(max = 200, message = "No puede superar los 200 caracteres")
    private String domicilioComercial;
}
