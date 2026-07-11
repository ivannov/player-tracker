package com.nosoftskills.lineup.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.endsWith;

@QuarkusTest
class AppResourceTest {

    @Test
    void indexRedirectsAnonymousToLogin() {
        given().redirects().follow(false)
                .when().get("/app")
                .then().statusCode(302)
                .header("Location", endsWith("/login"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void indexReturns200ForAuthenticatedAdmin() {
        given().when().get("/app")
                .then().statusCode(200);
    }
}
