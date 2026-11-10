package com.umc.pediupatrao.dashboard;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Tela do dashboard administrativo (Thymeleaf). Os dados vem da API /api/dashboard/**
 * via JavaScript (RF06). Rota protegida para ADMIN e GERENTE no SecurityConfig.
 */
@Controller
public class DashboardPageController {

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("content", "dashboard :: content");
        return "layout";
    }
}
