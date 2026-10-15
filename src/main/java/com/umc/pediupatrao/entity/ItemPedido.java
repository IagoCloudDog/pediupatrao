package com.umc.pediupatrao.entity;

/**
 * Item de um pedido (RF02). O preco unitario e o nome sao copiados do produto
 * no momento do registro (snapshot), para que alteracoes posteriores no catalogo
 * nao mudem o historico do pedido. O subtotal e sempre calculado no servidor.
 */
public class ItemPedido {

    private String produtoId;
    private String nomeProduto;
    private int quantidade;
    private double precoUnitario;

    public ItemPedido() {
    }

    public ItemPedido(String produtoId, String nomeProduto, int quantidade, double precoUnitario) {
        this.produtoId = produtoId;
        this.nomeProduto = nomeProduto;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
    }

    /** Subtotal do item (preco unitario x quantidade), calculado no servidor. */
    public double getSubtotal() {
        return precoUnitario * quantidade;
    }

    public String getProdutoId() {
        return produtoId;
    }

    public void setProdutoId(String produtoId) {
        this.produtoId = produtoId;
    }

    public String getNomeProduto() {
        return nomeProduto;
    }

    public void setNomeProduto(String nomeProduto) {
        this.nomeProduto = nomeProduto;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public double getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(double precoUnitario) {
        this.precoUnitario = precoUnitario;
    }
}
