package com.example.trabajo_practico_spring.repository;

import com.example.trabajo_practico_spring.dto.FacturaReporteDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class FacturaRepository {

    @PersistenceContext
    private EntityManager em;

    private static final String SELECT_BASE =
            "SELECT new com.example.trabajo_practico_spring.dto.FacturaReporteDTO(" +
            "   f.numero, " +
            "   f.fechaEmision, " +
            "   COALESCE(c.denominacion, 'Consumidor Final'), " +
            "   ci.denominacion, " +
            "   pv.descripcion, " +
            "   f.importeTotal, " +
            "   COUNT(d)" +
            ") " +
            "FROM FacturaVenta f " +
            "LEFT JOIN f.cliente c " +
            "JOIN f.condicionIva ci " +
            "JOIN f.puntoVenta pv " +
            "LEFT JOIN f.detalles d ";

    private static final String GROUP_BY =
            "GROUP BY f.id, f.numero, f.fechaEmision, c.denominacion, " +
            "ci.denominacion, pv.descripcion, f.importeTotal " +
            "ORDER BY f.fechaEmision DESC, f.numero DESC";

    public List<FacturaReporteDTO> buscarFacturasFiltradas(LocalDate fechaDesde,
                                                           LocalDate fechaHasta,
                                                           String estado,
                                                           Double montoMinimo) {
        StringBuilder jpql = new StringBuilder(SELECT_BASE);
        Map<String, Object> params = new HashMap<>();

        if (fechaDesde != null) {
            jpql.append("WHERE f.fechaEmision >= :fechaDesde ");
            params.put("fechaDesde", fechaDesde);
        }
        if (fechaHasta != null) {
            String where = params.isEmpty() ? "WHERE" : "AND";
            jpql.append(where).append(" f.fechaEmision <= :fechaHasta ");
            params.put("fechaHasta", fechaHasta);
        }
        if (estado != null && !estado.trim().isEmpty()) {
            String where = params.isEmpty() ? "WHERE" : "AND";
            jpql.append(where).append(" f.estado = :estado ");
            params.put("estado", estado.trim().toUpperCase());
        }
        if (montoMinimo != null) {
            String where = params.isEmpty() ? "WHERE" : "AND";
            jpql.append(where).append(" f.importeTotal >= :montoMinimo ");
            params.put("montoMinimo", montoMinimo);
        }

        jpql.append(GROUP_BY);

        TypedQuery<FacturaReporteDTO> query = em.createQuery(jpql.toString(), FacturaReporteDTO.class);
        params.forEach(query::setParameter);
        return query.getResultList();
    }
}
