package com.nosoftskills.lineup.scraping;

import java.util.List;

/**
 * Source-independent shape of a scraped match that LT-008's on-demand extraction wizard
 * consumes. Both the bfu-tournaments.com scraper (primary, LT-007.02) and the ebfu.net
 * scraper (fallback, lineups only, LT-006.02) adapt their raw DTOs into this shape.
 * Fields a given source can't provide (e.g. ebfu.net has no score/events) are null/empty.
 */
public record ScrapedMatch(
        ScrapedTeamLineup home,
        ScrapedTeamLineup away,
        Short homeScore,
        Short awayScore,
        List<ScrapedMatchEvent> events,
        List<ScrapedSubstitution> substitutions) {
}
