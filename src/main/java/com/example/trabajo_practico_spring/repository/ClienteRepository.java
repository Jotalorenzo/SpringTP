package com.example.trabajo_practico_spring.repository;

import com.example.trabajo_practico_spring.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
}
