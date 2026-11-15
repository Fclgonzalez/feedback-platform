package org.fiap.com.service;

import static org.fiap.com.service.EmailCriticoComposer.escapar;
import static org.fiap.com.service.RelatorioSemanalCalculator.FUSO;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.fiap.com.model.RelatorioSemanal;
import org.fiap.com.model.Urgencia;

/**
 * Monta o e-mail do relatório semanal: média, quantidade por dia, quantidade por urgência
 * e a lista de avaliações (descrição, urgência e data de envio).
 *
 * Classe pura, fácil de testar. O texto do aluno é sempre escapado no HTML.
 * A lista de avaliações é limitada a {@link #MAX_ITENS} para o e-mail não crescer sem controle;
 * os totais e a média sempre consideram TODAS as avaliações da janela.
 */
public final class RelatorioSemanalComposer {

    static final int MAX_ITENS = 100;

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DIA_CURTO = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(FUSO);

    private RelatorioSemanalComposer() {
    }

    public static EmailConteudo montar(RelatorioSemanal r) {
        LocalDate primeiro = r.inicio().atZone(FUSO).toLocalDate();
        LocalDate ultimo = r.fim().atZone(FUSO).toLocalDate();
        String media = r.media() == null ? "sem avaliações" : String.format(PT_BR, "%.2f", r.media());

        String assunto = "[Feedback] Relatório semanal " + DIA_CURTO.format(primeiro) + " a "
                + DIA_CURTO.format(ultimo) + ": "
                + (r.total() == 0
                        ? "nenhuma avaliação"
                        : r.total() + (r.total() == 1 ? " avaliação" : " avaliações") + ", média " + media);

        List<RelatorioSemanal.Item> itens = r.avaliacoes().stream().limit(MAX_ITENS).toList();
        String aviso = r.total() > MAX_ITENS
                ? "Mostrando as " + MAX_ITENS + " mais recentes de " + r.total() + "."
                : "";

        return new EmailConteudo(assunto, montarTexto(r, primeiro, ultimo, media, itens, aviso),
                montarHtml(r, primeiro, ultimo, media, itens, aviso));
    }

    private static String montarTexto(RelatorioSemanal r, LocalDate primeiro, LocalDate ultimo,
            String media, List<RelatorioSemanal.Item> itens, String aviso) {
        StringBuilder t = new StringBuilder();
        t.append("Relatório semanal de feedbacks\n");
        t.append("Período: ").append(DIA.format(primeiro)).append(" a ").append(DIA.format(ultimo))
                .append(" (horário de Brasília)\n\n");

        t.append("Resumo\n");
        t.append("- Total de avaliações: ").append(r.total()).append("\n");
        t.append("- Média das notas: ").append(media).append("\n\n");

        t.append("Avaliações por dia\n");
        for (Map.Entry<LocalDate, Long> e : r.porDia().entrySet()) {
            t.append("- ").append(DIA.format(e.getKey())).append(": ").append(e.getValue()).append("\n");
        }

        t.append("\nAvaliações por urgência\n");
        for (Map.Entry<Urgencia, Long> e : r.porUrgencia().entrySet()) {
            t.append("- ").append(e.getKey()).append(": ").append(e.getValue()).append("\n");
        }

        t.append("\nAvaliações da semana (mais recentes primeiro)\n");
        if (itens.isEmpty()) {
            t.append("Nenhuma avaliação recebida no período.\n");
        } else {
            for (RelatorioSemanal.Item i : itens) {
                t.append("- ").append(DATA_HORA.format(i.dataEnvio())).append(" | ").append(i.urgencia())
                        .append(" | ").append(i.descricao()).append("\n");
            }
            if (!aviso.isEmpty()) {
                t.append(aviso).append("\n");
            }
        }
        return t.toString();
    }

    private static String montarHtml(RelatorioSemanal r, LocalDate primeiro, LocalDate ultimo,
            String media, List<RelatorioSemanal.Item> itens, String aviso) {
        StringBuilder h = new StringBuilder();
        h.append("<html><body style=\"font-family:Arial,sans-serif\">");
        h.append("<h2>Relatório semanal de feedbacks</h2>");
        h.append("<p>Período: ").append(DIA.format(primeiro)).append(" a ").append(DIA.format(ultimo))
                .append(" (horário de Brasília)</p>");

        h.append("<h3>Resumo</h3><ul>");
        h.append("<li>Total de avaliações: <b>").append(r.total()).append("</b></li>");
        h.append("<li>Média das notas: <b>").append(escapar(media)).append("</b></li></ul>");

        h.append("<h3>Avaliações por dia</h3><table border=\"1\" cellpadding=\"6\" cellspacing=\"0\">");
        h.append("<tr><th>Dia</th><th>Quantidade</th></tr>");
        for (Map.Entry<LocalDate, Long> e : r.porDia().entrySet()) {
            h.append("<tr><td>").append(DIA.format(e.getKey())).append("</td><td>")
                    .append(e.getValue()).append("</td></tr>");
        }
        h.append("</table>");

        h.append("<h3>Avaliações por urgência</h3><table border=\"1\" cellpadding=\"6\" cellspacing=\"0\">");
        h.append("<tr><th>Urgência</th><th>Quantidade</th></tr>");
        for (Map.Entry<Urgencia, Long> e : r.porUrgencia().entrySet()) {
            h.append("<tr><td>").append(e.getKey()).append("</td><td>")
                    .append(e.getValue()).append("</td></tr>");
        }
        h.append("</table>");

        h.append("<h3>Avaliações da semana (mais recentes primeiro)</h3>");
        if (itens.isEmpty()) {
            h.append("<p>Nenhuma avaliação recebida no período.</p>");
        } else {
            h.append("<table border=\"1\" cellpadding=\"6\" cellspacing=\"0\">");
            h.append("<tr><th>Data de envio</th><th>Urgência</th><th>Descrição</th></tr>");
            for (RelatorioSemanal.Item i : itens) {
                h.append("<tr><td>").append(DATA_HORA.format(i.dataEnvio())).append("</td><td>")
                        .append(i.urgencia()).append("</td><td>")
                        .append(escapar(i.descricao()).replace("\n", "<br>")).append("</td></tr>");
            }
            h.append("</table>");
            if (!aviso.isEmpty()) {
                h.append("<p><i>").append(aviso).append("</i></p>");
            }
        }
        h.append("</body></html>");
        return h.toString();
    }
}
