package com.nosoftskills.lineup.matching;

import com.nosoftskills.lineup.matching.PlayerMatchingService.MatchResult;
import com.nosoftskills.lineup.model.AmbiguityCandidate;
import com.nosoftskills.lineup.model.AmbiguityReview;
import com.nosoftskills.lineup.model.AmbiguityReviewStatus;
import com.nosoftskills.lineup.model.AmbiguityReviewType;
import com.nosoftskills.lineup.model.Competition;
import com.nosoftskills.lineup.model.ExternalRefSource;
import com.nosoftskills.lineup.model.FormationType;
import com.nosoftskills.lineup.model.Match;
import com.nosoftskills.lineup.model.Participation;
import com.nosoftskills.lineup.model.Player;
import com.nosoftskills.lineup.model.PlayerAlias;
import com.nosoftskills.lineup.model.PlayerAppearance;
import com.nosoftskills.lineup.model.Team;
import com.nosoftskills.lineup.model.TeamFormation;
import com.nosoftskills.lineup.testsupport.TeamFormationFixtures;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class PlayerMatchingServiceTest {

    @Inject
    PlayerMatchingService matchingService;

    @Inject
    EntityManager em;

    @InjectMock
    OllamaEmbeddingClient embeddingClient;

    private Long teamId;
    private Long teamFormationId;
    private Long competitionId;
    private Long participationId;
    private Long matchId;
    private final List<Long> playerIds = new ArrayList<>();

    @BeforeEach
    void setup() {
        QuarkusTransaction.requiringNew().run(() -> {
            TeamFormationFixtures.Ids ids = TeamFormationFixtures.create(
                    "Matching Test Team", "Test City", FormationType.U15, "Matching Test League");
            teamId = ids.teamId();
            teamFormationId = ids.teamFormationId();
            competitionId = ids.competitionId();

            Participation participation = new Participation();
            participation.teamFormation = TeamFormation.findById(teamFormationId);
            participation.competition = Competition.findById(competitionId);
            participation.season = "2024/2025";
            participation.persist();
            participationId = participation.id;

            Match match = new Match();
            match.homeTeam = participation;
            match.awayTeam = participation;
            match.date = LocalDate.of(2024, 9, 1);
            match.persist();
            matchId = match.id;
        });

        // Default: behave as if Ollama is unreachable, matching the other tests' real-client
        // fallback behavior. Individual tests override this for a specific rawName as needed.
        Mockito.when(embeddingClient.embed(Mockito.anyString())).thenReturn(Optional.empty());
    }

    @AfterEach
    void cleanup() {
        QuarkusTransaction.requiringNew().run(() -> {
            AmbiguityCandidate.delete("player.id in ?1", playerIds);
            AmbiguityReview.delete("team.id", teamId);
            PlayerAlias.delete("team.id", teamId);
            PlayerAppearance.delete("match.id", matchId);
            Match.deleteById(matchId);
            Participation.deleteById(participationId);
            for (Long playerId : playerIds) {
                Player.deleteById(playerId);
            }
            playerIds.clear();
            TeamFormationFixtures.delete(new TeamFormationFixtures.Ids(teamId, teamFormationId, competitionId));
        });
    }

    private Long createPlayerWithAppearance(String name) {
        return QuarkusTransaction.requiringNew().call(() -> {
            Player player = new Player();
            player.names = name;
            player.persist();
            playerIds.add(player.id);

            PlayerAppearance appearance = new PlayerAppearance();
            appearance.player = player;
            appearance.match = Match.findById(matchId);
            appearance.participation = Participation.findById(participationId);
            appearance.starter = true;
            appearance.persist();

            return player.id;
        });
    }

    private void setEmbedding(Long playerId, double[] vector) {
        QuarkusTransaction.requiringNew().run(() ->
                em.createNativeQuery("UPDATE players SET name_embedding = CAST(:v AS vector) WHERE id = :id")
                        .setParameter("v", PgVector.toLiteral(vector))
                        .setParameter("id", playerId)
                        .executeUpdate());
    }

    @Test
    void exactAliasHitShortCircuitsWithoutRunningSimilarityQuery() {
        Long playerId = createPlayerWithAppearance("Georgi Georgiev");

        QuarkusTransaction.requiringNew().run(() -> {
            PlayerAlias alias = new PlayerAlias();
            alias.player = Player.findById(playerId);
            alias.source = ExternalRefSource.BFU_TOURNAMENTS;
            alias.rawName = "G. Georgiev";
            alias.team = Team.findById(teamId);
            alias.persist();
        });

        MatchResult result = matchingService.resolve("G. Georgiev", teamId, ExternalRefSource.BFU_TOURNAMENTS);

        assertTrue(result.isResolved());
        assertEquals(playerId, result.resolvedPlayer().id);
        assertNull(result.pendingReview());

        long aliasCount = QuarkusTransaction.requiringNew().call(() ->
                PlayerAlias.count("team.id = ?1 and rawName = ?2", teamId, "G. Georgiev"));
        assertEquals(1, aliasCount);
    }

    @Test
    void confidentSingleMatchAutoResolvesAndWritesAliasBack() {
        Long playerId = createPlayerWithAppearance("Petar Petrov");

        MatchResult result = matchingService.resolve("Petar Petrov", teamId, ExternalRefSource.BFU_TOURNAMENTS);

        assertTrue(result.isResolved());
        assertEquals(playerId, result.resolvedPlayer().id);
        assertNull(result.pendingReview());

        PlayerAlias alias = QuarkusTransaction.requiringNew().call(() ->
                PlayerAlias.<PlayerAlias>find(
                        "source = ?1 and rawName = ?2 and team.id = ?3",
                        ExternalRefSource.BFU_TOURNAMENTS, "Petar Petrov", teamId)
                        .firstResult());
        assertNotNull(alias);
        assertEquals(playerId, alias.player.id);
    }

    @Test
    void tiedCandidatesQueueAmbiguityReviewInsteadOfGuessing() {
        Long firstId = createPlayerWithAppearance("Ivan Ivanov");
        Long secondId = createPlayerWithAppearance("Ivan Ivanov");

        MatchResult result = matchingService.resolve("Ivan Ivanov", teamId, ExternalRefSource.BFU_TOURNAMENTS);

        assertTrue(!result.isResolved());
        assertNotNull(result.pendingReview());

        AmbiguityReview review = QuarkusTransaction.requiringNew().call(() ->
                AmbiguityReview.findById(result.pendingReview().id));
        assertEquals(AmbiguityReviewType.PLAYER, review.type);
        assertEquals(AmbiguityReviewStatus.PENDING, review.status);
        assertEquals("Ivan Ivanov", review.rawName);
        assertEquals(teamId, review.team.id);

        long candidateCount = QuarkusTransaction.requiringNew().call(() ->
                AmbiguityCandidate.count("ambiguityReview.id", review.id));
        assertEquals(2, candidateCount);

        List<Long> candidatePlayerIds = QuarkusTransaction.requiringNew().call(() ->
                AmbiguityCandidate.<AmbiguityCandidate>find("ambiguityReview.id", review.id).list()
                        .stream().map(c -> c.player.id).toList());
        assertTrue(candidatePlayerIds.contains(firstId));
        assertTrue(candidatePlayerIds.contains(secondId));
    }

    @Test
    void noCloseCandidateQueuesAmbiguityReviewWithLowScoreCandidates() {
        createPlayerWithAppearance("Zdravko Dimitrov");

        MatchResult result = matchingService.resolve("Kristiyan Nikolov", teamId, ExternalRefSource.BFU_TOURNAMENTS);

        assertTrue(!result.isResolved());
        assertNotNull(result.pendingReview());

        AmbiguityReview review = QuarkusTransaction.requiringNew().call(() ->
                AmbiguityReview.findById(result.pendingReview().id));
        assertEquals("Kristiyan Nikolov", review.rawName);
    }

    @Test
    void embeddingReRankDisambiguatesTiedTrigramCandidatesWhenAvailable() {
        Long firstId = createPlayerWithAppearance("Ivan Ivanov");
        Long secondId = createPlayerWithAppearance("Ivan Ivanov");
        setEmbedding(firstId, TestVectors.embedding(1, 0, 0));
        setEmbedding(secondId, TestVectors.embedding(0, 1, 0));
        Mockito.when(embeddingClient.embed("Ivan Ivanov"))
                .thenReturn(Optional.of(TestVectors.embedding(1, 0, 0)));

        MatchResult result = matchingService.resolve("Ivan Ivanov", teamId, ExternalRefSource.BFU_TOURNAMENTS);

        assertTrue(result.isResolved());
        assertEquals(firstId, result.resolvedPlayer().id);
        assertNull(result.pendingReview());
    }

    @Test
    void embeddingReRankIsSkippedWhenOllamaUnreachableEvenIfCandidatesHaveEmbeddings() {
        Long firstId = createPlayerWithAppearance("Ivan Ivanov");
        Long secondId = createPlayerWithAppearance("Ivan Ivanov");
        setEmbedding(firstId, TestVectors.embedding(1, 0, 0));
        setEmbedding(secondId, TestVectors.embedding(0, 1, 0));
        Mockito.when(embeddingClient.embed("Ivan Ivanov")).thenReturn(Optional.empty());

        MatchResult result = matchingService.resolve("Ivan Ivanov", teamId, ExternalRefSource.BFU_TOURNAMENTS);

        assertTrue(!result.isResolved());
        assertNotNull(result.pendingReview());

        long candidateCount = QuarkusTransaction.requiringNew().call(() ->
                AmbiguityCandidate.count("ambiguityReview.id", result.pendingReview().id));
        assertEquals(2, candidateCount);
    }
}
