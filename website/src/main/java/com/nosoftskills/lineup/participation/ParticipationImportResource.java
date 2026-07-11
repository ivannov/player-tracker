package com.nosoftskills.lineup.participation;

import com.nosoftskills.lineup.model.Competition;
import com.nosoftskills.lineup.model.FormationType;
import com.nosoftskills.lineup.model.Participation;
import com.nosoftskills.lineup.model.Team;
import com.nosoftskills.lineup.model.TeamFormation;
import com.nosoftskills.lineup.scraping.BfuLeagueScraperService;
import com.nosoftskills.lineup.scraping.BfuScraperException;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/participations/import")
@Authenticated
public class ParticipationImportResource {

    @Inject
    BfuLeagueScraperService scraperService;

    @Inject
    SecurityIdentity identity;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance step1(String username, List<Competition> competitions);

        public static native TemplateInstance step2(String username, List<TeamResolutionRow> rows,
                Long competitionId, String season, List<Team> allTeams, FormationType[] formationTypes);

        public static native TemplateInstance formationsSelect(List<TeamFormation> formations);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance showStep1() {
        return Templates.step1(identity.getPrincipal().getName(), Competition.listAll());
    }

    @POST
    @Path("/extract")
    @RolesAllowed("ADMIN")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance extract(@RestForm String url, @RestForm Long competitionId, @RestForm String season) {
        List<String> names;
        try {
            names = scraperService.extractTeamNames(url);
        } catch (BfuScraperException e) {
            throw new BadRequestException("Грешка при извличане: " + e.getMessage());
        }

        List<Team> allTeams = Team.listAll();
        Map<String, Team> byName = allTeams.stream()
                .collect(Collectors.toMap(t -> t.name.toLowerCase(), t -> t, (a, b) -> a));

        List<TeamResolutionRow> rows = names.stream()
                .map(name -> {
                    Team matched = byName.get(name.toLowerCase());
                    List<TeamFormation> formations = matched != null
                            ? TeamFormation.<TeamFormation>find("team.id = ?1", matched.id).list()
                            : List.of();
                    return new TeamResolutionRow(name, matched, formations);
                })
                .toList();

        return Templates.step2(identity.getPrincipal().getName(), rows, competitionId, season,
                allTeams, FormationType.values());
    }

    @POST
    @Path("/formations")
    @RolesAllowed("ADMIN")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance formations(@RestForm Long teamId) {
        List<TeamFormation> formations = teamId != null
                ? TeamFormation.<TeamFormation>find("team.id = ?1", teamId).list()
                : List.of();
        return Templates.formationsSelect(formations);
    }

    @POST
    @Path("/save")
    @RolesAllowed("ADMIN")
    @Transactional
    public Response save(
            @RestForm Long competitionId,
            @RestForm String season,
            @RestForm List<String> scrapedName,
            @RestForm List<String> teamId,
            @RestForm List<String> teamName,
            @RestForm List<String> teamLocation,
            @RestForm List<String> formationTypeId,
            @RestForm List<String> newFormationType) {

        Competition comp = Competition.findById(competitionId);
        if (comp == null) throw new NotFoundException();

        String normSeason = season.replace("-", "/");
        int count = scrapedName != null ? scrapedName.size() : 0;

        for (int i = 0; i < count; i++) {
            String tId = get(teamId, i);
            String tName = get(teamName, i);
            String tLocation = get(teamLocation, i);
            String fTypeId = get(formationTypeId, i);
            String newFType = get(newFormationType, i);

            if (isBlank(tId) && isBlank(tName)) continue;

            Team team;
            TeamFormation tf;

            if (isBlank(tId)) {
                team = new Team();
                team.name = tName.trim();
                team.location = isBlank(tLocation) ? "" : tLocation.trim();
                team.persist();
                tf = new TeamFormation();
                tf.team = team;
                tf.type = FormationType.FIRST;
                tf.persist();
            } else {
                team = Team.findById(Long.parseLong(tId));
                if (team == null) continue;

                if (!isBlank(fTypeId)) {
                    tf = TeamFormation.findById(Long.parseLong(fTypeId));
                    if (tf == null) continue;
                } else if (!isBlank(newFType)) {
                    FormationType fType = FormationType.valueOf(newFType);
                    tf = TeamFormation.<TeamFormation>find("team.id = ?1 AND type = ?2", team.id, fType)
                            .firstResult();
                    if (tf == null) {
                        tf = new TeamFormation();
                        tf.team = team;
                        tf.type = fType;
                        tf.persist();
                    }
                } else {
                    continue;
                }
            }

            long existing = Participation.count(
                    "teamFormation.id = ?1 AND competition.id = ?2 AND season = ?3",
                    tf.id, comp.id, normSeason);
            if (existing == 0) {
                Participation p = new Participation();
                p.teamFormation = tf;
                p.competition = comp;
                p.season = normSeason;
                p.persist();
            }
        }

        return Response.seeOther(URI.create("/participations")).build();
    }

    private String get(List<String> list, int i) {
        return list != null && i < list.size() ? list.get(i) : null;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
