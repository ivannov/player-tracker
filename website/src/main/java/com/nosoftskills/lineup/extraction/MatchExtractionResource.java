package com.nosoftskills.lineup.extraction;

import com.nosoftskills.lineup.model.Competition;
import com.nosoftskills.lineup.scraping.BfuScraperException;
import com.nosoftskills.lineup.security.CurrentUser;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Path("/matches/extract")
public class MatchExtractionResource {

    @Inject
    MatchExtractionService extractionService;

    @Inject
    CurrentUser currentUser;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance step1(String username, List<Competition> competitions, String today);

        public static native TemplateInstance step2(String username, List<ExtractionRowView> rows,
                Long competitionId, String fixturesUrl, String season, String date,
                Counts counts);

        public static native TemplateInstance step3(String username, List<ExtractionRowView> rows,
                Long competitionId, String fixturesUrl, String season, String date,
                Counts counts);
    }

    public record Counts(int matchCount, int resolvableMatchCount, int resolvedPlayerCount, int ambiguousPlayerCount) {
    }

    @GET
    @RolesAllowed("ADMIN")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance showStep1() {
        return Templates.step1(currentUser.username(), Competition.listAll(), LocalDate.now().toString());
    }

    @POST
    @Path("/discover")
    @RolesAllowed("ADMIN")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance discover(@RestForm Long competitionId, @RestForm String fixturesUrl,
            @RestForm String season, @RestForm String date) {
        requireCompetition(competitionId);

        List<MatchExtractionRow> rows;
        try {
            rows = extractionService.preview(new ExtractionRequest(competitionId, fixturesUrl, season, parseDate(date)));
        } catch (BfuScraperException e) {
            throw new BadRequestException("Грешка при извличане: " + e.getMessage());
        }

        List<ExtractionRowView> rowViews = rows.stream().map(ExtractionRowView::from).toList();
        return Templates.step2(currentUser.username(), rowViews, competitionId, fixturesUrl, season, date,
                aggregate(rowViews));
    }

    @POST
    @Path("/review")
    @RolesAllowed("ADMIN")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance review(
            @RestForm Long competitionId,
            @RestForm String fixturesUrl,
            @RestForm String season,
            @RestForm String date,
            @RestForm List<String> matchUrl,
            @RestForm List<String> homeTeamName,
            @RestForm List<String> homeResolved,
            @RestForm List<String> awayTeamName,
            @RestForm List<String> awayResolved,
            @RestForm List<String> fullyResolvable,
            @RestForm List<String> extractionError,
            @RestForm List<String> resolvedPlayerCount,
            @RestForm List<String> ambiguousPlayerCount,
            @RestForm List<String> totalPlayerCount) {

        int count = matchUrl != null ? matchUrl.size() : 0;
        List<ExtractionRowView> rowViews = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            rowViews.add(new ExtractionRowView(
                    get(matchUrl, i),
                    get(homeTeamName, i), parseBoolean(get(homeResolved, i)),
                    get(awayTeamName, i), parseBoolean(get(awayResolved, i)),
                    parseBoolean(get(fullyResolvable, i)),
                    blankToNull(get(extractionError, i)),
                    parseIntOrZero(get(resolvedPlayerCount, i)),
                    parseIntOrZero(get(ambiguousPlayerCount, i)),
                    parseIntOrZero(get(totalPlayerCount, i))));
        }

        return Templates.step3(currentUser.username(), rowViews, competitionId, fixturesUrl, season, date,
                aggregate(rowViews));
    }

    @POST
    @Path("/confirm")
    @RolesAllowed("ADMIN")
    public Response confirm(@RestForm Long competitionId, @RestForm String fixturesUrl,
            @RestForm String season, @RestForm String date) {
        requireCompetition(competitionId);

        try {
            extractionService.confirm(new ExtractionRequest(competitionId, fixturesUrl, season, parseDate(date)));
        } catch (BfuScraperException e) {
            throw new BadRequestException("Грешка при извличане: " + e.getMessage());
        }

        return Response.seeOther(URI.create("/matches")).build();
    }

    private void requireCompetition(Long competitionId) {
        if (Competition.findById(competitionId) == null) {
            throw new NotFoundException();
        }
    }

    private static LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date);
        } catch (Exception e) {
            throw new BadRequestException("Невалидна дата: " + date);
        }
    }

    private static Counts aggregate(List<ExtractionRowView> rows) {
        int resolvableMatches = 0;
        int resolvedPlayers = 0;
        int ambiguousPlayers = 0;
        for (ExtractionRowView row : rows) {
            if (row.fullyResolvable()) resolvableMatches++;
            resolvedPlayers += row.resolvedPlayerCount();
            ambiguousPlayers += row.ambiguousPlayerCount();
        }
        return new Counts(rows.size(), resolvableMatches, resolvedPlayers, ambiguousPlayers);
    }

    private static String get(List<String> list, int i) {
        return list != null && i < list.size() ? list.get(i) : null;
    }

    private static boolean parseBoolean(String s) {
        return "true".equals(s);
    }

    private static int parseIntOrZero(String s) {
        try {
            return s == null || s.isBlank() ? 0 : Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
