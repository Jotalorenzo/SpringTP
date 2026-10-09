package com.example.trabajo_practico_spring.repository;

import com.example.trabajo_practico_spring.model.FacturaVenta;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacturaVentaRepository extends JpaRepository<FacturaVenta, Long> {
}
