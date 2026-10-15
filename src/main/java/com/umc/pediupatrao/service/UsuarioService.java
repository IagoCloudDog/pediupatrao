package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Role;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Toda a administracao de usuarios/perfis e restrita a ADMIN (RF01),
 * garantida no backend via @PreAuthorize. A role informada e sempre
 * validada para impedir atribuicao indevida de privilegios (autoelevacao).
 */
@Service
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Busca um usuário por ID
    public Optional<Usuario> buscarPorId(String id) {
        return usuarioRepository.findById(id);
    }

    // Busca um usuário por username
    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    // Salva um novo usuário (criptografando a senha e validando o perfil)
    public void salvarUsuario(Usuario usuario) {
        validarRole(usuario.getRole());
        if (usuario.getPassword() == null || usuario.getPassword().isBlank()) {
            throw new IllegalArgumentException("Senha obrigatória para novo usuário.");
        }
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword())); // Criptografa a senha
        usuarioRepository.save(usuario);
    }

    // Deleta um usuário pelo ID
    public void deletarUsuario(String id) {
        usuarioRepository.deleteById(id);
    }

    // Lista todos os usuários
    public Iterable<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    // Atualiza o usuário
    public void atualizarUsuario(String id, Usuario usuarioAtualizado) {
        validarRole(usuarioAtualizado.getRole());
        usuarioRepository.findById(id).ifPresent(usuarioExistente -> {
            usuarioExistente.setUsername(usuarioAtualizado.getUsername());
            usuarioExistente.setRole(usuarioAtualizado.getRole());

            // Só atualiza a senha se uma nova foi informada
            if (usuarioAtualizado.getPassword() != null && !usuarioAtualizado.getPassword().isBlank()) {
                usuarioExistente.setPassword(passwordEncoder.encode(usuarioAtualizado.getPassword()));
            }

            usuarioRepository.save(usuarioExistente);
        });
    }

    // Impede atribuicao de perfis invalidos (ex.: autoelevacao via payload manipulado)
    private void validarRole(String role) {
        if (!Role.isValida(role)) {
            throw new IllegalArgumentException("Perfil inválido. Use ADMIN, GERENTE ou ATENDENTE.");
        }
    }
}
