package com.umc.pediupatrao.dashboard;

import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Massa de dados fixa usada nos testes do dashboard.
 *
 * Periodo de referencia: 2026-11-01 (domingo) a 2026-11-09 (segunda-feira).
 *
 * | pedido | entrada              | dia   | status     | itens                       | subtotal | desc | total |
 * |--------|----------------------|-------|------------|-----------------------------|----------|------|-------|
 * | p1     | 2026-11-02 19:10     | seg   | FINALIZADO | 2x Pizza A (40) + 1x Refri  | 90       | 10%  | 81    |
 * | p2     | 2026-11-03 12:30     | ter   | RECEBIDO   | 1x Pizza A (40)             | 40       | 0    | 40    |
 * | p3     | 2026-11-03 20:00     | ter   | CANCELADO  | 3x Pizza B (50)             | 150      | 0    | 150   |
 * | p4     | 2026-11-09 20:45     | seg   | FINALIZADO | 4x Refri (10)               | 40       | 0    | 40    |
 */
final class PedidosTestData {

    static final LocalDate INICIO = LocalDate.of(2026, 11, 1);
    static final LocalDate FIM = LocalDate.of(2026, 11, 9);
    static final LocalDate HOJE = FIM;

    private PedidosTestData() {
    }

    static FiltroDashboard filtro() {
        return FiltroDashboard.de(INICIO, FIM, null, null, HOJE);
    }

    static List<Pedido> pedidos() {
        return List.of(
                pedido(LocalDateTime.of(2026, 11, 2, 19, 10), StatusPedido.FINALIZADO, 10,
                        item("pizzaA", "Pizza A", 2, 40), item("refri", "Refri", 1, 10)),
                pedido(LocalDateTime.of(2026, 11, 3, 12, 30), StatusPedido.RECEBIDO, 0,
                        item("pizzaA", "Pizza A", 1, 40)),
                pedido(LocalDateTime.of(2026, 11, 3, 20, 0), StatusPedido.CANCELADO, 0,
                        item("pizzaB", "Pizza B", 3, 50)),
                pedido(LocalDateTime.of(2026, 11, 9, 20, 45), StatusPedido.FINALIZADO, 0,
                        item("refri", "Refri", 4, 10)));
    }

    static ItemPedido item(String id, String nome, int qtd, double preco) {
        return new ItemPedido(id, nome, qtd, preco);
    }

    static Pedido pedido(LocalDateTime entrada, StatusPedido status, double percentualDesconto, ItemPedido... itens) {
        Pedido p = new Pedido();
        p.setDataHoraEntrada(entrada);
        p.setStatus(status);
        p.setItens(new ArrayList<>(List.of(itens)));
        double subtotal = 0;
        for (ItemPedido i : itens) {
            subtotal += i.getSubtotal();
        }
        p.setSubtotal(subtotal);
        p.setPercentualDesconto(percentualDesconto);
        p.setValorDesconto(subtotal * percentualDesconto / 100.0);
        p.setValorTotal(subtotal - p.getValorDesconto());
        return p;
    }
}
