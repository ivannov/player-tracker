package com.nosoftskills.lineup.scraping;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Adapts {@link BfuMatchData} (bfu-tournaments.com's raw scraped match shape) into the
 * source-independent {@link ScrapedMatch} contract that LT-008's on-demand extraction wizard
 * consumes, so bfu-tournaments.com can be treated as the primary source with ebfu.net
 * (see {@link EbfuMatchLineupMapper}) as an interchangeable fallback for lineups.
 */
@ApplicationScoped
public class BfuMatchDataMapper {

    public ScrapedMatch toScrapedMatch(BfuMatchData matchData) {
        return new ScrapedMatch(
                toScrapedTeamLineup(matchData.home()),
                toScrapedTeamLineup(matchData.away()),
                matchData.homeScore(),
                matchData.awayScore(),
                toScrapedMatchEvents(matchData.events()),
                toScrapedSubstitutions(matchData.substitutions()));
    }

    private ScrapedTeamLineup toScrapedTeamLineup(BfuTeamLineup teamLineup) {
        return new ScrapedTeamLineup(
                teamLineup.teamName(),
                toScrapedLineupEntries(teamLineup.starters()),
                toScrapedLineupEntries(teamLineup.reserves()));
    }

    private List<ScrapedLineupEntry> toScrapedLineupEntries(List<BfuLineupEntry> entries) {
        return entries.stream()
                .map(entry -> new ScrapedLineupEntry(entry.number(), entry.playerName()))
                .toList();
    }

    private List<ScrapedMatchEvent> toScrapedMatchEvents(List<BfuMatchEvent> events) {
        return events.stream()
                .map(event -> new ScrapedMatchEvent(event.home(), toScrapedMatchEventType(event.type()), event.minute(), event.playerName()))
                .toList();
    }

    private ScrapedMatchEventType toScrapedMatchEventType(BfuMatchEventType type) {
        return switch (type) {
            case GOAL -> ScrapedMatchEventType.GOAL;
            case YELLOW_CARD -> ScrapedMatchEventType.YELLOW_CARD;
            case SECOND_YELLOW_CARD -> ScrapedMatchEventType.SECOND_YELLOW_CARD;
            case RED_CARD -> ScrapedMatchEventType.RED_CARD;
        };
    }

    private List<ScrapedSubstitution> toScrapedSubstitutions(List<BfuSubstitution> substitutions) {
        return substitutions.stream()
                .map(sub -> new ScrapedSubstitution(sub.home(), sub.minute(), sub.playerInName(), sub.playerOutName()))
                .toList();
    }
}
