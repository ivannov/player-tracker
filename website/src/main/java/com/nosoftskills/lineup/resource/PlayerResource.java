package com.nosoftskills.lineup.resource;

import com.nosoftskills.lineup.model.MatchEvent;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/players")
public class PlayerResource {

    @Inject
    CurrentUser currentUser;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance list(String username, boolean isAdmin, List<Player> players, String q);
        public static native TemplateInstance detail(String username, boolean isAdmin, Player player, List<PlayerCareerRow> timeline);
        public static native TemplateInstance form(String username, Player player);
    }

    public record PlayerCareerRow(PlayerAppearance appearance, List<MatchEvent> events) {
    }

    public static class PlayerForm {
        @RestForm public String names;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list(@QueryParam("q") String q) {
        List<Player> players = (q == null || q.isBlank())
                ? Player.listAll(Sort.by("names"))
                : Player.find("LOWER(names) LIKE LOWER(?1) ORDER BY names", "%" + q.trim() + "%").list();
        return Templates.list(currentUser.username(), currentUser.isAdmin(), players, q);
    }

    @GET
    @Path("/new")
    @RolesAllowed("ADMIN")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance newForm() {
        return Templates.form(currentUser.username(), new Player());
    }

    @GET
    @Path("/{id}/edit")
    @RolesAllowed("ADMIN")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance editForm(@PathParam("id") Long id) {
        Player p = Player.findById(id);
        if (p == null) throw new NotFoundException();
        return Templates.form(currentUser.username(), p);
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance detail(@PathParam("id") Long id) {
        Player player = Player.findById(id);
        if (player == null) throw new NotFoundException();

        List<PlayerAppearance> appearances = PlayerAppearance.find(
                "SELECT pa FROM PlayerAppearance pa " +
                "JOIN FETCH pa.match m " +
                "JOIN FETCH pa.participation p " +
                "JOIN FETCH p.teamFormation tf JOIN FETCH tf.team " +
                "JOIN FETCH p.competition " +
                "WHERE pa.player.id = ?1 " +
                "ORDER BY m.date, m.id", id
        ).list();

        List<Long> appearanceIds = appearances.stream().map(pa -> pa.id).toList();
        Map<Long, List<MatchEvent>> eventsByAppearance = appearanceIds.isEmpty()
                ? Map.of()
                : MatchEvent.<MatchEvent>find(
                        "playerAppearance.id IN ?1 ORDER BY minute", appearanceIds)
                    .list().stream()
                    .collect(Collectors.groupingBy(e -> e.playerAppearance.id));

        List<PlayerCareerRow> timeline = appearances.stream()
                .map(pa -> new PlayerCareerRow(pa, eventsByAppearance.getOrDefault(pa.id, List.of())))
                .toList();

        return Templates.detail(currentUser.username(), currentUser.isAdmin(), player, timeline);
    }

    @POST
    @Transactional
    @RolesAllowed("ADMIN")
    public Response create(@BeanParam PlayerForm f) {
        Player p = new Player();
        p.names = f.names;
        p.persist();
        return Response.seeOther(URI.create("/players")).build();
    }

    @POST
    @Path("/{id}")
    @Transactional
    @RolesAllowed("ADMIN")
    public Response update(@PathParam("id") Long id, @BeanParam PlayerForm f) {
        Player p = Player.findById(id);
        if (p == null) throw new NotFoundException();
        p.names = f.names;
        return Response.seeOther(URI.create("/players")).build();
    }
}
