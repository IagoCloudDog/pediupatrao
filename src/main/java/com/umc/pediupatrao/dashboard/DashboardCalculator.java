package com.umc.pediupatrao.dashboard;

import com.umc.pediupatrao.dashboard.DashboardDtos.*;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Regras de agregacao do dashboard, sem acesso a banco (funcoes puras => faceis de testar).
 *
 * Definicoes de negocio adotadas:
 *  - "Venda valida" = pedido com status diferente de CANCELADO; so ela entra em faturamento,
 *    ticket medio, serie, rankings e distribuicoes.
 *  - Faturamento liquido = soma de valorTotal (ja com desconto); bruto = soma de subtotal.
 *  - A data de referencia do pedido e dataHoraEntrada.
 */
public final class DashboardCalculator {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private DashboardCalculator() {
    }

    // ------------------------------------------------------------------ RF01

    public static ResumoDTO resumo(List<Pedido> pedidos, List<Pedido> periodoAnterior, FiltroDashboard filtro) {
        List<Pedido> validos = validos(pedidos);
        List<Pedido> validosAnteriores = validos(periodoAnterior);

        double bruto = soma(validos, Pedido::getSubtotal);
        double descontos = soma(validos, Pedido::getValorDesconto);
        double liquido = soma(validos, Pedido::getValorTotal);
        long cancelados = pedidos.stream().filter(p -> p.getStatus() == StatusPedido.CANCELADO).count();
        long itens = validos.stream().flatMap(p -> itens(p).stream()).mapToLong(ItemPedido::getQuantidade).sum();

        double liquidoAnterior = soma(validosAnteriores, Pedido::getValorTotal);

        return new ResumoDTO(
                periodo(filtro),
                arredondar(bruto),
                arredondar(descontos),
                arredondar(liquido),
                pedidos.size(),
                validos.size(),
                cancelados,
                pedidos.isEmpty() ? 0.0 : arredondar(cancelados * 100.0 / pedidos.size()),
                validos.isEmpty() ? 0.0 : arredondar(liquido / validos.size()),
                itens,
                arredondar(liquidoAnterior),
                variacao(liquido, liquidoAnterior),
                validosAnteriores.size(),
                variacao(validos.size(), validosAnteriores.size()));
    }

    // ------------------------------------------------------------------ RF02

    public static SerieVendasDTO serie(List<Pedido> pedidos, FiltroDashboard filtro, Agrupamento agrupamento) {
        Map<LocalDate, double[]> baldes = new LinkedHashMap<>();
        // Pre-preenche todos os baldes do periodo (inclusive os sem vendas) => grafico continuo.
        for (LocalDate b = agrupamento.inicioDoBalde(filtro.inicio());
             !b.isAfter(filtro.fim());
             b = agrupamento.proximoBalde(b)) {
            baldes.put(b, new double[]{0.0, 0.0});
        }
        for (Pedido p : validos(pedidos)) {
            if (p.getDataHoraEntrada() == null) {
                continue;
            }
            double[] acc = baldes.get(agrupamento.inicioDoBalde(p.getDataHoraEntrada().toLocalDate()));
            if (acc != null) {
                acc[0] += p.getValorTotal();
                acc[1] += 1;
            }
        }
        List<PontoSerieDTO> pontos = new ArrayList<>();
        baldes.forEach((data, acc) -> pontos.add(new PontoSerieDTO(data.toString(), arredondar(acc[0]), (long) acc[1])));
        return new SerieVendasDTO(periodo(filtro), agrupamento.name(), pontos);
    }

