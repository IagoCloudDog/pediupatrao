package com.umc.pediupatrao.dashboard;

import com.umc.pediupatrao.dashboard.DashboardDtos.*;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static com.umc.pediupatrao.dashboard.PedidosTestData.*;
import static org.junit.jupiter.api.Assertions.*;

/** Valida as regras de agregacao com a massa fixa descrita em {@link PedidosTestData}. */
class DashboardCalculatorTest {

    private static final double DELTA = 0.001;

    // ------------------------------------------------------------ RF01

    @Test
    @DisplayName("Resumo: cancelados ficam fora do faturamento, mas contam no total e na taxa de cancelamento")
    void resumo() {
        ResumoDTO r = DashboardCalculator.resumo(pedidos(), List.of(), filtro());

        assertEquals(170.0, r.faturamentoBruto(), DELTA);
        assertEquals(9.0, r.descontosConcedidos(), DELTA);
        assertEquals(161.0, r.faturamentoLiquido(), DELTA);
        assertEquals(4, r.totalPedidos());
        assertEquals(3, r.pedidosValidos());
        assertEquals(1, r.pedidosCancelados());
        assertEquals(25.0, r.taxaCancelamento(), DELTA);
        assertEquals(53.67, r.ticketMedio(), DELTA);       // 161 / 3
        assertEquals(8, r.itensVendidos());                // 3 + 1 + 4 (cancelado fora)
        assertEquals(9, r.periodo().dias());
    }

    @Test
    @DisplayName("Resumo: variacao em relacao ao periodo anterior")
    void resumoVariacao() {
        Pedido anterior = pedido(LocalDateTime.of(2026, 10, 25, 19, 0), StatusPedido.FINALIZADO, 0,
                item("pizzaA", "Pizza A", 1, 100));
        ResumoDTO r = DashboardCalculator.resumo(pedidos(), List.of(anterior), filtro());

        assertEquals(100.0, r.faturamentoLiquidoPeriodoAnterior(), DELTA);
        assertEquals(61.0, r.variacaoFaturamento(), DELTA);   // (161 - 100) / 100
        assertEquals(1, r.pedidosValidosPeriodoAnterior());
        assertEquals(200.0, r.variacaoPedidos(), DELTA);       // 3 vs 1
    }

    @Test
    @DisplayName("Resumo: sem base de comparacao a variacao e nula (nao divide por zero)")
    void resumoSemBase() {
        ResumoDTO r = DashboardCalculator.resumo(pedidos(), List.of(), filtro());
        assertNull(r.variacaoFaturamento());
        assertNull(r.variacaoPedidos());
    }

    @Test
    @DisplayName("Resumo: periodo vazio retorna zeros (sem NaN nem excecao)")
    void resumoVazio() {
        ResumoDTO r = DashboardCalculator.resumo(List.of(), List.of(), filtro());
        assertEquals(0, r.totalPedidos());
        assertEquals(0.0, r.faturamentoLiquido(), DELTA);
        assertEquals(0.0, r.ticketMedio(), DELTA);
        assertEquals(0.0, r.taxaCancelamento(), DELTA);
    }

    // ------------------------------------------------------------ RF02

    @Test
    @DisplayName("Serie por DIA: um ponto por dia do periodo, com zeros nos dias sem venda")
    void serieDia() {
        SerieVendasDTO s = DashboardCalculator.serie(pedidos(), filtro(), Agrupamento.DIA);

        assertEquals(9, s.pontos().size());
        assertEquals("2026-11-01", s.pontos().get(0).rotulo());
        assertEquals(0.0, s.pontos().get(0).faturamento(), DELTA);
        assertEquals(81.0, s.pontos().get(1).faturamento(), DELTA);   // 02/11
        assertEquals(1, s.pontos().get(1).pedidos());
        assertEquals(40.0, s.pontos().get(2).faturamento(), DELTA);   // 03/11 (cancelado excluido)
        assertEquals(1, s.pontos().get(2).pedidos());
        assertEquals(40.0, s.pontos().get(8).faturamento(), DELTA);   // 09/11
    }

