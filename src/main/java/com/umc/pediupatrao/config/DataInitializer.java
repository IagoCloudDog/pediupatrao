package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Role;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(UsuarioRepository repo, PasswordEncoder encoder) {
        return args -> {
            criarSeNaoExistir(repo, encoder, "admin", "teste123", Role.ADMIN);
            criarSeNaoExistir(repo, encoder, "gerente", "teste123", Role.GERENTE);
            criarSeNaoExistir(repo, encoder, "atendente", "teste123", Role.ATENDENTE);

            // Corrige usuarios antigos sem role definida, atribuindo o menor privilegio.
            repo.findAll().forEach(u -> {
                if (!Role.isValida(u.getRole())) {
                    u.setRole(Role.ATENDENTE.name());
                    repo.save(u);
                }
            });
        };
    }

    private void criarSeNaoExistir(UsuarioRepository repo, PasswordEncoder encoder,
                                   String username, String senha, Role role) {
        if (repo.findByUsername(username).isEmpty()) {
            Usuario user = new Usuario();
            user.setUsername(username);
            user.setPassword(encoder.encode(senha));
            user.setRole(role.name());
            repo.save(user);
            System.out.println("Usuário " + username + " (" + role + ") criado!");
        }
    }
}
