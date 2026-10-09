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
@Table(name = "rubro")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(exclude = {"denominacion"})
public class Rubro extends AuditoriaApp {

    @Column(name = "codigo", nullable = false)
    private Integer codigo;

    @Column(name = "denominacion", nullable = false, length = 80)
    private String denominacion;
}
