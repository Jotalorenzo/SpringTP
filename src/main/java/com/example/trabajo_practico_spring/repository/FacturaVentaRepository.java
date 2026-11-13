package com.example.trabajo_practico_spring.repository;

import com.example.trabajo_practico_spring.model.FacturaVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface FacturaVentaRepository extends JpaRepository<FacturaVenta, Long> {

    @Query("SELECT MAX(f.fechaEmision) FROM FacturaVenta f WHERE f.puntoVenta.id = :puntoVentaId")
    Optional<LocalDate> findMaxFechaEmisionByPuntoVentaId(@Param("puntoVentaId") Long puntoVentaId);
}