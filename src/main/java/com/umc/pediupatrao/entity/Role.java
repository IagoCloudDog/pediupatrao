package com.umc.pediupatrao.entity;

/**
 * Perfis de acesso do sistema (RF01).
 * ADMIN     - administra usuarios, atribui perfis, consulta pedidos e auditoria.
 * GERENTE   - gerencia pedidos, aplica descontos, cancela pedidos, consulta clientes e auditoria.
 * ATENDENTE - cadastra/atualiza clientes, registra pedidos e acompanha o andamento.
 */
public enum Role {
    ADMIN,
    GERENTE,
    ATENDENTE;

    public static boolean isValida(String valor) {
        if (valor == null) {
            return false;
        }
        for (Role r : values()) {
            if (r.name().equals(valor)) {
                return true;
            }
        }
        return false;
    }
}
