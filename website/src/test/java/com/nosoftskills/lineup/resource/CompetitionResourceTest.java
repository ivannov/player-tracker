package com.nosoftskills.lineup.resource;

import com.nosoftskills.lineup.model.Competition;
import com.nosoftskills.lineup.model.CompetitionExtractionConfig;
import com.nosoftskills.lineup.model.FormationType;
import com.nosoftskills.lineup.model.Participation;
import com.nosoftskills.lineup.model.TeamFormation;
import com.nosoftskills.lineup.testsupport.TeamFormationFixtures;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
class CompetitionResourceTest {

    private Long createdId;

    @AfterEach
    void cleanup() {
        if (createdId != null) {
            Long id = createdId;
            createdId = null;
            QuarkusTransaction.requiringNew().run(() -> {
                CompetitionExtractionConfig.delete("competition.id", id);
                Competition.deleteById(id);
            });
        }
    }

    @Test
    void listReturns200Anonymously() {
        given().when().get("/competitions")
                .then().statusCode(200)
                .body(not(containsString("hx-delete")), not(containsString("/competitions/new")));
    }

    @Test
    @TestSecurity(user = "user", roles = {"USER"})
    void listReturns200() {
        given().when().get("/competitions")
                .then().statusCode(200)
                .body(not(containsString("hx-delete")), not(containsString("/competitions/new")));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void listShowsAdminControlsForAdmin() {
        given().when().get("/competitions")
                .then().statusCode(200)
                .body(containsString("/competitions/new"));
    }

    @Test
    void newFormRedirectsAnonymousToLogin() {
        given().redirects().follow(false)
                .when().get("/competitions/new")
                .then().statusCode(302);
    }

    @Test
    @TestSecurity(user = "user", roles = {"USER"})
    void newFormForbiddenForUser() {
        given().redirects().follow(false)
                .when().get("/competitions/new")
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void newFormReturns200() {
        given().when().get("/competitions/new")
                .then().statusCode(200);
    }

    @Test
    @TestSecurity(user = "user", roles = {"USER"})
    void createForbiddenForUser() {
        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("name", "Should Not Be Created")
                .when().post("/competitions")
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void createRedirectsToList() {
        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("name", "Test League")
                .formParam("logoUrl", "")
                .when().post("/competitions")
                .then().statusCode(303)
                .header("Location", endsWith("/competitions"));

        createdId = QuarkusTransaction.requiringNew()
                .call(() -> Competition.<Competition>find("name", "Test League").firstResult().id);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void createWithOversizedNameReturnsFriendlyErrorNotServerError() {
        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("name", "x".repeat(2000))
                .when().post("/competitions")
                .then().statusCode(422)
                .body(containsString("Невалидни данни"), not(containsString("DataException")));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void editFormReturns200() {
        createdId = insertCompetition("Edit Test League");
        given().when().get("/competitions/" + createdId + "/edit")
                .then().statusCode(200);
    }

    @Test
    void editFormRedirectsAnonymousToLogin() {
        createdId = insertCompetition("Edit Anon Test League");
        given().redirects().follow(false)
                .when().get("/competitions/" + createdId + "/edit")
                .then().statusCode(302);
    }

    @Test
    @TestSecurity(user = "user", roles = {"USER"})
    void editFormForbiddenForUser() {
        createdId = insertCompetition("Edit Forbidden Test League");
        given().redirects().follow(false)
                .when().get("/competitions/" + createdId + "/edit")
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void updateRedirectsToList() {
        createdId = insertCompetition("Update Test League");
        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("name", "Updated League")
                .when().post("/competitions/" + createdId)
                .then().statusCode(303)
                .header("Location", endsWith("/competitions"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void createWithFixturesUrlAndSeasonPersistsExtractionConfig() {
        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("name", "Configured League")
                .formParam("fixturesUrl", "https://bfu-tournaments.com/leagues/test?view=past-matches")
                .formParam("currentSeason", "2025/2026")
                .when().post("/competitions")
                .then().statusCode(303);

        createdId = QuarkusTransaction.requiringNew()
                .call(() -> Competition.<Competition>find("name", "Configured League").firstResult().id);

        CompetitionExtractionConfig config = QuarkusTransaction.requiringNew().call(() ->
                CompetitionExtractionConfig.<CompetitionExtractionConfig>find("competition.id", createdId).firstResult());
        assertEquals("https://bfu-tournaments.com/leagues/test?view=past-matches", config.fixturesUrl);
        assertEquals("2025/2026", config.currentSeason);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void updateClearingFixturesUrlDeletesExtractionConfig() {
        createdId = insertCompetition("Deconfigure Test League");
        QuarkusTransaction.requiringNew().run(() -> {
            CompetitionExtractionConfig config = new CompetitionExtractionConfig();
            config.competition = Competition.findById(createdId);
            config.fixturesUrl = "https://bfu-tournaments.com/leagues/test?view=past-matches";
            config.currentSeason = "2025/2026";
            config.persist();
        });

        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("name", "Deconfigure Test League")
                .formParam("fixturesUrl", "")
                .formParam("currentSeason", "")
                .when().post("/competitions/" + createdId)
                .then().statusCode(303);

        CompetitionExtractionConfig config = QuarkusTransaction.requiringNew().call(() ->
                CompetitionExtractionConfig.<CompetitionExtractionConfig>find("competition.id", createdId).firstResult());
        assertNull(config);
    }

    @Test
    @TestSecurity(user = "user", roles = {"USER"})
    void deleteForbiddenForUser() {
        createdId = insertCompetition("Delete Forbidden League");
        given().redirects().follow(false)
                .when().delete("/competitions/" + createdId)
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void deleteReturns204() {
        Long id = insertCompetition("Delete Test League");
        given().redirects().follow(false)
                .when().delete("/competitions/" + id)
                .then().statusCode(204);
        // already deleted, no cleanup needed
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void deleteWithParticipationReturnsConflictNotServerError() {
        TeamFormationFixtures.Ids ids = QuarkusTransaction.requiringNew().call(() ->
                TeamFormationFixtures.create("Competition Conflict Team", "Test City",
                        FormationType.U15, "Competition With Participation"));

        Long participationId = QuarkusTransaction.requiringNew().call(() -> {
            Participation p = new Participation();
            p.teamFormation = TeamFormation.findById(ids.teamFormationId());
            p.competition = Competition.findById(ids.competitionId());
            p.season = "2024/2025";
            p.persist();
            return p.id;
        });

        try {
            given().redirects().follow(false)
                    .when().delete("/competitions/" + ids.competitionId())
                    .then().statusCode(409)
                    .body(not(containsString("Exception")), not(containsString("nosoftskills")));
        } finally {
            QuarkusTransaction.requiringNew().run(() -> {
                Participation.deleteById(participationId);
                TeamFormationFixtures.delete(ids);
            });
        }
    }

    private Long insertCompetition(String name) {
        return QuarkusTransaction.requiringNew().call(() -> {
            Competition c = new Competition();
            c.name = name;
            c.persist();
            return c.id;
        });
    }
}
