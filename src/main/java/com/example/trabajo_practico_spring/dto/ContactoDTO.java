package com.example.trabajo_practico_spring.dto;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * DTO de contacto. Se usa como respuesta y como dato anidado en ClienteRequestDTO (se persiste en cascada con el cliente).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ContactoDTO {

    private Long id;

    @Email(message = "El email no es valido")
    private String email;

    private String telefono;

    private String celular;
}
