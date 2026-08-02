package com.nosoftskills.lineup.resource;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

@Path("/login")
public class LoginResource {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance login(boolean error);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance login(@Context UriInfo uriInfo) {
        boolean error = uriInfo.getQueryParameters().containsKey("error");
        return Templates.login(error);
    }
}
