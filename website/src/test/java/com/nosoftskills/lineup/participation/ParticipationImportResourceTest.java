package com.nosoftskills.lineup.participation;

import com.nosoftskills.lineup.model.Competition;
import com.nosoftskills.lineup.model.FormationType;
import com.nosoftskills.lineup.model.Participation;
import com.nosoftskills.lineup.model.Team;
import com.nosoftskills.lineup.model.TeamFormation;
import com.nosoftskills.lineup.scraping.BfuLeagueScraperService;
import com.nosoftskills.lineup.scraping.BfuScraperException;
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
            Team t = new Team();
            t.name = "Import Test Team";
            t.location = "Import City";
            t.persist();
            teamId = t.id;

            TeamFormation tf = new TeamFormation();
            tf.team = t;
            tf.type = FormationType.FIRST;
            tf.persist();
            teamFormationId = tf.id;

            Competition c = new Competition();
            c.name = "Import Test League";
            c.persist();
            competitionId = c.id;
        });
    }

    @AfterEach
    void cleanup() {
        QuarkusTransaction.requiringNew().run(() -> {
            Participation.delete("teamFormation.team.id", teamId);
            TeamFormation.delete("team.id", teamId);
            Team.deleteById(teamId);
            Competition.deleteById(competitionId);
        });
    }

    @Test
    void unauthenticatedGetRedirectsToLogin() {
        given().redirects().follow(false)
                .when().get("/participations/import")
                .then().statusCode(302);
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
                .body(containsString(String.valueOf(teamFormationId)));
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
