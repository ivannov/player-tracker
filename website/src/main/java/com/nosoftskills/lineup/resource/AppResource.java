package com.nosoftskills.lineup.resource;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

import java.net.URI;

@Path("/app")
@Authenticated
public class AppResource {

    @Inject
    SecurityIdentity identity;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance index(String username);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index() {
        return Templates.index(identity.getPrincipal().getName());
    }

    @POST
    @Path("/logout")
    public Response logout() {
        NewCookie expired = new NewCookie.Builder("quarkus-credential")
                .maxAge(0)
                .path("/")
                .build();
        return Response.seeOther(URI.create("/")).cookie(expired).build();
    }
}
