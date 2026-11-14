package org.fiap.com.service;

import org.fiap.com.model.Urgencia;

/**
 * Regra de negócio que converte a nota em urgência.
 *
 * <ul>
 *   <li>0 a 3  → {@link Urgencia#CRITICA}</li>
 *   <li>4 a 6  → {@link Urgencia#MEDIA}</li>
 *   <li>7 a 10 → {@link Urgencia#BAIXA}</li>
 * </ul>
 *
 * Classe sem dependências de framework, o que a torna trivial de testar.
 * O enunciado do Tech Challenge não define esta regra; ela é uma decisão do grupo
 * e deve ser documentada no README.
 */
public final class UrgenciaClassifier {

    public static final int NOTA_MINIMA = 0;
    public static final int NOTA_MAXIMA = 10;

    private UrgenciaClassifier() {
    }

    public static Urgencia classificar(int nota) {
        if (nota < NOTA_MINIMA || nota > NOTA_MAXIMA) {
            throw new IllegalArgumentException(
                    "A nota deve estar entre " + NOTA_MINIMA + " e " + NOTA_MAXIMA + ", mas foi " + nota);
        }
        if (nota <= 3) {
            return Urgencia.CRITICA;
        }
        if (nota <= 6) {
            return Urgencia.MEDIA;
        }
        return Urgencia.BAIXA;
    }
}
