package com.nosoftskills.lineup.inbox;

import com.nosoftskills.lineup.inbox.AmbiguityInboxService.ReviewView;
import com.nosoftskills.lineup.security.CurrentUser;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestForm;

import java.util.List;

/**
 * Admin-only ambiguity inbox screen (LT-009.03), built on top of {@link AmbiguityInboxService}
 * (LT-009.02). Lists pending reviews and lets an admin resolve one -- picking a ranked candidate
 * or confirming a brand-new player -- via HTMX, swapping the whole list (mirroring the
 * whole-panel swap idiom used by MatchExtractionResource's wizard) rather than a single row.
 */
@Path("/inbox")
public class InboxResource {

    @Inject
    AmbiguityInboxService inboxService;

    @Inject
    CurrentUser currentUser;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance page(String username, List<ReviewView> reviews);
        public static native TemplateInstance list(List<ReviewView> reviews);
        public static native TemplateInstance badge(long pendingCount);
    }

    @GET
    @RolesAllowed("ADMIN")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance showInbox() {
        return Templates.page(currentUser.username(), inboxService.listPending());
    }

    // Not ADMIN-restricted: appNav shows this to every logged-in user (matching how the rest of
    // the nav's admin-oriented links are visible to any authenticated user and rely on the target
    // resource's own @RolesAllowed for enforcement), so a non-admin's automatic hx-trigger="load"
    // fetch must succeed with an empty badge rather than 403.
    @GET
    @Path("/badge")
    @Authenticated
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance badge() {
        long pendingCount = currentUser.isAdmin() ? inboxService.countPending() : 0;
        return Templates.badge(pendingCount);
    }

    @POST
    @Path("/{id}/resolve")
    @RolesAllowed("ADMIN")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance resolve(@PathParam("id") Long id, @RestForm Long playerId) {
        inboxService.resolveReview(id, playerId);
        return Templates.list(inboxService.listPending());
    }

    @POST
    @Path("/{id}/confirm-new")
    @RolesAllowed("ADMIN")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance confirmNew(@PathParam("id") Long id) {
        inboxService.confirmNewPlayer(id);
        return Templates.list(inboxService.listPending());
    }
}
