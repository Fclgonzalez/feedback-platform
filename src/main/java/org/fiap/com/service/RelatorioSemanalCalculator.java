package org.fiap.com.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.fiap.com.model.Avaliacao;
import org.fiap.com.model.RelatorioSemanal;
import org.fiap.com.model.Urgencia;

/**
 * Calcula os números do relatório semanal a partir de uma lista de avaliações.
 *
 * Classe pura (sem banco, sem Azure): recebe os dados e o "agora" como parâmetros,
 * o que torna os testes determinísticos.
 *
 * Decisão de projeto (documentar no README): a janela são os 7 dias de calendário que terminam
 * hoje, no fuso de Brasília. Ex.: executando no sábado 14/11, a janela vai de 08/11 00:00 até
 * o momento da execução. A contagem por dia usa a data de Brasília, não a de UTC.
 */
public final class RelatorioSemanalCalculator {

    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    public static final int DIAS = 7;

    private RelatorioSemanalCalculator() {
    }

    /** Primeiro instante da janela: 00:00 (Brasília) de 6 dias antes de hoje. */
    public static Instant inicioDaJanela(Instant agora) {
        LocalDate primeiroDia = agora.atZone(FUSO).toLocalDate().minusDays(DIAS - 1);
        return primeiroDia.atStartOfDay(FUSO).toInstant();
    }

    public static RelatorioSemanal calcular(List<Avaliacao> avaliacoes, Instant agora) {
        Instant inicio = inicioDaJanela(agora);
        LocalDate primeiroDia = inicio.atZone(FUSO).toLocalDate();

        // Todos os dias e todas as urgências aparecem, mesmo com zero
        Map<LocalDate, Long> porDia = new LinkedHashMap<>();
        for (int i = 0; i < DIAS; i++) {
            porDia.put(primeiroDia.plusDays(i), 0L);
        }
        Map<Urgencia, Long> porUrgencia = new LinkedHashMap<>();
        for (Urgencia urgencia : Urgencia.values()) {
            porUrgencia.put(urgencia, 0L);
        }

        List<Avaliacao> naJanela = avaliacoes.stream()
                .filter(a -> !a.getDataEnvio().isBefore(inicio) && !a.getDataEnvio().isAfter(agora))
                .sorted(Comparator.comparing(Avaliacao::getDataEnvio).reversed())
                .toList();

        long somaDasNotas = 0;
        List<RelatorioSemanal.Item> itens = new ArrayList<>();
        for (Avaliacao a : naJanela) {
            somaDasNotas += a.getNota();
            porDia.merge(a.getDataEnvio().atZone(FUSO).toLocalDate(), 1L, Long::sum);
            porUrgencia.merge(a.getUrgencia(), 1L, Long::sum);
            itens.add(new RelatorioSemanal.Item(a.getDescricao(), a.getUrgencia(), a.getDataEnvio()));
        }

        Double media = naJanela.isEmpty() ? null : (double) somaDasNotas / naJanela.size();
        return new RelatorioSemanal(inicio, agora, naJanela.size(), media, porDia, porUrgencia, itens);
    }
}
