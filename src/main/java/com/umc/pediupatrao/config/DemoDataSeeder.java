package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Gera pedidos FICTICIOS dos ultimos 45 dias para o dashboard ter o que exibir.
 *
 * - So roda se {@code pediupatrao.seed.demo-pedidos=true} (padrao no application.properties).
 * - Idempotente: nao faz nada se ja existir algum pedido marcado com responsavelEntrada = "seed-demo".
 * - Deterministico (Random com semente fixa) e grava direto no repositorio (nao passa pela auditoria,
 *   pois nao sao operacoes de usuarios reais). Para desligar: {@code pediupatrao.seed.demo-pedidos=false}.
 */
@Configuration
@ConditionalOnProperty(name = "pediupatrao.seed.demo-pedidos", havingValue = "true")
public class DemoDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    static final String MARCADOR = "seed-demo";
    private static final int DIAS = 45;

    @Bean
    CommandLineRunner seedPedidosDemo(PedidoRepository pedidos, ProdutoRepository produtos,
                                      ClienteRepository clientes) {
        return args -> {
            boolean jaGerado = pedidos.findAll().stream().anyMatch(p -> MARCADOR.equals(p.getResponsavelEntrada()));
            if (jaGerado) {
                return;
            }
            List<Produto> catalogo = garantirCatalogo(produtos);
            List<Cliente> listaClientes = clientes.findAll();

            List<Pedido> gerados = gerar(catalogo, listaClientes, LocalDateTime.now(), new Random(42));
            pedidos.saveAll(gerados);
            log.info("Dados de demonstracao: {} pedidos ficticios gerados para o dashboard.", gerados.size());
        };
    }

    private List<Produto> garantirCatalogo(ProdutoRepository repo) {
        List<Produto> existentes = repo.findAll().stream().filter(p -> p.getPreco() != null).toList();
        if (!existentes.isEmpty()) {
            return existentes;
        }
        List<Produto> novos = new ArrayList<>();
        novos.add(produto("Pizza Calabresa", "Pizza", 45.0));
        novos.add(produto("Pizza Marguerita", "Pizza", 42.0));
        novos.add(produto("Pizza Portuguesa", "Pizza", 48.0));
        novos.add(produto("Refrigerante 2L", "Bebida", 12.0));
        novos.add(produto("Suco Natural", "Bebida", 9.0));
        novos.add(produto("Borda Recheada", "Adicional", 8.0));
        return repo.saveAll(novos);
    }

    private Produto produto(String nome, String tipo, double preco) {
        Produto p = new Produto();
        p.setNome(nome);
        p.setTipo(tipo);
        p.setPreco(preco);
        p.setAtivo(true);
        return p;
    }

    /** Geracao pura (sem I/O) para ser reaproveitavel/testavel. */
    static List<Pedido> gerar(List<Produto> catalogo, List<Cliente> clientes, LocalDateTime agora, Random rnd) {
        List<Pedido> resultado = new ArrayList<>();
        LocalDate hoje = agora.toLocalDate();

        for (int d = DIAS - 1; d >= 0; d--) {
            LocalDate dia = hoje.minusDays(d);
            int dow = dia.getDayOfWeek().getValue(); // 1..7
            int base = (dow >= 5) ? 6 : 3;             // sexta a domingo vendem mais
            int quantidade = base + rnd.nextInt(base);

            for (int n = 0; n < quantidade; n++) {
                LocalDateTime entrada = dia.atTime(sortearHora(rnd), rnd.nextInt(60), rnd.nextInt(60));
                if (entrada.isAfter(agora)) {
                    continue; // nada de pedidos "no futuro" no dia de hoje
                }
                resultado.add(montarPedido(catalogo, clientes, entrada, agora, rnd, d == 0));
            }
        }
        return resultado;
    }

    /** Pico no jantar (18h-22h) e um menor no almoco (11h-13h). */
    private static int sortearHora(Random rnd) {
        int r = rnd.nextInt(100);
        if (r < 65) {
            return 18 + rnd.nextInt(5);   // 18..22
        }
        if (r < 90) {
            return 11 + rnd.nextInt(3);   // 11..13
        }
        return 14 + rnd.nextInt(4);       // 14..17
    }

    private static Pedido montarPedido(List<Produto> catalogo, List<Cliente> clientes, LocalDateTime entrada,
                                       LocalDateTime agora, Random rnd, boolean hoje) {
        Pedido p = new Pedido();
        if (!clientes.isEmpty()) {
            p.setClienteId(clientes.get(rnd.nextInt(clientes.size())).getId());
        }
        p.setResponsavelEntrada(MARCADOR);
        p.setDataHoraEntrada(entrada);

        int nItens = 1 + rnd.nextInt(3);
        List<ItemPedido> itens = new ArrayList<>();
        double subtotal = 0;
        for (int i = 0; i < nItens; i++) {
            // Ponderacao: os primeiros itens do catalogo (pizzas) saem mais.
            Produto prod = catalogo.get(Math.min(rnd.nextInt(catalogo.size()), rnd.nextInt(catalogo.size())));
            int qtd = 1 + rnd.nextInt(3);
            ItemPedido item = new ItemPedido(prod.getId(), prod.getNome(), qtd, prod.getPreco());
            itens.add(item);
            subtotal += item.getSubtotal();
        }
        p.setItens(itens);

        double percentual = (rnd.nextInt(100) < 12) ? new double[]{5, 10, 15, 20}[rnd.nextInt(4)] : 0;
        p.setSubtotal(subtotal);
        p.setPercentualDesconto(percentual);
        p.setValorDesconto(subtotal * percentual / 100.0);
        p.setValorTotal(subtotal - p.getValorDesconto());

        definirStatus(p, entrada, agora, rnd, hoje);
        return p;
    }

    private static void definirStatus(Pedido p, LocalDateTime entrada, LocalDateTime agora, Random rnd,
                                      boolean hoje) {
        int r = rnd.nextInt(100);
        if (r < 7) {
            p.setStatus(StatusPedido.CANCELADO);
            p.setJustificativaCancelamento("Cancelamento de demonstracao");
            p.setResponsavelCancelamento("gerente");
            p.setDataHoraCancelamento(entrada.plusMinutes(10));
            return;
        }
        if (hoje) {
            // Pedidos de hoje ainda em andamento, para o grafico de situacao ficar variado.
            StatusPedido[] andamento = {StatusPedido.RECEBIDO, StatusPedido.EM_PREPARACAO, StatusPedido.PRONTO,
                    StatusPedido.SAIU_PARA_ENTREGA, StatusPedido.RETIRADO, StatusPedido.FINALIZADO};
            StatusPedido s = andamento[rnd.nextInt(andamento.length)];
            p.setStatus(s);
            if (s.jaSaiu()) {
                registrarSaida(p, entrada, agora);
            }
            return;
        }
        p.setStatus(StatusPedido.FINALIZADO);
        registrarSaida(p, entrada, agora);
    }

    private static void registrarSaida(Pedido p, LocalDateTime entrada, LocalDateTime agora) {
        LocalDateTime saida = entrada.plusMinutes(40);
        p.setDataHoraSaida(saida.isAfter(agora) ? agora : saida);
        p.setResponsavelSaida("atendente");
    }
}
