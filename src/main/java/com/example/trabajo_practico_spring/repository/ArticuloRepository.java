package com.example.trabajo_practico_spring.repository;

import com.example.trabajo_practico_spring.model.Articulo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticuloRepository extends JpaRepository<Articulo, Long> {
}
