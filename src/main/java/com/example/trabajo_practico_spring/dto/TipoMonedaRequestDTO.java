package com.example.trabajo_practico_spring.dto;

import jakarta.validation.constraints.NotBlank;
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
public class TipoMonedaRequestDTO {

    @NotBlank(message = "El codigo AFIP es obligatorio")
    @Size(max = 10, message = "No puede superar los 10 caracteres")
    private String codigoAfip;

    @NotBlank(message = "La denominacion es obligatoria")
    @Size(max = 80, message = "No puede superar los 80 caracteres")
    private String denominacion;

    @NotBlank(message = "El simbolo es obligatorio")
    @Size(max = 10, message = "No puede superar los 10 caracteres")
    private String simbolo;
}
