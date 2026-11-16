package org.fiap.com.function;

import java.util.ArrayList;
import java.util.List;

import org.fiap.com.model.NovaAvaliacao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Converte o corpo JSON do POST /avaliacao em {@link NovaAvaliacao}, verificando os TIPOS.
 *
 * Por que não deixar o Jackson fazer o mapeamento direto? Porque ele aceita conversões
 * "generosas" (por exemplo, "nota": "5" vira 5)
 * Aqui: descricao precisa ser texto e nota precisa ser inteiro JSON (5, e não "5", 5.0 ou true).
 *
 * Esta classe só verifica tipos. Regras de valor (nota de 0 a 10, descrição não vazia)
 * ficam na validação de {@link NovaAvaliacao}, executada pelo serviço.
 */
@ApplicationScoped
public class AvaliacaoRequestParser {

    private final ObjectMapper mapper;

    public AvaliacaoRequestParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Resultado da interpretação: ou há dados, ou há erros (nunca os dois).
     */
    public record Resultado(NovaAvaliacao dados, List<String> erros) {
        public boolean valido() {
            return erros.isEmpty();
        }
    }

    public Resultado interpretar(String corpo) {
        if (corpo == null || corpo.isBlank()) {
            return falha("O corpo da requisição é obrigatório");
        }

        JsonNode raiz;
        try {
            raiz = mapper.readTree(corpo);
        } catch (JsonProcessingException e) {
            return falha("JSON inválido");
        }
        if (raiz == null || !raiz.isObject()) {
            return falha("O corpo deve ser um objeto JSON");
        }

        List<String> erros = new ArrayList<>();
        String descricao = null;
        Integer nota = null;

        JsonNode noDescricao = raiz.get("descricao");
        if (noDescricao != null && !noDescricao.isNull()) {
            if (noDescricao.isTextual()) {
                descricao = noDescricao.asText();
            } else {
                erros.add("descricao deve ser um texto");
            }
        }

        JsonNode noNota = raiz.get("nota");
        if (noNota != null && !noNota.isNull()) {
            if (noNota.isIntegralNumber() && noNota.canConvertToInt()) {
                nota = noNota.intValue();
            } else {
                erros.add("nota deve ser um número inteiro");
            }
        }

        if (!erros.isEmpty()) {
            return new Resultado(null, erros);
        }
        // Campos ausentes chegam como null e são rejeitados pela validação do serviço
        return new Resultado(new NovaAvaliacao(descricao, nota), List.of());
    }

    private static Resultado falha(String mensagem) {
        return new Resultado(null, List.of(mensagem));
    }
}
