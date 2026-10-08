package com.nosoftskills.lineup.inbox;

import com.nosoftskills.lineup.inbox.AmbiguityInboxService.ReviewView;
import com.nosoftskills.lineup.model.AmbiguityCandidate;
import com.nosoftskills.lineup.model.AmbiguityOccurrence;
import com.nosoftskills.lineup.model.AmbiguityOccurrenceEvent;
import com.nosoftskills.lineup.model.AmbiguityReview;
import com.nosoftskills.lineup.model.AmbiguityReviewStatus;
import com.nosoftskills.lineup.model.AmbiguityReviewType;
import com.nosoftskills.lineup.model.ExternalRefSource;
import com.nosoftskills.lineup.model.FormationType;
import com.nosoftskills.lineup.model.Match;
import com.nosoftskills.lineup.model.MatchEvent;
import com.nosoftskills.lineup.model.MatchEventType;
import com.nosoftskills.lineup.model.Participation;
import com.nosoftskills.lineup.model.Player;
import com.nosoftskills.lineup.model.PlayerAlias;
import com.nosoftskills.lineup.model.PlayerAppearance;
import com.nosoftskills.lineup.model.TeamFormation;
import com.nosoftskills.lineup.model.Team;
import com.nosoftskills.lineup.model.TeamAlias;
import com.nosoftskills.lineup.testsupport.TeamFormationFixtures;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    private final List<Long> extraTeamIds = new ArrayList<>();
    private final List<Long> teamReviewIds = new ArrayList<>();
    private final List<Long> matchIds = new ArrayList<>();

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
            if (!matchIds.isEmpty()) {
                // match_events and ambiguity_occurrence_events cascade at the DB level.
                PlayerAppearance.delete("match.id in ?1", matchIds);
                AmbiguityOccurrence.delete("match.id in ?1", matchIds);
                Match.delete("id in ?1", matchIds);
                matchIds.clear();
            }
            AmbiguityCandidate.delete("player.id in ?1", playerIds);
            for (Long reviewId : teamReviewIds) {
                AmbiguityReview.deleteById(reviewId);
            }
            teamReviewIds.clear();
            TeamAlias.delete("rawName like ?1", "LT18 Test%");
            for (Long extraTeamId : extraTeamIds) {
                Team.deleteById(extraTeamId);
            }
            extraTeamIds.clear();
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

    private Long createTeam(String name) {
        return QuarkusTransaction.requiringNew().call(() -> {
            Team team = new Team();
            team.name = name;
            team.location = "Test City";
            team.persist();
            extraTeamIds.add(team.id);
            return team.id;
        });
    }

    private Long queueTeamReview(String rawName, Long mappedTeamId) {
        return QuarkusTransaction.requiringNew().call(() -> {
            AmbiguityReview review = new AmbiguityReview();
            review.type = AmbiguityReviewType.TEAM;
            review.rawName = rawName;
            review.team = Team.findById(mappedTeamId);
            review.source = ExternalRefSource.BFU_TOURNAMENTS;
            review.status = AmbiguityReviewStatus.PENDING;
            review.persist();
            teamReviewIds.add(review.id);
            return review.id;
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

    // Both sides use the fixture's single participation: the inbox only reads the kept side back,
    // it never checks that home and away differ.
    private Long createMatchWithOccurrence(Long reviewId, LocalDate date, boolean starter, Short number,
            Short inMinute, Short outMinute, MatchEventType eventType, Short eventMinute) {
        return QuarkusTransaction.requiringNew().call(() -> {
            Participation participation = Participation.<Participation>find("teamFormation.id", teamFormationId)
                    .firstResultOptional().orElseGet(() -> {
                        Participation p = new Participation();
                        p.teamFormation = TeamFormation.findById(teamFormationId);
                        p.competition = com.nosoftskills.lineup.model.Competition.findById(competitionId);
                        p.season = "2024/2025";
                        p.persist();
                        return p;
                    });

            Match match = new Match();
            match.homeTeam = participation;
            match.awayTeam = participation;
            match.date = date;
            match.persist();
            matchIds.add(match.id);

            AmbiguityOccurrence occurrence = new AmbiguityOccurrence();
            occurrence.ambiguityReview = AmbiguityReview.findById(reviewId);
            occurrence.match = match;
            occurrence.participation = participation;
            occurrence.starter = starter;
            occurrence.number = number;
            occurrence.substitutedInMinute = inMinute;
            occurrence.substitutedOutMinute = outMinute;
            occurrence.persist();

            if (eventType != null) {
                AmbiguityOccurrenceEvent event = new AmbiguityOccurrenceEvent();
                event.ambiguityOccurrence = occurrence;
                event.type = eventType;
                event.minute = eventMinute;
                event.persist();
            }
            return match.id;
        });
    }

    private PlayerAppearance findAppearance(Long playerId, Long matchId) {
        return PlayerAppearance.<PlayerAppearance>find("player.id = ?1 and match.id = ?2", playerId, matchId).firstResult();
    }

    @Test
    @DisplayName("UC-011 BR-006: picking a candidate adds the player to every match the name was raised in, with the kept details")
    void resolveReviewAddsLineupEntriesFromKeptDetails() {
        Long playerId = createPlayer("BR6 Kiril Kirilov");
        Long reviewId = queueReview("BR6 Kiril Kirilov", List.of(playerId));
        Long firstMatchId = createMatchWithOccurrence(reviewId, LocalDate.of(2025, 3, 1), true, (short) 10,
                null, (short) 70, MatchEventType.GOAL, (short) 25);
        Long secondMatchId = createMatchWithOccurrence(reviewId, LocalDate.of(2025, 3, 8), false, (short) 14,
                (short) 60, null, MatchEventType.YELLOW_CARD, (short) 80);

        inboxService.resolveReview(reviewId, playerId);

        QuarkusTransaction.requiringNew().run(() -> {
            PlayerAppearance first = findAppearance(playerId, firstMatchId);
            assertNotNull(first);
            assertTrue(first.starter);
            assertEquals((short) 10, first.number);
            assertNull(first.substitutedInMinute);
            assertEquals((short) 70, first.substitutedOutMinute);
            assertEquals(1, MatchEvent.count("playerAppearance.id = ?1 and type = ?2 and minute = ?3",
                    first.id, MatchEventType.GOAL, (short) 25));

            PlayerAppearance second = findAppearance(playerId, secondMatchId);
            assertNotNull(second);
            assertEquals(false, second.starter);
            assertEquals((short) 14, second.number);
            assertEquals((short) 60, second.substitutedInMinute);
            assertEquals(1, MatchEvent.count("playerAppearance.id = ?1 and type = ?2 and minute = ?3",
                    second.id, MatchEventType.YELLOW_CARD, (short) 80));
        });
    }

    @Test
    @DisplayName("UC-011 BR-006: confirming a new player adds the new player to the match the name was raised in")
    void confirmNewPlayerAddsLineupEntryFromKeptDetails() {
        Long reviewId = queueReview("BR6 Nov Igrach", List.of());
        Long matchId = createMatchWithOccurrence(reviewId, LocalDate.of(2025, 3, 1), true, (short) 5,
                null, null, MatchEventType.GOAL, (short) 12);

        Player created = inboxService.confirmNewPlayer(reviewId);
        playerIds.add(created.id);

        QuarkusTransaction.requiringNew().run(() -> {
            PlayerAppearance appearance = findAppearance(created.id, matchId);
            assertNotNull(appearance);
            assertTrue(appearance.starter);
            assertEquals((short) 5, appearance.number);
            assertEquals(1, MatchEvent.count("playerAppearance.id", appearance.id));
        });
    }

    @Test
    @DisplayName("UC-011 BR-006: an existing lineup entry is kept and only goals and cards not yet stored are added")
    void resolveReviewKeepsExistingEntryAndAddsOnlyMissingEvents() {
        Long playerId = createPlayer("BR6 Vasil Vasilev");
        Long reviewId = queueReview("BR6 Vasil Vasilev", List.of(playerId));
        Long matchId = createMatchWithOccurrence(reviewId, LocalDate.of(2025, 3, 1), true, (short) 9,
                null, null, MatchEventType.GOAL, (short) 30);
        QuarkusTransaction.requiringNew().run(() -> {
            AmbiguityOccurrence occurrence = AmbiguityOccurrence.find("ambiguityReview.id", reviewId).firstResult();
            AmbiguityOccurrenceEvent card = new AmbiguityOccurrenceEvent();
            card.ambiguityOccurrence = occurrence;
            card.type = MatchEventType.YELLOW_CARD;
            card.minute = (short) 40;
            card.persist();

            PlayerAppearance existing = new PlayerAppearance();
            existing.player = Player.findById(playerId);
            existing.match = Match.findById(matchId);
            existing.participation = occurrence.participation;
            existing.starter = false;
            existing.number = (short) 99;
            existing.persist();

            MatchEvent goal = new MatchEvent();
            goal.playerAppearance = existing;
            goal.type = MatchEventType.GOAL;
            goal.minute = (short) 30;
            goal.persist();
        });

        inboxService.resolveReview(reviewId, playerId);

        QuarkusTransaction.requiringNew().run(() -> {
            assertEquals(1, PlayerAppearance.count("player.id = ?1 and match.id = ?2", playerId, matchId));
            PlayerAppearance appearance = findAppearance(playerId, matchId);
            assertEquals(false, appearance.starter, "existing entry must be kept unchanged");
            assertEquals((short) 99, appearance.number);
            assertEquals(1, MatchEvent.count("playerAppearance.id = ?1 and type = ?2", appearance.id, MatchEventType.GOAL));
            assertEquals(1, MatchEvent.count("playerAppearance.id = ?1 and type = ?2", appearance.id, MatchEventType.YELLOW_CARD));
        });
    }

    @Test
    @DisplayName("UC-011 BR-006: an item without kept lineup details adds no lineup entry")
    void resolveReviewWithoutKeptDetailsAddsNoLineupEntry() {
        Long playerId = createPlayer("BR6 Old Item");
        Long reviewId = queueReview("BR6 Old Item", List.of(playerId));

        inboxService.resolveReview(reviewId, playerId);

        long appearances = QuarkusTransaction.requiringNew().call(() -> PlayerAppearance.count("player.id", playerId));
        assertEquals(0, appearances);
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

    @Test
    void resolveTeamReviewPicksTeamWritesAliasAndMarksResolved() {
        Long teamAId = createTeam("LT18 Test Team A");
        Long teamBId = createTeam("LT18 Test Team B");
        Long reviewId = queueTeamReview("LT18 Test Raw Name", teamAId);

        Team resolved = inboxService.resolveTeamReview(reviewId, teamBId);

        assertEquals(teamBId, resolved.id);

        QuarkusTransaction.requiringNew().run(() -> {
            AmbiguityReview review = AmbiguityReview.findById(reviewId);
            assertEquals(AmbiguityReviewStatus.RESOLVED, review.status);
            assertEquals(teamBId, review.resolvedTeam.id);
            assertNotNull(review.resolvedAt);
            assertEquals("admin", review.resolvedBy);
        });

        long aliasCount = QuarkusTransaction.requiringNew().call(() ->
                TeamAlias.count("source = ?1 and rawName = ?2 and team.id = ?3",
                        ExternalRefSource.BFU_TOURNAMENTS, "LT18 Test Raw Name", teamBId));
        assertEquals(1, aliasCount);
    }

    @Test
    void resolveTeamReviewRepointsExistingAliasInsteadOfDuplicating() {
        Long teamAId = createTeam("LT18 Test Team A2");
        Long teamBId = createTeam("LT18 Test Team B2");
        QuarkusTransaction.requiringNew().run(() -> {
            TeamAlias alias = new TeamAlias();
            alias.source = ExternalRefSource.BFU_TOURNAMENTS;
            alias.rawName = "LT18 Test Raw Name 2";
            alias.team = Team.findById(teamAId);
            alias.persist();
        });
        Long reviewId = queueTeamReview("LT18 Test Raw Name 2", teamAId);

        inboxService.resolveTeamReview(reviewId, teamBId);

        long totalAliasCount = QuarkusTransaction.requiringNew().call(() -> TeamAlias.count(
                "source = ?1 and rawName = ?2", ExternalRefSource.BFU_TOURNAMENTS, "LT18 Test Raw Name 2"));
        assertEquals(1, totalAliasCount, "must repoint the existing alias, not duplicate it");

        long aliasNowOnB = QuarkusTransaction.requiringNew().call(() -> TeamAlias.count(
                "source = ?1 and rawName = ?2 and team.id = ?3",
                ExternalRefSource.BFU_TOURNAMENTS, "LT18 Test Raw Name 2", teamBId));
        assertEquals(1, aliasNowOnB);
    }

    @Test
    void resolveTeamReviewOnPlayerReviewThrowsBadRequestAndCreatesNoPlayer() {
        Long playerId = createPlayer("LT18 Should Not Resolve");
        Long reviewId = queueReview("LT18 Should Not Resolve", List.of(playerId));
        Long teamAId = createTeam("LT18 Test Team C");
        long playersBefore = Player.count();

        assertThrows(BadRequestException.class, () -> inboxService.resolveTeamReview(reviewId, teamAId));
        assertEquals(playersBefore, Player.count());
    }

    @Test
    void resolveReviewOnTeamReviewThrowsBadRequest() {
        Long teamAId = createTeam("LT18 Test Team D");
        Long reviewId = queueTeamReview("LT18 Test Raw Name D", teamAId);

        assertThrows(BadRequestException.class, () -> inboxService.resolveReview(reviewId, teamAId));
    }

    @Test
    void confirmNewPlayerOnTeamReviewThrowsBadRequestAndCreatesNoPlayer() {
        Long teamAId = createTeam("LT18 Test Team E");
        Long reviewId = queueTeamReview("LT18 Test Raw Name E", teamAId);
        long playersBefore = Player.count();

        assertThrows(BadRequestException.class, () -> inboxService.confirmNewPlayer(reviewId));
        assertEquals(playersBefore, Player.count());
    }

    @Test
    void resolveTeamReviewUnknownTeamThrowsNotFound() {
        Long teamAId = createTeam("LT18 Test Team F");
        Long reviewId = queueTeamReview("LT18 Test Raw Name F", teamAId);

        assertThrows(NotFoundException.class, () -> inboxService.resolveTeamReview(reviewId, 999_999_999L));
    }

    @Test
    void resolveTeamReviewUnknownReviewThrowsNotFound() {
        Long teamAId = createTeam("LT18 Test Team G");

        assertThrows(NotFoundException.class, () -> inboxService.resolveTeamReview(999_999_999L, teamAId));
    }

    @Test
    void listPendingMarksTeamReviewsAsTeamReview() {
        Long teamAId = createTeam("LT18 Test Team H");
        Long reviewId = queueTeamReview("LT18 Test Raw Name H", teamAId);

        List<ReviewView> pending = inboxService.listPending();

        ReviewView view = pending.stream().filter(r -> r.id().equals(reviewId)).findFirst().orElseThrow();
        assertTrue(view.teamReview());
        assertTrue(view.candidates().isEmpty());
    }
}
