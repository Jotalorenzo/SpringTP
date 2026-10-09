package com.example.trabajo_practico_spring.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "cliente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, exclude = {"contacto", "domicilio"})
@ToString(exclude = {"contacto", "domicilio"})
public class Cliente extends AuditoriaApp {

    @Column(name = "cuit_cuil", nullable = false, unique = true, length = 20)
    private String cuitCuil;

    @Column(name = "denominacion", nullable = false, length = 120)
    private String denominacion;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "contacto_id", nullable = false, unique = true)
    private Contacto contacto;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "domicilio_id", nullable = false, unique = true)
    private Domicilio domicilio;
}
