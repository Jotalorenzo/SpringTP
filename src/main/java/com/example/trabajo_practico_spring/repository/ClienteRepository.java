package com.example.trabajo_practico_spring.repository;

import com.example.trabajo_practico_spring.model.Cliente;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    @Query("SELECT c FROM Cliente c " +
           "WHERE (:termino IS NULL OR :termino = '' OR " +
           "       LOWER(c.denominacion) LIKE LOWER(CONCAT('%', :termino, '%')) OR " +
           "       c.cuitCuil LIKE CONCAT('%', :termino, '%')) " +
           "ORDER BY c.denominacion ASC")
    List<Cliente> buscarPorTermino(@Param("termino") String termino, Pageable pageable);
}