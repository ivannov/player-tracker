package com.nosoftskills.lineup.model;

import com.nosoftskills.lineup.testsupport.TeamFormationFixtures;
import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class DisambiguationSchemaTest {

    private Long teamId;
    private Long teamFormationId;
    private Long competitionId;
    private Long playerId;

    @BeforeEach
    void setup() {
        QuarkusTransaction.requiringNew().run(() -> {
            TeamFormationFixtures.Ids ids = TeamFormationFixtures.create(
                    "Disambiguation Test Team", "Test City", FormationType.U15, "Disambiguation Test League");
            teamId = ids.teamId();
            teamFormationId = ids.teamFormationId();
            competitionId = ids.competitionId();

            Player p = new Player();
            p.names = "Ivan Ivanov";
            p.persist();
            playerId = p.id;
        });
    }

    @AfterEach
    void cleanup() {
        QuarkusTransaction.requiringNew().run(() -> {
            Player.deleteById(playerId);
            TeamFormationFixtures.delete(new TeamFormationFixtures.Ids(teamId, teamFormationId, competitionId));
        });
    }

    @Test
    void teamAliasPersistsAndReloadsWithEnumSource() {
        Long aliasId = QuarkusTransaction.requiringNew().call(() -> {
            TeamAlias alias = new TeamAlias();
            alias.team = Team.findById(teamId);
            alias.source = ExternalRefSource.BFU_TOURNAMENTS;
            alias.rawName = "bfu-team-123";
            alias.persist();
            return alias.id;
        });

        QuarkusTransaction.requiringNew().run(() -> {
            TeamAlias reloaded = TeamAlias.findById(aliasId);
            assertNotNull(reloaded);
            assertEquals(ExternalRefSource.BFU_TOURNAMENTS, reloaded.source);
            assertEquals("bfu-team-123", reloaded.rawName);
            assertEquals(teamId, reloaded.team.id);

            TeamAlias.deleteById(aliasId);
        });
    }

    @Test
    void playerAliasPersistsAndIsFindableByTrigramSimilarity() {
        Long aliasId = QuarkusTransaction.requiringNew().call(() -> {
            PlayerAlias alias = new PlayerAlias();
            alias.player = Player.findById(playerId);
            alias.source = ExternalRefSource.BFU_TOURNAMENTS;
            alias.rawName = "Ivan Ivanov";
            alias.team = Team.findById(teamId);
            alias.persist();
            return alias.id;
        });

        QuarkusTransaction.requiringNew().run(() -> {
            PlayerAlias reloaded = PlayerAlias.findById(aliasId);
            assertNotNull(reloaded);
            assertEquals("Ivan Ivanov", reloaded.rawName);
            assertEquals(playerId, reloaded.player.id);
            assertEquals(teamId, reloaded.team.id);

            Query q = Panache.getEntityManager().createNativeQuery(
                    "SELECT id FROM player_aliases WHERE raw_name % :name");
            q.setParameter("name", "Ivan Ivanof");
            @SuppressWarnings("unchecked")
            List<Object> matches = q.getResultList();
            assertTrue(matches.stream().anyMatch(id -> ((Number) id).longValue() == aliasId));

            PlayerAlias.deleteById(aliasId);
        });
    }

    @Test
    void ambiguityReviewRoundTripsWithNullableMatchAndResolutionFields() {
        Long reviewId = QuarkusTransaction.requiringNew().call(() -> {
            AmbiguityReview review = new AmbiguityReview();
            review.type = AmbiguityReviewType.PLAYER;
            review.rawName = "I. Ivanov";
            review.team = Team.findById(teamId);
            review.persist();
            return review.id;
        });

        QuarkusTransaction.requiringNew().run(() -> {
            AmbiguityReview reloaded = AmbiguityReview.findById(reviewId);
            assertNotNull(reloaded);
            assertEquals(AmbiguityReviewStatus.PENDING, reloaded.status);
            assertNull(reloaded.match);
            assertNull(reloaded.resolvedPlayer);
            assertNull(reloaded.resolvedAt);
        });

        QuarkusTransaction.requiringNew().run(() -> {
            AmbiguityReview review = AmbiguityReview.findById(reviewId);
            review.status = AmbiguityReviewStatus.RESOLVED;
            review.resolvedPlayer = Player.findById(playerId);
            review.resolvedAt = java.time.LocalDateTime.of(2026, 7, 22, 10, 0);
            review.resolvedBy = "admin";
        });

        QuarkusTransaction.requiringNew().run(() -> {
            AmbiguityReview reloaded = AmbiguityReview.findById(reviewId);
            assertEquals(AmbiguityReviewStatus.RESOLVED, reloaded.status);
            assertEquals(playerId, reloaded.resolvedPlayer.id);
            assertEquals("admin", reloaded.resolvedBy);

            AmbiguityReview.deleteById(reviewId);
        });
    }

    @Test
    void deletingAmbiguityReviewCascadesToItsCandidates() {
        Long reviewId = QuarkusTransaction.requiringNew().call(() -> {
            AmbiguityReview review = new AmbiguityReview();
            review.type = AmbiguityReviewType.PLAYER;
            review.rawName = "I. Ivanov";
            review.team = Team.findById(teamId);
            review.persist();
            return review.id;
        });

        Long candidateId = QuarkusTransaction.requiringNew().call(() -> {
            AmbiguityCandidate candidate = new AmbiguityCandidate();
            candidate.ambiguityReview = AmbiguityReview.findById(reviewId);
            candidate.player = Player.findById(playerId);
            candidate.score = new BigDecimal("0.8765");
            candidate.persist();
            return candidate.id;
        });

        QuarkusTransaction.requiringNew().run(() -> {
            AmbiguityCandidate reloaded = AmbiguityCandidate.findById(candidateId);
            assertNotNull(reloaded);
            assertEquals(0, new BigDecimal("0.8765").compareTo(reloaded.score));
            assertEquals(reviewId, reloaded.ambiguityReview.id);
        });

        QuarkusTransaction.requiringNew().run(() -> AmbiguityReview.deleteById(reviewId));

        QuarkusTransaction.requiringNew().run(() -> {
            long remaining = AmbiguityCandidate.count("id", candidateId);
            assertEquals(0, remaining);
        });
    }
}
