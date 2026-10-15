package com.example.trabajo_practico_spring.dto;

import jakarta.validation.constraints.NotBlank;
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
public class MarcaRequestDTO {

    @NotNull(message = "El codigo es obligatorio")
    private Integer codigo;

    @NotBlank(message = "La denominacion es obligatoria")
    @Size(max = 80, message = "No puede superar los 80 caracteres")
    private String denominacion;
}
