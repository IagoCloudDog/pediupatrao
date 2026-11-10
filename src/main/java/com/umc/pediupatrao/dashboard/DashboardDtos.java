package com.umc.pediupatrao.dashboard;

import java.util.List;

/**
 * Contrato JSON da API do dashboard. Valores monetarios em reais, com 2 casas.
 */
public final class DashboardDtos {

    private DashboardDtos() {
    }

    /** Periodo efetivamente consultado (ja com os padroes aplicados). */
    public record PeriodoDTO(String inicio, String fim, long dias) {
    }

    /** RF01 - indicadores gerais. {@code variacao*} compara com o periodo anterior (null se sem base). */
    public record ResumoDTO(
            PeriodoDTO periodo,
            double faturamentoBruto,
            double descontosConcedidos,
            double faturamentoLiquido,
            long totalPedidos,
            long pedidosValidos,
            long pedidosCancelados,
            double taxaCancelamento,
            double ticketMedio,
            long itensVendidos,
            double faturamentoLiquidoPeriodoAnterior,
            Double variacaoFaturamento,
            long pedidosValidosPeriodoAnterior,
            Double variacaoPedidos) {
    }

    /** RF02 - ponto da serie temporal (rotulo = data ISO do inicio do dia/semana/mes). */
    public record PontoSerieDTO(String rotulo, double faturamento, long pedidos) {
    }

    public record SerieVendasDTO(PeriodoDTO periodo, String agrupamento, List<PontoSerieDTO> pontos) {
    }

    /** RF02 - vendas por hora do dia (0-23) ou por dia da semana (1=segunda ... 7=domingo). */
    public record PontoDistribuicaoDTO(int chave, String rotulo, double faturamento, long pedidos) {
    }

    public record DistribuicaoDTO(PeriodoDTO periodo, String tipo, List<PontoDistribuicaoDTO> pontos) {
    }

    /** RF03 - quantidade de pedidos por status. */
    public record StatusQuantidadeDTO(String status, long quantidade, double percentual) {
    }

    public record SituacaoPedidosDTO(PeriodoDTO periodo, long total, List<StatusQuantidadeDTO> status) {
    }

    /** RF04 - ranking de produtos. Receita = soma dos subtotais dos itens (antes do desconto do pedido). */
    public record ProdutoRankingDTO(int posicao, String produtoId, String nome, long quantidade,
                                    double receita, double percentualQuantidade) {
    }

    public record ProdutosMaisVendidosDTO(PeriodoDTO periodo, int limite, List<ProdutoRankingDTO> produtos) {
    }

    /** Opcoes para montar os filtros no front (RF05). */
    public record OpcaoProdutoDTO(String id, String nome) {
    }

    public record FiltrosDisponiveisDTO(List<String> status, List<String> agrupamentos,
                                        List<OpcaoProdutoDTO> produtos, int periodoPadraoDias,
                                        int periodoMaximoDias) {
    }

    /** Corpo padrao de erro da API do dashboard. */
    public record ErroDTO(String timestamp, int status, String erro, String mensagem, String caminho) {
    }
}
