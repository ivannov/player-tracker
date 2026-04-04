package com.nosoftskills.lineup.resource;

import com.nosoftskills.lineup.model.Competition;
import com.nosoftskills.lineup.model.FormationType;
import com.nosoftskills.lineup.model.Participation;
import com.nosoftskills.lineup.model.Team;
import com.nosoftskills.lineup.model.TeamFormation;
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
import java.util.Set;
import java.util.stream.Collectors;

@Path("/participations")
@Authenticated
public class ParticipationResource {

    @Inject
    SecurityIdentity identity;

    private static final List<FormationType> DEFAULT_SELECTED = List.of(
            FormationType.U15, FormationType.U16, FormationType.U17,
            FormationType.U18, FormationType.U19, FormationType.FIRST
    );

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance list(String username, List<Participation> participations);

        public static native TemplateInstance form(String username, Long groupId,
                Long teamId, String teamName, Long competitionId, String competitionName, String season,
                List<Team> teams, List<Competition> competitions,
                FormationType[] allTypes, List<FormationType> selectedTypes);
    }

    public static class ParticipationForm {
        @RestForm public Long teamId;
        @RestForm public Long competitionId;
        @RestForm public String season;
        @RestForm public List<String> formationTypes;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list() {
        List<Participation> participations = Participation.find(
                "SELECT p FROM Participation p " +
                "JOIN FETCH p.teamFormation tf JOIN FETCH tf.team " +
                "JOIN FETCH p.competition " +
                "ORDER BY p.season DESC, tf.team.name"
        ).list();
        return Templates.list(identity.getPrincipal().getName(), participations);
    }

    @GET
    @Path("/new")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance newForm() {
        return Templates.form(
                identity.getPrincipal().getName(),
                null, null, null, null, null, null,
                Team.listAll(), Competition.listAll(),
                FormationType.values(), DEFAULT_SELECTED
        );
    }

    @GET
    @Path("/{id}/edit")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance editForm(@PathParam("id") Long id) {
        Participation ref = loadById(id);
        if (ref == null) throw new NotFoundException();

        List<Participation> group = findGroup(ref.teamFormation.team.id, ref.competition.id, ref.season);
        List<FormationType> selectedTypes = group.stream()
                .map(p -> p.teamFormation.type)
                .collect(Collectors.toList());

        Team team = ref.teamFormation.team;
        Competition comp = ref.competition;

        return Templates.form(
                identity.getPrincipal().getName(),
                id, team.id, team.name, comp.id, comp.name, ref.season,
                List.of(), List.of(),
                FormationType.values(), selectedTypes
        );
    }

    @POST
    @Transactional
    @RolesAllowed("ADMIN")
    public Response create(@BeanParam ParticipationForm f) {
        Team team = Team.findById(f.teamId);
        Competition comp = Competition.findById(f.competitionId);
        if (team == null || comp == null) throw new NotFoundException();

        List<String> types = f.formationTypes != null ? f.formationTypes : List.of();
        for (String typeName : types) {
            FormationType type = FormationType.valueOf(typeName);
            TeamFormation tf = findOrCreateFormation(team, type);
            Participation p = new Participation();
            p.teamFormation = tf;
            p.competition = comp;
            p.season = f.season;
            p.persist();
        }
        return Response.seeOther(URI.create("/participations")).build();
    }

    @POST
    @Path("/{id}")
    @Transactional
    @RolesAllowed("ADMIN")
    public Response update(@PathParam("id") Long id, @BeanParam ParticipationForm f) {
        Participation ref = loadById(id);
        if (ref == null) throw new NotFoundException();

        Team team = ref.teamFormation.team;
        Competition comp = ref.competition;
        String season = ref.season;

        Set<FormationType> desired = f.formationTypes != null
                ? f.formationTypes.stream().map(FormationType::valueOf).collect(Collectors.toSet())
                : Set.of();

        List<Participation> existing = findGroup(team.id, comp.id, season);
        Set<FormationType> existingTypes = existing.stream()
                .map(p -> p.teamFormation.type)
                .collect(Collectors.toSet());

        for (Participation p : existing) {
            if (!desired.contains(p.teamFormation.type)) {
                p.delete();
            }
        }
        for (FormationType type : desired) {
            if (!existingTypes.contains(type)) {
                TeamFormation tf = findOrCreateFormation(team, type);
                Participation p = new Participation();
                p.teamFormation = tf;
                p.competition = comp;
                p.season = season;
                p.persist();
            }
        }

        return Response.seeOther(URI.create("/participations")).build();
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    @RolesAllowed("ADMIN")
    public Response delete(@PathParam("id") Long id) {
        Participation p = Participation.findById(id);
        if (p == null) throw new NotFoundException();
        p.delete();
        return Response.noContent().build();
    }

    private Participation loadById(Long id) {
        return Participation.find(
                "SELECT p FROM Participation p " +
                "JOIN FETCH p.teamFormation tf JOIN FETCH tf.team " +
                "JOIN FETCH p.competition " +
                "WHERE p.id = ?1", id
        ).firstResult();
    }

    private List<Participation> findGroup(Long teamId, Long competitionId, String season) {
        return Participation.find(
                "SELECT p FROM Participation p JOIN FETCH p.teamFormation tf " +
                "WHERE tf.team.id = ?1 AND p.competition.id = ?2 AND p.season = ?3",
                teamId, competitionId, season
        ).list();
    }

    private TeamFormation findOrCreateFormation(Team team, FormationType type) {
        TeamFormation tf = TeamFormation.<TeamFormation>find(
                "team.id = ?1 AND type = ?2", team.id, type).firstResult();
        if (tf == null) {
            tf = new TeamFormation();
            tf.team = team;
            tf.type = type;
            tf.persist();
        }
        return tf;
    }
}
