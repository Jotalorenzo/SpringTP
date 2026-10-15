package com.example.trabajo_practico_spring.service;

import com.example.trabajo_practico_spring.dto.ListaPrecioArticuloDTO;
import com.example.trabajo_practico_spring.dto.ListaPrecioArticuloRequestDTO;
import com.example.trabajo_practico_spring.exception.RecursoNoEncontradoException;
import com.example.trabajo_practico_spring.model.ListaPrecioArticulo;
import com.example.trabajo_practico_spring.repository.ListaPrecioArticuloRepository;
import com.example.trabajo_practico_spring.repository.ListaPrecioRepository;
import com.example.trabajo_practico_spring.repository.ArticuloRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListaPrecioArticuloService {

    private final ListaPrecioArticuloRepository repository;
    private final AuditoriaService auditoria;
    private final ListaPrecioRepository listaPrecioRepository;
    private final ArticuloRepository articuloRepository;

    @Transactional(readOnly = true)
    public List<ListaPrecioArticuloDTO> listar() {
        return repository.findAll().stream().map(this::aDto).toList();
    }

    @Transactional(readOnly = true)
    public ListaPrecioArticuloDTO obtener(Long id) {
        return aDto(buscar(id));
    }

    @Transactional
    public ListaPrecioArticuloDTO crear(ListaPrecioArticuloRequestDTO request) {
        ListaPrecioArticulo entidad = new ListaPrecioArticulo();
        copiar(entidad, request);
        auditoria.completarAlta(entidad);
        return aDto(repository.save(entidad));
    }

    @Transactional
    public ListaPrecioArticuloDTO actualizar(Long id, ListaPrecioArticuloRequestDTO request) {
        ListaPrecioArticulo entidad = buscar(id);
        copiar(entidad, request);
        auditoria.completarModificacion(entidad);
        return aDto(repository.save(entidad));
    }

    @Transactional
    public void eliminar(Long id) {
        repository.delete(buscar(id));
    }

    private ListaPrecioArticulo buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("ListaPrecioArticulo", id));
    }

    private void copiar(ListaPrecioArticulo entidad, ListaPrecioArticuloRequestDTO request) {
        entidad.setListaPrecio(listaPrecioRepository.findById(request.getListaPrecioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("ListaPrecio", request.getListaPrecioId())));
        entidad.setArticulo(articuloRepository.findById(request.getArticuloId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Articulo", request.getArticuloId())));
        entidad.setPrecioVenta(request.getPrecioVenta());
    }

    private ListaPrecioArticuloDTO aDto(ListaPrecioArticulo e) {
        return ListaPrecioArticuloDTO.builder()
                .id(e.getId())
                .listaPrecioId(e.getListaPrecio().getId())
                .articuloId(e.getArticulo().getId())
                .articuloCodigo(e.getArticulo().getCodigo())
                .articuloDenominacion(e.getArticulo().getDenominacion())
                .precioVenta(e.getPrecioVenta())
                .build();
    }
}
