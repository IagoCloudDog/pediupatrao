package com.umc.pediupatrao.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Pedido (RF02 + RF03 + RF04).
 *
 * Valores (subtotal, valorDesconto, valorTotal) sao SEMPRE calculados no servidor
 * a partir dos itens e do percentual de desconto autorizado. Requisicoes HTTP nao
 * podem definir arbitrariamente esses valores.
 */
@Document(collection = "pedidos")
public class Pedido {

    @Id
    private String id;

    private String clienteId;

    // Itens do pedido (RF02)
    private List<ItemPedido> itens = new ArrayList<>();

    // Valores calculados no servidor (RF02/RF04)
    private double subtotal;            // soma dos subtotais dos itens
    private double percentualDesconto;  // 0..20 (RF04)
    private double valorDesconto;       // subtotal * percentual / 100
    private double valorTotal;          // subtotal - valorDesconto

    // Fluxo de status (RF03)
    private StatusPedido status = StatusPedido.RECEBIDO;

    // Responsaveis e datas (RF02/RF03)
    private String responsavelEntrada;      // usuario que registrou o pedido
    private LocalDateTime dataHoraEntrada;  // data/hora do registro (RECEBIDO)
    private String responsavelSaida;        // usuario que registrou a saida
    private LocalDateTime dataHoraSaida;    // data/hora da saida (SAIU_PARA_ENTREGA/RETIRADO)

    // Cancelamento (RF04)
    private String justificativaCancelamento;
    private String responsavelCancelamento;
    private LocalDateTime dataHoraCancelamento;

    // Campos legados mantidos para compatibilidade com telas/dados antigos
    private String tipoPizza;
    private String sabor;

    public Pedido() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClienteId() {
        return clienteId;
    }

    public void setClienteId(String clienteId) {
        this.clienteId = clienteId;
    }

    public List<ItemPedido> getItens() {
        return itens;
    }

    public void setItens(List<ItemPedido> itens) {
        this.itens = (itens != null) ? itens : new ArrayList<>();
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public double getPercentualDesconto() {
        return percentualDesconto;
    }

    public void setPercentualDesconto(double percentualDesconto) {
        this.percentualDesconto = percentualDesconto;
    }

    public double getValorDesconto() {
        return valorDesconto;
    }

    public void setValorDesconto(double valorDesconto) {
        this.valorDesconto = valorDesconto;
    }

    public double getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(double valorTotal) {
        this.valorTotal = valorTotal;
    }

    public StatusPedido getStatus() {
        return status;
    }

    public void setStatus(StatusPedido status) {
        this.status = status;
    }

    public String getResponsavelEntrada() {
        return responsavelEntrada;
    }

    public void setResponsavelEntrada(String responsavelEntrada) {
        this.responsavelEntrada = responsavelEntrada;
    }

    public LocalDateTime getDataHoraEntrada() {
        return dataHoraEntrada;
    }

    public void setDataHoraEntrada(LocalDateTime dataHoraEntrada) {
        this.dataHoraEntrada = dataHoraEntrada;
    }

    public String getResponsavelSaida() {
        return responsavelSaida;
    }

    public void setResponsavelSaida(String responsavelSaida) {
        this.responsavelSaida = responsavelSaida;
    }

    public LocalDateTime getDataHoraSaida() {
        return dataHoraSaida;
    }

    public void setDataHoraSaida(LocalDateTime dataHoraSaida) {
        this.dataHoraSaida = dataHoraSaida;
    }

    public String getJustificativaCancelamento() {
        return justificativaCancelamento;
    }

    public void setJustificativaCancelamento(String justificativaCancelamento) {
        this.justificativaCancelamento = justificativaCancelamento;
    }

    public String getResponsavelCancelamento() {
        return responsavelCancelamento;
    }

    public void setResponsavelCancelamento(String responsavelCancelamento) {
        this.responsavelCancelamento = responsavelCancelamento;
    }

    public LocalDateTime getDataHoraCancelamento() {
        return dataHoraCancelamento;
    }

    public void setDataHoraCancelamento(LocalDateTime dataHoraCancelamento) {
        this.dataHoraCancelamento = dataHoraCancelamento;
    }

    public String getTipoPizza() {
        return tipoPizza;
    }

    public void setTipoPizza(String tipoPizza) {
        this.tipoPizza = tipoPizza;
    }

    public String getSabor() {
        return sabor;
    }

    public void setSabor(String sabor) {
        this.sabor = sabor;
    }
}
