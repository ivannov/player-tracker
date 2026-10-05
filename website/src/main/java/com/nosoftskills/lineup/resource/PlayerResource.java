package com.nosoftskills.lineup.resource;

import com.nosoftskills.lineup.matching.PlayerMatchingService;
import com.nosoftskills.lineup.model.MatchEvent;
import com.nosoftskills.lineup.model.Player;
import com.nosoftskills.lineup.model.PlayerAppearance;
import com.nosoftskills.lineup.security.CurrentUser;
import io.quarkus.panache.common.Sort;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
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
        public static native TemplateInstance detail(String username, boolean isAdmin, Player player, List<PlayerCareerRow> timeline,
                List<MergeCandidate> mergeCandidates, String error);
        public static native TemplateInstance form(String username, Player player);
    }

    public record PlayerCareerRow(PlayerAppearance appearance, List<MatchEvent> events) {
    }

    public record MergeCandidate(Long playerId, String names, String latestClub) {
    }

    static final double MERGE_CANDIDATE_MIN_SCORE = 0.4;
    static final int MAX_MERGE_CANDIDATES = 10;

    @Inject
    EntityManager em;

    @Inject
    PlayerMatchingService playerMatchingService;

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
        return renderDetail(id, null);
    }

    private TemplateInstance renderDetail(Long id, String error) {
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

        List<MergeCandidate> mergeCandidates = currentUser.isAdmin() ? findMergeCandidates(player) : List.of();
        return Templates.detail(currentUser.username(), currentUser.isAdmin(), player, timeline, mergeCandidates, error);
    }

    // Likely duplicates of this player (typically the record created after a transfer), by name
    // similarity across the whole players table.
    @SuppressWarnings("unchecked")
    private List<MergeCandidate> findMergeCandidates(Player player) {
        List<Object[]> rows = em.createNativeQuery("""
                SELECT p.id, p.names
                FROM players p
                WHERE p.id <> :id
                AND p.names % :names
                AND similarity(p.names, :names) >= :minScore
                ORDER BY similarity(p.names, :names) DESC, p.id ASC
                LIMIT :maxCandidates
                """)
                .setParameter("id", player.id)
                .setParameter("names", player.names)
                .setParameter("minScore", MERGE_CANDIDATE_MIN_SCORE)
                .setParameter("maxCandidates", MAX_MERGE_CANDIDATES)
                .getResultList();
        List<Long> ids = rows.stream().map(row -> ((Number) row[0]).longValue()).toList();
        Map<Long, PlayerMatchingService.LatestClub> clubs = playerMatchingService.latestClubs(ids);
        return rows.stream().map(row -> {
            Long candidateId = ((Number) row[0]).longValue();
            PlayerMatchingService.LatestClub club = clubs.get(candidateId);
            return new MergeCandidate(candidateId, (String) row[1], club == null ? null : club.teamName());
        }).toList();
    }

    // Folds the duplicate into this player: every reference to the duplicate is repointed here and
    // the duplicate row is deleted. Refused when both played the same match, since then they are
    // really two different people (and player_appearances' (player_id, match_id) key would clash).
    @POST
    @Path("/{id}/merge")
    @Transactional
    @RolesAllowed("ADMIN")
    @Produces(MediaType.TEXT_HTML)
    public Response merge(@PathParam("id") Long id, @RestForm Long duplicateId) {
        if (Player.findById(id) == null || duplicateId == null || Player.findById(duplicateId) == null) {
            throw new NotFoundException();
        }
        if (id.equals(duplicateId)) {
            return Response.status(409).entity(renderDetail(id, "Играчът не може да бъде обединен със себе си.")).build();
        }
        long sharedMatches = PlayerAppearance.count(
                "player.id = ?1 and match.id in (select pa.match.id from PlayerAppearance pa where pa.player.id = ?2)",
                id, duplicateId);
        if (sharedMatches > 0) {
            return Response.status(409).entity(renderDetail(id,
                    "Двамата играчи са участвали в един и същи мач -- това са различни хора и не могат да бъдат обединени.")).build();
        }

        // A review listing both players would otherwise end up offering this player twice.
        em.createNativeQuery("""
                DELETE FROM ambiguity_candidates WHERE player_id = :dup AND ambiguity_review_id IN (
                    SELECT ambiguity_review_id FROM ambiguity_candidates WHERE player_id = :keep)
                """).setParameter("dup", duplicateId).setParameter("keep", id).executeUpdate();
        for (String sql : List.of(
                "UPDATE player_appearances SET player_id = :keep WHERE player_id = :dup",
                "UPDATE player_aliases SET player_id = :keep WHERE player_id = :dup",
                "UPDATE ambiguity_candidates SET player_id = :keep WHERE player_id = :dup",
                "UPDATE ambiguity_reviews SET resolved_player_id = :keep WHERE resolved_player_id = :dup",
                "DELETE FROM players WHERE id = :dup")) {
            var query = em.createNativeQuery(sql).setParameter("dup", duplicateId);
            if (sql.contains(":keep")) {
                query.setParameter("keep", id);
            }
            query.executeUpdate();
        }
        return Response.seeOther(URI.create("/players/" + id)).build();
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
