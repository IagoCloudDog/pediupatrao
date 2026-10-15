package com.umc.pediupatrao.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * Estados e transicoes validas do pedido (RF03).
 *
 * Fluxo obrigatorio:
 *   RECEBIDO -> EM_PREPARACAO -> PRONTO -> (SAIU_PARA_ENTREGA | RETIRADO) -> FINALIZADO
 *
 * CANCELADO e excepcional: permitido apenas enquanto o pedido nao saiu para
 * entrega/retirada (ver PedidoService). Depois de FINALIZADO ou CANCELADO
 * nenhuma transicao e permitida.
 */
public enum StatusPedido {
    RECEBIDO,
    EM_PREPARACAO,
    PRONTO,
    SAIU_PARA_ENTREGA,
    RETIRADO,
    FINALIZADO,
    CANCELADO;

    private Set<StatusPedido> proximosPermitidos() {
        return switch (this) {
            case RECEBIDO -> EnumSet.of(EM_PREPARACAO, CANCELADO);
            case EM_PREPARACAO -> EnumSet.of(PRONTO, CANCELADO);
            case PRONTO -> EnumSet.of(SAIU_PARA_ENTREGA, RETIRADO, CANCELADO);
            case SAIU_PARA_ENTREGA, RETIRADO -> EnumSet.of(FINALIZADO);
            case FINALIZADO, CANCELADO -> EnumSet.noneOf(StatusPedido.class);
        };
    }

    /** Indica se a transicao deste status para {@code destino} e valida. */
    public boolean podeTransicionarPara(StatusPedido destino) {
        return destino != null && proximosPermitidos().contains(destino);
    }

    /** Estado final: nao admite mais nenhuma alteracao de status. */
    public boolean isFinal() {
        return this == FINALIZADO || this == CANCELADO;
    }

    /** Indica que o pedido ja saiu (nao pode mais ser cancelado nem receber desconto). */
    public boolean jaSaiu() {
        return this == SAIU_PARA_ENTREGA || this == RETIRADO || this == FINALIZADO;
    }
}
