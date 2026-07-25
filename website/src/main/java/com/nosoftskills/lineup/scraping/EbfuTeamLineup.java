package com.nosoftskills.lineup.scraping;

import java.util.List;

public record EbfuTeamLineup(String teamName, List<EbfuLineupEntry> starters, List<EbfuLineupEntry> reserves, String headCoach) {
}
