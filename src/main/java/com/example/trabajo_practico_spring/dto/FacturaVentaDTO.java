package com.example.trabajo_practico_spring.dto;

import java.time.LocalDate;
import java.util.List;
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
@ToString(exclude = {"detalles"})
public class FacturaVentaDTO {

    private Long id;

    private Long numero;

    private LocalDate fechaEmision;

    private Long puntoVentaId;

    private String puntoVentaDescripcion;

    private Long clienteId;

    private String clienteDenominacion;

    private Long tipoMonedaId;

    private Long condicionIvaId;

    private double importeCobrado;

    private double importeSaldo;

    private double importeTotal;

    private String cae;

    private LocalDate caeFechaVencimiento;

    private String resultadoAfip;

    private String motivoRechazo;

    private String estado;

    private LocalDate fechaAnulacion;

    private String observaciones;

    private List<FacturaVentaDetalleDTO> detalles;
}
