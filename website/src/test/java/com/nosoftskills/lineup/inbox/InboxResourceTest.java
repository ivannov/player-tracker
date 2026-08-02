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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

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
    void adminListRendersPendingReviewsWithCandidates() {
        Mockito.when(inboxService.listPending()).thenReturn(List.of(
                new ReviewView(1L, "Ivan Ivanov", "Test Team",
                        List.of(new CandidateView(10L, "Ivan Ivanov", new BigDecimal("0.9000"))))));

        given().when().get("/inbox")
                .then().statusCode(200)
                .contentType(ContentType.HTML)
                .body(containsString("Test Team"))
                .body(containsString("Ivan Ivanov"))
                .body(containsString("hx-post=\"/inbox/1/resolve\""))
                .body(containsString("hx-post=\"/inbox/1/confirm-new\""));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void adminListShowsEmptyStateWhenNothingPending() {
        Mockito.when(inboxService.listPending()).thenReturn(List.of());

        given().when().get("/inbox")
                .then().statusCode(200)
                .body(containsString("Няма чакащи прегледи"));
    }

    @Test
    void badgeUnauthenticatedRedirectsToLogin() {
        given().redirects().follow(false)
                .when().get("/inbox/badge")
                .then().statusCode(302);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void badgeShowsCountForAdminWhenReviewsPending() {
        Mockito.when(inboxService.countPending()).thenReturn(3L);

        given().when().get("/inbox/badge")
                .then().statusCode(200)
                .body(containsString("/inbox"))
                .body(containsString("3"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    void badgeHidesLinkForAdminWhenNothingPending() {
        Mockito.when(inboxService.countPending()).thenReturn(0L);

        given().when().get("/inbox/badge")
                .then().statusCode(200)
                .body(not(containsString("<a")));
    }

    @Test
    @TestSecurity(user = "user", roles = {"USER"})
    void badgeIsEmptyForNonAdmin() {
        given().when().get("/inbox/badge")
                .then().statusCode(200)
                .body(not(containsString("<a")));

        Mockito.verify(inboxService, Mockito.never()).countPending();
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
    void adminResolveReturnsUpdatedListFragment() {
        Mockito.when(inboxService.resolveReview(1L, 5L)).thenReturn(player(5L, "Petar Petrov"));
        Mockito.when(inboxService.listPending()).thenReturn(List.of());

        given().contentType(ContentType.URLENC)
                .formParam("playerId", 5L)
                .when().post("/inbox/1/resolve")
                .then().statusCode(200)
                .contentType(ContentType.HTML)
                .body(containsString("inbox-list"))
                .body(containsString("Няма чакащи прегледи"));

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
    void adminResolveAlreadyResolvedReviewShowsErrorInList() {
        Mockito.when(inboxService.resolveReview(1L, 5L))
                .thenThrow(new BadRequestException("Този преглед вече е разрешен от друг администратор."));
        Mockito.when(inboxService.listPending()).thenReturn(List.of());

        given().contentType(ContentType.URLENC)
                .formParam("playerId", 5L)
                .when().post("/inbox/1/resolve")
                .then().statusCode(200)
                .contentType(ContentType.HTML)
                .body(containsString("Този преглед вече е разрешен от друг администратор."));
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
    void adminConfirmNewReturnsUpdatedListFragment() {
        Mockito.when(inboxService.confirmNewPlayer(1L)).thenReturn(player(7L, "Nikolay Nikolov"));
        Mockito.when(inboxService.listPending()).thenReturn(List.of());

        given().when().post("/inbox/1/confirm-new")
                .then().statusCode(200)
                .contentType(ContentType.HTML)
                .body(containsString("inbox-list"))
                .body(containsString("Няма чакащи прегледи"));

        Mockito.verify(inboxService).confirmNewPlayer(1L);
    }
}
