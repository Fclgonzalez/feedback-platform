package org.fiap.com.service;

/**
 * Conteúdo pronto de um e-mail (assunto + corpo em texto e em HTML).
 * Reaproveitado pelo e-mail de urgência e, depois, pelo relatório semanal.
 */
public record EmailConteudo(String assunto, String texto, String html) {
}
