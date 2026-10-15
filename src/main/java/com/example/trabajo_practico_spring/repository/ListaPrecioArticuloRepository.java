package com.example.trabajo_practico_spring.repository;

import com.example.trabajo_practico_spring.model.ListaPrecioArticulo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ListaPrecioArticuloRepository extends JpaRepository<ListaPrecioArticulo, Long> {

    /** Precio vigente de un articulo: toma el primer registro de lista de precios que lo contiene. */
    Optional<ListaPrecioArticulo> findFirstByArticuloIdOrderByIdAsc(Long articuloId);
}
