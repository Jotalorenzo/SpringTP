package com.example.trabajo_practico_spring.service;

import com.example.trabajo_practico_spring.dto.CondicionIvaDTO;
import com.example.trabajo_practico_spring.dto.CondicionIvaRequestDTO;
import com.example.trabajo_practico_spring.exception.RecursoNoEncontradoException;
import com.example.trabajo_practico_spring.model.CondicionIva;
import com.example.trabajo_practico_spring.repository.CondicionIvaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CondicionIvaService {

    private final CondicionIvaRepository repository;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public List<CondicionIvaDTO> listar() {
        return repository.findAll().stream().map(this::aDto).toList();
    }

    @Transactional(readOnly = true)
    public CondicionIvaDTO obtener(Long id) {
        return aDto(buscar(id));
    }

    @Transactional
    public CondicionIvaDTO crear(CondicionIvaRequestDTO request) {
        CondicionIva entidad = new CondicionIva();
        copiar(entidad, request);
        auditoria.completarAlta(entidad);
        return aDto(repository.save(entidad));
    }

    @Transactional
    public CondicionIvaDTO actualizar(Long id, CondicionIvaRequestDTO request) {
        CondicionIva entidad = buscar(id);
        copiar(entidad, request);
        auditoria.completarModificacion(entidad);
        return aDto(repository.save(entidad));
    }

    @Transactional
    public void eliminar(Long id) {
        repository.delete(buscar(id));
    }

    private CondicionIva buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("CondicionIva", id));
    }

    private void copiar(CondicionIva entidad, CondicionIvaRequestDTO request) {
        entidad.setCodigoAfip(request.getCodigoAfip());
        entidad.setDenominacion(request.getDenominacion());
    }

    private CondicionIvaDTO aDto(CondicionIva e) {
        return CondicionIvaDTO.builder()
                .id(e.getId())
                .codigoAfip(e.getCodigoAfip())
                .denominacion(e.getDenominacion())
                .build();
    }
}
