package com.example.trabajo_practico_spring.service;

import com.example.trabajo_practico_spring.dto.PuntoVentaDTO;
import com.example.trabajo_practico_spring.dto.PuntoVentaRequestDTO;
import com.example.trabajo_practico_spring.exception.RecursoNoEncontradoException;
import com.example.trabajo_practico_spring.model.PuntoVenta;
import com.example.trabajo_practico_spring.repository.PuntoVentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PuntoVentaService {

    private final PuntoVentaRepository repository;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public List<PuntoVentaDTO> listar() {
        return repository.findAll().stream().map(this::aDto).toList();
    }

    @Transactional(readOnly = true)
    public PuntoVentaDTO obtener(Long id) {
        return aDto(buscar(id));
    }

    @Transactional
    public PuntoVentaDTO crear(PuntoVentaRequestDTO request) {
        PuntoVenta entidad = new PuntoVenta();
        copiar(entidad, request);
        auditoria.completarAlta(entidad);
        return aDto(repository.save(entidad));
    }

    @Transactional
    public PuntoVentaDTO actualizar(Long id, PuntoVentaRequestDTO request) {
        PuntoVenta entidad = buscar(id);
        copiar(entidad, request);
        auditoria.completarModificacion(entidad);
        return aDto(repository.save(entidad));
    }

    @Transactional
    public void eliminar(Long id) {
        repository.delete(buscar(id));
    }

    private PuntoVenta buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("PuntoVenta", id));
    }

    private void copiar(PuntoVenta entidad, PuntoVentaRequestDTO request) {
        entidad.setNumero(request.getNumero());
        entidad.setDescripcion(request.getDescripcion());
        entidad.setTipoEmision(request.getTipoEmision());
        entidad.setDomicilioComercial(request.getDomicilioComercial());
    }

    private PuntoVentaDTO aDto(PuntoVenta e) {
        return PuntoVentaDTO.builder()
                .id(e.getId())
                .numero(e.getNumero())
                .descripcion(e.getDescripcion())
                .tipoEmision(e.getTipoEmision())
                .domicilioComercial(e.getDomicilioComercial())
                .build();
    }
}
