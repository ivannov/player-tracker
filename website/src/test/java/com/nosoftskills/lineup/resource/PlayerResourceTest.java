package com.nosoftskills.lineup.resource;

import com.nosoftskills.lineup.model.Competition;
import com.nosoftskills.lineup.model.ExternalRefSource;
import com.nosoftskills.lineup.model.FormationType;
import com.nosoftskills.lineup.model.Match;
import com.nosoftskills.lineup.model.MatchEvent;
import com.nosoftskills.lineup.model.MatchEventType;
import com.nosoftskills.lineup.model.Participation;
import com.nosoftskills.lineup.model.Player;
import com.nosoftskills.lineup.model.PlayerAlias;
import com.nosoftskills.lineup.model.PlayerAppearance;
import com.nosoftskills.lineup.model.Team;
import com.nosoftskills.lineup.model.TeamFormation;
import com.nosoftskills.lineup.testsupport.TeamFormationFixtures;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
class PlayerResourceTest {

    private Long createdId;

    @AfterEach
    void cleanup() {
        if (createdId != null) {
            Long id = createdId;
            createdId = null;
            QuarkusTransaction.requiringNew().run(() -> Player.deleteById(id));
        }
    }

    @Test
    void listReturns200Anonymously() {
        given().when().get("/players")
                .then().statusCode(200)
                .body(not(containsString("/players/new")));
    }

