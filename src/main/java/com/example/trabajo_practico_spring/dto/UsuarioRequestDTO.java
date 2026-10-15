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
 * Request DTO (POST/PUT): recibe IDs para las relaciones, sin entidades anidadas. La clave llega en texto plano y debe hashearse en el servicio antes de persistir.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"clave"})
public class UsuarioRequestDTO {

    @NotBlank(message = "El usuario es obligatorio")
    @Size(max = 60, message = "No puede superar los 60 caracteres")
    private String usuario;

    @NotBlank(message = "La clave es obligatoria")
    @Size(max = 120, message = "No puede superar los 120 caracteres")
    private String clave;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 80, message = "No puede superar los 80 caracteres")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 80, message = "No puede superar los 80 caracteres")
    private String apellido;
}
