package com.nosoftskills.lineup.participation;

import com.nosoftskills.lineup.matching.TeamResolutionService;
import com.nosoftskills.lineup.model.Competition;
import com.nosoftskills.lineup.model.ExternalRefSource;
import com.nosoftskills.lineup.model.FormationType;
import com.nosoftskills.lineup.model.Participation;
import com.nosoftskills.lineup.model.Team;
import com.nosoftskills.lineup.model.TeamAlias;
import com.nosoftskills.lineup.model.TeamFormation;
import com.nosoftskills.lineup.scraping.BfuLeagueScraperService;
import com.nosoftskills.lineup.scraping.BfuScraperException;
import com.nosoftskills.lineup.security.CurrentUser;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Path("/participations/import")
public class ParticipationImportResource {

    @Inject
    BfuLeagueScraperService scraperService;

    @Inject
    TeamResolutionService teamResolutionService;

    @Inject
    CurrentUser currentUser;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance step1(String username, List<Competition> competitions, String error,
                String url, Long competitionId, String season);

        public static native TemplateInstance step2(String username, List<TeamResolutionRow> rows,
                Long competitionId, String season, List<Team> allTeams, FormationType[] formationTypes);

        public static native TemplateInstance formationsSelect(List<TeamFormation> formations,
                FormationType[] formationTypes);

