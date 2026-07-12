package com.nosoftskills.lineup.participation;

import com.nosoftskills.lineup.model.Competition;
import com.nosoftskills.lineup.model.FormationType;
import com.nosoftskills.lineup.model.Participation;
import com.nosoftskills.lineup.model.Team;
import com.nosoftskills.lineup.model.TeamFormation;
import com.nosoftskills.lineup.scraping.BfuLeagueScraperService;
import com.nosoftskills.lineup.scraping.BfuScraperException;
import com.nosoftskills.lineup.testsupport.TeamFormationFixtures;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class ParticipationImportResourceTest {

    @InjectMock
    BfuLeagueScraperService scraperService;

    private Long teamId;
    private Long teamFormationId;
    private Long competitionId;

    @BeforeEach
    void setup() {
        QuarkusTransaction.requiringNew().run(() -> {
            TeamFormationFixtures.Ids ids = TeamFormationFixtures.create(
                    "Import Test Team", "Import City", FormationType.FIRST, "Import Test League");
            teamId = ids.teamId();
            teamFormationId = ids.teamFormationId();
            competitionId = ids.competitionId();
        });
    }

    @AfterEach
    void cleanup() {
        QuarkusTransaction.requiringNew().run(() ->
                TeamFormationFixtures.delete(new TeamFormationFixtures.Ids(teamId, teamFormationId, competitionId)));
    }

    @Test
    void unauthenticatedGetRedirectsToLogin() {
        given().redirects().follow(false)
                .when().get("/participations/import")
                .then().statusCode(302);
    }

    @Test
    @TestSecurity(user = "user", roles = {"USER"})
    void nonAdminGetForbidden() {
        given().redirects().follow(false)
                .when().get("/participations/import")
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "user", roles = {"USER"})
    void nonAdminExtractForbidden() {
        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("url", "https://bfu-tournaments.com/test")
                .formParam("competitionId", competitionId)
                .formParam("season", "2024/2025")
                .when().post("/participations/import/extract")
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void extractReturns200WithMatchedAndUnmatchedRows() throws BfuScraperException {
        Mockito.when(scraperService.extractTeamNames(Mockito.anyString()))
                .thenReturn(List.of("import test team", "Unknown FC"));

        given().contentType(ContentType.URLENC)
                .formParam("url", "https://bfu-tournaments.com/test")
                .formParam("competitionId", competitionId)
                .formParam("season", "2024/2025")
                .when().post("/participations/import/extract")
                .then().statusCode(200)
                .body(containsString("import test team"))
                .body(containsString("Unknown FC"))
                .body(containsString("import-wizard"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void formationsReturnsSelectForTeam() {
        given().contentType(ContentType.URLENC)
                .formParam("teamId", teamId)
                .when().post("/participations/import/formations")
                .then().statusCode(200)
                .body(containsString("formationTypeId"))
                .body(containsString(String.valueOf(teamFormationId)))
                .body(containsString("newFormationType"));
    }

    @Test
    @TestSecurity(user = "user", roles = {"USER"})
    void nonAdminReviewForbidden() {
        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("competitionId", competitionId)
                .formParam("season", "2024/2025")
                .when().post("/participations/import/review")
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void reviewExistingFormationMarkedAsExists() {
        QuarkusTransaction.requiringNew().run(() -> {
            Participation p = new Participation();
            p.teamFormation = TeamFormation.findById(teamFormationId);
            p.competition = Competition.findById(competitionId);
            p.season = "2023/2024";
            p.persist();
        });

        given().contentType(ContentType.URLENC)
                .formParam("competitionId", competitionId)
                .formParam("season", "2023/2024")
                .formParam("scrapedName", "Import Test Team")
                .formParam("teamId", teamId)
                .formParam("teamName", "")
                .formParam("teamLocation", "")
                .formParam("formationTypeId", teamFormationId)
                .formParam("newFormationType", "")
                .when().post("/participations/import/review")
                .then().statusCode(200)
                .body(containsString("Вече съществува"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void reviewNewFormationMarkedAsCreate() {
        given().contentType(ContentType.URLENC)
                .formParam("competitionId", competitionId)
                .formParam("season", "2030/2031")
                .formParam("scrapedName", "Import Test Team")
                .formParam("teamId", teamId)
                .formParam("teamName", "")
                .formParam("teamLocation", "")
                .formParam("formationTypeId", teamFormationId)
                .formParam("newFormationType", "")
                .when().post("/participations/import/review")
                .then().statusCode(200)
                .body(containsString("Ще се създаде"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void reviewNewTeamAlwaysMarkedAsCreate() {
        given().contentType(ContentType.URLENC)
                .formParam("competitionId", competitionId)
                .formParam("season", "2025/2026")
                .formParam("scrapedName", "New FC")
                .formParam("teamId", "")
                .formParam("teamName", "New FC")
                .formParam("teamLocation", "New City")
                .formParam("formationTypeId", "")
                .formParam("newFormationType", "")
                .when().post("/participations/import/review")
                .then().statusCode(200)
                .body(containsString("Ще се създаде"))
                .body(containsString("Нов: New FC"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void reviewBlankRowMarkedAsSkip() {
        given().contentType(ContentType.URLENC)
                .formParam("competitionId", competitionId)
                .formParam("season", "2025/2026")
                .formParam("scrapedName", "Unknown FC")
                .formParam("teamId", "")
                .formParam("teamName", "")
                .formParam("teamLocation", "")
                .formParam("formationTypeId", "")
                .formParam("newFormationType", "")
                .when().post("/participations/import/review")
                .then().statusCode(200)
                .body(containsString("Липсват данни"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void reviewDoesNotPersistAnything() {
        given().contentType(ContentType.URLENC)
                .formParam("competitionId", competitionId)
                .formParam("season", "2027/2028")
                .formParam("scrapedName", "Dry Run FC")
                .formParam("teamId", "")
                .formParam("teamName", "Dry Run FC")
                .formParam("teamLocation", "Dry City")
                .formParam("formationTypeId", "")
                .formParam("newFormationType", "")
                .when().post("/participations/import/review")
                .then().statusCode(200);

        long teamCount = QuarkusTransaction.requiringNew().call(() ->
                Team.count("name", "Dry Run FC"));
        assertEquals(0, teamCount);

        long participationCount = QuarkusTransaction.requiringNew().call(() ->
                Participation.count("competition.id = ?1 AND season = ?2", competitionId, "2027/2028"));
        assertEquals(0, participationCount);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void reviewFormationBelongingToDifferentTeamMarkedAsSkip() {
        long[] otherTeamId = new long[1];
        long[] otherFormationId = new long[1];
        QuarkusTransaction.requiringNew().run(() -> {
            Team other = new Team();
            other.name = "Other Import Team";
            other.location = "Other City";
            other.persist();
            otherTeamId[0] = other.id;

            TeamFormation otf = new TeamFormation();
            otf.team = other;
            otf.type = FormationType.SECOND;
            otf.persist();
            otherFormationId[0] = otf.id;
        });

        try {
            given().contentType(ContentType.URLENC)
                    .formParam("competitionId", competitionId)
                    .formParam("season", "2025/2026")
                    .formParam("scrapedName", "Import Test Team")
                    .formParam("teamId", teamId)
                    .formParam("teamName", "")
                    .formParam("teamLocation", "")
                    .formParam("formationTypeId", otherFormationId[0])
                    .formParam("newFormationType", "")
                    .when().post("/participations/import/review")
                    .then().statusCode(200)
                    .body(containsString("Невалидна формация"));
        } finally {
            QuarkusTransaction.requiringNew().run(() -> {
                TeamFormation.delete("team.id", otherTeamId[0]);
                Team.deleteById(otherTeamId[0]);
            });
        }
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void reviewInvalidTeamIdDoesNotCrash() {
        given().contentType(ContentType.URLENC)
                .formParam("competitionId", competitionId)
                .formParam("season", "2025/2026")
                .formParam("scrapedName", "Import Test Team")
                .formParam("teamId", "not-a-number")
                .formParam("teamName", "")
                .formParam("teamLocation", "")
                .formParam("formationTypeId", "")
                .formParam("newFormationType", "")
                .when().post("/participations/import/review")
                .then().statusCode(200)
                .body(containsString("Невалиден отбор"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void reviewInvalidFormationTypeIdDoesNotCrash() {
        given().contentType(ContentType.URLENC)
                .formParam("competitionId", competitionId)
                .formParam("season", "2025/2026")
                .formParam("scrapedName", "Import Test Team")
                .formParam("teamId", teamId)
                .formParam("teamName", "")
                .formParam("teamLocation", "")
                .formParam("formationTypeId", "not-a-number")
                .formParam("newFormationType", "")
                .when().post("/participations/import/review")
                .then().statusCode(200)
                .body(containsString("Невалидна формация"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void reviewInvalidNewFormationTypeDoesNotCrash() {
        given().contentType(ContentType.URLENC)
                .formParam("competitionId", competitionId)
                .formParam("season", "2025/2026")
                .formParam("scrapedName", "Import Test Team")
                .formParam("teamId", teamId)
                .formParam("teamName", "")
                .formParam("teamLocation", "")
                .formParam("formationTypeId", "")
                .formParam("newFormationType", "NOT_A_REAL_TYPE")
                .when().post("/participations/import/review")
                .then().statusCode(200)
                .body(containsString("Невалидна формация"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void reviewExplicitNewFormationSentinelMarkedAsCreate() {
        given().contentType(ContentType.URLENC)
                .formParam("competitionId", competitionId)
                .formParam("season", "2025/2026")
                .formParam("scrapedName", "Import Test Team")
                .formParam("teamId", teamId)
                .formParam("teamName", "")
                .formParam("teamLocation", "")
                .formParam("formationTypeId", "new")
                .formParam("newFormationType", "U15")
                .when().post("/participations/import/review")
                .then().statusCode(200)
                .body(containsString("Ще се създаде"))
                .body(containsString("нова формация"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void saveHappyPathCreatesParticipationAndRedirects() {
        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("competitionId", competitionId)
                .formParam("season", "2024-2025")
                .formParam("scrapedName", "Import Test Team")
                .formParam("teamId", teamId)
                .formParam("teamName", "")
                .formParam("teamLocation", "")
                .formParam("formationTypeId", teamFormationId)
                .formParam("newFormationType", "")
                .when().post("/participations/import/save")
                .then().statusCode(303)
                .header("Location", endsWith("/participations"));

        long count = QuarkusTransaction.requiringNew().call(() ->
                Participation.count("teamFormation.id = ?1 AND season = ?2",
                        teamFormationId, "2024/2025"));
        assertEquals(1, count);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void saveDuplicateIsSkippedSilently() {
        QuarkusTransaction.requiringNew().run(() -> {
            Participation p = new Participation();
            p.teamFormation = TeamFormation.findById(teamFormationId);
            p.competition = Competition.findById(competitionId);
            p.season = "2023/2024";
            p.persist();
        });

        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("competitionId", competitionId)
                .formParam("season", "2023/2024")
                .formParam("scrapedName", "Import Test Team")
                .formParam("teamId", teamId)
                .formParam("teamName", "")
                .formParam("teamLocation", "")
                .formParam("formationTypeId", teamFormationId)
                .formParam("newFormationType", "")
                .when().post("/participations/import/save")
                .then().statusCode(303);

        long count = QuarkusTransaction.requiringNew().call(() ->
                Participation.count("teamFormation.id = ?1 AND season = ?2",
                        teamFormationId, "2023/2024"));
        assertEquals(1, count);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void saveFormationBelongingToDifferentTeamIsSkipped() {
        long[] otherTeamId = new long[1];
        long[] otherFormationId = new long[1];
        QuarkusTransaction.requiringNew().run(() -> {
            Team other = new Team();
            other.name = "Other Save Team";
            other.location = "Other City";
            other.persist();
            otherTeamId[0] = other.id;

            TeamFormation otf = new TeamFormation();
            otf.team = other;
            otf.type = FormationType.SECOND;
            otf.persist();
            otherFormationId[0] = otf.id;
        });

        try {
            given().redirects().follow(false)
                    .contentType(ContentType.URLENC)
                    .formParam("competitionId", competitionId)
                    .formParam("season", "2025/2026")
                    .formParam("scrapedName", "Import Test Team")
                    .formParam("teamId", teamId)
                    .formParam("teamName", "")
                    .formParam("teamLocation", "")
                    .formParam("formationTypeId", otherFormationId[0])
                    .formParam("newFormationType", "")
                    .when().post("/participations/import/save")
                    .then().statusCode(303);

            long count = QuarkusTransaction.requiringNew().call(() ->
                    Participation.count("competition.id = ?1 AND season = ?2", competitionId, "2025/2026"));
            assertEquals(0, count);
        } finally {
            QuarkusTransaction.requiringNew().run(() -> {
                TeamFormation.delete("team.id", otherTeamId[0]);
                Team.deleteById(otherTeamId[0]);
            });
        }
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void saveCreatesNewTeamAndParticipation() {
        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("competitionId", competitionId)
                .formParam("season", "2025/2026")
                .formParam("scrapedName", "New FC")
                .formParam("teamId", "")
                .formParam("teamName", "New FC")
                .formParam("teamLocation", "New City")
                .formParam("formationTypeId", "")
                .formParam("newFormationType", "")
                .when().post("/participations/import/save")
                .then().statusCode(303);

        long participations = QuarkusTransaction.requiringNew().call(() ->
                Participation.count("competition.id = ?1 AND season = ?2",
                        competitionId, "2025/2026"));
        assertEquals(1, participations);

        QuarkusTransaction.requiringNew().run(() -> {
            Team newTeam = Team.<Team>find("name", "New FC").firstResult();
            Participation.delete("teamFormation.team.id", newTeam.id);
            TeamFormation.delete("team.id", newTeam.id);
            newTeam.delete();
        });
    }
}
