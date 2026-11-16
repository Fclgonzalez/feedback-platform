package org.fiap.com.model;

/**
 * Mensagem publicada na fila "avaliacoes-criticas" pela Função 1 e consumida pela Função 2.
 *
 * Carrega tudo o que o e-mail precisa (descrição, urgência e data de envio), de modo que a
 * Função 2 não depende do banco de dados: as duas funções só se conhecem por este contrato.
 *
 * @param id         identificador da avaliação
 * @param descricao  texto escrito pelo aluno
 * @param urgencia   nome do enum {@link Urgencia}
 * @param dataEnvio  instante de envio em UTC, formato ISO-8601 (ex.: 2026-11-14T15:04:05Z)
 */
public record AvaliacaoCriticaEvento(Long id, String descricao, String urgencia, String dataEnvio) {

    public static AvaliacaoCriticaEvento de(Avaliacao avaliacao) {
        return new AvaliacaoCriticaEvento(
                avaliacao.getId(),
                avaliacao.getDescricao(),
                avaliacao.getUrgencia().name(),
                avaliacao.getDataEnvio().toString());
    }
}
