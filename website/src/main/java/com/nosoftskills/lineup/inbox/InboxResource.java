package com.nosoftskills.lineup.inbox;

import com.nosoftskills.lineup.inbox.AmbiguityInboxService.ReviewView;
import com.nosoftskills.lineup.model.Team;
import io.quarkus.panache.common.Sort;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
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
 * or confirming a brand-new player for PLAYER-type reviews, or picking an existing team for
 * TEAM-type reviews (LT-018) -- via HTMX, swapping the whole list (mirroring the whole-panel
 * swap idiom used by MatchExtractionResource's wizard) rather than a single row.
 */
@Path("/inbox")
public class InboxResource {

    @Inject
    AmbiguityInboxService inboxService;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance page(List<ReviewView> reviews, List<Team> teams);
        public static native TemplateInstance list(List<ReviewView> reviews, String error, List<Team> teams);
        public static native TemplateInstance badge(long pendingCount);
    }

    @GET
    @RolesAllowed("ADMIN")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance showInbox() {
        return Templates.page(inboxService.listPending(), Team.listAll(Sort.by("name")));
    }

    // UC-001 BR-004: the inbox badge is shown only to Administrators
    @GET
    @Path("/badge")
    @RolesAllowed("ADMIN")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance badge() {
        return Templates.badge(inboxService.countPending());
    }

    @POST
    @Path("/{id}/resolve")
    @RolesAllowed("ADMIN")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance resolve(@PathParam("id") Long id, @RestForm Long playerId) {
        String error = null;
        try {
            inboxService.resolveReview(id, playerId);
        } catch (BadRequestException e) {
            error = e.getMessage();
        }
        return Templates.list(inboxService.listPending(), error, Team.listAll(Sort.by("name")));
    }

    @POST
    @Path("/{id}/confirm-new")
    @RolesAllowed("ADMIN")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance confirmNew(@PathParam("id") Long id) {
        String error = null;
        try {
            inboxService.confirmNewPlayer(id);
        } catch (BadRequestException e) {
            error = e.getMessage();
        }
        return Templates.list(inboxService.listPending(), error, Team.listAll(Sort.by("name")));
    }

    @POST
    @Path("/{id}/resolve-team")
    @RolesAllowed("ADMIN")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance resolveTeam(@PathParam("id") Long id, @RestForm Long teamId) {
        String error = null;
        try {
            inboxService.resolveTeamReview(id, teamId);
        } catch (BadRequestException e) {
            error = e.getMessage();
        }
        return Templates.list(inboxService.listPending(), error, Team.listAll(Sort.by("name")));
    }
}