    @Test
    @DisplayName("Serie por SEMANA: baldes iniciam na segunda-feira")
    void serieSemana() {
        SerieVendasDTO s = DashboardCalculator.serie(pedidos(), filtro(), Agrupamento.SEMANA);

        assertEquals(3, s.pontos().size());
        assertEquals("2026-10-26", s.pontos().get(0).rotulo());   // 01/11 e domingo => semana de 26/10
        assertEquals(0, s.pontos().get(0).pedidos());
        assertEquals("2026-11-02", s.pontos().get(1).rotulo());
        assertEquals(121.0, s.pontos().get(1).faturamento(), DELTA);
        assertEquals(2, s.pontos().get(1).pedidos());
        assertEquals("2026-11-09", s.pontos().get(2).rotulo());
        assertEquals(40.0, s.pontos().get(2).faturamento(), DELTA);
    }

    @Test
    @DisplayName("Serie por MES: um unico balde para o periodo")
    void serieMes() {
        SerieVendasDTO s = DashboardCalculator.serie(pedidos(), filtro(), Agrupamento.MES);

        assertEquals(1, s.pontos().size());
        assertEquals("2026-11-01", s.pontos().get(0).rotulo());
        assertEquals(161.0, s.pontos().get(0).faturamento(), DELTA);
        assertEquals(3, s.pontos().get(0).pedidos());
    }

    @Test
    @DisplayName("Vendas por hora: 24 posicoes; horarios dos pedidos validos")
    void porHora() {
        DistribuicaoDTO d = DashboardCalculator.porHora(pedidos(), filtro());

        assertEquals(24, d.pontos().size());
        assertEquals(1, d.pontos().get(12).pedidos());
        assertEquals(40.0, d.pontos().get(12).faturamento(), DELTA);
        assertEquals(1, d.pontos().get(19).pedidos());
        assertEquals(81.0, d.pontos().get(19).faturamento(), DELTA);
        assertEquals(1, d.pontos().get(20).pedidos());              // so o p4; p3 (20:00) foi cancelado
        assertEquals(40.0, d.pontos().get(20).faturamento(), DELTA);
        assertEquals("19h", d.pontos().get(19).rotulo());
    }

    @Test
    @DisplayName("Vendas por dia da semana: 7 posicoes (1=segunda ... 7=domingo)")
    void porDiaSemana() {
        DistribuicaoDTO d = DashboardCalculator.porDiaSemana(pedidos(), filtro());

        assertEquals(7, d.pontos().size());
        assertEquals(1, d.pontos().get(0).chave());
        assertEquals(2, d.pontos().get(0).pedidos());               // segundas: p1 e p4
        assertEquals(121.0, d.pontos().get(0).faturamento(), DELTA);
        assertEquals(1, d.pontos().get(1).pedidos());               // tercas: p2 (p3 cancelado)
        assertEquals(40.0, d.pontos().get(1).faturamento(), DELTA);
        assertEquals(0, d.pontos().get(6).pedidos());               // domingo
    }

    // ------------------------------------------------------------ RF03

    @Test
    @DisplayName("Situacao: todas as 7 situacoes aparecem, com quantidade e percentual")
    void situacao() {
        SituacaoPedidosDTO s = DashboardCalculator.situacao(pedidos(), filtro());

        assertEquals(4, s.total());
        assertEquals(StatusPedido.values().length, s.status().size());
        assertEquals(2, quantidade(s, "FINALIZADO"));
        assertEquals(1, quantidade(s, "RECEBIDO"));
        assertEquals(1, quantidade(s, "CANCELADO"));
        assertEquals(0, quantidade(s, "PRONTO"));
        assertEquals(50.0, s.status().stream().filter(x -> x.status().equals("FINALIZADO")).findFirst().get().percentual(), DELTA);
    }

