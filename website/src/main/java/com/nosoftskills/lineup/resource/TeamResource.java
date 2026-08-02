package com.nosoftskills.lineup.resource;

import com.nosoftskills.lineup.model.FormationType;
import com.nosoftskills.lineup.model.Participation;
import com.nosoftskills.lineup.model.Team;
import com.nosoftskills.lineup.model.TeamFormation;
import com.nosoftskills.lineup.security.CurrentUser;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
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
import java.util.Set;
import java.util.stream.Collectors;

@Path("/teams")
public class TeamResource {

    @Inject
    CurrentUser currentUser;

    private static final List<FormationType> DEFAULT_SELECTED = List.of(
            FormationType.U15, FormationType.U16, FormationType.U17,
            FormationType.U18, FormationType.U19, FormationType.FIRST
    );

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance list(String username, boolean isAdmin, List<Team> teams);
        public static native TemplateInstance form(String username, Team team,
                FormationType[] allTypes, List<FormationType> selectedTypes);
    }

    public static class TeamForm {
        @RestForm public String name;
        @RestForm public String location;
        @RestForm public String logoUrl;
        @RestForm public List<String> formationTypes;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list() {
        return Templates.list(currentUser.username(), currentUser.isAdmin(), Team.listAll());
    }

    @GET
    @Path("/new")
    @RolesAllowed("ADMIN")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance newForm() {
        return Templates.form(currentUser.username(), new Team(),
                FormationType.values(), DEFAULT_SELECTED);
    }

    @GET
    @Path("/{id}/edit")
    @RolesAllowed("ADMIN")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance editForm(@PathParam("id") Long id) {
        Team t = Team.findById(id);
        if (t == null) throw new NotFoundException();
        List<FormationType> selected = TeamFormation.<TeamFormation>find("team.id = ?1", id).list()
                .stream().map(tf -> tf.type).collect(Collectors.toList());
        return Templates.form(currentUser.username(), t,
                FormationType.values(), selected);
    }

    @POST
    @Transactional
    @RolesAllowed("ADMIN")
    public Response create(@BeanParam TeamForm f) {
        Team t = new Team();
        t.name = f.name;
        t.location = f.location;
        t.logoUrl = f.logoUrl == null || f.logoUrl.isBlank() ? null : f.logoUrl;
        t.persist();

        List<String> types = f.formationTypes != null ? f.formationTypes : List.of();
        for (String typeName : types) {
            TeamFormation tf = new TeamFormation();
            tf.team = t;
            tf.type = FormationType.valueOf(typeName);
            tf.persist();
        }
        return Response.seeOther(URI.create("/teams")).build();
    }

    @POST
    @Path("/{id}")
    @Transactional
    @RolesAllowed("ADMIN")
    public Response update(@PathParam("id") Long id, @BeanParam TeamForm f) {
        Team t = Team.findById(id);
        if (t == null) throw new NotFoundException();
        t.name = f.name;
        t.location = f.location;
        t.logoUrl = f.logoUrl == null || f.logoUrl.isBlank() ? null : f.logoUrl;

        Set<FormationType> desired = f.formationTypes != null
                ? f.formationTypes.stream().map(FormationType::valueOf).collect(Collectors.toSet())
                : Set.of();

        List<TeamFormation> existing = TeamFormation.<TeamFormation>find("team.id = ?1", id).list();
        Set<FormationType> existingTypes = existing.stream()
                .map(tf -> tf.type).collect(Collectors.toSet());

        for (TeamFormation tf : existing) {
            if (!desired.contains(tf.type)) {
                Participation.delete("teamFormation.id", tf.id);
                tf.delete();
            }
        }
        for (FormationType type : desired) {
            if (!existingTypes.contains(type)) {
                TeamFormation tf = new TeamFormation();
                tf.team = t;
                tf.type = type;
                tf.persist();
            }
        }
        return Response.seeOther(URI.create("/teams")).build();
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    @RolesAllowed("ADMIN")
    public Response delete(@PathParam("id") Long id) {
        Team t = Team.findById(id);
        if (t == null) throw new NotFoundException();
        if (TeamFormation.count("team.id", id) > 0) {
            return Response.status(Response.Status.CONFLICT)
                    .type(MediaType.TEXT_PLAIN)
                    .entity("Отборът не може да бъде изтрит, докато има свързани формации или участия.")
                    .build();
        }
        t.delete();
        return Response.noContent().build();
    }
}
