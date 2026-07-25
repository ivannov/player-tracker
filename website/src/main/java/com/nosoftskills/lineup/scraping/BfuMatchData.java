package com.nosoftskills.lineup.scraping;

import java.util.List;

public record BfuMatchData(
        BfuTeamLineup home,
        BfuTeamLineup away,
        short homeScore,
        short awayScore,
        List<BfuMatchEvent> events,
        List<BfuSubstitution> substitutions) {
}
