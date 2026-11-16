package org.fiap.com.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Dados consolidados do relatório semanal.
 *
 * @param inicio       início da janela (00:00 do primeiro dia, horário de Brasília, em UTC)
 * @param fim          fim da janela (momento da geração)
 * @param total        quantidade de avaliações na janela
 * @param media        média das notas, ou {@code null} quando não há avaliações
 * @param porDia       quantidade por dia (data em Brasília), com todos os 7 dias, inclusive os zerados
 * @param porUrgencia  quantidade por urgência, com as 3 urgências, inclusive as zeradas
 * @param avaliacoes   as avaliações da janela, da mais recente para a mais antiga
 */
public record RelatorioSemanal(
        Instant inicio,
        Instant fim,
        long total,
        Double media,
        Map<LocalDate, Long> porDia,
        Map<Urgencia, Long> porUrgencia,
        List<Item> avaliacoes) {

    /** Uma avaliação listada no relatório: descrição, urgência e data de envio. */
    public record Item(String descricao, Urgencia urgencia, Instant dataEnvio) {
    }
}
