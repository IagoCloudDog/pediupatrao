package com.umc.pediupatrao.dashboard;

import com.umc.pediupatrao.entity.StatusPedido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class FiltroDashboardTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 9);

    @Test
    @DisplayName("Sem parametros: ultimos 30 dias terminando hoje")
    void periodoPadrao() {
        FiltroDashboard f = FiltroDashboard.de(null, null, null, null, HOJE);
        assertEquals(HOJE, f.fim());
        assertEquals(HOJE.minusDays(29), f.inicio());
        assertEquals(30, f.diasNoPeriodo());
        assertNull(f.status());
        assertNull(f.produtoId());
    }

    @Test
    @DisplayName("Somente inicio informado: fim = hoje")
    void somenteInicio() {
        FiltroDashboard f = FiltroDashboard.de(LocalDate.of(2026, 11, 1), null, null, null, HOJE);
        assertEquals(HOJE, f.fim());
        assertEquals(9, f.diasNoPeriodo());
    }

    @Test
    @DisplayName("Inicio posterior ao fim e rejeitado")
    void inicioDepoisDoFim() {
        DadosInvalidosException e = assertThrows(DadosInvalidosException.class,
                () -> FiltroDashboard.de(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 1), null, null, HOJE));
        assertTrue(e.getMessage().contains("posterior"));
    }

    @Test
    @DisplayName("Periodo acima de 366 dias e rejeitado; exatamente 366 e aceito")
    void periodoMaximo() {
        LocalDate fim = HOJE;
        assertDoesNotThrow(() -> FiltroDashboard.de(fim.minusDays(365), fim, null, null, HOJE));
        assertThrows(DadosInvalidosException.class,
                () -> FiltroDashboard.de(fim.minusDays(366), fim, null, null, HOJE));
    }

    @Test
    @DisplayName("Status e normalizado (case-insensitive) e status inexistente e rejeitado")
    void status() {
        assertEquals(StatusPedido.FINALIZADO, FiltroDashboard.de(null, null, " finalizado ", null, HOJE).status());
        assertNull(FiltroDashboard.de(null, null, "  ", null, HOJE).status());
        DadosInvalidosException e = assertThrows(DadosInvalidosException.class,
                () -> FiltroDashboard.de(null, null, "ENTREGUE", null, HOJE));
        assertTrue(e.getMessage().contains("ENTREGUE"));
    }

    @Test
    @DisplayName("Limites do dia: inicio 00:00 e fim 23:59:59.999999999 (ambos inclusivos)")
    void limitesDoDia() {
        FiltroDashboard f = FiltroDashboard.de(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 9), null, null, HOJE);
        assertEquals(LocalTime.MIDNIGHT, f.inicioDateTime().toLocalTime());
        assertEquals(LocalTime.MAX, f.fimDateTime().toLocalTime());
    }

    @Test
    @DisplayName("Periodo anterior tem a mesma duracao e termina na vespera do inicio")
    void periodoAnterior() {
        FiltroDashboard f = FiltroDashboard.de(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 9), "FINALIZADO", "x", HOJE);
        FiltroDashboard anterior = f.periodoAnterior();
        assertEquals(LocalDate.of(2026, 10, 23), anterior.inicio());
        assertEquals(LocalDate.of(2026, 10, 31), anterior.fim());
        assertEquals(f.diasNoPeriodo(), anterior.diasNoPeriodo());
        assertEquals(f.status(), anterior.status());
        assertEquals(f.produtoId(), anterior.produtoId());
    }

    @Test
    @DisplayName("Agrupamento: padrao DIA, case-insensitive e invalido rejeitado")
    void agrupamento() {
        assertEquals(Agrupamento.DIA, Agrupamento.de(null));
        assertEquals(Agrupamento.SEMANA, Agrupamento.de("semana"));
        assertThrows(DadosInvalidosException.class, () -> Agrupamento.de("ANO"));
    }
}