    @Test
    @DisplayName("Situacao: periodo vazio retorna total 0 e percentuais 0")
    void situacaoVazia() {
        SituacaoPedidosDTO s = DashboardCalculator.situacao(List.of(), filtro());
        assertEquals(0, s.total());
        assertTrue(s.status().stream().allMatch(x -> x.quantidade() == 0 && x.percentual() == 0.0));
    }

    private static long quantidade(SituacaoPedidosDTO s, String status) {
        return s.status().stream().filter(x -> x.status().equals(status)).findFirst().orElseThrow().quantidade();
    }

    // ------------------------------------------------------------ RF04

    @Test
    @DisplayName("Mais vendidos: ordena por quantidade, ignora cancelados e calcula receita e percentual")
    void maisVendidos() {
        ProdutosMaisVendidosDTO r = DashboardCalculator.maisVendidos(pedidos(), filtro(), 5);

        assertEquals(2, r.produtos().size());                       // Pizza B so existe em pedido cancelado
        ProdutoRankingDTO primeiro = r.produtos().get(0);
        assertEquals(1, primeiro.posicao());
        assertEquals("Refri", primeiro.nome());
        assertEquals(5, primeiro.quantidade());                     // 1 (p1) + 4 (p4)
        assertEquals(50.0, primeiro.receita(), DELTA);
        assertEquals(62.5, primeiro.percentualQuantidade(), DELTA); // 5 de 8 unidades

        ProdutoRankingDTO segundo = r.produtos().get(1);
        assertEquals("Pizza A", segundo.nome());
        assertEquals(3, segundo.quantidade());                      // 2 (p1) + 1 (p2)
        assertEquals(120.0, segundo.receita(), DELTA);
        assertEquals(37.5, segundo.percentualQuantidade(), DELTA);
    }

    @Test
    @DisplayName("Mais vendidos: respeita o limite")
    void maisVendidosLimite() {
        ProdutosMaisVendidosDTO r = DashboardCalculator.maisVendidos(pedidos(), filtro(), 1);
        assertEquals(1, r.produtos().size());
        assertEquals("Refri", r.produtos().get(0).nome());
    }

    @Test
    @DisplayName("Mais vendidos: empate de quantidade desempata por receita")
    void maisVendidosEmpate() {
        List<Pedido> lista = List.of(pedido(LocalDateTime.of(2026, 11, 2, 19, 0), StatusPedido.FINALIZADO, 0,
                item("a", "Barato", 2, 10), item("b", "Caro", 2, 30)));
        ProdutosMaisVendidosDTO r = DashboardCalculator.maisVendidos(lista, filtro(), 5);
        assertEquals("Caro", r.produtos().get(0).nome());
        assertEquals("Barato", r.produtos().get(1).nome());
    }

    @Test
    @DisplayName("Mais vendidos: sem vendas retorna lista vazia")
    void maisVendidosVazio() {
        assertTrue(DashboardCalculator.maisVendidos(List.of(), filtro(), 5).produtos().isEmpty());
    }

    // ------------------------------------------------------------ robustez (RF07)

    @Test
    @DisplayName("Pedidos legados (sem itens/data/status) nao quebram os calculos")
    void pedidosLegados() {
        Pedido legado = new Pedido();   // sem data, itens vazios, status padrao RECEBIDO
        legado.setItens(null);
        List<Pedido> lista = List.of(legado);

        assertDoesNotThrow(() -> DashboardCalculator.resumo(lista, List.of(), filtro()));
        assertDoesNotThrow(() -> DashboardCalculator.serie(lista, filtro(), Agrupamento.DIA));
        assertDoesNotThrow(() -> DashboardCalculator.porHora(lista, filtro()));
        assertDoesNotThrow(() -> DashboardCalculator.porDiaSemana(lista, filtro()));
        assertDoesNotThrow(() -> DashboardCalculator.situacao(lista, filtro()));
        assertDoesNotThrow(() -> DashboardCalculator.maisVendidos(lista, filtro(), 5));
    }
}
