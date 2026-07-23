package com.nosoftskills.lineup.matching;

import com.nosoftskills.lineup.model.Player;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class PlayerEmbeddingSyncJobTest {

    @Inject
    PlayerEmbeddingSyncJob syncJob;

    @Inject
    EntityManager em;

    @InjectMock
    OllamaEmbeddingClient embeddingClient;

    private final List<Long> playerIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        QuarkusTransaction.requiringNew().run(() -> {
            for (Long id : playerIds) {
                Player.deleteById(id);
            }
            playerIds.clear();
        });
    }

    private Long createPlayer(String name) {
        return QuarkusTransaction.requiringNew().call(() -> {
            Player p = new Player();
            p.names = name;
            p.persist();
            playerIds.add(p.id);
            return p.id;
        });
    }

    private boolean hasEmbedding(Long playerId) {
        Object result = em.createNativeQuery("SELECT name_embedding IS NOT NULL FROM players WHERE id = :id")
                .setParameter("id", playerId)
                .getSingleResult();
        return (Boolean) result;
    }

    @Test
    void syncPopulatesEmbeddingForPlayerMissingOne() {
        Long playerId = createPlayer("Georgi Georgiev");
        Mockito.when(embeddingClient.embed("Georgi Georgiev"))
                .thenReturn(Optional.of(TestVectors.embedding(0.1, 0.2, 0.3)));

        syncJob.syncMissingEmbeddings();

        assertTrue(hasEmbedding(playerId));
    }

    @Test
    void syncLeavesEmbeddingNullWhenOllamaUnavailable() {
        Long playerId = createPlayer("Petar Petrov");
        Mockito.when(embeddingClient.embed("Petar Petrov")).thenReturn(Optional.empty());

        syncJob.syncMissingEmbeddings();

        assertFalse(hasEmbedding(playerId));
    }

    @Test
    void syncSkipsPlayersThatAlreadyHaveAnEmbedding() {
        Long playerId = createPlayer("Ivan Ivanov");
        QuarkusTransaction.requiringNew().run(() ->
                em.createNativeQuery("UPDATE players SET name_embedding = CAST(:v AS vector) WHERE id = :id")
                        .setParameter("v", PgVector.toLiteral(TestVectors.embedding(1, 0, 0)))
                        .setParameter("id", playerId)
                        .executeUpdate());

        syncJob.syncMissingEmbeddings();

        Mockito.verifyNoInteractions(embeddingClient);
        assertTrue(hasEmbedding(playerId));
    }
}
