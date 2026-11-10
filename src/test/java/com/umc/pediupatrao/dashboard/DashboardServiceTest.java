package com.umc.pediupatrao.dashboard;

import com.umc.pediupatrao.dashboard.DashboardDtos.FiltrosDisponiveisDTO;
import com.umc.pediupatrao.dashboard.DashboardDtos.ResumoDTO;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.repository.ProdutoRepository;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

import static com.umc.pediupatrao.dashboard.PedidosTestData.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Verifica a consulta ao MongoDB (filtros -> Query) e a orquestracao do servico. */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private ProdutoRepository produtoRepository;

    private DashboardService service;

    @BeforeEach
    void setUp() {
        service = new DashboardService(mongoTemplate, produtoRepository);
    }

    private Document consultaCapturada(int vezes) {
        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate, times(vezes)).find(captor.capture(), eq(Pedido.class));
        return captor.getAllValues().get(0).getQueryObject();
    }

    @Test
    @DisplayName("Consulta filtra por periodo (dataHoraEntrada >= inicio e <= fim)")
    void consultaPorPeriodo() {
        when(mongoTemplate.find(any(Query.class), eq(Pedido.class))).thenReturn(pedidos());

        service.situacaoPedidos(filtro());

        Document q = consultaCapturada(1);
        Document periodo = (Document) q.get("dataHoraEntrada");
        assertEquals(filtro().inicioDateTime(), periodo.get("$gte"));
        assertEquals(filtro().fimDateTime(), periodo.get("$lte"));
        assertFalse(q.containsKey("status"));
        assertFalse(q.containsKey("itens.produtoId"));
    }

    @Test
    @DisplayName("Consulta inclui status e produto quando informados")
    void consultaComStatusEProduto() {
        when(mongoTemplate.find(any(Query.class), eq(Pedido.class))).thenReturn(List.of());
        FiltroDashboard f = FiltroDashboard.de(INICIO, FIM, "FINALIZADO", "pizzaA", HOJE);

        service.situacaoPedidos(f);

        Document q = consultaCapturada(1);
        assertEquals("FINALIZADO", q.get("status"));
        assertEquals("pizzaA", q.get("itens.produtoId"));
    }

    @Test
    @DisplayName("Resumo consulta o periodo atual e o anterior (2 consultas) e calcula a variacao")
    void resumoConsultaDoisPeriodos() {
        FiltroDashboard f = filtro();
        when(mongoTemplate.find(any(Query.class), eq(Pedido.class))).thenReturn(pedidos(), List.of());

        ResumoDTO r = service.resumo(f);

        verify(mongoTemplate, times(2)).find(any(Query.class), eq(Pedido.class));
        assertEquals(161.0, r.faturamentoLiquido(), 0.001);
        assertNull(r.variacaoFaturamento());   // periodo anterior vazio => sem base
    }

    @Test
    @DisplayName("Limite do ranking fora de 1..50 e rejeitado antes de consultar o banco")
    void limiteInvalido() {
        assertThrows(DadosInvalidosException.class, () -> service.produtosMaisVendidos(filtro(), 0));
        assertThrows(DadosInvalidosException.class, () -> service.produtosMaisVendidos(filtro(), 51));
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Limite do ranking nulo usa o padrao (5)")
    void limitePadrao() {
        when(mongoTemplate.find(any(Query.class), eq(Pedido.class))).thenReturn(pedidos());
        assertEquals(DashboardService.LIMITE_PADRAO, service.produtosMaisVendidos(filtro(), null).limite());
    }

    @Test
    @DisplayName("Filtros disponiveis: status, agrupamentos e produtos ordenados por nome")
    void filtrosDisponiveis() {
        Produto b = produto("2", "Suco");
        Produto a = produto("1", "borda");
        when(produtoRepository.findAll()).thenReturn(List.of(b, a));

        FiltrosDisponiveisDTO f = service.filtrosDisponiveis();

        assertEquals(7, f.status().size());
        assertEquals(List.of("DIA", "SEMANA", "MES"), f.agrupamentos());
        assertEquals("borda", f.produtos().get(0).nome());   // ordenacao case-insensitive
        assertEquals("Suco", f.produtos().get(1).nome());
        assertEquals(30, f.periodoPadraoDias());
    }

    private static Produto produto(String id, String nome) {
        Produto p = new Produto();
        p.setId(id);
        p.setNome(nome);
        return p;
    }
}
