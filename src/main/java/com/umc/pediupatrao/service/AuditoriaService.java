package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Auditoria;
import com.umc.pediupatrao.entity.Auditoria.CampoAlterado;
import com.umc.pediupatrao.repository.AuditoriaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Servico de auditoria (RF05).
 *
 * O registro (registrar/registrarAlteracoes) e usado internamente pelos demais
 * services; a consulta (listarTodas/listarPorEntidade) e restrita a ADMIN e
 * GERENTE via @PreAuthorize. Nao ha metodos de alteracao/exclusao: os registros
 * sao imutaveis. Senhas/tokens nunca sao gravados aqui.
 */
@Service
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaService(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    /** Registra um evento simples (sem lista de campos), opcionalmente com justificativa. */
    public void registrar(String operacao, String entidade, String entidadeId, String justificativa) {
        registrarAlteracoes(operacao, entidade, entidadeId, justificativa, new ArrayList<>());
    }

    /** Registra um evento com os campos alterados (valor anterior/novo). */
    public void registrarAlteracoes(String operacao, String entidade, String entidadeId,
                                     String justificativa, List<CampoAlterado> alteracoes) {
        Auditoria auditoria = new Auditoria();
        auditoria.setUsuario(usuarioAtual());
        auditoria.setPerfil(perfilAtual());
        auditoria.setDataHora(LocalDateTime.now());
        auditoria.setOperacao(operacao);
        auditoria.setEntidade(entidade);
        auditoria.setEntidadeId(entidadeId);
        auditoria.setJustificativa(justificativa);
        auditoria.setAlteracoes(alteracoes != null ? alteracoes : new ArrayList<>());
        auditoriaRepository.save(auditoria);
    }

    /** Consulta de todos os registros - restrita a ADMIN e GERENTE (RF05). */
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public List<Auditoria> listarTodas() {
        return auditoriaRepository.findAll(Sort.by(Sort.Direction.DESC, "dataHora"));
    }

    /** Consulta por entidade/id - restrita a ADMIN e GERENTE (RF05). */
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public List<Auditoria> listarPorEntidade(String entidade, String entidadeId) {
        return auditoriaRepository.findByEntidadeAndEntidadeId(
                entidade, entidadeId, Sort.by(Sort.Direction.DESC, "dataHora"));
    }

    // ----------------------------------------------------------------

    private String usuarioAtual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : "sistema";
    }

    private String perfilAtual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return "DESCONHECIDO";
        }
        for (GrantedAuthority authority : auth.getAuthorities()) {
            String role = authority.getAuthority();
            if (role != null && role.startsWith("ROLE_")) {
                return role.substring("ROLE_".length());
            }
        }
        return "DESCONHECIDO";
    }
}
