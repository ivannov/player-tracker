package com.nosoftskills.lineup.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@QuarkusTest
class LandingResourceTest {

    @Test
    @DisplayName("UC-001 Main: home page counts and links teams, competitions, participations and matches")
    void landingLinksEveryListAnonymously() {
        given().when().get("/")
                .then().statusCode(200)
                .body(containsString("href=\"/teams\""))
                .body(containsString("href=\"/competitions\""))
                .body(containsString("href=\"/participations\""))
                .body(containsString("Разгледай мачовете"));
    }

    @Test
    @DisplayName("UC-001 BR-004: anonymous visitor sees the browse menu and sign-in, but no admin or account menus")
    void anonymousSeesBrowseMenuOnly() {
        for (String path : new String[] {"/", "/matches"}) {
            assertBrowseMenu(given().when().get(path).then().statusCode(200))
                    .body(containsString("href=\"/login\""))
                    .body(not(containsString("Въвеждане")))
                    .body(not(containsString("Извличане")))
                    .body(not(containsString("/inbox")))
                    .body(not(containsString("Изход")));
        }
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    @DisplayName("UC-001 BR-004: administrator sees the browse, extraction, inbox and account menus, but no data-entry menu")
    void adminSeesBrowseExtractionAndInbox() {
        assertBrowseMenu(given().when().get("/").then().statusCode(200))
                .body(containsString("Извличане"))
                .body(containsString("href=\"/matches/extract\""))
                .body(containsString("/inbox"))
                .body(containsString("Изход"))
                .body(not(containsString("Въвеждане")));
    }

    @Test
    @TestSecurity(user = "viewer", roles = {"USER"})
    @DisplayName("UC-003 Main: signed-in User sees the browse and account menus, but no extraction menu or inbox")
    void userSeesBrowseAndAccountMenusOnly() {
        assertBrowseMenu(given().when().get("/").then().statusCode(200))
                .body(containsString("Изход"))
                .body(not(containsString("Въвеждане")))
                .body(not(containsString("Извличане")))
                .body(not(containsString("/inbox")));
    }

    private static ValidatableResponse assertBrowseMenu(ValidatableResponse response) {
        return response
                .body(containsString("Преглед"))
                .body(containsString("href=\"/players\""))
                .body(containsString("href=\"/matches\""));
    }
}
