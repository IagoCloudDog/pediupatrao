package com.umc.pediupatrao.dashboard;

import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.umc.pediupatrao.dashboard.PedidosTestData.filtro;
import static com.umc.pediupatrao.dashboard.PedidosTestData.pedidos;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Camada web + seguranca real (SecurityConfig): perfis, formato dos erros e validacao de parametros.
 * O servico e simulado; a logica de negocio e coberta nos outros testes.
 */
@WebMvcTest(DashboardRestController.class)
@Import(SecurityConfig.class)
class DashboardRestControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private DashboardService service;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;   // exigido pelo SecurityConfig

    private static final String RESUMO = "/api/dashboard/resumo";

    // ------------------------------------------------------------ autorizacao

    @Test
    @WithAnonymousUser
    @DisplayName("Sem autenticacao: 401 em JSON (nao redireciona para login)")
    void anonimo() throws Exception {
        mvc.perform(get(RESUMO))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.caminho").value(RESUMO));
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    @DisplayName("ATENDENTE: 403 em JSON em todos os endpoints do dashboard")
    void atendenteBloqueado() throws Exception {
        for (String url : List.of(RESUMO, "/api/dashboard/vendas/serie", "/api/dashboard/vendas/por-hora",
                "/api/dashboard/vendas/por-dia-semana", "/api/dashboard/pedidos/situacao",
                "/api/dashboard/produtos/mais-vendidos", "/api/dashboard/filtros")) {
            mvc.perform(get(url))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.erro").value("Forbidden"));
        }
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("GERENTE: acesso permitido")
    void gerentePermitido() throws Exception {
        when(service.resumo(any())).thenReturn(DashboardCalculator.resumo(pedidos(), List.of(), filtro()));

        mvc.perform(get(RESUMO).param("inicio", "2026-11-01").param("fim", "2026-11-09"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.faturamentoLiquido").value(161.0))
                .andExpect(jsonPath("$.pedidosValidos").value(3))
                .andExpect(jsonPath("$.pedidosCancelados").value(1))
                .andExpect(jsonPath("$.variacaoFaturamento").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN: acesso permitido")
    void adminPermitido() throws Exception {
        when(service.situacaoPedidos(any())).thenReturn(DashboardCalculator.situacao(pedidos(), filtro()));

        mvc.perform(get("/api/dashboard/pedidos/situacao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$.status.length()").value(7));
    }

    // ------------------------------------------------------------ validacao de parametros (400)

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("Inicio posterior ao fim: 400 com mensagem")
    void periodoInvalido() throws Exception {
        mvc.perform(get(RESUMO).param("inicio", "2026-11-10").param("fim", "2026-11-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("posterior")));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("Data em formato incorreto: 400 indicando o formato esperado")
    void dataMalFormatada() throws Exception {
        mvc.perform(get(RESUMO).param("inicio", "31/12/2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("yyyy-MM-dd")));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("Status inexistente: 400")
    void statusInvalido() throws Exception {
        mvc.perform(get(RESUMO).param("status", "ENTREGUE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("ENTREGUE")));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("Agrupamento inexistente: 400")
    void agrupamentoInvalido() throws Exception {
        mvc.perform(get("/api/dashboard/vendas/serie").param("agrupamento", "ANO"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("Limite nao numerico: 400")
    void limiteNaoNumerico() throws Exception {
        mvc.perform(get("/api/dashboard/produtos/mais-vendidos").param("limite", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("limite")));
    }

    // ------------------------------------------------------------ erro interno (500)

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("Falha inesperada (ex.: banco fora do ar): 500 generico, sem vazar detalhes internos")
    void erroInterno() throws Exception {
        when(service.resumo(any())).thenThrow(new IllegalStateException("mongodb://usuario:senha@host caiu"));

        mvc.perform(get(RESUMO))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("senha"))));
    }
}