    public static DistribuicaoDTO porHora(List<Pedido> pedidos, FiltroDashboard filtro) {
        double[] fat = new double[24];
        long[] qtd = new long[24];
        for (Pedido p : validos(pedidos)) {
            if (p.getDataHoraEntrada() != null) {
                int h = p.getDataHoraEntrada().getHour();
                fat[h] += p.getValorTotal();
                qtd[h]++;
            }
        }
        List<PontoDistribuicaoDTO> pontos = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            pontos.add(new PontoDistribuicaoDTO(h, String.format("%02dh", h), arredondar(fat[h]), qtd[h]));
        }
        return new DistribuicaoDTO(periodo(filtro), "HORA", pontos);
    }

    public static DistribuicaoDTO porDiaSemana(List<Pedido> pedidos, FiltroDashboard filtro) {
        double[] fat = new double[8];
        long[] qtd = new long[8];
        for (Pedido p : validos(pedidos)) {
            if (p.getDataHoraEntrada() != null) {
                int d = p.getDataHoraEntrada().getDayOfWeek().getValue(); // 1=segunda ... 7=domingo
                fat[d] += p.getValorTotal();
                qtd[d]++;
            }
        }
        List<PontoDistribuicaoDTO> pontos = new ArrayList<>();
        for (int d = 1; d <= 7; d++) {
            String rotulo = DayOfWeek.of(d).getDisplayName(TextStyle.SHORT, PT_BR);
            pontos.add(new PontoDistribuicaoDTO(d, rotulo, arredondar(fat[d]), qtd[d]));
        }
        return new DistribuicaoDTO(periodo(filtro), "DIA_SEMANA", pontos);
    }

    // ------------------------------------------------------------------ RF03

    public static SituacaoPedidosDTO situacao(List<Pedido> pedidos, FiltroDashboard filtro) {
        Map<StatusPedido, Long> contagem = new LinkedHashMap<>();
        for (StatusPedido s : StatusPedido.values()) {
            contagem.put(s, 0L);
        }
        for (Pedido p : pedidos) {
            StatusPedido s = (p.getStatus() != null) ? p.getStatus() : StatusPedido.RECEBIDO;
            contagem.merge(s, 1L, Long::sum);
        }
        long total = pedidos.size();
        List<StatusQuantidadeDTO> lista = new ArrayList<>();
        contagem.forEach((s, q) -> lista.add(
                new StatusQuantidadeDTO(s.name(), q, total == 0 ? 0.0 : arredondar(q * 100.0 / total))));
        return new SituacaoPedidosDTO(periodo(filtro), total, lista);
    }

    // ------------------------------------------------------------------ RF04

    public static ProdutosMaisVendidosDTO maisVendidos(List<Pedido> pedidos, FiltroDashboard filtro, int limite) {
        record Acumulador(String id, String nome, long qtd, double receita) {
            Acumulador somar(long q, double r) {
                return new Acumulador(id, nome, qtd + q, receita + r);
            }
        }
        Map<String, Acumulador> porProduto = new LinkedHashMap<>();
        for (Pedido p : validos(pedidos)) {
            for (ItemPedido i : itens(p)) {
                String chave = (i.getProdutoId() != null) ? i.getProdutoId() : String.valueOf(i.getNomeProduto());
                porProduto.merge(chave,
                        new Acumulador(i.getProdutoId(), i.getNomeProduto(), i.getQuantidade(), i.getSubtotal()),
                        (a, b) -> a.somar(b.qtd(), b.receita()));
            }
        }
        long totalQtd = porProduto.values().stream().mapToLong(Acumulador::qtd).sum();
        List<Acumulador> ordenados = porProduto.values().stream()
                .sorted(Comparator.comparingLong(Acumulador::qtd).reversed()
                        .thenComparing(Comparator.comparingDouble(Acumulador::receita).reversed())
                        .thenComparing(a -> String.valueOf(a.nome())))
                .limit(limite)
                .toList();

        List<ProdutoRankingDTO> ranking = new ArrayList<>();
        for (int idx = 0; idx < ordenados.size(); idx++) {
            Acumulador a = ordenados.get(idx);
            ranking.add(new ProdutoRankingDTO(idx + 1, a.id(), a.nome(), a.qtd(), arredondar(a.receita()),
                    totalQtd == 0 ? 0.0 : arredondar(a.qtd() * 100.0 / totalQtd)));
        }
        return new ProdutosMaisVendidosDTO(periodo(filtro), limite, ranking);
    }

    // ------------------------------------------------------------------ utilitarios

    public static PeriodoDTO periodo(FiltroDashboard f) {
        return new PeriodoDTO(f.inicio().toString(), f.fim().toString(), f.diasNoPeriodo());
    }

    static List<Pedido> validos(List<Pedido> pedidos) {
        return pedidos.stream().filter(p -> p.getStatus() != StatusPedido.CANCELADO).toList();
    }

    private static List<ItemPedido> itens(Pedido p) {
        return (p.getItens() != null) ? p.getItens() : List.of();
    }

    private static double soma(List<Pedido> pedidos, java.util.function.ToDoubleFunction<Pedido> f) {
        return pedidos.stream().mapToDouble(f).sum();
    }

    /** Variacao percentual atual x anterior; null quando nao ha base de comparacao (anterior = 0). */
    static Double variacao(double atual, double anterior) {
        if (anterior == 0.0) {
            return null;
        }
        return arredondar((atual - anterior) * 100.0 / anterior);
    }

    static double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
