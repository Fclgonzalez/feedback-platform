package org.fiap.com.service;

import org.fiap.com.model.Avaliacao;
import org.fiap.com.model.NovaAvaliacao;
import org.fiap.com.repository.AvaliacaoRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

/**
 * Regras de negócio para registrar avaliações.
 * Não conhece HTTP nem filas: quem chama (a função) decide o que fazer com o resultado.
 */
@ApplicationScoped
public class AvaliacaoService {

    @Inject
    AvaliacaoRepository repository;

    /**
     * Valida, classifica a urgência e grava a avaliação.
     *
     * @throws jakarta.validation.ConstraintViolationException se {@code dados} for inválido
     */
    @Transactional
    public Avaliacao registrar(@Valid NovaAvaliacao dados) {
        Avaliacao avaliacao = new Avaliacao();
        avaliacao.setDescricao(dados.descricao().trim());
        avaliacao.setNota(dados.nota());
        avaliacao.setUrgencia(UrgenciaClassifier.classificar(dados.nota()));
        repository.persist(avaliacao);
        return avaliacao;
    }
}
