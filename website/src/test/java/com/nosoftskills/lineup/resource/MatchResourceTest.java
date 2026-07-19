package com.nosoftskills.lineup.resource;

import com.nosoftskills.lineup.model.Competition;
import com.nosoftskills.lineup.model.FormationType;
import com.nosoftskills.lineup.model.Match;
import com.nosoftskills.lineup.model.Participation;
import com.nosoftskills.lineup.model.Team;
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
import static org.hamcrest.Matchers.not;

@QuarkusTest
class MatchResourceTest {

    private TeamFormationFixtures.Ids mainFixture;
    private TeamFormationFixtures.Ids otherFixture;
    private Long awayTeamId;
    private Long awayTeamFormationId;
    private Long otherAwayTeamId;
    private Long otherAwayTeamFormationId;
    private Long homeParticipationId;
    private Long awayParticipationId;
    private Long otherParticipationId;
    private Long matchId;
    private Long createdMatchId;

    @BeforeEach
    void setup() {
        QuarkusTransaction.requiringNew().run(() -> {
            mainFixture = TeamFormationFixtures.create(
                    "Match Test Home", "Home City", FormationType.U15, "Match Test League");
            otherFixture = TeamFormationFixtures.create(
                    "Match Test Other Home", "Other City", FormationType.U15, "Match Test Other League");

            Team awayTeam = new Team();
            awayTeam.name = "Match Test Away";
            awayTeam.location = "Away City";
            awayTeam.persist();
            awayTeamId = awayTeam.id;

            TeamFormation awayTf = new TeamFormation();
            awayTf.team = awayTeam;
            awayTf.type = FormationType.U15;
            awayTf.persist();
            awayTeamFormationId = awayTf.id;

            Team otherAwayTeam = new Team();
            otherAwayTeam.name = "Match Test Other Away";
            otherAwayTeam.location = "Other Away City";
            otherAwayTeam.persist();
            otherAwayTeamId = otherAwayTeam.id;

            TeamFormation otherAwayTf = new TeamFormation();
            otherAwayTf.team = otherAwayTeam;
            otherAwayTf.type = FormationType.U15;
            otherAwayTf.persist();
            otherAwayTeamFormationId = otherAwayTf.id;

            Participation home = new Participation();
            home.teamFormation = TeamFormation.findById(mainFixture.teamFormationId());
            home.competition = Competition.findById(mainFixture.competitionId());
            home.season = "2023/2024";
            home.persist();
            homeParticipationId = home.id;

            Participation away = new Participation();
            away.teamFormation = awayTf;
            away.competition = Competition.findById(mainFixture.competitionId());
            away.season = "2023/2024";
            away.persist();
            awayParticipationId = away.id;

            Participation other = new Participation();
            other.teamFormation = otherAwayTf;
            other.competition = Competition.findById(otherFixture.competitionId());
            other.season = "2024/2025";
            other.persist();
            otherParticipationId = other.id;

            Match m = new Match();
            m.homeTeam = home;
            m.awayTeam = away;
            m.date = LocalDate.of(2023, 10, 1);
            m.homeScore = 2;
            m.awayScore = 1;
            m.persist();
            matchId = m.id;
        });
    }

    @AfterEach
    void cleanup() {
        QuarkusTransaction.requiringNew().run(() -> {
            if (createdMatchId != null) {
                Match.delete("id", createdMatchId);
                createdMatchId = null;
            }
            Match.delete("id", matchId);
            Participation.delete("id", homeParticipationId);
            Participation.delete("id", awayParticipationId);
            Participation.delete("id", otherParticipationId);
            TeamFormation.delete("id", awayTeamFormationId);
            TeamFormation.delete("id", otherAwayTeamFormationId);
            Team.delete("id", awayTeamId);
            Team.delete("id", otherAwayTeamId);
            TeamFormationFixtures.delete(mainFixture);
            TeamFormationFixtures.delete(otherFixture);
        });
    }

