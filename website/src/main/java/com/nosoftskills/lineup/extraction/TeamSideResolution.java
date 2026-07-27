package com.nosoftskills.lineup.extraction;

import com.nosoftskills.lineup.model.Participation;
import com.nosoftskills.lineup.model.Team;

/**
 * Resolution of one scraped match side's raw team name to a {@link Team} (via TeamAlias or a
 * name fallback) and, further, to the specific {@link Participation} (team formation +
 * competition + season) that {@link com.nosoftskills.lineup.model.Match#homeTeam}/{@code
 * awayTeam} actually reference. {@code team} can be resolved while {@code participation} stays
 * null (the team exists but hasn't been imported into this competition/season yet) -- either
 * case leaves the side unresolved for persistence purposes.
 */
public record TeamSideResolution(String rawName, Team team, Participation participation) {

    public boolean isResolved() {
        return participation != null;
    }
}
