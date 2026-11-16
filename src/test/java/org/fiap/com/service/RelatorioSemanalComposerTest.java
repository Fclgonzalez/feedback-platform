package org.fiap.com.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.fiap.com.service.RelatorioSemanalCalculatorTest.AGORA;
import static org.fiap.com.service.RelatorioSemanalCalculatorTest.amostra;
import static org.fiap.com.service.RelatorioSemanalCalculatorTest.avaliacao;

import java.util.ArrayList;
import java.util.List;

import org.fiap.com.model.Avaliacao;
import org.fiap.com.model.RelatorioSemanal;
import org.fiap.com.model.Urgencia;
import org.junit.jupiter.api.Test;

/** Teste unitário puro, reaproveitando a amostra e o "agora" fixo do teste do cálculo. */
class RelatorioSemanalComposerTest {

    @Test
    void assuntoResumeOPeriodoOTotalEAMedia() {
        RelatorioSemanal r = RelatorioSemanalCalculator.calcular(amostra(), AGORA);

        EmailConteudo email = RelatorioSemanalComposer.montar(r);

        assertEquals("[Feedback] Relatório semanal 08/11 a 14/11: 4 avaliações, média 5,75", email.assunto());
    }

    @Test
    void textoTrazMediaQuantidadePorDiaEPorUrgencia() {
        RelatorioSemanal r = RelatorioSemanalCalculator.calcular(amostra(), AGORA);

        String texto = RelatorioSemanalComposer.montar(r).texto();

        assertTrue(texto.contains("Total de avaliações: 4"));
        assertTrue(texto.contains("Média das notas: 5,75"));
        assertTrue(texto.contains("- 08/11/2026: 1"));
        assertTrue(texto.contains("- 09/11/2026: 0"), "dia sem avaliações também aparece");
        assertTrue(texto.contains("- 13/11/2026: 2"));
        assertTrue(texto.contains("- CRITICA: 1"));
        assertTrue(texto.contains("- BAIXA: 2"));
    }

    @Test
    void listaDescricaoUrgenciaEDataDeEnvioNoHorarioDeBrasilia() {
        RelatorioSemanal r = RelatorioSemanalCalculator.calcular(amostra(), AGORA);

        String texto = RelatorioSemanalComposer.montar(r).texto();

        // 14:00 UTC = 11:00 em Brasília
        assertTrue(texto.contains("14/11/2026 11:00:00 | CRITICA | dia 14 de manhã"));
    }

    @Test
    void htmlEscapaOTextoDoAluno() {
        List<Avaliacao> lista = List.of(
                avaliacao("2026-11-14T14:00:00Z", 1, Urgencia.CRITICA, "<script>alert(1)</script> & cia"));
        RelatorioSemanal r = RelatorioSemanalCalculator.calcular(lista, AGORA);

        String html = RelatorioSemanalComposer.montar(r).html();

        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt; &amp; cia"));
    }

    @Test
    void semAvaliacoesDizQueNaoHouve() {
        RelatorioSemanal r = RelatorioSemanalCalculator.calcular(List.of(), AGORA);

        EmailConteudo email = RelatorioSemanalComposer.montar(r);

        assertEquals("[Feedback] Relatório semanal 08/11 a 14/11: nenhuma avaliação", email.assunto());
        assertTrue(email.texto().contains("Nenhuma avaliação recebida no período."));
        assertTrue(email.html().contains("Nenhuma avaliação recebida no período."));
    }

    @Test
    void umaUnicaAvaliacaoUsaOSingular() {
        List<Avaliacao> lista = List.of(avaliacao("2026-11-14T14:00:00Z", 8, Urgencia.BAIXA, "ok"));
        RelatorioSemanal r = RelatorioSemanalCalculator.calcular(lista, AGORA);

        assertEquals("[Feedback] Relatório semanal 08/11 a 14/11: 1 avaliação, média 8,00",
                RelatorioSemanalComposer.montar(r).assunto());
    }

    @Test
    void listaELimitadaMasOsTotaisContamTodas() {
        List<Avaliacao> muitas = new ArrayList<>();
        for (int i = 0; i < 120; i++) {
            muitas.add(avaliacao("2026-11-14T14:00:00Z", 10, Urgencia.BAIXA, "item " + i));
        }
        RelatorioSemanal r = RelatorioSemanalCalculator.calcular(muitas, AGORA);

        String texto = RelatorioSemanalComposer.montar(r).texto();

        assertEquals(120, r.total());
        assertTrue(texto.contains("Total de avaliações: 120"));
        assertTrue(texto.contains("Mostrando as 100 mais recentes de 120."));
        long linhasListadas = texto.lines().filter(l -> l.contains(" | BAIXA | ")).count();
        assertEquals(100, linhasListadas);
    }
}
