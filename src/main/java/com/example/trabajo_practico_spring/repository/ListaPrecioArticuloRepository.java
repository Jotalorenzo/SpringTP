package com.example.trabajo_practico_spring.repository;

import com.example.trabajo_practico_spring.model.ListaPrecioArticulo;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ListaPrecioArticuloRepository extends JpaRepository<ListaPrecioArticulo, Long> {

    Optional<ListaPrecioArticulo> findFirstByArticuloIdOrderByIdAsc(Long articuloId);

    Optional<ListaPrecioArticulo> findByListaPrecioIdAndArticuloId(Long listaPrecioId, Long articuloId);

    @Query("SELECT lpa FROM ListaPrecioArticulo lpa " +
           "JOIN FETCH lpa.articulo a " +
           "WHERE lpa.listaPrecio.id = :listaPrecioId " +
           "AND (:termino IS NULL OR :termino = '' OR " +
           "     LOWER(a.denominacion) LIKE LOWER(CONCAT('%', :termino, '%')) OR " +
           "     LOWER(a.codigo) LIKE LOWER(CONCAT('%', :termino, '%'))) " +
           "ORDER BY a.denominacion ASC")
    List<ListaPrecioArticulo> buscarPorListaYTermino(
            @Param("listaPrecioId") Long listaPrecioId,
            @Param("termino") String termino,
            Pageable pageable);
}