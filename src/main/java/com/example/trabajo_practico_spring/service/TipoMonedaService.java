package com.example.trabajo_practico_spring.service;

import com.example.trabajo_practico_spring.dto.TipoMonedaDTO;
import com.example.trabajo_practico_spring.dto.TipoMonedaRequestDTO;
import com.example.trabajo_practico_spring.exception.RecursoNoEncontradoException;
import com.example.trabajo_practico_spring.model.TipoMoneda;
import com.example.trabajo_practico_spring.repository.TipoMonedaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TipoMonedaService {

    private final TipoMonedaRepository repository;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public List<TipoMonedaDTO> listar() {
        return repository.findAll().stream().map(this::aDto).toList();
    }

    @Transactional(readOnly = true)
    public TipoMonedaDTO obtener(Long id) {
        return aDto(buscar(id));
    }

    @Transactional
    public TipoMonedaDTO crear(TipoMonedaRequestDTO request) {
        TipoMoneda entidad = new TipoMoneda();
        copiar(entidad, request);
        auditoria.completarAlta(entidad);
        return aDto(repository.save(entidad));
    }

    @Transactional
    public TipoMonedaDTO actualizar(Long id, TipoMonedaRequestDTO request) {
        TipoMoneda entidad = buscar(id);
        copiar(entidad, request);
        auditoria.completarModificacion(entidad);
        return aDto(repository.save(entidad));
    }

    @Transactional
    public void eliminar(Long id) {
        repository.delete(buscar(id));
    }

    private TipoMoneda buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("TipoMoneda", id));
    }

    private void copiar(TipoMoneda entidad, TipoMonedaRequestDTO request) {
        entidad.setCodigoAfip(request.getCodigoAfip());
        entidad.setDenominacion(request.getDenominacion());
        entidad.setSimbolo(request.getSimbolo());
    }

    private TipoMonedaDTO aDto(TipoMoneda e) {
        return TipoMonedaDTO.builder()
                .id(e.getId())
                .codigoAfip(e.getCodigoAfip())
                .denominacion(e.getDenominacion())
                .simbolo(e.getSimbolo())
                .build();
    }
}