    @Test
    void listReturns200Anonymously() {
        given().when().get("/matches")
                .then().statusCode(200)
                .body(not(containsString("/matches/new")),
                        containsString("Match Test Home"),
                        containsString("Match Test Away"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void listShowsAdminControlsForAdmin() {
        given().when().get("/matches")
                .then().statusCode(200)
                .body(containsString("/matches/new"));
    }

    @Test
    void listFiltersByCompetition() {
        given().queryParam("competitionId", mainFixture.competitionId())
                .when().get("/matches")
                .then().statusCode(200)
                .body(containsString("Match Test Home"));

        given().queryParam("competitionId", otherFixture.competitionId())
                .when().get("/matches")
                .then().statusCode(200)
                .body(not(containsString("Match Test Home")));
    }

    @Test
    void listFiltersByDate() {
        given().queryParam("date", "2023-10-01")
                .when().get("/matches")
                .then().statusCode(200)
                .body(containsString("Match Test Home"));

        given().queryParam("date", "2099-01-01")
                .when().get("/matches")
                .then().statusCode(200)
                .body(not(containsString("Match Test Home")));
    }

    @Test
    void detailReturns200Anonymously() {
        given().when().get("/matches/" + matchId)
                .then().statusCode(200)
                .body(containsString("Match Test Home"),
                        containsString("Match Test Away"),
                        containsString("Match Test League"),
                        containsString("2023/2024"));
    }

    @Test
    void detailReturns404ForUnknownId() {
        given().when().get("/matches/999999999")
                .then().statusCode(404);
    }

    @Test
    void newFormRedirectsAnonymousToLogin() {
        given().redirects().follow(false)
                .when().get("/matches/new")
                .then().statusCode(302);
    }

    @Test
    @TestSecurity(user = "user", roles = {"USER"})
    void newFormForbiddenForUser() {
        given().redirects().follow(false)
                .when().get("/matches/new")
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void newFormReturns200() {
        given().when().get("/matches/new")
                .then().statusCode(200)
                .body(containsString("Match Test Home"));
    }

    @Test
    @TestSecurity(user = "user", roles = {"USER"})
    void createForbiddenForUser() {
        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("homeParticipationId", homeParticipationId)
                .formParam("awayParticipationId", awayParticipationId)
                .formParam("date", "2023-11-01")
                .when().post("/matches")
                .then().statusCode(403);
    }

    @Test
    void createRedirectsAnonymousToLogin() {
        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("homeParticipationId", homeParticipationId)
                .formParam("awayParticipationId", awayParticipationId)
                .formParam("date", "2023-11-01")
                .when().post("/matches")
                .then().statusCode(302);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void createRedirectsToDetailWithScore() {
        String location = given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("homeParticipationId", homeParticipationId)
                .formParam("awayParticipationId", awayParticipationId)
                .formParam("date", "2023-11-01")
                .formParam("homeScore", "3")
                .formParam("awayScore", "0")
                .when().post("/matches")
                .then().statusCode(303)
                .extract().header("Location");

        org.junit.jupiter.api.Assertions.assertTrue(location.contains("/matches/"));
        createdMatchId = Long.valueOf(location.substring(location.lastIndexOf('/') + 1));

        QuarkusTransaction.requiringNew().run(() -> {
            Match created = Match.findById(createdMatchId);
            org.junit.jupiter.api.Assertions.assertEquals((short) 3, created.homeScore);
            org.junit.jupiter.api.Assertions.assertEquals((short) 0, created.awayScore);
        });
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void createWithoutScoreLeavesScoreNull() {
        String location = given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("homeParticipationId", homeParticipationId)
                .formParam("awayParticipationId", awayParticipationId)
                .formParam("date", "2023-11-08")
                .when().post("/matches")
                .then().statusCode(303)
                .extract().header("Location");

        createdMatchId = Long.valueOf(location.substring(location.lastIndexOf('/') + 1));

        QuarkusTransaction.requiringNew().run(() -> {
            Match created = Match.findById(createdMatchId);
            org.junit.jupiter.api.Assertions.assertNull(created.homeScore);
            org.junit.jupiter.api.Assertions.assertNull(created.awayScore);
        });
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void createRejectsMismatchedCompetitionAndSeason() {
        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("homeParticipationId", homeParticipationId)
                .formParam("awayParticipationId", otherParticipationId)
                .formParam("date", "2023-11-01")
                .when().post("/matches")
                .then().statusCode(422)
                .body(containsString("една и съща"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void createRejectsSameParticipationOnBothSides() {
        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("homeParticipationId", homeParticipationId)
                .formParam("awayParticipationId", homeParticipationId)
                .formParam("date", "2023-11-01")
                .when().post("/matches")
                .then().statusCode(422);
    }
}
