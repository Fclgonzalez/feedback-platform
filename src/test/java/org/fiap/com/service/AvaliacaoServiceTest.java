package org.fiap.com.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.fiap.com.model.Avaliacao;
import org.fiap.com.model.NovaAvaliacao;
import org.fiap.com.model.Urgencia;
import org.fiap.com.repository.AvaliacaoRepository;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;

/**
 * Sobe o Quarkus com um PostgreSQL real em container (Dev Services).
 * Precisa do Docker em execução.
 */
@QuarkusTest
class AvaliacaoServiceTest {

    @Inject
    AvaliacaoService service;

    @Inject
    AvaliacaoRepository repository;

    @Test
    void registraAvaliacaoCriticaComIdEDataDeEnvio() {
        Avaliacao salva = service.registrar(new NovaAvaliacao("Aula confusa", 2));

        assertNotNull(salva.getId());
        assertNotNull(salva.getDataEnvio());
        assertEquals(Urgencia.CRITICA, salva.getUrgencia());
    }

    @Test
    void persisteOsDadosNoBanco() {
        Avaliacao salva = service.registrar(new NovaAvaliacao("  Ótima aula  ", 9));

        Avaliacao lida = repository.findById(salva.getId());

        assertNotNull(lida);
        assertEquals("Ótima aula", lida.getDescricao(), "a descrição deve ser gravada sem espaços nas pontas");
        assertEquals(9, lida.getNota());
        assertEquals(Urgencia.BAIXA, lida.getUrgencia());
    }

    @Test
    void rejeitaNotaAcimaDeDez() {
        assertThrows(ConstraintViolationException.class,
                () -> service.registrar(new NovaAvaliacao("texto", 11)));
    }

    @Test
    void rejeitaNotaNegativa() {
        assertThrows(ConstraintViolationException.class,
                () -> service.registrar(new NovaAvaliacao("texto", -1)));
    }

    @Test
    void rejeitaNotaNula() {
        assertThrows(ConstraintViolationException.class,
                () -> service.registrar(new NovaAvaliacao("texto", null)));
    }

    @Test
    void rejeitaDescricaoEmBranco() {
        assertThrows(ConstraintViolationException.class,
                () -> service.registrar(new NovaAvaliacao("   ", 5)));
    }

    @Test
    void rejeitaDescricaoComMaisDeMilCaracteres() {
        String longa = "a".repeat(1001);
        assertThrows(ConstraintViolationException.class,
                () -> service.registrar(new NovaAvaliacao(longa, 5)));
    }
}
