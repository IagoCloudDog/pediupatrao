package com.umc.pediupatrao.dashboard;

/**
 * Parametro de consulta invalido na API do dashboard (periodo incoerente, status
 * inexistente, limite fora da faixa...). Convertida em HTTP 400 pelo
 * {@link DashboardExceptionHandler}.
 */
public class DadosInvalidosException extends RuntimeException {

    public DadosInvalidosException(String mensagem) {
        super(mensagem);
    }
}
