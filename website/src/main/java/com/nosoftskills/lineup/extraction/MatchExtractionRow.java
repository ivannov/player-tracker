package com.nosoftskills.lineup.extraction;

import com.nosoftskills.lineup.model.ExternalRefSource;
import com.nosoftskills.lineup.scraping.BfuFixture;
import com.nosoftskills.lineup.scraping.ScrapedMatch;

import java.util.List;

/**
 * One discovered fixture's full extraction preview: which source (primary bfu-tournaments.com
 * or fallback ebfu.net) supplied the data (or {@code extractionError} when both failed), the
 * per-side team resolution, and every lineup entry's player resolution. Produced by both
 * {@link MatchExtractionService#preview(ExtractionRequest)} (read-only) and
 * {@link MatchExtractionService#confirm(ExtractionRequest)} (which additionally persists).
 */
public record MatchExtractionRow(
        BfuFixture fixture,
        ExternalRefSource source,
        ScrapedMatch scrapedMatch,
        String extractionError,
        TeamSideResolution home,
        TeamSideResolution away,
        List<PlayerRowResolution> homeStarters,
        List<PlayerRowResolution> homeReserves,
        List<PlayerRowResolution> awayStarters,
        List<PlayerRowResolution> awayReserves) {

    public boolean isFullyResolvable() {
        return scrapedMatch != null && home.isResolved() && away.isResolved();
    }
}
