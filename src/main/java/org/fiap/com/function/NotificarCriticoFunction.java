package org.fiap.com.function;

import java.util.logging.Logger;

import org.fiap.com.model.AvaliacaoCriticaEvento;
import org.fiap.com.service.EmailCriticoComposer;
import org.fiap.com.service.EmailService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.QueueTrigger;

import jakarta.inject.Inject;

/**
 * Função 2: envia o e-mail de aviso quando chega uma avaliação crítica na fila.
 *
 * Gatilho: mensagem na fila "avaliacoes-criticas" (publicada pela Função 1).
 *
 * Tratamento de falhas: qualquer exceção faz o host tentar de novo. Depois de 5 tentativas
 * (padrão do Azure Functions) a mensagem vai para a fila "avaliacoes-criticas-poison",
 * que funciona como DLQ. Assim, uma falha no e-mail nunca perde a notificação em silêncio
 * e também nunca desfaz a gravação da avaliação (que já foi concluída pela Função 1).
 *
 * Responsabilidade única: notificar. Não acessa banco nem recebe HTTP.
 */
public class NotificarCriticoFunction {

    @Inject
    ObjectMapper mapper;

    @Inject
    EmailService emailService;

    @FunctionName("NotificarCritico")
    public void run(
            @QueueTrigger(
                    name = "mensagem",
                    queueName = Filas.CRITICAS,
                    connection = Filas.CONEXAO)
            String mensagem,
            final ExecutionContext context) {

        Logger log = context.getLogger();

        AvaliacaoCriticaEvento evento;
        try {
            evento = mapper.readValue(mensagem, AvaliacaoCriticaEvento.class);
        } catch (JsonProcessingException e) {
            // Mensagem ilegível: não adianta tentar de novo, mas deixamos o host levá-la à poison queue
            throw new IllegalArgumentException("Mensagem da fila avaliacoes-criticas inválida", e);
        }

        emailService.enviarParaAdministradores(EmailCriticoComposer.montar(evento));

        // Nunca logar a descrição: pode conter dados pessoais do aluno
        log.info("E-mail de avaliação crítica enviado: id=" + evento.id());
    }
}
