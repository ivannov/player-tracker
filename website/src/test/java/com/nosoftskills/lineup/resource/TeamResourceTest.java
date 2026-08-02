package com.nosoftskills.lineup.resource;

import com.nosoftskills.lineup.model.FormationType;
import com.nosoftskills.lineup.model.Team;
import com.nosoftskills.lineup.model.TeamFormation;
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

@QuarkusTest
class TeamResourceTest {

    private Long createdId;

    @AfterEach
    void cleanup() {
        if (createdId != null) {
            Long id = createdId;
            createdId = null;
            QuarkusTransaction.requiringNew().run(() -> {
                TeamFormation.delete("team.id", id);
                Team.deleteById(id);
            });
        }
    }

    @Test
    void listReturns200Anonymously() {
        given().when().get("/teams")
                .then().statusCode(200)
                .body(not(containsString("hx-delete")), not(containsString("/teams/new")));
    }

    @Test
    @TestSecurity(user = "user", roles = {"USER"})
    void listReturns200() {
        given().when().get("/teams")
                .then().statusCode(200)
                .body(not(containsString("hx-delete")), not(containsString("/teams/new")));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void listShowsAdminControlsForAdmin() {
        given().when().get("/teams")
                .then().statusCode(200)
                .body(containsString("/teams/new"));
    }

    @Test
    void newFormRedirectsAnonymousToLogin() {
        given().redirects().follow(false)
                .when().get("/teams/new")
                .then().statusCode(302);
    }

    @Test
    @TestSecurity(user = "user", roles = {"USER"})
    void newFormForbiddenForUser() {
        given().redirects().follow(false)
                .when().get("/teams/new")
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void newFormReturns200() {
        given().when().get("/teams/new")
                .then().statusCode(200);
    }

    @Test
    @TestSecurity(user = "user", roles = {"USER"})
    void createForbiddenForUser() {
        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("name", "Should Not Be Created")
                .formParam("location", "Nowhere")
                .when().post("/teams")
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void createRedirectsToList() {
        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("name", "Test Team")
                .formParam("location", "Test City")
                .formParam("logoUrl", "")
                .when().post("/teams")
                .then().statusCode(303)
                .header("Location", endsWith("/teams"));

        createdId = QuarkusTransaction.requiringNew()
                .call(() -> Team.<Team>find("name", "Test Team").firstResult().id);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void editFormReturns200() {
        createdId = insertTeam("Edit Test Team");
        given().when().get("/teams/" + createdId + "/edit")
                .then().statusCode(200);
    }

    @Test
    void editFormRedirectsAnonymousToLogin() {
        createdId = insertTeam("Edit Anon Test Team");
        given().redirects().follow(false)
                .when().get("/teams/" + createdId + "/edit")
                .then().statusCode(302);
    }

    @Test
    @TestSecurity(user = "user", roles = {"USER"})
    void editFormForbiddenForUser() {
        createdId = insertTeam("Edit Forbidden Test Team");
        given().redirects().follow(false)
                .when().get("/teams/" + createdId + "/edit")
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void updateRedirectsToList() {
        createdId = insertTeam("Update Test Team");
        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("name", "Updated Team")
                .formParam("location", "Updated City")
                .when().post("/teams/" + createdId)
                .then().statusCode(303)
                .header("Location", endsWith("/teams"));
    }

    @Test
    @TestSecurity(user = "user", roles = {"USER"})
    void deleteForbiddenForUser() {
        createdId = insertTeam("Delete Forbidden Team");
        given().redirects().follow(false)
                .when().delete("/teams/" + createdId)
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void deleteReturns204() {
        Long id = insertTeam("Delete Test Team");
        given().redirects().follow(false)
                .when().delete("/teams/" + id)
                .then().statusCode(204);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void deleteWithFormationReturnsConflictNotServerError() {
        createdId = insertTeam("Team With Formation");
        QuarkusTransaction.requiringNew().run(() -> {
            TeamFormation tf = new TeamFormation();
            tf.team = Team.findById(createdId);
            tf.type = FormationType.U15;
            tf.persist();
        });

        given().redirects().follow(false)
                .when().delete("/teams/" + createdId)
                .then().statusCode(409)
                .body(not(containsString("Exception")), not(containsString("nosoftskills")));
    }

    private Long insertTeam(String name) {
        return QuarkusTransaction.requiringNew().call(() -> {
            Team t = new Team();
            t.name = name;
            t.location = "Test City";
            t.persist();
            return t.id;
        });
    }
}
