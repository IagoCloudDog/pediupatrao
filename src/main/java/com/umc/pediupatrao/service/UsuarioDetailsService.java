package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Role;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return usuarioRepository.findByUsername(username)
                .map(usuario -> {
                    // Usa a role real persistida no banco (RF01). Caso ausente/invalida,
                    // atribui o perfil de menor privilegio (ATENDENTE) por seguranca.
                    String role = Role.isValida(usuario.getRole())
                            ? usuario.getRole()
                            : Role.ATENDENTE.name();
                    return User.builder()
                            .username(usuario.getUsername())
                            .password(usuario.getPassword())
                            .roles(role) // Spring prefixa automaticamente com ROLE_
                            .build();
                })
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
    }
}
