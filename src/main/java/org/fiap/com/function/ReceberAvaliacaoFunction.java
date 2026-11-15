package org.fiap.com.function;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.fiap.com.model.Avaliacao;
import org.fiap.com.model.AvaliacaoCriticaEvento;
import org.fiap.com.model.Urgencia;
import org.fiap.com.service.AvaliacaoService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.azure.functions.annotation.QueueOutput;

import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

/**
 * Função 1: recebe uma avaliação via HTTP.
 *
 * <pre>
 * POST /api/avaliacao
 * { "descricao": "texto", "nota": 0..10 }
 * </pre>
 *
 * Respostas:
 * <ul>
 *   <li>201: avaliação gravada ({@link AvaliacaoRegistrada})</li>
 *   <li>400: JSON inválido, tipo errado ou valor fora das regras ({@link ErroResposta})</li>
 *   <li>500: falha inesperada</li>
 * </ul>
 *
 * Responsabilidade única: traduzir HTTP para o serviço e de volta.
 * Regras de negócio estão em {@link AvaliacaoService}; a checagem de tipos do JSON está
 * em {@link AvaliacaoRequestParser}.
 *
 * Se a urgência for CRITICA, a avaliação também é publicada na fila "avaliacoes-criticas"
 * (consumida pela Função 2, que envia o e-mail). Esta função não envia e-mail.
 *
 * Limitação conhecida (documentar no README): o binding de saída é gravado pelo host DEPOIS
 * que a função retorna. Se a gravação na fila falhar, a avaliação já está salva no banco, mas o
 * cliente pode receber erro. Uma solução completa exigiria o padrão "transactional outbox".
 */
public class ReceberAvaliacaoFunction {

    @Inject
    AvaliacaoRequestParser parser;

    @Inject
    AvaliacaoService service;

    @Inject
    ObjectMapper mapper;

    /** Corpo da resposta 201. */
    public record AvaliacaoRegistrada(Long id, String urgencia, String dataEnvio) {
    }

    /** Corpo das respostas de erro. */
    public record ErroResposta(String erro, List<String> detalhes) {
    }

    @FunctionName("ReceberAvaliacao")
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = "req",
                    methods = { HttpMethod.POST },
                    authLevel = AuthorizationLevel.FUNCTION,
                    route = "avaliacao")
            HttpRequestMessage<Optional<String>> request,
            @QueueOutput(
                    name = "filaCriticas",
                    queueName = Filas.CRITICAS,
                    connection = Filas.CONEXAO)
            OutputBinding<String> filaCriticas,
            final ExecutionContext context) {

        Logger log = context.getLogger();

        AvaliacaoRequestParser.Resultado resultado = parser.interpretar(request.getBody().orElse(null));
        if (!resultado.valido()) {
            return resposta(request, HttpStatus.BAD_REQUEST,
                    new ErroResposta("Requisição inválida", resultado.erros()));
        }

        try {
            Avaliacao salva = service.registrar(resultado.dados());
            // Nunca logar a descrição: pode conter dados pessoais do aluno
            log.info("Avaliação registrada: id=" + salva.getId() + ", urgencia=" + salva.getUrgencia());
            publicarSeCritica(salva, filaCriticas, log);
            return resposta(request, HttpStatus.CREATED, new AvaliacaoRegistrada(
                    salva.getId(), salva.getUrgencia().name(), salva.getDataEnvio().toString()));
        } catch (ConstraintViolationException e) {
            List<String> detalhes = e.getConstraintViolations().stream()
                    .map(ConstraintViolation::getMessage)
                    .sorted()
                    .toList();
            return resposta(request, HttpStatus.BAD_REQUEST, new ErroResposta("Requisição inválida", detalhes));
        } catch (Exception e) {
            log.log(Level.SEVERE, "Erro inesperado ao registrar avaliação", e);
            return resposta(request, HttpStatus.INTERNAL_SERVER_ERROR,
                    new ErroResposta("Erro interno ao registrar a avaliação", List.of()));
        }
    }

    /** Coloca a avaliação na fila somente se for crítica. Falha de serialização não derruba a resposta. */
    private void publicarSeCritica(Avaliacao avaliacao, OutputBinding<String> fila, Logger log) {
        if (avaliacao.getUrgencia() != Urgencia.CRITICA) {
            return;
        }
        try {
            fila.setValue(mapper.writeValueAsString(AvaliacaoCriticaEvento.de(avaliacao)));
        } catch (JsonProcessingException e) {
            // A avaliação já está salva; registramos o erro para o alerta de monitoramento
            log.log(Level.SEVERE, "Falha ao montar a mensagem da fila para a avaliação id=" + avaliacao.getId(), e);
        }
    }

    private HttpResponseMessage resposta(HttpRequestMessage<?> request, HttpStatus status, Object corpo) {
        try {
            return request.createResponseBuilder(status)
                    .header("Content-Type", "application/json")
                    .body(mapper.writeValueAsString(corpo))
                    .build();
        } catch (JsonProcessingException e) {
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("Content-Type", "application/json")
                    .body("{\"erro\":\"Erro interno\"}")
                    .build();
        }
    }
}
