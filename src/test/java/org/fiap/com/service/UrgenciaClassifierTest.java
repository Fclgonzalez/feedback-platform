package org.fiap.com.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.fiap.com.model.Urgencia;
import org.junit.jupiter.api.Test;

/** Teste unitário puro: não sobe o Quarkus nem o banco, roda em milissegundos. */
class UrgenciaClassifierTest {

    @Test
    void notasDeZeroATresSaoCriticas() {
        for (int nota = 0; nota <= 3; nota++) {
            assertEquals(Urgencia.CRITICA, UrgenciaClassifier.classificar(nota), "nota " + nota);
        }
    }

    @Test
    void notasDeQuatroASeisSaoMedias() {
        for (int nota = 4; nota <= 6; nota++) {
            assertEquals(Urgencia.MEDIA, UrgenciaClassifier.classificar(nota), "nota " + nota);
        }
    }

    @Test
    void notasDeSeteADezSaoBaixas() {
        for (int nota = 7; nota <= 10; nota++) {
            assertEquals(Urgencia.BAIXA, UrgenciaClassifier.classificar(nota), "nota " + nota);
        }
    }

    @Test
    void notaAbaixoDoMinimoLancaExcecao() {
        assertThrows(IllegalArgumentException.class, () -> UrgenciaClassifier.classificar(-1));
    }

    @Test
    void notaAcimaDoMaximoLancaExcecao() {
        assertThrows(IllegalArgumentException.class, () -> UrgenciaClassifier.classificar(11));
    }
}
