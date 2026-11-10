package com.umc.pediupatrao.dashboard;

import com.umc.pediupatrao.dashboard.DashboardDtos.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * API do dashboard administrativo (somente leitura). Base: /api/dashboard.
 *
 * Todos os endpoints de dados aceitam os mesmos filtros opcionais:
 *   inicio (yyyy-MM-dd), fim (yyyy-MM-dd), status (StatusPedido), produtoId.
 * Acesso: ADMIN e GERENTE (SecurityConfig + @PreAuthorize no service).
 * Erros: JSON padronizado (ver {@link DashboardExceptionHandler}).
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardRestController {

    private final DashboardService service;

    public DashboardRestController(DashboardService service) {
        this.service = service;
    }

    /** RF01 - indicadores gerais. */
    @GetMapping("/resumo")
    public ResumoDTO resumo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String produtoId) {
        return service.resumo(FiltroDashboard.de(inicio, fim, status, produtoId));
    }

    /** RF02 - serie temporal de vendas (agrupamento DIA | SEMANA | MES). */
    @GetMapping("/vendas/serie")
    public SerieVendasDTO serie(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String produtoId,
            @RequestParam(required = false) String agrupamento) {
        return service.serieVendas(FiltroDashboard.de(inicio, fim, status, produtoId), Agrupamento.de(agrupamento));
    }

    /** RF02 - vendas por hora do dia (tendencia de horario de pico). */
    @GetMapping("/vendas/por-hora")
    public DistribuicaoDTO porHora(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String produtoId) {
        return service.vendasPorHora(FiltroDashboard.de(inicio, fim, status, produtoId));
    }

    /** RF02 - vendas por dia da semana (tendencia semanal). */
    @GetMapping("/vendas/por-dia-semana")
    public DistribuicaoDTO porDiaSemana(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String produtoId) {
        return service.vendasPorDiaSemana(FiltroDashboard.de(inicio, fim, status, produtoId));
    }

    /** RF03 - situacao dos pedidos (quantidade por status). */
    @GetMapping("/pedidos/situacao")
    public SituacaoPedidosDTO situacao(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String produtoId) {
        return service.situacaoPedidos(FiltroDashboard.de(inicio, fim, status, produtoId));
    }

    /** RF04 - ranking de produtos mais vendidos (limite 1..50, padrao 5). */
    @GetMapping("/produtos/mais-vendidos")
    public ProdutosMaisVendidosDTO maisVendidos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String produtoId,
            @RequestParam(required = false) Integer limite) {
        return service.produtosMaisVendidos(FiltroDashboard.de(inicio, fim, status, produtoId), limite);
    }

    /** RF05 - opcoes disponiveis para os filtros (status, agrupamentos, produtos). */
    @GetMapping("/filtros")
    public FiltrosDisponiveisDTO filtros() {
        return service.filtrosDisponiveis();
    }
}
