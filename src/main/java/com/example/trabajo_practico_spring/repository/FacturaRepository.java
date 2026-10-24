package com.example.trabajo_practico_spring.repository;

import com.example.trabajo_practico_spring.dto.FacturaReporteDTO;
import com.example.trabajo_practico_spring.dto.PaginaDTO;
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
            "   f.id, " +
            "   f.numero, " +
            "   f.fechaEmision, " +
            "   COALESCE(c.denominacion, 'Consumidor Final'), " +
            "   ci.denominacion, " +
            "   pv.descripcion, " +
            "   f.importeTotal, " +
            "   COUNT(d), " +
            "   f.estado, " +
            "   f.cae" +
            ") " +
            "FROM FacturaVenta f " +
            "LEFT JOIN f.cliente c " +
            "JOIN f.condicionIva ci " +
            "JOIN f.puntoVenta pv " +
            "LEFT JOIN f.detalles d ";

    private static final String GROUP_BY =
            "GROUP BY f.id, f.numero, f.fechaEmision, c.denominacion, " +
            "ci.denominacion, pv.descripcion, f.importeTotal, f.estado, f.cae " +
            "ORDER BY f.fechaEmision DESC, f.id DESC";

    public PaginaDTO<FacturaReporteDTO> buscarFacturasPaginadas(
            Long numeroFactura, LocalDate fechaDesde, LocalDate fechaHasta,
            String cliente, String estado, Boolean informadaArca, int pagina, int tamanio) {

        StringBuilder whereClause = new StringBuilder();
        Map<String, Object> params = new HashMap<>();

        if (numeroFactura != null) {
            agregarCondicion(whereClause, "f.numero = :numeroFactura");
            params.put("numeroFactura", numeroFactura);
        }
        if (fechaDesde != null) {
            agregarCondicion(whereClause, "f.fechaEmision >= :fechaDesde");
            params.put("fechaDesde", fechaDesde);
        }
        if (fechaHasta != null) {
            agregarCondicion(whereClause, "f.fechaEmision <= :fechaHasta");
            params.put("fechaHasta", fechaHasta);
        }
        if (cliente != null && !cliente.isBlank()) {
            agregarCondicion(whereClause, "LOWER(c.denominacion) LIKE LOWER(:cliente)");
            params.put("cliente", "%" + cliente.trim() + "%");
        }
        if (estado != null && !estado.isBlank()) {
            agregarCondicion(whereClause, "f.estado = :estado");
            params.put("estado", estado.trim().toUpperCase());
        }
        if (informadaArca != null) {
            if (informadaArca) {
                agregarCondicion(whereClause, "(f.cae IS NOT NULL AND f.cae <> '')");
            } else {
                agregarCondicion(whereClause, "(f.cae IS NULL OR f.cae = '')");
            }
        }

        // 1. Consulta de Contenido Paginado
        String jpql = SELECT_BASE + whereClause + " " + GROUP_BY;
        TypedQuery<FacturaReporteDTO> query = em.createQuery(jpql, FacturaReporteDTO.class);
        params.forEach(query::setParameter);

        query.setFirstResult(pagina * tamanio);
        query.setMaxResults(tamanio);
        List<FacturaReporteDTO> contenido = query.getResultList();

        // 2. Consulta de Conteo Total
        String jpqlCount = "SELECT COUNT(f) FROM FacturaVenta f LEFT JOIN f.cliente c " + whereClause;
        TypedQuery<Long> queryCount = em.createQuery(jpqlCount, Long.class);
        params.forEach(queryCount::setParameter);
        long totalElementos = queryCount.getSingleResult();

        int totalPaginas = (int) Math.ceil((double) totalElementos / tamanio);

        return PaginaDTO.<FacturaReporteDTO>builder()
                .contenido(contenido)
                .pagina(pagina)
                .tamanio(tamanio)
                .totalElementos(totalElementos)
                .totalPaginas(totalPaginas)
                .build();
    }

    private void agregarCondicion(StringBuilder where, String condicion) {
        if (where.length() == 0) {
            where.append(" WHERE ").append(condicion);
        } else {
            where.append(" AND ").append(condicion);
        }
    }

    // Mantenido para los reportes de Excel y PDF anteriores
    public List<FacturaReporteDTO> buscarFacturasFiltradas(LocalDate fechaDesde, LocalDate fechaHasta, String estado, Double montoMinimo) {
        PaginaDTO<FacturaReporteDTO> p = buscarFacturasPaginadas(null, fechaDesde, fechaHasta, null, estado, null, 0, 1000);
        return p.getContenido();
    }
}