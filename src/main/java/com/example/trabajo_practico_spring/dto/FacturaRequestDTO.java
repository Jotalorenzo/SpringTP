package com.example.trabajo_practico_spring.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Cuerpo de las peticiones POST y PUT de /api/facturas.
 *
 * Las relaciones se referencian por ID (puntoVentaId, clienteId, ...) en lugar de
 * enviar entidades anidadas, desacoplando el contrato de la API del modelo JPA.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"observaciones"})
public class FacturaRequestDTO {

    private Long numero;

    @NotNull(message = "La fecha de emision es obligatoria")
    private LocalDate fechaEmision;

    @NotNull(message = "El puntoVentaId es obligatorio")
    private Long puntoVentaId;

    @NotNull(message = "El condicionIvaId es obligatorio")
    private Long condicionIvaId;

    @NotNull(message = "El tipoMonedaId es obligatorio")
    private Long tipoMonedaId;

    /** Opcional: si es null se factura como Consumidor Final. */
    private Long clienteId;

    /** Opcional en el alta (por defecto "EMITIDA"); se usa para cambiar el estado en el PUT. */
    @Size(max = 30, message = "El estado no puede superar los 30 caracteres")
    private String estado;

    @Size(max = 255, message = "Las observaciones no pueden superar los 255 caracteres")
    private String observaciones;

    @Valid
    @NotEmpty(message = "La factura debe tener al menos un detalle")
    @Builder.Default
    private List<FacturaDetalleRequestDTO> detalles = new ArrayList<>();
}
