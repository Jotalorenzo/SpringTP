package com.example.trabajo_practico_spring.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "punto_venta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(exclude = {"descripcion", "domicilioComercial"})
public class PuntoVenta extends AuditoriaApp {

    @Column(name = "numero", nullable = false)
    private int numero;

    @Column(name = "descripcion", length = 120)
    private String descripcion;

    @Column(name = "tipo_emision", length = 40)
    private String tipoEmision;

    @Column(name = "domicilio_comercial", length = 200)
    private String domicilioComercial;
}
