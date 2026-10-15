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
public class ArticuloRequestDTO {

    @NotBlank(message = "El codigo es obligatorio")
    @Size(max = 30, message = "No puede superar los 30 caracteres")
    private String codigo;

    @NotBlank(message = "La denominacion es obligatoria")
    @Size(max = 150, message = "No puede superar los 150 caracteres")
    private String denominacion;

    private Long rubroId;

    private Long marcaId;
}
