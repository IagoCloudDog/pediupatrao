package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.entity.Auditoria;
import com.umc.pediupatrao.service.AuditoriaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Tela de consulta de auditoria (RF05). A rota /auditoria e protegida no
 * SecurityConfig (ADMIN e GERENTE) e o service reforça a restricao via
 * @PreAuthorize. Nao existem rotas de edicao/exclusao de auditoria.
 */
@Controller
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping("/auditoria")
    public String auditoria(Model model) {
        List<Auditoria> registros = auditoriaService.listarTodas();
        model.addAttribute("auditorias", registros);
        model.addAttribute("content", "auditoria :: content");
        return "layout";
    }
}
