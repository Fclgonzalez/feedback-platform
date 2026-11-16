package org.fiap.com.service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.azure.communication.email.EmailClient;
import com.azure.communication.email.EmailClientBuilder;
import com.azure.communication.email.models.EmailMessage;
import com.azure.communication.email.models.EmailSendResult;
import com.azure.communication.email.models.EmailSendStatus;
import com.azure.core.util.polling.SyncPoller;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Envia e-mails aos administradores pelo Azure Communication Services (ACS) Email.
 *
 * Responsabilidade única: entregar um {@link EmailConteudo}. Não sabe nada sobre avaliações.
 * Reaproveitado pela Função 2 (urgência) e pela Função 3 (relatório semanal).
 *
 * Configuração (variáveis de ambiente / App Settings):
 * <ul>
 *   <li>ADMIN_EMAILS: destinatários separados por vírgula</li>
 *   <li>EMAIL_SENDER: endereço remetente do domínio do ACS (DoNotReply@xxxx.azurecomm.net)</li>
 *   <li>ACS_CONNECTION_STRING: connection string do ACS (segredo; vem do Key Vault)</li>
 * </ul>
 */
@ApplicationScoped
public class EmailService {

    private static final Duration TEMPO_MAXIMO = Duration.ofSeconds(60);

    @ConfigProperty(name = "app.email.connection-string")
    Optional<String> connectionString;

    @ConfigProperty(name = "app.email.sender")
    Optional<String> remetente;

    @ConfigProperty(name = "app.admin-emails")
    Optional<List<String>> administradores;

    private volatile EmailClient client;

    /**
     * Envia o e-mail para todos os administradores.
     *
     * @throws IllegalStateException se a configuração estiver incompleta ou o envio falhar.
     *                               A exceção é proposital: na Função 2 ela provoca nova tentativa
     *                               e, esgotadas as tentativas, a mensagem vai para a poison queue.
     */
    public void enviarParaAdministradores(EmailConteudo conteudo) {
        List<String> destinatarios = administradores.orElse(List.of()).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        if (destinatarios.isEmpty()) {
            throw new IllegalStateException("Nenhum administrador configurado (ADMIN_EMAILS)");
        }
        String de = remetente.filter(s -> !s.isBlank())
                .orElseThrow(() -> new IllegalStateException("EMAIL_SENDER não configurado"));

        EmailMessage mensagem = new EmailMessage()
                .setSenderAddress(de)
                .setToRecipients(destinatarios.toArray(new String[0]))
                .setSubject(conteudo.assunto())
                .setBodyPlainText(conteudo.texto())
                .setBodyHtml(conteudo.html());

        SyncPoller<EmailSendResult, EmailSendResult> poller = cliente().beginSend(mensagem);
        EmailSendResult resultado = poller.waitForCompletion(TEMPO_MAXIMO).getValue();

        if (resultado.getStatus() != EmailSendStatus.SUCCEEDED) {
            throw new IllegalStateException("Falha no envio do e-mail: status " + resultado.getStatus());
        }
    }

    private EmailClient cliente() {
        EmailClient atual = client;
        if (atual == null) {
            synchronized (this) {
                if (client == null) {
                    String conexao = connectionString.filter(s -> !s.isBlank())
                            .orElseThrow(() -> new IllegalStateException("ACS_CONNECTION_STRING não configurada"));
                    client = new EmailClientBuilder().connectionString(conexao).buildClient();
                }
                atual = client;
            }
        }
        return atual;
    }
}
