package com.example.trabajo_practico_spring.service;

import com.example.trabajo_practico_spring.dto.ListaPrecioDTO;
import com.example.trabajo_practico_spring.dto.ListaPrecioRequestDTO;
import com.example.trabajo_practico_spring.exception.RecursoNoEncontradoException;
import com.example.trabajo_practico_spring.model.ListaPrecio;
import com.example.trabajo_practico_spring.repository.ListaPrecioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListaPrecioService {

    private final ListaPrecioRepository repository;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public List<ListaPrecioDTO> listar() {
        return repository.findAll().stream().map(this::aDto).toList();
    }

    @Transactional(readOnly = true)
    public ListaPrecioDTO obtener(Long id) {
        return aDto(buscar(id));
    }

    @Transactional
    public ListaPrecioDTO crear(ListaPrecioRequestDTO request) {
        ListaPrecio entidad = new ListaPrecio();
        copiar(entidad, request);
        auditoria.completarAlta(entidad);
        return aDto(repository.save(entidad));
    }

    @Transactional
    public ListaPrecioDTO actualizar(Long id, ListaPrecioRequestDTO request) {
        ListaPrecio entidad = buscar(id);
        copiar(entidad, request);
        auditoria.completarModificacion(entidad);
        return aDto(repository.save(entidad));
    }

    @Transactional
    public void eliminar(Long id) {
        repository.delete(buscar(id));
    }

    private ListaPrecio buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("ListaPrecio", id));
    }

    private void copiar(ListaPrecio entidad, ListaPrecioRequestDTO request) {
        entidad.setCodigo(request.getCodigo());
        entidad.setDenominacion(request.getDenominacion());
    }

    private ListaPrecioDTO aDto(ListaPrecio e) {
        return ListaPrecioDTO.builder()
                .id(e.getId())
                .codigo(e.getCodigo())
                .denominacion(e.getDenominacion())
                .build();
    }
}
