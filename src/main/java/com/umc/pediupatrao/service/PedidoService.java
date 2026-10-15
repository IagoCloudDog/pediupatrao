package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Auditoria.CampoAlterado;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PedidoService {

    private static final double DESCONTO_MAXIMO = 20.0; // RF04
    private static final String ENTIDADE = "Pedido";

    private final PedidoRepository pedidoRepository;
    private final ProdutoRepository produtoRepository;
    private final AuditoriaService auditoriaService;

    public PedidoService(PedidoRepository pedidoRepository,
                         ProdutoRepository produtoRepository,
                         AuditoriaService auditoriaService) {
        this.pedidoRepository = pedidoRepository;
        this.produtoRepository = produtoRepository;
        this.auditoriaService = auditoriaService;
    }

    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll();
    }

    public Optional<Pedido> buscarPorId(String id) {
        return pedidoRepository.findById(id);
    }

    /**
     * Cria o pedido (RF02). O preco de cada item e obtido do catalogo no servidor
     * (nao do request) e os valores sao recalculados. O status inicial e RECEBIDO,
     * registrando responsavel e data/hora de entrada (RF03).
     */
    public Pedido criarPedido(Pedido pedido) {
        List<ItemPedido> itensNormalizados = normalizarItens(pedido.getItens());

        Pedido novo = new Pedido();
        novo.setClienteId(pedido.getClienteId());
        novo.setItens(itensNormalizados);
        novo.setTipoPizza(pedido.getTipoPizza());
        novo.setSabor(pedido.getSabor());

        // Status inicial e registro de entrada
        novo.setStatus(StatusPedido.RECEBIDO);
        novo.setResponsavelEntrada(usuarioAtual());
        novo.setDataHoraEntrada(LocalDateTime.now());

        // Nenhum desconto na criacao
        novo.setPercentualDesconto(0.0);
        recalcularValores(novo);

        Pedido salvo = pedidoRepository.save(novo);

        // Auditoria da inclusao (RF05)
        List<CampoAlterado> campos = new ArrayList<>();
        campos.add(new CampoAlterado("status", null, salvo.getStatus().name()));
        campos.add(new CampoAlterado("valorTotal", null, String.valueOf(salvo.getValorTotal())));
        campos.add(new CampoAlterado("itens", null, String.valueOf(salvo.getItens().size())));
        auditoriaService.registrarAlteracoes("CRIACAO_PEDIDO", ENTIDADE, salvo.getId(), null, campos);

        return salvo;
    }

    /**
     * Atualiza o status validando as transicoes permitidas (RF03).
     * Registra responsavel/data da saida quando aplicavel. Bloqueia alteracoes
     * em pedidos finalizados/cancelados e transicoes invalidas.
     */
    public Pedido atualizarStatus(String id, StatusPedido novoStatus) {
        Pedido pedido = obrigatorio(id);

        if (pedido.getStatus().isFinal()) {
            throw new IllegalStateException(
                    "Pedido " + pedido.getStatus() + " não admite alteração de status.");
        }
        // Cancelamento tem fluxo proprio (exige justificativa e perfil GERENTE)
        if (novoStatus == StatusPedido.CANCELADO) {
            throw new IllegalStateException(
                    "Use a operação de cancelamento (exige justificativa e perfil GERENTE).");
        }
        if (!pedido.getStatus().podeTransicionarPara(novoStatus)) {
            throw new IllegalStateException(
                    "Transição inválida: " + pedido.getStatus() + " -> " + novoStatus);
        }

        StatusPedido anterior = pedido.getStatus();

        // Registro de saida (RF03)
        if (novoStatus == StatusPedido.SAIU_PARA_ENTREGA || novoStatus == StatusPedido.RETIRADO) {
            pedido.setResponsavelSaida(usuarioAtual());
            pedido.setDataHoraSaida(LocalDateTime.now());
        }

        pedido.setStatus(novoStatus);
        Pedido salvo = pedidoRepository.save(pedido);

        // Auditoria da mudanca de status (RF05)
        List<CampoAlterado> campos = new ArrayList<>();
        campos.add(new CampoAlterado("status", anterior.name(), novoStatus.name()));
        auditoriaService.registrarAlteracoes("ALTERACAO_STATUS", ENTIDADE, salvo.getId(), null, campos);

        return salvo;
    }

    /**
     * Aplica desconto percentual (RF04) - exclusivo do GERENTE.
     * Limitado a 20% e permitido somente antes da saida para entrega/retirada.
     */
    @PreAuthorize("hasRole('GERENTE')")
    public Pedido aplicarDesconto(String id, double percentual) {
        Pedido pedido = obrigatorio(id);

        if (pedido.getStatus().isFinal() || pedido.getStatus().jaSaiu()) {
            throw new IllegalStateException(
                    "Desconto não permitido após a saída/finalização do pedido.");
        }
        if (percentual < 0 || percentual > DESCONTO_MAXIMO) {
            throw new IllegalArgumentException(
                    "Desconto deve estar entre 0% e " + (int) DESCONTO_MAXIMO + "%.");
        }

        double percentualAnterior = pedido.getPercentualDesconto();
        double totalAnterior = pedido.getValorTotal();

        pedido.setPercentualDesconto(percentual);
        recalcularValores(pedido);
        Pedido salvo = pedidoRepository.save(pedido);

        // Auditoria do desconto (RF04/RF05)
        List<CampoAlterado> campos = new ArrayList<>();
        campos.add(new CampoAlterado("percentualDesconto",
                String.valueOf(percentualAnterior), String.valueOf(percentual)));
        campos.add(new CampoAlterado("valorTotal",
                String.valueOf(totalAnterior), String.valueOf(salvo.getValorTotal())));
        auditoriaService.registrarAlteracoes("DESCONTO", ENTIDADE, salvo.getId(), null, campos);

        return salvo;
    }

    /**
     * Cancela o pedido (RF04) - exclusivo do GERENTE. Exige justificativa e so e
     * permitido enquanto o pedido nao tiver saido. Preserva os dados historicos e,
     * uma vez cancelado, nenhuma outra alteracao de status e aceita.
     */
    @PreAuthorize("hasRole('GERENTE')")
    public Pedido cancelarPedido(String id, String justificativa) {
        Pedido pedido = obrigatorio(id);

        if (justificativa == null || justificativa.isBlank()) {
            throw new IllegalArgumentException("Justificativa é obrigatória para o cancelamento.");
        }
        if (pedido.getStatus().isFinal()) {
            throw new IllegalStateException(
                    "Pedido " + pedido.getStatus() + " não pode ser cancelado.");
        }
        if (pedido.getStatus().jaSaiu()) {
            throw new IllegalStateException(
                    "Pedido que já saiu para entrega/retirada não pode ser cancelado.");
        }

        StatusPedido anterior = pedido.getStatus();
        pedido.setStatus(StatusPedido.CANCELADO);
        pedido.setJustificativaCancelamento(justificativa);
        pedido.setResponsavelCancelamento(usuarioAtual());
        pedido.setDataHoraCancelamento(LocalDateTime.now());
        Pedido salvo = pedidoRepository.save(pedido);

        // Auditoria do cancelamento com justificativa (RF04/RF05)
        List<CampoAlterado> campos = new ArrayList<>();
        campos.add(new CampoAlterado("status", anterior.name(), StatusPedido.CANCELADO.name()));
        auditoriaService.registrarAlteracoes(
                "CANCELAMENTO", ENTIDADE, salvo.getId(), justificativa, campos);

        return salvo;
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    /** Normaliza itens buscando nome e preco reais do catalogo (RF02). */
    private List<ItemPedido> normalizarItens(List<ItemPedido> itensRecebidos) {
        List<ItemPedido> resultado = new ArrayList<>();
        if (itensRecebidos == null) {
            return resultado;
        }
        for (ItemPedido item : itensRecebidos) {
            if (item == null || item.getProdutoId() == null || item.getQuantidade() <= 0) {
                continue;
            }
            Produto produto = produtoRepository.findById(item.getProdutoId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Produto não encontrado: " + item.getProdutoId()));
            double preco = (produto.getPreco() != null) ? produto.getPreco() : 0.0;
            resultado.add(new ItemPedido(
                    produto.getId(),
                    produto.getNome(),
                    item.getQuantidade(),
                    preco));
        }
        return resultado;
    }

    /** Recalcula subtotal, valor do desconto e total no servidor (RF02/RF04). */
    private void recalcularValores(Pedido pedido) {
        double subtotal = 0.0;
        for (ItemPedido item : pedido.getItens()) {
            subtotal += item.getSubtotal();
        }
        double percentual = pedido.getPercentualDesconto();
        double valorDesconto = subtotal * (percentual / 100.0);
        pedido.setSubtotal(arredondar(subtotal));
        pedido.setValorDesconto(arredondar(valorDesconto));
        pedido.setValorTotal(arredondar(subtotal - valorDesconto));
    }

    private double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    private Pedido obrigatorio(String id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado: " + id));
    }

    private String usuarioAtual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : "sistema";
    }
}
