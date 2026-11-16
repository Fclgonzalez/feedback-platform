package org.fiap.com.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.fiap.com.model.AvaliacaoCriticaEvento;
import org.junit.jupiter.api.Test;

/** Teste unitário puro: não sobe Quarkus, Azure nem banco. */
class EmailCriticoComposerTest {

    private static AvaliacaoCriticaEvento evento(String descricao) {
        // 15:04:05 UTC = 12:04:05 em Brasília (UTC-3)
        return new AvaliacaoCriticaEvento(42L, descricao, "CRITICA", "2026-11-14T15:04:05Z");
    }

    @Test
    void emailTrazDescricaoUrgenciaEDataDeEnvio() {
        EmailConteudo email = EmailCriticoComposer.montar(evento("Aula confusa"));

        assertTrue(email.texto().contains("Aula confusa"));
        assertTrue(email.texto().contains("CRITICA"));
        assertTrue(email.texto().contains("14/11/2026 12:04:05"), "data deve estar no horário de Brasília");
        assertTrue(email.html().contains("Aula confusa"));
        assertTrue(email.html().contains("CRITICA"));
        assertTrue(email.html().contains("14/11/2026 12:04:05"));
    }

    @Test
    void assuntoTemIdEUrgenciaMasNaoADescricao() {
        EmailConteudo email = EmailCriticoComposer.montar(evento("Texto secreto do aluno"));

        assertEquals("[Feedback] Avaliação CRITICA recebida (#42)", email.assunto());
        assertFalse(email.assunto().contains("secreto"));
    }

    @Test
    void htmlEscapaConteudoDoAluno() {
        EmailConteudo email = EmailCriticoComposer.montar(evento("<script>alert(1)</script> & \"aspas\""));

        assertFalse(email.html().contains("<script>"), "script não pode aparecer cru no HTML");
        assertTrue(email.html().contains("&lt;script&gt;alert(1)&lt;/script&gt; &amp; &quot;aspas&quot;"));
        // O texto puro não é interpretado como HTML, então mantém o conteúdo original
        assertTrue(email.texto().contains("<script>alert(1)</script>"));
    }

    @Test
    void quebraDeLinhaViraBrNoHtml() {
        EmailConteudo email = EmailCriticoComposer.montar(evento("linha 1\nlinha 2"));

        assertTrue(email.html().contains("linha 1<br>linha 2"));
    }
}
