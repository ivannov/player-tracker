package com.nosoftskills.lineup.resource;

import com.nosoftskills.lineup.model.Competition;
import com.nosoftskills.lineup.model.FormationType;
import com.nosoftskills.lineup.model.Match;
import com.nosoftskills.lineup.model.Participation;
import com.nosoftskills.lineup.model.TeamFormation;
import com.nosoftskills.lineup.testsupport.TeamFormationFixtures;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;

@QuarkusTest
class ParticipationResourceTest {

    private Long teamId;
    private Long teamFormationId;
    private Long competitionId;
    private Long createdParticipationId;

    @BeforeEach
    void setup() {
        QuarkusTransaction.requiringNew().run(() -> {
            TeamFormationFixtures.Ids ids = TeamFormationFixtures.create(
                    "Participation Test Team", "Test City", FormationType.U15, "Participation Test League");
            teamId = ids.teamId();
            teamFormationId = ids.teamFormationId();
            competitionId = ids.competitionId();
        });
    }

    @AfterEach
    void cleanup() {
        QuarkusTransaction.requiringNew().run(() -> {
            if (createdParticipationId != null) {
                Participation.deleteById(createdParticipationId);
                createdParticipationId = null;
            }
            TeamFormationFixtures.delete(new TeamFormationFixtures.Ids(teamId, teamFormationId, competitionId));
        });
    }

    @Test
    void listReturns200Anonymously() {
        given().when().get("/participations")
                .then().statusCode(200)
                .body(not(containsString("hx-delete")), not(containsString("/participations/new")));
    }

    @Test
    @TestSecurity(user = "user", roles = {"USER"})
    void listReturns200() {
        given().when().get("/participations")
                .then().statusCode(200)
                .body(not(containsString("hx-delete")), not(containsString("/participations/new")));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void listShowsAdminControlsForAdmin() {
        given().when().get("/participations")
                .then().statusCode(200)
                .body(containsString("/participations/new"));
    }

    @Test
    void newFormRedirectsAnonymousToLogin() {
        given().redirects().follow(false)
                .when().get("/participations/new")
                .then().statusCode(302);
    }

    @Test
    @TestSecurity(user = "user", roles = {"USER"})
    void newFormForbiddenForUser() {
        given().redirects().follow(false)
                .when().get("/participations/new")
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void newFormReturns200() {
        given().when().get("/participations/new")
                .then().statusCode(200);
    }

    @Test
    @TestSecurity(user = "user", roles = {"USER"})
    void createForbiddenForUser() {
        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("teamId", teamId)
                .formParam("competitionId", competitionId)
                .formParam("season", "2024/2025")
                .formParam("formationTypes", "U15")
                .when().post("/participations")
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void createRedirectsToList() {
        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("teamId", teamId)
                .formParam("competitionId", competitionId)
                .formParam("season", "2024/2025")
                .formParam("formationTypes", "U15")
                .when().post("/participations")
                .then().statusCode(303)
                .header("Location", endsWith("/participations"));

        createdParticipationId = QuarkusTransaction.requiringNew().call(() ->
                Participation.<Participation>find(
                        "teamFormation.team.id = ?1 and season = ?2", teamId, "2024/2025"
                ).firstResult().id);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void createDuplicateReturnsFriendlyErrorNotServerError() {
        createdParticipationId = insertParticipation("2024/2025");

        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("teamId", teamId)
                .formParam("competitionId", competitionId)
                .formParam("season", "2024/2025")
                .formParam("formationTypes", "U15")
                .when().post("/participations")
                .then().statusCode(422)
                .body(containsString("вече съществува"), not(containsString("ConstraintViolationException")));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void createOversizedSeasonReturnsFriendlyErrorNotServerError() {
        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("teamId", teamId)
                .formParam("competitionId", competitionId)
                .formParam("season", "2".repeat(300))
                .formParam("formationTypes", "U15")
                .when().post("/participations")
                .then().statusCode(422)
                .body(containsString("Невалиден сезон"), not(containsString("DataException")));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void editFormReturns200() {
        createdParticipationId = insertParticipation("2023/2024");
        given().when().get("/participations/" + createdParticipationId + "/edit")
                .then().statusCode(200);
    }

    @Test
    void editFormRedirectsAnonymousToLogin() {
        createdParticipationId = insertParticipation("2021/2022");
        given().redirects().follow(false)
                .when().get("/participations/" + createdParticipationId + "/edit")
                .then().statusCode(302);
    }

    @Test
    @TestSecurity(user = "user", roles = {"USER"})
    void editFormForbiddenForUser() {
        createdParticipationId = insertParticipation("2018/2019");
        given().redirects().follow(false)
                .when().get("/participations/" + createdParticipationId + "/edit")
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void updateRedirectsToList() {
        createdParticipationId = insertParticipation("2022/2023");
        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("formationTypes", "U15")
                .when().post("/participations/" + createdParticipationId)
                .then().statusCode(303)
                .header("Location", endsWith("/participations"));
    }

    @Test
    @TestSecurity(user = "user", roles = {"USER"})
    void deleteForbiddenForUser() {
        createdParticipationId = insertParticipation("2020/2021");
        given().redirects().follow(false)
                .when().delete("/participations/" + createdParticipationId)
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void deleteReturns204() {
        Long id = insertParticipation("2019/2020");
        given().redirects().follow(false)
                .when().delete("/participations/" + id)
                .then().statusCode(204);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void deleteReferencedByMatchReturnsConflictNotServerError() {
        Long homeId = insertParticipation("2017/2018");
        Long awayId = insertParticipation("2016/2017");
        Long matchId = QuarkusTransaction.requiringNew().call(() -> {
            Match m = new Match();
            m.homeTeam = Participation.findById(homeId);
            m.awayTeam = Participation.findById(awayId);
            m.date = LocalDate.now();
            m.persist();
            return m.id;
        });

        try {
            given().redirects().follow(false)
                    .when().delete("/participations/" + homeId)
                    .then().statusCode(409)
                    .body(not(containsString("Exception")), not(containsString("nosoftskills")));
        } finally {
            QuarkusTransaction.requiringNew().run(() -> Match.deleteById(matchId));
            QuarkusTransaction.requiringNew().run(() -> {
                Participation.deleteById(homeId);
                Participation.deleteById(awayId);
            });
        }
    }

    private Long insertParticipation(String season) {
        return QuarkusTransaction.requiringNew().call(() -> {
            Participation p = new Participation();
            p.teamFormation = TeamFormation.findById(teamFormationId);
            p.competition = Competition.findById(competitionId);
            p.season = season;
            p.persist();
            return p.id;
        });
    }
}
