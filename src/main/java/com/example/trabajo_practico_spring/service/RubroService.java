package com.example.trabajo_practico_spring.service;

import com.example.trabajo_practico_spring.dto.RubroDTO;
import com.example.trabajo_practico_spring.dto.RubroRequestDTO;
import com.example.trabajo_practico_spring.exception.RecursoNoEncontradoException;
import com.example.trabajo_practico_spring.model.Rubro;
import com.example.trabajo_practico_spring.repository.RubroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RubroService {

    private final RubroRepository repository;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public List<RubroDTO> listar() {
        return repository.findAll().stream().map(this::aDto).toList();
    }

    @Transactional(readOnly = true)
    public RubroDTO obtener(Long id) {
        return aDto(buscar(id));
    }

    @Transactional
    public RubroDTO crear(RubroRequestDTO request) {
        Rubro entidad = new Rubro();
        copiar(entidad, request);
        auditoria.completarAlta(entidad);
        return aDto(repository.save(entidad));
    }

    @Transactional
    public RubroDTO actualizar(Long id, RubroRequestDTO request) {
        Rubro entidad = buscar(id);
        copiar(entidad, request);
        auditoria.completarModificacion(entidad);
        return aDto(repository.save(entidad));
    }

    @Transactional
    public void eliminar(Long id) {
        repository.delete(buscar(id));
    }

    private Rubro buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Rubro", id));
    }

    private void copiar(Rubro entidad, RubroRequestDTO request) {
        entidad.setCodigo(request.getCodigo());
        entidad.setDenominacion(request.getDenominacion());
    }

    private RubroDTO aDto(Rubro e) {
        return RubroDTO.builder()
                .id(e.getId())
                .codigo(e.getCodigo())
                .denominacion(e.getDenominacion())
                .build();
    }
}
