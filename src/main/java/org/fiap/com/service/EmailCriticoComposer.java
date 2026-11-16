package org.fiap.com.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.fiap.com.model.AvaliacaoCriticaEvento;

/**
 * Monta o e-mail de aviso de urgência com: descrição, urgência e data de envio.
 *
 * Classe pura (sem Quarkus, sem Azure), por isso fácil de testar.
 *
 * Duas decisões de segurança:
 * <ul>
 *   <li>A descrição vem do aluno, então é escapada no HTML (evita injeção de HTML/script no e-mail).</li>
 *   <li>O assunto NÃO contém a descrição (só id e urgência), evitando conteúdo livre em cabeçalho.</li>
 * </ul>
 * A data é guardada em UTC e exibida no horário de Brasília.
 */
public final class EmailCriticoComposer {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter
            .ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.of("America/Sao_Paulo"));

    private EmailCriticoComposer() {
    }

    public static EmailConteudo montar(AvaliacaoCriticaEvento evento) {
        String data = FORMATO_DATA.format(Instant.parse(evento.dataEnvio()));

        String assunto = "[Feedback] Avaliação " + evento.urgencia() + " recebida (#" + evento.id() + ")";

        String texto = "Uma nova avaliação de urgência " + evento.urgencia() + " foi recebida.\n\n"
                + "Descrição: " + evento.descricao() + "\n"
                + "Urgência: " + evento.urgencia() + "\n"
                + "Data de envio: " + data + " (horário de Brasília)\n"
                + "Identificador: " + evento.id() + "\n";

        String html = "<html><body style=\"font-family:Arial,sans-serif\">"
                + "<h2>Avaliação " + escapar(evento.urgencia()) + " recebida</h2>"
                + "<table cellpadding=\"6\">"
                + "<tr><td><b>Descrição</b></td><td>" + escapar(evento.descricao()).replace("\n", "<br>") + "</td></tr>"
                + "<tr><td><b>Urgência</b></td><td>" + escapar(evento.urgencia()) + "</td></tr>"
                + "<tr><td><b>Data de envio</b></td><td>" + data + " (horário de Brasília)</td></tr>"
                + "<tr><td><b>Identificador</b></td><td>" + evento.id() + "</td></tr>"
                + "</table></body></html>";

        return new EmailConteudo(assunto, texto, html);
    }

    /** Escapa os caracteres especiais do HTML. */
    static String escapar(String valor) {
        if (valor == null) {
            return "";
        }
        return valor
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
