package com.nosoftskills.lineup.scraping;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Adapts {@link EbfuMatchLineup} (ebfu.net's raw scraped lineup shape) into the source-independent
 * {@link ScrapedMatch} contract that LT-008's on-demand extraction wizard consumes, so the ebfu.net
 * fallback can be treated interchangeably with the bfu-tournaments.com primary source. ebfu.net only
 * exposes starting lineups and reserves -- score, events and substitutions are always empty/null.
 */
@ApplicationScoped
public class EbfuMatchLineupMapper {

    public ScrapedMatch toScrapedMatch(EbfuMatchLineup lineup) {
        return new ScrapedMatch(
                toScrapedTeamLineup(lineup.home()),
                toScrapedTeamLineup(lineup.away()),
                null,
                null,
                List.of(),
                List.of());
    }

    private ScrapedTeamLineup toScrapedTeamLineup(EbfuTeamLineup teamLineup) {
        return new ScrapedTeamLineup(
                teamLineup.teamName(),
                toScrapedLineupEntries(teamLineup.starters()),
                toScrapedLineupEntries(teamLineup.reserves()));
    }

    private List<ScrapedLineupEntry> toScrapedLineupEntries(List<EbfuLineupEntry> entries) {
        return entries.stream()
                .map(entry -> new ScrapedLineupEntry(entry.number(), entry.rawName()))
                .toList();
    }
}
