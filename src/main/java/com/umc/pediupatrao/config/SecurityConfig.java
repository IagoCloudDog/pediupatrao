package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Role;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Habilita @PreAuthorize nos services/controllers (RF01, RF04, RF05)
public class SecurityConfig {

    private final UsuarioDetailsService usuarioDetailsService;

    public SecurityConfig(UsuarioDetailsService usuarioDetailsService) {
        this.usuarioDetailsService = usuarioDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(usuarioDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF ativo para formularios Thymeleaf. As APIs REST /api/** usam token via cookie
                // e sao chamadas pelo proprio front; o repositorio em cookie permite leitura pelo JS.
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                )
                .authorizeHttpRequests(auth -> auth
                        // Recursos estaticos e login liberados
                        .requestMatchers("/login", "/css/**", "/dist/**", "/plugins/**", "/js/**", "/images/**").permitAll()

                        // RF01 - Administracao de usuarios/perfis: somente ADMIN
                        .requestMatchers("/usuarios/**").hasRole(Role.ADMIN.name())
                        .requestMatchers("/api/usuarios/**").hasRole(Role.ADMIN.name())

                        // RF05 - Consulta de auditoria: somente ADMIN e GERENTE
                        .requestMatchers("/auditoria/**", "/api/auditoria/**")
                            .hasAnyRole(Role.ADMIN.name(), Role.GERENTE.name())

                        // Dashboard administrativo (tela e API): somente ADMIN e GERENTE
                        .requestMatchers("/dashboard/**", "/api/dashboard/**")
                            .hasAnyRole(Role.ADMIN.name(), Role.GERENTE.name())

                        // Demais rotas exigem autenticacao; o controle fino por perfil
                        // (cancelar, descontar, etc.) e feito via @PreAuthorize no backend.
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll()
                        .successHandler(authenticationSuccessHandler())
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                // Tratamento de acesso negado (403) sem expor stacktrace.
                // Para /api/** respondemos JSON (401/403) em vez de redirecionar para paginas HTML,
                // pois quem consome e o JavaScript do dashboard ou um cliente HTTP.
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(apiAuthenticationEntryPoint(), new AntPathRequestMatcher("/api/**"))
                        .accessDeniedHandler(accessDeniedHandler())
                );

        return http.build();
    }

    private AuthenticationEntryPoint apiAuthenticationEntryPoint() {
        return (request, response, authException) -> escreverErroJson(request, response,
                HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized", "Autenticacao necessaria para acessar este recurso.");
    }

    /**
     * 403: em /api/** devolve JSON; nas demais rotas mantem a pagina amigavel /acesso-negado.
     */
    private AccessDeniedHandler accessDeniedHandler() {
        AccessDeniedHandlerImpl paginaAcessoNegado = new AccessDeniedHandlerImpl();
        paginaAcessoNegado.setErrorPage("/acesso-negado");
        return (request, response, accessDeniedException) -> {
            if (request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {
                escreverErroJson(request, response, HttpServletResponse.SC_FORBIDDEN, "Forbidden",
                        "Acesso negado: perfil sem permissao para este recurso.");
            } else {
                paginaAcessoNegado.handle(request, response, accessDeniedException);
            }
        };
    }

    private void escreverErroJson(HttpServletRequest request, HttpServletResponse response,
                                  int status, String erro, String mensagem) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        String caminho = request.getRequestURI().replace("\\", "\\\\").replace("\"", "\\\"");
        response.getWriter().write("{\"timestamp\":\"" + LocalDateTime.now() + "\",\"status\":" + status
                + ",\"erro\":\"" + erro + "\",\"mensagem\":\"" + mensagem + "\",\"caminho\":\"" + caminho + "\"}");
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            response.sendRedirect("/"); // Redireciona para a página inicial após login bem-sucedido
        };
    }
}
