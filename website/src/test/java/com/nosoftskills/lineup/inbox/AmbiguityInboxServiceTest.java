package com.nosoftskills.lineup.inbox;

import com.nosoftskills.lineup.inbox.AmbiguityInboxService.ReviewView;
import com.nosoftskills.lineup.model.AmbiguityCandidate;
import com.nosoftskills.lineup.model.AmbiguityReview;
import com.nosoftskills.lineup.model.AmbiguityReviewStatus;
import com.nosoftskills.lineup.model.AmbiguityReviewType;
import com.nosoftskills.lineup.model.ExternalRefSource;
import com.nosoftskills.lineup.model.FormationType;
import com.nosoftskills.lineup.model.Player;
import com.nosoftskills.lineup.model.PlayerAlias;
import com.nosoftskills.lineup.model.Team;
import com.nosoftskills.lineup.testsupport.TeamFormationFixtures;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestSecurity(user = "admin", roles = {"ADMIN"})
class AmbiguityInboxServiceTest {

    @Inject
    AmbiguityInboxService inboxService;

    private Long teamId;
    private Long teamFormationId;
    private Long competitionId;
    private final List<Long> playerIds = new ArrayList<>();

    @BeforeEach
    void setup() {
        QuarkusTransaction.requiringNew().run(() -> {
            TeamFormationFixtures.Ids ids = TeamFormationFixtures.create(
                    "Inbox Test Team", "Test City", FormationType.U15, "Inbox Test League");
            teamId = ids.teamId();
            teamFormationId = ids.teamFormationId();
            competitionId = ids.competitionId();
        });
    }

    @AfterEach
    void cleanup() {
        QuarkusTransaction.requiringNew().run(() -> {
            AmbiguityCandidate.delete("player.id in ?1", playerIds);
            AmbiguityReview.delete("team.id", teamId);
            PlayerAlias.delete("team.id", teamId);
            for (Long playerId : playerIds) {
                Player.deleteById(playerId);
            }
            playerIds.clear();
            TeamFormationFixtures.delete(new TeamFormationFixtures.Ids(teamId, teamFormationId, competitionId));
        });
    }

    private Long createPlayer(String name) {
        return QuarkusTransaction.requiringNew().call(() -> {
            Player player = new Player();
            player.names = name;
            player.persist();
            playerIds.add(player.id);
            return player.id;
        });
    }

    private Long queueReview(String rawName, List<Long> candidatePlayerIds) {
        return QuarkusTransaction.requiringNew().call(() -> {
            AmbiguityReview review = new AmbiguityReview();
            review.type = AmbiguityReviewType.PLAYER;
            review.rawName = rawName;
            review.team = Team.findById(teamId);
            review.source = ExternalRefSource.BFU_TOURNAMENTS;
            review.status = AmbiguityReviewStatus.PENDING;
            review.persist();

            double score = 0.9;
            for (Long playerId : candidatePlayerIds) {
                AmbiguityCandidate candidate = new AmbiguityCandidate();
                candidate.ambiguityReview = review;
                candidate.player = Player.findById(playerId);
                candidate.score = BigDecimal.valueOf(score);
                candidate.persist();
                score -= 0.1;
            }
            return review.id;
        });
    }

    @Test
    void listPendingReturnsCandidatesRankedByScoreDescending() {
        Long firstId = createPlayer("Ivan Ivanov");
        Long secondId = createPlayer("Ivan Ivanov");
        Long reviewId = queueReview("Ivan Ivanov", List.of(firstId, secondId));

        List<ReviewView> pending = inboxService.listPending();

        ReviewView view = pending.stream().filter(r -> r.id().equals(reviewId)).findFirst().orElseThrow();
        assertEquals("Ivan Ivanov", view.rawName());
        assertEquals("Inbox Test Team", view.teamName());
        assertEquals(2, view.candidates().size());
        assertEquals(firstId, view.candidates().get(0).playerId());
        assertTrue(view.candidates().get(0).score().compareTo(view.candidates().get(1).score()) > 0);
    }

    @Test
    void countPendingReflectsOnlyPendingReviews() {
        Long playerId = createPlayer("Dimitar Dimitrov");
        Long pendingReviewId = queueReview("Dimitar Dimitrov", List.of(playerId));
        long before = inboxService.countPending();

        inboxService.resolveReview(pendingReviewId, playerId);

        assertEquals(before - 1, inboxService.countPending());
    }

    @Test
    void resolveReviewPicksExistingPlayerWritesAliasAndMarksResolved() {
        Long playerId = createPlayer("Petar Petrov");
        Long reviewId = queueReview("Petar Petrov", List.of(playerId));

        Player resolved = inboxService.resolveReview(reviewId, playerId);

        assertEquals(playerId, resolved.id);

        QuarkusTransaction.requiringNew().run(() -> {
            AmbiguityReview review = AmbiguityReview.findById(reviewId);
            assertEquals(AmbiguityReviewStatus.RESOLVED, review.status);
            assertEquals(playerId, review.resolvedPlayer.id);
            assertNotNull(review.resolvedAt);
            assertEquals("admin", review.resolvedBy);
        });

        long aliasCount = QuarkusTransaction.requiringNew().call(() ->
                PlayerAlias.count("team.id = ?1 and rawName = ?2 and player.id = ?3",
                        teamId, "Petar Petrov", playerId));
        assertEquals(1, aliasCount);
    }

    @Test
    void confirmNewPlayerCreatesPlayerWritesAliasAndMarksResolved() {
        Long reviewId = queueReview("Nikolay Nikolov", List.of());

        Player created = inboxService.confirmNewPlayer(reviewId);
        playerIds.add(created.id);

        assertEquals("Nikolay Nikolov", created.names);

        QuarkusTransaction.requiringNew().run(() -> {
            AmbiguityReview review = AmbiguityReview.findById(reviewId);
            assertEquals(AmbiguityReviewStatus.RESOLVED, review.status);
            assertEquals(created.id, review.resolvedPlayer.id);
            assertEquals("admin", review.resolvedBy);
        });

        long aliasCount = QuarkusTransaction.requiringNew().call(() ->
                PlayerAlias.count("team.id = ?1 and rawName = ?2 and player.id = ?3",
                        teamId, "Nikolay Nikolov", created.id));
        assertEquals(1, aliasCount);
    }

    @Test
    void resolveReviewOnAlreadyResolvedReviewThrowsBadRequest() {
        Long playerId = createPlayer("Georgi Georgiev");
        Long reviewId = queueReview("Georgi Georgiev", List.of(playerId));

        inboxService.resolveReview(reviewId, playerId);

        assertThrows(BadRequestException.class, () -> inboxService.resolveReview(reviewId, playerId));
    }

    @Test
    void resolveReviewUnknownReviewThrowsNotFound() {
        assertThrows(NotFoundException.class, () -> inboxService.resolveReview(999_999_999L, 1L));
    }

    @Test
    void resolveReviewUnknownPlayerThrowsNotFound() {
        Long playerId = createPlayer("Stefan Stefanov");
        Long reviewId = queueReview("Stefan Stefanov", List.of(playerId));

        assertThrows(NotFoundException.class, () -> inboxService.resolveReview(reviewId, 999_999_999L));
    }
}
