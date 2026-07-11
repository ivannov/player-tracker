package com.nosoftskills.lineup.resource;

import com.nosoftskills.lineup.security.CurrentUser;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/")
public class LandingResource {

    @Inject
    CurrentUser currentUser;

    @Inject
    EntityManager em;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance landing(String username,
                long teamCount, long competitionCount, long participationCount);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance landing() {
        Object[] counts = (Object[]) em.createNativeQuery(
                "SELECT (SELECT COUNT(*) FROM teams), (SELECT COUNT(*) FROM competitions), " +
                "(SELECT COUNT(*) FROM participations)"
        ).getSingleResult();
        long teamCount = ((Number) counts[0]).longValue();
        long competitionCount = ((Number) counts[1]).longValue();
        long participationCount = ((Number) counts[2]).longValue();
        return Templates.landing(currentUser.username(), teamCount, competitionCount, participationCount);
    }
}
