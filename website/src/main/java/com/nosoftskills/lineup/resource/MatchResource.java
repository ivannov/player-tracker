package com.nosoftskills.lineup.resource;

import com.nosoftskills.lineup.model.Competition;
import com.nosoftskills.lineup.model.Match;
import com.nosoftskills.lineup.model.Participation;
import com.nosoftskills.lineup.security.CurrentUser;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@Path("/matches")
public class MatchResource {

    @Inject
    CurrentUser currentUser;

    private static final String DETAIL_FETCH_JOINS =
            "JOIN FETCH m.homeTeam ht JOIN FETCH ht.teamFormation htf JOIN FETCH htf.team JOIN FETCH ht.competition " +
            "JOIN FETCH m.awayTeam at JOIN FETCH at.teamFormation atf JOIN FETCH atf.team JOIN FETCH at.competition";

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance list(String username, boolean isAdmin, List<Match> matches,
                List<Competition> competitions, Long competitionId, String date);
        public static native TemplateInstance detail(String username, boolean isAdmin, Match match);
        public static native TemplateInstance form(String username, List<Participation> participations, String error);
    }

    public static class MatchForm {
        @RestForm public Long homeParticipationId;
        @RestForm public Long awayParticipationId;
        @RestForm public String date;
        @RestForm public String homeScore;
        @RestForm public String awayScore;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list(@QueryParam("competitionId") Long competitionId, @QueryParam("date") String date) {
        LocalDate parsedDate = (date == null || date.isBlank()) ? null : LocalDate.parse(date);

        String baseQuery = "SELECT m FROM Match m " + DETAIL_FETCH_JOINS;
        List<Match> matches;
        if (competitionId != null && parsedDate != null) {
            matches = Match.find(baseQuery + " WHERE ht.competition.id = ?1 AND m.date = ?2 ORDER BY m.date DESC, m.id DESC",
                    competitionId, parsedDate).list();
        } else if (competitionId != null) {
            matches = Match.find(baseQuery + " WHERE ht.competition.id = ?1 ORDER BY m.date DESC, m.id DESC",
                    competitionId).list();
        } else if (parsedDate != null) {
            matches = Match.find(baseQuery + " WHERE m.date = ?1 ORDER BY m.date DESC, m.id DESC",
                    parsedDate).list();
        } else {
            matches = Match.find(baseQuery + " ORDER BY m.date DESC, m.id DESC").list();
        }

        return Templates.list(currentUser.username(), currentUser.isAdmin(), matches, Competition.listAll(), competitionId, date);
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance detail(@PathParam("id") Long id) {
        Match match = Match.find("SELECT m FROM Match m " + DETAIL_FETCH_JOINS + " WHERE m.id = ?1", id).firstResult();
        if (match == null) throw new NotFoundException();
        return Templates.detail(currentUser.username(), currentUser.isAdmin(), match);
    }

    @GET
    @Path("/new")
    @RolesAllowed("ADMIN")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance newForm() {
        return Templates.form(currentUser.username(), participationPickerOptions(), null);
    }

    @POST
    @Transactional
    @RolesAllowed("ADMIN")
    public Response create(@BeanParam MatchForm f) {
        Participation home = Participation.findById(f.homeParticipationId);
        Participation away = Participation.findById(f.awayParticipationId);
        if (home == null || away == null) throw new NotFoundException();

        if (home.id.equals(away.id)
                || !home.competition.id.equals(away.competition.id)
                || !home.season.equals(away.season)) {
            return Response.status(422)
                    .type(MediaType.TEXT_HTML)
                    .entity(Templates.form(currentUser.username(), participationPickerOptions(),
                            "Домакинът и гостът трябва да са от една и съща лига/турнир и сезон."))
                    .build();
        }

        Match m = new Match();
        m.homeTeam = home;
        m.awayTeam = away;
        m.date = LocalDate.parse(f.date);
        m.homeScore = parseScore(f.homeScore);
        m.awayScore = parseScore(f.awayScore);
        m.persist();
        return Response.seeOther(URI.create("/matches/" + m.id)).build();
    }

    private List<Participation> participationPickerOptions() {
        return Participation.find(
                "SELECT p FROM Participation p " +
                "JOIN FETCH p.teamFormation tf JOIN FETCH tf.team " +
                "JOIN FETCH p.competition " +
                "ORDER BY p.competition.name, p.season DESC, tf.team.name"
        ).list();
    }

    private static Short parseScore(String value) {
        return (value == null || value.isBlank()) ? null : Short.parseShort(value.trim());
    }
}
