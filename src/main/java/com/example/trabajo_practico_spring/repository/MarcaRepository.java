package com.example.trabajo_practico_spring.repository;

import com.example.trabajo_practico_spring.model.Marca;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarcaRepository extends JpaRepository<Marca, Long> {
}
