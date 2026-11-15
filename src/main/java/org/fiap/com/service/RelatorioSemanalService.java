package org.fiap.com.service;

import java.time.Instant;
import java.util.List;

import org.fiap.com.model.Avaliacao;
import org.fiap.com.model.RelatorioSemanal;
import org.fiap.com.repository.AvaliacaoRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Busca as avaliações da janela e entrega o relatório calculado.
 * Responsabilidade única: juntar o banco com o cálculo. Não monta e-mail.
 *
 * Observação: as agregações são feitas em memória, o que é adequado para o volume deste desafio.
 * Para volumes grandes, o ideal seria fazer COUNT/AVG/GROUP BY direto no banco.
 */
@ApplicationScoped
public class RelatorioSemanalService {

    @Inject
    AvaliacaoRepository repository;

    @Transactional
    public RelatorioSemanal gerar(Instant agora) {
        List<Avaliacao> avaliacoes = repository.listarDesde(RelatorioSemanalCalculator.inicioDaJanela(agora));
        return RelatorioSemanalCalculator.calcular(avaliacoes, agora);
    }
}
