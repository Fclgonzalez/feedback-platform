package org.fiap.com.function;

/**
 * Nomes usados nas anotações de fila. Precisam ser constantes em tempo de compilação,
 * por isso ficam aqui e são compartilhadas entre a Função 1 (publica) e a Função 2 (consome).
 */
public final class Filas {

    /** Fila de avaliações críticas. A fila de mensagens com falha é criada pelo host: "avaliacoes-criticas-poison". */
    public static final String CRITICAS = "avaliacoes-criticas";

    /** Nome do app setting com a connection string da Storage Account da Function App. */
    public static final String CONEXAO = "AzureWebJobsStorage";

    private Filas() {
    }
}
