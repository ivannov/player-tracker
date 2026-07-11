package com.nosoftskills.lineup.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@QuarkusTest
class LandingResourceTest {

    @Test
    void landingReturns200Anonymously() {
        given().when().get("/")
                .then().statusCode(200)
                .body(containsString("href=\"/teams\""))
                .body(containsString("href=\"/competitions\""))
                .body(containsString("href=\"/participations\""))
                .body(not(containsString("Въвеждане")));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void landingReturns200ForAuthenticatedAdmin() {
        given().when().get("/")
                .then().statusCode(200)
                .body(containsString("Въвеждане"));
    }
}
