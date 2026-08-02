package com.nosoftskills.lineup.resource;

import com.nosoftskills.lineup.model.Competition;
import com.nosoftskills.lineup.model.FormationType;
import com.nosoftskills.lineup.model.Match;
import com.nosoftskills.lineup.model.MatchEvent;
import com.nosoftskills.lineup.model.Participation;
import com.nosoftskills.lineup.model.Player;
import com.nosoftskills.lineup.model.PlayerAppearance;
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
import static org.hamcrest.MatcherAssert.assertThat;
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
    private Long existingPlayerId;
    private Long createdAppearanceId;
    private Long createdPlayerId;
    private Long createdEventId;

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

            Player player = new Player();
            player.names = "Match Test Existing Player";
            player.persist();
            existingPlayerId = player.id;
        });
    }

    @AfterEach
    void cleanup() {
        QuarkusTransaction.requiringNew().run(() -> {
            if (createdEventId != null) {
                MatchEvent.delete("id", createdEventId);
                createdEventId = null;
            }
            if (createdAppearanceId != null) {
                PlayerAppearance.delete("id", createdAppearanceId);
                createdAppearanceId = null;
            }
            if (createdPlayerId != null) {
                Player.delete("id", createdPlayerId);
                createdPlayerId = null;
            }
            Player.delete("id", existingPlayerId);
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

    @Test
    void addAppearanceRedirectsAnonymousToLogin() {
        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("participationId", homeParticipationId)
                .formParam("playerId", existingPlayerId)
                .when().post("/matches/" + matchId + "/appearances")
                .then().statusCode(302);
    }

    @Test
    @TestSecurity(user = "user", roles = {"USER"})
    void addAppearanceForbiddenForUser() {
        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("participationId", homeParticipationId)
                .formParam("playerId", existingPlayerId)
                .when().post("/matches/" + matchId + "/appearances")
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void addAppearanceWithExistingPlayerAddsToLineup() {
        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("participationId", homeParticipationId)
                .formParam("playerId", existingPlayerId)
                .formParam("starter", "true")
                .formParam("number", "9")
                .when().post("/matches/" + matchId + "/appearances")
                .then().statusCode(303)
                .header("Location", org.hamcrest.Matchers.endsWith("/matches/" + matchId));

        createdAppearanceId = QuarkusTransaction.requiringNew().call(() ->
                PlayerAppearance.<PlayerAppearance>find("player.id = ?1 and match.id = ?2", existingPlayerId, matchId)
                        .firstResult().id);

        QuarkusTransaction.requiringNew().run(() -> {
            PlayerAppearance pa = PlayerAppearance.findById(createdAppearanceId);
            org.junit.jupiter.api.Assertions.assertTrue(pa.starter);
            org.junit.jupiter.api.Assertions.assertEquals((short) 9, pa.number);
            org.junit.jupiter.api.Assertions.assertEquals(homeParticipationId, pa.participation.id);
        });

        given().when().get("/matches/" + matchId)
                .then().statusCode(200)
                .body(containsString("Match Test Existing Player"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void addAppearanceWithNewPlayerNameCreatesPlayerInline() {
        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("participationId", awayParticipationId)
                .formParam("newPlayerName", "Match Test Inline Player")
                .when().post("/matches/" + matchId + "/appearances")
                .then().statusCode(303);

        createdPlayerId = QuarkusTransaction.requiringNew()
                .call(() -> Player.<Player>find("names", "Match Test Inline Player").firstResult().id);
        createdAppearanceId = QuarkusTransaction.requiringNew().call(() ->
                PlayerAppearance.<PlayerAppearance>find("player.id = ?1 and match.id = ?2", createdPlayerId, matchId)
                        .firstResult().id);

        org.junit.jupiter.api.Assertions.assertNotNull(createdPlayerId);
        org.junit.jupiter.api.Assertions.assertNotNull(createdAppearanceId);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void addAppearanceRejectsDuplicatePlayerInSameMatch() {
        createdAppearanceId = QuarkusTransaction.requiringNew().call(() -> {
            PlayerAppearance pa = new PlayerAppearance();
            pa.player = Player.findById(existingPlayerId);
            pa.match = Match.findById(matchId);
            pa.participation = Participation.findById(homeParticipationId);
            pa.starter = false;
            pa.persist();
            return pa.id;
        });

        String location = given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("participationId", awayParticipationId)
                .formParam("playerId", existingPlayerId)
                .when().post("/matches/" + matchId + "/appearances")
                .then().statusCode(303)
                .extract().header("Location");

        assertThat(location, containsString("/matches/" + matchId));
        assertThat(location, containsString("error="));
        given().urlEncodingEnabled(false).when().get(location).then().statusCode(200)
                .body(containsString("Играчът вече е добавен към този мач."));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void addAppearanceRejectsParticipationNotInMatch() {
        String location = given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("participationId", otherParticipationId)
                .formParam("playerId", existingPlayerId)
                .when().post("/matches/" + matchId + "/appearances")
                .then().statusCode(303)
                .extract().header("Location");

        assertThat(location, containsString("/matches/" + matchId));
        assertThat(location, containsString("error="));
        given().urlEncodingEnabled(false).when().get(location).then().statusCode(200)
                .body(containsString("Невалиден отбор за този мач."));
    }

    @Test
    @TestSecurity(user = "user", roles = {"USER"})
    void removeAppearanceForbiddenForUser() {
        createdAppearanceId = insertAppearance(existingPlayerId, homeParticipationId, false, null);

        given().redirects().follow(false)
                .when().delete("/matches/" + matchId + "/appearances/" + createdAppearanceId)
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void removeAppearanceReturns204ForAdmin() {
        Long appearanceId = insertAppearance(existingPlayerId, homeParticipationId, false, null);

        given().redirects().follow(false)
                .when().delete("/matches/" + matchId + "/appearances/" + appearanceId)
                .then().statusCode(204);

        QuarkusTransaction.requiringNew().run(() ->
                org.junit.jupiter.api.Assertions.assertNull(PlayerAppearance.findById(appearanceId)));
    }

    @Test
    @TestSecurity(user = "user", roles = {"USER"})
    void addEventForbiddenForUser() {
        createdAppearanceId = insertAppearance(existingPlayerId, homeParticipationId, true, (short) 9);

        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("type", "GOAL")
                .formParam("minute", "23")
                .when().post("/matches/" + matchId + "/appearances/" + createdAppearanceId + "/events")
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void addEventAddsGoalEvent() {
        createdAppearanceId = insertAppearance(existingPlayerId, homeParticipationId, true, (short) 9);

        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("type", "GOAL")
                .formParam("minute", "23")
                .when().post("/matches/" + matchId + "/appearances/" + createdAppearanceId + "/events")
                .then().statusCode(303);

        createdEventId = QuarkusTransaction.requiringNew()
                .call(() -> MatchEvent.<MatchEvent>find("playerAppearance.id", createdAppearanceId).firstResult().id);

        QuarkusTransaction.requiringNew().run(() -> {
            MatchEvent event = MatchEvent.findById(createdEventId);
            org.junit.jupiter.api.Assertions.assertEquals(
                    com.nosoftskills.lineup.model.MatchEventType.GOAL, event.type);
            org.junit.jupiter.api.Assertions.assertEquals((short) 23, event.minute);
        });

        given().when().get("/matches/" + matchId)
                .then().statusCode(200)
                .body(containsString("Гол"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void addEventRejectsInvalidType() {
        createdAppearanceId = insertAppearance(existingPlayerId, homeParticipationId, true, (short) 9);

        String location = given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("type", "NOT_A_REAL_TYPE")
                .formParam("minute", "23")
                .when().post("/matches/" + matchId + "/appearances/" + createdAppearanceId + "/events")
                .then().statusCode(303)
                .extract().header("Location");

        assertThat(location, containsString("/matches/" + matchId));
        assertThat(location, containsString("error="));
        given().urlEncodingEnabled(false).when().get(location).then().statusCode(200)
                .body(containsString("Невалиден тип събитие."));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void addAppearanceRejectsMissingPlayerSelection() {
        String location = given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("participationId", homeParticipationId)
                .when().post("/matches/" + matchId + "/appearances")
                .then().statusCode(303)
                .extract().header("Location");

        assertThat(location, containsString("/matches/" + matchId));
        assertThat(location, containsString("error="));
        given().urlEncodingEnabled(false).when().get(location).then().statusCode(200)
                .body(containsString("Изберете играч или въведете име за нов играч."));
    }

    @Test
    @TestSecurity(user = "user", roles = {"USER"})
    void removeEventForbiddenForUser() {
        createdAppearanceId = insertAppearance(existingPlayerId, homeParticipationId, true, (short) 9);
        createdEventId = insertEvent(createdAppearanceId, com.nosoftskills.lineup.model.MatchEventType.YELLOW_CARD, (short) 40);

        given().redirects().follow(false)
                .when().delete("/matches/" + matchId + "/events/" + createdEventId)
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void removeEventReturns204ForAdmin() {
        createdAppearanceId = insertAppearance(existingPlayerId, homeParticipationId, true, (short) 9);
        Long eventId = insertEvent(createdAppearanceId, com.nosoftskills.lineup.model.MatchEventType.YELLOW_CARD, (short) 40);

        given().redirects().follow(false)
                .when().delete("/matches/" + matchId + "/events/" + eventId)
                .then().statusCode(204);

        QuarkusTransaction.requiringNew().run(() ->
                org.junit.jupiter.api.Assertions.assertNull(MatchEvent.findById(eventId)));
    }

    @Test
    @TestSecurity(user = "user", roles = {"USER"})
    void updateSubstitutionForbiddenForUser() {
        createdAppearanceId = insertAppearance(existingPlayerId, homeParticipationId, false, null);

        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("substitutedInMinute", "60")
                .when().post("/matches/" + matchId + "/appearances/" + createdAppearanceId + "/substitution")
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void updateSubstitutionSetsMinutes() {
        createdAppearanceId = insertAppearance(existingPlayerId, homeParticipationId, false, null);

        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("substitutedInMinute", "60")
                .when().post("/matches/" + matchId + "/appearances/" + createdAppearanceId + "/substitution")
                .then().statusCode(303);

        QuarkusTransaction.requiringNew().run(() -> {
            PlayerAppearance pa = PlayerAppearance.findById(createdAppearanceId);
            org.junit.jupiter.api.Assertions.assertEquals((short) 60, pa.substitutedInMinute);
            org.junit.jupiter.api.Assertions.assertNull(pa.substitutedOutMinute);
        });
    }

    private Long insertAppearance(Long playerId, Long participationId, boolean starter, Short number) {
        return QuarkusTransaction.requiringNew().call(() -> {
            PlayerAppearance pa = new PlayerAppearance();
            pa.player = Player.findById(playerId);
            pa.match = Match.findById(matchId);
            pa.participation = Participation.findById(participationId);
            pa.starter = starter;
            pa.number = number;
            pa.persist();
            return pa.id;
        });
    }

    private Long insertEvent(Long appearanceId, com.nosoftskills.lineup.model.MatchEventType type, Short minute) {
        return QuarkusTransaction.requiringNew().call(() -> {
            MatchEvent event = new MatchEvent();
            event.playerAppearance = PlayerAppearance.findById(appearanceId);
            event.type = type;
            event.minute = minute;
            event.persist();
            return event.id;
        });
    }
}
