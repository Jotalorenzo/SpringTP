package com.example.trabajo_practico_spring.service;

import com.example.trabajo_practico_spring.model.AuditoriaApp;
import com.example.trabajo_practico_spring.model.Usuario;
import com.example.trabajo_practico_spring.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Completa los campos de auditoria (fechas y usuarios) de las entidades que extienden AuditoriaApp.
 *
 * El proyecto no tiene autenticacion, por lo que se usa el usuario "admin" cargado por el DataSeeder.
 * Cuando se agregue seguridad, solo hay que cambiar usuarioActual().
 */
@Component
@RequiredArgsConstructor
public class AuditoriaService {

    private static final String USUARIO_POR_DEFECTO = "admin";

    private final UsuarioRepository usuarioRepository;

    public void completarAlta(AuditoriaApp entidad) {
        Usuario usuario = usuarioActual();
        LocalDateTime ahora = LocalDateTime.now();
        entidad.setFechaAlta(ahora);
        entidad.setFechaModificacion(ahora);
        entidad.setUsuarioCarga(usuario);
        entidad.setUsuarioModificacion(usuario);
    }

    public void completarModificacion(AuditoriaApp entidad) {
        entidad.setFechaModificacion(LocalDateTime.now());
        entidad.setUsuarioModificacion(usuarioActual());
    }

    private Usuario usuarioActual() {
        return usuarioRepository.findByUsuario(USUARIO_POR_DEFECTO)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe el usuario '" + USUARIO_POR_DEFECTO + "' para completar la auditoria"));
    }
}
