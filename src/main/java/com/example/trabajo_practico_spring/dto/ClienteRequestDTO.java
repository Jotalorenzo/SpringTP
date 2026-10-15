package com.example.trabajo_practico_spring.dto;

import jakarta.validation.Valid;
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
public class ClienteRequestDTO {

    @NotBlank(message = "El CUIT/CUIL es obligatorio")
    @Size(max = 20, message = "No puede superar los 20 caracteres")
    private String cuitCuil;

    @NotBlank(message = "La denominacion es obligatoria")
    @Size(max = 120, message = "No puede superar los 120 caracteres")
    private String denominacion;

    @Valid
    @NotNull(message = "El contacto es obligatorio")
    private ContactoDTO contacto;

    @Valid
    @NotNull(message = "El domicilio es obligatorio")
    private DomicilioDTO domicilio;
}
