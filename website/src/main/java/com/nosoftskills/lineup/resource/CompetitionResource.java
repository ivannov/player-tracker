package com.nosoftskills.lineup.resource;

import com.nosoftskills.lineup.model.Competition;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;

import java.net.URI;
import java.util.List;

@Path("/competitions")
@Authenticated
public class CompetitionResource {

    @Inject
    SecurityIdentity identity;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance list(String username, List<Competition> competitions);
        public static native TemplateInstance form(String username, Competition competition);
    }

    public static class CompetitionForm {
        @RestForm
        public String name;
        @RestForm
        public String logoUrl;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list() {
        return Templates.list(identity.getPrincipal().getName(), Competition.listAll());
    }

    @GET
    @Path("/new")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance newForm() {
        return Templates.form(identity.getPrincipal().getName(), new Competition());
    }

    @GET
    @Path("/{id}/edit")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance editForm(@PathParam("id") Long id) {
        Competition c = Competition.findById(id);
        if (c == null) throw new NotFoundException();
        return Templates.form(identity.getPrincipal().getName(), c);
    }

    @POST
    @Transactional
    @RolesAllowed("ADMIN")
    public Response create(@BeanParam CompetitionForm f) {
        Competition c = new Competition();
        c.name = f.name;
        c.logoUrl = f.logoUrl == null || f.logoUrl.isBlank() ? null : f.logoUrl;
        c.persist();
        return Response.seeOther(URI.create("/competitions")).build();
    }

    @POST
    @Path("/{id}")
    @Transactional
    @RolesAllowed("ADMIN")
    public Response update(@PathParam("id") Long id, @BeanParam CompetitionForm f) {
        Competition c = Competition.findById(id);
        if (c == null) throw new NotFoundException();
        c.name = f.name;
        c.logoUrl = f.logoUrl == null || f.logoUrl.isBlank() ? null : f.logoUrl;
        return Response.seeOther(URI.create("/competitions")).build();
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    @RolesAllowed("ADMIN")
    public Response delete(@PathParam("id") Long id) {
        Competition c = Competition.findById(id);
        if (c == null) throw new NotFoundException();
        c.delete();
        return Response.noContent().build();
    }
}
