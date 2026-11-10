package com.umc.pediupatrao.dashboard;

import com.umc.pediupatrao.entity.StatusPedido;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/**
 * Filtros de consulta do dashboard (RF05), validados e normalizados.
 *
 * Regras:
 *  - periodo padrao: ultimos 30 dias (fim = hoje);
 *  - inicio e fim sao inclusivos (fim vai ate 23:59:59.999999999);
 *  - inicio nao pode ser posterior ao fim e o periodo e limitado a 366 dias;
 *  - status e produtoId sao opcionais.
 */
public record FiltroDashboard(LocalDate inicio, LocalDate fim, StatusPedido status, String produtoId) {

    public static final int PERIODO_PADRAO_DIAS = 30;
    public static final int PERIODO_MAXIMO_DIAS = 366;

    public static FiltroDashboard de(LocalDate inicio, LocalDate fim, String status, String produtoId) {
        return de(inicio, fim, status, produtoId, LocalDate.now());
    }

    /** Sobrecarga com "hoje" injetavel, para testes deterministicos. */
    public static FiltroDashboard de(LocalDate inicio, LocalDate fim, String status, String produtoId,
                                     LocalDate hoje) {
        LocalDate fimFinal = (fim != null) ? fim : hoje;
        LocalDate inicioFinal = (inicio != null) ? inicio : fimFinal.minusDays(PERIODO_PADRAO_DIAS - 1L);

        if (inicioFinal.isAfter(fimFinal)) {
            throw new DadosInvalidosException(
                    "Periodo invalido: 'inicio' (" + inicioFinal + ") e posterior a 'fim' (" + fimFinal + ").");
        }
        if (ChronoUnit.DAYS.between(inicioFinal, fimFinal) + 1 > PERIODO_MAXIMO_DIAS) {
            throw new DadosInvalidosException(
                    "Periodo muito longo: o maximo permitido e " + PERIODO_MAXIMO_DIAS + " dias.");
        }

        StatusPedido statusFinal = null;
        if (status != null && !status.isBlank()) {
            try {
                statusFinal = StatusPedido.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new DadosInvalidosException("Status invalido: '" + status
                        + "'. Valores aceitos: RECEBIDO, EM_PREPARACAO, PRONTO, SAIU_PARA_ENTREGA, "
                        + "RETIRADO, FINALIZADO, CANCELADO.");
            }
        }

        String produtoFinal = (produtoId != null && !produtoId.isBlank()) ? produtoId.trim() : null;
        return new FiltroDashboard(inicioFinal, fimFinal, statusFinal, produtoFinal);
    }

    public LocalDateTime inicioDateTime() {
        return inicio.atStartOfDay();
    }

    public LocalDateTime fimDateTime() {
        return fim.atTime(LocalTime.MAX);
    }

    public long diasNoPeriodo() {
        return ChronoUnit.DAYS.between(inicio, fim) + 1;
    }

    /** Periodo imediatamente anterior, com a mesma duracao (base da comparacao do RF01). */
    public FiltroDashboard periodoAnterior() {
        long dias = diasNoPeriodo();
        return new FiltroDashboard(inicio.minusDays(dias), inicio.minusDays(1), status, produtoId);
    }
}
