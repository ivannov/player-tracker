package com.nosoftskills.lineup.extraction;

import java.util.List;

/**
 * Template-facing, string-serializable projection of a {@link MatchExtractionRow}, so the wizard
 * can round-trip a discovered match through hidden form fields across steps without re-scraping.
 */
public record ExtractionRowView(
        String matchUrl,
        String homeTeamName,
        boolean homeResolved,
        String awayTeamName,
        boolean awayResolved,
        boolean fullyResolvable,
        String extractionError,
        int resolvedPlayerCount,
        int ambiguousPlayerCount,
        int totalPlayerCount) {

    public static ExtractionRowView from(MatchExtractionRow row) {
        int resolved = 0;
        int ambiguous = 0;
        for (List<PlayerRowResolution> side : List.of(
                row.homeStarters(), row.homeReserves(), row.awayStarters(), row.awayReserves())) {
            for (PlayerRowResolution resolution : side) {
                if (resolution.isResolved()) {
                    resolved++;
                } else {
                    ambiguous++;
                }
            }
        }
        return new ExtractionRowView(
                row.fixture().matchUrl(),
                row.home().rawName(), row.home().isResolved(),
                row.away().rawName(), row.away().isResolved(),
                row.isFullyResolvable(),
                row.extractionError(),
                resolved, ambiguous, resolved + ambiguous);
    }
}
