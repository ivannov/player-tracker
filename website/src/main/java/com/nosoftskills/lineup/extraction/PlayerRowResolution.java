package com.nosoftskills.lineup.extraction;

import com.nosoftskills.lineup.matching.PlayerMatchingService;

/**
 * One scraped lineup entry (shirt number + raw name) together with the outcome of resolving it
 * via {@link PlayerMatchingService}. {@code matchResult} is null when resolution was never
 * attempted -- the side's team couldn't be resolved, so there is no team to scope candidate
 * players by (see {@link TeamSideResolution#isResolved()}).
 */
public record PlayerRowResolution(int number, String rawName, PlayerMatchingService.MatchResult matchResult) {

    public boolean isResolved() {
        return matchResult != null && matchResult.isResolved();
    }
}
