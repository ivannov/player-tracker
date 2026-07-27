package com.nosoftskills.lineup.extraction;

import com.nosoftskills.lineup.matching.PlayerMatchingService;
import com.nosoftskills.lineup.matching.TeamResolutionService;
import com.nosoftskills.lineup.model.AmbiguityReview;
import com.nosoftskills.lineup.model.Competition;
import com.nosoftskills.lineup.model.ExternalRefSource;
import com.nosoftskills.lineup.model.Match;
import com.nosoftskills.lineup.model.MatchEvent;
import com.nosoftskills.lineup.model.MatchEventType;
import com.nosoftskills.lineup.model.Participation;
import com.nosoftskills.lineup.model.Player;
import com.nosoftskills.lineup.model.PlayerAppearance;
import com.nosoftskills.lineup.model.Team;
import com.nosoftskills.lineup.model.TeamAlias;
import com.nosoftskills.lineup.scraping.BfuFixture;
import com.nosoftskills.lineup.scraping.BfuFixtureScraperService;
import com.nosoftskills.lineup.scraping.BfuMatchData;
import com.nosoftskills.lineup.scraping.BfuMatchDataMapper;
import com.nosoftskills.lineup.scraping.BfuMatchScraperService;
import com.nosoftskills.lineup.scraping.BfuScraperException;
import com.nosoftskills.lineup.scraping.EbfuMatchLineup;
import com.nosoftskills.lineup.scraping.EbfuMatchLineupMapper;
import com.nosoftskills.lineup.scraping.EbfuMatchLineupScraperService;
import com.nosoftskills.lineup.scraping.EbfuScraperException;
import com.nosoftskills.lineup.scraping.ScrapedLineupEntry;
import com.nosoftskills.lineup.scraping.ScrapedMatch;
import com.nosoftskills.lineup.scraping.ScrapedMatchEvent;
import com.nosoftskills.lineup.scraping.ScrapedMatchEventType;
import com.nosoftskills.lineup.scraping.ScrapedSubstitution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Given a competition + date, discovers that day's matches (LT-008.01), extracts full match
 * detail per fixture -- bfu-tournaments.com primary, ebfu.net fallback (lineups only) when the
 * primary has nothing for that match -- resolves each side's team and every scraped player name,
 * and (on {@link #confirm}) persists Match/PlayerAppearance/MatchEvent rows. Reusable as-is by
 * LT-009's scheduled job: {@link #confirm} never blocks on ambiguity, it just queues it.
 */
@ApplicationScoped
public class MatchExtractionService {

    @Inject
    BfuFixtureScraperService fixtureScraperService;

    @Inject
    BfuMatchScraperService bfuMatchScraperService;

    @Inject
    BfuMatchDataMapper bfuMatchDataMapper;

    @Inject
    EbfuMatchLineupScraperService ebfuMatchLineupScraperService;

    @Inject
    EbfuMatchLineupMapper ebfuMatchLineupMapper;

    @Inject
    TeamResolutionService teamResolutionService;

    @Inject
    PlayerMatchingService playerMatchingService;

    /**
     * Dry run: discovers and resolves everything a {@link #confirm} call for the same request
     * would, without persisting any Match/PlayerAppearance/MatchEvent row. Player-name resolution
     * still goes through {@link PlayerMatchingService#resolve}, which -- independent of this
     * being a "dry run" -- writes PlayerAlias/AmbiguityReview rows as its own learn-on-read
     * behavior (same as every other caller of that service); that bookkeeping is not the match
     * data this method promises not to persist.
     */
    public List<MatchExtractionRow> preview(ExtractionRequest request) throws BfuScraperException {
        return buildRows(request).rows();
    }

    @Transactional
    public MatchExtractionSummary confirm(ExtractionRequest request) throws BfuScraperException {
        BuildResult built = buildRows(request);
        int created = 0;
        int reused = 0;
        int skipped = 0;

        for (MatchExtractionRow row : built.rows()) {
            if (!row.isFullyResolvable()) {
                skipped++;
                continue;
            }

            Map<String, TeamAlias> aliases = built.teamAliasesBySource().getOrDefault(row.source(), new HashMap<>());
            teamResolutionService.recordAlias(aliases, row.source(), row.home().rawName(), row.home().team());
            teamResolutionService.recordAlias(aliases, row.source(), row.away().rawName(), row.away().team());

            Match match = findExistingMatch(row.home().participation().id, row.away().participation().id, request.date());
            if (match == null) {
                match = new Match();
                match.homeTeam = row.home().participation();
                match.awayTeam = row.away().participation();
                match.date = request.date();
                match.homeScore = row.scrapedMatch().homeScore();
                match.awayScore = row.scrapedMatch().awayScore();
                match.persist();
                created++;
            } else {
                reused++;
            }

            Map<Long, PlayerAppearance> appearanceByPlayerId = new HashMap<>();
            persistSide(match, row.home().participation(), row.homeStarters(), true, appearanceByPlayerId);
            persistSide(match, row.home().participation(), row.homeReserves(), false, appearanceByPlayerId);
            persistSide(match, row.away().participation(), row.awayStarters(), true, appearanceByPlayerId);
            persistSide(match, row.away().participation(), row.awayReserves(), false, appearanceByPlayerId);

            Map<String, PlayerRowResolution> homeByName = byRawName(row.homeStarters(), row.homeReserves());
            Map<String, PlayerRowResolution> awayByName = byRawName(row.awayStarters(), row.awayReserves());
            persistEvents(row.scrapedMatch().events(), homeByName, awayByName, appearanceByPlayerId);
            applySubstitutions(row.scrapedMatch().substitutions(), homeByName, awayByName, appearanceByPlayerId);

            linkPendingReviewsToMatch(match, row);
        }

        return new MatchExtractionSummary(created, reused, skipped);
    }

    private record ExtractedMatch(BfuFixture fixture, ExternalRefSource source, ScrapedMatch scrapedMatch, String error) {
    }

    private record BuildResult(List<MatchExtractionRow> rows, Map<ExternalRefSource, Map<String, TeamAlias>> teamAliasesBySource) {
    }

    private BuildResult buildRows(ExtractionRequest request) throws BfuScraperException {
        if (Competition.findById(request.competitionId()) == null) {
            throw new IllegalArgumentException("Unknown competition id: " + request.competitionId());
        }

        List<BfuFixture> fixtures = fixtureScraperService.findMatches(request.fixturesUrl(), request.date());
        List<ExtractedMatch> extractedMatches = new ArrayList<>(fixtures.size());
        for (BfuFixture fixture : fixtures) {
            extractedMatches.add(extractOne(fixture, request.date()));
        }

        Map<String, Team> teamsByLowerName = Team.<Team>listAll().stream()
                .collect(Collectors.toMap(t -> t.name.toLowerCase(Locale.ROOT), t -> t, (a, b) -> a));

        Map<ExternalRefSource, Set<String>> rawTeamNamesBySource = new EnumMap<>(ExternalRefSource.class);
        for (ExtractedMatch extracted : extractedMatches) {
            if (extracted.scrapedMatch() == null) continue;
            rawTeamNamesBySource.computeIfAbsent(extracted.source(), s -> new HashSet<>())
                    .add(extracted.scrapedMatch().home().teamName());
            rawTeamNamesBySource.get(extracted.source()).add(extracted.scrapedMatch().away().teamName());
        }
        Map<ExternalRefSource, Map<String, TeamAlias>> teamAliasesBySource = new EnumMap<>(ExternalRefSource.class);
        for (Map.Entry<ExternalRefSource, Set<String>> entry : rawTeamNamesBySource.entrySet()) {
            teamAliasesBySource.put(entry.getKey(),
                    teamResolutionService.preloadAliases(entry.getKey(), entry.getValue()));
        }

        List<MatchExtractionRow> rows = new ArrayList<>(extractedMatches.size());
        for (ExtractedMatch extracted : extractedMatches) {
            rows.add(toRow(extracted, teamsByLowerName, teamAliasesBySource, request));
        }

        return new BuildResult(rows, teamAliasesBySource);
    }

    // Isolates a single fixture's failure (bad match page, ebfu.net day list surprise, ...) so
    // one broken match doesn't abort extracting the rest of the day's fixtures.
    private ExtractedMatch extractOne(BfuFixture fixture, LocalDate date) {
        try {
            BfuMatchData data = bfuMatchScraperService.extractMatch(fixture.matchUrl());
            return new ExtractedMatch(fixture, ExternalRefSource.BFU_TOURNAMENTS, bfuMatchDataMapper.toScrapedMatch(data), null);
        } catch (BfuScraperException primaryError) {
            try {
                EbfuMatchLineup lineup = ebfuMatchLineupScraperService.extractLineup(
                        date, fixture.homeTeamName(), fixture.awayTeamName());
                return new ExtractedMatch(fixture, ExternalRefSource.EBFU, ebfuMatchLineupMapper.toScrapedMatch(lineup), null);
            } catch (EbfuScraperException fallbackError) {
                String message = "bfu-tournaments.com: " + primaryError.getMessage()
                        + "; ebfu.net: " + fallbackError.getMessage();
                return new ExtractedMatch(fixture, null, null, message);
            }
        }
    }

    private MatchExtractionRow toRow(ExtractedMatch extracted, Map<String, Team> teamsByLowerName,
            Map<ExternalRefSource, Map<String, TeamAlias>> teamAliasesBySource, ExtractionRequest request) {
        if (extracted.scrapedMatch() == null) {
            TeamSideResolution home = new TeamSideResolution(extracted.fixture().homeTeamName(), null, null);
            TeamSideResolution away = new TeamSideResolution(extracted.fixture().awayTeamName(), null, null);
            return new MatchExtractionRow(extracted.fixture(), null, null, extracted.error(),
                    home, away, List.of(), List.of(), List.of(), List.of());
        }

        Map<String, TeamAlias> aliases = teamAliasesBySource.getOrDefault(extracted.source(), Map.of());
        TeamSideResolution home = resolveTeamSide(extracted.scrapedMatch().home().teamName(), aliases,
                teamsByLowerName, request.competitionId(), request.season());
        TeamSideResolution away = resolveTeamSide(extracted.scrapedMatch().away().teamName(), aliases,
                teamsByLowerName, request.competitionId(), request.season());

        List<PlayerRowResolution> homeStarters = resolvePlayers(extracted.scrapedMatch().home().starters(), home, extracted.source());
        List<PlayerRowResolution> homeReserves = resolvePlayers(extracted.scrapedMatch().home().reserves(), home, extracted.source());
        List<PlayerRowResolution> awayStarters = resolvePlayers(extracted.scrapedMatch().away().starters(), away, extracted.source());
        List<PlayerRowResolution> awayReserves = resolvePlayers(extracted.scrapedMatch().away().reserves(), away, extracted.source());

        return new MatchExtractionRow(extracted.fixture(), extracted.source(), extracted.scrapedMatch(), null,
                home, away, homeStarters, homeReserves, awayStarters, awayReserves);
    }

    private TeamSideResolution resolveTeamSide(String rawName, Map<String, TeamAlias> aliases,
            Map<String, Team> teamsByLowerName, Long competitionId, String season) {
        TeamAlias alias = aliases.get(rawName);
        Team team = alias != null ? alias.team : teamsByLowerName.get(rawName.toLowerCase(Locale.ROOT));
        Participation participation = team != null
                ? Participation.<Participation>find(
                        "teamFormation.team.id = ?1 and competition.id = ?2 and season = ?3",
                        team.id, competitionId, season).firstResult()
                : null;
        return new TeamSideResolution(rawName, team, participation);
    }

    // side.team() is null when the team itself couldn't be resolved -- there is no roster to
    // scope PlayerMatchingService's candidate search by, so resolution is never attempted (not
    // "attempted and failed").
    private List<PlayerRowResolution> resolvePlayers(List<ScrapedLineupEntry> entries, TeamSideResolution side,
            ExternalRefSource source) {
        List<PlayerRowResolution> resolutions = new ArrayList<>(entries.size());
        for (ScrapedLineupEntry entry : entries) {
            PlayerMatchingService.MatchResult matchResult = side.team() != null
                    ? playerMatchingService.resolve(entry.playerName(), side.team().id, source)
                    : null;
            resolutions.add(new PlayerRowResolution(entry.number(), entry.playerName(), matchResult));
        }
        return resolutions;
    }

    private Match findExistingMatch(Long homeParticipationId, Long awayParticipationId, LocalDate date) {
        return Match.find("homeTeam.id = ?1 and awayTeam.id = ?2 and date = ?3",
                homeParticipationId, awayParticipationId, date).firstResult();
    }

    // Idempotent: re-running the same extraction finds and reuses the existing appearance
    // (matching the (player_id, match_id) unique constraint) instead of attempting a duplicate
    // insert.
    private void persistSide(Match match, Participation participation, List<PlayerRowResolution> entries,
            boolean starter, Map<Long, PlayerAppearance> appearanceByPlayerId) {
        for (PlayerRowResolution resolution : entries) {
            if (!resolution.isResolved()) continue;

            Player player = resolution.matchResult().resolvedPlayer();
            PlayerAppearance appearance = PlayerAppearance.<PlayerAppearance>find(
                    "player.id = ?1 and match.id = ?2", player.id, match.id).firstResult();
            if (appearance == null) {
                appearance = new PlayerAppearance();
                appearance.player = player;
                appearance.match = match;
                appearance.participation = participation;
                appearance.starter = starter;
                appearance.number = (short) resolution.number();
                appearance.persist();
            }
            appearanceByPlayerId.put(player.id, appearance);
        }
    }

    private Map<String, PlayerRowResolution> byRawName(List<PlayerRowResolution> starters, List<PlayerRowResolution> reserves) {
        Map<String, PlayerRowResolution> byRawName = new HashMap<>();
        for (PlayerRowResolution resolution : starters) byRawName.put(resolution.rawName(), resolution);
        for (PlayerRowResolution resolution : reserves) byRawName.put(resolution.rawName(), resolution);
        return byRawName;
    }

    // Dedups on (playerAppearance, type, minute) since match_events has no unique constraint of
    // its own -- re-running the same extraction must not double up goals/cards.
    private void persistEvents(List<ScrapedMatchEvent> events, Map<String, PlayerRowResolution> homeByName,
            Map<String, PlayerRowResolution> awayByName, Map<Long, PlayerAppearance> appearanceByPlayerId) {
        for (ScrapedMatchEvent scrapedEvent : events) {
            PlayerRowResolution resolution = (scrapedEvent.home() ? homeByName : awayByName).get(scrapedEvent.playerName());
            if (resolution == null || !resolution.isResolved()) continue;
            PlayerAppearance appearance = appearanceByPlayerId.get(resolution.matchResult().resolvedPlayer().id);
            if (appearance == null) continue;

            MatchEventType type = toMatchEventType(scrapedEvent.type());
            Short minute = (short) scrapedEvent.minute();
            boolean exists = MatchEvent.count("playerAppearance.id = ?1 and type = ?2 and minute = ?3",
                    appearance.id, type, minute) > 0;
            if (exists) continue;

            MatchEvent event = new MatchEvent();
            event.playerAppearance = appearance;
            event.type = type;
            event.minute = minute;
            event.persist();
        }
    }

    private MatchEventType toMatchEventType(ScrapedMatchEventType type) {
        return switch (type) {
            case GOAL -> MatchEventType.GOAL;
            case YELLOW_CARD -> MatchEventType.YELLOW_CARD;
            case SECOND_YELLOW_CARD -> MatchEventType.SECOND_YELLOW_CARD;
            case RED_CARD -> MatchEventType.RED_CARD;
        };
    }

    private void applySubstitutions(List<ScrapedSubstitution> substitutions, Map<String, PlayerRowResolution> homeByName,
            Map<String, PlayerRowResolution> awayByName, Map<Long, PlayerAppearance> appearanceByPlayerId) {
        for (ScrapedSubstitution substitution : substitutions) {
            Map<String, PlayerRowResolution> sideByName = substitution.home() ? homeByName : awayByName;
            Short minute = (short) substitution.minute();
            applySubstitutionMinute(sideByName.get(substitution.playerInName()), appearanceByPlayerId, minute, true);
            applySubstitutionMinute(sideByName.get(substitution.playerOutName()), appearanceByPlayerId, minute, false);
        }
    }

    private void applySubstitutionMinute(PlayerRowResolution resolution, Map<Long, PlayerAppearance> appearanceByPlayerId,
            Short minute, boolean comingIn) {
        if (resolution == null || !resolution.isResolved()) return;
        PlayerAppearance appearance = appearanceByPlayerId.get(resolution.matchResult().resolvedPlayer().id);
        if (appearance == null) return;
        if (comingIn) {
            appearance.substitutedInMinute = minute;
        } else {
            appearance.substitutedOutMinute = minute;
        }
    }

    // Only reachable for rows that just got a Match persisted (see confirm()), so a review's
    // match, once set here, reflects the first match extraction actually saved for it -- not
    // overwritten on a later dry-run preview() call for the same rawName+team.
    private void linkPendingReviewsToMatch(Match match, MatchExtractionRow row) {
        linkSide(match, row.homeStarters());
        linkSide(match, row.homeReserves());
        linkSide(match, row.awayStarters());
        linkSide(match, row.awayReserves());
    }

    private void linkSide(Match match, List<PlayerRowResolution> entries) {
        for (PlayerRowResolution resolution : entries) {
            if (resolution.matchResult() == null) continue;
            AmbiguityReview review = resolution.matchResult().pendingReview();
            if (review != null && review.match == null) {
                review.match = match;
            }
        }
    }
}
