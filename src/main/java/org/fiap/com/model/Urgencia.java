package org.fiap.com.model;

/**
 * Nível de urgência de uma avaliação, derivado da nota atribuída pelo aluno.
 * A regra de classificação está em {@link org.fiap.com.service.UrgenciaClassifier}.
 */
public enum Urgencia {
    CRITICA,
    MEDIA,
    BAIXA
}
