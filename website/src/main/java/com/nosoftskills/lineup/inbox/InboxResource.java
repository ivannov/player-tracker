package com.nosoftskills.lineup.inbox;

import com.nosoftskills.lineup.inbox.AmbiguityInboxService.ReviewView;
import com.nosoftskills.lineup.model.Player;
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
 * Admin-only JSON backend for the ambiguity inbox (LT-009.02). Every endpoint requires ADMIN --
 * unlike the public-read/admin-write resources elsewhere, this whole screen is admin-only per the
 * LT-009 epic. The LT-009.03 UI is the intended caller.
 */
@Path("/inbox")
@RolesAllowed("ADMIN")
public class InboxResource {

    @Inject
    AmbiguityInboxService inboxService;

    public record ResolvedView(Long reviewId, Long playerId, String playerNames) {
        static ResolvedView of(Long reviewId, Player player) {
            return new ResolvedView(reviewId, player.id, player.names);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<ReviewView> list() {
        return inboxService.listPending();
    }

    @POST
    @Path("/{id}/resolve")
    @Produces(MediaType.APPLICATION_JSON)
    public ResolvedView resolve(@PathParam("id") Long id, @RestForm Long playerId) {
        Player resolved = inboxService.resolveReview(id, playerId);
        return ResolvedView.of(id, resolved);
    }

    @POST
    @Path("/{id}/confirm-new")
    @Produces(MediaType.APPLICATION_JSON)
    public ResolvedView confirmNew(@PathParam("id") Long id) {
        Player resolved = inboxService.confirmNewPlayer(id);
        return ResolvedView.of(id, resolved);
    }
}
