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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"observaciones"})
public class FacturaRequestDTO {

    private Long numero;

    @NotNull(message = "La fecha de emisión es obligatoria")
    private LocalDate fechaEmision;

    @NotNull(message = "El puntoVentaId es obligatorio")
    private Long puntoVentaId;

    //@NotNull(message = "El condicionIvaId es obligatorio")
    private Long condicionIvaId;

    @NotNull(message = "El tipoMonedaId es obligatorio")
    private Long tipoMonedaId;

    @NotNull(message = "Debe indicar la Lista de Precios")
    private Long listaPrecioId;

    @Size(max = 5)
    @Builder.Default
    private String letra = "B"; // 'A', 'B', 'C', 'X'

    private Long clienteId;

    @Size(max = 30)
    private String estado;

    @Size(max = 255)
    private String observaciones;

    @Valid
    @NotEmpty(message = "La factura debe tener al menos un detalle")
    @Builder.Default
    private List<FacturaDetalleRequestDTO> detalles = new ArrayList<>();
}