package org.fiap.com.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.fiap.com.model.Avaliacao;
import org.fiap.com.model.RelatorioSemanal;
import org.fiap.com.model.Urgencia;
import org.junit.jupiter.api.Test;

/**
 * Teste unitário puro. O "agora" é fixo: sábado 14/11/2026 às 15:00 UTC = 12:00 em Brasília.
 * Janela esperada: de 08/11/2026 00:00 (Brasília) = 2026-11-08T03:00:00Z até o "agora".
 */
class RelatorioSemanalCalculatorTest {

    static final Instant AGORA = Instant.parse("2026-11-14T15:00:00Z");

    static Avaliacao avaliacao(String instante, int nota, Urgencia urgencia, String descricao) {
        Avaliacao a = new Avaliacao();
        a.setDataEnvio(Instant.parse(instante));
        a.setNota(nota);
        a.setUrgencia(urgencia);
        a.setDescricao(descricao);
        return a;
    }

    /** Quatro avaliações dentro da janela e duas fora. Notas dentro: 2, 9, 5, 7 (média 5,75). */
    static List<Avaliacao> amostra() {
        return List.of(
                avaliacao("2026-11-14T14:00:00Z", 2, Urgencia.CRITICA, "dia 14 de manhã"),
                // 02:30 UTC de 14/11 ainda é 23:30 de 13/11 em Brasília
                avaliacao("2026-11-14T02:30:00Z", 9, Urgencia.BAIXA, "dia 13 à noite em Brasília"),
                avaliacao("2026-11-13T12:00:00Z", 5, Urgencia.MEDIA, "dia 13 ao meio-dia"),
                // exatamente no início da janela: entra
                avaliacao("2026-11-08T03:00:00Z", 7, Urgencia.BAIXA, "primeiro instante da janela"),
                // um segundo antes do início da janela: fica de fora
                avaliacao("2026-11-08T02:59:59Z", 0, Urgencia.CRITICA, "fora, antes da janela"),
                // depois do "agora": fica de fora
                avaliacao("2026-11-14T15:00:01Z", 1, Urgencia.CRITICA, "fora, no futuro"));
    }

    @Test
    void janelaComecaMeiaNoiteDeBrasiliaSeisDiasAntesDeHoje() {
        assertEquals(Instant.parse("2026-11-08T03:00:00Z"), RelatorioSemanalCalculator.inicioDaJanela(AGORA));
    }

    @Test
    void semAvaliacoesTudoZeradoEMediaNula() {
        RelatorioSemanal r = RelatorioSemanalCalculator.calcular(List.of(), AGORA);

        assertEquals(0, r.total());
        assertNull(r.media());
        assertEquals(7, r.porDia().size(), "os 7 dias aparecem mesmo zerados");
        assertEquals(3, r.porUrgencia().size(), "as 3 urgências aparecem mesmo zeradas");
        assertEquals(0L, r.porDia().get(LocalDate.of(2026, 11, 10)));
        assertEquals(0L, r.porUrgencia().get(Urgencia.CRITICA));
    }

    @Test
    void contaSomenteAvaliacoesDentroDaJanela() {
        RelatorioSemanal r = RelatorioSemanalCalculator.calcular(amostra(), AGORA);

        assertEquals(4, r.total());
        assertEquals(4, r.avaliacoes().size());
    }

    @Test
    void calculaAMediaDasNotas() {
        RelatorioSemanal r = RelatorioSemanalCalculator.calcular(amostra(), AGORA);

        assertEquals(5.75, r.media(), 0.0001);
    }

    @Test
    void agrupaPorDiaNoFusoDeBrasilia() {
        RelatorioSemanal r = RelatorioSemanalCalculator.calcular(amostra(), AGORA);

        assertEquals(1L, r.porDia().get(LocalDate.of(2026, 11, 8)));
        assertEquals(0L, r.porDia().get(LocalDate.of(2026, 11, 9)));
        assertEquals(2L, r.porDia().get(LocalDate.of(2026, 11, 13)), "inclui a das 23:30 de Brasília");
        assertEquals(1L, r.porDia().get(LocalDate.of(2026, 11, 14)));
    }

    @Test
    void agrupaPorUrgencia() {
        RelatorioSemanal r = RelatorioSemanalCalculator.calcular(amostra(), AGORA);

        assertEquals(1L, r.porUrgencia().get(Urgencia.CRITICA));
        assertEquals(1L, r.porUrgencia().get(Urgencia.MEDIA));
        assertEquals(2L, r.porUrgencia().get(Urgencia.BAIXA));
    }

    @Test
    void listaAsMaisRecentesPrimeiro() {
        RelatorioSemanal r = RelatorioSemanalCalculator.calcular(amostra(), AGORA);

        assertEquals("dia 14 de manhã", r.avaliacoes().get(0).descricao());
        assertEquals("primeiro instante da janela", r.avaliacoes().get(3).descricao());
    }
}