    @Test
    @TestSecurity(user = "user", roles = {"USER"})
    void listReturns200() {
        given().when().get("/players")
                .then().statusCode(200)
                .body(not(containsString("/players/new")));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void listShowsAdminControlsForAdmin() {
        given().when().get("/players")
                .then().statusCode(200)
                .body(containsString("/players/new"));
    }

    @Test
    void listFiltersBySearchQuery() {
        createdId = insertPlayer("Searchable Player Xyz");
        given().queryParam("q", "Searchable Player")
                .when().get("/players")
                .then().statusCode(200)
                .body(containsString("Searchable Player Xyz"));

        given().queryParam("q", "NoSuchPlayerAtAll")
                .when().get("/players")
                .then().statusCode(200)
                .body(not(containsString("Searchable Player Xyz")));
    }

    @Test
    void newFormRedirectsAnonymousToLogin() {
        given().redirects().follow(false)
                .when().get("/players/new")
                .then().statusCode(302);
    }

    @Test
    @TestSecurity(user = "user", roles = {"USER"})
    void newFormForbiddenForUser() {
        given().redirects().follow(false)
                .when().get("/players/new")
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void newFormReturns200() {
        given().when().get("/players/new")
                .then().statusCode(200);
    }

    @Test
    @TestSecurity(user = "user", roles = {"USER"})
    void createForbiddenForUser() {
        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("names", "Should Not Be Created")
                .when().post("/players")
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void createRedirectsToList() {
        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("names", "Test Player")
                .when().post("/players")
                .then().statusCode(303)
                .header("Location", endsWith("/players"));

        createdId = QuarkusTransaction.requiringNew()
                .call(() -> Player.<Player>find("names", "Test Player").firstResult().id);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void createWithOversizedNameReturnsFriendlyErrorNotServerError() {
        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("names", "x".repeat(2000))
                .when().post("/players")
                .then().statusCode(422)
                .body(containsString("Невалидни данни"), not(containsString("DataException")));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void editFormReturns200() {
        createdId = insertPlayer("Edit Test Player");
        given().when().get("/players/" + createdId + "/edit")
                .then().statusCode(200);
    }

    @Test
    void editFormRedirectsAnonymousToLogin() {
        createdId = insertPlayer("Edit Anon Test Player");
        given().redirects().follow(false)
                .when().get("/players/" + createdId + "/edit")
                .then().statusCode(302);
    }

    @Test
    @TestSecurity(user = "user", roles = {"USER"})
    void editFormForbiddenForUser() {
        createdId = insertPlayer("Edit Forbidden Test Player");
        given().redirects().follow(false)
                .when().get("/players/" + createdId + "/edit")
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "user", roles = {"USER"})
    void updateForbiddenForUser() {
        createdId = insertPlayer("Update Forbidden Test Player");
        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("names", "Updated Player")
                .when().post("/players/" + createdId)
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void updateRedirectsToList() {
        createdId = insertPlayer("Update Test Player");
        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("names", "Updated Player")
                .when().post("/players/" + createdId)
                .then().statusCode(303)
                .header("Location", endsWith("/players"));
    }

    @Test
    void detailReturns200Anonymously() {
        createdId = insertPlayer("Detail Test Player");
        given().when().get("/players/" + createdId)
                .then().statusCode(200)
                .body(containsString("Detail Test Player"));
    }

    @Test
    void detailReturns404ForUnknownId() {
        given().when().get("/players/999999999")
                .then().statusCode(404);
    }

    @Test
    void detailShowsCareerTimelineAcrossTeamsAndSeasons() {
        TeamFormationFixtures.Ids teamAFixture = QuarkusTransaction.requiringNew()
                .call(() -> TeamFormationFixtures.create(
                        "Career Team Alpha", "Alpha City", FormationType.U15, "Alpha League"));
        TeamFormationFixtures.Ids teamBFixture = QuarkusTransaction.requiringNew()
                .call(() -> TeamFormationFixtures.create(
                        "Career Team Beta", "Beta City", FormationType.U16, "Beta League"));
        TeamFormationFixtures.Ids rivalFixture = QuarkusTransaction.requiringNew()
                .call(() -> TeamFormationFixtures.create(
                        "Career Rival Team", "Rival City", FormationType.U15, "Rival League"));

        try {
            record Fixture(Long playerId, Long homeAId, Long rivalAId, Long homeBId, Long rivalBId,
                    Long matchAId, Long matchBId, Long paAId, Long paBId) {
            }

            Fixture fixture = QuarkusTransaction.requiringNew().call(() -> {
                Player player = new Player();
                player.names = "Career Timeline Player";
                player.persist();

                Participation homeA = new Participation();
                homeA.teamFormation = TeamFormation.findById(teamAFixture.teamFormationId());
                homeA.competition = Competition.findById(teamAFixture.competitionId());
                homeA.season = "2023/2024";
                homeA.persist();

                Participation rivalA = new Participation();
                rivalA.teamFormation = TeamFormation.findById(rivalFixture.teamFormationId());
                rivalA.competition = Competition.findById(teamAFixture.competitionId());
                rivalA.season = "2023/2024";
                rivalA.persist();

                Participation homeB = new Participation();
                homeB.teamFormation = TeamFormation.findById(teamBFixture.teamFormationId());
                homeB.competition = Competition.findById(teamBFixture.competitionId());
                homeB.season = "2024/2025";
                homeB.persist();

                Participation rivalB = new Participation();
                rivalB.teamFormation = TeamFormation.findById(rivalFixture.teamFormationId());
                rivalB.competition = Competition.findById(teamBFixture.competitionId());
                rivalB.season = "2024/2025";
                rivalB.persist();

                Match matchA = new Match();
                matchA.homeTeam = homeA;
                matchA.awayTeam = rivalA;
                matchA.date = LocalDate.of(2023, 10, 1);
                matchA.homeScore = 2;
                matchA.awayScore = 0;
                matchA.persist();

                Match matchB = new Match();
                matchB.homeTeam = homeB;
                matchB.awayTeam = rivalB;
                matchB.date = LocalDate.of(2024, 11, 5);
                matchB.homeScore = 1;
                matchB.awayScore = 1;
                matchB.persist();

                PlayerAppearance paA = new PlayerAppearance();
                paA.player = player;
                paA.match = matchA;
                paA.participation = homeA;
                paA.starter = true;
                paA.persist();

                PlayerAppearance paB = new PlayerAppearance();
                paB.player = player;
                paB.match = matchB;
                paB.participation = homeB;
                paB.starter = false;
                paB.persist();

                MatchEvent goal = new MatchEvent();
                goal.playerAppearance = paA;
                goal.type = MatchEventType.GOAL;
                goal.minute = 23;
                goal.persist();

                MatchEvent card = new MatchEvent();
                card.playerAppearance = paB;
                card.type = MatchEventType.YELLOW_CARD;
                card.minute = 60;
                card.persist();

                return new Fixture(player.id, homeA.id, rivalA.id, homeB.id, rivalB.id,
                        matchA.id, matchB.id, paA.id, paB.id);
            });

            String body = given().when().get("/players/" + fixture.playerId())
                    .then().statusCode(200)
                    .extract().body().asString();

            int alphaIndex = body.indexOf("Career Team Alpha");
            int betaIndex = body.indexOf("Career Team Beta");
            org.junit.jupiter.api.Assertions.assertTrue(alphaIndex >= 0 && betaIndex >= 0 && alphaIndex < betaIndex,
                    "Expected 2023/2024 Alpha appearance to be listed before the 2024/2025 Beta appearance");

            given().when().get("/players/" + fixture.playerId())
                    .then().statusCode(200)
                    .body(containsString("Career Team Alpha U15"),
                            containsString("Alpha League"),
                            containsString("2023/2024"),
                            containsString("Career Team Beta U16"),
                            containsString("Beta League"),
                            containsString("2024/2025"),
                            containsString("Гол"),
                            containsString("Жълт картон"));

            QuarkusTransaction.requiringNew().run(() -> {
                MatchEvent.delete("playerAppearance.id", fixture.paAId());
                MatchEvent.delete("playerAppearance.id", fixture.paBId());
                PlayerAppearance.deleteById(fixture.paAId());
                PlayerAppearance.deleteById(fixture.paBId());
                Match.deleteById(fixture.matchAId());
                Match.deleteById(fixture.matchBId());
                Participation.deleteById(fixture.homeAId());
                Participation.deleteById(fixture.rivalAId());
                Participation.deleteById(fixture.homeBId());
                Participation.deleteById(fixture.rivalBId());
                Player.deleteById(fixture.playerId());
            });
        } finally {
            QuarkusTransaction.requiringNew().run(() -> {
                TeamFormationFixtures.delete(teamAFixture);
                TeamFormationFixtures.delete(teamBFixture);
                TeamFormationFixtures.delete(rivalFixture);
            });
        }
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void detailOffersSimilarlyNamedPlayersForMergeToAdmin() {
        createdId = insertPlayer("Merge Suggest Player");
        Long similarId = insertPlayer("Merge Suggest Player");
        try {
            given().when().get("/players/" + createdId)
                    .then().statusCode(200)
                    .body(containsString("action=\"/players/" + createdId + "/merge\""),
                            containsString("value=\"" + similarId + "\""));
        } finally {
            QuarkusTransaction.requiringNew().run(() -> Player.deleteById(similarId));
        }
    }

    @Test
    @TestSecurity(user = "user", roles = {"USER"})
    void mergeForbiddenForUser() {
        createdId = insertPlayer("Merge Forbidden Player");
        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("duplicateId", createdId)
                .when().post("/players/" + createdId + "/merge")
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void mergeMovesAppearancesAndAliasesToKeptPlayerAndDeletesDuplicate() {
        MergeFixture f = createMergeFixture();
        try {
            given().redirects().follow(false)
                    .contentType(ContentType.URLENC)
                    .formParam("duplicateId", f.duplicateId())
                    .when().post("/players/" + f.keepId() + "/merge")
                    .then().statusCode(303)
                    .header("Location", endsWith("/players/" + f.keepId()));

            QuarkusTransaction.requiringNew().run(() -> {
                assertNull(Player.findById(f.duplicateId()));
                assertEquals(f.keepId(), PlayerAppearance.<PlayerAppearance>findById(f.appearanceId()).player.id);
                assertEquals(f.keepId(), PlayerAlias.<PlayerAlias>findById(f.aliasId()).player.id);
            });
        } finally {
            deleteMergeFixture(f);
        }
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void mergeRefusedWhenBothPlayedSameMatch() {
        MergeFixture f = createMergeFixture();
        Long keepAppearanceId = QuarkusTransaction.requiringNew().call(() -> {
            PlayerAppearance pa = new PlayerAppearance();
            pa.player = Player.findById(f.keepId());
            pa.match = Match.findById(f.matchId());
            pa.participation = Participation.findById(f.participationId());
            pa.starter = true;
            pa.persist();
            return pa.id;
        });
        try {
            given().redirects().follow(false)
                    .contentType(ContentType.URLENC)
                    .formParam("duplicateId", f.duplicateId())
                    .when().post("/players/" + f.keepId() + "/merge")
                    .then().statusCode(409)
                    .body(containsString("един и същи мач"));

            QuarkusTransaction.requiringNew().run(() -> assertNotNull(Player.findById(f.duplicateId())));
        } finally {
            QuarkusTransaction.requiringNew().run(() -> PlayerAppearance.deleteById(keepAppearanceId));
            deleteMergeFixture(f);
        }
    }

    private record MergeFixture(TeamFormationFixtures.Ids team, Long participationId, Long matchId,
            Long keepId, Long duplicateId, Long appearanceId, Long aliasId) {
    }

    private MergeFixture createMergeFixture() {
        return QuarkusTransaction.requiringNew().call(() -> {
            TeamFormationFixtures.Ids team = TeamFormationFixtures.create(
                    "Merge Test Team", "Merge City", FormationType.U15, "Merge League");

            Participation participation = new Participation();
            participation.teamFormation = TeamFormation.findById(team.teamFormationId());
            participation.competition = Competition.findById(team.competitionId());
            participation.season = "2024/2025";
            participation.persist();

            Match match = new Match();
            match.homeTeam = participation;
            match.awayTeam = participation;
            match.date = LocalDate.of(2024, 9, 1);
            match.persist();

            Player keep = new Player();
            keep.names = "Merge Keep Player";
            keep.persist();
            Player duplicate = new Player();
            duplicate.names = "Merge Keep Player";
            duplicate.persist();

            PlayerAppearance appearance = new PlayerAppearance();
            appearance.player = duplicate;
            appearance.match = match;
            appearance.participation = participation;
            appearance.starter = true;
            appearance.persist();

            PlayerAlias alias = new PlayerAlias();
            alias.player = duplicate;
            alias.source = ExternalRefSource.BFU_TOURNAMENTS;
            alias.rawName = "Merge Keep Player";
            alias.team = Team.findById(team.teamId());
            alias.persist();

            return new MergeFixture(team, participation.id, match.id, keep.id, duplicate.id, appearance.id, alias.id);
        });
    }

    private void deleteMergeFixture(MergeFixture f) {
        QuarkusTransaction.requiringNew().run(() -> {
            PlayerAlias.deleteById(f.aliasId());
            PlayerAppearance.deleteById(f.appearanceId());
            Match.deleteById(f.matchId());
            Participation.deleteById(f.participationId());
            Player.deleteById(f.duplicateId());
            Player.deleteById(f.keepId());
            TeamFormationFixtures.delete(f.team());
        });
    }

    private Long insertPlayer(String names) {
        return QuarkusTransaction.requiringNew().call(() -> {
            Player p = new Player();
            p.names = names;
            p.persist();
            return p.id;
        });
    }
}
