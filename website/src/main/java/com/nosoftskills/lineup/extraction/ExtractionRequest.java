package com.nosoftskills.lineup.extraction;

import java.time.LocalDate;

/**
 * Input to {@link MatchExtractionService}: one competition's bfu-tournaments.com league
 * "results" page URL (see BfuFixtureScraperService), the competition/season those matches
 * should be attributed to, and the date to extract. "All competitions" (LT-008's admin-facing
 * wording) is a caller-side concern -- the wizard/scheduled job loops this request once per
 * known competition+URL.
 */
public record ExtractionRequest(Long competitionId, String fixturesUrl, String season, LocalDate date) {
}
