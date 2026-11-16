package org.fiap.com.function;

import java.time.Instant;
import java.util.logging.Logger;

import org.fiap.com.model.RelatorioSemanal;
import org.fiap.com.service.EmailService;
import org.fiap.com.service.RelatorioSemanalComposer;
import org.fiap.com.service.RelatorioSemanalService;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;

import jakarta.inject.Inject;

/**
 * Função 3: gera e envia o relatório semanal de feedbacks.
 *
 * Gatilho: timer. O agendamento vem do app setting RELATORIO_CRON em UTC.
 *
 * Para a demonstração, a função pode ser disparada manualmente pelo endpoint administrativo:
 * POST /admin/functions/GerarRelatorioSemanal (veja o README). Não é preciso uma função extra.
 *
 * Responsabilidade única: orquestrar "gerar relatório" + "enviar e-mail". Não recebe HTTP
 * e não tem regra de cálculo (está em {@link RelatorioSemanalService}).
 */
public class GerarRelatorioSemanalFunction {

    @Inject
    RelatorioSemanalService relatorioService;

    @Inject
    EmailService emailService;

    @FunctionName("GerarRelatorioSemanal")
    public void run(
            @TimerTrigger(name = "timer", schedule = "%RELATORIO_CRON%")
            String timerInfo,
            final ExecutionContext context) {

        Logger log = context.getLogger();

        RelatorioSemanal relatorio = relatorioService.gerar(Instant.now());
        emailService.enviarParaAdministradores(RelatorioSemanalComposer.montar(relatorio));

        // Só contagens no log; nunca o conteúdo das descrições
        log.info("Relatório semanal enviado: total=" + relatorio.total());
    }
}
