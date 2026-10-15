package com.example.trabajo_practico_spring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * DTO de domicilio. Se usa como respuesta y como dato anidado en ClienteRequestDTO (se persiste en cascada con el cliente).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class DomicilioDTO {

    private Long id;

    private String nombreCalle;

    private String numeroCalle;
}
