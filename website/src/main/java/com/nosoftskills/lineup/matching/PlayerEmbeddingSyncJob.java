package com.nosoftskills.lineup.matching;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Catches up players.name_embedding for players missing it -- both freshly created players and
 * any that existed before the embedding layer was added. Runs off the request path entirely, so
 * an admin creating a player never waits on an Ollama round trip; if Ollama is unreachable this
 * simply no-ops and retries on the next tick.
 */
@ApplicationScoped
public class PlayerEmbeddingSyncJob {

    private static final Logger LOG = Logger.getLogger(PlayerEmbeddingSyncJob.class);
    private static final int BATCH_SIZE = 25;

    @Inject
    EntityManager em;

    @Inject
    OllamaEmbeddingClient embeddingClient;

    // SKIP (not the default PROCEED) because a batch's blocking Ollama calls can legitimately
    // outrun the 1-minute tick if Ollama is slow; overlapping runs would otherwise race to read
    // and write the same "missing embedding" rows.
    @Scheduled(every = "1m", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void syncMissingEmbeddings() {
        for (Object[] row : playersMissingEmbeddings()) {
            Long playerId = ((Number) row[0]).longValue();
            String name = (String) row[1];
            embeddingClient.embed(name).ifPresentOrElse(
                    vector -> writeEmbedding(playerId, vector),
                    () -> LOG.debugf("No embedding available for player %d yet; will retry next run", playerId));
        }
    }

    // Its own short-lived transaction, separate from the blocking Ollama calls in the loop above,
    // so a pooled DB connection is never held open across network I/O. Uses QuarkusTransaction
    // directly rather than @Transactional because @Transactional would be a no-op here anyway --
    // it's called via self-invocation (this.playersMissingEmbeddings()) from the same bean, which
    // CDI interceptors don't see.
    @SuppressWarnings("unchecked")
    private List<Object[]> playersMissingEmbeddings() {
        return QuarkusTransaction.requiringNew().call(() -> {
            Query query = em.createNativeQuery(
                    "SELECT id, names FROM players WHERE name_embedding IS NULL LIMIT :batchSize");
            query.setParameter("batchSize", BATCH_SIZE);
            return query.getResultList();
        });
    }

    private void writeEmbedding(Long playerId, double[] vector) {
        QuarkusTransaction.requiringNew().run(() ->
                em.createNativeQuery("UPDATE players SET name_embedding = CAST(:vector AS vector) WHERE id = :id")
                        .setParameter("vector", PgVector.toLiteral(vector))
                        .setParameter("id", playerId)
                        .executeUpdate());
    }
}
