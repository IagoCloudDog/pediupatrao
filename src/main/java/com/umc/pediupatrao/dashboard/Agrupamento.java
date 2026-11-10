package com.umc.pediupatrao.dashboard;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/** Granularidade da serie temporal de vendas (RF02). */
public enum Agrupamento {
    DIA,
    SEMANA,
    MES;

    /** Primeiro dia do "balde" (dia, segunda-feira da semana ou dia 1 do mes) que contem a data. */
    public LocalDate inicioDoBalde(LocalDate data) {
        return switch (this) {
            case DIA -> data;
            case SEMANA -> data.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MES -> data.withDayOfMonth(1);
        };
    }

    /** Inicio do balde seguinte. */
    public LocalDate proximoBalde(LocalDate inicioDoBalde) {
        return switch (this) {
            case DIA -> inicioDoBalde.plusDays(1);
            case SEMANA -> inicioDoBalde.plusWeeks(1);
            case MES -> inicioDoBalde.plusMonths(1);
        };
    }

    /** Converte o texto recebido na URL (case-insensitive); nulo = DIA. */
    public static Agrupamento de(String valor) {
        if (valor == null || valor.isBlank()) {
            return DIA;
        }
        try {
            return valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DadosInvalidosException(
                    "Agrupamento invalido: '" + valor + "'. Valores aceitos: DIA, SEMANA, MES.");
        }
    }
}
