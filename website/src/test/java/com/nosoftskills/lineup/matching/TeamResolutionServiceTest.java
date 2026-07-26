package com.nosoftskills.lineup.matching;

import com.nosoftskills.lineup.model.AmbiguityReview;
import com.nosoftskills.lineup.model.AmbiguityReviewStatus;
import com.nosoftskills.lineup.model.AmbiguityReviewType;
import com.nosoftskills.lineup.model.ExternalRefSource;
import com.nosoftskills.lineup.model.Team;
import com.nosoftskills.lineup.model.TeamAlias;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class TeamResolutionServiceTest {

    @Inject
    TeamResolutionService teamResolutionService;

    private Long teamAId;
    private Long teamBId;

    @AfterEach
    void cleanup() {
        QuarkusTransaction.requiringNew().run(() -> {
            AmbiguityReview.delete("type = ?1 and rawName like ?2", AmbiguityReviewType.TEAM, "TRS Test%");
            TeamAlias.delete("rawName like ?1", "TRS Test%");
            if (teamAId != null) Team.deleteById(teamAId);
            if (teamBId != null) Team.deleteById(teamBId);
        });
    }

    private Long createTeam(String name) {
        return QuarkusTransaction.requiringNew().call(() -> {
            Team team = new Team();
            team.name = name;
            team.location = "Test City";
            team.persist();
            return team.id;
        });
    }

    // recordAlias() is @Transactional (REQUIRED), so running it inside the same
    // requiringNew() block as the Team lookup keeps the team entity attached to the
    // persistence context that both the lookup and the write share, exactly as it is in
    // production where the resource's own @Transactional method supplies that context.
    @Test
    void recordAliasCreatesNewAliasWhenNoneExists() {
        teamAId = createTeam("TRS Test Team A");

        Map<String, TeamAlias> preloaded = teamResolutionService.preloadAliases(
                ExternalRefSource.BFU_TOURNAMENTS, List.of("TRS Test Raw Name"));
        assertTrue(preloaded.isEmpty());

        QuarkusTransaction.requiringNew().run(() -> {
            Team team = Team.findById(teamAId);
            teamResolutionService.recordAlias(preloaded, ExternalRefSource.BFU_TOURNAMENTS, "TRS Test Raw Name", team);
        });

        TeamAlias alias = QuarkusTransaction.requiringNew().call(() ->
                TeamAlias.<TeamAlias>find(
                        "source = ?1 and rawName = ?2", ExternalRefSource.BFU_TOURNAMENTS, "TRS Test Raw Name")
                        .firstResult());
        assertEquals(teamAId, alias.team.id);
    }

    @Test
    void recordAliasReusesExistingAliasWithoutDuplicating() {
        teamAId = createTeam("TRS Test Team A");
        Map<String, TeamAlias> preloaded = new HashMap<>();

        QuarkusTransaction.requiringNew().run(() -> {
            Team team = Team.findById(teamAId);
            teamResolutionService.recordAlias(preloaded, ExternalRefSource.BFU_TOURNAMENTS, "TRS Test Raw Name", team);
        });
        QuarkusTransaction.requiringNew().run(() -> {
            Team team = Team.findById(teamAId);
            teamResolutionService.recordAlias(preloaded, ExternalRefSource.BFU_TOURNAMENTS, "TRS Test Raw Name", team);
        });

        long aliasCount = QuarkusTransaction.requiringNew().call(() -> TeamAlias.count(
                "source = ?1 and rawName = ?2", ExternalRefSource.BFU_TOURNAMENTS, "TRS Test Raw Name"));
        assertEquals(1, aliasCount);
    }

    @Test
    void recordAliasQueuesAmbiguityReviewInsteadOfRepointingMapping() {
        teamAId = createTeam("TRS Test Team A");
        teamBId = createTeam("TRS Test Team B");
        Map<String, TeamAlias> preloaded = new HashMap<>();

        QuarkusTransaction.requiringNew().run(() -> {
            Team teamA = Team.findById(teamAId);
            teamResolutionService.recordAlias(preloaded, ExternalRefSource.BFU_TOURNAMENTS, "TRS Test Raw Name", teamA);
        });
        QuarkusTransaction.requiringNew().run(() -> {
            Team teamB = Team.findById(teamBId);
            teamResolutionService.recordAlias(preloaded, ExternalRefSource.BFU_TOURNAMENTS, "TRS Test Raw Name", teamB);
        });

        QuarkusTransaction.requiringNew().run(() -> {
            TeamAlias alias = TeamAlias.<TeamAlias>find(
                    "source = ?1 and rawName = ?2", ExternalRefSource.BFU_TOURNAMENTS, "TRS Test Raw Name")
                    .firstResult();
            assertEquals(teamAId, alias.team.id, "existing mapping must not be repointed");

            AmbiguityReview review = AmbiguityReview.<AmbiguityReview>find(
                    "type = ?1 and rawName = ?2 and status = ?3",
                    AmbiguityReviewType.TEAM, "TRS Test Raw Name", AmbiguityReviewStatus.PENDING)
                    .firstResult();
            assertEquals(teamAId, review.team.id);
        });
    }

    @Test
    void recordAliasDoesNotDuplicatePendingAmbiguityReview() {
        teamAId = createTeam("TRS Test Team A");
        teamBId = createTeam("TRS Test Team B");
        Map<String, TeamAlias> preloaded = new HashMap<>();

        QuarkusTransaction.requiringNew().run(() -> {
            Team teamA = Team.findById(teamAId);
            teamResolutionService.recordAlias(preloaded, ExternalRefSource.BFU_TOURNAMENTS, "TRS Test Raw Name", teamA);
        });
        for (int i = 0; i < 2; i++) {
            QuarkusTransaction.requiringNew().run(() -> {
                Team teamB = Team.findById(teamBId);
                teamResolutionService.recordAlias(preloaded, ExternalRefSource.BFU_TOURNAMENTS, "TRS Test Raw Name", teamB);
            });
        }

        long reviewCount = QuarkusTransaction.requiringNew().call(() -> AmbiguityReview.count(
                "type = ?1 and rawName = ?2 and status = ?3",
                AmbiguityReviewType.TEAM, "TRS Test Raw Name", AmbiguityReviewStatus.PENDING));
        assertEquals(1, reviewCount);
    }

    @Test
    void recordAliasIgnoresBlankRawName() {
        teamAId = createTeam("TRS Test Team A");
        Map<String, TeamAlias> preloaded = new HashMap<>();

        QuarkusTransaction.requiringNew().run(() -> {
            Team team = Team.findById(teamAId);
            teamResolutionService.recordAlias(preloaded, ExternalRefSource.BFU_TOURNAMENTS, "", team);
            teamResolutionService.recordAlias(preloaded, ExternalRefSource.BFU_TOURNAMENTS, null, team);
        });

        long aliasCount = QuarkusTransaction.requiringNew().call(() -> TeamAlias.count("team.id", teamAId));
        assertEquals(0, aliasCount);
    }

    @Test
    void preloadAliasesReturnsEmptyMapForBlankOrNullInput() {
        assertTrue(teamResolutionService.preloadAliases(ExternalRefSource.BFU_TOURNAMENTS, null).isEmpty());
        assertTrue(teamResolutionService.preloadAliases(ExternalRefSource.BFU_TOURNAMENTS, List.of("", "  ")).isEmpty());
    }

    @Test
    void preloadAliasesIsScopedBySource() {
        teamAId = createTeam("TRS Test Team A");
        Map<String, TeamAlias> empty = new HashMap<>();

        QuarkusTransaction.requiringNew().run(() -> {
            Team team = Team.findById(teamAId);
            teamResolutionService.recordAlias(empty, ExternalRefSource.BFU_TOURNAMENTS, "TRS Test Raw Name", team);
        });

        Map<String, TeamAlias> forOtherSource = teamResolutionService.preloadAliases(
                ExternalRefSource.EBFU, List.of("TRS Test Raw Name"));
        assertTrue(forOtherSource.isEmpty());

        Map<String, TeamAlias> forSameSource = teamResolutionService.preloadAliases(
                ExternalRefSource.BFU_TOURNAMENTS, List.of("TRS Test Raw Name"));
        assertEquals(1, forSameSource.size());
        assertNull(forOtherSource.get("TRS Test Raw Name"));
    }
}
