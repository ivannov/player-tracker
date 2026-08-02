package com.nosoftskills.lineup.resource;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@QuarkusTest
class LoginResourceTest {

    private static final String ERROR_BANNER = "Грешно потребителско име или парола";

    @Test
    void loginWithoutErrorParamShowsNoBanner() {
        given().when().get("/login")
                .then().statusCode(200)
                .body(not(containsString(ERROR_BANNER)));
    }

    @Test
    void loginWithBareErrorParamShowsBanner() {
        given().when().get("/login?error")
                .then().statusCode(200)
                .body(containsString(ERROR_BANNER));
    }

    @Test
    void loginWithEmptyErrorParamShowsBanner() {
        given().when().get("/login?error=")
                .then().statusCode(200)
                .body(containsString(ERROR_BANNER));
    }

    @Test
    void loginWithErrorParamValueShowsBanner() {
        given().when().get("/login?error=1")
                .then().statusCode(200)
                .body(containsString(ERROR_BANNER));
    }
}
