package com.example.trabajo_practico_spring.service;

import com.example.trabajo_practico_spring.dto.MarcaDTO;
import com.example.trabajo_practico_spring.dto.MarcaRequestDTO;
import com.example.trabajo_practico_spring.exception.RecursoNoEncontradoException;
import com.example.trabajo_practico_spring.model.Marca;
import com.example.trabajo_practico_spring.repository.MarcaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MarcaService {

    private final MarcaRepository repository;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public List<MarcaDTO> listar() {
        return repository.findAll().stream().map(this::aDto).toList();
    }

    @Transactional(readOnly = true)
    public MarcaDTO obtener(Long id) {
        return aDto(buscar(id));
    }

    @Transactional
    public MarcaDTO crear(MarcaRequestDTO request) {
        Marca entidad = new Marca();
        copiar(entidad, request);
        auditoria.completarAlta(entidad);
        return aDto(repository.save(entidad));
    }

    @Transactional
    public MarcaDTO actualizar(Long id, MarcaRequestDTO request) {
        Marca entidad = buscar(id);
        copiar(entidad, request);
        auditoria.completarModificacion(entidad);
        return aDto(repository.save(entidad));
    }

    @Transactional
    public void eliminar(Long id) {
        repository.delete(buscar(id));
    }

    private Marca buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Marca", id));
    }

    private void copiar(Marca entidad, MarcaRequestDTO request) {
        entidad.setCodigo(request.getCodigo());
        entidad.setDenominacion(request.getDenominacion());
    }

    private MarcaDTO aDto(Marca e) {
        return MarcaDTO.builder()
                .id(e.getId())
                .codigo(e.getCodigo())
                .denominacion(e.getDenominacion())
                .build();
    }
}
