package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.entity.Auditoria;
import com.umc.pediupatrao.service.AuditoriaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API de leitura de auditoria (RF05). Somente consulta - nao ha POST/PUT/DELETE.
 * Protegida por /api/auditoria/** (ADMIN e GERENTE) no SecurityConfig e por
 * @PreAuthorize no service.
 */
@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaRestController {

    private final AuditoriaService auditoriaService;

    public AuditoriaRestController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public List<Auditoria> listar(@RequestParam(required = false) String entidade,
                                  @RequestParam(required = false) String entidadeId) {
        if (entidade != null && entidadeId != null) {
            return auditoriaService.listarPorEntidade(entidade, entidadeId);
        }
        return auditoriaService.listarTodas();
    }
}
