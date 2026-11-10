package com.umc.pediupatrao.dashboard;

import com.umc.pediupatrao.dashboard.DashboardDtos.*;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.ProdutoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Servico do dashboard administrativo (RF01-RF05 da atividade de dashboard).
 *
 * Busca no MongoDB apenas os pedidos que atendem aos filtros (periodo, status, produto)
 * e delega as agregacoes ao {@link DashboardCalculator}. Acesso restrito a ADMIN e GERENTE
 * tambem aqui (alem do SecurityConfig), como defesa em profundidade.
 */
@Service
@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
public class DashboardService {

    public static final int LIMITE_PADRAO = 5;
    public static final int LIMITE_MAXIMO = 50;

    private final MongoTemplate mongoTemplate;
    private final ProdutoRepository produtoRepository;

    public DashboardService(MongoTemplate mongoTemplate, ProdutoRepository produtoRepository) {
        this.mongoTemplate = mongoTemplate;
        this.produtoRepository = produtoRepository;
    }

    public ResumoDTO resumo(FiltroDashboard filtro) {
        List<Pedido> atual = buscarPedidos(filtro);
        List<Pedido> anterior = buscarPedidos(filtro.periodoAnterior());
        return DashboardCalculator.resumo(atual, anterior, filtro);
    }

    public SerieVendasDTO serieVendas(FiltroDashboard filtro, Agrupamento agrupamento) {
        return DashboardCalculator.serie(buscarPedidos(filtro), filtro, agrupamento);
    }

    public DistribuicaoDTO vendasPorHora(FiltroDashboard filtro) {
        return DashboardCalculator.porHora(buscarPedidos(filtro), filtro);
    }

    public DistribuicaoDTO vendasPorDiaSemana(FiltroDashboard filtro) {
        return DashboardCalculator.porDiaSemana(buscarPedidos(filtro), filtro);
    }

    public SituacaoPedidosDTO situacaoPedidos(FiltroDashboard filtro) {
        return DashboardCalculator.situacao(buscarPedidos(filtro), filtro);
    }

    public ProdutosMaisVendidosDTO produtosMaisVendidos(FiltroDashboard filtro, Integer limite) {
        int lim = (limite == null) ? LIMITE_PADRAO : limite;
        if (lim < 1 || lim > LIMITE_MAXIMO) {
            throw new DadosInvalidosException("Limite invalido: " + lim + ". Informe um valor entre 1 e "
                    + LIMITE_MAXIMO + ".");
        }
        return DashboardCalculator.maisVendidos(buscarPedidos(filtro), filtro, lim);
    }

    public FiltrosDisponiveisDTO filtrosDisponiveis() {
        List<OpcaoProdutoDTO> produtos = produtoRepository.findAll().stream()
                .map(p -> new OpcaoProdutoDTO(p.getId(), p.getNome()))
                .sorted(Comparator.comparing(o -> String.valueOf(o.nome()), String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new FiltrosDisponiveisDTO(
                Arrays.stream(StatusPedido.values()).map(Enum::name).toList(),
                Arrays.stream(Agrupamento.values()).map(Enum::name).toList(),
                produtos,
                FiltroDashboard.PERIODO_PADRAO_DIAS,
                FiltroDashboard.PERIODO_MAXIMO_DIAS);
    }

    /** Consulta ao MongoDB: periodo (dataHoraEntrada) + status + produto contido nos itens. */
    List<Pedido> buscarPedidos(FiltroDashboard filtro) {
        Criteria criterio = Criteria.where("dataHoraEntrada")
                .gte(filtro.inicioDateTime())
                .lte(filtro.fimDateTime());
        Query query = new Query(criterio).with(Sort.by(Sort.Direction.ASC, "dataHoraEntrada"));
        if (filtro.status() != null) {
            query.addCriteria(Criteria.where("status").is(filtro.status().name()));
        }
        if (filtro.produtoId() != null) {
            query.addCriteria(Criteria.where("itens.produtoId").is(filtro.produtoId()));
        }
        return mongoTemplate.find(query, Pedido.class);
    }
}
