package org.fiap.com.repository;

import java.time.Instant;
import java.util.List;

import org.fiap.com.model.Avaliacao;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Acesso a dados de {@link Avaliacao}. Responsabilidade única: falar com o banco.
 * Herda de PanacheRepository os métodos persist, findById, listAll, count etc.
 */
@ApplicationScoped
public class AvaliacaoRepository implements PanacheRepository<Avaliacao> {

    /** Avaliações enviadas a partir de {@code inicio} (inclusive), da mais antiga para a mais nova. */
    public List<Avaliacao> listarDesde(Instant inicio) {
        return list("dataEnvio >= ?1 order by dataEnvio", inicio);
    }
}
