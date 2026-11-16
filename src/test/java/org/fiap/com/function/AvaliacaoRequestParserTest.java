package org.fiap.com.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/** Teste unitário puro: não sobe Quarkus, Azure nem banco. */
class AvaliacaoRequestParserTest {

    private final AvaliacaoRequestParser parser = new AvaliacaoRequestParser(new ObjectMapper());

    @Test
    void corpoValidoGeraOsDados() {
        var r = parser.interpretar("{\"descricao\":\"Boa aula\",\"nota\":8}");

        assertTrue(r.valido());
        assertEquals("Boa aula", r.dados().descricao());
        assertEquals(8, r.dados().nota());
    }

    @Test
    void camposDesconhecidosSaoIgnorados() {
        var r = parser.interpretar("{\"descricao\":\"ok\",\"nota\":5,\"extra\":true}");

        assertTrue(r.valido());
    }

    @Test
    void notaComoTextoERejeitada() {
        var r = parser.interpretar("{\"descricao\":\"x\",\"nota\":\"5\"}");

        assertFalse(r.valido());
        assertEquals(List.of("nota deve ser um número inteiro"), r.erros());
    }

    @Test
    void notaDecimalERejeitada() {
        assertFalse(parser.interpretar("{\"descricao\":\"x\",\"nota\":5.5}").valido());
        assertFalse(parser.interpretar("{\"descricao\":\"x\",\"nota\":5.0}").valido());
    }

    @Test
    void notaBooleanaERejeitada() {
        assertFalse(parser.interpretar("{\"descricao\":\"x\",\"nota\":true}").valido());
    }

    @Test
    void notaGrandeDemaisParaIntERejeitada() {
        assertFalse(parser.interpretar("{\"descricao\":\"x\",\"nota\":99999999999}").valido());
    }

    @Test
    void descricaoNumericaERejeitada() {
        var r = parser.interpretar("{\"descricao\":123,\"nota\":5}");

        assertFalse(r.valido());
        assertEquals(List.of("descricao deve ser um texto"), r.erros());
    }

    @Test
    void erroDeTipoEmAmbosOsCamposListaDoisErros() {
        var r = parser.interpretar("{\"descricao\":123,\"nota\":\"5\"}");

        assertEquals(2, r.erros().size());
    }

    @Test
    void camposAusentesNaoSaoErroDeTipo() {
        // A obrigatoriedade é checada depois, pela validação do serviço (retorna 400 também)
        var r = parser.interpretar("{}");

        assertTrue(r.valido());
        assertNull(r.dados().descricao());
        assertNull(r.dados().nota());
    }

    @Test
    void jsonMalformadoERejeitado() {
        var r = parser.interpretar("{descricao: oi");

        assertEquals(List.of("JSON inválido"), r.erros());
    }

    @Test
    void corpoVazioOuNuloERejeitado() {
        assertFalse(parser.interpretar(null).valido());
        assertFalse(parser.interpretar("").valido());
        assertFalse(parser.interpretar("   ").valido());
    }

    @Test
    void corpoQueNaoEObjetoERejeitado() {
        assertFalse(parser.interpretar("[1,2]").valido());
        assertFalse(parser.interpretar("\"texto\"").valido());
        assertFalse(parser.interpretar("null").valido());
    }
}
