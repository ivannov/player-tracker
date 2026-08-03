package com.nosoftskills.lineup.resource;

import com.nosoftskills.lineup.model.Competition;
import com.nosoftskills.lineup.model.Match;
import com.nosoftskills.lineup.model.MatchEvent;
import com.nosoftskills.lineup.model.MatchEventType;
import com.nosoftskills.lineup.model.Participation;
import com.nosoftskills.lineup.model.Player;
import com.nosoftskills.lineup.model.PlayerAppearance;
import com.nosoftskills.lineup.security.CurrentUser;
import io.quarkus.panache.common.Sort;
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
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
                List<Competition> competitions, Long competitionId, String date, String error);
        public static native TemplateInstance detail(String username, boolean isAdmin, Match match,
                List<AppearanceRow> homeAppearances, List<AppearanceRow> awayAppearances,
                List<Player> allPlayers, MatchEventType[] eventTypes, String error);
        public static native TemplateInstance form(String username, List<Participation> participations, String error);
    }

    public record AppearanceRow(PlayerAppearance appearance, List<MatchEvent> events) {
    }

    public static class MatchForm {
        @RestForm public Long homeParticipationId;
        @RestForm public Long awayParticipationId;
        @RestForm public String date;
        @RestForm public String homeScore;
        @RestForm public String awayScore;
    }

    public static class AppearanceForm {
        @RestForm public Long participationId;
        @RestForm public Long playerId;
        @RestForm public String newPlayerName;
        @RestForm public boolean starter;
        @RestForm public String number;
    }

    public static class EventForm {
        @RestForm public String type;
        @RestForm public String minute;
    }

    public static class SubstitutionForm {
        @RestForm public String substitutedInMinute;
        @RestForm public String substitutedOutMinute;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list(@QueryParam("competitionId") Long competitionId, @QueryParam("date") String date) {
        LocalDate parsedDate = null;
        String error = null;
        if (date != null && !date.isBlank()) {
            try {
                parsedDate = LocalDate.parse(date);
            } catch (DateTimeParseException e) {
                error = "Невалидна дата: " + date;
            }
        }

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

        return Templates.list(currentUser.username(), currentUser.isAdmin(), matches, Competition.listAll(), competitionId, date, error);
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance detail(@PathParam("id") Long id, @QueryParam("error") String error) {
        Match match = loadMatch(id);
        if (match == null) throw new NotFoundException();

        List<PlayerAppearance> appearances = PlayerAppearance.find(
                "SELECT pa FROM PlayerAppearance pa " +
                "JOIN FETCH pa.player JOIN FETCH pa.participation " +
                "WHERE pa.match.id = ?1 ORDER BY pa.starter DESC, pa.number", id
        ).list();

        List<Long> appearanceIds = appearances.stream().map(pa -> pa.id).toList();
        Map<Long, List<MatchEvent>> eventsByAppearance = appearanceIds.isEmpty()
                ? Map.of()
                : MatchEvent.<MatchEvent>find("playerAppearance.id IN ?1 ORDER BY minute", appearanceIds)
                        .list().stream()
                        .collect(Collectors.groupingBy(e -> e.playerAppearance.id));

        List<AppearanceRow> home = new ArrayList<>();
        List<AppearanceRow> away = new ArrayList<>();
        for (PlayerAppearance pa : appearances) {
            AppearanceRow row = new AppearanceRow(pa, eventsByAppearance.getOrDefault(pa.id, List.of()));
            if (pa.participation.id.equals(match.homeTeam.id)) {
                home.add(row);
            } else {
                away.add(row);
            }
        }

        List<Player> allPlayers = currentUser.isAdmin() ? Player.listAll(Sort.by("names")) : List.of();

        return Templates.detail(currentUser.username(), currentUser.isAdmin(), match, home, away,
                allPlayers, MatchEventType.values(), error);
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
        m.homeScore = parseShort(f.homeScore);
        m.awayScore = parseShort(f.awayScore);
        m.persist();
        return Response.seeOther(URI.create("/matches/" + m.id)).build();
    }

    @POST
    @Path("/{id}/appearances")
    @Transactional
    @RolesAllowed("ADMIN")
    public Response addAppearance(@PathParam("id") Long id, @BeanParam AppearanceForm f) {
        Match match = Match.findById(id);
        if (match == null) throw new NotFoundException();

        Participation participation = Participation.findById(f.participationId);
        if (participation == null
                || (!participation.id.equals(match.homeTeam.id) && !participation.id.equals(match.awayTeam.id))) {
            return Response.seeOther(errorRedirect(id, "Невалиден отбор за този мач.")).build();
        }

        Player player;
        if (f.playerId != null) {
            player = Player.findById(f.playerId);
            if (player == null) throw new NotFoundException();
        } else if (f.newPlayerName != null && !f.newPlayerName.isBlank()) {
            player = new Player();
            player.names = f.newPlayerName.trim();
            player.persist();
        } else {
            return Response.seeOther(errorRedirect(id, "Изберете играч или въведете име за нов играч.")).build();
        }

        if (PlayerAppearance.count("player.id = ?1 and match.id = ?2", player.id, id) > 0) {
            return Response.seeOther(errorRedirect(id, "Играчът вече е добавен към този мач.")).build();
        }

        PlayerAppearance pa = new PlayerAppearance();
        pa.player = player;
        pa.match = match;
        pa.participation = participation;
        pa.starter = f.starter;
        pa.number = parseShort(f.number);
        pa.persist();

        return Response.seeOther(URI.create("/matches/" + id)).build();
    }

    @DELETE
    @Path("/{id}/appearances/{appearanceId}")
    @Transactional
    @RolesAllowed("ADMIN")
    public Response removeAppearance(@PathParam("id") Long id, @PathParam("appearanceId") Long appearanceId) {
        PlayerAppearance pa = PlayerAppearance.findById(appearanceId);
        if (pa == null || !pa.match.id.equals(id)) throw new NotFoundException();
        pa.delete();
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/appearances/{appearanceId}/substitution")
    @Transactional
    @RolesAllowed("ADMIN")
    public Response updateSubstitution(@PathParam("id") Long id, @PathParam("appearanceId") Long appearanceId,
            @BeanParam SubstitutionForm f) {
        PlayerAppearance pa = PlayerAppearance.findById(appearanceId);
        if (pa == null || !pa.match.id.equals(id)) throw new NotFoundException();
        pa.substitutedInMinute = parseShort(f.substitutedInMinute);
        pa.substitutedOutMinute = parseShort(f.substitutedOutMinute);
        return Response.seeOther(URI.create("/matches/" + id)).build();
    }

    @POST
    @Path("/{id}/appearances/{appearanceId}/events")
    @Transactional
    @RolesAllowed("ADMIN")
    public Response addEvent(@PathParam("id") Long id, @PathParam("appearanceId") Long appearanceId,
            @BeanParam EventForm f) {
        PlayerAppearance pa = PlayerAppearance.findById(appearanceId);
        if (pa == null || !pa.match.id.equals(id)) throw new NotFoundException();

        MatchEventType type;
        try {
            type = MatchEventType.valueOf(f.type);
        } catch (IllegalArgumentException | NullPointerException e) {
            return Response.seeOther(errorRedirect(id, "Невалиден тип събитие.")).build();
        }

        MatchEvent event = new MatchEvent();
        event.playerAppearance = pa;
        event.type = type;
        event.minute = parseShort(f.minute);
        event.persist();

        return Response.seeOther(URI.create("/matches/" + id)).build();
    }

    @DELETE
    @Path("/{id}/events/{eventId}")
    @Transactional
    @RolesAllowed("ADMIN")
    public Response removeEvent(@PathParam("id") Long id, @PathParam("eventId") Long eventId) {
        MatchEvent event = MatchEvent.findById(eventId);
        if (event == null || !event.playerAppearance.match.id.equals(id)) throw new NotFoundException();
        event.delete();
        return Response.noContent().build();
    }

    private Match loadMatch(Long id) {
        return Match.find("SELECT m FROM Match m " + DETAIL_FETCH_JOINS + " WHERE m.id = ?1", id).firstResult();
    }

    private List<Participation> participationPickerOptions() {
        return Participation.find(
                "SELECT p FROM Participation p " +
                "JOIN FETCH p.teamFormation tf JOIN FETCH tf.team " +
                "JOIN FETCH p.competition " +
                "ORDER BY p.competition.name, p.season DESC, tf.team.name"
        ).list();
    }

    private static URI errorRedirect(Long matchId, String message) {
        return URI.create("/matches/" + matchId + "?error=" + URLEncoder.encode(message, StandardCharsets.UTF_8));
    }

    private static Short parseShort(String value) {
        return (value == null || value.isBlank()) ? null : Short.parseShort(value.trim());
    }
}
