package com.nosoftskills.lineup.resource;

import com.nosoftskills.lineup.model.Competition;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.endsWith;

@QuarkusTest
class CompetitionResourceTest {

    private Long createdId;

    @AfterEach
    void cleanup() {
        if (createdId != null) {
            Long id = createdId;
            createdId = null;
            QuarkusTransaction.requiringNew().run(() -> Competition.deleteById(id));
        }
    }

    @Test
    void listUnauthenticatedRedirectsToLogin() {
        given().redirects().follow(false)
                .when().get("/competitions")
                .then().statusCode(302);
    }

    @Test
    @TestSecurity(user = "user", roles = {"USER"})
    void listReturns200() {
        given().when().get("/competitions")
                .then().statusCode(200);
    }

    @Test
    @TestSecurity(user = "user", roles = {"USER"})
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
    void editFormReturns200() {
        createdId = insertCompetition("Edit Test League");
        given().when().get("/competitions/" + createdId + "/edit")
                .then().statusCode(200);
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

    private Long insertCompetition(String name) {
        return QuarkusTransaction.requiringNew().call(() -> {
            Competition c = new Competition();
            c.name = name;
            c.persist();
            return c.id;
        });
    }
}