        public static native TemplateInstance step3(String username, List<ImportReviewRow> rows,
                Long competitionId, String season);
    }

    @GET
    @RolesAllowed("ADMIN")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance showStep1() {
        return Templates.step1(currentUser.username(), Competition.listAll(), null, null, null, null);
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
            return Templates.step1(currentUser.username(), Competition.listAll(),
                    "Грешка при извличане: " + e.getMessage(), url, competitionId, season);
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

        return Templates.step2(currentUser.username(), rows, competitionId, season,
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
        return Templates.formationsSelect(formations, FormationType.values());
    }

    @POST
    @Path("/review")
    @RolesAllowed("ADMIN")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance review(
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

        String normSeason = normalizeSeason(season);
        int count = scrapedName != null ? scrapedName.size() : 0;

        ResolveContext ctx = buildContext(teamId, formationTypeId);
        List<RowResolution> resolutions = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            resolutions.add(resolveRow(ctx, get(teamId, i), get(teamName, i), get(teamLocation, i),
                    get(formationTypeId, i), get(newFormationType, i)));
        }
        Set<Long> existingFormationIds = existingParticipationFormationIds(
                resolutions.stream()
                        .filter(r -> r.outcome() == RowOutcome.EXISTING_FORMATION)
                        .map(r -> r.formation().id)
                        .collect(Collectors.toSet()),
                comp.id, normSeason);

        List<ImportReviewRow> rows = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            String sName = get(scrapedName, i);
            String tId = get(teamId, i);
            String tName = get(teamName, i);
            String tLocation = get(teamLocation, i);
            String fTypeId = get(formationTypeId, i);
            String newFType = get(newFormationType, i);

            RowResolution r = resolutions.get(i);
            String teamDisplay;
            String formationDisplay;
            ImportAction action;

            switch (r.outcome()) {
                case SKIP_EMPTY -> {
                    teamDisplay = "—";
                    formationDisplay = "—";
                    action = ImportAction.SKIP;
                }
                case SKIP_INVALID_TEAM -> {
                    teamDisplay = "Невалиден отбор";
                    formationDisplay = "—";
                    action = ImportAction.SKIP;
                }
                case SKIP_INVALID_FORMATION -> {
                    teamDisplay = r.team().name;
                    formationDisplay = "Невалидна формация";
                    action = ImportAction.SKIP;
                }
                case SKIP_NO_FORMATION -> {
                    teamDisplay = r.team().name;
                    formationDisplay = "Няма избрана формация";
                    action = ImportAction.SKIP;
                }
                case NEW_TEAM -> {
                    teamDisplay = "Нов: " + r.newTeamName()
                            + (r.newTeamLocation().isEmpty() ? "" : " (" + r.newTeamLocation() + ")");
                    formationDisplay = FormationType.FIRST.getDisplayLabel() + " (нов отбор)";
                    action = ImportAction.CREATE;
                }
                case EXISTING_FORMATION -> {
                    teamDisplay = r.team().name;
                    formationDisplay = r.formationType().getDisplayLabel();
                    action = existingFormationIds.contains(r.formation().id)
                            ? ImportAction.EXISTS : ImportAction.CREATE;
                }
                case NEW_FORMATION -> {
                    teamDisplay = r.team().name;
                    formationDisplay = r.formationType().getDisplayLabel() + " (нова формация)";
                    action = ImportAction.CREATE;
                }
                default -> throw new IllegalStateException("Unhandled outcome: " + r.outcome());
            }

            rows.add(new ImportReviewRow(sName, tId, tName, tLocation, fTypeId, newFType,
                    teamDisplay, formationDisplay, action));
        }

        return Templates.step3(currentUser.username(), rows, competitionId, season);
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

        String normSeason = normalizeSeason(season);
        int count = scrapedName != null ? scrapedName.size() : 0;

        ResolveContext ctx = buildContext(teamId, formationTypeId);
        List<RowResolution> resolutions = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            resolutions.add(resolveRow(ctx, get(teamId, i), get(teamName, i), get(teamLocation, i),
                    get(formationTypeId, i), get(newFormationType, i)));
        }
        Set<Long> existingFormationIds = existingParticipationFormationIds(
                resolutions.stream()
                        .filter(r -> r.outcome() == RowOutcome.EXISTING_FORMATION)
                        .map(r -> r.formation().id)
                        .collect(Collectors.toSet()),
                comp.id, normSeason);

        Map<String, TeamAlias> teamAliasesByRawName =
                teamResolutionService.preloadAliases(ExternalRefSource.BFU_TOURNAMENTS, scrapedName);

        for (int i = 0; i < count; i++) {
            RowResolution r = resolutions.get(i);
            TeamFormation tf = switch (r.outcome()) {
                case SKIP_EMPTY, SKIP_INVALID_TEAM, SKIP_INVALID_FORMATION, SKIP_NO_FORMATION -> null;
                case NEW_TEAM -> {
                    Team team = new Team();
                    team.name = r.newTeamName();
                    team.location = r.newTeamLocation();
                    team.persist();
                    TeamFormation newTf = new TeamFormation();
                    newTf.team = team;
                    newTf.type = FormationType.FIRST;
                    newTf.persist();
                    yield newTf;
                }
                case EXISTING_FORMATION -> r.formation();
                case NEW_FORMATION -> {
                    TeamFormation newTf = new TeamFormation();
                    newTf.team = r.team();
                    newTf.type = r.formationType();
                    newTf.persist();
                    yield newTf;
                }
            };
            if (tf == null) continue;

            teamResolutionService.recordAlias(
                    teamAliasesByRawName, ExternalRefSource.BFU_TOURNAMENTS, get(scrapedName, i), tf.team);

            // A freshly created team/formation can't already have a participation row.
            boolean exists = r.outcome() == RowOutcome.EXISTING_FORMATION && existingFormationIds.contains(tf.id);
            if (!exists) {
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

    private static String normalizeSeason(String season) {
        return season == null ? null : season.replace("-", "/");
    }

    private static Long parseLongOrNull(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static FormationType parseFormationTypeOrNull(String s) {
        try {
            return FormationType.valueOf(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Set<Long> existingParticipationFormationIds(
            Collection<Long> formationIds, Long competitionId, String season) {
        if (formationIds.isEmpty()) return Set.of();
        return Participation.<Participation>list(
                        "teamFormation.id in ?1 and competition.id = ?2 and season = ?3",
                        formationIds, competitionId, season)
                .stream()
                .map(p -> p.teamFormation.id)
                .collect(Collectors.toSet());
    }

    private enum RowOutcome {
        SKIP_EMPTY, SKIP_INVALID_TEAM, SKIP_INVALID_FORMATION, SKIP_NO_FORMATION,
        NEW_TEAM, EXISTING_FORMATION, NEW_FORMATION
    }

    private record RowResolution(
            RowOutcome outcome,
            Team team,
            TeamFormation formation,
            FormationType formationType,
            String newTeamName,
            String newTeamLocation) {
    }

    /**
     * Batched lookups for every team/formation referenced by a submitted row set, fetched
     * once up front so resolveRow() never issues a per-row query.
     */
    private record ResolveContext(
            Map<Long, Team> teamsById,
            Map<Long, TeamFormation> formationsById,
            Map<String, TeamFormation> formationsByTeamAndType) {

        private static String key(Long teamId, FormationType type) {
            return teamId + ":" + type;
        }

        TeamFormation formationByTeamAndType(Long teamId, FormationType type) {
            return formationsByTeamAndType.get(key(teamId, type));
        }
    }

    private ResolveContext buildContext(List<String> teamIds, List<String> formationTypeIds) {
        Set<Long> teamIdSet = new HashSet<>();
        if (teamIds != null) {
            for (String id : teamIds) {
                Long parsed = isBlank(id) ? null : parseLongOrNull(id);
                if (parsed != null) teamIdSet.add(parsed);
            }
        }
        Map<Long, Team> teamsById = teamIdSet.isEmpty() ? Map.of()
                : Team.<Team>list("id in ?1", teamIdSet).stream().collect(Collectors.toMap(t -> t.id, t -> t));

        Set<Long> formationIdSet = new HashSet<>();
        if (formationTypeIds != null) {
            for (String id : formationTypeIds) {
                if (isBlank(id) || "new".equals(id)) continue;
                Long parsed = parseLongOrNull(id);
                if (parsed != null) formationIdSet.add(parsed);
            }
        }
        Map<Long, TeamFormation> formationsById = formationIdSet.isEmpty() ? Map.of()
                : TeamFormation.<TeamFormation>list("id in ?1", formationIdSet).stream()
                        .collect(Collectors.toMap(tf -> tf.id, tf -> tf));

        Map<String, TeamFormation> formationsByTeamAndType = teamIdSet.isEmpty() ? Map.of()
                : TeamFormation.<TeamFormation>list("team.id in ?1", teamIdSet).stream()
                        .collect(Collectors.toMap(tf -> ResolveContext.key(tf.team.id, tf.type), tf -> tf, (a, b) -> a));

        return new ResolveContext(teamsById, formationsById, formationsByTeamAndType);
    }

    /**
     * Single source of truth for interpreting one submitted import row, shared by both
     * review() (preview only) and save() (persists) so the two can never disagree about
     * what a row means.
     */
    private RowResolution resolveRow(ResolveContext ctx, String tId, String tName, String tLocation,
            String fTypeId, String newFType) {
        if (isBlank(tId) && isBlank(tName)) {
            return new RowResolution(RowOutcome.SKIP_EMPTY, null, null, null, null, null);
        }

        if (isBlank(tId)) {
            String newTeamName = tName.trim();
            String newTeamLocation = isBlank(tLocation) ? "" : tLocation.trim();
            return new RowResolution(RowOutcome.NEW_TEAM, null, null, FormationType.FIRST,
                    newTeamName, newTeamLocation);
        }

        Long parsedTeamId = parseLongOrNull(tId);
        Team team = parsedTeamId != null ? ctx.teamsById().get(parsedTeamId) : null;
        if (team == null) {
            return new RowResolution(RowOutcome.SKIP_INVALID_TEAM, null, null, null, null, null);
        }

        boolean explicitNewFormation = "new".equals(fTypeId);
        if (!isBlank(fTypeId) && !explicitNewFormation) {
            Long parsedFormationId = parseLongOrNull(fTypeId);
            TeamFormation tf = parsedFormationId != null ? ctx.formationsById().get(parsedFormationId) : null;
            if (tf == null || !tf.team.id.equals(team.id)) {
                return new RowResolution(RowOutcome.SKIP_INVALID_FORMATION, team, null, null, null, null);
            }
            return new RowResolution(RowOutcome.EXISTING_FORMATION, team, tf, tf.type, null, null);
        }

        if (!isBlank(newFType)) {
            FormationType fType = parseFormationTypeOrNull(newFType);
            if (fType == null) {
                return new RowResolution(RowOutcome.SKIP_INVALID_FORMATION, team, null, null, null, null);
            }
            TeamFormation existing = ctx.formationByTeamAndType(team.id, fType);
            if (existing != null) {
                return new RowResolution(RowOutcome.EXISTING_FORMATION, team, existing, fType, null, null);
            }
            return new RowResolution(RowOutcome.NEW_FORMATION, team, null, fType, null, null);
        }

        return new RowResolution(RowOutcome.SKIP_NO_FORMATION, team, null, null, null, null);
    }
}
