package com.nosoftskills.lineup.extraction;

import com.nosoftskills.lineup.matching.PlayerMatchingService;
import com.nosoftskills.lineup.model.Competition;
import com.nosoftskills.lineup.model.FormationType;
import com.nosoftskills.lineup.model.Participation;
import com.nosoftskills.lineup.model.Player;
import com.nosoftskills.lineup.model.TeamFormation;
import com.nosoftskills.lineup.scraping.BfuFixture;
import com.nosoftskills.lineup.scraping.BfuScraperException;
import com.nosoftskills.lineup.scraping.ScrapedMatch;
import com.nosoftskills.lineup.scraping.ScrapedTeamLineup;
import com.nosoftskills.lineup.testsupport.TeamFormationFixtures;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class MatchExtractionResourceTest {

    private static final String FIXTURES_URL = "https://bfu-tournaments.com/leagues/test?view=past-matches";
    private static final String MATCH_URL = "https://bfu-tournaments.com/stats/match/12345";
    private static final String SEASON = "2025/2026";
    private static final String DATE = "2026-07-27";

    @InjectMock
    MatchExtractionService extractionService;

    private Long teamId;
    private Long teamFormationId;
    private Long competitionId;
    private Long participationId;

    @BeforeEach
    void setup() {
        QuarkusTransaction.requiringNew().run(() -> {
            TeamFormationFixtures.Ids ids = TeamFormationFixtures.create(
                    "Extraction Test Team", "Test City", FormationType.FIRST, "Extraction Test League");
            teamId = ids.teamId();
            teamFormationId = ids.teamFormationId();
            competitionId = ids.competitionId();

            Participation p = new Participation();
            p.teamFormation = TeamFormation.findById(teamFormationId);
            p.competition = Competition.findById(competitionId);
            p.season = SEASON;
            p.persist();
            participationId = p.id;
        });
    }

    @AfterEach
    void cleanup() {
        QuarkusTransaction.requiringNew().run(() -> {
            Participation.deleteById(participationId);
            TeamFormationFixtures.delete(new TeamFormationFixtures.Ids(teamId, teamFormationId, competitionId));
        });
    }

    private MatchExtractionRow fullyResolvableRow() {
        Participation participation = Participation.findById(participationId);
        TeamSideResolution home = new TeamSideResolution("Home FC", null, participation);
        TeamSideResolution away = new TeamSideResolution("Away FC", null, participation);
        ScrapedMatch scrapedMatch = new ScrapedMatch(
                new ScrapedTeamLineup("Home FC", List.of(), List.of()),
                new ScrapedTeamLineup("Away FC", List.of(), List.of()),
                (short) 1, (short) 0, List.of(), List.of());

        Player resolvedPlayer = new Player();
        PlayerRowResolution resolved = new PlayerRowResolution(9, "Ivan Petrov",
                new PlayerMatchingService.MatchResult(resolvedPlayer, null));
        PlayerRowResolution ambiguous = new PlayerRowResolution(7, "Georgi Ivanov", null);

        return new MatchExtractionRow(new BfuFixture(MATCH_URL, "Home FC", "Away FC"),
                com.nosoftskills.lineup.model.ExternalRefSource.BFU_TOURNAMENTS, scrapedMatch, null,
                home, away, List.of(resolved), List.of(), List.of(ambiguous), List.of());
    }

    @Test
    void unauthenticatedGetRedirectsToLogin() {
        given().redirects().follow(false)
                .when().get("/matches/extract")
                .then().statusCode(302);
    }

    @Test
    @TestSecurity(user = "user", roles = {"USER"})
    void nonAdminGetForbidden() {
        given().redirects().follow(false)
                .when().get("/matches/extract")
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void adminGetShowsStep1() {
        given().when().get("/matches/extract")
                .then().statusCode(200)
                .body(containsString("extraction-wizard"));
    }

    @Test
    @TestSecurity(user = "user", roles = {"USER"})
    void nonAdminDiscoverForbidden() {
        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("competitionId", competitionId)
                .formParam("fixturesUrl", FIXTURES_URL)
                .formParam("season", SEASON)
                .formParam("date", DATE)
                .when().post("/matches/extract/discover")
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void discoverReturns200WithResolvedAndAmbiguousBreakdown() throws Exception {
        Mockito.when(extractionService.preview(Mockito.any(ExtractionRequest.class)))
                .thenReturn(List.of(fullyResolvableRow()));

        given().contentType(ContentType.URLENC)
                .formParam("competitionId", competitionId)
                .formParam("fixturesUrl", FIXTURES_URL)
                .formParam("season", SEASON)
                .formParam("date", DATE)
                .when().post("/matches/extract/discover")
                .then().statusCode(200)
                .body(containsString("Home FC"))
                .body(containsString("Away FC"))
                .body(containsString("extraction-wizard"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void discoverPassesRequestFieldsToService() throws Exception {
        ArgumentCaptor<ExtractionRequest> captor = ArgumentCaptor.forClass(ExtractionRequest.class);
        Mockito.when(extractionService.preview(captor.capture())).thenReturn(List.of());

        given().contentType(ContentType.URLENC)
                .formParam("competitionId", competitionId)
                .formParam("fixturesUrl", FIXTURES_URL)
                .formParam("season", SEASON)
                .formParam("date", DATE)
                .when().post("/matches/extract/discover")
                .then().statusCode(200);

        ExtractionRequest captured = captor.getValue();
        assertEquals(competitionId, captured.competitionId());
        assertEquals(FIXTURES_URL, captured.fixturesUrl());
        assertEquals(SEASON, captured.season());
        assertEquals(DATE, captured.date().toString());
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void discoverUnknownCompetitionReturns404() {
        given().contentType(ContentType.URLENC)
                .formParam("competitionId", 999_999_999L)
                .formParam("fixturesUrl", FIXTURES_URL)
                .formParam("season", SEASON)
                .formParam("date", DATE)
                .when().post("/matches/extract/discover")
                .then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void discoverScraperErrorReturns400() throws Exception {
        Mockito.when(extractionService.preview(Mockito.any(ExtractionRequest.class)))
                .thenThrow(new BfuScraperException("results page unreachable"));

        given().contentType(ContentType.URLENC)
                .formParam("competitionId", competitionId)
                .formParam("fixturesUrl", FIXTURES_URL)
                .formParam("season", SEASON)
                .formParam("date", DATE)
                .when().post("/matches/extract/discover")
                .then().statusCode(400);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void reviewReconstructsRowsFromHiddenFieldsWithoutCallingService() {
        given().contentType(ContentType.URLENC)
                .formParam("competitionId", competitionId)
                .formParam("fixturesUrl", FIXTURES_URL)
                .formParam("season", SEASON)
                .formParam("date", DATE)
                .formParam("matchUrl", MATCH_URL)
                .formParam("homeTeamName", "Home FC")
                .formParam("homeResolved", "true")
                .formParam("awayTeamName", "Away FC")
                .formParam("awayResolved", "true")
                .formParam("fullyResolvable", "true")
                .formParam("extractionError", "")
                .formParam("resolvedPlayerCount", "1")
                .formParam("ambiguousPlayerCount", "1")
                .formParam("totalPlayerCount", "2")
                .when().post("/matches/extract/review")
                .then().statusCode(200)
                .body(containsString("Home FC"))
                .body(containsString("Away FC"));

        Mockito.verifyNoInteractions(extractionService);
    }

    @Test
    @TestSecurity(user = "user", roles = {"USER"})
    void nonAdminConfirmForbidden() {
        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("competitionId", competitionId)
                .formParam("fixturesUrl", FIXTURES_URL)
                .formParam("season", SEASON)
                .formParam("date", DATE)
                .when().post("/matches/extract/confirm")
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void confirmHappyPathRedirectsToMatches() throws Exception {
        Mockito.when(extractionService.confirm(Mockito.any(ExtractionRequest.class)))
                .thenReturn(new MatchExtractionSummary(1, 0, 0));

        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("competitionId", competitionId)
                .formParam("fixturesUrl", FIXTURES_URL)
                .formParam("season", SEASON)
                .formParam("date", DATE)
                .when().post("/matches/extract/confirm")
                .then().statusCode(303)
                .header("Location", endsWith("/matches"));

        Mockito.verify(extractionService).confirm(
                new ExtractionRequest(competitionId, FIXTURES_URL, SEASON, java.time.LocalDate.parse(DATE)));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void confirmUnknownCompetitionReturns404() {
        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("competitionId", 999_999_999L)
                .formParam("fixturesUrl", FIXTURES_URL)
                .formParam("season", SEASON)
                .formParam("date", DATE)
                .when().post("/matches/extract/confirm")
                .then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void confirmScraperErrorReturns400() throws Exception {
        Mockito.when(extractionService.confirm(Mockito.any(ExtractionRequest.class)))
                .thenThrow(new BfuScraperException("match page unreachable"));

        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("competitionId", competitionId)
                .formParam("fixturesUrl", FIXTURES_URL)
                .formParam("season", SEASON)
                .formParam("date", DATE)
                .when().post("/matches/extract/confirm")
                .then().statusCode(400);
    }
}
