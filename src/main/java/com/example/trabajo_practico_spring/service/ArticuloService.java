package com.example.trabajo_practico_spring.service;

import com.example.trabajo_practico_spring.dto.ArticuloDTO;
import com.example.trabajo_practico_spring.dto.ArticuloRequestDTO;
import com.example.trabajo_practico_spring.exception.RecursoNoEncontradoException;
import com.example.trabajo_practico_spring.model.Articulo;
import com.example.trabajo_practico_spring.repository.ArticuloRepository;
import com.example.trabajo_practico_spring.repository.RubroRepository;
import com.example.trabajo_practico_spring.repository.MarcaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticuloService {

    private final ArticuloRepository repository;
    private final AuditoriaService auditoria;
    private final RubroRepository rubroRepository;
    private final MarcaRepository marcaRepository;

    @Transactional(readOnly = true)
    public List<ArticuloDTO> listar() {
        return repository.findAll().stream().map(this::aDto).toList();
    }

    @Transactional(readOnly = true)
    public ArticuloDTO obtener(Long id) {
        return aDto(buscar(id));
    }

    @Transactional
    public ArticuloDTO crear(ArticuloRequestDTO request) {
        Articulo entidad = new Articulo();
        copiar(entidad, request);
        auditoria.completarAlta(entidad);
        return aDto(repository.save(entidad));
    }

    @Transactional
    public ArticuloDTO actualizar(Long id, ArticuloRequestDTO request) {
        Articulo entidad = buscar(id);
        copiar(entidad, request);
        auditoria.completarModificacion(entidad);
        return aDto(repository.save(entidad));
    }

    @Transactional
    public void eliminar(Long id) {
        repository.delete(buscar(id));
    }

    private Articulo buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Articulo", id));
    }

    private void copiar(Articulo entidad, ArticuloRequestDTO request) {
        entidad.setCodigo(request.getCodigo());
        entidad.setDenominacion(request.getDenominacion());
        entidad.setRubro(request.getRubroId() == null ? null : rubroRepository.findById(request.getRubroId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Rubro", request.getRubroId())));
        entidad.setMarca(request.getMarcaId() == null ? null : marcaRepository.findById(request.getMarcaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Marca", request.getMarcaId())));
    }

    private ArticuloDTO aDto(Articulo e) {
        return ArticuloDTO.builder()
                .id(e.getId())
                .codigo(e.getCodigo())
                .denominacion(e.getDenominacion())
                .rubroId(e.getRubro() == null ? null : e.getRubro().getId())
                .rubroDenominacion(e.getRubro() == null ? null : e.getRubro().getDenominacion())
                .marcaId(e.getMarca() == null ? null : e.getMarca().getId())
                .marcaDenominacion(e.getMarca() == null ? null : e.getMarca().getDenominacion())
                .build();
    }
}
