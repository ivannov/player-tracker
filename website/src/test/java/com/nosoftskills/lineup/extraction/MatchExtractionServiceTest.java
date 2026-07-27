package com.nosoftskills.lineup.extraction;

import com.nosoftskills.lineup.matching.OllamaEmbeddingClient;
import com.nosoftskills.lineup.model.AmbiguityReview;
import com.nosoftskills.lineup.model.Competition;
import com.nosoftskills.lineup.model.ExternalRefSource;
import com.nosoftskills.lineup.model.FormationType;
import com.nosoftskills.lineup.model.Match;
import com.nosoftskills.lineup.model.MatchEventType;
import com.nosoftskills.lineup.model.Participation;
import com.nosoftskills.lineup.model.Player;
import com.nosoftskills.lineup.model.PlayerAlias;
import com.nosoftskills.lineup.model.PlayerAppearance;
import com.nosoftskills.lineup.model.Team;
import com.nosoftskills.lineup.model.TeamAlias;
import com.nosoftskills.lineup.model.TeamFormation;
import com.nosoftskills.lineup.scraping.BfuFixture;
import com.nosoftskills.lineup.scraping.BfuFixtureScraperService;
import com.nosoftskills.lineup.scraping.BfuLineupEntry;
import com.nosoftskills.lineup.scraping.BfuMatchData;
import com.nosoftskills.lineup.scraping.BfuMatchEvent;
import com.nosoftskills.lineup.scraping.BfuMatchEventType;
import com.nosoftskills.lineup.scraping.BfuMatchScraperService;
import com.nosoftskills.lineup.scraping.BfuScraperException;
import com.nosoftskills.lineup.scraping.BfuTeamLineup;
import com.nosoftskills.lineup.scraping.EbfuLineupEntry;
import com.nosoftskills.lineup.scraping.EbfuMatchLineup;
import com.nosoftskills.lineup.scraping.EbfuMatchLineupScraperService;
import com.nosoftskills.lineup.scraping.EbfuScraperException;
import com.nosoftskills.lineup.scraping.EbfuTeamLineup;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class MatchExtractionServiceTest {

    private static final String FIXTURES_URL = "https://bfu-tournaments.com/leagues/mes-test?view=past-matches";
    private static final String MATCH_URL = "https://bfu-tournaments.com/stats/match/99999";
    private static final LocalDate MATCH_DATE = LocalDate.of(2025, 3, 15);
    private static final String SEASON = "2024/2025";
    private static final String HOME_NAME = "MES Home FC";
    private static final String AWAY_NAME = "MES Away FC";

    @Inject
    MatchExtractionService extractionService;

    @InjectMock
    BfuFixtureScraperService fixtureScraperService;

    @InjectMock
    BfuMatchScraperService bfuMatchScraperService;

    @InjectMock
    EbfuMatchLineupScraperService ebfuMatchLineupScraperService;

    @InjectMock
    OllamaEmbeddingClient embeddingClient;

    private Long competitionId;
    private Long homeTeamId;
    private Long awayTeamId;
    private Long homeParticipationId;
    private Long awayParticipationId;
    private Long priorMatchId;
    private Long ivanPetrovId;

    private BfuFixture fixture;
    private ExtractionRequest request;

    @BeforeEach
    void setup() throws Exception {
        Mockito.when(embeddingClient.embed(Mockito.anyString())).thenReturn(Optional.empty());

        QuarkusTransaction.requiringNew().run(() -> {
            Competition competition = new Competition();
            competition.name = "MES Test League";
            competition.persist();
            competitionId = competition.id;

            Team homeTeam = new Team();
            homeTeam.name = HOME_NAME;
            homeTeam.location = "Home City";
            homeTeam.persist();
            homeTeamId = homeTeam.id;

            Team awayTeam = new Team();
            awayTeam.name = AWAY_NAME;
            awayTeam.location = "Away City";
            awayTeam.persist();
            awayTeamId = awayTeam.id;

            TeamFormation homeFormation = new TeamFormation();
            homeFormation.team = homeTeam;
            homeFormation.type = FormationType.FIRST;
            homeFormation.persist();

            TeamFormation awayFormation = new TeamFormation();
            awayFormation.team = awayTeam;
            awayFormation.type = FormationType.FIRST;
            awayFormation.persist();

            Participation homeParticipation = new Participation();
            homeParticipation.teamFormation = homeFormation;
            homeParticipation.competition = competition;
            homeParticipation.season = SEASON;
            homeParticipation.persist();
            homeParticipationId = homeParticipation.id;

            Participation awayParticipation = new Participation();
            awayParticipation.teamFormation = awayFormation;
            awayParticipation.competition = competition;
            awayParticipation.season = SEASON;
            awayParticipation.persist();
            awayParticipationId = awayParticipation.id;

            // A player who has already appeared for the home team, so PlayerMatchingService has
            // a roster to confidently match "Ivan Petrov" against (it scopes candidates to
            // players with an existing appearance for the team).
            Player ivanPetrov = new Player();
            ivanPetrov.names = "Ivan Petrov";
            ivanPetrov.persist();
            ivanPetrovId = ivanPetrov.id;

            Match priorMatch = new Match();
            priorMatch.homeTeam = homeParticipation;
            priorMatch.awayTeam = awayParticipation;
            priorMatch.date = MATCH_DATE.minusMonths(1);
            priorMatch.persist();
            priorMatchId = priorMatch.id;

            PlayerAppearance priorAppearance = new PlayerAppearance();
            priorAppearance.player = ivanPetrov;
            priorAppearance.match = priorMatch;
            priorAppearance.participation = homeParticipation;
            priorAppearance.starter = true;
            priorAppearance.persist();
        });

        fixture = new BfuFixture(MATCH_URL, HOME_NAME, AWAY_NAME);
        request = new ExtractionRequest(competitionId, FIXTURES_URL, SEASON, MATCH_DATE);
        Mockito.when(fixtureScraperService.findMatches(FIXTURES_URL, MATCH_DATE)).thenReturn(List.of(fixture));
    }

    // Uses bulk HQL deletes throughout (never deleteById, which loads the entity and defers the
    // actual DELETE to flush time) -- mixing that managed-remove style with immediate bulk
    // deletes in the same transaction is unreliable: Hibernate's auto-flush-before-bulk-query
    // only flushes pending changes whose "query space" it judges overlaps the bulk statement,
    // so a deleteById's DELETE can still be unflushed when a later bulk delete against a
    // referencing table runs, tripping a foreign key violation.
    @AfterEach
    void cleanup() {
        QuarkusTransaction.requiringNew().run(() -> {
            AmbiguityReview.delete("team.id in ?1", List.of(homeTeamId, awayTeamId));
            PlayerAlias.delete("team.id in ?1", List.of(homeTeamId, awayTeamId));
            TeamAlias.delete("rawName in ?1", List.of(HOME_NAME, AWAY_NAME));

            // match_events cascades from player_appearances at the DB level.
            PlayerAppearance.delete("participation.id in ?1", List.of(homeParticipationId, awayParticipationId));
            Match.delete("homeTeam.id = ?1 and awayTeam.id = ?2", homeParticipationId, awayParticipationId);
            Match.delete("id = ?1", priorMatchId);

            Player.delete("id = ?1", ivanPetrovId);
            Participation.delete("id in ?1", List.of(homeParticipationId, awayParticipationId));
            TeamFormation.delete("team.id in ?1", List.of(homeTeamId, awayTeamId));
            Team.delete("id in ?1", List.of(homeTeamId, awayTeamId));
            Competition.delete("id = ?1", competitionId);
        });
    }

    private BfuMatchData bfuMatchDataWithGoalAndAmbiguousAwayPlayer() {
        BfuTeamLineup home = new BfuTeamLineup(HOME_NAME,
                List.of(new BfuLineupEntry(9, "Ivan Petrov")), List.of());
        BfuTeamLineup away = new BfuTeamLineup(AWAY_NAME,
                List.of(new BfuLineupEntry(7, "Georgi Ivanov")), List.of());
        List<BfuMatchEvent> events = List.of(new BfuMatchEvent(true, BfuMatchEventType.GOAL, 55, "Ivan Petrov"));
        return new BfuMatchData(home, away, (short) 2, (short) 0, events, List.of());
    }

    @Test
    void previewResolvesConfidentPlayerAndQueuesAmbiguousPlayerWithoutPersistingMatchData() throws Exception {
        Mockito.when(bfuMatchScraperService.extractMatch(MATCH_URL)).thenReturn(bfuMatchDataWithGoalAndAmbiguousAwayPlayer());

        List<MatchExtractionRow> rows = extractionService.preview(request);

        assertEquals(1, rows.size());
        MatchExtractionRow row = rows.get(0);
        assertEquals(ExternalRefSource.BFU_TOURNAMENTS, row.source());
        assertTrue(row.home().isResolved());
        assertTrue(row.away().isResolved());
        assertTrue(row.isFullyResolvable());

        assertEquals(1, row.homeStarters().size());
        assertTrue(row.homeStarters().get(0).isResolved());
        assertEquals(ivanPetrovId, row.homeStarters().get(0).matchResult().resolvedPlayer().id);

        assertEquals(1, row.awayStarters().size());
        assertFalse(row.awayStarters().get(0).isResolved());
        assertNotNull(row.awayStarters().get(0).matchResult().pendingReview());

        long matchCount = QuarkusTransaction.requiringNew().call(() ->
                Match.count("homeTeam.id = ?1 and awayTeam.id = ?2 and date = ?3",
                        homeParticipationId, awayParticipationId, MATCH_DATE));
        assertEquals(0, matchCount, "preview must not persist Match/PlayerAppearance/MatchEvent rows");
    }

    @Test
    void confirmPersistsMatchAppearancesAndEventsAndLinksAmbiguityReviewToMatch() throws Exception {
        Mockito.when(bfuMatchScraperService.extractMatch(MATCH_URL)).thenReturn(bfuMatchDataWithGoalAndAmbiguousAwayPlayer());

        MatchExtractionSummary summary = extractionService.confirm(request);
        assertEquals(1, summary.matchesCreated());
        assertEquals(0, summary.matchesReused());
        assertEquals(0, summary.rowsSkipped());

        QuarkusTransaction.requiringNew().run(() -> {
            Match match = Match.<Match>find("homeTeam.id = ?1 and awayTeam.id = ?2 and date = ?3",
                    homeParticipationId, awayParticipationId, MATCH_DATE).firstResult();
            assertNotNull(match);
            assertEquals((short) 2, match.homeScore);
            assertEquals((short) 0, match.awayScore);

            PlayerAppearance appearance = PlayerAppearance.<PlayerAppearance>find(
                    "player.id = ?1 and match.id = ?2", ivanPetrovId, match.id).firstResult();
            assertNotNull(appearance);
            assertTrue(appearance.starter);
            assertEquals((short) 9, appearance.number);

            long eventCount = com.nosoftskills.lineup.model.MatchEvent.count(
                    "playerAppearance.id = ?1 and type = ?2 and minute = ?3",
                    appearance.id, MatchEventType.GOAL, (short) 55);
            assertEquals(1, eventCount);

            AmbiguityReview review = AmbiguityReview.<AmbiguityReview>find(
                    "rawName = ?1 and team.id = ?2", "Georgi Ivanov", awayTeamId).firstResult();
            assertNotNull(review);
            assertNotNull(review.match, "ambiguous player review must be linked to the persisted match");
            assertEquals(match.id, review.match.id);

            long awayAppearanceCount = PlayerAppearance.count("participation.id = ?1", awayParticipationId);
            assertEquals(0, awayAppearanceCount, "an unresolved player must not get a PlayerAppearance row");
        });
    }

    @Test
    void confirmIsIdempotentOnRerun() throws Exception {
        Mockito.when(bfuMatchScraperService.extractMatch(MATCH_URL)).thenReturn(bfuMatchDataWithGoalAndAmbiguousAwayPlayer());

        extractionService.confirm(request);
        MatchExtractionSummary secondRun = extractionService.confirm(request);

        assertEquals(0, secondRun.matchesCreated());
        assertEquals(1, secondRun.matchesReused());
        assertEquals(0, secondRun.rowsSkipped());

        QuarkusTransaction.requiringNew().run(() -> {
            long matchCount = Match.count("homeTeam.id = ?1 and awayTeam.id = ?2 and date = ?3",
                    homeParticipationId, awayParticipationId, MATCH_DATE);
            assertEquals(1, matchCount);

            Match match = Match.<Match>find("homeTeam.id = ?1 and awayTeam.id = ?2 and date = ?3",
                    homeParticipationId, awayParticipationId, MATCH_DATE).firstResult();

            long appearanceCount = PlayerAppearance.count("player.id = ?1 and match.id = ?2", ivanPetrovId, match.id);
            assertEquals(1, appearanceCount, "re-running the same extraction must not duplicate appearances");

            PlayerAppearance appearance = PlayerAppearance.<PlayerAppearance>find(
                    "player.id = ?1 and match.id = ?2", ivanPetrovId, match.id).firstResult();
            long eventCount = com.nosoftskills.lineup.model.MatchEvent.count(
                    "playerAppearance.id = ?1", appearance.id);
            assertEquals(1, eventCount, "re-running the same extraction must not duplicate events");
        });
    }

    @Test
    void fallsBackToEbfuLineupWhenPrimarySourceFails() throws Exception {
        Mockito.when(bfuMatchScraperService.extractMatch(MATCH_URL))
                .thenThrow(new BfuScraperException("could not parse match page"));

        EbfuTeamLineup home = new EbfuTeamLineup(HOME_NAME,
                List.of(new EbfuLineupEntry(9, "Ivan Petrov", false, false)), List.of(), null);
        EbfuTeamLineup away = new EbfuTeamLineup(AWAY_NAME,
                List.of(new EbfuLineupEntry(7, "Georgi Ivanov", false, false)), List.of(), null);
        Mockito.when(ebfuMatchLineupScraperService.extractLineup(MATCH_DATE, HOME_NAME, AWAY_NAME))
                .thenReturn(new EbfuMatchLineup(home, away));

        List<MatchExtractionRow> rows = extractionService.preview(request);
        assertEquals(1, rows.size());
        assertEquals(ExternalRefSource.EBFU, rows.get(0).source());
        assertTrue(rows.get(0).isFullyResolvable());
        assertNull(rows.get(0).scrapedMatch().homeScore(), "ebfu.net fallback has no score");

        MatchExtractionSummary summary = extractionService.confirm(request);
        assertEquals(1, summary.matchesCreated());

        QuarkusTransaction.requiringNew().run(() -> {
            PlayerAlias alias = PlayerAlias.<PlayerAlias>find(
                    "source = ?1 and rawName = ?2 and team.id = ?3",
                    ExternalRefSource.EBFU, "Ivan Petrov", homeTeamId).firstResult();
            assertNotNull(alias, "player resolved via ebfu.net fallback must be aliased under the EBFU source");
        });
    }

    @Test
    void rowIsSkippedWhenBothSourcesFail() throws Exception {
        Mockito.when(bfuMatchScraperService.extractMatch(MATCH_URL))
                .thenThrow(new BfuScraperException("bfu page broken"));
        Mockito.when(ebfuMatchLineupScraperService.extractLineup(MATCH_DATE, HOME_NAME, AWAY_NAME))
                .thenThrow(new EbfuScraperException("ebfu page broken"));

        List<MatchExtractionRow> rows = extractionService.preview(request);
        assertEquals(1, rows.size());
        assertNull(rows.get(0).scrapedMatch());
        assertNotNull(rows.get(0).extractionError());
        assertFalse(rows.get(0).isFullyResolvable());

        MatchExtractionSummary summary = extractionService.confirm(request);
        assertEquals(0, summary.matchesCreated());
        assertEquals(1, summary.rowsSkipped());
    }

    @Test
    void rowIsSkippedWhenATeamCannotBeResolved() throws Exception {
        BfuFixture unknownFixture = new BfuFixture(MATCH_URL, "Unknown FC", AWAY_NAME);
        Mockito.when(fixtureScraperService.findMatches(FIXTURES_URL, MATCH_DATE)).thenReturn(List.of(unknownFixture));

        BfuTeamLineup home = new BfuTeamLineup("Unknown FC", List.of(new BfuLineupEntry(1, "Someone")), List.of());
        BfuTeamLineup away = new BfuTeamLineup(AWAY_NAME, List.of(new BfuLineupEntry(2, "Someone Else")), List.of());
        Mockito.when(bfuMatchScraperService.extractMatch(MATCH_URL))
                .thenReturn(new BfuMatchData(home, away, (short) 1, (short) 1, List.of(), List.of()));

        List<MatchExtractionRow> rows = extractionService.preview(request);
        assertEquals(1, rows.size());
        assertFalse(rows.get(0).home().isResolved());
        assertFalse(rows.get(0).isFullyResolvable());

        MatchExtractionSummary summary = extractionService.confirm(request);
        assertEquals(0, summary.matchesCreated());
        assertEquals(1, summary.rowsSkipped());
    }
}
