package com.example.trabajo_practico_spring.service;

import com.example.trabajo_practico_spring.dto.ClienteDTO;
import com.example.trabajo_practico_spring.dto.ClienteRequestDTO;
import com.example.trabajo_practico_spring.exception.RecursoNoEncontradoException;
import com.example.trabajo_practico_spring.model.Cliente;
import com.example.trabajo_practico_spring.repository.ClienteRepository;
import com.example.trabajo_practico_spring.dto.ContactoDTO;
import com.example.trabajo_practico_spring.dto.DomicilioDTO;
import com.example.trabajo_practico_spring.model.Contacto;
import com.example.trabajo_practico_spring.model.Domicilio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository repository;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public List<ClienteDTO> listar() {
        return repository.findAll().stream().map(this::aDto).toList();
    }

    @Transactional(readOnly = true)
    public ClienteDTO obtener(Long id) {
        return aDto(buscar(id));
    }

    @Transactional
    public ClienteDTO crear(ClienteRequestDTO request) {
        Cliente entidad = new Cliente();
        copiar(entidad, request);
        auditoria.completarAlta(entidad);
        return aDto(repository.save(entidad));
    }

    @Transactional
    public ClienteDTO actualizar(Long id, ClienteRequestDTO request) {
        Cliente entidad = buscar(id);
        copiar(entidad, request);
        auditoria.completarModificacion(entidad);
        return aDto(repository.save(entidad));
    }

    @Transactional
    public void eliminar(Long id) {
        repository.delete(buscar(id));
    }

    private Cliente buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", id));
    }

    private void copiar(Cliente entidad, ClienteRequestDTO request) {
        entidad.setCuitCuil(request.getCuitCuil());
        entidad.setDenominacion(request.getDenominacion());

        // Contacto y domicilio se guardan en cascada junto con el cliente.
        Contacto contacto = entidad.getContacto() != null ? entidad.getContacto() : new Contacto();
        contacto.setEmail(request.getContacto().getEmail());
        contacto.setTelefono(request.getContacto().getTelefono());
        contacto.setCelular(request.getContacto().getCelular());
        entidad.setContacto(contacto);

        Domicilio domicilio = entidad.getDomicilio() != null ? entidad.getDomicilio() : new Domicilio();
        domicilio.setNombreCalle(request.getDomicilio().getNombreCalle());
        domicilio.setNumeroCalle(request.getDomicilio().getNumeroCalle());
        entidad.setDomicilio(domicilio);
    }

    private ClienteDTO aDto(Cliente e) {
        Contacto c = e.getContacto();
        Domicilio d = e.getDomicilio();
        return ClienteDTO.builder()
                .id(e.getId())
                .cuitCuil(e.getCuitCuil())
                .denominacion(e.getDenominacion())
                .contacto(c == null ? null : ContactoDTO.builder()
                        .id(c.getId()).email(c.getEmail()).telefono(c.getTelefono()).celular(c.getCelular()).build())
                .domicilio(d == null ? null : DomicilioDTO.builder()
                        .id(d.getId()).nombreCalle(d.getNombreCalle()).numeroCalle(d.getNumeroCalle()).build())
                .build();
    }
}
