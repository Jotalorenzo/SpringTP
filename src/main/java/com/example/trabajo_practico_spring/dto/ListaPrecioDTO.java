package com.example.trabajo_practico_spring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Response DTO: representacion plana de la entidad, sin referencias circulares ni datos sensibles.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ListaPrecioDTO {

    private Long id;

    private String codigo;

    private String denominacion;
}
