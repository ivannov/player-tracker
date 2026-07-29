package com.nosoftskills.lineup.inbox;

import com.nosoftskills.lineup.inbox.AmbiguityInboxService.CandidateView;
import com.nosoftskills.lineup.inbox.AmbiguityInboxService.ReviewView;
import com.nosoftskills.lineup.model.Player;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
class InboxResourceTest {

    @InjectMock
    AmbiguityInboxService inboxService;

    private static Player player(Long id, String names) {
        Player p = new Player();
        p.id = id;
        p.names = names;
        return p;
    }

    @Test
    void unauthenticatedListRedirectsToLogin() {
        given().redirects().follow(false)
                .when().get("/inbox")
                .then().statusCode(302);
    }

    @Test
    @TestSecurity(user = "user", roles = {"USER"})
    void nonAdminListForbidden() {
        given().redirects().follow(false)
                .when().get("/inbox")
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void adminListReturnsPendingReviewsAsJson() {
        Mockito.when(inboxService.listPending()).thenReturn(List.of(
                new ReviewView(1L, "Ivan Ivanov", "Test Team",
                        List.of(new CandidateView(10L, "Ivan Ivanov", new BigDecimal("0.9000"))))));

        given().when().get("/inbox")
                .then().statusCode(200)
                .contentType(ContentType.JSON)
                .body("[0].id", equalTo(1))
                .body("[0].rawName", equalTo("Ivan Ivanov"))
                .body("[0].teamName", equalTo("Test Team"))
                .body("[0].candidates[0].playerId", equalTo(10));
    }

    @Test
    @TestSecurity(user = "user", roles = {"USER"})
    void nonAdminResolveForbidden() {
        given().redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("playerId", 5L)
                .when().post("/inbox/1/resolve")
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void adminResolveReturnsResolvedPlayer() {
        Mockito.when(inboxService.resolveReview(1L, 5L)).thenReturn(player(5L, "Petar Petrov"));

        given().contentType(ContentType.URLENC)
                .formParam("playerId", 5L)
                .when().post("/inbox/1/resolve")
                .then().statusCode(200)
                .contentType(ContentType.JSON)
                .body("reviewId", equalTo(1))
                .body("playerId", equalTo(5))
                .body("playerNames", equalTo("Petar Petrov"));

        Mockito.verify(inboxService).resolveReview(1L, 5L);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void adminResolveUnknownReviewReturns404() {
        Mockito.when(inboxService.resolveReview(999L, 5L)).thenThrow(new NotFoundException());

        given().contentType(ContentType.URLENC)
                .formParam("playerId", 5L)
                .when().post("/inbox/999/resolve")
                .then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void adminResolveAlreadyResolvedReviewReturns400() {
        Mockito.when(inboxService.resolveReview(1L, 5L)).thenThrow(new BadRequestException());

        given().contentType(ContentType.URLENC)
                .formParam("playerId", 5L)
                .when().post("/inbox/1/resolve")
                .then().statusCode(400);
    }

    @Test
    @TestSecurity(user = "user", roles = {"USER"})
    void nonAdminConfirmNewForbidden() {
        given().redirects().follow(false)
                .when().post("/inbox/1/confirm-new")
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void adminConfirmNewReturnsCreatedPlayer() {
        Mockito.when(inboxService.confirmNewPlayer(1L)).thenReturn(player(7L, "Nikolay Nikolov"));

        given().when().post("/inbox/1/confirm-new")
                .then().statusCode(200)
                .contentType(ContentType.JSON)
                .body("reviewId", equalTo(1))
                .body("playerId", equalTo(7))
                .body("playerNames", equalTo("Nikolay Nikolov"));

        Mockito.verify(inboxService).confirmNewPlayer(1L);
    }
}
