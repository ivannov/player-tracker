package com.nosoftskills.lineup.participation;

import com.nosoftskills.lineup.model.Team;
import com.nosoftskills.lineup.model.TeamFormation;

import java.util.List;

public record TeamResolutionRow(
        String scrapedName,
        Team resolvedTeam,
        List<TeamFormation> formations) {
}
