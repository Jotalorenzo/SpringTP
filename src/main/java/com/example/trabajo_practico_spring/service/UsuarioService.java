package com.example.trabajo_practico_spring.service;

import com.example.trabajo_practico_spring.dto.UsuarioDTO;
import com.example.trabajo_practico_spring.dto.UsuarioRequestDTO;
import com.example.trabajo_practico_spring.exception.RecursoNoEncontradoException;
import com.example.trabajo_practico_spring.model.Usuario;
import com.example.trabajo_practico_spring.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository repository;

    @Transactional(readOnly = true)
    public List<UsuarioDTO> listar() {
        return repository.findAll().stream().map(this::aDto).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioDTO obtener(Long id) {
        return aDto(buscar(id));
    }

    @Transactional
    public UsuarioDTO crear(UsuarioRequestDTO request) {
        Usuario entidad = new Usuario();
        copiar(entidad, request);
        return aDto(repository.save(entidad));
    }

    @Transactional
    public UsuarioDTO actualizar(Long id, UsuarioRequestDTO request) {
        Usuario entidad = buscar(id);
        copiar(entidad, request);
        return aDto(repository.save(entidad));
    }

    @Transactional
    public void eliminar(Long id) {
        repository.delete(buscar(id));
    }

    private Usuario buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", id));
    }

    private void copiar(Usuario entidad, UsuarioRequestDTO request) {
        entidad.setUsuario(request.getUsuario());
        entidad.setClave(hashearClave(request.getClave()));
        entidad.setNombre(request.getNombre());
        entidad.setApellido(request.getApellido());
    }

    private String hashearClave(String clavePlana) {
        if (clavePlana == null || clavePlana.isBlank()) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(clavePlana.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error al hashear la clave del usuario", e);
        }
    }

    private UsuarioDTO aDto(Usuario e) {
        return UsuarioDTO.builder()
                .id(e.getId())
                .usuario(e.getUsuario())
                .nombre(e.getNombre())
                .apellido(e.getApellido())
                .build();
    }
}